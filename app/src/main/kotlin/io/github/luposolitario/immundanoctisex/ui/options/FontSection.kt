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
import io.github.luposolitario.immundanoctisex.util.ReadingFont

// Scelta del font per il testo di lettura (UI.md schermata 7, richiesta
// Michele 17/07/2026): un'anteprima nel font stesso, non solo il nome —
// così si vede davvero la differenza prima di scegliere.
@Composable
fun FontSection(selected: ReadingFont, onSelect: (ReadingFont) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Font di lettura", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ReadingFont.entries.forEach { font ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = font == selected, onClick = { onSelect(font) })
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = font == selected, onClick = { onSelect(font) })
                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            font.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Il narratore racconta la storia",
                            fontFamily = font.family,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Font di lettura")
@Composable
private fun FontSectionPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        FontSection(selected = ReadingFont.SERIF, onSelect = {})
    }
}
