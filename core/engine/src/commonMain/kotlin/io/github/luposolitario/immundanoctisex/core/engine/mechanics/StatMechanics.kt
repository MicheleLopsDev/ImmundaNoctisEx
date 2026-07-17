package io.github.luposolitario.immundanoctisex.core.engine.mechanics

import io.github.luposolitario.immundanoctisex.core.data.model.StatModifier
import io.github.luposolitario.immundanoctisex.core.data.model.StatType
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveCombatSkill
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveEndurance
import kotlinx.serialization.json.JsonObject

// Comandi di scena sulle statistiche. Convenzione di Ex (si serializzano i
// fatti): le variazioni di ENDURANCE toccano direttamente currentEndurance
// (danno/cura sono fatti), le variazioni di COMBAT_SKILL diventano
// StatModifier narrativi che la funzione di stat effettive somma.
internal object StatMechanics {

    const val MEAL_ITEM_NAME = "Meal"

    // ID canonici delle statistiche nei dati (CLAUDE.md): niente alias.
    private const val STAT_ENDURANCE = "ENDURANCE"
    private const val STAT_COMBAT_SKILL = "COMBAT_SKILL"

    fun healStat(state: GameState, params: JsonObject) {
        if (params.stringParam("statName") != STAT_ENDURANCE) return
        val amount = params.stringParam("amount") ?: return
        state.updateHero { hero ->
            val healed = if (amount == "FULL") hero.maxEndurance
            else hero.currentEndurance + (amount.toIntOrNull() ?: 0)
            hero.copy(currentEndurance = healed.coerceIn(0, hero.maxEndurance))
        }
    }

    fun applyStatModifier(state: GameState, params: JsonObject) {
        val amount = params.intParam("amount") ?: return
        when (params.stringParam("statName")) {
            STAT_ENDURANCE -> state.updateHero { hero ->
                hero.copy(currentEndurance = (hero.currentEndurance + amount).coerceIn(0, hero.maxEndurance))
            }
            STAT_COMBAT_SKILL -> state.updateHero { hero ->
                hero.copy(
                    activeModifiers = hero.activeModifiers +
                        StatModifier(StatType.COMBAT_SKILL, amount, sourceType = "NARRATIVE"),
                )
            }
        }
    }

    // requireAction EAT_MEAL (STATO.md §4.4): HUNTING auto-soddisfa a costo
    // zero; altrimenti si consuma un Pasto; senza Pasto scatta la penalità
    // dichiarata dall'autore. Altre azioni: non gestite, degradano.
    fun requireAction(state: GameState, params: JsonObject) {
        if (params.stringParam("action") != "EAT_MEAL") return
        val hero = state.hero
        if (hero.kaiDisciplines.contains("HUNTING")) return
        if (Inventory.countOf(hero, MEAL_ITEM_NAME) > 0) {
            state.updateHero { Inventory.removeItem(it, MEAL_ITEM_NAME, 1) }
            return
        }
        val penaltyStat = params["penaltyStat"] ?: return
        val penaltyAmount = params["penaltyValue"] ?: return
        applyStatModifier(state, JsonObject(mapOf("statName" to penaltyStat, "amount" to penaltyAmount)))
    }

    // ifStat (REGOLE.md §5.2): statName canonico oppure nome di variabile di
    // sessione; condizione vera -> salto a targetScene.
    fun checkStatAndJump(state: GameState, params: JsonObject): String? {
        val statName = params.stringParam("statName") ?: return null
        val operator = params.stringParam("operator") ?: return null
        val value = params.intParam("value") ?: return null
        val current = when (statName) {
            STAT_ENDURANCE -> effectiveEndurance(state.hero)
            STAT_COMBAT_SKILL -> effectiveCombatSkill(state.hero)
            else -> state.variable(statName)
        }
        return if (compare(current, operator, value)) params.stringParam("targetScene") else null
    }
}
