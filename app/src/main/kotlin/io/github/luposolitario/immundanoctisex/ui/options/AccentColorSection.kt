package io.github.luposolitario.immundanoctisex.ui.options

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

@Composable
private fun ColorSwatch(color: Color, name: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent),
                CircleShape,
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(20.dp),
            )
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
