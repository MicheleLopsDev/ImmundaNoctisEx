package io.github.luposolitario.immundanoctisex.core.engine.choice

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChoiceAvailabilityTest {

    private fun state(
        disciplines: List<String> = emptyList(),
        items: List<GameItem> = emptyList(),
        flags: Map<String, String> = emptyMap(),
    ) = GameState(
        SessionData(
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
                    kaiDisciplines = disciplines,
                    inventory = items,
                ),
            ),
            flags = flags,
            lastUpdate = 0L,
        ),
    )

    private fun scene(
        choices: List<Choice> = emptyList(),
        disciplineChoices: List<DisciplineChoice> = emptyList(),
    ) = Scene(
        id = "1",
        sceneType = SceneType.TRANSITION,
        genre = "FANTASY",
        narrativeText = "testo",
        choices = choices,
        disciplineChoices = disciplineChoices,
    )

    private fun choice(
        id: String,
        requiredItem: String? = null,
        requiredFlag: String? = null,
        minRoll: Int? = null,
        maxRoll: Int? = null,
    ) = Choice(
        id = id,
        choiceText = "scelta $id",
        nextSceneId = "next_$id",
        minRoll = minRoll,
        maxRoll = maxRoll,
        requiredItem = requiredItem,
        requiredFlag = requiredFlag,
    )

    @Test
    fun sceltaSenzaCondizioniESempreDisponibile() {
        val available = ChoiceAvailability.available(scene(listOf(choice("a"))), state())

        assertEquals(listOf("a"), available.map { it.id })
    }

    @Test
    fun requiredItemFiltraSuPossesso() {
        val sceneWithItem = scene(listOf(choice("a", requiredItem = "Lantern")))

        assertEquals(emptyList(), ChoiceAvailability.available(sceneWithItem, state()).map { it.id })
        assertEquals(
            listOf("a"),
            ChoiceAvailability.available(
                sceneWithItem,
                state(items = listOf(GameItem(name = "Lantern", type = ItemType.SPECIAL_ITEM))),
            ).map { it.id },
        )
    }

    @Test
    fun requiredFlagSoddisfattoSoloSeNonFalse() {
        val sceneWithFlag = scene(listOf(choice("a", requiredFlag = "porta_aperta")))

        // Flag mai posto: scelta nascosta.
        assertEquals(emptyList(), ChoiceAvailability.available(sceneWithFlag, state()).map { it.id })
        // Flag posto a "false": la condizione è NEGATA, resta nascosta.
        assertEquals(
            emptyList(),
            ChoiceAvailability.available(sceneWithFlag, state(flags = mapOf("porta_aperta" to "false"))).map { it.id },
        )
        // Flag posto a "true": disponibile.
        assertEquals(
            listOf("a"),
            ChoiceAvailability.available(sceneWithFlag, state(flags = mapOf("porta_aperta" to "true"))).map { it.id },
        )
    }

    @Test
    fun leSceltteATiroNonSonoScelteNormali() {
        val mixed = scene(listOf(choice("normale"), choice("tiro", minRoll = 0, maxRoll = 4)))

        assertEquals(listOf("normale"), ChoiceAvailability.available(mixed, state()).map { it.id })
        assertEquals(listOf("tiro"), ChoiceAvailability.rollChoices(mixed).map { it.id })
    }

    @Test
    fun ilTiroApreLaPortaDelSuoIntervallo() {
        val table = scene(
            listOf(
                choice("bassa", minRoll = 0, maxRoll = 4),
                choice("alta", minRoll = 5, maxRoll = 9),
            ),
        )

        assertEquals("bassa", ChoiceAvailability.forRoll(table, 0)?.id)
        assertEquals("bassa", ChoiceAvailability.forRoll(table, 4)?.id)
        assertEquals("alta", ChoiceAvailability.forRoll(table, 5)?.id)
        assertEquals("alta", ChoiceAvailability.forRoll(table, 9)?.id)
    }

    @Test
    fun tabellaIncompletaNonBloccaIlGioco() {
        val incomplete = scene(listOf(choice("solo_bassa", minRoll = 0, maxRoll = 4)))

        assertNull(ChoiceAvailability.forRoll(incomplete, 7))
    }

    @Test
    fun disciplineSoloSePossedute() {
        val sceneWithDisciplines = scene(
            disciplineChoices = listOf(
                DisciplineChoice("d1", "SIXTH_SENSE", "senti il pericolo", "5"),
                DisciplineChoice("d2", "CAMOUFLAGE", "ti confondi", "5"),
            ),
        )

        val available = ChoiceAvailability.disciplineChoices(
            sceneWithDisciplines,
            state(disciplines = listOf("SIXTH_SENSE")),
        )

        assertEquals(listOf("d1"), available.map { it.id })
    }
}
