package io.github.luposolitario.immundanoctisex.ui.options

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import io.github.luposolitario.immundanoctisex.util.BundledTrack

// Quel che serve alla card per disegnarsi (stato sollevato, come TtsUi).
data class MusicUi(
    val musicEnabled: Boolean,
    val tracks: List<BundledTrack>,
    val selectedTrackId: String,
    val volume: Float,
)

// Musica di sottofondo (UI.md schermata 7): quale delle tracce incluse è
// attiva e a che volume. Menu a tendina, non un selettore di file
// (Michele 22/07/2026: "una combo con le canzoni che ho fatto, non un
// picker") — selezionare una traccia la fa partire subito per
// un'anteprima (gestita da OptionsRoute, qui solo l'evento in uscita).
// Il collegamento vero durante la partita arriva in un passo separato.
@Composable
fun MusicSection(
    ui: MusicUi,
    onMusicEnabledChange: (Boolean) -> Unit,
    onTrackSelect: (String) -> Unit,
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

            TrackPicker(tracks = ui.tracks, selectedId = ui.selectedTrackId, onSelect = onTrackSelect)

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

@Composable
private fun TrackPicker(tracks: List<BundledTrack>, selectedId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = tracks.firstOrNull { it.id == selectedId }?.displayName ?: selectedId
    Box {
        TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            tracks.forEach { track ->
                DropdownMenuItem(
                    text = { Text(track.displayName) },
                    onClick = {
                        expanded = false
                        onSelect(track.id)
                    },
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
            ui = MusicUi(
                musicEnabled = true,
                tracks = listOf(
                    BundledTrack("esplorazione", "Esplorazione", "music/esplorazione.mp3"),
                    BundledTrack("combattimento", "Combattimento", "music/combattimento.mp3"),
                ),
                selectedTrackId = "esplorazione",
                volume = 0.15f,
            ),
            onMusicEnabledChange = {},
            onTrackSelect = {},
            onVolumeChange = {},
            onVolumeCommit = {},
        )
    }
}
