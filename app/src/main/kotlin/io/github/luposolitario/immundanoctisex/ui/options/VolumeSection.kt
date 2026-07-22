package io.github.luposolitario.immundanoctisex.ui.options

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// Quel che serve alla card per disegnarsi (stato sollevato, come TtsUi/
// MusicUi).
data class VolumeUi(
    val ttsVolume: Float,
    val musicVolume: Float,
    val effectsVolume: Float,
    val generalVolume: Float,
)

// Quattro volumi indipendenti raccolti in un solo posto (22/07/2026,
// richiesta Michele: prima "3 barre volume" — voce/musica/generale — poi
// "la barra con il volume dei suoni deve essere una barra a parte": gli
// effetti sonori (dado, passi, mangiare/bere...) usavano solo il generale,
// senza un proprio controllo). Il generale moltiplica gli altri tre
// (TtsService.speak, la riproduzione musica, SoundEffectPlayer), non li
// sostituisce.
@Composable
fun VolumeSection(
    ui: VolumeUi,
    onTtsVolumeChange: (Float) -> Unit,
    onTtsVolumeCommit: () -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onMusicVolumeCommit: () -> Unit,
    onEffectsVolumeChange: (Float) -> Unit,
    onEffectsVolumeCommit: () -> Unit,
    onGeneralVolumeChange: (Float) -> Unit,
    onGeneralVolumeCommit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Volumi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            VolumeSlider("Voce (TTS)", ui.ttsVolume, onTtsVolumeChange, onTtsVolumeCommit)
            VolumeSlider("Musica", ui.musicVolume, onMusicVolumeChange, onMusicVolumeCommit)
            VolumeSlider("Effetti sonori", ui.effectsVolume, onEffectsVolumeChange, onEffectsVolumeCommit)
            VolumeSlider("Generale", ui.generalVolume, onGeneralVolumeChange, onGeneralVolumeCommit)
        }
    }
}

@Composable
private fun VolumeSlider(label: String, value: Float, onChange: (Float) -> Unit, onCommit: () -> Unit) {
    Column {
        Text("$label — ${(value * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, onValueChangeFinished = onCommit, valueRange = 0f..1f)
    }
}

@Preview(showBackground = true, name = "Volumi")
@Composable
private fun VolumeSectionPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        VolumeSection(
            ui = VolumeUi(ttsVolume = 0.75f, musicVolume = 0.15f, effectsVolume = 0.7f, generalVolume = 0.8f),
            onTtsVolumeChange = {},
            onTtsVolumeCommit = {},
            onMusicVolumeChange = {},
            onMusicVolumeCommit = {},
            onEffectsVolumeChange = {},
            onEffectsVolumeCommit = {},
            onGeneralVolumeChange = {},
            onGeneralVolumeCommit = {},
        )
    }
}
