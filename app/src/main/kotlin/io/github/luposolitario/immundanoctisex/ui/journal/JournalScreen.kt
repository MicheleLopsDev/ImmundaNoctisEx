package io.github.luposolitario.immundanoctisex.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.data.model.CombatOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.JourneyEntry
import io.github.luposolitario.immundanoctisex.core.data.model.Transition
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// Diario del viaggio (UI.md §schermata 6), due viste: Racconto (rilettura
// voce per voce) e Mappa logica (i luoghi nell'ordine del viaggio, v0.1
// solo il nome). Stateless: voci in ingresso, export in uscita.
@Composable
fun JournalScreen(
    journey: List<JourneyEntry>,
    onExport: () -> Unit,
    onClose: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Racconto") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Mappa logica") })
        }
        Spacer(Modifier.height(12.dp))

        if (tab == 0) StoryView(journey, Modifier.weight(1f)) else MapView(journey, Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                Text("Esporta (Markdown)")
            }
            Button(onClick = onClose, modifier = Modifier.weight(1f)) {
                Text("Chiudi")
            }
        }
    }
}

@Composable
private fun StoryView(journey: List<JourneyEntry>, modifier: Modifier) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(journey) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Scena ${entry.sceneId}", fontWeight = FontWeight.Bold)
                        entry.locationName?.let {
                            Text(it, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    Text(entry.enrichedText, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        transitionText(entry.transition),
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

// Mappa logica v0.1: le voci raggruppate per luogo consecutivo, in ordine
// di viaggio (derivata dal diario-grafo, mai salvata).
@Composable
private fun MapView(journey: List<JourneyEntry>, modifier: Modifier) {
    val stops = buildList {
        journey.forEach { entry ->
            val location = entry.locationName ?: return@forEach
            if (lastOrNull() != location) add(location)
        }
    }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(stops.withIndex().toList()) { (index, location) ->
            Row {
                Text("${index + 1}.", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(0.dp))
                Text("  $location")
            }
        }
    }
}

private fun transitionText(transition: Transition): String = when (transition) {
    is Transition.ChoiceTaken -> "→ hai scelto (${transition.choiceId})"
    is Transition.DisciplineUsed -> "→ hai usato ${transition.disciplineId}"
    is Transition.CombatResolved -> when (transition.outcome) {
        CombatOutcome.WIN -> "→ combattimento VINTO"
        CombatOutcome.LOSE -> "→ combattimento PERSO"
        CombatOutcome.EVADE -> "→ sei fuggito dal combattimento"
    }
    is Transition.AutoJump -> "→ il destino ha deciso (${transition.reason})"
}

// Il diario è già un generatore di racconto (STATO.md Blocco 3).
fun journeyToMarkdown(bookTitle: String, journey: List<JourneyEntry>): String = buildString {
    appendLine("# $bookTitle — Diario del viaggio")
    appendLine()
    journey.forEach { entry ->
        append("## Scena ${entry.sceneId}")
        entry.locationName?.let { append(" — $it") }
        appendLine()
        appendLine()
        appendLine(entry.enrichedText)
        appendLine()
        appendLine("*${transitionText(entry.transition)}*")
        appendLine()
    }
}

@Preview(showBackground = true, name = "Diario (scuro)", heightDp = 700)
@Composable
private fun JournalPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        JournalScreen(
            journey = listOf(
                JourneyEntry("1", "La lettera ti aspetta sul tavolo della locanda.", Transition.ChoiceTaken("c1"), "Riverside Inn"),
                JourneyEntry("2", "Le strade del porto brulicano di marinai.", Transition.ChoiceTaken("c2"), "Harbour Town"),
                JourneyEntry("3", "Il vicolo si stringe verso il magazzino.", Transition.DisciplineUsed("SIXTH_SENSE", "d1"), "Old Quarter"),
            ),
            onExport = {},
            onClose = {},
        )
    }
}
