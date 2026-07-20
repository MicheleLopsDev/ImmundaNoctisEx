package io.github.luposolitario.immundanoctisex.core.data.session

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.JourneyEntry
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.model.Transition
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileSessionStoreTest {

    private val directory: File = createTempDirectory("immunda-test").toFile()
    private val store = FileSessionStore(directory)

    @AfterTest
    fun cleanup() {
        directory.deleteRecursively()
    }

    private fun session(packageId: String = "sample", lastUpdate: Long = 1L) = SessionData(
        saveFormatVersion = 1,
        packageId = packageId,
        packageVersion = "1.0",
        difficulty = Difficulty.IRON,
        currentSceneId = "3",
        characters = listOf(
            Character(
                role = CharacterRole.HERO,
                name = "Lupo di prova",
                baseCombatSkill = 15,
                currentEndurance = 18,
                maxEndurance = 20,
            ),
        ),
        journey = listOf(
            JourneyEntry("1", "testo", Transition.ChoiceTaken("c1")),
        ),
        lastUpdate = lastUpdate,
    )

    @Test
    fun salvaERicaricaLaFotografiaCompleta() {
        store.saveSession(session())

        val loaded = store.loadSession("sample")

        assertEquals(session(), loaded)
    }

    @Test
    fun autoSaveSovrascriveIlPrecedente() {
        store.saveSession(session(lastUpdate = 1L))
        store.saveSession(session(lastUpdate = 2L).copy(currentSceneId = "5"))

        assertEquals("5", store.loadSession("sample")?.currentSceneId)
    }

    @Test
    fun laScritturaNonLasciaFileTemporanei() {
        store.saveSession(session())

        assertEquals(emptyList(), directory.listFiles().orEmpty().filter { it.name.endsWith(".tmp") })
    }

    @Test
    fun salvataggioCorrottoDegradaANessunSalvataggio() {
        File(directory, "session_sample.json").writeText("{ json rotto")

        assertNull(store.loadSession("sample"))
    }

    @Test
    fun listSessionsOrdinaPerUltimoAggiornamento() {
        store.saveSession(session(packageId = "vecchio", lastUpdate = 1L))
        store.saveSession(session(packageId = "recente", lastUpdate = 99L))

        assertEquals(listOf("recente", "vecchio"), store.listSessions().map { it.packageId })
    }

    @Test
    fun ilCheckpointSiScriveUnaVoltaSola() {
        assertTrue(store.saveCheckpoint(session(), slot = 1))
        assertFalse(store.saveCheckpoint(session().copy(currentSceneId = "9"), slot = 1))

        // Il contenuto resta quello del PRIMO piazzamento (mai spostabile).
        assertEquals("3", store.loadCheckpoint("sample", 1)?.currentSceneId)
    }

    // Lo store resta una porta NEUTRA: rileggere non consuma. Il consumo
    // (una vita per ricaricamento, deciso il 20/07/2026) e' una regola di
    // gioco e vive in AdventureState, che chiama deleteCheckpoint.
    @Test
    fun rileggereUnCheckpointNonLoConsuma() {
        store.saveCheckpoint(session(), slot = 1)

        repeat(3) {
            assertEquals(session(), store.loadCheckpoint("sample", 1))
        }
    }

    @Test
    fun deleteCheckpointBruciaSoloQuelloSlot() {
        store.saveCheckpoint(session(), slot = 1)
        store.saveCheckpoint(session(), slot = 2)

        store.deleteCheckpoint("sample", 1)

        assertNull(store.loadCheckpoint("sample", 1), "lo slot usato sparisce")
        assertEquals(session(), store.loadCheckpoint("sample", 2), "gli altri restano")
    }

    @Test
    fun deleteCheckpointDiUnoSlotVuotoNonEsplode() {
        store.deleteCheckpoint("sample", 1)
        assertNull(store.loadCheckpoint("sample", 1))
    }

    // Bruciato lo slot, quel numero NON si riusa per un piazzamento
    // nuovo: il budget lo conta la sessione (checkpointsUsed), non la
    // presenza del file. Altrimenti si rigenererebbero vite all'infinito.
    @Test
    fun unoSlotBruciatoTornaScrivibile() {
        store.saveCheckpoint(session(), slot = 1)
        store.deleteCheckpoint("sample", 1)

        assertTrue(store.saveCheckpoint(session(lastUpdate = 99L), slot = 1))
    }

    @Test
    fun deleteAdventureCancellaSessioneECheckpoint() {
        store.saveSession(session())
        store.saveCheckpoint(session(), slot = 1)
        store.saveCheckpoint(session(), slot = 2)
        store.saveSession(session(packageId = "altro"))

        store.deleteAdventure("sample")

        assertNull(store.loadSession("sample"))
        assertNull(store.loadCheckpoint("sample", 1))
        assertNull(store.loadCheckpoint("sample", 2))
        assertEquals("altro", store.listSessions().single().packageId)
    }
}
