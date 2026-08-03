package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.CombatOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.checkpointBudget
import io.github.luposolitario.immundanoctisex.core.data.model.EndingOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ImageReference
import io.github.luposolitario.immundanoctisex.core.data.model.JourneyEntry
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.model.Transition
import io.github.luposolitario.immundanoctisex.core.data.session.SessionStore
import io.github.luposolitario.immundanoctisex.core.engine.choice.ChoiceAvailability
import io.github.luposolitario.immundanoctisex.core.engine.choice.RollModifiers
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatSession
import io.github.luposolitario.immundanoctisex.core.engine.combat.CombatStatus
import io.github.luposolitario.immundanoctisex.core.engine.combat.RoundResult
import io.github.luposolitario.immundanoctisex.core.engine.dice.DiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.ending.AdventureEnding
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.inventory.ItemOffers
import io.github.luposolitario.immundanoctisex.core.engine.inventory.MealRules
import io.github.luposolitario.immundanoctisex.core.engine.mechanics.MechanicsExecutor
import io.github.luposolitario.immundanoctisex.core.engine.sfx.SceneSfxResolver
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveMaxEndurance
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import io.github.luposolitario.immundanoctisex.core.engine.transition.TransitionEngine
import io.github.luposolitario.immundanoctisex.inference.NarrationEvent
import io.github.luposolitario.immundanoctisex.inference.SceneNarrator
import io.github.luposolitario.immundanoctisex.inference.TokenInfo
import io.github.luposolitario.immundanoctisex.sfx.SoundEffect
import io.github.luposolitario.immundanoctisex.sfx.SoundEffectPlayer
import io.github.luposolitario.immundanoctisex.tts.TtsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    // BUG (01/08/2026, Michele: "l'avventura in inglese... dopo un paio di
    // volte il motore si è avviato"): il controllo a tre vie di
    // AdventureRoute (pronto/in caricamento/spento) scattava solo
    // all'ingresso in avventura — moveTo() chiama startNarration()
    // direttamente ad ogni scena successiva, e SceneNarrator.narrate() ha
    // un suo controllo indipendente (engine.isLoaded) che degrada in
    // silenzio se il caricamento in background non è ancora finito.
    // Risultato: scena 1 muta, poi le scene dopo iniziano a tradursi da
    // sole appena il motore finisce di caricare, senza alcun segnale.
    // Questa callback (AppContainer.ensureModelLoaded, iniettata da
    // AdventureRoute) si richiama PRIMA di ogni narrazione, non solo la
    // prima: aspetta un caricamento già in corso invece di arrendersi
    // subito. Default no-op (true) per i test/@Preview che non
    // costruiscono un AppContainer vero.
    private val ensureEngineLoaded: suspend () -> Boolean = { true },
    // Chiamata quando il libro si chiude con la VITTORIA (03/08/2026):
    // il personaggio trasportabile registra il libro completato e si
    // porta dietro com'è arrivato alla fine. Chi muore non la vede
    // passare — il libro non l'ha finito, non ha guadagnato nulla.
    // Default no-op per i test e le @Preview.
    private val onLibroVinto: (Character) -> Unit = {},
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

    // Terzo stato, distinto da "sta caricando" (01/08/2026, Michele:
    // "se il motore è spento... dammi l'opportunità di giocare anche
    // senza motore attivo"): il modello è sul telefono ma nessuno lo sta
    // caricando — invece di forzare un'attesa di 15-20s in silenzio, si
    // chiede esplicitamente se avviarlo ora o continuare col testo
    // originale. Vedi awaitEngineChoice()/beginLoadingEngineNow() sotto.
    var awaitingEngineChoice: Boolean by mutableStateOf(false)
        private set

    // Terzo valore dello stato del narratore unificato (UI.md: IDLE /
    // GENERATING / SPEAKING) — il cerchio d'oro nel banner si accende
    // anche qui, non solo mentre Gemma scrive.
    var isSpeaking: Boolean by mutableStateOf(false)
        private set

    // Rete di sicurezza (28/07/2026, Michele: passando a un'altra app il
    // TTS smetteva davvero di parlare ma il sottofondo SFX continuava
    // all'infinito, finché non chiudeva l'app): TtsService.onSpeakingFinished
    // dipende dai callback di UtteranceProgressListener, che possono non
    // arrivare MAI se il sistema operativo interrompe l'audio in background
    // (perdita di focus) senza passare da una stop() esplicita — coerente
    // col fatto che avesse pensato al polling fin dall'inizio. Finché
    // isSpeaking risulta true, si ricontrolla periodicamente lo stato VERO
    // del motore (TtsService.isCurrentlySpeaking, non il nostro flag): se
    // dice che non sta più parlando, si tratta il narratore come finito
    // anche senza il callback.
    private var ttsWatchdogJob: Job? = null

    private fun startTtsWatchdog() {
        val scope = this.scope ?: return
        ttsWatchdogJob?.cancel()
        ttsWatchdogJob = scope.launch {
            while (isSpeaking) {
                delay(TTS_WATCHDOG_POLL_MS)
                if (isSpeaking && ttsService?.isCurrentlySpeaking() == false) {
                    isSpeaking = false
                    soundEffectPlayer?.setDuckedByTts(false)
                }
            }
        }
    }

    init {
        // Il ducking (SoundEffectPlayer.setDuckedByTts) segue esattamente
        // isSpeaking: stesso segnale, non uno stato separato da tenere
        // sincronizzato a mano.
        ttsService?.onSpeakingStarted = {
            isSpeaking = true
            soundEffectPlayer?.setDuckedByTts(true)
            startTtsWatchdog()
        }
        ttsService?.onSpeakingFinished = {
            ttsWatchdogJob?.cancel()
            isSpeaking = false
            soundEffectPlayer?.setDuckedByTts(false)
        }
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
        soundEffectPlayer?.play(SoundEffect.DICE_ROLL)
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

    // Suono del pasto in sospeso (22/07/2026): messo da moveTo, consumato
    // qui appena la narrazione ha davvero del testo da mostrare, invece
    // di partire nell'istante del tocco — molto prima che Gemma finisca.
    private var pendingMealSound = false

    private fun playPendingMealSoundIfAny() {
        if (!pendingMealSound) return
        pendingMealSound = false
        soundEffectPlayer?.play(SoundEffect.EAT)
    }

    // Sticky per la sessione (01/08/2026, BUG Michele: "l'avventura in
    // inglese... dopo un paio di volte il motore si è avviato"): una volta
    // deciso "niente motore" — dichiarato dal giocatore
    // (awaitEngineChoice -> skip), modello assente, o caricamento fallito
    // — le scene successive non ritentano da sole. Senza questo, moveTo()
    // (che chiama startNarration() per OGNI scena, non solo la prima)
    // avrebbe riacceso la traduzione a sorpresa non appena il motore
    // avviato in background finiva di caricare, contraddicendo la scelta
    // fatta.
    private var engineOptedOut = false

    // Avvia (o riavvia) la narrazione della scena corrente. Lo streaming
    // è BUFFERIZZATO: si aggiorna la UI al massimo ogni ~90ms, altrimenti
    // ogni token farebbe ricomporre l'intera schermata (CRITICITA.md).
    fun startNarration(previousSceneText: String?) {
        val narrator = this.narrator ?: run { playPendingMealSoundIfAny(); return }
        val scope = this.scope ?: run { playPendingMealSoundIfAny(); return }
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
        // BUG (01/08/2026): qui non è detto che il modello sia già
        // caricato — vale solo per la primissima scena, già filtrata da
        // AdventureRoute prima di chiamare startNarration(). Per le scene
        // successive (moveTo -> startNarration diretto) il motore
        // potrebbe essere ancora in caricamento in background: se non ha
        // ancora rinunciato per questa sessione (engineOptedOut), si
        // aspetta invece di arrendersi subito — vedi il controllo dentro
        // la coroutine sotto.
        val engineMightStillBeStarting = !engineOptedOut && !narrator.isReady
        isLoadingModel = engineMightStillBeStarting

        narrationJob = scope.launch {
            if (engineMightStillBeStarting) {
                // Aspetta un caricamento già in corso (l'auto-load
                // all'avvio, o un "Avvia il motore" scelto in una scena
                // precedente) invece di far decidere a
                // SceneNarrator.narrate() — che degraderebbe subito e in
                // silenzio guardando solo l'istantanea attuale di
                // engine.isLoaded.
                ensureEngineLoaded()
                isLoadingModel = false
            }
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
                        // Lo sfondo può arrivare solo ora da Gemma (l'autore
                        // non ne aveva uno valido): richiama la stessa sync
                        // di moveTo, che suona solo se il valore è CAMBIATO.
                        syncImageSounds()
                        isGenerating = false
                        // Suono in sospeso (22/07/2026, Michele: partiva
                        // ancora mentre lo streaming era a metà — "meglio
                        // che parta dopo che finisce lo streaming del
                        // testo"): qui il testo tradotto è completo, non al
                        // primo pezzo che arriva.
                        playPendingMealSoundIfAny()
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
    // fallita) o il giocatore ha scelto di continuare senza (vedi
    // awaitEngineChoice() sotto): si torna al testo del pacchetto invece
    // di lasciare "il narratore scrive" per sempre. engineOptedOut = true
    // rende la decisione valida per TUTTA la sessione (vedi
    // startNarration sopra): niente ritentativi a sorpresa sulle scene
    // successive.
    fun narrationUnavailable() {
        engineOptedOut = true
        awaitingEngineChoice = false
        isGenerating = false
        isLoadingModel = false
        if (narrative.isBlank()) narrative = currentScene.narrativeText
        playPendingMealSoundIfAny()
    }

    // Il modello è sul telefono ma il motore è spento e nessuno lo sta
    // caricando: AdventureRoute chiama questo invece di avviare subito
    // ensureModelLoaded(), per lasciare la scelta al giocatore.
    fun awaitEngineChoice() {
        isGenerating = false
        isLoadingModel = false
        awaitingEngineChoice = true
    }

    // Il giocatore ha scelto "Avvia il motore ora": prepara lo stato
    // PRIMA che AdventureRoute lanci davvero ensureModelLoaded() in una
    // coroutine — stesso schema di startNarration/narrationUnavailable,
    // lo stato cambia qui, il lavoro vero parte fuori.
    fun beginLoadingEngineNow() {
        awaitingEngineChoice = false
        isGenerating = true
        isLoadingModel = true
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

    // Il tiro GREZZO, sempre: è il fatto ("si serializzano i fatti, i
    // bonus si calcolano"). Il bonus condizionale sta in rollModifier
    // qui sotto e si somma solo al momento di risolvere.
    var lastChoiceRoll: Int? by mutableStateOf(null)
        private set

    // Bonus/malus della scena applicabili a QUESTO personaggio adesso
    // (01/08/2026, Scene.rollModifiers): "se hai la Disciplina del Sesto
    // Senso aggiungi 2". Zero per ogni libro che non li dichiara.
    val rollModifier: Int
        get() = RollModifiers.totalFor(currentScene, gameState)

    fun rollForChoice() {
        if (!requiresRoll || lastChoiceRoll != null) return
        lastChoiceRoll = dice.roll()
    }

    // Risolve il tiro mostrato: la scelta il cui intervallo contiene il
    // numero, modificatori inclusi (col bonus il totale può uscire da
    // 0-9, ed è normale: i libri hanno intervalli tipo "7–11" proprio
    // per questo). Nessun intervallo coperto (pacchetto scritto male):
    // il tiro si azzera e si riprova, il gioco non si blocca mai.
    fun resolveRolledChoice() {
        val roll = lastChoiceRoll ?: return
        val choice = ChoiceAvailability.forRoll(currentScene, roll + rollModifier)
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
        soundEffectPlayer?.play(SoundEffect.COMBAT_START)
    }

    // Modalità COMPLETA: round per round col menu tattico.
    fun startCompleteCombat() {
        val combat = currentScene.combat ?: return
        combatSession = CombatSession(gameState.hero, combat, dice)
        lastRound = null
        soundEffectPlayer?.play(SoundEffect.COMBAT_START)
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
        get() = gameState.session.difficulty.checkpointBudget()

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
                it.copy(currentEndurance = (it.currentEndurance + heal).coerceIn(0, effectiveMaxEndurance(it)))
            }
        }
        // Suono diverso per cosa si consuma davvero (Michele 22/07/2026:
        // "ho aggiunto altri 2 suoni... se le associ alle azioni
        // specifiche andrebbe bene") — il Pasto si mangia, tutto il
        // resto con effetto HEAL (pozioni) si beve.
        soundEffectPlayer?.play(
            if (item.name.equals(MealRules.ITEM_NAME, ignoreCase = true)) SoundEffect.EAT else SoundEffect.DRINK,
        )
        autoSave()
    }

    private fun moveTo(targetSceneId: String, transition: Transition) {
        // La voce della scena che si lascia non deve continuare a leggere
        // sopra quella nuova che sta per generarsi.
        ttsService?.stop()
        // Rete di sicurezza (28/07/2026): il sottofondo SFX della scena
        // lasciata normalmente si ferma da sé quando il TTS finisce di
        // leggere, ma se l'auto-lettura è spenta e nessuno ha toccato
        // "leggi" il TTS non parla mai in quella scena — senza questo
        // giro andrebbe in loop per sempre, sopravvivendo anche nella
        // scena successiva.
        soundEffectPlayer?.stopBackgroundSounds()
        lastChoiceRoll = null // ogni scena nuova riarma il dado (reset di v1)
        // Nel diario finisce il testo che il giocatore HA LETTO (quello
        // arricchito, se c'era): si salva e non si rigenera mai
        // (STATO.md Blocco 3).
        val textJustRead = narrative
        gameState.addJourneyEntry(
            JourneyEntry(currentScene.id, textJustRead, transition, currentLocation),
        )
        val result = engine.transitionTo(gameState, targetSceneId)
        // Pasto mangiato durante la transizione (Michele 22/07/2026:
        // "EAT_MEAL lo possiamo mettere nel JSON" — requireAction EAT_MEAL
        // è già dichiarato dall'autore, non generato da Gemma): stesso
        // suono del consumo manuale dalla scheda. NON parte subito qui
        // (Michele 22/07/2026, dopo averlo sentito arrivare secondi
        // prima del testo): resta in sospeso finché la narrazione ha
        // davvero qualcosa da mostrare — lo consuma startNarration.
        pendingMealSound = result.mealEaten
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
        // Passi (Michele 22/07/2026: "un suono... di passi da usare nelle
        // transizioni"): solo sul cammino narrativo vero, non su
        // START/ENDING né sull'ingresso in combattimento (TRANSITION può
        // essere anche una scena di scontro, es. sample-adventure scena
        // "4") — lì i passi stonerebbero. Parte subito, non in sospeso
        // come il pasto: accompagna il gesto di scegliere, non un fatto
        // da confermare col testo.
        if (currentScene.sceneType == SceneType.TRANSITION && currentScene.combat == null) {
            soundEffectPlayer?.play(SoundEffect.FOOTSTEPS)
        }
        syncImageSounds()
        playEndingSoundIfNew()
        autoSave()
        registraVittoriaSePresente()
        handleIronDeath()
        // La scena nuova si racconta da sé; il contesto è la CODA della
        // precedente (mai il diario: inferenza senza memoria).
        startNarration(previousSceneText = textJustRead)
    }

    // Un mp3 per ogni risorsa immagine (Michele 22/07/2026: "tutte le
    // risorse immagine hanno un corrispettivo mp3 che si suona insieme
    // una volta, la musica è sempre sottofondo"): vocabolario APERTO
    // (SoundEffectPlayer.playNamed), non un catalogo chiuso come le
    // immagini stesse — un file mancante è solo silenzio. Ogni campo si
    // suona solo quando CAMBIA rispetto all'ultima volta, non ad ogni
    // ricomposizione: enemyImage/npcImage sono sempre dell'autore, noti
    // subito; backgroundImage può arrivare più tardi da Gemma (per questo
    // la funzione si richiama anche a narrazione completata).
    private var lastPlayedBackgroundImage: String? = null
    private var lastPlayedEnemyImage: String? = null
    private var lastPlayedNpcImage: String? = null

    // Scene.sfx (31/07/2026, doc/UPGRADE.md §7): l'autore può sovrascrivere
    // i 3 suoni automatici sopra con un mp3 scelto per questa scena
    // (static: o url:, risolto e validato in fase di caricamento del
    // libro). Override NON addittivo: se presente, si suona SOLO quello e
    // si esce, i tre suoni automatici sotto non partono.
    private var lastPlayedCustomSfx: String? = null

    private fun syncImageSounds() {
        val customSfx = SceneSfxResolver.resolve(currentScene, manifest)
        if (customSfx != null) {
            if (customSfx != lastPlayedCustomSfx) {
                ImageReference.parse(customSfx)?.let { soundEffectPlayer?.playCustomSfx(it) }
            }
            lastPlayedCustomSfx = customSfx
            return
        }
        lastPlayedCustomSfx = null

        val bg = backgroundImage
        if (bg != null && bg != lastPlayedBackgroundImage) soundEffectPlayer?.playNamed(bg)
        lastPlayedBackgroundImage = bg

        val enemy = currentScene.combat?.enemyImage
        if (enemy != null && enemy != lastPlayedEnemyImage) soundEffectPlayer?.playNamed(enemy)
        lastPlayedEnemyImage = enemy

        val npc = currentScene.npcImage
        if (npc != null && npc != lastPlayedNpcImage) soundEffectPlayer?.playNamed(npc)
        lastPlayedNpcImage = npc
    }

    // Suono di finale (Michele 22/07/2026: "una voce di gioia quando
    // termina l'avventura e un grido quando muore"). Non più diviso per
    // genere (26/07/2026, Michele: "ho cambiato i suoni che ora sono
    // solo 3 indipendentemente dal sesso") — un solo file per esito.
    // Cartella a parte (endings/) dallo stesso vocabolario aperto delle
    // immagini — nomi attesi: ending_victory, ending_defeat,
    // ending_neutral.
    private var lastPlayedEnding: EndingOutcome? = null

    private fun playEndingSoundIfNew() {
        if (!isEnding) return
        val outcome = endingOutcome
        if (outcome == lastPlayedEnding) return
        lastPlayedEnding = outcome
        // Parte SUBITO, non più in attesa che il TTS finisca di leggere
        // (28/07/2026, Michele): con gli SFX a nome libero ora ridotti di
        // volume mentre il narratore parla (SoundEffectPlayer
        // .setDuckedByTts), la sovrapposizione che l'attesa serviva a
        // evitare è diventata l'effetto voluto — un sottofondo, non più
        // un suono a piena voce sopra il narratore.
        // Una volta sola, non in loop (03/08/2026): dopo l'ultima scena
        // non c'è nessun cambio di scena a fermarlo, e con l'auto-lettura
        // spenta nemmeno il TTS — restava a girare finché non si chiudeva
        // l'app.
        soundEffectPlayer?.playNamed(
            "ending_${outcome.name.lowercase()}",
            folder = "endings",
            inLoop = false,
        )
    }

    // Ogni mutazione dello stato di gioco passa di qui: è il punto giusto
    // per risincronizzare la copia osservabile dalla UI.
    private fun autoSave() {
        store.saveSession(gameState.snapshot().copy(lastUpdate = System.currentTimeMillis()))
        rinfresca()
    }

    // Il libro finito con la vittoria aggiorna il personaggio
    // trasportabile (03/08/2026). Una volta sola per partita: entrando
    // nella scena finale, non a ogni ricomposizione.
    //
    // Solo VICTORY: un finale neutro o una morte non danno nulla, e il
    // canone assegna la Disciplina Kai a chi il libro l'ha portato a
    // termine.
    private var vittoriaGiaRegistrata = false

    private fun registraVittoriaSePresente() {
        if (vittoriaGiaRegistrata) return
        if (!isEnding || endingOutcome != EndingOutcome.VICTORY) return
        vittoriaGiaRegistrata = true
        onLibroVinto(gameState.hero)
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

    // BUG (24/07/2026, Michele: "il rumore dei passi e quello della
    // taverna sono partiti contemporaneamente"): la scena INIZIALE (sia
    // una nuova avventura sia una ripresa) non passa mai da `moveTo()` —
    // il suo suono location restava agganciato SOLO al completamento
    // della narrazione (`syncImageSounds()` richiamata da
    // `NarrationEvent.Completed`). Avanzando in fretta alla scena
    // successiva (i cui passi partono SUBITO, senza attese) i due suoni
    // potevano scontrarsi in un momento qualunque, sembrando un unico
    // suono confuso. Ora la sincronizzazione parte anche qui, una volta
    // sola, alla costruzione dello stato — stesso trattamento immediato
    // di ogni altra scena, indipendente dal narratore.
    init {
        syncImageSounds()
    }

    private companion object {
        // Buffer dello streaming: la UI si aggiorna al massimo ogni tanto,
        // non a ogni token (CRITICITA.md ~80-100ms).
        const val STREAM_BUFFER_MS = 90L

        // Intervallo del watchdog TTS (28/07/2026): solo una rete di
        // sicurezza per il caso in cui manchi il callback, non il percorso
        // principale — non serve granularità fine, un paio di secondi di
        // sottofondo in più nel caso raro non si sente.
        const val TTS_WATCHDOG_POLL_MS = 2_000L
    }
}
