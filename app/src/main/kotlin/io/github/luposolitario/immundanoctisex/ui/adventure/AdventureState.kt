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
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.model.Transition
import io.github.luposolitario.immundanoctisex.core.data.session.SessionStore
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatSession
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatStatus
import io.github.luposolitario.immundanoctisex.core.engine.combat.RoundResult
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

    // Luogo corrente APPICCICOSO (UI.md): la scena che non dichiara
    // locationName eredita quello precedente. Alla ripresa riparte
    // dall'ultima voce del diario.
    private var currentLocation: String? =
        sceneById(session.currentSceneId).locationName
            ?: session.journey.lastOrNull()?.locationName
    var combatSession: CombatSession? by mutableStateOf(null)
        private set
    var adventureDeleted: Boolean by mutableStateOf(false)
        private set

    val isEnding: Boolean get() = currentScene.sceneType == SceneType.ENDING

    // Gating delle scelte (pattern v1, UI.md §Zona scelte): condizioni non
    // soddisfatte = scelta non mostrata. Le scelte con tiro arriveranno
    // col flusso del Dado (il sample non ne ha).
    // requiredFlag è un NOME di flag: soddisfatto se il flag è stato posto
    // a un valore diverso da "false" (un autore che scrive value="false"
    // intende negare la condizione, non soddisfarla).
    val availableChoices: List<Choice>
        get() = currentScene.choices.filter { choice ->
            val flagOk = choice.requiredFlag?.let { gameState.flag(it)?.equals("false", ignoreCase = true) == false } ?: true
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

    // CombatSession è una classe dell'engine (niente Compose): la UI si
    // riaggancia ai suoi cambiamenti osservando questo contatore, che ogni
    // azione di combattimento incrementa.
    var combatTick: Int by mutableStateOf(0)
        private set
    var lastRound: RoundResult? by mutableStateOf(null)
        private set

    // Modalità RAPIDA (REGOLE.md §1.1): un tocco, il motore va fino in fondo.
    fun startQuickCombat() {
        val combat = currentScene.combat ?: return
        combatSession = CombatSession(gameState.hero, combat, dice).also { it.quickResolve() }
        lastRound = null
    }

    // Modalità COMPLETA: round per round col menu tattico.
    fun startCompleteCombat() {
        val combat = currentScene.combat ?: return
        combatSession = CombatSession(gameState.hero, combat, dice)
        lastRound = null
    }

    fun combatFightRound() {
        lastRound = combatSession?.fightRound()
        combatTick++
    }

    fun combatActivateMindblast() {
        combatSession?.activateMindblast()
        combatTick++
    }

    fun combatUseItem(itemName: String) {
        combatSession?.useItem(itemName)
        combatTick++
    }

    fun combatEvade() {
        lastRound = combatSession?.evade()
        combatTick++
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

    // Checkpoint (STATO.md Blocco 2): budget per difficoltà, piazzati dal
    // giocatore, scritti una volta e mai sovrascrivibili.
    val checkpointBudget: Int
        get() = when (gameState.session.difficulty) {
            Difficulty.NORMAL -> 2
            Difficulty.HARD -> 1
            Difficulty.IRON -> 0
        }

    val checkpointsRemaining: Int
        get() = (checkpointBudget - gameState.session.checkpointsUsed).coerceAtLeast(0)

    fun placeCheckpoint(): Boolean {
        if (checkpointsRemaining <= 0) return false
        val slot = gameState.session.checkpointsUsed + 1
        val snapshot = gameState.snapshot().copy(lastUpdate = System.currentTimeMillis())
        if (!store.saveCheckpoint(snapshot, slot)) return false
        gameState.incrementCheckpointsUsed()
        autoSave()
        return true
    }

    // Gli slot piazzati e ricaricabili (alla morte, fuori da IRON).
    fun placedCheckpoints(): List<Int> =
        (1..checkpointBudget).filter { store.loadCheckpoint(manifest.id, it) != null }

    fun loadCheckpoint(slot: Int): SessionData? =
        store.loadCheckpoint(manifest.id, slot)

    val isDeathEnding: Boolean
        get() = isEnding && currentScene.id == manifest.deathSceneId

    // Azioni della Scheda personaggio (UI.md §Inventario operativo): ogni
    // modifica passa dall'engine e viene auto-salvata.
    fun equipWeapon(itemName: String) {
        gameState.updateHero { Inventory.equipWeapon(it, itemName) }
        autoSave()
    }

    fun unequipWeapon() {
        gameState.updateHero { Inventory.unequipWeapon(it) }
        autoSave()
    }

    // Consuma un oggetto con effetto dichiarato (v0.1: solo HEAL:n).
    fun consumeItem(itemName: String) {
        val item = gameState.hero.inventory.firstOrNull {
            it.name.equals(itemName, ignoreCase = true) && it.quantity > 0
        } ?: return
        val heal = item.effect?.takeIf { it.startsWith("HEAL:") }
            ?.substringAfter("HEAL:")?.toIntOrNull() ?: return
        gameState.updateHero { hero ->
            Inventory.removeItem(hero, item.name, 1).let {
                it.copy(currentEndurance = (it.currentEndurance + heal).coerceIn(0, it.maxEndurance))
            }
        }
        autoSave()
    }

    private fun moveTo(targetSceneId: String, transition: Transition) {
        gameState.addJourneyEntry(
            JourneyEntry(currentScene.id, currentScene.narrativeText, transition, currentLocation),
        )
        val result = engine.transitionTo(gameState, targetSceneId)
        // Anche i salti d'ufficio sono porte del diario-grafo (col luogo
        // risolto della scena da cui si salta).
        result.autoJumps.forEach { hop ->
            val hopScene = sceneById(hop.fromSceneId)
            currentLocation = hopScene.locationName ?: currentLocation
            gameState.addJourneyEntry(
                JourneyEntry(hop.fromSceneId, hopScene.narrativeText, Transition.AutoJump(hop.reason), currentLocation),
            )
        }
        currentScene = sceneById(result.sceneId)
        currentLocation = currentScene.locationName ?: currentLocation
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
