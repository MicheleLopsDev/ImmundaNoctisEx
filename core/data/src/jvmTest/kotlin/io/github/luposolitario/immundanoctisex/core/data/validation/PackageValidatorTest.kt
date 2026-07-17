package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.ComparisonOperator
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.GameMechanic
import io.github.luposolitario.immundanoctisex.core.data.model.GlobalRule
import io.github.luposolitario.immundanoctisex.core.data.model.GlobalRuleType
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

// Milestone Fase 1: un pacchetto rotto viene bocciato con messaggi chiari.
// Ogni test isola UNA violazione per volta.
class PackageValidatorTest {

    private fun scene(id: String, type: SceneType = SceneType.TRANSITION, vararg choices: Choice) = Scene(
        id = id,
        sceneType = type,
        genre = "FANTASY",
        narrativeText = "testo",
        choices = choices.toList(),
    )

    private fun manifest(scenes: List<Scene>, globalRules: List<GlobalRule> = emptyList()) = Manifest(
        id = "test-package",
        version = "1.0.0",
        title = "Test",
        description = "Test",
        language = "en",
        genre = "FANTASY",
        globalRules = globalRules,
        scenes = scenes,
    )

    @Test
    fun unPacchettoMinimoValidoNonHaErrori() {
        val result = PackageValidator.validate(manifest(listOf(scene("1", SceneType.START))))

        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun unaDestinazioneInesistenteVieneRigettata() {
        val start = scene("1", SceneType.START, Choice("c1", "vai", nextSceneId = "999"))

        val result = PackageValidator.validate(manifest(listOf(start)))

        assertTrue(result.errors.any { it.contains("999") })
    }

    @Test
    fun sceneIdDuplicateVengonoRigettate() {
        val scenes = listOf(scene("1", SceneType.START), scene("1"))

        val result = PackageValidator.validate(manifest(scenes))

        assertTrue(result.errors.any { it.contains("duplicata") })
    }

    @Test
    fun assenzaDiUnaScenaStartVieneRigettata() {
        val result = PackageValidator.validate(manifest(listOf(scene("1", SceneType.TRANSITION))))

        assertTrue(result.errors.any { it.contains("START") })
    }

    @Test
    fun unaDisciplinaNonCanonicaVieneRigettata() {
        val start = scene("1", SceneType.START).copy(
            disciplineChoices = listOf(DisciplineChoice("d1", "SHADOWSTEP", "testo", nextSceneId = "1")),
        )

        val result = PackageValidator.validate(manifest(listOf(start)))

        assertTrue(result.errors.any { it.contains("SHADOWSTEP") })
    }

    @Test
    fun unCombatConWinSceneIdVuotoVieneRigettato() {
        val start = scene("1", SceneType.START).copy(
            combat = Combat(enemyName = "Nemico", enemyCombatSkill = 10, enemyEndurance = 10, winSceneId = ""),
        )

        val result = PackageValidator.validate(manifest(listOf(start)))

        assertTrue(result.errors.any { it.contains("obbligatorio") })
    }

    private fun rollOnItemTable(vararg intervals: Pair<Int, Int>) = GameMechanic(
        command = "rollOnItemTable",
        params = JsonObject(
            mapOf(
                "outcomes" to kotlinx.serialization.json.JsonArray(
                    intervals.map { (min, max) ->
                        JsonObject(mapOf("minRoll" to JsonPrimitive(min), "maxRoll" to JsonPrimitive(max)))
                    },
                ),
            ),
        ),
    )

    @Test
    fun unIntervalloRollOnItemTableConBuchiVieneRigettato() {
        val start = scene("1", SceneType.START).copy(gameMechanics = listOf(rollOnItemTable(0 to 4, 6 to 9)))

        val result = PackageValidator.validate(manifest(listOf(start)))

        assertTrue(result.errors.any { it.contains("non coperti") && it.contains("5") })
    }

    @Test
    fun unIntervalloRollOnItemTableConSovrapposizioniVieneRigettato() {
        val start = scene("1", SceneType.START).copy(gameMechanics = listOf(rollOnItemTable(0 to 5, 4 to 9)))

        val result = PackageValidator.validate(manifest(listOf(start)))

        assertTrue(result.errors.any { it.contains("più intervalli") })
    }

    @Test
    fun unaGlobalRuleVersoUnaSceneNonEndingDaWarningNonErrore() {
        val start = scene("1", SceneType.START)
        val rule = GlobalRule(GlobalRuleType.FLAG, "vittoria", ComparisonOperator.EQ, "true", targetSceneId = "1")

        val result = PackageValidator.validate(manifest(listOf(start), globalRules = listOf(rule)))

        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.any { it.contains("ENDING") })
    }
}
