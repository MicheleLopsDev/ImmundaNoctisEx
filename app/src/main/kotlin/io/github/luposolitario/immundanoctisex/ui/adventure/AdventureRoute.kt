package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.luposolitario.immundanoctisex.AppContainer
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageLoadResult

// Raccordo dell'Avventura: carica il pacchetto, costruisce lo stato di
// gioco dalla sessione (nuova o ripresa dall'auto-save) e monta la scena.
@Composable
fun AdventureRoute(
    container: AppContainer,
    session: SessionData,
    onExitToHome: () -> Unit,
) {
    val loadResult = remember { container.packageRepository.load() }

    when (loadResult) {
        is PackageLoadResult.Success -> {
            // La ricarica di un checkpoint ripristina la fotografia (diario
            // già troncato per costruzione) e ricrea lo stato di gioco.
            var currentSession by remember { mutableStateOf(session) }
            val state = remember(currentSession) {
                AdventureState(
                    manifest = loadResult.manifest,
                    session = currentSession,
                    dice = container.diceRoller,
                    store = container.sessionStore,
                )
            }
            AdventureScreen(
                state = state,
                onExitToHome = onExitToHome,
                onReloadCheckpoint = { slot ->
                    state.loadCheckpoint(slot)?.let { checkpoint ->
                        container.sessionStore.saveSession(checkpoint)
                        currentSession = checkpoint
                    }
                },
            )
        }

        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Impossibile caricare il libro (pacchetto non valido).")
        }
    }
}
