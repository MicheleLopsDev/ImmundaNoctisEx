package io.github.luposolitario.immundanoctisex.inference

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Gender
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Motore finto: risposte decise dal test, nessun modello, nessun device.
// È anche il pezzo che permette di sviluppare la UI senza caricare
// ogni volta 3,66 GB.
private class FakeEngine(
    private val loaded: Boolean = true,
    private val chunks: List<String> = emptyList(),
    private val failMidway: Boolean = false,
) : InferenceEngine {
    var sessionsOpened = 0
        private set
    var lastPrompt: String? = null
        private set

    override val tokenInfo: StateFlow<TokenInfo> = MutableStateFlow(TokenInfo())
    override val isLoaded: Boolean get() = loaded

    override suspend fun load(modelFile: File, config: InferenceConfig) = Result.success(Unit)

    override suspend fun newSession() {
        sessionsOpened++
    }

    override fun generate(prompt: String): Flow<String> = flow {
        lastPrompt = prompt
        chunks.forEach { emit(it) }
        if (failMidway) throw IllegalStateException("motore caduto")
    }

    override suspend fun unload() = Unit
}

class SceneNarratorTest {

    private val scene = Scene(
        id = "3",
        sceneType = SceneType.TRANSITION,
        genre = "FANTASY",
        toneHints = listOf("dark"),
        narrativeText = "The alley narrows as the old quarter swallows the daylight.",
        choices = listOf(Choice(id = "c1", choiceText = "Walk on", nextSceneId = "4")),
    )

    private val nextScene = Scene(
        id = "4",
        sceneType = SceneType.TRANSITION,
        genre = "FANTASY",
        narrativeText = "The thugs step out of the shadows.",
    )

    private val manifest = Manifest(
        id = "sample",
        version = "1.0",
        title = "Test",
        description = "",
        language = "English",
        genre = "FANTASY",
        scenes = listOf(scene, nextScene),
    )

    private fun narrator(engine: InferenceEngine) =
        SceneNarrator(engine, PromptBuilder(), manifest)

    private fun run(engine: InferenceEngine) = runBlocking {
        narrator(engine).narrate(
            scene = scene,
            previousSceneText = "You left the inn.",
            choices = scene.choices,
            disciplineChoices = emptyList(),
            playerGender = Gender.MALE,
        ).toList()
    }

    @Test
    fun senzaMotoreCaricato_degradaSulTestoOriginale() {
        val events = run(FakeEngine(loaded = false))

        val completed = events.filterIsInstance<NarrationEvent.Completed>().single()
        assertEquals(scene.narrativeText, completed.scene.narrative)
        assertEquals("Walk on", completed.scene.choiceTexts["c1"])
    }

    @Test
    fun motoreCheCadeAMeta_degradaSenzaPropagareEccezioni() {
        val events = run(FakeEngine(chunks = listOf("Il vicolo si stringe"), failMidway = true))

        val completed = events.filterIsInstance<NarrationEvent.Completed>().single()
        assertEquals(scene.narrativeText, completed.scene.narrative)
    }

    @Test
    fun streamingProgressivoPoiRisultatoParsato() {
        val engine = FakeEngine(
            chunks = listOf(
                "Il vicolo ",
                "si stringe.",
                "\n--- TAGS ---\n",
                "CHOICE|4|1|Avanza, la mano sull'arma",
            ),
        )

        val events = run(engine)

        val streaming = events.filterIsInstance<NarrationEvent.Streaming>()
        assertEquals("Il vicolo", streaming.first().textSoFar)
        assertEquals("Il vicolo si stringe.", streaming.last().textSoFar)

        val completed = events.filterIsInstance<NarrationEvent.Completed>().single()
        assertEquals("Il vicolo si stringe.", completed.scene.narrative)
        assertEquals("Avanza, la mano sull'arma", completed.scene.choiceTexts["c1"])
    }

    @Test
    fun leRigheDeiTagNonCompaionoMaiNelloStreaming() {
        val engine = FakeEngine(
            chunks = listOf("Prosa.", "\n--- TAGS ---\n", "CHOICE|4|1|Avanza"),
        )

        val streaming = run(engine).filterIsInstance<NarrationEvent.Streaming>()

        assertTrue(streaming.none { it.textSoFar.contains("CHOICE|") })
        assertTrue(streaming.none { it.textSoFar.contains("--- TAGS ---") })
    }

    @Test
    fun ogniScenaApreUnaSessioneNuova() {
        val engine = FakeEngine(chunks = listOf("Prosa."))

        run(engine)

        assertEquals(1, engine.sessionsOpened)
    }

    @Test
    fun ilPromptPortaLeContinuazioniMaNonIlDiario() {
        val engine = FakeEngine(chunks = listOf("Prosa."))

        run(engine)

        val prompt = requireNotNull(engine.lastPrompt)
        // La scena raggiungibile entra come continuazione...
        assertTrue(prompt.contains(nextScene.narrativeText))
        // ...e la coda della scena precedente come contesto.
        assertTrue(prompt.contains("You left the inn."))
    }
}
