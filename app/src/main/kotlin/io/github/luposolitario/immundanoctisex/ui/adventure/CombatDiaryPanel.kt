package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import io.github.luposolitario.immundanoctisex.util.ParchmentStyle
import io.github.luposolitario.immundanoctisex.util.inkColor
import io.github.luposolitario.immundanoctisex.util.resolved

// Il Diario di Combattimento cartaceo di Lupo Solitario (richiesta Michele
// 20/07/2026, riferimento fotografato dal registro ufficiale): non un
// popup per round, un PANNELLO che resta aperto per l'intero
// combattimento completo e si aggiorna a ogni colpo — RES/CS di entrambi,
// Rapporto di Forza al centro, il dado a 10 facce come innesco del tiro.
// Sostituisce le barre di Resistenza e il testo del round di prima; il
// menu tattico (oggetto/disciplina/fuga) resta un blocco separato sotto.
@Composable
fun CombatDiaryPanel(
    state: AdventureState,
    session: CombatSession,
    parchmentStyle: ParchmentStyle = ParchmentStyle.OFF,
    // Serve solo a risolvere AUTO (23/07/2026): quale pergamena scegliere
    // segue il tema EFFETTIVO dell'app (override incluso), non il solo
    // sistema — vedi ParchmentStyle.resolved().
    isDarkTheme: Boolean = false,
) {
    // Stile pergamena (22/07/2026, richiesta Michele, scelta in Opzioni;
    // 23/07/2026 AUTO; poi ancora 23/07/2026 la pila a tre fasce — vedi
    // ParchmentBackground.kt): OFF resta il Card Material3 di sempre.
    // Attivo, `ParchmentBackground` dipinge la pergamena SOTTO al
    // contenuto dentro lo stesso Box. Il colore del testo di default
    // diventa l'inchiostro giusto per LA VARIANTE RISOLTA
    // (CompositionLocalProvider, non serve toccare ogni singolo Text —
    // solo quelli con un colore ESPLICITO, es. i modificatori tertiary,
    // restano quello che erano).
    val resolvedStyle = parchmentStyle.resolved(isDarkTheme)
    val parchmentActive = resolvedStyle.topRes != null

    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(16.dp)) {
            // Il Diario di Combattimento cartaceo lo mostra sempre in testa
            // (Michele 22/07/2026, foto del registro): un dato che avevamo
            // già (currentScene.id), solo mai portato dentro al combattimento.
            Text(
                "Paragrafo ${state.currentScene.id}",
                style = MaterialTheme.typography.labelMedium,
                color = if (resolvedStyle == ParchmentStyle.OFF) MaterialTheme.colorScheme.onSurfaceVariant else resolvedStyle.inkColor(),
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
                CenterColumn(state, session)
                CombatantColumn(
                    name = session.enemy.name,
                    character = session.enemy,
                    maxEndurance = session.enemy.maxEndurance,
                    alignEnd = true,
                    // Stessa illustrazione già mostrata sopra (EnemyPortrait
                    // in CombatZone.kt) — null se l'autore non l'ha
                    // dichiarata, niente segnaposto rotto.
                    iconRes = enemyImageRes(state.currentScene.combat?.enemyImage),
                )
            }
            if (session.status != CombatStatus.ONGOING) {
                Spacer(Modifier.height(12.dp))
                CombatOutcome(state, session)
            }
        }
    }

    if (!parchmentActive) {
        Card { content() }
    } else {
        CompositionLocalProvider(LocalContentColor provides resolvedStyle.inkColor()) {
            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))) {
                ParchmentBackground(resolvedStyle)
                content()
            }
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
private fun CenterColumn(state: AdventureState, session: CombatSession) {
    val ratio = effectiveCombatSkill(session.player) - effectiveCombatSkill(session.enemy)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Rapporto\ndi Forza", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        // Riquadro invece di solo testo (Michele 22/07/2026, dal Diario di
        // Combattimento cartaceo: il Rapporto di Forza è l'unico numero
        // dentro un box vero, al centro tra le due colonne).
        Box(
            modifier = Modifier
                .border(1.5.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(6.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (ratio >= 0) "+$ratio" else "$ratio",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
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
