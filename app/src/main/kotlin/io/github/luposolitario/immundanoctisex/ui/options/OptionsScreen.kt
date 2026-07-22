package io.github.luposolitario.immundanoctisex.ui.options

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctisex.util.AccentColor
import io.github.luposolitario.immundanoctisex.util.NarrativeTone
import io.github.luposolitario.immundanoctisex.util.OutputLanguage
import io.github.luposolitario.immundanoctisex.util.ReadingFont
import io.github.luposolitario.immundanoctisex.util.StatusCardColor

// Opzioni (UI.md schermata 7): tema, font, lingua, TTS, link ai modelli.
// Componente stateless — la Route legge/scrive le preferenze vere.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(
    darkOverride: Boolean?,
    onThemeSelect: (Boolean?) -> Unit,
    accentColor: AccentColor,
    onAccentColorSelect: (AccentColor) -> Unit,
    statusCardColor: StatusCardColor,
    onStatusCardColorSelect: (StatusCardColor) -> Unit,
    readingFont: ReadingFont,
    onFontSelect: (ReadingFont) -> Unit,
    boldText: Boolean,
    onBoldTextChange: (Boolean) -> Unit,
    outputLanguage: OutputLanguage,
    onLanguageSelect: (OutputLanguage) -> Unit,
    narrativeTone: NarrativeTone,
    onToneSelect: (NarrativeTone) -> Unit,
    ttsUi: TtsUi,
    onAutoReadChange: (Boolean) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onSpeechRateCommit: () -> Unit,
    onPitchChange: (Float) -> Unit,
    onPitchCommit: () -> Unit,
    onMaleVoiceSelect: (String?) -> Unit,
    onFemaleVoiceSelect: (String?) -> Unit,
    onTestMaleVoice: () -> Unit,
    onTestFemaleVoice: () -> Unit,
    musicUi: MusicUi,
    onMusicEnabledChange: (Boolean) -> Unit,
    onTrackSelect: (String) -> Unit,
    volumeUi: VolumeUi,
    onTtsVolumeChange: (Float) -> Unit,
    onTtsVolumeCommit: () -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onMusicVolumeCommit: () -> Unit,
    onEffectsVolumeChange: (Float) -> Unit,
    onEffectsVolumeCommit: () -> Unit,
    onGeneralVolumeChange: (Float) -> Unit,
    onGeneralVolumeCommit: () -> Unit,
    onModelsClick: () -> Unit,
    onClose: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Opzioni") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeSection(darkOverride, onThemeSelect)
            AccentColorSection(accentColor, onAccentColorSelect)
            StatusCardColorSection(statusCardColor, onStatusCardColorSelect)
            LanguageSection(outputLanguage, onLanguageSelect)
            ToneSection(narrativeTone, onToneSelect)
            FontSection(readingFont, onFontSelect, boldText, onBoldTextChange)
            TtsSection(
                ui = ttsUi,
                onAutoReadChange = onAutoReadChange,
                onSpeechRateChange = onSpeechRateChange,
                onSpeechRateCommit = onSpeechRateCommit,
                onPitchChange = onPitchChange,
                onPitchCommit = onPitchCommit,
                onMaleVoiceSelect = onMaleVoiceSelect,
                onFemaleVoiceSelect = onFemaleVoiceSelect,
                onTestMaleVoice = onTestMaleVoice,
                onTestFemaleVoice = onTestFemaleVoice,
            )
            MusicSection(
                ui = musicUi,
                onMusicEnabledChange = onMusicEnabledChange,
                onTrackSelect = onTrackSelect,
            )
            VolumeSection(
                ui = volumeUi,
                onTtsVolumeChange = onTtsVolumeChange,
                onTtsVolumeCommit = onTtsVolumeCommit,
                onMusicVolumeChange = onMusicVolumeChange,
                onMusicVolumeCommit = onMusicVolumeCommit,
                onEffectsVolumeChange = onEffectsVolumeChange,
                onEffectsVolumeCommit = onEffectsVolumeCommit,
                onGeneralVolumeChange = onGeneralVolumeChange,
                onGeneralVolumeCommit = onGeneralVolumeCommit,
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Modelli LLM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onModelsClick) { Text("Gestisci modelli scaricati") }
                }
            }
        }
    }
}

// null = segui il sistema (ThemePreferences.darkOverride).
@Composable
private fun ThemeSection(darkOverride: Boolean?, onSelect: (Boolean?) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Tema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ThemeOption("Segui il sistema", darkOverride == null) { onSelect(null) }
            ThemeOption("Chiaro", darkOverride == false) { onSelect(false) }
            ThemeOption("Scuro", darkOverride == true) { onSelect(true) }
        }
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun LanguageSection(selected: OutputLanguage, onSelect: (OutputLanguage) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Lingua della narrazione", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutputLanguage.entries.forEach { language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = language == selected, onClick = { onSelect(language) })
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = language == selected, onClick = { onSelect(language) })
                    Text(language.displayName, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Opzioni", heightDp = 1400)
@Composable
private fun OptionsScreenPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        OptionsScreen(
            darkOverride = null,
            onThemeSelect = {},
            accentColor = AccentColor.BLUE,
            onAccentColorSelect = {},
            statusCardColor = StatusCardColor.DEFAULT,
            onStatusCardColorSelect = {},
            readingFont = ReadingFont.SERIF,
            onFontSelect = {},
            boldText = false,
            onBoldTextChange = {},
            outputLanguage = OutputLanguage.ITALIAN,
            onLanguageSelect = {},
            narrativeTone = NarrativeTone.AUTHOR,
            onToneSelect = {},
            ttsUi = TtsUi(
                autoReadEnabled = false,
                speechRate = 1f,
                pitch = 1f,
                maleVoices = emptyList(),
                selectedMaleVoice = null,
                femaleVoices = emptyList(),
                selectedFemaleVoice = null,
            ),
            onAutoReadChange = {},
            onSpeechRateChange = {},
            onSpeechRateCommit = {},
            onPitchChange = {},
            onPitchCommit = {},
            onMaleVoiceSelect = {},
            onFemaleVoiceSelect = {},
            onTestMaleVoice = {},
            onTestFemaleVoice = {},
            musicUi = MusicUi(
                musicEnabled = false,
                tracks = io.github.luposolitario.immundanoctisex.util.BundledMusicCatalog.TRACKS,
                selectedTrackId = io.github.luposolitario.immundanoctisex.util.BundledMusicCatalog.default.id,
            ),
            onMusicEnabledChange = {},
            onTrackSelect = {},
            volumeUi = VolumeUi(ttsVolume = 0.75f, musicVolume = 0.15f, effectsVolume = 0.7f, generalVolume = 0.8f),
            onTtsVolumeChange = {},
            onTtsVolumeCommit = {},
            onMusicVolumeChange = {},
            onMusicVolumeCommit = {},
            onEffectsVolumeChange = {},
            onEffectsVolumeCommit = {},
            onGeneralVolumeChange = {},
            onGeneralVolumeCommit = {},
            onModelsClick = {},
            onClose = {},
        )
    }
}
