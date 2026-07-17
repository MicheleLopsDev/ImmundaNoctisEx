package io.github.luposolitario.immundanoctisex.core.engine.state

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.JourneyEntry
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.model.Transition
import kotlin.test.Test
import kotlin.test.assertEquals

class GameStateTest {

    private fun session() = SessionData(
        saveFormatVersion = 1,
        packageId = "sample",
        packageVersion = "1.0",
        difficulty = Difficulty.NORMAL,
        currentSceneId = "1",
        characters = listOf(
            Character(
                role = CharacterRole.HERO,
                name = "Eroe di prova",
                baseCombatSkill = 15,
                currentEndurance = 20,
                maxEndurance = 20,
            ),
        ),
        lastUpdate = 0L,
    )

    @Test
    fun updateHeroTrasformaSoloLEroe() {
        val state = GameState(session())

        state.updateHero { it.copy(currentEndurance = 12) }

        assertEquals(12, state.hero.currentEndurance)
        assertEquals("Eroe di prova", state.hero.name)
    }

    @Test
    fun flagEVariabiliTipizzate() {
        val state = GameState(session())

        state.setFlag("traditore_smascherato", "true")
        state.setVariable("sospetto", 3)
        state.updateVariable("sospetto", 4)

        assertEquals("true", state.flag("traditore_smascherato"))
        assertEquals(7, state.variable("sospetto"))
        assertEquals(0, state.variable("mai_scritta"))
    }

    @Test
    fun snapshotFotografaLoStatoCorrente() {
        val state = GameState(session())

        state.moveTo("2")
        state.addJourneyEntry(
            JourneyEntry(sceneId = "1", enrichedText = "testo", transition = Transition.ChoiceTaken("c1")),
        )
        val snapshot = state.snapshot()

        assertEquals("2", snapshot.currentSceneId)
        assertEquals(1, snapshot.journey.size)
        assertEquals("1", snapshot.journey.single().sceneId)
    }
}
