package io.github.luposolitario.immundanoctisex.ui.options

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctisex.util.NarrativeTone

// Tono della narrazione (UI.md schermata 7, richiesta Michele
// 21/07/2026): "Come l'autore" lascia tutto com'è oggi (default),
// le altre voci forzano il tono per tutta la sessione, scena dopo
// scena — non un tocco temporaneo, dura finché non si cambia di nuovo.
@Composable
fun ToneSection(selected: NarrativeTone, onSelect: (NarrativeTone) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Tono della narrazione", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            NarrativeTone.entries.forEach { tone ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = tone == selected, onClick = { onSelect(tone) })
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = tone == selected, onClick = { onSelect(tone) })
                    Text(tone.displayName, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Tono della narrazione")
@Composable
private fun ToneSectionPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        ToneSection(selected = NarrativeTone.AUTHOR, onSelect = {})
    }
}
