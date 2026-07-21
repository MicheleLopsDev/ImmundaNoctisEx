package io.github.luposolitario.immundanoctisex.core.engine.mechanics

import io.github.luposolitario.immundanoctisex.core.data.model.AutoJumpReason
import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.GameMechanic
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.model.StatType
import io.github.luposolitario.immundanoctisex.core.engine.dice.FixedDiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MechanicsExecutorTest {

    private fun state(
        disciplines: List<String> = emptyList(),
        items: List<GameItem> = emptyList(),
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
                    maxEndurance = 20,
                    kaiDisciplines = disciplines,
                    inventory = items,
                ),
            ),
            lastUpdate = 0L,
        ),
    )

    private fun executor(vararg rolls: Int) = MechanicsExecutor(FixedDiceRoller(rolls.toList()))

    private fun mechanic(command: String, params: JsonObject) = GameMechanic(command, params)

    @Test
    fun addItemEntraConTipoEQuantita() {
        val state = state()
        val params = buildJsonObject {
            put("itemType", "BACKPACK_ITEM"); put("itemName", "Meal"); put("quantity", "2")
        }

        executor().execute(state, listOf(mechanic("addItem", params)))

        assertEquals(2, Inventory.countOf(state.hero, "Meal"))
    }

    @Test
    fun rollForQuantityAggiungeOroBasePiuTiro() {
        val state = state()
        val params = buildJsonObject { put("item", "Gold Crowns"); put("baseValue", "2") }

        executor(5).execute(state, listOf(mechanic("rollForQuantity", params)))

        assertEquals(7, Inventory.countOf(state.hero, "Gold Crowns"))
    }

    @Test
    fun rollOnItemTableTrovaOggettoNellIntervallo() {
        val state = state()
        val params = buildJsonObject {
            putJsonArray("outcomes") {
                add(buildJsonObject { put("minRoll", 0); put("maxRoll", 4) })
                add(buildJsonObject {
                    put("minRoll", 5); put("maxRoll", 9)
                    put("itemName", "Dagger"); put("itemType", "WEAPON")
                })
            }
        }

        executor(7).execute(state, listOf(mechanic("rollOnItemTable", params)))

        assertEquals(1, Inventory.countOf(state.hero, "Dagger"))
    }

    @Test
    fun rollOnItemTableIntervalloVuotoNonTrovaNiente() {
        val state = state()
        val params = buildJsonObject {
            putJsonArray("outcomes") {
                add(buildJsonObject { put("minRoll", 0); put("maxRoll", 9) })
            }
        }

        executor(3).execute(state, listOf(mechanic("rollOnItemTable", params)))

        assertEquals(0, state.hero.inventory.size)
    }

    @Test
    fun healStatFullRiportaAlMassimo() {
        val state = state(endurance = 8)
        val params = buildJsonObject { put("statName", "ENDURANCE"); put("amount", "FULL") }

        executor().execute(state, listOf(mechanic("healStat", params)))

        assertEquals(20, state.hero.currentEndurance)
    }

    @Test
    fun statModifierSuEnduranceEDannoDiretto() {
        val state = state(endurance = 10)
        val params = buildJsonObject { put("statName", "ENDURANCE"); put("amount", "-4") }

        executor().execute(state, listOf(mechanic("applyStatModifier", params)))

        assertEquals(6, state.hero.currentEndurance)
    }

    @Test
    fun statModifierSuCombatSkillDiventaModificatore() {
        val state = state()
        val params = buildJsonObject { put("statName", "COMBAT_SKILL"); put("amount", "-2") }

        executor().execute(state, listOf(mechanic("applyStatModifier", params)))

        val modifier = state.hero.activeModifiers.single()
        assertEquals(StatType.COMBAT_SKILL, modifier.stat)
        assertEquals(-2, modifier.amount)
    }

    @Test
    fun requireActionConHuntingNonCostaNulla() {
        val state = state(disciplines = listOf("HUNTING"))
        val params = buildJsonObject {
            put("action", "EAT_MEAL"); put("penaltyStat", "ENDURANCE"); put("penaltyValue", "-3")
        }

        executor().execute(state, listOf(mechanic("requireAction", params)))

        assertEquals(20, state.hero.currentEndurance)
    }

    @Test
    fun requireActionConsumaUnPasto() {
        val state = state(items = listOf(GameItem(name = "Meal", type = ItemType.BACKPACK_ITEM, quantity = 2)))
        val params = buildJsonObject {
            put("action", "EAT_MEAL"); put("penaltyStat", "ENDURANCE"); put("penaltyValue", "-3")
        }

        executor().execute(state, listOf(mechanic("requireAction", params)))

        assertEquals(1, Inventory.countOf(state.hero, "Meal"))
        // Già a 20/20: verifica anche che +1 dal pasto non sfori il massimo.
        assertEquals(20, state.hero.currentEndurance)
    }

    @Test
    fun requireActionMangiareCuraUnPuntoSottoIlMassimo() {
        val state = state(endurance = 15, items = listOf(GameItem(name = "Meal", type = ItemType.BACKPACK_ITEM, quantity = 1)))
        val params = buildJsonObject {
            put("action", "EAT_MEAL"); put("penaltyStat", "ENDURANCE"); put("penaltyValue", "-3")
        }

        executor().execute(state, listOf(mechanic("requireAction", params)))

        assertEquals(0, Inventory.countOf(state.hero, "Meal"))
        assertEquals(16, state.hero.currentEndurance)
    }

    @Test
    fun requireActionConHuntingNonCura() {
        // HUNTING auto-soddisfa a costo zero: non consuma un pasto vero,
        // quindi non deve guadagnare la cura che spetta solo a chi mangia.
        val state = state(disciplines = listOf("HUNTING"), endurance = 15)
        val params = buildJsonObject {
            put("action", "EAT_MEAL"); put("penaltyStat", "ENDURANCE"); put("penaltyValue", "-3")
        }

        executor().execute(state, listOf(mechanic("requireAction", params)))

        assertEquals(15, state.hero.currentEndurance)
    }

    @Test
    fun requireActionSenzaPastoApplicaLaPenalita() {
        val state = state(endurance = 10)
        val params = buildJsonObject {
            put("action", "EAT_MEAL"); put("penaltyStat", "ENDURANCE"); put("penaltyValue", "-3")
        }

        executor().execute(state, listOf(mechanic("requireAction", params)))

        assertEquals(7, state.hero.currentEndurance)
    }

    @Test
    fun setGlobalVarNumericoVaNelleVariabili() {
        val state = state()
        val params = buildJsonObject { put("varName", "sospetto"); put("value", "3"); put("operation", "SET") }
        val update = buildJsonObject { put("varName", "sospetto"); put("value", "2"); put("operation", "ADD") }

        executor().execute(state, listOf(mechanic("setGlobalVar", params), mechanic("updateGlobalVar", update)))

        assertEquals(5, state.variable("sospetto"))
    }

    @Test
    fun checkStatAndJumpSaltaSullaStatEffettiva() {
        val state = state(endurance = 5)
        val params = buildJsonObject {
            put("statName", "ENDURANCE"); put("operator", "<="); put("value", "6"); put("targetScene", "99")
        }

        val outcome = executor().execute(state, listOf(mechanic("checkStatAndJump", params)))

        assertEquals("99", outcome.jumpTo)
        assertEquals(AutoJumpReason.IF_STAT, outcome.jumpReason)
    }

    @Test
    fun checkItemAndJumpRamoFalso() {
        val state = state()
        val params = buildJsonObject {
            put("itemName", "Lantern"); put("nextSceneId_TRUE", "112"); put("nextSceneId_FALSE", "87")
        }

        val outcome = executor().execute(state, listOf(mechanic("checkItemAndJump", params)))

        assertEquals("87", outcome.jumpTo)
        assertEquals(AutoJumpReason.CHECK_ITEM_AND_JUMP, outcome.jumpReason)
    }

    @Test
    fun skillCheckConDisciplinaSommaIlModificatore() {
        val state = state(disciplines = listOf("SIXTH_SENSE"))
        val params = buildJsonObject {
            put("discipline", "SIXTH_SENSE"); put("modifier", "3")
            putJsonArray("outcomes") {
                add(buildJsonObject { put("minRoll", 0); put("maxRoll", 6); put("nextSceneId", "fail") })
                add(buildJsonObject { put("minRoll", 7); put("maxRoll", 12); put("nextSceneId", "success") })
            }
        }

        // Tiro 5 + modifier 3 = 8 -> success; senza disciplina sarebbe fail.
        val outcome = executor(5).execute(state, listOf(mechanic("handleSkillCheck", params)))

        assertEquals("success", outcome.jumpTo)
        assertEquals(AutoJumpReason.SKILL_CHECK, outcome.jumpReason)
    }

    @Test
    fun conditionalActionEsegueIlComandoAnnidato() {
        val state = state(items = listOf(GameItem(name = "Lantern", type = ItemType.SPECIAL_ITEM)))
        val params = buildJsonObject {
            put("condition", "HAS_ITEM"); put("itemName", "Lantern")
            putJsonObject("action") {
                put("command", "setFlag")
                putJsonObject("params") { put("flagName", "luce"); put("value", "true") }
            }
        }

        executor().execute(state, listOf(mechanic("handleConditionalAction", params)))

        assertEquals("true", state.flag("luce"))
    }

    @Test
    fun ilPrimoSaltoInterrompeIComandiSuccessivi() {
        val state = state()
        val jump = buildJsonObject {
            put("itemName", "Lantern"); put("nextSceneId_FALSE", "87")
        }
        val addAfter = buildJsonObject {
            put("itemType", "GOLD"); put("itemName", "Gold Crowns"); put("quantity", "10")
        }

        val outcome = executor().execute(
            state,
            listOf(mechanic("checkItemAndJump", jump), mechanic("addItem", addAfter)),
        )

        assertEquals("87", outcome.jumpTo)
        assertEquals(0, Inventory.countOf(state.hero, "Gold Crowns"))
    }

    @Test
    fun comandoSconosciutoNonBloccaIlGioco() {
        val state = state()

        val outcome = executor().execute(state, listOf(mechanic("comandoInventato", JsonObject(emptyMap()))))

        assertNull(outcome.jumpTo)
    }
}
