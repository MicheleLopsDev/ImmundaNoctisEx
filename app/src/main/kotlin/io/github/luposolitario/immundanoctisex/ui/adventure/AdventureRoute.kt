package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.luposolitario.immundanoctisex.AppContainer
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageLoadResult
import io.github.luposolitario.immundanoctisex.core.engine.ending.AdventureEnding
import io.github.luposolitario.immundanoctisex.core.engine.inference.PromptBuilder
import io.github.luposolitario.immundanoctisex.inference.SceneNarrator
import io.github.luposolitario.immundanoctisex.tts.TtsService
import io.github.luposolitario.immundanoctisex.util.locale
import kotlinx.coroutines.launch

// Raccordo dell'Avventura: carica il pacchetto, costruisce lo stato di
// gioco dalla sessione (nuova o ripresa dall'auto-save) e monta la scena.
@Composable
fun AdventureRoute(
    container: AppContainer,
    session: SessionData,
    onExitToHome: () -> Unit,
    // Tema EFFETTIVAMENTE in uso (override incluso): serve solo a
    // risolvere lo stile AUTO della pergamena (23/07/2026).
    isDarkTheme: Boolean = false,
) {
    val context = LocalContext.current
    val loadResult = remember { container.packageRepository.load() }

    when (loadResult) {
        is PackageLoadResult.Success -> {
            // La ricarica di un checkpoint ripristina la fotografia (diario
            // già troncato per costruzione) e ricrea lo stato di gioco.
            var currentSession by remember { mutableStateOf(session) }
            // Tasto rapido musica nell'header (24/07/2026, richiesta
            // Michele: "un tasto per spegnere o attivare la musica senza
            // andare in menu") — stesso pattern di currentSession: stato
            // qui perché solo la Route ha accesso a container.musicPlayer.
            var musicEnabled by remember { mutableStateOf(container.musicPreferences.musicEnabled) }
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
            // askImageInPrompt (27/07/2026, Michele: "vorrei che fosse una
            // cosa configurabile e magari deselezionabile dal menu LLM") è
            // una chiave esplicita come le altre: cambiarla in Modelli LLM
            // ricrea il narratore, effetto dalla prossima scena.
            val askImageInPrompt = container.inferencePreferences.askImageInPrompt
            // Modalità Traduzione (31/07/2026, Michele): stessa forma di
            // askImageInPrompt sopra — chiave esplicita del remember, così
            // il narratore si ricrea (testo diverso dalla prossima scena)
            // appena cambia in Opzioni avanzate Modelli LLM.
            val translationMode = container.inferencePreferences.translationMode
            val narrator = remember(manifest, userLanguage, toneOverride, askImageInPrompt, translationMode) {
                SceneNarrator(
                    engine = container.inferenceEngine,
                    promptBuilder = PromptBuilder(askImageInPrompt, translationMode),
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
            // TTS (Tappa 2, 22/07/2026): connesso una volta sola per tutta
            // la vita della route, indipendentemente da manifest/tono —
            // a differenza del narratore non ha bisogno di ricrearsi
            // quando cambiano le preferenze di narrazione.
            val ttsService = remember { TtsService(context) {} }
            DisposableEffect(Unit) {
                onDispose { ttsService.shutdown() }
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
                    ttsService = ttsService,
                    autoReadEnabled = container.ttsPreferences.autoReadEnabled,
                    userLocale = container.languagePreferences.outputLanguage.locale,
                    soundEffectPlayer = container.soundEffectPlayer,
                    // BUG (01/08/2026): ogni scena (non solo la prima)
                    // deve poter aspettare un caricamento del motore già
                    // in corso — vedi il commento su AdventureState per
                    // il dettaglio.
                    ensureEngineLoaded = { container.ensureModelLoaded() },
                )
            }

            // Il modello si carica alla prima scena e poi resta caricato.
            // Tre esiti distinti (01/08/2026, Michele): già pronto -> si
            // gioca subito; qualcun altro lo sta già caricando (es.
            // l'auto-load di AppNavigation.kt) -> si aspetta senza
            // avviarne un secondo (ensureModelLoaded aspetta il lock
            // condiviso di AppContainer); spento e nessuno lo sta
            // caricando -> si chiede al giocatore invece di forzare
            // un'attesa di 15-20s in silenzio.
            LaunchedEffect(state) {
                val model = container.modelPreferences.selectedModel
                when {
                    container.isModelReady(model) || container.isModelLoading -> {
                        if (container.ensureModelLoaded()) {
                            state.startNarration(previousSceneText = null)
                        } else {
                            state.narrationUnavailable()
                        }
                    }
                    !container.modelPreferences.isDownloaded(model) -> state.narrationUnavailable()
                    else -> state.awaitEngineChoice()
                }
            }
            AdventureScreen(
                state = state,
                onExitToHome = onExitToHome,
                onStartEngineNow = {
                    state.beginLoadingEngineNow()
                    scope.launch {
                        if (container.ensureModelLoaded()) {
                            state.startNarration(previousSceneText = null)
                        } else {
                            state.narrationUnavailable()
                        }
                    }
                },
                onSkipEngine = { state.narrationUnavailable() },
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
                parchmentStyle = container.parchmentPreferences.style,
                isDarkTheme = isDarkTheme,
                autoReadEnabled = container.ttsPreferences.autoReadEnabled,
                onReadAloud = state::readAloud,
                musicEnabled = musicEnabled,
                onMusicToggle = {
                    val next = !musicEnabled
                    musicEnabled = next
                    container.musicPreferences.musicEnabled = next
                    if (next) {
                        val volume = (container.musicPreferences.volume * container.audioPreferences.generalVolume)
                            .coerceIn(0f, 1f)
                        container.musicPlayer.playConfigured(container.musicPreferences, volume)
                    } else {
                        container.musicPlayer.pause()
                    }
                },
                diceColor = container.diceColorPreferences.diceColor,
            )
        }

        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Impossibile caricare il libro (pacchetto non valido).")
        }
    }
}
