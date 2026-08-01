package io.github.luposolitario.immundanoctisex.core.engine.choice

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.ComparisonOperator
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.RollCondition
import io.github.luposolitario.immundanoctisex.core.data.model.RollConditionType
import io.github.luposolitario.immundanoctisex.core.data.model.RollModifier
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import kotlin.test.Test
import kotlin.test.assertEquals

// Scene.rollModifiers: il bonus condizionale che si somma al tiro della
// Tabella dei Numeri Casuali prima di cercare l'intervallo. Casi presi
// dai libri veri (vedi doc/DIARIO.md 01/08/2026).
class RollModifiersTest {

    private fun state(
        disciplines: List<String> = emptyList(),
        items: List<GameItem> = emptyList(),
        flags: Map<String, String> = emptyMap(),
        endurance: Int = 20,
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
                    currentEndurance = endurance,
                    maxEndurance = 30,
                    kaiDisciplines = disciplines,
                    inventory = items,
                ),
            ),
            flags = flags,
            lastUpdate = 0L,
        ),
    )

    private fun scene(vararg modifiers: RollModifier) = Scene(
        id = "1",
        sceneType = SceneType.TRANSITION,
        genre = "FANTASY",
        narrativeText = "testo",
        rollModifiers = modifiers.toList(),
    )

    private fun disciplina(vararg ids: String, amount: Int) = RollModifier(
        amount = amount,
        condition = RollCondition(RollConditionType.DISCIPLINE, values = ids.toList()),
    )

    @Test
    fun senzaModificatoriIlTiroRestaGrezzo() {
        assertEquals(0, RollModifiers.totalFor(scene(), state()))
    }

    @Test
    fun unModificatoreSenzaCondizioneSiApplicaSempre() {
        // "Pick a number from the Random Number Table and add 5 to it".
        assertEquals(5, RollModifiers.totalFor(scene(RollModifier(amount = 5)), state()))
    }

    @Test
    fun laDisciplinaPosseduraDaIlBonus() {
        val s = scene(disciplina("SIXTH_SENSE", amount = 2))
        assertEquals(2, RollModifiers.totalFor(s, state(disciplines = listOf("SIXTH_SENSE"))))
    }

    @Test
    fun laDisciplinaNonPosseduraNonDaNulla() {
        val s = scene(disciplina("SIXTH_SENSE", amount = 2))
        assertEquals(0, RollModifiers.totalFor(s, state(disciplines = listOf("HUNTING"))))
    }

    @Test
    fun bastaUnaDelleDisciplineElencate() {
        // "the Kai Discipline of either Mind Over Matter or Mindblast".
        val s = scene(disciplina("MIND_OVER_MATTER", "MINDBLAST", amount = 3))
        assertEquals(3, RollModifiers.totalFor(s, state(disciplines = listOf("MINDBLAST"))))
    }

    @Test
    fun unMalusSottraeDavvero() {
        // "deduct 2 from the number that you have picked".
        val s = scene(disciplina("TRACKING", amount = -2))
        assertEquals(-2, RollModifiers.totalFor(s, state(disciplines = listOf("TRACKING"))))
    }

    @Test
    fun piuModificatoriSiSommano() {
        val s = scene(
            RollModifier(amount = 1),
            disciplina("HUNTING", amount = 2),
            disciplina("HEALING", amount = 4), // non posseduta
        )
        assertEquals(3, RollModifiers.totalFor(s, state(disciplines = listOf("HUNTING"))))
    }

    @Test
    fun enduranceSottoSogliaDaIlMalus() {
        // "If your current ENDURANCE point total is less than 10, deduct 3".
        val s = scene(
            RollModifier(
                amount = -3,
                condition = RollCondition(
                    RollConditionType.ENDURANCE,
                    operator = ComparisonOperator.LT,
                    threshold = 10,
                ),
            ),
        )
        assertEquals(-3, RollModifiers.totalFor(s, state(endurance = 8)))
        assertEquals(0, RollModifiers.totalFor(s, state(endurance = 10)))
    }

    @Test
    fun enduranceSopraSogliaDaIlBonus() {
        // "If your current ENDURANCE point total is greater than 20, add 1".
        val s = scene(
            RollModifier(
                amount = 1,
                condition = RollCondition(
                    RollConditionType.ENDURANCE,
                    operator = ComparisonOperator.GT,
                    threshold = 20,
                ),
            ),
        )
        assertEquals(1, RollModifiers.totalFor(s, state(endurance = 25)))
        assertEquals(0, RollModifiers.totalFor(s, state(endurance = 20)))
    }

    @Test
    fun loggettoPossedutoDaIlBonus() {
        // "If you possess either a Pick or a Shovel, add 2".
        val s = scene(
            RollModifier(
                amount = 2,
                condition = RollCondition(RollConditionType.ITEM, values = listOf("Pick", "Shovel")),
            ),
        )
        val zaino = listOf(GameItem(name = "Shovel", type = ItemType.BACKPACK_ITEM))
        assertEquals(2, RollModifiers.totalFor(s, state(items = zaino)))
        assertEquals(0, RollModifiers.totalFor(s, state()))
    }

    @Test
    fun ilFlagSeguuLaStessaRegolaDiRequiredFlag() {
        // Posto a "false" NEGA la condizione; mai posto non la soddisfa.
        val s = scene(
            RollModifier(
                amount = 2,
                condition = RollCondition(RollConditionType.FLAG, values = listOf("baknar_oil")),
            ),
        )
        assertEquals(2, RollModifiers.totalFor(s, state(flags = mapOf("baknar_oil" to "true"))))
        assertEquals(0, RollModifiers.totalFor(s, state(flags = mapOf("baknar_oil" to "false"))))
        assertEquals(0, RollModifiers.totalFor(s, state()))
    }

    @Test
    fun unaCondizioneIncompletaNonBloccaIlGioco() {
        // ENDURANCE senza operator/threshold: pacchetto scritto male
        // (il validatore lo segnala), qui semplicemente non si applica.
        val s = scene(
            RollModifier(amount = 9, condition = RollCondition(RollConditionType.ENDURANCE)),
        )
        assertEquals(0, RollModifiers.totalFor(s, state()))
    }

    @Test
    fun activeForElencaSoloQuelliScattati() {
        val s = scene(
            disciplina("HUNTING", amount = 2),
            disciplina("HEALING", amount = 4),
        )
        val attivi = RollModifiers.activeFor(s, state(disciplines = listOf("HUNTING")))
        assertEquals(listOf(2), attivi.map { it.amount })
    }
}
