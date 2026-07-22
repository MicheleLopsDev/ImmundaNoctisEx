package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.StatModifier
import io.github.luposolitario.immundanoctisex.core.data.model.StatType
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatSession
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatStatus
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveCombatSkill
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveMaxEndurance
import io.github.luposolitario.immundanoctisex.ui.creation.disciplineName
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// Il Diario di Combattimento cartaceo di Lupo Solitario (richiesta Michele
// 20/07/2026, riferimento fotografato dal registro ufficiale): non un
// popup per round, un PANNELLO che resta aperto per l'intero
// combattimento completo e si aggiorna a ogni colpo — RES/CS di entrambi,
// Rapporto di Forza al centro, il dado a 10 facce come innesco del tiro.
// Sostituisce le barre di Resistenza e il testo del round di prima; il
// menu tattico (oggetto/disciplina/fuga) resta un blocco separato sotto.
@Composable
fun CombatDiaryPanel(state: AdventureState, session: CombatSession) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CombatantColumn(
                    name = "Tu",
                    character = session.player,
                    maxEndurance = effectiveMaxEndurance(session.player),
                    alignEnd = false,
                )
                CenterColumn(state, session)
                CombatantColumn(
                    name = session.enemy.name,
                    character = session.enemy,
                    maxEndurance = session.enemy.maxEndurance,
                    alignEnd = true,
                )
            }
            if (session.status != CombatStatus.ONGOING) {
                Spacer(Modifier.height(12.dp))
                CombatOutcome(state, session)
            }
        }
    }
}

@Composable
private fun CombatantColumn(name: String, character: Character, maxEndurance: Int, alignEnd: Boolean) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text("RES ${character.currentEndurance}/$maxEndurance", style = MaterialTheme.typography.bodyMedium)
        Text("CS ${effectiveCombatSkill(character)}", style = MaterialTheme.typography.bodyMedium)
        // I modificatori attivi (MINDBLAST, ecc.): il fatto sta nel
        // personaggio, qui si mostra solo. Nome localizzato dove esiste
        // (le discipline Kai ce l'hanno già), altrimenti l'ID grezzo.
        character.activeModifiers.forEach { modifier ->
            Text(
                modifierLabel(modifier),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
private fun modifierLabel(modifier: StatModifier): String {
    val sign = if (modifier.amount >= 0) "+" else ""
    val stat = if (modifier.stat == StatType.COMBAT_SKILL) "CS" else "RES"
    val name = disciplineName(modifier.sourceType)?.let { stringResource(it) } ?: modifier.sourceType
    return "$sign${modifier.amount} $stat ($name)"
}

@Composable
private fun CenterColumn(state: AdventureState, session: CombatSession) {
    val ratio = effectiveCombatSkill(session.player) - effectiveCombatSkill(session.enemy)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Rapporto", style = MaterialTheme.typography.labelSmall)
        Text(
            if (ratio >= 0) "+$ratio" else "$ratio",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        if (session.status == CombatStatus.ONGOING) {
            // fightRound() è SINCRONO: se combatFightRound() mutasse lo
            // stato al TOCCO invece che a fine animazione, RES e CS
            // cambierebbero mentre il dado sta ancora girando. TenSidedDie
            // chiama onRoll solo a fine giro apposta per questo.
            TenSidedDie(
                onRoll = { state.combatFightRound()?.roll },
                onTap = { state.playDiceRollSound() },
                initialFace = state.lastRound?.roll,
            )
        }
    }
}

// L'esito resta DENTRO lo stesso pannello (Michele 20/07/2026: "resta
// aperto fino alla fine") invece di saltare a una schermata diversa:
// i numeri sopra restano visibili, congelati sull'ultimo round.
@Composable
private fun CombatOutcome(state: AdventureState, session: CombatSession) {
    val text = when (session.status) {
        CombatStatus.WIN -> "VITTORIA in ${session.roundsFought} round!"
        CombatStatus.LOSE -> "SCONFITTA dopo ${session.roundsFought} round."
        else -> "Sei fuggito dopo ${session.roundsFought} round."
    }
    Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    androidx.compose.material3.Button(
        onClick = { state.resolveCombat() },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Continua")
    }
}

@Preview(showBackground = true, name = "Diario di combattimento")
@Composable
private fun CombatDiaryPanelPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        val player = Character(
            role = CharacterRole.HERO,
            name = "Lupo Solitario",
            baseCombatSkill = 18,
            currentEndurance = 17,
            maxEndurance = 25,
            activeModifiers = listOf(StatModifier(StatType.COMBAT_SKILL, 2, sourceType = "MINDBLAST")),
        )
        Column(Modifier.padding(12.dp)) {
            CombatantColumn("Tu", player, 25, alignEnd = false)
        }
    }
}
