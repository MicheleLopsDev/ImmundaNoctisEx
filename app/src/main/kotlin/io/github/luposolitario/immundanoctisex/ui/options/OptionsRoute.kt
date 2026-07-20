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
    onModelsClick: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    var themeOverride by remember { mutableStateOf(darkOverride) }
    var font by remember { mutableStateOf(container.fontPreferences.readingFont) }
    var bold by remember { mutableStateOf(container.fontPreferences.boldText) }
    var language by remember { mutableStateOf(container.languagePreferences.outputLanguage) }
    var tone by remember { mutableStateOf(container.narrativeTonePreferences.narrativeTone) }

    var autoRead by remember { mutableStateOf(container.ttsPreferences.autoReadEnabled) }
    var speechRate by remember { mutableStateOf(container.ttsPreferences.speechRate) }
    var pitch by remember { mutableStateOf(container.ttsPreferences.pitch) }
    var maleVoice by remember { mutableStateOf(container.ttsPreferences.voiceFor(Gender.MALE)) }
    var femaleVoice by remember { mutableStateOf(container.ttsPreferences.voiceFor(Gender.FEMALE)) }

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
        onModelsClick = onModelsClick,
        onClose = onClose,
    )
}
