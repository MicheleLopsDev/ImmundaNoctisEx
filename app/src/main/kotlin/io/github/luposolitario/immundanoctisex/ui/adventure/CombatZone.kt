package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatResultsTable
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatSession
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatStatus
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveCombatSkill
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveMaxEndurance

// La zona scelte trasformata in quadro di combattimento (UI.md §Il
// combattimento nella scena): nessuna schermata separata. v0.1 senza
// estetica: barre Resistenza, Rapporto di Forza, menu tattico.

// Prima del combattimento: fughe gratis via disciplina, poi la scelta di
// modalità (REGOLE.md §1.1: evasione/oggetti/discipline solo nel completo).
@Composable
fun CombatEntryZone(state: AdventureState) {
    val combat = requireNotNull(state.currentScene.combat)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "${combat.enemyName} — CS ${combat.enemyCombatSkill}, RES ${combat.enemyEndurance}",
            fontWeight = FontWeight.Bold,
        )
        state.availableDisciplineChoices.forEach { choice ->
            OutlinedButton(onClick = { state.useDiscipline(choice) }, modifier = Modifier.fillMaxWidth()) {
                Text(choice.choiceText, fontStyle = FontStyle.Italic)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { state.startQuickCombat() }, modifier = Modifier.weight(1f)) {
                Text("Rapido")
            }
            Button(onClick = { state.startCompleteCombat() }, modifier = Modifier.weight(1f)) {
                Text("Completo")
            }
        }
    }
}

// Combattimento in corso o concluso: quadro + menu tattico / riepilogo.
@Composable
fun CombatActiveZone(state: AdventureState) {
    state.combatTick // sottoscrive gli aggiornamenti della sessione engine
    val session = requireNotNull(state.combatSession)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        EnduranceBars(session)

        state.lastRound?.let { round ->
            val playerLoss = if (round.playerLoss == CombatResultsTable.KILL_DAMAGE) "MORTE" else "-${round.playerLoss}"
            val enemyLoss = if (round.enemyLoss == CombatResultsTable.KILL_DAMAGE) "MORTE" else "-${round.enemyLoss}"
            Text(
                "Tiro ${round.roll} (rapporto ${round.combatRatio}): tu $playerLoss, nemico $enemyLoss",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (session.status == CombatStatus.ONGOING) {
            TacticalMenu(state, session)
        } else {
            CombatSummary(state, session)
        }
    }
}

@Composable
private fun EnduranceBars(session: CombatSession) {
    val ratio = effectiveCombatSkill(session.player) - effectiveCombatSkill(session.enemy)
    Text("Rapporto di Forza: $ratio", fontWeight = FontWeight.Bold)
    Text("Tu: ${session.player.currentEndurance}/${effectiveMaxEndurance(session.player)}")
    LinearProgressIndicator(
        progress = { session.player.currentEndurance.toFloat() / effectiveMaxEndurance(session.player) },
        modifier = Modifier.fillMaxWidth(),
    )
    Text("${session.enemy.name}: ${session.enemy.currentEndurance}/${session.enemy.maxEndurance}")
    LinearProgressIndicator(
        progress = { session.enemy.currentEndurance.toFloat() / session.enemy.maxEndurance },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.error,
    )
}

// Menu tattico (REGOLE.md §1.1): continua / oggetto / disciplina / fuga.
@Composable
private fun TacticalMenu(state: AdventureState, session: CombatSession) {
    val combat = requireNotNull(state.currentScene.combat)

    Button(onClick = { state.combatFightRound() }, modifier = Modifier.fillMaxWidth()) {
        Text("Combatti il round (tira il dado)")
    }

    val hasMindblast = session.player.kaiDisciplines.contains("MINDBLAST")
    if (hasMindblast) {
        // Immunità mostrata col motivo (UI.md: disabilitata, non nascosta).
        OutlinedButton(
            onClick = { state.combatActivateMindblast() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !combat.immuneToMindblast && !session.mindblastActive,
        ) {
            Text(
                when {
                    combat.immuneToMindblast -> "MINDBLAST — il nemico è immune"
                    session.mindblastActive -> "MINDBLAST attivo (+2 CS)"
                    else -> "Usa MINDBLAST (+2 CS)"
                },
            )
        }
    }

    session.player.inventory.filter { it.combatUsable && it.quantity > 0 }.forEach { item ->
        OutlinedButton(onClick = { state.combatUseItem(item.name) }, modifier = Modifier.fillMaxWidth()) {
            Text("Usa ${item.name} (${item.effect ?: ""})")
        }
    }

    if (combat.evadeSceneId != null) {
        OutlinedButton(
            onClick = { state.combatEvade() },
            modifier = Modifier.fillMaxWidth(),
            enabled = session.canEvade,
        ) {
            Text(
                if (session.canEvade) "Fuggi (subisci un ultimo round di danni)"
                else "Fuga disponibile dopo il round ${combat.evadeAfterRound}",
            )
        }
    }
}

@Composable
private fun CombatSummary(state: AdventureState, session: CombatSession) {
    val outcomeText = when (session.status) {
        CombatStatus.WIN -> "VITTORIA in ${session.roundsFought} round!"
        CombatStatus.LOSE -> "SCONFITTA dopo ${session.roundsFought} round."
        else -> "Sei fuggito dopo ${session.roundsFought} round."
    }
    Text(outcomeText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    Button(onClick = { state.resolveCombat() }, modifier = Modifier.fillMaxWidth()) {
        Text("Continua")
    }
}
