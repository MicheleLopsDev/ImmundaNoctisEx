package io.github.luposolitario.immundanoctisex.tool.inference

import io.github.luposolitario.immundanoctisex.core.data.model.Gender
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.engine.inference.EnrichedScene
import io.github.luposolitario.immundanoctisex.core.engine.inference.LinguaOutput
import io.github.luposolitario.immundanoctisex.core.engine.inference.PromptBuilder
import io.github.luposolitario.immundanoctisex.core.engine.inference.PromptContext
import io.github.luposolitario.immundanoctisex.core.engine.inference.ResponseParser
import io.github.luposolitario.immundanoctisex.core.engine.inference.ripulisciTokenDiServizio
import io.github.luposolitario.immundanoctisex.tool.editor.EditorLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Traduce (o arricchisce) UNA scena mostrando all'autore ciò che vedrà
// il giocatore (02/08/2026, Michele: "dobbiamo aggiungere le versioni
// modificate dai modelli se questi sono attivi per tutte le stringhe
// che traduciamo").
//
// La scelta che conta: **non si traduce stringa per stringa**. Si manda
// la SCENA INTERA con lo stesso `PromptBuilder` del client e si scompone
// la risposta con lo stesso `ResponseParser` — testo narrato, testo di
// ogni scelta, scelte-disciplina e nome del nemico escono già separati.
// Tradurre i campi uno per uno darebbe un risultato diverso da quello
// vero: nel gioco il modello vede la scena tutta insieme, e le scelte le
// rende sapendo che cosa racconta il testo sopra.
//
// Le continuazioni (il testo delle scene raggiungibili) entrano nel
// prompt come nel client: servono al modello per non contraddire il
// seguito, e non compaiono mai in ciò che si legge.
class TraduttoreScene(private val motore: EditorInferenceEngine) {

    // Il motore è uno solo e non è rientrante: due traduzioni lanciate
    // insieme (due schede aperte, o un doppio clic) si mangerebbero la
    // sessione a vicenda. Si aspetta il proprio turno.
    private val turno = Mutex()

    val disponibile: Boolean get() = motore.isLoaded

    suspend fun traduci(
        scene: Scene,
        manifest: Manifest,
        lingua: LinguaOutput,
        modalitaTraduzione: Boolean,
        onParziale: (String) -> Unit = {},
    ): Result<EnrichedScene> {
        if (!motore.isLoaded) {
            return Result.failure(IllegalStateException("Modello non caricato: aprilo dalla schermata Modello."))
        }
        return turno.withLock {
            runCatching {
                val prompt = PromptBuilder(
                    // L'editor non chiede mai a Gemma di scegliere lo
                    // sfondo: qui l'immagine la decide l'autore, ed è
                    // proprio lui che sta guardando la scheda.
                    askImageInPrompt = false,
                    translationMode = modalitaTraduzione,
                ).build(
                    PromptContext(
                        scene = scene,
                        // Nessuna scena precedente: nell'editor si guarda
                        // una scena per conto suo, non una partita in
                        // corso. È l'unica differenza dichiarata rispetto
                        // al prompt del gioco.
                        previousSceneText = null,
                        continuations = continuazioniDi(scene, manifest),
                        choices = scene.choices,
                        disciplineChoices = scene.disciplineChoices,
                        sourceLanguage = manifest.language,
                        userLanguage = lingua.promptValue,
                        genre = scene.genre.ifBlank { manifest.genre },
                        toneHints = scene.toneHints.ifEmpty { manifest.toneHints },
                        // L'eroe dell'anteprima è maschile: il genere
                        // cambia solo gli accordi grammaticali, e l'autore
                        // qui sta valutando la resa, non la partita.
                        playerGender = Gender.MALE,
                    ),
                )

                val grezzo = StringBuilder()
                motore.newSession()
                val inizio = System.currentTimeMillis()
                motore.generate(prompt).collect { pezzo ->
                    grezzo.append(pezzo)
                    onParziale(ResponseParser.narrativeOf(ripulisciTokenDiServizio(grezzo.toString())))
                }
                val secondi = (System.currentTimeMillis() - inizio) / 1000.0
                EditorLog.i(TAG, "Scena ${scene.id} tradotta in ${"%.1f".format(secondi)} s su ${motore.activeBackend}")

                val testo = ripulisciTokenDiServizio(grezzo.toString())
                if (testo.isBlank()) error("Il modello non ha prodotto testo.")
                ResponseParser.parse(testo, scene)
            }.onFailure { EditorLog.e(TAG, "Traduzione della scena ${scene.id} fallita: ${it.message}", it) }
        }
    }

    // Le stesse continuazioni che costruisce SceneNarrator nel client.
    private fun continuazioniDi(scene: Scene, manifest: Manifest): List<String> {
        val destinazioni = buildList {
            scene.choices.forEach { add(it.nextSceneId) }
            scene.disciplineChoices.forEach { add(it.nextSceneId) }
            scene.combat?.let { combat ->
                add(combat.winSceneId)
                combat.loseSceneId?.let { add(it) }
                combat.evadeSceneId?.let { add(it) }
            }
        }.distinct()
        return destinazioni.mapNotNull { id ->
            manifest.scenes.firstOrNull { it.id == id }?.narrativeText
        }
    }

    private companion object {
        const val TAG = "TraduttoreScene"
    }
}
