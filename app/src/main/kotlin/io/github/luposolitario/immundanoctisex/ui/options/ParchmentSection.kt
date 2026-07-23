package io.github.luposolitario.immundanoctisex.ui.options

import androidx.compose.foundation.layout.Arrangement
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
import io.github.luposolitario.immundanoctisex.util.ParchmentStyle

// Stile pergamena del Diario di Combattimento (22/07/2026, richiesta
// Michele: "potremmo far scegliere nelle opzioni?"). OFF di default:
// chi non la vuole resta sul Material3 piatto di oggi.
@Composable
fun ParchmentSection(selected: ParchmentStyle, onSelect: (ParchmentStyle) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Stile del Diario di Combattimento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ParchmentStyle.entries.forEach { style ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = style == selected, onClick = { onSelect(style) })
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = style == selected, onClick = { onSelect(style) })
                    Text(style.displayName, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Stile pergamena")
@Composable
private fun ParchmentSectionPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        ParchmentSection(selected = ParchmentStyle.OFF, onSelect = {})
    }
}
