package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.tool.inference.RiassuntorePercorso

// Lo stato del riassunto in corso o concluso per UNA scena di arrivo.
data class StatoRiassunto(
    val sceneId: String,
    val percorso: List<Scene> = emptyList(),
    val inCorso: Boolean = false,
    val avanzamento: RiassuntorePercorso.Avanzamento? = null,
    val testo: String? = null,
    val errore: String? = null,
)

// La finestra del riassunto (03/08/2026, Michele: "quando è pronta apre
// una popup... e genera il riassunto come ci eravamo detti").
//
// Resta aperta anche mentre il modello lavora, mostrando a che punto è:
// un percorso lungo si spezza in blocchi e ognuno costa una generazione,
// quindi l'attesa può essere di minuti — senza un avanzamento visibile
// sarebbe indistinguibile da un programma piantato.
@Composable
fun FinestraRiassunto(
    stato: StatoRiassunto,
    manifest: Manifest,
    onChiudi: () -> Unit,
) {
    var esito by remember(stato.sceneId) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!stato.inCorso) onChiudi() },
        title = {
            Text(
                "Riassunto — da ${stato.percorso.firstOrNull()?.id ?: "START"} a ${stato.sceneId}",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (stato.percorso.isNotEmpty()) {
                    Text(
                        "${stato.percorso.size} scene, nell'ordine in cui il giocatore le attraversa.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (stato.inCorso) {
                    val avanzamento = stato.avanzamento
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp))
                        Text(
                            "  " + if (avanzamento != null) {
                                "Passo ${avanzamento.passo} di ${avanzamento.totale} — ${avanzamento.descrizione}"
                            } else {
                                "Preparazione…"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (avanzamento != null && avanzamento.totale > 1) {
                        LinearProgressIndicator(
                            progress = { avanzamento.passo.toFloat() / avanzamento.totale },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        "Ogni blocco è una generazione a sé: su un percorso lungo ci vogliono minuti.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                stato.errore?.let { errore ->
                    Text("✖ $errore", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }

                stato.testo?.let { testo ->
                    Text(testo, style = MaterialTheme.typography.bodyLarge)
                }

                esito?.let { messaggio ->
                    Text(messaggio, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            // L'esportazione compare solo a riassunto pronto: un pulsante
            // che scrive un file vuoto non serve a nessuno.
            if (stato.testo != null) {
                OutlinedButton(onClick = {
                    val primo = stato.percorso.firstOrNull()?.id ?: "start"
                    val nome = "riassunto_scene_${primo}-${stato.sceneId}.md"
                    scegliPercorsoSalvataggio(nome)?.let { destinazione ->
                        esito = EsportaRiassunto.scrivi(destinazione, manifest, stato.percorso, stato.testo).fold(
                            onSuccess = { risultato ->
                                "✔ Salvato in ${risultato.fileMd.absolutePath}" +
                                    if (risultato.immaginiCopiate > 0) {
                                        " (${risultato.immaginiCopiate} immagini nella cartella accanto)"
                                    } else {
                                        ""
                                    }
                            },
                            onFailure = { "✖ ${it.message}" },
                        )
                    }
                }) {
                    Text("Esporta in Markdown")
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !stato.inCorso, onClick = onChiudi) {
                Text(if (stato.inCorso) "Attendi…" else "Chiudi")
            }
        },
    )
}
