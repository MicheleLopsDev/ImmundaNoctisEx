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
            // BUG (22/07/2026, Michele: "cambiando il tono non succede
            // nulla"): remember(manifest) da solo non bastava — lingua e
            // tono venivano letti UNA VOLTA alla creazione del narratore,
            // e manifest non cambia mai durante la sessione. Se le Opzioni
            // cambiavano senza uno smontaggio/rimontaggio completo di
            // questa route, il narratore restava quello vecchio. Ora sono
            // chiavi esplicite del remember: quando cambiano, il
            // narratore si ricrea, qualunque sia il percorso di
            // navigazione che ha portato al cambio.
            val userLanguage = container.languagePreferences.outputLanguage.promptValue
            val toneOverride = container.narrativeTonePreferences.narrativeTone.hints
            val narrator = remember(manifest, userLanguage, toneOverride) {
                SceneNarrator(
                    engine = container.inferenceEngine,
                    promptBuilder = PromptBuilder(promptFragments(context)),
                    manifest = manifest,
                    userLanguage = userLanguage,
                    // AUTHOR (default) -> null, l'autore decide come sempre.
                    toneOverride = toneOverride,
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
                boldText = container.fontPreferences.boldText,
                statusCardColor = container.statusCardColorPreferences.statusCardColor,
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
