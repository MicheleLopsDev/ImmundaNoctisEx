package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.engine.inference.DownloadableModel
import io.github.luposolitario.immundanoctisex.core.engine.inference.ModelCatalog
import io.github.luposolitario.immundanoctisex.tool.inference.ModelDownloader
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

// Scaricare i modelli dall'editor (02/08/2026, Michele: "aggiungi le
// stesse impostazioni che permettono di scaricare anche 2b e che puoi
// mettere l'url o il default di link da huggingface").
//
// I due modelli offerti sono `ModelCatalog.protected`, cioè esattamente
// quelli che il client considera di base — 4B e 2B, gli stessi file
// .litertlm, gli stessi link. Non una lista parallela da tenere
// allineata a mano: viene da :core:engine, si aggiorna in un posto solo.
//
// Il campo URL libero sotto serve per i link che il catalogo non ha
// (una variante nuova su HuggingFace, un file messo da parte): stesso
// scaricatore, nessuna scorciatoia diversa.
@Composable
fun ScaricaModelloCard(
    preferenze: EditorPreferences,
    abilitato: Boolean,
    onModelloPronto: (File) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val scaricatore = remember { ModelDownloader() }

    var cartella by remember { mutableStateOf(preferenze.cartellaModelli) }
    var urlLibero by remember { mutableStateOf("") }
    var nomeFileLibero by remember { mutableStateOf("") }
    var inCorso by remember { mutableStateOf<String?>(null) }
    var scaricati by remember { mutableStateOf(0L) }
    var totale by remember { mutableStateOf(0L) }
    var messaggio by remember { mutableStateOf("") }
    var lavoro by remember { mutableStateOf<Job?>(null) }

    fun avviaDownload(etichetta: String, url: String, nomeFile: String) {
        val destinazione = File(cartella, nomeFile)
        inCorso = etichetta
        scaricati = 0
        totale = 0
        messaggio = "Scaricamento di $etichetta in corso..."
        lavoro = scope.launch {
            val esito = scaricatore.scarica(url, destinazione) { fatti, quanti ->
                scaricati = fatti
                totale = quanti
            }
            messaggio = esito.fold(
                onSuccess = {
                    // Scaricato e subito selezionato: nessuno vuole
                    // aspettare 3,7 GB per poi doverlo cercare a mano.
                    onModelloPronto(it)
                    "✔ $etichetta pronto in ${it.absolutePath}"
                },
                onFailure = { "✖ ${ModelDownloader.descriviErrore(it)}  —  dettagli nel log" },
            )
            inCorso = null
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Scarica un modello", fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Cartella di destinazione", style = MaterialTheme.typography.labelMedium)
                    Text(cartella, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    enabled = inCorso == null,
                    onClick = {
                        scegliCartellaModelli()?.let {
                            cartella = it.absolutePath
                            preferenze.cartellaModelli = cartella
                        }
                    },
                ) {
                    Text("Cambia")
                }
            }

            HorizontalDivider()

            ModelCatalog.protected.forEach { modello ->
                RigaModello(
                    modello = modello,
                    cartella = cartella,
                    abilitato = abilitato && inCorso == null,
                    onScarica = { avviaDownload(modello.displayName, modello.url, modello.fileName) },
                    onUsa = onModelloPronto,
                )
            }

            HorizontalDivider()

            Text("Oppure da un link tuo", style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = urlLibero,
                onValueChange = { testo ->
                    urlLibero = testo
                    // Il nome del file si deduce dall'URL finché non lo
                    // si tocca: un campo in meno da riempire nel caso
                    // normale, e resta modificabile per i link che
                    // finiscono con un nome inutile.
                    if (nomeFileLibero.isBlank() || urlLibero.substringAfterLast('/').startsWith(nomeFileLibero.take(3))) {
                        nomeFileLibero = testo.substringAfterLast('/').substringBefore('?')
                    }
                },
                label = { Text("URL del modello (.litertlm)") },
                placeholder = { Text(ModelCatalog.default.url) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nomeFileLibero,
                    onValueChange = { nomeFileLibero = it },
                    label = { Text("Nome del file") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    enabled = abilitato && inCorso == null &&
                        urlLibero.startsWith("http") && nomeFileLibero.isNotBlank(),
                    onClick = { avviaDownload(nomeFileLibero, urlLibero, nomeFileLibero) },
                ) {
                    Text("Scarica")
                }
            }

            if (inCorso != null) {
                val frazione = if (totale > 0) scaricati.toFloat() / totale else 0f
                LinearProgressIndicator(progress = { frazione }, modifier = Modifier.fillMaxWidth())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (totale > 0) {
                            "${gigabyte(scaricati)} di ${gigabyte(totale)} GB (${(frazione * 100).toInt()}%)"
                        } else {
                            "${gigabyte(scaricati)} GB scaricati"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = {
                        lavoro?.cancel()
                        inCorso = null
                        messaggio = "Download annullato. Quello che è già stato scaricato resta: «Riprendi» riparte da lì."
                    }) {
                        Text("Annulla")
                    }
                }
            }

            if (messaggio.isNotBlank()) {
                Text(messaggio, style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider()

            // 02/08/2026, Michele: "come si vede il log su windows?" —
            // qui non c'è logcat, e lanciando l'.exe col doppio click non
            // c'è nemmeno una console. Il percorso va detto in chiaro
            // nella schermata dove serve.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Registro dell'editor", style = MaterialTheme.typography.labelMedium)
                    Text(EditorLog.file.absolutePath, style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = {
                    runCatching { java.awt.Desktop.getDesktop().open(EditorLog.file) }
                }) {
                    Text("Apri log")
                }
            }
        }
    }
}

@Composable
private fun RigaModello(
    modello: DownloadableModel,
    cartella: String,
    abilitato: Boolean,
    onScarica: () -> Unit,
    onUsa: (File) -> Unit,
) {
    val file = File(cartella, modello.fileName)
    // "Già scaricato" solo se il file c'è ED è grande all'incirca quanto
    // deve essere: un residuo troncato non deve passare per completo.
    val giaScaricato = file.exists() && file.length() > modello.sizeBytes / 2
    // Un troncone da un tentativo caduto: il download non ricomincia da
    // capo, riparte da qui (vedi ModelDownloader, richiesta Range).
    val parziale = File(cartella, modello.fileName + ".parziale")
    val giaPresenti = if (!giaScaricato && parziale.exists()) parziale.length() else 0L

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(modello.displayName, fontWeight = FontWeight.Bold)
            Text(
                "${"%.1f".format(modello.sizeGigabytes)} GB — ${modello.note}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (giaPresenti > 0) {
                Text(
                    "Interrotto a ${gigabyte(giaPresenti)} GB: riprende da lì.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        when {
            giaScaricato -> OutlinedButton(enabled = abilitato, onClick = { onUsa(file) }) { Text("Usa") }
            giaPresenti > 0 -> OutlinedButton(enabled = abilitato, onClick = onScarica) { Text("Riprendi") }
            else -> OutlinedButton(enabled = abilitato, onClick = onScarica) { Text("Scarica") }
        }
    }
}

private fun gigabyte(byte: Long): String = "%.1f".format(byte / 1_000_000_000.0)
