package io.github.luposolitario.immundanoctisex.ui.options

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
import io.github.luposolitario.immundanoctisex.util.OutputLanguage
import io.github.luposolitario.immundanoctisex.util.ParchmentStyle
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
    var parchmentStyle by remember { mutableStateOf(container.parchmentPreferences.style) }
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
    // La preferenza GREZZA, non effectiveTrack (24/07/2026): effectiveTrack
    // degrada sul default per un id sconosciuto, e "random" non è un id
    // di TRACKS — con effectiveTrack.id il picker avrebbe mostrato il
    // default invece di "Casuale" riaprendo le Opzioni a shuffle attivo.
    var selectedTrackId by remember {
        mutableStateOf(container.musicPreferences.selectedTrackId ?: BundledMusicCatalog.default.id)
    }

    // I quattro volumi (22/07/2026, richiesta Michele): TTS, musica ed
    // effetti hanno ciascuno il proprio, il generale moltiplica tutti e
    // tre - vive in AudioPreferences perche non appartiene a nessuno dei
    // tre. Gli effetti sono arrivati dopo ("la barra con il volume dei
    // suoni deve essere una barra a parte").
    var ttsVolume by remember { mutableStateOf(container.ttsPreferences.volume) }
    var musicVolume by remember { mutableStateOf(container.musicPreferences.volume) }
    var effectsVolume by remember { mutableStateOf(container.soundEffectPreferences.volume) }
    var generalVolume by remember { mutableStateOf(container.audioPreferences.generalVolume) }

    // Player a scope APPLICAZIONE (container.musicPlayer, non piu locale
    // a questa Route): Michele 22/07/2026, "quando esco dalle opzioni
    // smette di suonare" - un MediaPlayer dentro OptionsRoute moriva col
    // DisposableEffect appena si usciva. Qui si legge/scrive solo lo
    // stato, il player vero vive nel container e sopravvive alla
    // navigazione.
    fun effectiveMusicVolume(): Float = (musicVolume * generalVolume).coerceIn(0f, 1f)

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
        parchmentStyle = parchmentStyle,
        onParchmentStyleSelect = { selected ->
            parchmentStyle = selected
            container.parchmentPreferences.style = selected
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
            tracks = BundledMusicCatalog.PICKER_ENTRIES,
            selectedTrackId = selectedTrackId,
        ),
        onMusicEnabledChange = { enabled ->
            musicEnabled = enabled
            container.musicPreferences.musicEnabled = enabled
            // BUG (Michele 22/07/2026, "quando abilito non parte a meno
            // che non cambio canzone"): mancava di avviare la traccia
            // gia selezionata, si limitava a mettere in pausa quando si
            // spegneva - non faceva mai partire nulla quando si accendeva.
            if (enabled) {
                container.musicPlayer.playConfigured(container.musicPreferences, effectiveMusicVolume())
            } else {
                container.musicPlayer.pause()
            }
        },
        onTrackSelect = { id ->
            selectedTrackId = id
            container.musicPreferences.selectedTrackId = id
            // playConfigured, non più play(byId(id)) diretto (24/07/2026):
            // "id" può essere anche BundledMusicCatalog.RANDOM_ID, che non
            // è un file vero — playConfigured sa distinguerlo e avvia la
            // modalità casuale invece di caricare un asset inesistente.
            container.musicPlayer.playConfigured(container.musicPreferences, effectiveMusicVolume())
            // Scegliere una traccia vuol dire volerla sentire (Michele:
            // "seleziono una combo e questa parte per provarla") - se lo
            // switch era spento si accende da solo, cosi lo stato visibile
            // non mente su cosa sta suonando davvero.
            if (!musicEnabled) {
                musicEnabled = true
                container.musicPreferences.musicEnabled = true
            }
        },
        volumeUi = VolumeUi(
            ttsVolume = ttsVolume,
            musicVolume = musicVolume,
            effectsVolume = effectsVolume,
            generalVolume = generalVolume,
        ),
        onTtsVolumeChange = { ttsVolume = it },
        onTtsVolumeCommit = { container.ttsPreferences.volume = ttsVolume },
        onMusicVolumeChange = { newVolume ->
            musicVolume = newVolume
            // Il volume cambia subito mentre si trascina lo slider, non
            // solo al rilascio: sentire il cambio e il punto.
            container.musicPlayer.setVolume(effectiveMusicVolume())
        },
        onMusicVolumeCommit = { container.musicPreferences.volume = musicVolume },
        onEffectsVolumeChange = { effectsVolume = it },
        onEffectsVolumeCommit = { container.soundEffectPreferences.volume = effectsVolume },
        onGeneralVolumeChange = { newVolume ->
            generalVolume = newVolume
            container.musicPlayer.setVolume(effectiveMusicVolume())
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
