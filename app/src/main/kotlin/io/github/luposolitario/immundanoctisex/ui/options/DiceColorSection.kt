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
import io.github.luposolitario.immundanoctisex.util.DiceColor

// Colore del Dado del Destino (24/07/2026, richiesta Michele dopo aver
// tolto il lupo dalla faccia zero: "fai che nelle preferenze si può
// scegliere il colore del dado") — stesso pattern di AccentColorSection:
// swatch cliccabili col colore vero.
@Composable
fun DiceColorSection(selected: DiceColor, onSelect: (DiceColor) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Colore del dado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DiceColor.entries.forEach { dice ->
                    ColorSwatch(
                        color = dice.base,
                        name = dice.displayName,
                        isSelected = dice == selected,
                        onClick = { onSelect(dice) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Colore del dado")
@Composable
private fun DiceColorSectionPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        DiceColorSection(selected = DiceColor.GRAY, onSelect = {})
    }
}
