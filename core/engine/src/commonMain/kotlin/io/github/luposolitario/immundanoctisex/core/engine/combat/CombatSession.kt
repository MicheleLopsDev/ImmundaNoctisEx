package io.github.luposolitario.immundanoctisex.core.engine.combat

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.StatModifier
import io.github.luposolitario.immundanoctisex.core.data.model.StatType
import io.github.luposolitario.immundanoctisex.core.engine.dice.DiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveCombatSkill

enum class CombatStatus { ONGOING, WIN, LOSE, EVADED }

// Esito di un round: dati puri, il testo lo compone la UI (REGOLE.md §1.2).
data class RoundResult(
    val roll: Int,
    val combatRatio: Int,
    val playerLoss: Int,
    val enemyLoss: Int,
)

// Il combattimento è ATOMICO (STATO.md §1.3): questa sessione vive in
// memoria e non si salva mai a metà; a fine combattimento il chiamante
// riporta playerAfterCombat nel GameState. Macchina a stati del completo:
// tra un round e l'altro il menu tattico chiama fightRound / useItem /
// activateMindblast / evade; il rapido è quickResolve, un loop sullo
// stesso round (nessuna logica duplicata, REGOLE.md §1.1).
class CombatSession(
    initialPlayer: Character,
    private val combat: Combat,
    private val dice: DiceRoller,
) {

    // Il nemico minimale del JSON è idratato in un Character unico
    // (REGOLE.md §1.5): la funzione di round è simmetrica per costruzione.
    var player: Character = initialPlayer
        private set
    var enemy: Character = Character(
        role = CharacterRole.ENEMY,
        name = combat.enemyName,
        baseCombatSkill = combat.enemyCombatSkill,
        currentEndurance = combat.enemyEndurance,
        maxEndurance = combat.enemyEndurance,
    )
        private set

    var roundsFought: Int = 0
        private set
    var status: CombatStatus = CombatStatus.ONGOING
        private set
    var mindblastActive: Boolean = false
        private set

    // Fuga: solo se la scena la offre e il round di sblocco è passato
    // (REGOLE.md §1.3, evadeAfterRound default 0 = subito).
    val canEvade: Boolean
        get() = status == CombatStatus.ONGOING &&
            combat.evadeSceneId != null &&
            roundsFought >= combat.evadeAfterRound

    // Round pieno: entrambi subiscono i danni della CRT.
    fun fightRound(): RoundResult {
        val result = lookupRound()
        applyPlayerLoss(result.playerLoss)
        applyEnemyLoss(result.enemyLoss)
        roundsFought++
        updateStatusAfterRound()
        return result
    }

    // Evasione con costo canonico (REGOLE.md §1.3): un ultimo round in cui
    // SOLO il giocatore subisce danni; se sopravvive, EVADED. Il danno
    // dell'evasione può uccidere: in quel caso è una sconfitta.
    fun evade(): RoundResult? {
        if (!canEvade) return null
        val result = lookupRound()
        applyPlayerLoss(result.playerLoss)
        roundsFought++
        status = if (player.currentEndurance <= 0) CombatStatus.LOSE else CombatStatus.EVADED
        return result.copy(enemyLoss = 0)
    }

    // MINDBLAST (REGOLE.md §4.1): +2 CS per tutto il combattimento, una
    // sola attivazione, negato dai nemici immuni.
    fun activateMindblast(): Boolean {
        if (status != CombatStatus.ONGOING || mindblastActive || combat.immuneToMindblast) return false
        if (!player.kaiDisciplines.contains("MINDBLAST")) return false
        mindblastActive = true
        player = player.copy(
            activeModifiers = player.activeModifiers +
                StatModifier(StatType.COMBAT_SKILL, 2, sourceType = MINDBLAST_SOURCE),
        )
        return true
    }

    // Oggetti nel menu tattico (REGOLE.md §4.4): solo combatUsable, v0.1
    // implementa il solo effetto HEAL:n. L'oggetto si consuma.
    fun useItem(itemName: String): Boolean {
        if (status != CombatStatus.ONGOING) return false
        val item = player.inventory.firstOrNull {
            it.combatUsable && it.name.equals(itemName, ignoreCase = true) && it.quantity > 0
        } ?: return false
        val healAmount = item.effect
            ?.takeIf { it.startsWith("HEAL:") }
            ?.substringAfter("HEAL:")?.toIntOrNull()
            ?: return false
        player = Inventory.removeItem(player, item.name, 1).let { healed ->
            healed.copy(currentEndurance = (healed.currentEndurance + healAmount).coerceIn(0, healed.maxEndurance))
        }
        return true
    }

    // Modalità rapida (REGOLE.md §1.1): loop dello stesso round fino
    // all'esito, nessuna tattica. Restituisce la cronaca per il riepilogo.
    fun quickResolve(): List<RoundResult> {
        val chronicle = mutableListOf<RoundResult>()
        while (status == CombatStatus.ONGOING) {
            chronicle += fightRound()
        }
        return chronicle
    }

    // Il giocatore da riportare nel GameState a fine combattimento: i fatti
    // (Resistenza, oggetti consumati) restano, i modificatori di solo
    // combattimento (MINDBLAST) decadono.
    val playerAfterCombat: Character
        get() = player.copy(activeModifiers = player.activeModifiers.filterNot { it.sourceType == MINDBLAST_SOURCE })

    // Destinazione per lo status corrente; loseSceneId assente -> null e il
    // chiamante degrada sul deathSceneId globale (lo specifico batte il
    // globale, REGOLE.md §1.4).
    val destinationSceneId: String?
        get() = when (status) {
            CombatStatus.WIN -> combat.winSceneId
            CombatStatus.LOSE -> combat.loseSceneId
            CombatStatus.EVADED -> combat.evadeSceneId
            CombatStatus.ONGOING -> null
        }

    private fun lookupRound(): RoundResult {
        val ratio = effectiveCombatSkill(player) - effectiveCombatSkill(enemy)
        val roll = dice.roll()
        val damage = CombatResultsTable.damageFor(ratio, roll)
        return RoundResult(
            roll = roll,
            combatRatio = ratio,
            playerLoss = damage.playerLoss,
            enemyLoss = damage.enemyLoss,
        )
    }

    private fun applyPlayerLoss(loss: Int) {
        val endurance = if (loss == CombatResultsTable.KILL_DAMAGE) 0 else player.currentEndurance - loss
        player = player.copy(currentEndurance = endurance.coerceAtLeast(0))
    }

    private fun applyEnemyLoss(loss: Int) {
        val endurance = if (loss == CombatResultsTable.KILL_DAMAGE) 0 else enemy.currentEndurance - loss
        enemy = enemy.copy(currentEndurance = endurance.coerceAtLeast(0))
    }

    // Priorità degli esiti (REGOLE.md §1.4): la morte del giocatore batte
    // quella del nemico se cadono nello stesso round.
    private fun updateStatusAfterRound() {
        status = when {
            player.currentEndurance <= 0 -> CombatStatus.LOSE
            enemy.currentEndurance <= 0 -> CombatStatus.WIN
            else -> CombatStatus.ONGOING
        }
    }

    private companion object {
        const val MINDBLAST_SOURCE = "MINDBLAST"
    }
}
