package io.github.luposolitario.immundanoctisex.core.engine.mechanics

import io.github.luposolitario.immundanoctisex.core.data.model.StatModifier
import io.github.luposolitario.immundanoctisex.core.data.model.StatType
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.inventory.MealRules
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveCombatSkill
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveEndurance
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveMaxEndurance
import kotlinx.serialization.json.JsonObject

// Comandi di scena sulle statistiche. Convenzione di Ex (si serializzano i
// fatti): le variazioni di ENDURANCE toccano direttamente currentEndurance
// (danno/cura sono fatti), le variazioni di COMBAT_SKILL diventano
// StatModifier narrativi che la funzione di stat effettive somma.
internal object StatMechanics {

    // ID canonici delle statistiche nei dati (CLAUDE.md): niente alias.
    private const val STAT_ENDURANCE = "ENDURANCE"
    private const val STAT_COMBAT_SKILL = "COMBAT_SKILL"

    fun healStat(state: GameState, params: JsonObject) {
        if (params.stringParam("statName") != STAT_ENDURANCE) return
        val amount = params.stringParam("amount") ?: return
        state.updateHero { hero ->
            val cap = effectiveMaxEndurance(hero)
            val healed = if (amount == "FULL") cap
            else hero.currentEndurance + (amount.toIntOrNull() ?: 0)
            hero.copy(currentEndurance = healed.coerceIn(0, cap))
        }
    }

    fun applyStatModifier(state: GameState, params: JsonObject) {
        val amount = params.intParam("amount") ?: return
        when (params.stringParam("statName")) {
            STAT_ENDURANCE -> state.updateHero { hero ->
                hero.copy(currentEndurance = (hero.currentEndurance + amount).coerceIn(0, effectiveMaxEndurance(hero)))
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
    //
    // Mangiare cura +1 Resistenza (Michele 22/07/2026): bilancia i pasti
    // trovati in più rispetto alle richieste EAT_MEAL del libro — se non
    // servono a evitare la penalità, almeno curano un poco. HUNTING non
    // consuma un pasto vero (auto-soddisfa a costo zero) e infatti esce
    // PRIMA di questo blocco: caccia gratis, non guadagna la cura.
    //
    // Ritorna true se ha consumato un pasto per davvero (Michele, stesso
    // giorno: "EAT_MEAL lo possiamo mettere nel JSON" — è già lì, scritto
    // dall'autore, non generato da Gemma: il fatto "si è mangiato" può
    // risalire fino a :app per far partire il suono, senza serializzare
    // altro che un booleano — REGOLE.md, si serializzano i fatti).
    fun requireAction(state: GameState, params: JsonObject): Boolean {
        if (params.stringParam("action") != "EAT_MEAL") return false
        val hero = state.hero
        if (hero.kaiDisciplines.contains("HUNTING")) return false
        if (Inventory.countOf(hero, MealRules.ITEM_NAME) > 0) {
            state.updateHero { h ->
                val afterMeal = Inventory.removeItem(h, MealRules.ITEM_NAME, 1)
                val cap = effectiveMaxEndurance(afterMeal)
                afterMeal.copy(currentEndurance = (afterMeal.currentEndurance + MealRules.HEAL_AMOUNT).coerceIn(0, cap))
            }
            return true
        }
        val penaltyStat = params["penaltyStat"] ?: return false
        val penaltyAmount = params["penaltyValue"] ?: return false
        applyStatModifier(state, JsonObject(mapOf("statName" to penaltyStat, "amount" to penaltyAmount)))
        return false
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
