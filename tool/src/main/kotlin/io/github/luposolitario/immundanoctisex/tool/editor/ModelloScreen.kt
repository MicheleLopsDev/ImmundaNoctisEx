package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import io.github.luposolitario.immundanoctisex.core.engine.inference.InferenceConfig
import io.github.luposolitario.immundanoctisex.tool.inference.EditorInferenceEngine
import kotlinx.coroutines.launch
import java.io.File

// Schermata del modello (02/08/2026): scegli il .litertlm, caricalo,
// mandagli un prompt, guarda cosa risponde.
//
// Esiste per una ragione precisa e limitata: prima di costruirci sopra
// la traduzione nelle schede e il riassunto delle scene, si deve vedere
// Gemma rispondere DAVVERO dentro l'editor, sul PC di chi scrive. Se
// qualcosa non va — file sbagliato, backend assente, memoria — deve
// venire fuori qui, dove c'è un solo pulsante da guardare, non dentro
// una funzione più grande dove il guasto si confonde col resto.
@Composable
fun ModelloScreen(
    preferenze: EditorPreferences,
    motore: EditorInferenceEngine,
    onTornaAvvio: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var percorso by remember { mutableStateOf(preferenze.percorsoModello) }
    var modalitaTraduzione by remember { mutableStateOf(preferenze.modalitaTraduzione) }
    var caricamentoInCorso by remember { mutableStateOf(false) }
    var generazioneInCorso by remember { mutableStateOf(false) }
    var stato by remember { mutableStateOf(if (motore.isLoaded) "Modello caricato (${motore.activeBackend})" else "Modello non caricato") }
    var prompt by remember { mutableStateOf(PROMPT_DI_PROVA) }
    var risposta by remember { mutableStateOf("") }

    val fileModello = percorso.takeIf { it.isNotBlank() }?.let(::File)
    val modelloEsiste = fileModello?.exists() == true

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Modello", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Lo stesso motore e lo stesso file del telefono. Il testo non sarà identico " +
                "parola per parola (backend diverso, campionamento casuale), ma il " +
                "comportamento sì.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("File del modello", fontWeight = FontWeight.Bold)
                Text(
                    when {
                        percorso.isBlank() -> "Nessun modello scelto. Serve un file .litertlm — " +
                            "gemma-4-E4B-it (3,7 GB) o gemma-4-E2B-it (2,6 GB), gli stessi del client."
                        !modelloEsiste -> "⚠ Il file non esiste più: $percorso"
                        else -> "$percorso  (${"%.1f".format(fileModello.length() / 1_073_741_824.0)} GB)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = {
                        scegliFileModello()?.let { scelto ->
                            percorso = scelto.absolutePath
                            preferenze.percorsoModello = percorso
                            stato = "Modello scelto, non ancora caricato"
                        }
                    }) {
                        Text("Scegli file...")
                    }
                    Button(
                        enabled = modelloEsiste && !caricamentoInCorso && !generazioneInCorso,
                        onClick = {
                            caricamentoInCorso = true
                            stato = "Caricamento in corso... (il primo può volerci un minuto)"
                            scope.launch {
                                val config = if (modalitaTraduzione) {
                                    InferenceConfig.TRANSLATION_PRESET
                                } else {
                                    InferenceConfig()
                                }
                                val esito = motore.load(File(percorso), config)
                                stato = esito.fold(
                                    onSuccess = { "✔ Caricato su ${motore.activeBackend}" },
                                    onFailure = { "✖ ${it.message}" },
                                )
                                caricamentoInCorso = false
                            }
                        },
                    ) {
                        Text(if (motore.isLoaded) "Ricarica" else "Carica modello")
                    }
                    if (motore.isLoaded) {
                        OutlinedButton(
                            enabled = !generazioneInCorso,
                            onClick = {
                                scope.launch {
                                    motore.unload()
                                    stato = "Modello scaricato"
                                }
                            },
                        ) {
                            Text("Scarica")
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = modalitaTraduzione,
                        enabled = !caricamentoInCorso && !generazioneInCorso,
                        onCheckedChange = {
                            modalitaTraduzione = it
                            preferenze.modalitaTraduzione = it
                            if (motore.isLoaded) {
                                stato = "Modalità cambiata: ricarica il modello per applicarla"
                            }
                        },
                    )
                    Spacer(Modifier.height(0.dp))
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(
                            if (modalitaTraduzione) "Solo traduzione" else "Arricchimento",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (modalitaTraduzione) {
                                "Preset fedele del client: temperatura 0,2 — resta vicino al testo sorgente."
                            } else {
                                "Preset libero del client: temperatura 0,5 — riscrive arricchendo."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (caricamentoInCorso || generazioneInCorso) {
                        CircularProgressIndicator(Modifier.height(18.dp).padding(end = 10.dp))
                    }
                    Text(stato, style = MaterialTheme.typography.bodyMedium)
                }
                // Perché si è finiti su CPU (02/08/2026): sul telefono il
                // modello gira su GPU, quindi un'anteprima su CPU è una
                // prova fatta in condizioni diverse. Va detto, non lasciato
                // nel log nativo.
                motore.motivoRipiegoCpu?.let { motivo ->
                    Text(
                        motivo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        // Scaricamento dal catalogo condiviso col client, o da un link
        // proprio. Spento mentre il motore lavora: 3,7 GB in arrivo e
        // una generazione in corso si contenderebbero disco e memoria.
        ScaricaModelloCard(
            preferenze = preferenze,
            abilitato = !caricamentoInCorso && !generazioneInCorso,
            onModelloPronto = { file ->
                percorso = file.absolutePath
                preferenze.percorsoModello = percorso
                stato = "Modello pronto, non ancora caricato: premi «Carica modello»"
            },
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Prova", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Button(
                    enabled = motore.isLoaded && !generazioneInCorso && prompt.isNotBlank(),
                    onClick = {
                        generazioneInCorso = true
                        risposta = ""
                        scope.launch {
                            // Sessione nuova ogni volta, come nel client:
                            // due prove della stessa cosa devono partire
                            // dalle stesse condizioni.
                            motore.newSession()
                            val inizio = System.currentTimeMillis()
                            runCatching {
                                motore.generate(prompt).collect { pezzo -> risposta += pezzo }
                            }.onFailure { risposta = "✖ ${it.message}" }
                            val secondi = (System.currentTimeMillis() - inizio) / 1000.0
                            stato = "✔ Risposta in ${"%.1f".format(secondi)} s su ${motore.activeBackend}"
                            generazioneInCorso = false
                        }
                    },
                ) {
                    Text("Genera")
                }
                if (risposta.isNotEmpty()) {
                    Text("Risposta", fontWeight = FontWeight.Bold)
                    Text(risposta, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        OutlinedButton(onClick = onTornaAvvio) { Text("← Torna indietro") }
    }
}

// Una scena vera in inglese, non un "ciao come stai": il punto è vedere
// come se la cava col lavoro che dovrà fare davvero.
private const val PROMPT_DI_PROVA =
    "Translate the following gamebook scene into Italian, staying as close as possible " +
        "to the original wording. Answer with the translation only.\n\n" +
        "You are standing at the edge of the Fryelund Forest. The trail ahead is narrow " +
        "and overgrown, and the light is failing fast."
