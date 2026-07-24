package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.StatModifier
import io.github.luposolitario.immundanoctisex.core.data.model.StatType
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatSession
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatStatus
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveCombatSkill
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveMaxEndurance
import io.github.luposolitario.immundanoctisex.ui.creation.disciplineName
import io.github.luposolitario.immundanoctisex.ui.creation.heroIconRes
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctisex.util.DiceColor

// Il Diario di Combattimento cartaceo di Lupo Solitario (richiesta Michele
// 20/07/2026, riferimento fotografato dal registro ufficiale): non un
// popup per round, un PANNELLO che resta aperto per l'intero
// combattimento completo e si aggiorna a ogni colpo — RES/CS di entrambi,
// Rapporto di Forza al centro, il dado a 10 facce come innesco del tiro.
// Sostituisce le barre di Resistenza e il testo del round di prima; il
// menu tattico (oggetto/disciplina/fuga) resta un blocco separato sotto.
//
// SENZA sfondo (24/07/2026, richiesta Michele: "dalla card del combat
// rimuovi lo sfondo... intendo nessuna pergamena e basta") — tolti sia
// il Card Material3 di prima (pergamena OFF) sia la pila a tre fasce di
// ParchmentBackground (pergamena attiva): il contenuto sta direttamente
// sullo schermo, niente riquadro dietro. `ParchmentBackground.kt` non ha
// più nessun altro punto d'uso, cancellato.
@Composable
fun CombatDiaryPanel(state: AdventureState, session: CombatSession, diceColor: DiceColor = DiceColor.GRAY) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Il Diario di Combattimento cartaceo lo mostra sempre in testa
        // (Michele 22/07/2026, foto del registro): un dato che avevamo
        // già (currentScene.id), solo mai portato dentro al combattimento.
        Text(
            "Paragrafo ${state.currentScene.id}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CombatantColumn(
                name = "Tu",
                character = session.player,
                maxEndurance = effectiveMaxEndurance(session.player),
                alignEnd = false,
                iconRes = heroIconRes(session.player.icon),
            )
            CenterColumn(state, session, diceColor)
            CombatantColumn(
                name = session.enemy.name,
                character = session.enemy,
                maxEndurance = session.enemy.maxEndurance,
                alignEnd = true,
                // Stessa illustrazione già mostrata sopra (EnemyPortrait
                // in CombatZone.kt) se l'autore l'ha dichiarata; altrimenti
                // l'icona generica ic_enemy_placeholder (24/07/2026,
                // richiesta Michele: "al posto del nome del nemico...
                // questa è di default se non viene indicata nel json") —
                // qui, a differenza del grande EnemyPortrait, un'icona
                // c'è SEMPRE, mai solo testo nudo.
                iconRes = enemyImageRes(state.currentScene.combat?.enemyImage)
                    ?: R.drawable.ic_enemy_placeholder,
            )
        }
        if (session.status != CombatStatus.ONGOING) {
            Spacer(Modifier.height(12.dp))
            CombatOutcome(state, session)
        }
    }
}

@Composable
private fun CombatantColumn(
    name: String,
    character: Character,
    maxEndurance: Int,
    alignEnd: Boolean,
    // Icona del PG al posto della sola scritta (24/07/2026, richiesta
    // Michele): l'animale scelto in creazione per l'eroe, l'illustrazione
    // del nemico (Combat.enemyImage) per l'avversario. Null = niente
    // icona, resta solo il testo come prima — mai un segnaposto rotto.
    iconRes: Int? = null,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        iconRes?.let { res ->
            Image(
                painter = painterResource(id = res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(36.dp).clip(CircleShape),
            )
            Spacer(Modifier.height(2.dp))
        }
        Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text("RES ${character.currentEndurance}/$maxEndurance", style = MaterialTheme.typography.bodyMedium)
        // Barra RES verde per te, rossa per il nemico (24/07/2026,
        // richiesta Michele): stesso dato del testo sopra, solo più
        // leggibile a colpo d'occhio durante lo scambio di colpi.
        LinearProgressIndicator(
            progress = { (character.currentEndurance.toFloat() / maxEndurance.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.width(72.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = if (alignEnd) Color(0xFFE53935) else Color(0xFF4CAF50),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
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

// internal: riusata anche dalla scheda personaggio per scomporre
// Combattività in base + modificatori (richiesta Michele 22/07/2026,
// dal registro cartaceo: "BASE" + caselle "MODIFICATORI").
@Composable
internal fun modifierLabel(modifier: StatModifier): String {
    val sign = if (modifier.amount >= 0) "+" else ""
    val stat = if (modifier.stat == StatType.COMBAT_SKILL) "CS" else "RES"
    val name = disciplineName(modifier.sourceType)?.let { stringResource(it) } ?: modifier.sourceType
    return "$sign${modifier.amount} $stat ($name)"
}

@Composable
private fun CenterColumn(state: AdventureState, session: CombatSession, diceColor: DiceColor) {
    val ratio = effectiveCombatSkill(session.player) - effectiveCombatSkill(session.enemy)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Il Dado del Destino in cima, tra le icone di eroe e nemico
        // (24/07/2026, richiesta Michele: "il dado del destino al centro
        // in alto tra le icone" — prima stava sotto il Rapporto di Forza).
        if (session.status == CombatStatus.ONGOING) {
            // fightRound() è SINCRONO: se combatFightRound() mutasse lo
            // stato al TOCCO invece che a fine animazione, RES e CS
            // cambierebbero mentre il dado sta ancora girando. TenSidedDie
            // chiama onRoll solo a fine giro apposta per questo.
            TenSidedDie(
                onRoll = { state.combatFightRound()?.roll },
                onTap = { state.playDiceRollSound() },
                initialFace = state.lastRound?.roll,
                diceColor = diceColor,
            )
            Spacer(Modifier.height(8.dp))
        }
        // Icona 3D colorata al posto della scritta "Rapporto di Forza"
        // (24/07/2026, stessa richiesta: "scompare il testo... è
        // abbastanza intuitiva la grafica" — verde/giallo/rosso secondo
        // quanto è squilibrato lo scontro, il segno dice chi è più forte,
        // il numero piccolo sotto per chi vuole il dato esatto).
        ForceRatioIcon(ratio, modifier = Modifier.size(40.dp))
        Text(
            if (ratio >= 0) "+$ratio" else "$ratio",
            style = MaterialTheme.typography.labelSmall,
        )
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
