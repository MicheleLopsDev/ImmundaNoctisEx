package io.github.luposolitario.immundanoctisex.core.engine.transition

import io.github.luposolitario.immundanoctisex.core.data.model.AutoJumpReason
import io.github.luposolitario.immundanoctisex.core.data.model.ComparisonOperator
import io.github.luposolitario.immundanoctisex.core.data.model.GlobalRule
import io.github.luposolitario.immundanoctisex.core.data.model.GlobalRuleType
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.engine.mechanics.MechanicsExecutor
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveEndurance
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveMaxEndurance

// Un salto d'ufficio avvenuto durante la transizione: serve all'app per
// scrivere le voci del diario-grafo (Transition.AutoJump).
data class AutoJumpHop(
    val fromSceneId: String,
    val toSceneId: String,
    val reason: AutoJumpReason,
)

// mealEaten (22/07/2026): risale dal gameMechanics requireAction EAT_MEAL
// fino a :app, per il suono del pasto obbligatorio — dichiarato
// dall'autore nel JSON, mai generato da Gemma.
data class TransitionResult(
    val sceneId: String,
    val autoJumps: List<AutoJumpHop> = emptyList(),
    val mealEaten: Boolean = false,
)

// La pipeline di transizione (REGOLE.md §2.3): arrivo in scena -> HEALING
// passiva -> gameMechanics in ordine -> morte built-in -> globalRules in
// ordine (prima che matcha vince) -> se qualcosa è scattato, si ripete il
// giro sulla nuova scena. Il combattimento resta fuori: i suoi esiti sono
// del Blocco 1 e passano di qui solo come destinazione già decisa.
class TransitionEngine(
    private val manifest: Manifest,
    private val executor: MechanicsExecutor,
) {

    // Rete di sicurezza contro i cicli di regole scritti male: oltre questa
    // soglia la pipeline si ferma dov'è (il gioco non si blocca mai).
    private val maxHops = 20

    fun transitionTo(state: GameState, targetSceneId: String): TransitionResult {
        val hops = mutableListOf<AutoJumpHop>()
        var currentTarget = targetSceneId
        // Accumulato su TUTTI gli hop del giro (non solo l'ultimo): un
        // salto d'ufficio può attraversare più scene prima di fermarsi,
        // e il pasto può essere stato mangiato in una qualunque di esse.
        var mealEaten = false

        repeat(maxHops) {
            val scene = manifest.scenes.firstOrNull { it.id == currentTarget }
                ?: return TransitionResult(state.currentSceneId, hops, mealEaten) // grafo rotto: si resta dov'eravamo
            state.moveTo(scene.id)

            applyPassiveHealing(state, scene)
            val mechanicsOutcome = executor.execute(state, scene.gameMechanics)
            if (mechanicsOutcome.mealEaten) mealEaten = true

            val nextJump = builtInDeath(state, scene)
                ?: mechanicsOutcome.jumpTo?.let { AutoJumpHop(scene.id, it, mechanicsOutcome.jumpReason!!) }
                ?: firstMatchingGlobalRule(state, scene)

            if (nextJump == null) return TransitionResult(scene.id, hops, mealEaten)
            hops += nextJump
            currentTarget = nextJump.toSceneId
        }
        return TransitionResult(state.currentSceneId, hops, mealEaten)
    }

    // HEALING (REGOLE.md §4.3): +1 Resistenza a ogni transizione verso una
    // scena SENZA combattimento, fino al massimo. La applica il motore,
    // non è un'azione.
    private fun applyPassiveHealing(state: GameState, scene: Scene) {
        if (scene.combat != null) return
        if (!state.hero.kaiDisciplines.contains("HEALING")) return
        state.updateHero { hero ->
            hero.copy(currentEndurance = (hero.currentEndurance + 1).coerceAtMost(effectiveMaxEndurance(hero)))
        }
    }

    // Morte built-in (REGOLE.md §2.1): si valuta PRIMA delle globalRules
    // (morire batte vincere). Senza deathSceneId nel manifest la regola
    // non è attiva. Sulla scena di morte non si rivaluta (niente loop).
    private fun builtInDeath(state: GameState, scene: Scene): AutoJumpHop? {
        val deathSceneId = manifest.deathSceneId ?: return null
        if (scene.id == deathSceneId) return null
        if (effectiveEndurance(state.hero) > 0) return null
        return AutoJumpHop(scene.id, deathSceneId, AutoJumpReason.BUILT_IN_DEATH)
    }

    private fun firstMatchingGlobalRule(state: GameState, scene: Scene): AutoJumpHop? {
        val rule = manifest.globalRules.firstOrNull { matches(state, it) } ?: return null
        if (rule.targetSceneId == scene.id) return null // già a destinazione: niente loop
        return AutoJumpHop(scene.id, rule.targetSceneId, AutoJumpReason.GLOBAL_RULE)
    }

    private fun matches(state: GameState, rule: GlobalRule): Boolean = when (rule.type) {
        GlobalRuleType.VAR -> {
            val value = rule.value.toIntOrNull()
            value != null && compare(state.variable(rule.name), rule.operator, value)
        }
        GlobalRuleType.FLAG -> {
            val current = state.flag(rule.name)
            when (rule.operator) {
                ComparisonOperator.EQ -> current == rule.value
                ComparisonOperator.NEQ -> current != rule.value
                // Operatori d'ordine su un FLAG: sensati solo se entrambi i
                // lati sono numerici, altrimenti la regola non scatta.
                else -> {
                    val left = current?.toIntOrNull()
                    val right = rule.value.toIntOrNull()
                    left != null && right != null && compare(left, rule.operator, right)
                }
            }
        }
    }

    private fun compare(left: Int, operator: ComparisonOperator, right: Int): Boolean = when (operator) {
        ComparisonOperator.EQ -> left == right
        ComparisonOperator.NEQ -> left != right
        ComparisonOperator.GTE -> left >= right
        ComparisonOperator.LTE -> left <= right
        ComparisonOperator.GT -> left > right
        ComparisonOperator.LT -> left < right
    }
}
