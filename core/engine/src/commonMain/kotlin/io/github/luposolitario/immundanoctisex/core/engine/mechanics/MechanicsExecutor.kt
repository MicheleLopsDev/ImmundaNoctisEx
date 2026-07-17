package io.github.luposolitario.immundanoctisex.core.engine.mechanics

import io.github.luposolitario.immundanoctisex.core.data.model.AutoJumpReason
import io.github.luposolitario.immundanoctisex.core.data.model.GameMechanic
import io.github.luposolitario.immundanoctisex.core.engine.dice.DiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

// Esito dell'esecuzione dei gameMechanics di una scena: se un comando di
// salto è scattato, la pipeline di transizione deve andare lì.
data class MechanicsOutcome(
    val jumpTo: String? = null,
    val jumpReason: AutoJumpReason? = null,
)

// Esegue i comandi di scena in ordine di scrittura (REGOLE.md §5.2). Il
// primo salto che scatta interrompe l'esecuzione dei comandi successivi
// (salto immediato). Comando sconosciuto o malformato: nessun effetto,
// il gioco non si blocca mai.
class MechanicsExecutor(private val dice: DiceRoller) {

    fun execute(state: GameState, mechanics: List<GameMechanic>): MechanicsOutcome {
        mechanics.forEach { mechanic ->
            val outcome = executeSingle(state, mechanic)
            if (outcome.jumpTo != null) return outcome
        }
        return MechanicsOutcome()
    }

    private fun executeSingle(state: GameState, mechanic: GameMechanic): MechanicsOutcome {
        val params = mechanic.params
        when (mechanic.command) {
            "addItem" -> ItemMechanics.addItem(state, params)
            "removeItem" -> ItemMechanics.removeItem(state, params)
            "removeAllItems" -> ItemMechanics.removeAllItems(state, params)
            "rollForQuantity" -> ItemMechanics.rollForQuantity(state, params, dice)
            "rollOnItemTable" -> ItemMechanics.rollOnItemTable(state, params, dice)
            "healStat" -> StatMechanics.healStat(state, params)
            "applyStatModifier" -> StatMechanics.applyStatModifier(state, params)
            "requireAction" -> StatMechanics.requireAction(state, params)
            "setFlag" -> setFlag(state, params)
            "setGlobalVar" -> setGlobalVar(state, params)
            "updateGlobalVar" -> updateGlobalVar(state, params)
            "checkItemAndJump" ->
                return jump(ItemMechanics.checkItemAndJump(state, params), AutoJumpReason.CHECK_ITEM_AND_JUMP)
            "checkStatAndJump" ->
                return jump(StatMechanics.checkStatAndJump(state, params), AutoJumpReason.IF_STAT)
            "handleRandomChoice" ->
                return jump(rollAgainstOutcomes(state, params, applyDisciplineBonus = false), AutoJumpReason.RANDOM_CHOICE)
            "handleSkillCheck" ->
                return jump(rollAgainstOutcomes(state, params, applyDisciplineBonus = true), AutoJumpReason.SKILL_CHECK)
            "handleConditionalAction" -> return conditionalAction(state, params)
        }
        return MechanicsOutcome()
    }

    // skillCheck e randomChoiceTable (REGOLE.md Blocco 6, tira il
    // giocatore): tiro 0-9, per lo skillCheck +modifier se l'eroe possiede
    // la disciplina dichiarata; l'outcome sceglie la scena. Nessun outcome
    // per il tiro: nessun salto (degradazione).
    private fun rollAgainstOutcomes(state: GameState, params: JsonObject, applyDisciplineBonus: Boolean): String? {
        var roll = dice.roll()
        if (applyDisciplineBonus) {
            val discipline = params.stringParam("discipline")
            if (discipline != null && state.hero.kaiDisciplines.contains(discipline)) {
                roll += params.intParam("modifier") ?: 0
            }
        }
        return params.outcomesParam().matchRoll(roll)?.stringParam("nextSceneId")
    }

    // conditionalAction: condizione su possesso oggetto o disciplina;
    // se vera, esegue il comando annidato in params.action (oggetto
    // {command, params}, non stringa-tag come in v1: qui i dati sono già
    // strutturati).
    private fun conditionalAction(state: GameState, params: JsonObject): MechanicsOutcome {
        val met = when (params.stringParam("condition")) {
            "HAS_ITEM" -> hasItem(state, params)
            "NOT_HAS_ITEM" -> !hasItem(state, params)
            "HAS_DISCIPLINE" -> hasDiscipline(state, params)
            "NOT_HAS_DISCIPLINE" -> !hasDiscipline(state, params)
            else -> false
        }
        if (!met) return MechanicsOutcome()
        val action = runCatching { params["action"]?.jsonObject }.getOrNull() ?: return MechanicsOutcome()
        val command = action.stringParam("command") ?: return MechanicsOutcome()
        val nestedParams = runCatching { action["params"]?.jsonObject }.getOrNull() ?: JsonObject(emptyMap())
        return executeSingle(state, GameMechanic(command = command, params = nestedParams))
    }

    private fun hasItem(state: GameState, params: JsonObject): Boolean {
        val name = params.stringParam("itemName") ?: return false
        return Inventory.countOf(state.hero, name) > 0
    }

    private fun hasDiscipline(state: GameState, params: JsonObject): Boolean {
        val id = params.stringParam("disciplineName") ?: return false
        return state.hero.kaiDisciplines.contains(id)
    }

    private fun setFlag(state: GameState, params: JsonObject) {
        val name = params.stringParam("flagName") ?: return
        val value = params.stringParam("value") ?: return
        state.setFlag(name, value)
    }

    // setGlobalVar: valore numerico -> variables tipizzate, altrimenti flag
    // testuale (variables è Map<String, Int>: niente Any, STATO.md §1.1).
    private fun setGlobalVar(state: GameState, params: JsonObject) {
        if (params.stringParam("operation") != "SET") return
        val name = params.stringParam("varName") ?: return
        val raw = params.stringParam("value") ?: return
        val numeric = raw.toIntOrNull()
        if (numeric != null) state.setVariable(name, numeric) else state.setFlag(name, raw)
    }

    private fun updateGlobalVar(state: GameState, params: JsonObject) {
        if (params.stringParam("operation") != "ADD") return
        val name = params.stringParam("varName") ?: return
        val delta = params.intParam("value") ?: return
        state.updateVariable(name, delta)
    }

    private fun jump(target: String?, reason: AutoJumpReason): MechanicsOutcome =
        if (target != null) MechanicsOutcome(jumpTo = target, jumpReason = reason) else MechanicsOutcome()
}
