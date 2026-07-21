package io.github.luposolitario.immundanoctisex.ui.options

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import io.github.luposolitario.immundanoctisex.util.StatusCardColor

// Raccordo delle Opzioni: legge le preferenze da AppContainer, le scrive
// ad ogni cambio (stessa politica "commit al rilascio" di
// AdvancedSettingsCard per gli slider — non ad ogni frame di trascinamento).
// Il tema è un caso a parte: vive nello stato di MainActivity, non solo
// nella preference, perché deve applicarsi SUBITO senza riavviare l'app.
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
    // trackName parte già valorizzato con la traccia inclusa se l'utente
    // non ne ha ancora scelta una sua (MusicPreferences.effectiveTrackName).
    var trackName by remember { mutableStateOf(container.musicPreferences.effectiveTrackName) }
    var volume by remember { mutableStateOf(container.musicPreferences.volume) }
    val trackPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        val name = context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?: uri.lastPathSegment
            ?: "Traccia selezionata"
        trackName = name
        container.musicPreferences.selectedTrackUri = uri.toString()
        container.musicPreferences.selectedTrackName = name
    }

    // Le voci disponibili si sanno solo dopo l'inizializzazione del
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
        musicUi = MusicUi(
            musicEnabled = musicEnabled,
            trackName = trackName,
            volume = volume,
        ),
        onMusicEnabledChange = { enabled ->
            musicEnabled = enabled
            container.musicPreferences.musicEnabled = enabled
        },
        onPickTrack = { trackPickerLauncher.launch(arrayOf("audio/*")) },
        onVolumeChange = { volume = it },
        onVolumeCommit = { container.musicPreferences.volume = volume },
        onModelsClick = onModelsClick,
        onClose = onClose,
    )
}
