package io.github.luposolitario.immundanoctisex.core.engine.choice

import io.github.luposolitario.immundanoctisex.core.data.model.ComparisonOperator
import io.github.luposolitario.immundanoctisex.core.data.model.RollCondition
import io.github.luposolitario.immundanoctisex.core.data.model.RollConditionType
import io.github.luposolitario.immundanoctisex.core.data.model.RollModifier
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveEndurance

// Quanto va sommato al tiro della Tabella dei Numeri Casuali prima di
// cercare l'intervallo che lo copre (REGOLE.md Blocco 6, schema in
// core:data/RollModifier.kt). Sono REGOLE, quindi vivono qui accanto a
// ChoiceAvailability e non nel modulo :app — stesso principio, stesso
// package, testabili da terminale.
//
// ChoiceAvailability.forRoll NON cambia: continua a ricevere un numero e
// a cercarne l'intervallo. È il chiamante che le passa il totale già
// modificato — così resta una funzione pura sul solo dato della scena.
object RollModifiers {

    // Somma dei modificatori applicabili adesso. Zero se la scena non ne
    // dichiara (il caso di ogni libro esistente): il tiro grezzo decide
    // da solo, esattamente come prima che questo campo esistesse.
    fun totalFor(scene: Scene, state: GameState): Int =
        activeFor(scene, state).sumOf { it.amount }

    // Quali modificatori sono scattati davvero: serve alla UI per
    // spiegare il numero al giocatore ("5 + 2 = 7") invece di mostrargli
    // un totale che non torna col dado che ha appena visto.
    fun activeFor(scene: Scene, state: GameState): List<RollModifier> =
        scene.rollModifiers.filter { modifier ->
            modifier.condition?.let { matches(it, state) } ?: true
        }

    // Vocabolario chiuso: una condizione che non si sa valutare è FALSA,
    // mai un'eccezione — un pacchetto scritto male non blocca il gioco
    // (stessa promessa di ChoiceAvailability.forRoll).
    private fun matches(condition: RollCondition, state: GameState): Boolean =
        when (condition.type) {
            // In OR: "the Kai Discipline of either Mind Over Matter or
            // Mindblast" — ne basta una. Stesso test di
            // ChoiceAvailability.disciplineChoices.
            RollConditionType.DISCIPLINE ->
                condition.values.any { state.hero.kaiDisciplines.contains(it) }

            RollConditionType.ITEM ->
                condition.values.any { Inventory.countOf(state.hero, it) > 0 }

            // Stessa semantica di Choice.requiredFlag in
            // ChoiceAvailability.available(): un flag posto a "false"
            // NEGA la condizione, un flag mai posto non la soddisfa.
            RollConditionType.FLAG ->
                condition.values.any { state.flag(it)?.equals("false", ignoreCase = true) == false }

            // effectiveEndurance, non currentEndurance: conta il valore
            // VERO in gioco, modificatori attivi inclusi (stessa scelta
            // di StatMechanics.checkStatAndJump).
            RollConditionType.ENDURANCE -> {
                val operator = condition.operator
                val threshold = condition.threshold
                if (operator == null || threshold == null) false
                else compare(effectiveEndurance(state.hero), operator, threshold)
            }
        }

    private fun compare(left: Int, operator: ComparisonOperator, right: Int): Boolean =
        when (operator) {
            ComparisonOperator.EQ -> left == right
            ComparisonOperator.NEQ -> left != right
            ComparisonOperator.GTE -> left >= right
            ComparisonOperator.LTE -> left <= right
            ComparisonOperator.GT -> left > right
            ComparisonOperator.LT -> left < right
        }
}
