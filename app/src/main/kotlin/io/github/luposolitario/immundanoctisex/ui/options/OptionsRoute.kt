package io.github.luposolitario.immundanoctisex.ui.options

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.luposolitario.immundanoctisex.AppContainer
import io.github.luposolitario.immundanoctisex.core.data.model.Gender
import io.github.luposolitario.immundanoctisex.tts.TtsService
import io.github.luposolitario.immundanoctisex.util.AccentColor
import io.github.luposolitario.immundanoctisex.util.BundledMusicCatalog
import io.github.luposolitario.immundanoctisex.util.BundledTrack
import io.github.luposolitario.immundanoctisex.util.OutputLanguage
import io.github.luposolitario.immundanoctisex.util.StatusCardColor
import java.util.Locale

// Raccordo delle Opzioni: legge le preferenze da AppContainer, le scrive
// ad ogni cambio (stessa politica commit-al-rilascio di
// AdvancedSettingsCard per gli slider, non ad ogni frame di trascinamento).
// Il tema e un caso a parte: vive nello stato di MainActivity, non solo
// nella preference, perche deve applicarsi SUBITO senza riavviare l app.
@Composable
fun OptionsRoute(
    container: AppContainer,
    darkOverride: Boolean?,
    onThemeOverrideChange: (Boolean?) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
    onModelsClick: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    var themeOverride by remember { mutableStateOf(darkOverride) }
    var accentColor by remember { mutableStateOf(container.accentColorPreferences.accentColor) }
    var statusCardColor by remember { mutableStateOf(container.statusCardColorPreferences.statusCardColor) }
    var font by remember { mutableStateOf(container.fontPreferences.readingFont) }
    var bold by remember { mutableStateOf(container.fontPreferences.boldText) }
    var language by remember { mutableStateOf(container.languagePreferences.outputLanguage) }
    var tone by remember { mutableStateOf(container.narrativeTonePreferences.narrativeTone) }

    var autoRead by remember { mutableStateOf(container.ttsPreferences.autoReadEnabled) }
    var speechRate by remember { mutableStateOf(container.ttsPreferences.speechRate) }
    var pitch by remember { mutableStateOf(container.ttsPreferences.pitch) }
    var maleVoice by remember { mutableStateOf(container.ttsPreferences.voiceFor(Gender.MALE)) }
    var femaleVoice by remember { mutableStateOf(container.ttsPreferences.voiceFor(Gender.FEMALE)) }

    var musicEnabled by remember { mutableStateOf(container.musicPreferences.musicEnabled) }
    var selectedTrackId by remember { mutableStateOf(container.musicPreferences.effectiveTrack.id) }

    // I tre volumi (22/07/2026, richiesta Michele): TTS e musica hanno
    // ciascuno il proprio, il generale moltiplica entrambi - vive in
    // AudioPreferences perche non appartiene ne all uno ne all altra.
    var ttsVolume by remember { mutableStateOf(container.ttsPreferences.volume) }
    var musicVolume by remember { mutableStateOf(container.musicPreferences.volume) }
    var generalVolume by remember { mutableStateOf(container.audioPreferences.generalVolume) }

    // Anteprima locale a questa schermata (Michele 22/07/2026: seleziona
    // una combo e parte per provarla) - un MediaPlayer separato dalla
    // riproduzione vera in partita (non ancora collegata), vive e muore
    // con Opzioni: non deve continuare a suonare una volta usciti da
    // qui. Il volume riflette musica * generale, non solo musica: il
    // generale deve sentirsi anche qui, non solo in partita.
    val previewPlayer = remember { MediaPlayer() }
    DisposableEffect(Unit) {
        onDispose { previewPlayer.release() }
    }
    fun previewVolume(): Float = (musicVolume * generalVolume).coerceIn(0f, 1f)
    fun playPreview(track: BundledTrack) {
        runCatching {
            previewPlayer.reset()
            context.assets.openFd(track.assetPath).use { afd ->
                previewPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            previewPlayer.isLooping = true
            previewPlayer.setVolume(previewVolume(), previewVolume())
            previewPlayer.setOnPreparedListener { it.start() }
            previewPlayer.prepareAsync()
        }
    }

    // Le voci disponibili si sanno solo dopo l inizializzazione del
    // motore TTS: prima di allora la lista resta vuota, non bloccante.
    var ttsReady by remember { mutableStateOf(false) }
    val ttsService = remember { TtsService(context) { ttsReady = true } }
    DisposableEffect(Unit) {
        onDispose { ttsService.shutdown() }
    }
    val availableVoices = if (ttsReady) ttsService.availableVoices(language.locale).map { it.name } else emptyList()

    OptionsScreen(
        darkOverride = themeOverride,
        onThemeSelect = { override ->
            themeOverride = override
            onThemeOverrideChange(override)
        },
        accentColor = accentColor,
        onAccentColorSelect = { selected ->
            accentColor = selected
            onAccentColorChange(selected)
        },
        statusCardColor = statusCardColor,
        onStatusCardColorSelect = { selected ->
            statusCardColor = selected
            container.statusCardColorPreferences.statusCardColor = selected
        },
        readingFont = font,
        onFontSelect = { selected ->
            font = selected
            container.fontPreferences.readingFont = selected
        },
        boldText = bold,
        onBoldTextChange = { enabled ->
            bold = enabled
            container.fontPreferences.boldText = enabled
        },
        outputLanguage = language,
        onLanguageSelect = { selected ->
            language = selected
            container.languagePreferences.outputLanguage = selected
        },
        narrativeTone = tone,
        onToneSelect = { selected ->
            tone = selected
            container.narrativeTonePreferences.narrativeTone = selected
        },
        ttsUi = TtsUi(
            autoReadEnabled = autoRead,
            speechRate = speechRate,
            pitch = pitch,
            maleVoices = availableVoices,
            selectedMaleVoice = maleVoice,
            femaleVoices = availableVoices,
            selectedFemaleVoice = femaleVoice,
        ),
        onAutoReadChange = { enabled ->
            autoRead = enabled
            container.ttsPreferences.autoReadEnabled = enabled
        },
        onSpeechRateChange = { speechRate = it },
        onSpeechRateCommit = { container.ttsPreferences.speechRate = speechRate },
        onPitchChange = { pitch = it },
        onPitchCommit = { container.ttsPreferences.pitch = pitch },
        onMaleVoiceSelect = { voice ->
            maleVoice = voice
            container.ttsPreferences.setVoiceFor(Gender.MALE, voice)
        },
        onFemaleVoiceSelect = { voice ->
            femaleVoice = voice
            container.ttsPreferences.setVoiceFor(Gender.FEMALE, voice)
        },
        onTestMaleVoice = {
            val (phrase, locale) = ttsTestPhrase(language)
            ttsService.speak(phrase, Gender.MALE, locale)
        },
        onTestFemaleVoice = {
            val (phrase, locale) = ttsTestPhrase(language)
            ttsService.speak(phrase, Gender.FEMALE, locale)
        },
        musicUi = MusicUi(
            musicEnabled = musicEnabled,
            tracks = BundledMusicCatalog.TRACKS,
            selectedTrackId = selectedTrackId,
        ),
        onMusicEnabledChange = { enabled ->
            musicEnabled = enabled
            container.musicPreferences.musicEnabled = enabled
            if (!enabled) runCatching { previewPlayer.pause() }
        },
        onTrackSelect = { id ->
            selectedTrackId = id
            container.musicPreferences.selectedTrackId = id
            playPreview(BundledMusicCatalog.byId(id))
        },
        volumeUi = VolumeUi(
            ttsVolume = ttsVolume,
            musicVolume = musicVolume,
            generalVolume = generalVolume,
        ),
        onTtsVolumeChange = { ttsVolume = it },
        onTtsVolumeCommit = { container.ttsPreferences.volume = ttsVolume },
        onMusicVolumeChange = { newVolume ->
            musicVolume = newVolume
            // L anteprima riflette subito il volume mentre si trascina lo
            // slider, non solo al rilascio: sentire il cambio e il punto.
            runCatching { previewPlayer.setVolume(previewVolume(), previewVolume()) }
        },
        onMusicVolumeCommit = { container.musicPreferences.volume = musicVolume },
        onGeneralVolumeChange = { newVolume ->
            generalVolume = newVolume
            runCatching { previewPlayer.setVolume(previewVolume(), previewVolume()) }
        },
        onGeneralVolumeCommit = { container.audioPreferences.generalVolume = generalVolume },
        onModelsClick = onModelsClick,
        onClose = onClose,
    )
}

// Frase di test del TTS (22/07/2026, richiesta Michele): in italiano se
// l output e italiano, altrimenti in inglese per tutte le altre lingue -
// una sola frase di prova, non tradotta lingua per lingua. La locale
// segue il TESTO che verra letto, non la lingua di output configurata:
// leggere una frase inglese con voce/locale tedesca suonerebbe male.
private fun ttsTestPhrase(language: OutputLanguage): Pair<String, Locale> =
    if (language == OutputLanguage.ITALIAN) {
        "Ciao, sono il TTS di Android." to Locale.ITALIAN
    } else {
        "Hello, I am Android TTS." to Locale.ENGLISH
    }
