package io.github.luposolitario.immundanoctisex

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageRepository
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageSource
import io.github.luposolitario.immundanoctisex.core.data.session.FileSessionStore
import io.github.luposolitario.immundanoctisex.core.data.session.SessionStore
import io.github.luposolitario.immundanoctisex.core.engine.dice.DiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.dice.RandomDiceRoller
import io.github.luposolitario.immundanoctisex.image.AnimatedImageLoader
import io.github.luposolitario.immundanoctisex.inference.InferenceEngine
import io.github.luposolitario.immundanoctisex.inference.InferencePreferences
import io.github.luposolitario.immundanoctisex.inference.LiteRtLmEngine
import io.github.luposolitario.immundanoctisex.inference.LlamaCppEngine
import io.github.luposolitario.immundanoctisex.inference.NativeLlamaCppEngine
import io.github.luposolitario.immundanoctisex.model.DownloadableModel
import io.github.luposolitario.immundanoctisex.model.EngineType
import io.github.luposolitario.immundanoctisex.model.ModelPreferences
import io.github.luposolitario.immundanoctisex.music.MusicPlayer
import io.github.luposolitario.immundanoctisex.sfx.SoundEffectPlayer
import io.github.luposolitario.immundanoctisex.util.AccentColorPreferences
import io.github.luposolitario.immundanoctisex.util.AudioPreferences
import io.github.luposolitario.immundanoctisex.util.DiceColorPreferences
import io.github.luposolitario.immundanoctisex.util.FontPreferences
import io.github.luposolitario.immundanoctisex.util.LanguagePreferences
import io.github.luposolitario.immundanoctisex.util.MusicPreferences
import io.github.luposolitario.immundanoctisex.util.NarrativeTonePreferences
import io.github.luposolitario.immundanoctisex.util.ParchmentPreferences
import io.github.luposolitario.immundanoctisex.util.SoundEffectPreferences
import io.github.luposolitario.immundanoctisex.util.StatusCardColorPreferences
import io.github.luposolitario.immundanoctisex.util.ThemePreferences
import io.github.luposolitario.immundanoctisex.util.TtsPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.InputStream

// DI leggera (ARCHITETTURA.md): un contenitore costruito una volta dalla
// MainActivity, al posto dei singleton con Context di v1. Qui vivono solo
// le dipendenze condivise; i ViewModel per schermata se le fanno passare.
class AppContainer(context: Context) {

    // Immagini animate (31/07/2026, doc/UPGRADE.md §7): idempotente di
    // proposito — AppContainer si ricrea a ogni rotazione schermo (`by
    // lazy` sull'istanza Activity in MainActivity.kt), ma l'ImageLoader
    // globale di Coil va installato una volta sola per processo, non
    // ributtato via a ogni rotazione.
    init {
        AnimatedImageLoader.installOnce(context)
    }

    val themePreferences = ThemePreferences(context)

    val modelPreferences = ModelPreferences(context)

    val inferencePreferences = InferencePreferences(context)

    // Opzioni (UI.md schermata 7): font di lettura, lingua della
    // narrazione, TTS. Il tema resta sopra: c'era già prima di questo giro.
    val fontPreferences = FontPreferences(context)

    val languagePreferences = LanguagePreferences(context)

    val ttsPreferences = TtsPreferences(context)

    val narrativeTonePreferences = NarrativeTonePreferences(context)

    val musicPreferences = MusicPreferences(context)

    // Scope applicazione apposta (Michele 22/07/2026): deve sopravvivere
    // alla navigazione tra schermate, non solo vivere dentro Opzioni.
    val musicPlayer = MusicPlayer(context)

    val soundEffectPreferences = SoundEffectPreferences(context)

    // musicPlayer + shouldResumeMusic (24/07/2026): mette la musica in
    // pausa durante i suoni "a nome libero" (loc/enemy/npc/finali,
    // 28/07/2026 in loop finché parla il TTS) e la fa ripartire da sola
    // quando smettono, ma solo se l'utente non l'ha spenta a mano nel
    // frattempo — vedi SoundEffectPlayer.playNamed/stopBackgroundSounds.
    val soundEffectPlayer = SoundEffectPlayer(
        context,
        soundEffectPreferences,
        musicPlayer = musicPlayer,
        shouldResumeMusic = { musicPreferences.musicEnabled },
    )

    val audioPreferences = AudioPreferences(context)

    val accentColorPreferences = AccentColorPreferences(context)

    val parchmentPreferences = ParchmentPreferences(context)

    val statusCardColorPreferences = StatusCardColorPreferences(context)

    val diceColorPreferences = DiceColorPreferences(context)

    // Tre motori, non due (27/07/2026, branch sperimentale
    // feature/llama-cpp-adreno: llama.cpp compilato da noi con backend
    // OpenCL/Adreno vero, contro Llamatik che su Android gira sempre su
    // CPU). Istanze uniche a scope applicazione (ARCHITETTURA §istanze):
    // il modello costa GB e secondi di caricamento, si caricano una volta
    // ciascuno. Solo UNO dei tre è mai "attivo" per davvero: caricare un
    // modello in un altro scarica quello in uso, non si tengono due
    // modelli multi-GB in memoria insieme.
    private val liteRtLmEngine = LiteRtLmEngine(context)
    private val llamaCppEngine = LlamaCppEngine()

    // BUG (31/07/2026, crash immediato all'avvio su device con
    // buildLlama=false, log: "NoClassDefFoundError: android.llama.cpp
    // .LLamaAndroid"): il costruttore di NativeLlamaCppEngine tocca subito
    // LLamaAndroid.instance() (init eager della proprietà), e con
    // buildLlama=false quella classe è solo compileOnly (vedi
    // app/build.gradle.kts) — presente a compile-time ma MAI impacchettata
    // nell'APK. Costruirlo incondizionatamente qui crashava ad ogni avvio,
    // non solo se il motore nativo veniva davvero scelto. Ora null quando
    // BuildConfig.NATIVE_LLAMA_AVAILABLE è falso, mai istanziato.
    private val nativeLlamaCppEngine: NativeLlamaCppEngine? =
        if (BuildConfig.NATIVE_LLAMA_AVAILABLE) NativeLlamaCppEngine() else null

    // Quale motore serve DAVVERO adesso: il resto dell'app (SceneNarrator
    // e giù) continua a parlare solo con InferenceEngine, non sa che ne
    // esistono tre — stessa promessa di ARCHITETTURA.md, qui pagata tre
    // volte invece di una.
    private var activeEngineType: EngineType = EngineType.LITERT_LM

    val inferenceEngine: InferenceEngine
        get() = when (activeEngineType) {
            EngineType.LITERT_LM -> liteRtLmEngine
            EngineType.LLAMA_CPP -> llamaCppEngine
            // Ripiego su LiteRT-LM se un modello selezionato in una
            // sessione precedente (con buildLlama=true) è rimasto marcato
            // LLAMA_CPP_NATIVE nelle preferenze: mai un crash, engine.load()
            // fallirà in modo leggibile su un file del formato sbagliato
            // (il gioco degrada sul testo originale, non si blocca mai).
            EngineType.LLAMA_CPP_NATIVE -> nativeLlamaCppEngine ?: liteRtLmEngine
        }

    // Quale modello e' DAVVERO caricato nel motore in questo momento —
    // diverso da modelPreferences.selectedModelId, che e' solo la scelta
    // salvata (STATO.md: si serializzano i fatti). Serve alla schermata
    // Modelli per sapere quale card e' "in uso ora" davvero.
    // BUG (01/08/2026, Michele: entra in Modelli LLM mentre l'auto-load
    // sta ancora caricando, entrambe le card dicono "Attiva" — poi
    // l'avventura è tradotta lo stesso, cioè il motore ERA caricato e la
    // UI mentiva): era un var normale, che Compose non osserva, e
    // ModelsRoute ne teneva una COPIA presa una volta sola alla
    // composizione — quando l'auto-load finiva, quella copia restava
    // null per sempre. Osservabile come isModelLoading: una sola fonte
    // di verità, letta direttamente dalle schermate.
    var loadedModelId: String? by mutableStateOf(null)
        private set

    // BUG (01/08/2026, Michele: "cerca di far salire su il modello ma
    // visto che ci vuole un po' tu puoi andare nella schermata dei
    // modelli e premere avvia mentre sta per farlo avviare"): l'auto-load
    // all'avvio (AppNavigation.kt) e un'attivazione manuale in Modelli
    // LLM potevano chiamare ENTRAMBI engine.load() in parallelo sullo
    // stesso motore nativo, nessuna esclusione reciproca. loadMutex
    // serializza ogni load() vero; isModelLoading è lo stesso stato
    // osservabile DA QUALUNQUE schermata (a differenza di loadedModelId,
    // var semplice letta oggi solo come istantanea una tantum) — serve
    // per disabilitare "Attiva" e per far sapere ad AdventureRoute se
    // conviene aspettare invece di proporre di avviare un secondo
    // caricamento.
    private val loadMutex = Mutex()

    var isModelLoading: Boolean by mutableStateOf(false)
        private set

    // Il modello richiesto è DAVVERO pronto all'uso ora, senza dover
    // toccare load()? Usata da AdventureRoute per distinguere "già
    // pronto" da "spento"/"in caricamento" prima di decidere cosa fare.
    fun isModelReady(model: DownloadableModel): Boolean =
        engineFor(model.engineType).isLoaded && loadedModelId == model.id

    // Carica il modello selezionato se è già sul telefono. Restituisce
    // false senza rumore se non c'è: il gioco parte comunque, col testo
    // originale del pacchetto. PRIMO controllo di tutti (01/08/2026,
    // Michele: "dovresti darmi la possibilità di disattivare il
    // modello"): se l'utente ha spento il motore a mano
    // (inferencePreferences.engineEnabled), questa funzione non lo tocca
    // MAI — né l'auto-load all'avvio né l'ingresso in avventura. Resta
    // così finché non si preme di nuovo "Attiva" su una card
    // (activateModel sotto la riaccende esplicitamente).
    suspend fun ensureModelLoaded(): Boolean {
        if (!inferencePreferences.engineEnabled) return false
        val model = modelPreferences.selectedModel
        if (isModelReady(model)) return true
        if (!modelPreferences.isDownloaded(model)) return false
        isModelLoading = true
        try {
            return loadMutex.withLock {
                // Ricontrollo DENTRO il lock: se un'altra chiamata (es.
                // l'auto-load all'avvio) ha già finito di caricare questo
                // stesso modello mentre aspettavamo, non c'è nulla da rifare.
                if (isModelReady(model)) return@withLock true
                switchToEngine(model.engineType)
                    .load(modelPreferences.fileFor(model), inferencePreferences.toConfig())
                    .isSuccess
                    .also { if (it) loadedModelId = model.id }
            }
        } finally {
            isModelLoading = false
        }
    }

    // Cambio motore a caldo (Michele 22/07/2026: "un tasto per rendere
    // attivo uno dei motori che scarico, così posso scaricarne più di
    // uno e provare"): load() fa già l'unload del precedente da sé
    // (LiteRtLmEngine), quindi si può cambiare modello anche a partita
    // in corso — ogni scena apre comunque una sessione nuova, senza
    // memoria, quindi non c'è contesto da perdere nel cambio. Se il
    // modello scelto usa un ENGINE diverso da quello in uso (27/07/2026:
    // LiteRT-LM <-> GGUF), si scarica prima l'altro — un modello alla
    // volta, mai due processi nativi multi-GB insieme.
    suspend fun activateModel(model: DownloadableModel): Result<Unit> {
        // Un tocco esplicito su "Attiva" vince sempre su un precedente
        // spegnimento (01/08/2026): altrimenti si attiverebbe un modello
        // che poi AdventureRoute si rifiuterebbe di usare, contraddicendo
        // il tocco appena fatto.
        inferencePreferences.engineEnabled = true
        if (isModelReady(model)) return Result.success(Unit)
        android.util.Log.i("AppContainer", "activateModel: ${model.id}, activeEngineType=$activeEngineType")
        isModelLoading = true
        try {
            return loadMutex.withLock {
                if (isModelReady(model)) return@withLock Result.success(Unit)
                val engine = switchToEngine(model.engineType)
                android.util.Log.i("AppContainer", "activateModel: switchToEngine tornato, chiamo load()")
                engine.load(modelPreferences.fileFor(model), inferencePreferences.toConfig())
                    .onSuccess {
                        modelPreferences.selectedModelId = model.id
                        loadedModelId = model.id
                    }
            }
        } finally {
            isModelLoading = false
        }
    }

    // Spegnimento esplicito (01/08/2026, Michele: "dovresti darmi la
    // possibilità di disattivare il modello"): scarica il motore dalla
    // memoria SUBITO (non aspetta la prossima apertura dell'app) e imposta
    // la preferenza — ensureModelLoaded() sopra la rispetta da qui in poi,
    // resta spento finché non si preme di nuovo "Attiva".
    suspend fun disableEngine() {
        inferencePreferences.engineEnabled = false
        loadMutex.withLock {
            runCatching { inferenceEngine.unload() }
            loadedModelId = null
        }
    }

    // Il file del modello in uso è stato cancellato: il motore ce l'ha
    // ancora in RAM, ma tenerlo caricato non ha più senso (e la card
    // direbbe "In uso ora" per un modello che non esiste più sul
    // telefono — BUG del 22/07/2026, prima aggirato azzerando solo lo
    // stato locale della schermata). NON tocca engineEnabled: è una
    // conseguenza della cancellazione, non la scelta di spegnere il
    // motore — al prossimo avvio riparte con un altro modello.
    suspend fun unloadIfLoaded(model: DownloadableModel) {
        if (loadedModelId != model.id) return
        loadMutex.withLock {
            runCatching { inferenceEngine.unload() }
            loadedModelId = null
        }
    }

    private fun engineFor(type: EngineType): InferenceEngine = when (type) {
        EngineType.LITERT_LM -> liteRtLmEngine
        EngineType.LLAMA_CPP -> llamaCppEngine
        EngineType.LLAMA_CPP_NATIVE -> nativeLlamaCppEngine ?: liteRtLmEngine
    }

    private suspend fun switchToEngine(type: EngineType): InferenceEngine {
        if (type != activeEngineType) {
            android.util.Log.i("AppContainer", "switchToEngine: scarico $activeEngineType prima di passare a $type")
            runCatching { engineFor(activeEngineType).unload() }
            android.util.Log.i("AppContainer", "switchToEngine: $activeEngineType scaricato")
            activeEngineType = type
            loadedModelId = null
        }
        return engineFor(type)
    }

    val sessionStore: SessionStore =
        FileSessionStore(File(context.filesDir, "saves"))

    val diceRoller: DiceRoller = RandomDiceRoller()

    // Il libro incluso nell'APK: scenes.sample.json dagli asset (content/
    // è montato come cartella asset dal build). `var`, non `val`: il
    // side-load (20/07/2026, richiesta urgente di Michele per i test —
    // "devo poter caricare vari file") lo sostituisce a runtime, senza
    // riavviare l'app.
    var packageRepository = PackageRepository(
        // In test-books/ insieme agli altri libri (01/08/2026, Michele:
        // "tieni sempre quello che si trova in test-books"): prima ne
        // esistevano due copie, una qui in radice e una lì, con la
        // seconda rimasta indietro di quattro immagini. `content/` è
        // montata come cartella asset dal build, quindi il percorso
        // dell'asset include la sottocartella.
        AssetPackageSource(context, "test-books/scenes.sample.json"),
    )
        private set

    // Side-load da picker di sistema (SAF): PackageSource lo prevedeva
    // già nel suo stesso commento come terza implementazione, oltre
    // all'asset e al file temporaneo dei test. Il permesso persistente
    // non è strettamente necessario per l'uso immediato (si sceglie e si
    // usa nella sessione corrente), ma costa una riga e rende l'URI
    // ancora leggibile se l'app viene riavviata con lo stesso file.
    fun loadSideloadedPackage(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        packageRepository = PackageRepository(UriPackageSource(context, uri))
    }
}

// Implementazione Android di PackageSource: apre un asset dell'APK.
class AssetPackageSource(
    private val context: Context,
    private val assetName: String,
) : PackageSource {
    override fun open(): InputStream = context.assets.open(assetName)
}

// Implementazione Android di PackageSource: apre un file scelto dal
// picker di sistema. Nessun path da gestire a mano — l'Uri, coi suoi
// permessi, è tutto ciò che serve per riaprirlo.
class UriPackageSource(
    private val context: Context,
    private val uri: Uri,
) : PackageSource {
    override fun open(): InputStream =
        context.contentResolver.openInputStream(uri)
            ?: throw java.io.FileNotFoundException("Impossibile aprire il file scelto: $uri")
}
