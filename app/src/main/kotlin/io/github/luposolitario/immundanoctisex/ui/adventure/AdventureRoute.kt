package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.luposolitario.immundanoctisex.AppContainer
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageLoadResult
import io.github.luposolitario.immundanoctisex.inference.PromptBuilder
import io.github.luposolitario.immundanoctisex.inference.PromptFragments
import io.github.luposolitario.immundanoctisex.inference.SceneNarrator

// Raccordo dell'Avventura: carica il pacchetto, costruisce lo stato di
// gioco dalla sessione (nuova o ripresa dall'auto-save) e monta la scena.
@Composable
fun AdventureRoute(
    container: AppContainer,
    session: SessionData,
    onExitToHome: () -> Unit,
) {
    val context = LocalContext.current
    val loadResult = remember { container.packageRepository.load() }

    when (loadResult) {
        is PackageLoadResult.Success -> {
            // La ricarica di un checkpoint ripristina la fotografia (diario
            // già troncato per costruzione) e ricrea lo stato di gioco.
            var currentSession by remember { mutableStateOf(session) }
            val scope = rememberCoroutineScope()
            val narrator = remember(loadResult.manifest) {
                SceneNarrator(
                    engine = container.inferenceEngine,
                    promptBuilder = PromptBuilder(promptFragments(context)),
                    manifest = loadResult.manifest,
                )
            }
            val state = remember(currentSession) {
                AdventureState(
                    manifest = loadResult.manifest,
                    session = currentSession,
                    dice = container.diceRoller,
                    store = container.sessionStore,
                    narrator = narrator,
                    scope = scope,
                )
            }

            // Il modello si carica alla prima scena e poi resta caricato.
            // Se manca, il gioco prosegue col testo del pacchetto.
            LaunchedEffect(state) {
                if (container.ensureModelLoaded()) {
                    state.startNarration(previousSceneText = null)
                }
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

// I frammenti del prompt vivono in content/config.json, montato negli
// asset dell'APK. Config assente o rotta -> default hardcoded.
private fun promptFragments(context: android.content.Context): PromptFragments =
    runCatching {
        context.assets.open("config.json").bufferedReader().use { it.readText() }
    }.map { PromptFragments.fromConfig(it) }
        .getOrDefault(PromptFragments.DEFAULTS)
