package io.github.luposolitario.immundanoctisex.ui.options

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// Quel che serve alla card per disegnarsi (stato sollevato, come
// AdvancedSettingsCard): i nomi voce sono quelli restituiti dal motore
// TTS di sistema, la Route li recupera interrogando TtsService.
data class TtsUi(
    val autoReadEnabled: Boolean,
    val speechRate: Float,
    val pitch: Float,
    val maleVoices: List<String>,
    val selectedMaleVoice: String?,
    val femaleVoices: List<String>,
    val selectedFemaleVoice: String?,
)

// Preferenze TTS (UI.md schermata 7): auto-lettura, velocità, pitch,
// voce per genere. Il servizio che legge ad alta voce vive nel flusso
// della scena (AdventureState/AdventureRoute, Tappa 2 fatta il
// 22/07/2026) — qui si configura solo come dovrà suonare. Il volume
// vive in VolumeSection, non qui (raccolto insieme a musica e generale).
@Composable
fun TtsSection(
    ui: TtsUi,
    onAutoReadChange: (Boolean) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onSpeechRateCommit: () -> Unit,
    onPitchChange: (Float) -> Unit,
    onPitchCommit: () -> Unit,
    onMaleVoiceSelect: (String?) -> Unit,
    onFemaleVoiceSelect: (String?) -> Unit,
    // Test rapido (22/07/2026, richiesta Michele): una frase fissa detta
    // con la voce configurata per quel genere, per sentire subito
    // l'effetto dei cursori sopra senza dover entrare in partita.
    onTestMaleVoice: () -> Unit = {},
    onTestFemaleVoice: () -> Unit = {},
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Lettura ad alta voce (TTS)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (ui.autoReadEnabled) "Auto-lettura attiva" else "Auto-lettura spenta")
                Switch(checked = ui.autoReadEnabled, onCheckedChange = onAutoReadChange)
            }

            Column {
                Text("Velocità", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = ui.speechRate,
                    onValueChange = onSpeechRateChange,
                    onValueChangeFinished = onSpeechRateCommit,
                    valueRange = 0.5f..2f,
                )
            }

            Column {
                Text("Tono (pitch)", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = ui.pitch,
                    onValueChange = onPitchChange,
                    onValueChangeFinished = onPitchCommit,
                    valueRange = 0.5f..2f,
                )
            }

            HorizontalDivider()
            VoicePicker("Voce maschile", ui.maleVoices, ui.selectedMaleVoice, onMaleVoiceSelect, onTestMaleVoice)
            Spacer(Modifier.height(4.dp))
            VoicePicker("Voce femminile", ui.femaleVoices, ui.selectedFemaleVoice, onFemaleVoiceSelect, onTestFemaleVoice)
        }
    }
}

// Selezione voce di sistema per genere: se il telefono non ne ha per la
// lingua scelta, la lista è vuota e si degrada sulla voce di default
// del motore (TtsService.speak) — nessun blocco, solo meno scelta.
@Composable
private fun VoicePicker(
    label: String,
    voices: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onTest: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                TextButton(onClick = { expanded = voices.isNotEmpty() }) {
                    Text(selected ?: if (voices.isEmpty()) "Nessuna voce disponibile" else "Automatica")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Automatica") }, onClick = { onSelect(null); expanded = false })
                    voices.forEach { voice ->
                        DropdownMenuItem(text = { Text(voice) }, onClick = { onSelect(voice); expanded = false })
                    }
                }
            }
            IconButton(onClick = onTest) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Prova voce")
            }
        }
    }
}

@Preview(showBackground = true, name = "TTS")
@Composable
private fun TtsSectionPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        TtsSection(
            ui = TtsUi(
                autoReadEnabled = true,
                speechRate = 1f,
                pitch = 1f,
                maleVoices = listOf("it-it-x-iol-local"),
                selectedMaleVoice = null,
                femaleVoices = listOf("it-it-x-ist-local"),
                selectedFemaleVoice = "it-it-x-ist-local",
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
        )
    }
}
