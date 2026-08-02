package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

// La resa del modello per UNA stringa, sotto il campo che la contiene.
//
// Era una riga sola in `labelSmall` corsivo (02/08/2026, Michele: "il
// testo è troppo piccolo e sarebbe bene utilizzare dei box per un
// maggior focus sul testo"): la dimensione più piccola del tema, per
// il testo che l'autore deve leggere con più attenzione di tutti —
// esattamente al contrario. Ora è un riquadro con sfondo proprio,
// filetto colorato a sinistra e corpo in `bodyMedium`, la stessa
// misura dei campi di modifica.
@Composable
fun RigaTradotta(testo: String?, etichetta: String = "Anteprima", modifier: Modifier = Modifier) {
    val contenuto = testo?.takeIf { it.isNotBlank() } ?: return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 2.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
    ) {
        // Il filetto verticale lega visivamente il riquadro al campo che
        // sta sopra: senza, in una scheda con molte scelte non si capisce
        // a quale testo appartenga quale anteprima.
        Box(
            Modifier
                .padding(vertical = 6.dp)
                .width(3.dp)
                .heightIn(min = 20.dp)
                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(2.dp)),
        )
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                etichetta.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                contenuto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
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
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            stato.errore?.let { errore ->
                Text(
                    "✖ $errore",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            // Il testo che si sta formando: si legge davvero, mentre
            // arriva, invece di fissare una rotella per mezzo minuto.
            if (stato.inCorso && stato.parziale.isNotBlank()) {
                Text(
                    stato.parziale,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (stato.risultato != null && !stato.inCorso) {
                Text(
                    "L'anteprima non entra nel libro: il JSON conserva sempre il testo originale.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

// Un riquadro con titolo attorno a un gruppo di campi (02/08/2026,
// Michele: "sarebbe bene utilizzare dei box per un maggior focus sul
// testo"). Prima le sezioni della scheda erano separate solo da spazi
// vuoti e da un titolo: su una scena con combattimento e cinque scelte
// non si capiva dove finisse una cosa e cominciasse l'altra.
@Composable
fun SezioneScheda(
    titolo: String,
    modifier: Modifier = Modifier,
    azione: @Composable (() -> Unit)? = null,
    contenuto: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    titolo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                azione?.invoke()
            }
            contenuto()
        }
    }
}
