package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.engine.inference.EnrichedScene
import io.github.luposolitario.immundanoctisex.core.engine.inference.LinguaOutput
import io.github.luposolitario.immundanoctisex.tool.inference.TraduttoreScene

// Tutto ciò che serve per mostrare, sotto ogni stringa della scena, come
// il modello la renderà al giocatore (02/08/2026, Michele: "dobbiamo
// aggiungere le versioni modificate dai modelli se questi sono attivi
// per tutte le stringhe che traduciamo").
//
// Passato come `null` quando il modello non è caricato: in quel caso
// l'editor di scena non mostra né il pulsante né le righe, esattamente
// com'era prima. Nessuna funzione a metà, nessun pulsante che non fa
// nulla.
data class AnteprimaModello(
    val traduttore: TraduttoreScene,
    val manifest: Manifest,
    val lingua: LinguaOutput,
    val modalitaTraduzione: Boolean,
)

// Lo stato dell'anteprima per la scena aperta.
data class StatoAnteprima(
    val risultato: EnrichedScene? = null,
    val inCorso: Boolean = false,
    val errore: String? = null,
    // Testo che si forma mentre il modello scrive: si vede la resa
    // nascere invece di fissare una rotella per dieci secondi.
    val parziale: String = "",
)

// La riga sotto un campo: la resa del modello per QUELLA stringa.
// In corsivo e attenuata perché non è testo del libro — è un'anteprima,
// e il JSON non la contiene (il libro conserva sempre l'originale).
@Composable
fun RigaTradotta(testo: String?, etichetta: String = "Anteprima", modifier: Modifier = Modifier) {
    val contenuto = testo?.takeIf { it.isNotBlank() } ?: return
    Text(
        text = "$etichetta: $contenuto",
        style = MaterialTheme.typography.labelSmall,
        fontStyle = FontStyle.Italic,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, bottom = 6.dp),
    )
}

// La barra in cima all'editor di scena: dice in che lingua e con quale
// modalità si sta guardando, e lancia la traduzione dell'intera scena.
//
// Un solo pulsante per tutta la scena, non uno per campo: il modello
// riceve la scena INTERA come nel gioco, e le scelte le rende sapendo
// che cosa racconta il testo sopra. Tradurre i campi separatamente
// darebbe un risultato che il giocatore non vedrà mai.
@Composable
fun BarraAnteprima(
    anteprima: AnteprimaModello,
    stato: StatoAnteprima,
    onTraduci: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(enabled = !stato.inCorso, onClick = onTraduci) {
                Text(if (stato.risultato == null) "Anteprima col modello" else "Rigenera anteprima")
            }
            if (stato.inCorso) {
                CircularProgressIndicator(Modifier.padding(start = 12.dp).size(18.dp))
            }
            Text(
                "  ${anteprima.lingua.displayName} · " +
                    (if (anteprima.modalitaTraduzione) "solo traduzione" else "arricchimento") +
                    // Il tempo va detto prima, non scoperto aspettando:
                    // una scena intera sta sui 35 secondi (misurato il
                    // 02/08/2026 su GPU integrata), non è un istante.
                    " · circa mezzo minuto",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        stato.errore?.let { errore ->
            Text(
                "✖ $errore",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (stato.inCorso && stato.parziale.isNotBlank()) {
            Text(
                stato.parziale,
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (stato.risultato != null && !stato.inCorso) {
            Text(
                "L'anteprima non entra nel libro: il JSON conserva sempre il testo originale.",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
