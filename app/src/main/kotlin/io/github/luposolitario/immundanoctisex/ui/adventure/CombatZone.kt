package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatSession
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatStatus
import io.github.luposolitario.immundanoctisex.util.ParchmentStyle

// La zona scelte trasformata in quadro di combattimento (UI.md §Il
// combattimento nella scena): nessuna schermata separata. Il quadro vero
// e proprio (Diario di Combattimento + dado) vive in CombatDiaryPanel.

// Prima del combattimento: fughe gratis via disciplina, poi la scelta di
// modalità (REGOLE.md §1.1: evasione/oggetti/discipline solo nel completo).
@Composable
fun CombatEntryZone(state: AdventureState) {
    val combat = requireNotNull(state.currentScene.combat)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        EnemyPortrait(combat.enemyImage)
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
fun CombatActiveZone(
    state: AdventureState,
    parchmentStyle: ParchmentStyle = ParchmentStyle.OFF,
    isDarkTheme: Boolean = false,
) {
    val session = requireNotNull(state.combatSession)
    val combat = requireNotNull(state.currentScene.combat)

    // BUG (Michele 21/07/2026, "click su MINDBLAST/sul dado e i RES non
    // cambiano"): CombatSession è una classe pura dell'engine (niente
    // Compose per vincolo architetturale) — le sue `var` interne mutano
    // davvero (fightRound/activateMindblast funzionano, i dati sono
    // giusti), ma Compose non ha modo di saperlo: il riferimento a
    // `session` non cambia mai, solo i suoi campi. La sola LETTURA di
    // combatTick in questo punto non bastava a garantire la ricomposizione
    // (il gruppo poteva restare skippato). key() forza la ricreazione
    // completa del blocco ad ogni azione di combattimento.
    key(state.combatTick) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Resta visibile per tutto lo scontro (Michele 22/07/2026:
            // "quando combatte puoi lasciare sotto l'immagine del
            // nemico?") — prima spariva non appena si sceglieva Rapido/
            // Completo, perché la mostrava solo CombatEntryZone.
            EnemyPortrait(combat.enemyImage)
            CombatDiaryPanel(state, session, parchmentStyle, isDarkTheme)
            if (session.status == CombatStatus.ONGOING) {
                TacticalMenu(state, session)
            }
        }
    }
}

// Dichiarato dall'autore (Combat.enemyImage): stessa illustrazione a piena
// larghezza usata per Scene.npcImage — un ritratto piccolo "si perdeva"
// (Michele 22/07/2026). Se assente o non riconosciuto, niente immagine.
@Composable
private fun EnemyPortrait(enemyImage: String?) {
    enemyImageRes(enemyImage)?.let { res ->
        Image(
            painter = painterResource(id = res),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            // 120dp, stessa misura di Scene.npcImage (Michele 22/07/2026:
            // 100 -> 110 -> 120, "un altro 10% e ci siamo").
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
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
