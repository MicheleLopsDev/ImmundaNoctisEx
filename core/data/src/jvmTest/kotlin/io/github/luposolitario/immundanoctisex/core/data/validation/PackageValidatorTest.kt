package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.ComparisonOperator
import io.github.luposolitario.immundanoctisex.core.data.model.CustomResourceEntry
import io.github.luposolitario.immundanoctisex.core.data.model.CustomResources
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.EndingOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.GameMechanic
import io.github.luposolitario.immundanoctisex.core.data.model.GlobalRule
import io.github.luposolitario.immundanoctisex.core.data.model.GlobalRuleType
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.RollCondition
import io.github.luposolitario.immundanoctisex.core.data.model.RollConditionType
import io.github.luposolitario.immundanoctisex.core.data.model.RollModifier
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

    @Test
    fun unBackgroundImageSenzaPrefissoVieneRigettato() {
        val start = scene("1", SceneType.START).copy(backgroundImage = "loc_tavern")

        val result = PackageValidator.validate(manifest(listOf(start)))

        assertTrue(result.errors.any { it.contains("backgroundImage") && it.contains("prefisso") })
    }

    @Test
    fun unNpcImageStaticNonDaNeErroriNeAvvisi() {
        val start = scene("1", SceneType.START).copy(npcImage = "static:npc_traveler")

        val result = PackageValidator.validate(manifest(listOf(start)))

        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun unEnemyImageUrlHttpsDaSoloWarningNonErrore() {
        val start = scene("1", SceneType.START).copy(
            combat = Combat(
                enemyName = "Test",
                enemyImage = "url:https://example.invalid/illustrazione.png",
                enemyCombatSkill = 10,
                enemyEndurance = 10,
                winSceneId = "1",
            ),
        )

        val result = PackageValidator.validate(manifest(listOf(start)))

        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.any { it.contains("combat.enemyImage") && it.contains("link esterno") })
    }

    @Test
    fun unUrlConSchemaNonHttpVieneRigettato() {
        val start = scene("1", SceneType.START).copy(backgroundImage = "url:file:///etc/passwd")

        val result = PackageValidator.validate(manifest(listOf(start)))

        assertTrue(result.errors.any { it.contains("schema non supportato") })
    }

    @Test
    fun risorsePersonalizzateSenzaDuplicatiNonHannoAvvisi() {
        val libro = manifest(listOf(scene("1", SceneType.START))).copy(
            customResources = CustomResources(images = listOf(CustomResourceEntry("mio_villain", "https://example.invalid/a.png"))),
        )

        val result = PackageValidator.validate(libro)

        assertTrue(result.warnings.none { it.contains("Risorse personalizzate") })
    }

    @Test
    fun unIdDuplicatoTraRisorsePersonalizzateEUnAvvisoNonUnErrore() {
        val libro = manifest(listOf(scene("1", SceneType.START))).copy(
            customResources = CustomResources(
                images = listOf(
                    CustomResourceEntry("duplicato", "https://example.invalid/a.png"),
                    CustomResourceEntry("duplicato", "https://example.invalid/b.png"),
                ),
            ),
        )

        val result = PackageValidator.validate(libro)

        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.any { it.contains("duplicato") && it.contains("immagini") })
    }

    @Test
    fun unaSceneSenzaSfxNonHaErrori() {
        val result = PackageValidator.validate(manifest(listOf(scene("1", SceneType.START))))

        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun unSfxRegistratoConUnUrlNonDaErrori() {
        val libro = manifest(listOf(scene("1", SceneType.START).copy(sfx = "mio_suono"))).copy(
            customResources = CustomResources(sounds = listOf(CustomResourceEntry("mio_suono", "url:https://example.invalid/a.mp3"))),
        )

        val result = PackageValidator.validate(libro)

        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun unSfxRegistratoConUnaRisorsaStaticaNonDaErrori() {
        // §18.4 (Michele: "puoi associare... una risorsa statica presente
        // nel apk"): la voce del registro può valere anche static:<id>,
        // non solo url: — Scene.sfx punta comunque solo all'ID.
        val libro = manifest(listOf(scene("1", SceneType.START).copy(sfx = "mio_suono"))).copy(
            customResources = CustomResources(sounds = listOf(CustomResourceEntry("mio_suono", "static:loc_tavern"))),
        )

        val result = PackageValidator.validate(libro)

        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun unaVoceSuoniSenzaPrefissoVieneRigettata() {
        val libro = manifest(listOf(scene("1", SceneType.START))).copy(
            customResources = CustomResources(sounds = listOf(CustomResourceEntry("mio_suono", "https://example.invalid/a.mp3"))),
        )

        val result = PackageValidator.validate(libro)

        assertTrue(result.errors.any { it.contains("mio_suono") && it.contains("prefisso") })
    }

    @Test
    fun unSfxNonRegistratoVieneRigettato() {
        val start = scene("1", SceneType.START).copy(sfx = "sconosciuto")

        val result = PackageValidator.validate(manifest(listOf(start)))

        assertTrue(result.errors.any { it.contains("sfx") && it.contains("sconosciuto") })
    }

    @Test
    fun unSfxVuotoVieneRigettato() {
        val start = scene("1", SceneType.START).copy(sfx = "")

        val result = PackageValidator.validate(manifest(listOf(start)))

        assertTrue(result.errors.any { it.contains("sfx") && it.contains("vuota") })
    }

    @Test
    fun unaSceneTransitionSenzaUsciteDaUnAvvisoDiVicoloCieco() {
        val start = scene("1", SceneType.START, Choice("c1", "vai", nextSceneId = "2"))
        val vicoloCieco = scene("2", SceneType.TRANSITION)

        val result = PackageValidator.validate(manifest(listOf(start, vicoloCieco)))

        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.any { it.contains("'2'") && it.contains("vicolo cieco") })
    }

    @Test
    fun unaSceneEndingSenzaUsciteNonDaAvvisi() {
        val start = scene("1", SceneType.START, Choice("c1", "vai", nextSceneId = "2"))
        val finale = scene("2", SceneType.ENDING)

        val result = PackageValidator.validate(manifest(listOf(start, finale)))

        assertTrue(result.warnings.none { it.contains("vicolo cieco") })
    }

    // --- Scene.rollModifiers (01/08/2026) ---
    // Un modificatore scritto male verrebbe solo ignorato a runtime, e il
    // giocatore finirebbe nella scena sbagliata senza accorgersene:
    // meglio bloccare qui.

    private fun scenaConTiro(modifiers: List<RollModifier>) = Scene(
        id = "1",
        sceneType = SceneType.START,
        genre = "FANTASY",
        narrativeText = "testo",
        choices = listOf(Choice("c1", "0-9", nextSceneId = "1", minRoll = 0, maxRoll = 9)),
        rollModifiers = modifiers,
    )

    @Test
    fun unModificatoreConDisciplinaCanonicaEValido() {
        val scena = scenaConTiro(
            listOf(
                RollModifier(2, RollCondition(RollConditionType.DISCIPLINE, values = listOf("SIXTH_SENSE"))),
            ),
        )

        assertTrue(PackageValidator.validate(manifest(listOf(scena))).errors.isEmpty())
    }

    @Test
    fun unModificatoreConDisciplinaInventataEBocciato() {
        val scena = scenaConTiro(
            listOf(
                RollModifier(2, RollCondition(RollConditionType.DISCIPLINE, values = listOf("SHADOWSTEP"))),
            ),
        )

        val result = PackageValidator.validate(manifest(listOf(scena)))

        assertTrue(result.errors.any { it.contains("SHADOWSTEP") && it.contains("non canonica") })
    }

    @Test
    fun unaCondizioneSuEnduranceSenzaSogliaEBocciata() {
        val scena = scenaConTiro(listOf(RollModifier(-3, RollCondition(RollConditionType.ENDURANCE))))

        val result = PackageValidator.validate(manifest(listOf(scena)))

        assertTrue(result.errors.any { it.contains("ENDURANCE") && it.contains("threshold") })
    }

    @Test
    fun unaCondizioneSenzaValoriEBocciata() {
        val scena = scenaConTiro(listOf(RollModifier(2, RollCondition(RollConditionType.ITEM))))

        val result = PackageValidator.validate(manifest(listOf(scena)))

        assertTrue(result.errors.any { it.contains("ITEM") && it.contains("nessun valore") })
    }

    @Test
    fun unModificatoreSuUnaScenaSenzaTiroDaSoloUnAvviso() {
        // Non romperebbe nulla: semplicemente non verrebbe mai applicato.
        val scena = Scene(
            id = "1",
            sceneType = SceneType.START,
            genre = "FANTASY",
            narrativeText = "testo",
            rollModifiers = listOf(RollModifier(5)),
        )

        val result = PackageValidator.validate(manifest(listOf(scena)))

        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.any { it.contains("rollModifiers") && it.contains("nessuna scelta") })
    }

    // EndingsValidator (06/08/2026): l'equilibrio dei finali. Vedi
    // doc/FORMA-DEI-GRAFI.md per il perche' — nei librogame veri le
    // sconfitte sono la maggioranza dei finali, e sono scritte.
    private fun finale(id: String, outcome: EndingOutcome?) = Scene(
        id = id,
        sceneType = SceneType.ENDING,
        genre = "FANTASY",
        narrativeText = "testo",
        outcome = outcome,
    )

    @Test
    fun unLibroSenzaFinaliDiSconfittaDaUnAvviso() {
        val scene = listOf(scene("1", SceneType.START), finale("2", EndingOutcome.VICTORY))

        val result = PackageValidator.validate(manifest(scene))

        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.any { it.contains("sconfitta") && it.contains("DEFEAT") })
        assertTrue(result.warnings.none { it.contains("vittoria") })
    }

    @Test
    fun unLibroSenzaFinaliDiVittoriaDaUnAvviso() {
        val scene = listOf(scene("1", SceneType.START), finale("2", EndingOutcome.DEFEAT))

        val result = PackageValidator.validate(manifest(scene))

        assertTrue(result.warnings.any { it.contains("vittoria") && it.contains("VICTORY") })
        assertTrue(result.warnings.none { it.contains("sconfitta") })
    }

    @Test
    fun unLibroConVittoriaESconfittaNonDaAvvisiSuiFinali() {
        val scene = listOf(
            scene("1", SceneType.START),
            finale("2", EndingOutcome.VICTORY),
            finale("3", EndingOutcome.DEFEAT),
            finale("4", EndingOutcome.NEUTRAL),
        )

        val result = PackageValidator.validate(manifest(scene))

        assertTrue(result.warnings.none { it.contains("finale") })
    }

    @Test
    fun unPacchettoSenzaNessunaScenaEndingNonVieneGiudicatoSuiFinali() {
        // Un frammento (fixture di test, libro in costruzione) non
        // dichiara finali: la domanda non ha senso, e a runtime ci pensa
        // AdventureEnding.withGuaranteedEnding.
        val result = PackageValidator.validate(manifest(listOf(scene("1", SceneType.START))))

        assertTrue(result.warnings.none { it.contains("finale") })
    }
}
