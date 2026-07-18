package io.github.luposolitario.immundanoctisex.core.engine

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.JourneyEntry
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.model.Transition
import io.github.luposolitario.immundanoctisex.core.data.session.FileSessionStore
import io.github.luposolitario.immundanoctisex.core.data.session.SessionStore
import io.github.luposolitario.immundanoctisex.core.engine.dice.FixedDiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.mechanics.MechanicsExecutor
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import io.github.luposolitario.immundanoctisex.core.engine.transition.TransitionEngine
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// MILESTONE FASE 3 automatizzata: il ciclo di vita della partita che sul
// Razr si prova a mano — gioca -> auto-save atomico -> chiusura e RIPRESA
// -> checkpoint -> ricarica -> morte in IRON che CANCELLA. Riproduce la
// logica di persistenza di AdventureState (che vive in :app e non è
// testabile da JVM) usando lo store vero su directory temporanea.
class CicloVitaPartitaTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val directory: File = createTempDirectory("immunda-lifecycle").toFile()
    private val store: SessionStore = FileSessionStore(directory)

    @AfterTest
    fun cleanup() {
        directory.deleteRecursively()
    }

    private fun manifest(): Manifest {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream("scenes.sample.json")) {
            "content/scenes.sample.json non trovato sul classpath di test"
        }
        return json.decodeFromString(stream.bufferedReader().use { it.readText() })
    }

    private fun newSession(difficulty: Difficulty, endurance: Int = 24) = SessionData(
        saveFormatVersion = 1,
        packageId = "sample-adventure",
        packageVersion = "1.0.0",
        difficulty = difficulty,
        currentSceneId = "1",
        characters = listOf(
            Character(
                role = CharacterRole.HERO,
                name = "Lupo di prova",
                baseCombatSkill = 15,
                currentEndurance = endurance,
                maxEndurance = endurance,
            ),
        ),
        lastUpdate = 1L,
    )

    // Come AdventureState.moveTo: transizione + voce diario + auto-save.
    private fun advance(
        state: GameState,
        engine: TransitionEngine,
        manifest: Manifest,
        targetSceneId: String,
    ) {
        val fromScene = manifest.scenes.first { it.id == state.currentSceneId }
        state.addJourneyEntry(JourneyEntry(fromScene.id, fromScene.narrativeText, Transition.ChoiceTaken("c")))
        engine.transitionTo(state, targetSceneId)
        store.saveSession(state.snapshot().copy(lastUpdate = System.currentTimeMillis()))
    }

    @Test
    fun chiusuraARiapertura_riprendeDoveEravamo() {
        val manifest = manifest()
        val engine = TransitionEngine(manifest, MechanicsExecutor(FixedDiceRoller(emptyList())))
        val state = GameState(newSession(Difficulty.NORMAL))
        store.saveSession(state.snapshot())

        advance(state, engine, manifest, "2")
        advance(state, engine, manifest, "3")

        // "L'app si chiude": si riparte SOLO da ciò che è su disco.
        val resumed = requireNotNull(store.loadSession("sample-adventure"))
        assertEquals("3", resumed.currentSceneId)
        assertEquals(2, resumed.journey.size)
    }

    @Test
    fun checkpoint_piazzatoRicaricabileEImmutabile() {
        val manifest = manifest()
        val engine = TransitionEngine(manifest, MechanicsExecutor(FixedDiceRoller(emptyList())))
        val state = GameState(newSession(Difficulty.NORMAL))

        advance(state, engine, manifest, "2")
        // Piazza il checkpoint sullo slot 1: fotografia della scena 2.
        assertTrue(store.saveCheckpoint(state.snapshot(), slot = 1))

        // Prosegue e "muore" (scena arbitraria); poi ricarica il checkpoint.
        advance(state, engine, manifest, "3")
        val checkpoint = requireNotNull(store.loadCheckpoint("sample-adventure", 1))
        assertEquals("2", checkpoint.currentSceneId)

        // Immutabile: un secondo piazzamento sullo stesso slot è rifiutato.
        assertEquals(false, store.saveCheckpoint(state.snapshot(), slot = 1))
        assertEquals("2", store.loadCheckpoint("sample-adventure", 1)?.currentSceneId)
    }

    @Test
    fun morteInIron_cancellaSessioneECheckpoint() {
        val manifest = manifest()
        val engine = TransitionEngine(manifest, MechanicsExecutor(FixedDiceRoller(emptyList())))
        // In IRON il budget checkpoint è 0, ma verifichiamo comunque che
        // deleteAdventure spazzi via tutto (auto-save incluso).
        val state = GameState(newSession(Difficulty.IRON))
        store.saveSession(state.snapshot())
        store.saveCheckpoint(state.snapshot(), slot = 1) // difesa: non deve sopravvivere

        advance(state, engine, manifest, "2")

        // Morte in IRON -> la sessione si cancella, il libro riparte da capo.
        store.deleteAdventure("sample-adventure")

        assertNull(store.loadSession("sample-adventure"))
        assertNull(store.loadCheckpoint("sample-adventure", 1))
        assertEquals(emptyList(), store.listSessions())
    }

    @Test
    fun autoSalvataggioSempreAtomico_nessunFileTemporaneoResiduo() {
        val manifest = manifest()
        val engine = TransitionEngine(manifest, MechanicsExecutor(FixedDiceRoller(emptyList())))
        val state = GameState(newSession(Difficulty.NORMAL))

        advance(state, engine, manifest, "2")
        advance(state, engine, manifest, "3")

        assertEquals(emptyList(), directory.listFiles().orEmpty().filter { it.name.endsWith(".tmp") })
    }
}
