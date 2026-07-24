package io.github.luposolitario.immundanoctisex

import android.content.Context
import android.net.Uri
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageRepository
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageSource
import io.github.luposolitario.immundanoctisex.core.data.session.FileSessionStore
import io.github.luposolitario.immundanoctisex.core.data.session.SessionStore
import io.github.luposolitario.immundanoctisex.core.engine.dice.DiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.dice.RandomDiceRoller
import io.github.luposolitario.immundanoctisex.inference.InferenceEngine
import io.github.luposolitario.immundanoctisex.inference.InferencePreferences
import io.github.luposolitario.immundanoctisex.inference.LiteRtLmEngine
import io.github.luposolitario.immundanoctisex.model.DownloadableModel
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
import java.io.File
import java.io.InputStream

// DI leggera (ARCHITETTURA.md): un contenitore costruito una volta dalla
// MainActivity, al posto dei singleton con Context di v1. Qui vivono solo
// le dipendenze condivise; i ViewModel per schermata se le fanno passare.
class AppContainer(context: Context) {

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
    // pausa durante i suoni "a nome libero" (loc/enemy/npc/finali) e la
    // fa ripartire da sola, ma solo se l'utente non l'ha spenta a mano
    // nel frattempo — vedi SoundEffectPlayer.playNamed/MusicPlayer.duckFor.
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

    // Istanza unica a scope applicazione (ARCHITETTURA §istanze): il
    // modello costa GB e secondi di caricamento, si carica una volta.
    val inferenceEngine: InferenceEngine = LiteRtLmEngine(context)

    // Quale modello e' DAVVERO caricato nel motore in questo momento —
    // diverso da modelPreferences.selectedModelId, che e' solo la scelta
    // salvata (STATO.md: si serializzano i fatti). Serve alla schermata
    // Modelli per sapere quale card e' "in uso ora" davvero.
    var loadedModelId: String? = null
        private set

    // Carica il modello selezionato se è già sul telefono. Restituisce
    // false senza rumore se non c'è: il gioco parte comunque, col testo
    // originale del pacchetto.
    suspend fun ensureModelLoaded(): Boolean {
        if (inferenceEngine.isLoaded) return true
        val model = modelPreferences.selectedModel
        if (!modelPreferences.isDownloaded(model)) return false
        return inferenceEngine
            .load(modelPreferences.fileFor(model), inferencePreferences.toConfig())
            .isSuccess
            .also { if (it) loadedModelId = model.id }
    }

    // Cambio motore a caldo (Michele 22/07/2026: "un tasto per rendere
    // attivo uno dei motori che scarico, così posso scaricarne più di
    // uno e provare"): load() fa già l'unload del precedente da sé
    // (LiteRtLmEngine), quindi si può cambiare modello anche a partita
    // in corso — ogni scena apre comunque una sessione nuova, senza
    // memoria, quindi non c'è contesto da perdere nel cambio.
    suspend fun activateModel(model: DownloadableModel): Result<Unit> =
        inferenceEngine
            .load(modelPreferences.fileFor(model), inferencePreferences.toConfig())
            .onSuccess {
                modelPreferences.selectedModelId = model.id
                loadedModelId = model.id
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
        AssetPackageSource(context, "scenes.sample.json"),
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
