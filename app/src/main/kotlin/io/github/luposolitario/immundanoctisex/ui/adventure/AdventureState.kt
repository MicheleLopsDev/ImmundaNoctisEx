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
import io.github.luposolitario.immundanoctisex.core.engine.choice.ChoiceAvailability
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatSession
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatStatus
import io.github.luposolitario.immundanoctisex.core.engine.combat.RoundResult
import io.github.luposolitario.immundanoctisex.core.engine.dice.DiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.mechanics.MechanicsExecutor
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import io.github.luposolitario.immundanoctisex.core.engine.transition.TransitionEngine
import io.github.luposolitario.immundanoctisex.inference.NarrationEvent
import io.github.luposolitario.immundanoctisex.inference.SceneNarrator
import io.github.luposolitario.immundanoctisex.inference.TokenInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Stato della schermata Avventura (v0.1 senza Gemma): cabla GameState,
// TransitionEngine e CombatSession; ogni transizione registra la voce del
// diario-grafo e fa l'auto-save ATOMICO (STATO.md §1.3). Il testo
// "arricchito" senza modello è il testo originale del pacchetto.
class AdventureState(
    private val manifest: Manifest,
    session: SessionData,
    private val dice: DiceRoller,
    private val store: SessionStore,
    // Narratore opzionale: se manca (modello non scaricato) il gioco
    // funziona esattamente come in Fase 3, col testo del pacchetto.
    private val narrator: SceneNarrator? = null,
    private val scope: CoroutineScope? = null,
    // Il modello e' sul telefono? Si sa SUBITO (basta il file), mentre
    // caricarlo richiede secondi: senza questo si vedrebbe il testo
    // inglese per tutta la durata del caricamento.
    private val expectsNarration: Boolean = false,
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

    // --- Narrazione ---
    // Il testo che la UI mostra: parte dall'originale del pacchetto e
    // viene sostituito da quello arricchito man mano che arriva. Così la
    // scena è leggibile fin dal primo istante, anche mentre Gemma pensa.
    var narrative: String by mutableStateOf(
        if (expectsNarration) "" else sceneById(session.currentSceneId).narrativeText,
    )
        private set
    // Vero gia' durante il CARICAMENTO del modello, non solo durante la
    // generazione: e' il periodo in cui non c'e' ancora nulla da leggere.
    var isGenerating: Boolean by mutableStateOf(expectsNarration)
        private set

    // Distingue i DUE momenti di attesa, che durano molto diversamente: il
    // caricamento del modello (una volta sola, secondi) e la generazione
    // della singola scena. La UI ci dice sopra due frasi diverse, cosi'
    // l'attesa lunga della prima volta non sembra un blocco.
    var isLoadingModel: Boolean by mutableStateOf(expectsNarration)
        private set

    // Testi delle scelte tradotti (id -> testo). Vuoto = si usa
    // l'originale del pacchetto.
    private var translatedChoices: Map<String, String> by mutableStateOf(emptyMap())
    private var translatedEnemyName: String? by mutableStateOf(null)

    fun choiceText(choice: Choice): String = translatedChoices[choice.id] ?: choice.choiceText

    fun disciplineChoiceText(choice: DisciplineChoice): String =
        translatedChoices[choice.id] ?: choice.choiceText

    val enemyName: String? get() = translatedEnemyName ?: currentScene.combat?.enemyName

    // Consumo del contesto, per il semaforo nell'header. Null quando
    // non c'e' narratore (si gioca col testo del pacchetto).
    val tokenInfo: TokenInfo? get() = narrator?.tokenInfo

    private var narrationJob: Job? = null

    // Avvia (o riavvia) la narrazione della scena corrente. Lo streaming
    // è BUFFERIZZATO: si aggiorna la UI al massimo ogni ~90ms, altrimenti
    // ogni token farebbe ricomporre l'intera schermata (CRITICITA.md).
    fun startNarration(previousSceneText: String?) {
        val narrator = this.narrator ?: return
        val scope = this.scope ?: return
        narrationJob?.cancel()
        translatedChoices = emptyMap()
        translatedEnemyName = null
        // Con il narratore pronto il testo originale NON si mostra: la
        // scena la scrive Gemma, e fino al primo pezzo si vede solo
        // l'indicatore "il narratore scrive" (richiesta Michele 19/07).
        // Senza motore resta il testo del pacchetto: il gioco non si
        // ferma mai davanti a una schermata vuota.
        narrative = if (expectsNarration || narrator.isReady) "" else currentScene.narrativeText
        isGenerating = true
        // Se si arriva qui il modello e' caricato: da qui in poi l'attesa
        // e' quella (breve) della generazione.
        isLoadingModel = false

        narrationJob = scope.launch {
            var lastUpdate = 0L
            narrator.narrate(
                scene = currentScene,
                previousSceneText = previousSceneText,
                choices = currentScene.choices,
                disciplineChoices = currentScene.disciplineChoices,
                playerGender = gameState.hero.gender,
            ).collect { event ->
                when (event) {
                    is NarrationEvent.Streaming -> {
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate >= STREAM_BUFFER_MS) {
                            lastUpdate = now
                            narrative = event.textSoFar
                        }
                    }
                    is NarrationEvent.Completed -> {
                        narrative = event.scene.narrative
                        translatedChoices = event.scene.choiceTexts + event.scene.disciplineChoiceTexts
                        translatedEnemyName = event.scene.enemyName
                        isGenerating = false
                    }
                }
            }
            isGenerating = false
        }
    }

    // Il motore non e' partito (modello mancante o inizializzazione
    // fallita): si torna al testo del pacchetto invece di lasciare
    // "il narratore scrive" per sempre.
    fun narrationUnavailable() {
        isGenerating = false
        isLoadingModel = false
        if (narrative.isBlank()) narrative = currentScene.narrativeText
    }

    val isEnding: Boolean get() = currentScene.sceneType == SceneType.ENDING

    // Quali porte sono aperte: le REGOLE stanno nell'engine
    // (ChoiceAvailability), qui si espone solo ciò che la UI disegna.
    val availableChoices: List<Choice>
        get() = ChoiceAvailability.available(currentScene, gameState)

    val availableDisciplineChoices: List<DisciplineChoice>
        get() = ChoiceAvailability.disciplineChoices(currentScene, gameState)

    // --- Tiro del Dado del Destino fuori dal combattimento ---
    // Le scelte con minRoll/maxRoll sono una tabella dei numeri casuali: il
    // giocatore non sceglie, TIRA (REGOLE.md Blocco 6). Flusso a due fasi
    // ereditato da v1 (arma -> tira -> risolvi) ma col trigger STRUTTURALE
    // (v1 fiutava il testo italiano della scena) e il DiceRoller iniettato.
    // v0.1: il tiro è un bottone; l'overlay animato arriva in Fase 7.
    val requiresRoll: Boolean
        get() = combatSession == null && ChoiceAvailability.rollChoices(currentScene).isNotEmpty()

    var lastChoiceRoll: Int? by mutableStateOf(null)
        private set

    fun rollForChoice() {
        if (!requiresRoll || lastChoiceRoll != null) return
        lastChoiceRoll = dice.roll()
    }

    // Risolve il tiro mostrato: la scelta il cui intervallo contiene il
    // numero. Nessun intervallo coperto (pacchetto scritto male): il tiro
    // si azzera e si riprova, il gioco non si blocca mai.
    fun resolveRolledChoice() {
        val roll = lastChoiceRoll ?: return
        val choice = ChoiceAvailability.forRoll(currentScene, roll)
        lastChoiceRoll = null
        if (choice != null) takeChoice(choice)
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
        lastChoiceRoll = null // ogni scena nuova riarma il dado (reset di v1)
        // Nel diario finisce il testo che il giocatore HA LETTO (quello
        // arricchito, se c'era): si salva e non si rigenera mai
        // (STATO.md Blocco 3).
        val textJustRead = narrative
        gameState.addJourneyEntry(
            JourneyEntry(currentScene.id, textJustRead, transition, currentLocation),
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
        // La scena nuova si racconta da sé; il contesto è la CODA della
        // precedente (mai il diario: inferenza senza memoria).
        startNarration(previousSceneText = textJustRead)
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

    private companion object {
        // Buffer dello streaming: la UI si aggiorna al massimo ogni tanto,
        // non a ogni token (CRITICITA.md ~80-100ms).
        const val STREAM_BUFFER_MS = 90L
    }
}
