package io.github.luposolitario.immundanoctisex.ui.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// SPIKE (27/07/2026, Michele: "facciamo una prova ma non spenderci
// troppo codice"): card temporanea per verificare se Llamatik carica un
// GGUF e genera testo sul Razr, prompt fisso (LlamaCppSpike.kt). Non è
// pensata per restare: se lo spike regge, si sostituisce con un vero
// motore selezionabile come gli altri modelli.
//
// Selettore file di SISTEMA, non un campo per incollare il percorso
// (prima versione, sbagliata): Android blocca l'accesso diretto a
// percorsi come /sdcard/Download/... senza un permesso di storage
// esteso che l'app non ha — stesso motivo per cui l'import di un
// modello .litertlm personalizzato usa già il selettore.
@Composable
fun GgufSpikeCard(
    isRunning: Boolean,
    result: String?,
    onPickFile: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Prova GGUF (spike)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Scegli un file .gguf dal telefono. Genera due frasi con un prompt " +
                    "fisso, solo per vedere se carica ed è veloce.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onPickFile, enabled = !isRunning) {
                Text(if (isRunning) "Generazione…" else "Scegli file .gguf e genera")
            }
            result?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
