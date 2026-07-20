package io.github.luposolitario.immundanoctisex.inference

import android.util.Log
import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.Gender
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.engine.ending.AdventureEnding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Cosa la UI riceve mentre la scena si genera.
sealed interface NarrationEvent {
    // Testo da mostrare finora (già ripulito del blocco tag).
    data class Streaming(val textSoFar: String) : NarrationEvent
    // Fine: prosa definitiva + scelte tradotte (o fallback).
    data class Completed(val scene: EnrichedScene) : NarrationEvent
}

// Orchestra il giro completo di UNA scena: compone il prompt, apre una
// sessione nuova, genera in streaming, e a fine corsa consegna il
// risultato parsato. È il punto in cui "il resto dell'app non sa che
// esiste Gemma" (ARCHITETTURA §inference): chiede di arricchire una
// scena e riceve testo.
//
// REGOLA NON NEGOZIABILE: qualunque cosa vada storta (modello assente,
// motore che non parte, generazione vuota) NON blocca il gioco — si
// consegna il testo originale del pacchetto.
class SceneNarrator(
    private val engine: InferenceEngine,
    private val promptBuilder: PromptBuilder,
    private val manifest: Manifest,
    private val userLanguage: String = "Italian",
    // Scelto in Opzioni (NarrativeTonePreferences, 21/07/2026): null =
    // l'autore decide (comportamento di sempre), altrimenti SOSTITUISCE
    // i toneHints della scena per tutta la sessione — il giocatore vince
    // esplicitamente, non si mischia coi toni dell'autore.
    private val toneOverride: List<String>? = null,
) {

    // Se il motore e' pronto la scena verra' RIscritta da Gemma: la UI
    // lo usa per non mostrare il testo originale in attesa.
    val isReady: Boolean get() = engine.isLoaded

    // Lo stato del contesto viene dal motore: la UI lo mostra nel
    // semaforo senza sapere chi lo produce.
    val tokenInfo: TokenInfo get() = engine.tokenInfo.value

    fun narrate(
        scene: Scene,
        previousSceneText: String?,
        choices: List<Choice>,
        disciplineChoices: List<DisciplineChoice>,
        playerGender: Gender,
    ): Flow<NarrationEvent> = flow {
        // Senza motore pronto si degrada subito: il giocatore legge il
        // testo del libro invece di restare davanti a una schermata vuota.
        if (!engine.isLoaded) {
            emit(NarrationEvent.Completed(fallback(scene, choices, disciplineChoices)))
            return@flow
        }

        val prompt = promptBuilder.build(
            PromptContext(
                scene = scene,
                previousSceneText = previousSceneText,
                continuations = continuationsOf(scene),
                choices = choices,
                disciplineChoices = disciplineChoices,
                sourceLanguage = manifest.language,
                userLanguage = userLanguage,
                genre = scene.genre.ifBlank { manifest.genre },
                toneHints = toneOverride ?: scene.toneHints.ifEmpty { manifest.toneHints },
                playerGender = playerGender,
                isSyntheticEnding = scene.id == AdventureEnding.SYNTHETIC_DEFEAT_SCENE_ID,
            ),
        )

        // Diagnostica (21/07/2026, richiesta Michele: "voglio provare il
        // prompt su Gemma in locale sul PC"): il prompt intero, per
        // copiarlo fuori dal device. logChunked perché logcat tronca in
        // silenzio oltre ~4000 caratteri per riga, e il prompt (col
        // dizionario delle 24 location) li supera.
        logChunked(TAG, "PROMPT completo", prompt)

        // Sessione NUOVA per ogni scena: inferenza senza memoria.
        engine.newSession()

        val raw = StringBuilder()
        var lastEmitted = ""
        runCatching {
            engine.generate(prompt).collect { chunk ->
                raw.append(chunk)
                // Si mostra solo ciò che precede il separatore: le righe
                // dei tag non devono mai comparire a schermo.
                val visible = ResponseParser.narrativeOf(raw.toString())
                if (visible != lastEmitted) {
                    lastEmitted = visible
                    emit(NarrationEvent.Streaming(visible))
                }
            }
        }.onFailure {
            emit(NarrationEvent.Completed(fallback(scene, choices, disciplineChoices)))
            return@flow
        }

        val rawText = raw.toString()
        val parsed = ResponseParser.parse(rawText, scene)
        // Diagnostica per l'esperimento IMAGE (21/07/2026): senza questo
        // log non c'è modo di distinguere "Gemma non ha scritto la riga"
        // da "l'ha scritta in un formato che il parser scarta" — i due
        // casi degradano allo stesso modo (sfondo di default) e da fuori
        // sembrano identici. adb logcat -s SceneNarrator.
        logChunked(
            TAG,
            "IMAGE risolto=${parsed.backgroundImage} — blocco tag",
            rawText.substringAfter(ResponseParser.SEPARATOR, missingDelimiterValue = "(separatore assente)").trim(),
        )
        emit(NarrationEvent.Completed(parsed))
    }

    // Le continuazioni servono al modello per non contraddire il seguito;
    // sono testo delle scene raggiungibili, mai rivelato al giocatore.
    private fun continuationsOf(scene: Scene): List<String> {
        val destinations = buildList {
            scene.choices.forEach { add(it.nextSceneId) }
            scene.disciplineChoices.forEach { add(it.nextSceneId) }
            scene.combat?.let { combat ->
                add(combat.winSceneId)
                combat.loseSceneId?.let { add(it) }
                combat.evadeSceneId?.let { add(it) }
            }
        }.distinct()
        return destinations.mapNotNull { id ->
            manifest.scenes.firstOrNull { it.id == id }?.narrativeText
        }
    }

    // La degradazione: prosa e scelte originali del pacchetto.
    private fun fallback(
        scene: Scene,
        choices: List<Choice>,
        disciplineChoices: List<DisciplineChoice>,
    ) = EnrichedScene(
        narrative = scene.narrativeText,
        choiceTexts = choices.associate { it.id to it.choiceText },
        disciplineChoiceTexts = disciplineChoices.associate { it.id to it.choiceText },
        enemyName = scene.combat?.enemyName,
        backgroundImage = scene.backgroundImage,
    )

    private companion object {
        const val TAG = "SceneNarrator"
    }
}

// logcat tronca in silenzio oltre ~4000 caratteri per riga: un prompt con
// il dizionario delle location li supera abbondantemente. Si spezza in
// pezzi numerati così si ricostruiscono copiandoli in ordine.
// runCatching: android.util.Log non è mockato nei test JVM del modulo
// (nessun Robolectric, per scelta — vedi CLAUDE.md), un log non deve mai
// far fallire un test.
private fun logChunked(tag: String, label: String, text: String) {
    val chunkSize = 3500
    val chunks = text.chunked(chunkSize)
    runCatching {
        chunks.forEachIndexed { index, chunk ->
            Log.i(tag, "$label [${index + 1}/${chunks.size}]\n$chunk")
        }
    }
}
