package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.CombatOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.EndingOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
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
import io.github.luposolitario.immundanoctisex.core.engine.ending.AdventureEnding
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.inventory.ItemOffers
import io.github.luposolitario.immundanoctisex.core.engine.inventory.MealRules
import io.github.luposolitario.immundanoctisex.core.engine.mechanics.MechanicsExecutor
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import io.github.luposolitario.immundanoctisex.core.engine.transition.TransitionEngine
import io.github.luposolitario.immundanoctisex.inference.NarrationEvent
import io.github.luposolitario.immundanoctisex.inference.SceneNarrator
import io.github.luposolitario.immundanoctisex.inference.TokenInfo
import io.github.luposolitario.immundanoctisex.sfx.SoundEffectPlayer
import io.github.luposolitario.immundanoctisex.tts.TtsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

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
    // TTS opzionale (Tappa 2, 22/07/2026): se manca (motore non pronto)
    // il gioco resta esattamente come senza — solo testo, nessuna voce,
    // stesso trattamento già dato al narratore assente.
    private val ttsService: TtsService? = null,
    private val autoReadEnabled: Boolean = false,
    private val userLocale: Locale = Locale.getDefault(),
    // Effetto sonoro del tiro (22/07/2026, richiesta Michele): opzionale
    // come narratore e TTS, stesso trattamento — se manca, il gioco resta
    // silenzioso invece di rompersi.
    private val soundEffectPlayer: SoundEffectPlayer? = null,
) {
    val gameState = GameState(session)

    // COMPOSE NON VEDE GameState. `:core:engine` non dipende da Android
    // (vincolo di progetto), quindi `gameState.session` è un var normale:
    // modificarlo NON fa ridisegnare nulla. Prima di questo, piazzare un
    // checkpoint scriveva il file ma il contatore restava fermo, e bere
    // una pozione non cambiava la Resistenza a schermo finché non si
    // cambiava scena — sembrava tutto rotto mentre funzionava.
    //
    // Qui si tiene una COPIA osservabile di ciò che la UI disegna,
    // risincronizzata dopo ogni mutazione da `rinfresca()`.
    var hero: Character by mutableStateOf(gameState.hero)
        private set

    var checkpointsRemaining: Int by mutableStateOf(0)
        private set

    private fun rinfresca() {
        hero = gameState.hero
        checkpointsRemaining =
            (checkpointBudget - gameState.session.checkpointsUsed).coerceAtLeast(0)
    }

    init {
        rinfresca()
    }
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

    // Oggetti "sul banco" già presi in questa scena (Michele 21/07/2026,
    // §pick esplicito): stato COMPOSE-osservabile — i flag di GameState
    // non lo sono (stesso problema già risolto per CombatSession/
    // combatTick), qui serve la UI del pulsante "Preso" per aggiornarsi
    // subito. Ricostruito dai flag alla ripresa di un checkpoint, così un
    // oggetto già preso prima del salvataggio non ricompare.
    private var pickedItemNames: Set<String> by mutableStateOf(pickedNamesFor(currentScene))
        private set

    private fun pickedNamesFor(scene: Scene): Set<String> =
        ItemOffers.offeredItems(scene)
            .map { it.name }
            .filter { gameState.flag(pickedFlagKey(scene.id, it)) != null }
            .toSet()

    private fun pickedFlagKey(sceneId: String, itemName: String) = "picked_item_${sceneId}_$itemName"

    // Quelli ancora disponibili — la UI ne fa un pulsante "Prendi" per
    // ciascuno.
    val availableItems: List<GameItem>
        get() = ItemOffers.offeredItems(currentScene).filterNot { it.name in pickedItemNames }

    // Per disabilitare il pulsante col motivo giusto PRIMA del tocco, non
    // scoprirlo da un click che silenziosamente non fa nulla.
    fun canPickItem(item: GameItem): Boolean = Inventory.canAdd(hero, item)

    fun pickItem(item: GameItem) {
        if (item.name in pickedItemNames || !canPickItem(item)) return
        gameState.updateHero { Inventory.addItem(it, item) }
        gameState.setFlag(pickedFlagKey(currentScene.id, item.name), "true")
        pickedItemNames = pickedItemNames + item.name
        autoSave()
    }

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

    // Terzo valore dello stato del narratore unificato (UI.md: IDLE /
    // GENERATING / SPEAKING) — il cerchio d'oro nel banner si accende
    // anche qui, non solo mentre Gemma scrive.
    var isSpeaking: Boolean by mutableStateOf(false)
        private set

    init {
        ttsService?.onSpeakingStarted = { isSpeaking = true }
        ttsService?.onSpeakingFinished = { isSpeaking = false }
    }

    // Icona "leggi" manuale (UI.md: attiva solo se l'auto-lettura è
    // spenta) e trigger dell'auto-lettura stessa. Se il TTS non è pronto
    // TtsService.speak degrada da sé (log, nessun effetto) — qui non
    // serve un'altra guardia.
    fun readAloud() {
        ttsService?.speak(narrative, hero.gender, userLocale)
    }

    // Il tocco sul dado del destino in combattimento (Michele 22/07/2026:
    // "quando premi il dado si sente questo suono").
    fun playDiceRollSound() {
        soundEffectPlayer?.playDiceRoll()
    }

    // Testi delle scelte tradotti (id -> testo). Vuoto = si usa
    // l'originale del pacchetto.
    private var translatedChoices: Map<String, String> by mutableStateOf(emptyMap())
    private var translatedEnemyName: String? by mutableStateOf(null)
    // Nome dell'immagine di sfondo (SceneImageCatalog), non un drawable:
    // la UI risolve il nome in risorsa. Arriva già deciso da
    // EnrichedScene.backgroundImage — dichiarato dal pacchetto se c'è,
    // altrimenti il tag di Gemma se valido (esperimento 20/07/2026).
    private var sceneBackgroundImage: String? by mutableStateOf(null)

    fun choiceText(choice: Choice): String = translatedChoices[choice.id] ?: choice.choiceText

    fun disciplineChoiceText(choice: DisciplineChoice): String =
        translatedChoices[choice.id] ?: choice.choiceText

    val enemyName: String? get() = translatedEnemyName ?: currentScene.combat?.enemyName

    // Fallback sul pacchetto: se il motore non è mai partito
    // (narrationUnavailable) sceneBackgroundImage resta null, ma la
    // scena potrebbe comunque avere un backgroundImage dichiarato.
    val backgroundImage: String? get() = sceneBackgroundImage ?: currentScene.backgroundImage

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
        sceneBackgroundImage = null
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
                        sceneBackgroundImage = event.scene.backgroundImage
                        isGenerating = false
                        // Auto-lettura (UI.md, Tappa 2): solo qui, a testo
                        // finito — leggere durante lo streaming rincorrerebbe
                        // un testo che cambia sotto la voce.
                        if (autoReadEnabled) readAloud()
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

    fun combatFightRound(): RoundResult? {
        lastRound = combatSession?.fightRound()
        combatTick++
        return lastRound
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
        // Il `?: return` di prima lasciava il giocatore FERMO nella scena
        // del combattimento perso, senza sbocchi: dopo
        // withGuaranteedEnding il deathSceneId c'è sempre, quindi la
        // sconfitta porta comunque a un finale.
        val destination = session.destinationSceneId
            ?: manifest.deathSceneId
            ?: AdventureEnding.SYNTHETIC_DEFEAT_SCENE_ID
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

    // RICARICARE CONSUMA IL CHECKPOINT (decisione Michele 20/07/2026):
    // prima si poteva tornare allo stesso punto all'infinito e due
    // piazzamenti rendevano l'avventura innocua. Ora ogni ritorno brucia
    // una vita, e quando finiscono la morte è definitiva.
    fun loadCheckpoint(slot: Int): SessionData? {
        val checkpoint = store.loadCheckpoint(manifest.id, slot) ?: return null
        store.deleteCheckpoint(manifest.id, slot)
        return checkpoint
    }

    // Come è andata a finire: la regola sta nell'engine (testata in JVM),
    // qui si espone solo quello che la UI disegna.
    val endingOutcome: EndingOutcome
        get() = AdventureEnding.outcomeOf(manifest, currentScene)

    val isDeathEnding: Boolean
        get() = isEnding && endingOutcome == EndingOutcome.DEFEAT

    // Il finale FABBRICATO dal motore perché il pacchetto non ne aveva
    // uno: nasce senza testo, e se il narratore non riesce a scriverlo la
    // UI mette quello fisso di strings.xml.
    val isSyntheticEnding: Boolean
        get() = currentScene.id == AdventureEnding.SYNTHETIC_DEFEAT_SCENE_ID

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

    // Scarta un oggetto dallo zaino (Michele 21/07/2026: "manca la
    // possibilità di scartare tenendo premuto sull'oggetto" — la UI
    // chiede conferma prima di chiamare questa funzione, qui si esegue
    // e basta). Una unità alla volta: ogni slot disegnato nello zaino
    // È un'unità (BackpackCard), scartare uno slot = -1 quantità.
    fun discardItem(itemName: String) {
        gameState.updateHero { Inventory.removeItem(it, itemName, 1) }
        autoSave()
    }

    // Consuma un oggetto con effetto dichiarato (v0.1: solo HEAL:n). Un
    // Pasto senza `effect` esplicito cura comunque MealRules.HEAL_AMOUNT
    // (Michele 22/07/2026: "anche fuori puoi consumarli con questo
    // effetto" — stesso valore del consumo obbligatorio in requireAction,
    // così il giocatore può mangiare a piacere dalla scheda, non solo
    // quando il libro lo richiede).
    fun consumeItem(itemName: String) {
        val item = gameState.hero.inventory.firstOrNull {
            it.name.equals(itemName, ignoreCase = true) && it.quantity > 0
        } ?: return
        val heal = item.effect?.takeIf { it.startsWith("HEAL:") }
            ?.substringAfter("HEAL:")?.toIntOrNull()
            ?: MealRules.HEAL_AMOUNT.takeIf { item.name.equals(MealRules.ITEM_NAME, ignoreCase = true) }
            ?: return
        gameState.updateHero { hero ->
            Inventory.removeItem(hero, item.name, 1).let {
                it.copy(currentEndurance = (it.currentEndurance + heal).coerceIn(0, it.maxEndurance))
            }
        }
        autoSave()
    }

    private fun moveTo(targetSceneId: String, transition: Transition) {
        // La voce della scena che si lascia non deve continuare a leggere
        // sopra quella nuova che sta per generarsi.
        ttsService?.stop()
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
        pickedItemNames = pickedNamesFor(currentScene)
        autoSave()
        handleIronDeath()
        // La scena nuova si racconta da sé; il contesto è la CODA della
        // precedente (mai il diario: inferenza senza memoria).
        startNarration(previousSceneText = textJustRead)
    }

    // Ogni mutazione dello stato di gioco passa di qui: è il punto giusto
    // per risincronizzare la copia osservabile dalla UI.
    private fun autoSave() {
        store.saveSession(gameState.snapshot().copy(lastUpdate = System.currentTimeMillis()))
        rinfresca()
    }

    // MORTE DEFINITIVA (STATO.md Blocco 2, rivisto 20/07/2026): la
    // sessione si cancella e il libro riparte da capo. Vale in IRON, che
    // non ha checkpoint per definizione, e ora anche quando il giocatore
    // ha esaurito le sue vite — cioè non ha più nessun checkpoint da
    // ricaricare. La schermata mostra comunque la scena di morte.
    private fun handleIronDeath() {
        if (!isEnding) return
        val isDeathScene = currentScene.id == manifest.deathSceneId
        val senzaPiuVite = placedCheckpoints().isEmpty()
        if (isDeathScene && senzaPiuVite) {
            store.deleteAdventure(manifest.id)
            adventureDeleted = true
        }
    }

    // Non lancia MAI: un id che non esiste (grafo rotto, sessione salvata
    // di un libro poi cambiato) chiudeva il gioco con un'eccezione. Ora si
    // degrada sul finale garantito, che dopo withGuaranteedEnding esiste
    // sempre: l'avventura si chiude dichiarando com'è andata invece di
    // schiantarsi.
    private fun sceneById(id: String): Scene =
        manifest.scenes.firstOrNull { it.id == id }
            ?: manifest.scenes.first { it.id == manifest.deathSceneId }

    private companion object {
        // Buffer dello streaming: la UI si aggiorna al massimo ogni tanto,
        // non a ogni token (CRITICITA.md ~80-100ms).
        const val STREAM_BUFFER_MS = 90L
    }
}
