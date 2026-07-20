package io.github.luposolitario.immundanoctisex.ui.options

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctisex.util.StatusCardColor

// Sfondo della card di stato (richiesta Michele 21/07/2026, dopo il
// colore d'accento: "un altro picker per la barra di sotto" — questa
// card, non la navigation bar di sistema, chiarito con lo screenshot).
// DEFAULT ha uno swatch col colore di superficie del tema attivo, per
// far vedere "cosa succede se non tocco nulla" accanto alle alternative.
@Composable
fun StatusCardColorSection(selected: StatusCardColor, onSelect: (StatusCardColor) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sfondo della card di stato", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusCardColor.entries.forEach { option ->
                    val swatchColor = option.background ?: MaterialTheme.colorScheme.surfaceVariant
                    ColorSwatch(
                        color = swatchColor,
                        name = option.displayName,
                        isSelected = option == selected,
                        onClick = { onSelect(option) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Sfondo card di stato")
@Composable
private fun StatusCardColorSectionPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        StatusCardColorSection(selected = StatusCardColor.DEFAULT, onSelect = {})
    }
}
