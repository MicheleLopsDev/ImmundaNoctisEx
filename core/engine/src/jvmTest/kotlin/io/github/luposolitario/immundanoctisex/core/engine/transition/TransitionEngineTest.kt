package io.github.luposolitario.immundanoctisex.core.engine.transition

import io.github.luposolitario.immundanoctisex.core.data.model.AutoJumpReason
import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.ComparisonOperator
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.GameMechanic
import io.github.luposolitario.immundanoctisex.core.data.model.GlobalRule
import io.github.luposolitario.immundanoctisex.core.data.model.GlobalRuleType
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.engine.dice.FixedDiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.mechanics.MechanicsExecutor
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class TransitionEngineTest {

    private fun scene(
        id: String,
        mechanics: List<GameMechanic> = emptyList(),
        combat: Combat? = null,
        type: SceneType = SceneType.TRANSITION,
    ) = Scene(
        id = id,
        sceneType = type,
        genre = "FANTASY",
        narrativeText = "testo scena $id",
        gameMechanics = mechanics,
        combat = combat,
    )

    private fun manifest(
        scenes: List<Scene>,
        deathSceneId: String? = "99",
        globalRules: List<GlobalRule> = emptyList(),
    ) = Manifest(
        id = "sample",
        version = "1.0",
        title = "Test",
        description = "",
        language = "en",
        genre = "FANTASY",
        deathSceneId = deathSceneId,
        globalRules = globalRules,
        scenes = scenes,
    )

    private fun state(disciplines: List<String> = emptyList(), endurance: Int = 20) = GameState(
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
                ),
            ),
            lastUpdate = 0L,
        ),
    )

    private fun engine(manifest: Manifest, vararg rolls: Int) =
        TransitionEngine(manifest, MechanicsExecutor(FixedDiceRoller(rolls.toList())))

    @Test
    fun transizioneSempliceArrivaEBasta() {
        val manifest = manifest(listOf(scene("1"), scene("2")))
        val state = state()

        val result = engine(manifest).transitionTo(state, "2")

        assertEquals("2", result.sceneId)
        assertEquals("2", state.currentSceneId)
        assertEquals(0, result.autoJumps.size)
    }

    @Test
    fun healingPassivaCuraVersoScenaSenzaCombat() {
        val manifest = manifest(listOf(scene("2")))
        val state = state(disciplines = listOf("HEALING"), endurance = 10)

        engine(manifest).transitionTo(state, "2")

        assertEquals(11, state.hero.currentEndurance)
    }

    @Test
    fun healingPassivaNonCuraVersoScenaConCombat() {
        val combat = Combat(
            enemyName = "Thug",
            enemyCombatSkill = 14,
            enemyEndurance = 22,
            winSceneId = "6",
        )
        val manifest = manifest(listOf(scene("4", combat = combat)))
        val state = state(disciplines = listOf("HEALING"), endurance = 10)

        engine(manifest).transitionTo(state, "4")

        assertEquals(10, state.hero.currentEndurance)
    }

    @Test
    fun morteBuiltInScattaDopoIMechanics() {
        val damage = GameMechanic(
            "applyStatModifier",
            buildJsonObject { put("statName", "ENDURANCE"); put("amount", "-10") },
        )
        val manifest = manifest(listOf(scene("2", mechanics = listOf(damage)), scene("99", type = SceneType.ENDING)))
        val state = state(endurance = 5)

        val result = engine(manifest).transitionTo(state, "2")

        assertEquals("99", result.sceneId)
        assertEquals(AutoJumpReason.BUILT_IN_DEATH, result.autoJumps.single().reason)
    }

    @Test
    fun morteBatteLaVittoria() {
        // Il flag di vittoria è vero MA la Resistenza va a zero nella stessa
        // transizione: la morte built-in si valuta prima (REGOLE.md §2.4).
        val mechanics = listOf(
            GameMechanic("setFlag", buildJsonObject { put("flagName", "vittoria"); put("value", "true") }),
            GameMechanic("applyStatModifier", buildJsonObject { put("statName", "ENDURANCE"); put("amount", "-30") }),
        )
        val victoryRule = GlobalRule(GlobalRuleType.FLAG, "vittoria", ComparisonOperator.EQ, "true", "100")
        val manifest = manifest(
            listOf(scene("2", mechanics = mechanics), scene("99", type = SceneType.ENDING), scene("100", type = SceneType.ENDING)),
            globalRules = listOf(victoryRule),
        )
        val state = state()

        val result = engine(manifest).transitionTo(state, "2")

        assertEquals("99", result.sceneId)
    }

    @Test
    fun globalRuleSuVariabileScattaDopoIMechanics() {
        val raiseSuspicion = GameMechanic(
            "updateGlobalVar",
            buildJsonObject { put("varName", "sospetto"); put("value", "10"); put("operation", "ADD") },
        )
        val arrestRule = GlobalRule(GlobalRuleType.VAR, "sospetto", ComparisonOperator.GTE, "10", "66")
        val manifest = manifest(
            listOf(scene("2", mechanics = listOf(raiseSuspicion)), scene("66", type = SceneType.ENDING)),
            globalRules = listOf(arrestRule),
        )
        val state = state()

        val result = engine(manifest).transitionTo(state, "2")

        assertEquals("66", result.sceneId)
        assertEquals(AutoJumpReason.GLOBAL_RULE, result.autoJumps.single().reason)
    }

    @Test
    fun saltoDeiMechanicsProseguePoiSullaDestinazione() {
        val jump = GameMechanic(
            "checkItemAndJump",
            buildJsonObject { put("itemName", "Lantern"); put("nextSceneId_FALSE", "87") },
        )
        val manifest = manifest(listOf(scene("2", mechanics = listOf(jump)), scene("87")))
        val state = state()

        val result = engine(manifest).transitionTo(state, "2")

        assertEquals("87", result.sceneId)
        assertEquals(AutoJumpReason.CHECK_ITEM_AND_JUMP, result.autoJumps.single().reason)
    }

    @Test
    fun scenaInesistenteNonBloccaIlGioco() {
        val manifest = manifest(listOf(scene("1")))
        val state = state()

        val result = engine(manifest).transitionTo(state, "manca")

        assertEquals("1", result.sceneId)
    }

    @Test
    fun cicloDiRegoleScritteMaleSiFermaSenzaBloccare() {
        // Due scene i cui mechanics si rimandano a vicenda: la rete di
        // sicurezza maxHops interrompe il giro.
        val toB = GameMechanic(
            "checkItemAndJump",
            buildJsonObject { put("itemName", "X"); put("nextSceneId_FALSE", "B") },
        )
        val toA = GameMechanic(
            "checkItemAndJump",
            buildJsonObject { put("itemName", "X"); put("nextSceneId_FALSE", "A") },
        )
        val manifest = manifest(listOf(scene("A", mechanics = listOf(toB)), scene("B", mechanics = listOf(toA))))
        val state = state()

        val result = engine(manifest).transitionTo(state, "A")

        // Non importa dove si ferma: importa che si fermi.
        assertEquals(true, result.sceneId in listOf("A", "B"))
    }
}
