package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.CombatOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.JourneyEntry
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.model.Transition
import io.github.luposolitario.immundanoctisex.core.data.session.SessionStore
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatSession
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatStatus
import io.github.luposolitario.immundanoctisex.core.engine.dice.DiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.mechanics.MechanicsExecutor
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import io.github.luposolitario.immundanoctisex.core.engine.transition.TransitionEngine

// Stato della schermata Avventura (v0.1 senza Gemma): cabla GameState,
// TransitionEngine e CombatSession; ogni transizione registra la voce del
// diario-grafo e fa l'auto-save ATOMICO (STATO.md §1.3). Il testo
// "arricchito" senza modello è il testo originale del pacchetto.
class AdventureState(
    private val manifest: Manifest,
    session: SessionData,
    private val dice: DiceRoller,
    private val store: SessionStore,
) {
    val gameState = GameState(session)
    val bookTitle: String get() = manifest.title
    private val engine = TransitionEngine(manifest, MechanicsExecutor(dice))

    var currentScene: Scene by mutableStateOf(sceneById(session.currentSceneId))
        private set
    var combatSession: CombatSession? by mutableStateOf(null)
        private set
    var adventureDeleted: Boolean by mutableStateOf(false)
        private set

    val isEnding: Boolean get() = currentScene.sceneType.name == "ENDING"

    // Gating delle scelte (pattern v1, UI.md §Zona scelte): condizioni non
    // soddisfatte = scelta non mostrata. Le scelte con tiro arriveranno
    // col flusso del Dado (il sample non ne ha).
    val availableChoices: List<Choice>
        get() = currentScene.choices.filter { choice ->
            val flagOk = choice.requiredFlag == null || gameState.flag(choice.requiredFlag!!) != null
            val itemOk = choice.requiredItem == null ||
                Inventory.countOf(gameState.hero, choice.requiredItem!!) > 0
            val noRoll = choice.minRoll == null && choice.maxRoll == null
            flagOk && itemOk && noRoll
        }

    val availableDisciplineChoices: List<DisciplineChoice>
        get() = currentScene.disciplineChoices.filter {
            gameState.hero.kaiDisciplines.contains(it.disciplineId)
        }

    fun takeChoice(choice: Choice) =
        moveTo(choice.nextSceneId, Transition.ChoiceTaken(choice.id))

    fun useDiscipline(choice: DisciplineChoice) =
        moveTo(choice.nextSceneId, Transition.DisciplineUsed(choice.disciplineId, choice.id))

    // Combattimento v0.1: solo modalità RAPIDA (il menu tattico completo è
    // il prossimo task). La sessione resta in memoria: combat atomico.
    fun startQuickCombat() {
        val combat = currentScene.combat ?: return
        combatSession = CombatSession(gameState.hero, combat, dice).also { it.quickResolve() }
    }

    fun resolveCombat() {
        val session = combatSession ?: return
        val outcome = when (session.status) {
            CombatStatus.WIN -> CombatOutcome.WIN
            CombatStatus.LOSE -> CombatOutcome.LOSE
            else -> CombatOutcome.EVADE
        }
        gameState.updateHero { session.playerAfterCombat }
        combatSession = null
        // Lo specifico batte il globale: senza loseSceneId si degrada sul
        // deathSceneId del manifest (REGOLE.md §1.4).
        val destination = session.destinationSceneId ?: manifest.deathSceneId ?: return
        moveTo(destination, Transition.CombatResolved(outcome))
    }

    private fun moveTo(targetSceneId: String, transition: Transition) {
        gameState.addJourneyEntry(
            JourneyEntry(currentScene.id, currentScene.narrativeText, transition),
        )
        val result = engine.transitionTo(gameState, targetSceneId)
        // Anche i salti d'ufficio sono porte del diario-grafo.
        result.autoJumps.forEach { hop ->
            gameState.addJourneyEntry(
                JourneyEntry(hop.fromSceneId, sceneById(hop.fromSceneId).narrativeText, Transition.AutoJump(hop.reason)),
            )
        }
        currentScene = sceneById(result.sceneId)
        autoSave()
        handleIronDeath()
    }

    private fun autoSave() {
        store.saveSession(gameState.snapshot().copy(lastUpdate = System.currentTimeMillis()))
    }

    // Morte in IRON: la sessione si cancella, il libro riparte da capo
    // (STATO.md Blocco 2). La schermata mostra comunque la scena di morte.
    private fun handleIronDeath() {
        if (!isEnding) return
        val isDeathScene = currentScene.id == manifest.deathSceneId
        if (isDeathScene && gameState.session.difficulty == Difficulty.IRON) {
            store.deleteAdventure(manifest.id)
            adventureDeleted = true
        }
    }

    private fun sceneById(id: String): Scene =
        manifest.scenes.first { it.id == id }
}
