package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatStatus
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveCombatSkill
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveEndurance
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.ui.creation.disciplineIcon

// La scena teatrale in forma minima (Fase 3, "prima funziona poi è
// bello"): header con titolo e numero scena, testo ORIGINALE del
// pacchetto come pagina di libro, zona scelte (normali + discipline),
// combat rapido, card di stato coi valori effettivi dell'engine.
@Composable
fun AdventureScreen(
    state: AdventureState,
    onExitToHome: () -> Unit,
) {
    // Scheda personaggio come overlay dentro la route (stato condiviso;
    // diventerà una destinazione propria in Fase 5).
    var showSheet by remember { mutableStateOf(false) }
    if (showSheet) {
        io.github.luposolitario.immundanoctisex.ui.sheet.CharacterSheetScreen(
            hero = state.gameState.hero,
            onEquipWeapon = state::equipWeapon,
            onConsumeItem = state::consumeItem,
            onClose = { showSheet = false },
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Header(state)

        Card(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Text(
                text = state.currentScene.narrativeText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            )
        }

        StatusCard(state, onClick = { showSheet = true })
        Spacer(Modifier.height(8.dp))

        when {
            state.combatSession != null -> CombatActiveZone(state)
            state.currentScene.combat != null -> CombatEntryZone(state)
            state.isEnding -> EndingZone(state, onExitToHome)
            else -> ChoicesZone(state)
        }
    }
}

@Composable
private fun Header(state: AdventureState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(state.bookTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Scena ${state.currentScene.id}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Il tocco apre la Scheda personaggio (UI.md §Card di stato).
@Composable
private fun StatusCard(state: AdventureState, onClick: () -> Unit) {
    val hero = state.gameState.hero
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(hero.name, fontWeight = FontWeight.Bold)
            Text("CS ${effectiveCombatSkill(hero)}")
            Text("RES ${effectiveEndurance(hero)}/${hero.maxEndurance}")
            Text("${Inventory.countOf(hero, "Gold Crowns")} Corone")
        }
    }
}

@Composable
private fun ChoicesZone(state: AdventureState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        state.availableChoices.forEach { choice ->
            Button(onClick = { state.takeChoice(choice) }, modifier = Modifier.fillMaxWidth()) {
                Text(choice.choiceText)
            }
        }
        state.availableDisciplineChoices.forEach { choice ->
            OutlinedCard(
                onClick = { state.useDiscipline(choice) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = disciplineIcon(choice.disciplineId),
                        contentDescription = choice.disciplineId,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(choice.choiceText, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EndingZone(state: AdventureState, onExitToHome: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.adventureDeleted) {
            Text(
                "Morte in IRON: la sessione è stata cancellata.",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
        }
        Button(onClick = onExitToHome, modifier = Modifier.fillMaxWidth()) {
            Text("Torna alla Home")
        }
    }
}
