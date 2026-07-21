package io.github.luposolitario.immundanoctisex.ui.options

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// Quel che serve alla card per disegnarsi (stato sollevato, come TtsUi).
data class MusicUi(
    val musicEnabled: Boolean,
    val trackName: String?,
    val volume: Float,
)

// Musica di sottofondo (UI.md schermata 7): traccia MP3 scelta
// dall'utente e volume. SOLO configurazione (22/07/2026): la
// riproduzione vera durante la partita arriva in un passo separato —
// qui si configura solo come dovrà suonare quando ci arriverà, stesso
// trattamento di TtsSection prima della Tappa 2.
@Composable
fun MusicSection(
    ui: MusicUi,
    onMusicEnabledChange: (Boolean) -> Unit,
    onPickTrack: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeCommit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Musica di sottofondo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (ui.musicEnabled) "Musica attiva" else "Musica spenta")
                Switch(checked = ui.musicEnabled, onCheckedChange = onMusicEnabledChange)
            }

            OutlinedButton(onClick = onPickTrack, modifier = Modifier.fillMaxWidth()) {
                Text(ui.trackName ?: "Scegli file MP3")
            }

            Column {
                Text("Volume", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = ui.volume,
                    onValueChange = onVolumeChange,
                    onValueChangeFinished = onVolumeCommit,
                    valueRange = 0f..1f,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Musica")
@Composable
private fun MusicSectionPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        MusicSection(
            ui = MusicUi(musicEnabled = true, trackName = "tema_avventura.mp3", volume = 0.5f),
            onMusicEnabledChange = {},
            onPickTrack = {},
            onVolumeChange = {},
            onVolumeCommit = {},
        )
    }
}
