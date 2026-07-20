package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatSession
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatStatus

// La zona scelte trasformata in quadro di combattimento (UI.md §Il
// combattimento nella scena): nessuna schermata separata. Il quadro vero
// e proprio (Diario di Combattimento + dado) vive in CombatDiaryPanel.

// Prima del combattimento: fughe gratis via disciplina, poi la scelta di
// modalità (REGOLE.md §1.1: evasione/oggetti/discipline solo nel completo).
@Composable
fun CombatEntryZone(state: AdventureState) {
    val combat = requireNotNull(state.currentScene.combat)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "${state.enemyName ?: combat.enemyName} — CS ${combat.enemyCombatSkill}, RES ${combat.enemyEndurance}",
            fontWeight = FontWeight.Bold,
        )
        state.availableDisciplineChoices.forEach { choice ->
            OutlinedButton(onClick = { state.useDiscipline(choice) }, modifier = Modifier.fillMaxWidth()) {
                Text(state.disciplineChoiceText(choice), fontStyle = FontStyle.Italic)
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

// Combattimento in corso o concluso: il Diario di Combattimento (richiesta
// Michele 20/07/2026) + menu tattico. Il pannello resta lo stesso dal
// primo colpo al riepilogo finale — mai un salto visivo a metà scontro.
@Composable
fun CombatActiveZone(state: AdventureState) {
    state.combatTick // sottoscrive gli aggiornamenti della sessione engine
    val session = requireNotNull(state.combatSession)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CombatDiaryPanel(state, session)
        if (session.status == CombatStatus.ONGOING) {
            TacticalMenu(state, session)
        }
    }
}

// Menu tattico (REGOLE.md §1.1): oggetto / disciplina / fuga. Il tiro del
// round non è più un bottone qui: è il tocco sul dado del Diario.
@Composable
private fun TacticalMenu(state: AdventureState, session: CombatSession) {
    val combat = requireNotNull(state.currentScene.combat)

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
