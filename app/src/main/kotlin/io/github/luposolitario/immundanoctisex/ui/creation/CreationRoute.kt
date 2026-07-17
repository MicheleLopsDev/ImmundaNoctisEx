package io.github.luposolitario.immundanoctisex.ui.creation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.luposolitario.immundanoctisex.AppContainer
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageLoadResult

// Raccordo della creazione personaggio: carica il pacchetto, costruisce la
// SessionData iniziale, la salva (primo auto-save) e consegna la partita.
@Composable
fun CreationRoute(
    container: AppContainer,
    difficulty: Difficulty,
    onSessionCreated: (SessionData) -> Unit,
) {
    val loadResult = remember { container.packageRepository.load() }
    val state = remember { CreationState(container.diceRoller) }

    when (loadResult) {
        is PackageLoadResult.Success -> CharacterCreationScreen(
            state = state,
            onCreate = {
                val startSceneId = container.packageRepository.startScene()?.id ?: return@CharacterCreationScreen
                val session = state.buildSession(loadResult.manifest, difficulty, startSceneId)
                container.sessionStore.saveSession(session)
                onSessionCreated(session)
            },
        )

        // Pacchetto rotto: messaggio semplice, il gioco non crasha mai.
        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Impossibile caricare il libro (pacchetto non valido).")
        }
    }
}
