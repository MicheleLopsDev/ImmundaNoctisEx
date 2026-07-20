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
import io.github.luposolitario.immundanoctisex.core.engine.ending.AdventureEnding
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
            // Il grafo con la GARANZIA che una scena di morte esista: da qui
            // in giù tutti lavorano sullo stesso manifest completato, così
            // non esistono due verità sul finale.
            val manifest = remember(loadResult.manifest) {
                AdventureEnding.withGuaranteedEnding(loadResult.manifest)
            }
            val narrator = remember(manifest) {
                SceneNarrator(
                    engine = container.inferenceEngine,
                    promptBuilder = PromptBuilder(promptFragments(context)),
                    manifest = manifest,
                    // Scelta in Opzioni (UI.md schermata 7); prima era
                    // "Italian" fisso nel default del costruttore.
                    userLanguage = container.languagePreferences.outputLanguage.promptValue,
                )
            }
            // Si sa subito se il modello è sul telefono: serve a non
            // mostrare il testo originale durante il caricamento.
            val modelPresent = remember {
                container.modelPreferences.isDownloaded(container.modelPreferences.selectedModel)
            }
            val state = remember(currentSession) {
                AdventureState(
                    manifest = manifest,
                    session = currentSession,
                    dice = container.diceRoller,
                    store = container.sessionStore,
                    narrator = narrator,
                    scope = scope,
                    expectsNarration = modelPresent,
                )
            }

            // Il modello si carica alla prima scena e poi resta caricato.
            // Se non parte, si degrada sul testo del pacchetto.
            LaunchedEffect(state) {
                if (container.ensureModelLoaded()) {
                    state.startNarration(previousSceneText = null)
                } else {
                    state.narrationUnavailable()
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
                readingFont = container.fontPreferences.readingFont.family,
                initialTextScale = container.fontPreferences.textScale,
                onTextScaleChange = { container.fontPreferences.textScale = it },
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
