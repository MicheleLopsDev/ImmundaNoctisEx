package io.github.luposolitario.immundanoctisex.ui.options

import androidx.compose.foundation.isSystemInDarkTheme
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
import io.github.luposolitario.immundanoctisex.util.AccentColor

// Colore d'accento (UI.md schermata 7, richiesta Michele 21/07/2026,
// vista la card di stato: "voglio una selezione dei colori"). Swatch
// cliccabili col colore VERO, non un elenco di nomi — è una scelta
// visiva, si sceglie guardando, non leggendo. Il colore mostrato è
// quello del tema attivo (scuro o chiaro): sono diversi apposta, per
// restare leggibili sul rispettivo sfondo.
@Composable
fun AccentColorSection(selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    val dark = isSystemInDarkTheme()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Colore d'accento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentColor.entries.forEach { accent ->
                    val swatchColor = if (dark) accent.darkPrimary else accent.lightPrimary
                    ColorSwatch(
                        color = swatchColor,
                        name = accent.displayName,
                        isSelected = accent == selected,
                        onClick = { onSelect(accent) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Colore d'accento")
@Composable
private fun AccentColorSectionPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        AccentColorSection(selected = AccentColor.BLUE, onSelect = {})
    }
}
