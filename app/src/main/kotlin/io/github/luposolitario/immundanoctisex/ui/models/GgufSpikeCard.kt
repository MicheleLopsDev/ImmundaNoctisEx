package io.github.luposolitario.immundanoctisex.ui.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
@Composable
fun GgufSpikeCard(
    modelPath: String,
    onModelPathChange: (String) -> Unit,
    isRunning: Boolean,
    result: String?,
    onRun: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Prova GGUF (spike)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Metti un file .gguf sul telefono (es. in Download) e incolla qui il percorso " +
                    "completo. Genera due frasi con un prompt fisso, solo per vedere se carica.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = modelPath,
                onValueChange = onModelPathChange,
                label = { Text("Percorso file .gguf") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onRun, enabled = modelPath.isNotBlank() && !isRunning) {
                Text(if (isRunning) "Generazione…" else "Genera (prova)")
            }
            result?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
