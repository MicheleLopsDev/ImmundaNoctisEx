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

        StatusCard(state)
        Spacer(Modifier.height(8.dp))

        when {
            state.combatSession != null -> CombatSummaryZone(state)
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

@Composable
private fun StatusCard(state: AdventureState) {
    val hero = state.gameState.hero
    Card(modifier = Modifier.fillMaxWidth()) {
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
private fun CombatEntryZone(state: AdventureState) {
    val combat = requireNotNull(state.currentScene.combat)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "${combat.enemyName} — CS ${combat.enemyCombatSkill}, RES ${combat.enemyEndurance}",
            fontWeight = FontWeight.Bold,
        )
        // Fuga gratis via disciplina: offerta PRIMA del combattimento.
        state.availableDisciplineChoices.forEach { choice ->
            OutlinedCard(onClick = { state.useDiscipline(choice) }, modifier = Modifier.fillMaxWidth()) {
                Text(choice.choiceText, Modifier.padding(12.dp), fontStyle = FontStyle.Italic)
            }
        }
        Button(onClick = { state.startQuickCombat() }, modifier = Modifier.fillMaxWidth()) {
            Text("Combatti (rapido)")
        }
    }
}

@Composable
private fun CombatSummaryZone(state: AdventureState) {
    val session = requireNotNull(state.combatSession)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val outcomeText = when (session.status) {
            CombatStatus.WIN -> "VITTORIA in ${session.roundsFought} round!"
            CombatStatus.LOSE -> "SCONFITTA dopo ${session.roundsFought} round."
            else -> "Fuga riuscita."
        }
        Text(outcomeText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text("La tua Resistenza: ${session.player.currentEndurance} — ${session.enemy.name}: ${session.enemy.currentEndurance}")
        Button(onClick = { state.resolveCombat() }, modifier = Modifier.fillMaxWidth()) {
            Text("Continua")
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
