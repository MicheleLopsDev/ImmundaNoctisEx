package io.github.luposolitario.immundanoctisex.inference

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.Gender
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PromptBuilderTest {

    private fun scene(combat: Combat? = null) = Scene(
        id = "3",
        sceneType = SceneType.TRANSITION,
        genre = "FANTASY",
        toneHints = listOf("dark", "suspenseful"),
        narrativeText = "The alley narrows as the old quarter swallows the daylight.",
        combat = combat,
    )

    private fun context(
        scene: Scene = scene(),
        previous: String? = "You left the inn with the letter.",
        continuations: List<String> = listOf("The warehouse door stands ajar."),
        choices: List<Choice> = listOf(
            Choice(id = "c1", choiceText = "Walk on, hand on your weapon", nextSceneId = "4"),
        ),
        disciplineChoices: List<DisciplineChoice> = emptyList(),
        gender: Gender = Gender.MALE,
    ) = PromptContext(
        scene = scene,
        previousSceneText = previous,
        continuations = continuations,
        choices = choices,
        disciplineChoices = disciplineChoices,
        sourceLanguage = "English",
        userLanguage = "Italian",
        genre = "FANTASY",
        toneHints = scene.toneHints,
        playerGender = gender,
    )

    @Test
    fun tuttiIPlaceholderVengonoRiempiti() {
        val prompt = PromptBuilder().build(context())

        assertFalse(prompt.contains("{"), "placeholder non sostituito in:\n$prompt")
    }

    @Test
    fun ilPromptPortaScenaLingueTonoEScelte() {
        val prompt = PromptBuilder().build(context())

        assertContains(prompt, "The alley narrows as the old quarter swallows the daylight.")
        assertContains(prompt, "English")
        assertContains(prompt, "Italian")
        assertContains(prompt, "dark, suspenseful")
        assertContains(prompt, "CHOICE|4|1|Walk on, hand on your weapon")
    }

    @Test
    fun ilDiarioNonEntraMai_soloLaCodaDellaScenaPrecedente() {
        val prompt = PromptBuilder().build(context(previous = "You left the inn with the letter."))

        assertContains(prompt, "You left the inn with the letter.")
        // Nessuna sezione di storia lunga: il contesto è solo la scena
        // precedente (inferenza senza memoria, CRITICITA.md).
        assertEquals(1, Regex("THE STORY SO FAR").findAll(prompt).count())
    }

    @Test
    fun sezioniVuoteNonVengonoScritte() {
        val prompt = PromptBuilder().build(
            context(previous = null, continuations = emptyList(), choices = emptyList()),
        )

        assertFalse(prompt.contains("THE STORY SO FAR"))
        assertFalse(prompt.contains("POSSIBLE CONTINUATIONS"))
        assertFalse(prompt.contains("CHOICES TO TRANSLATE"))
        // La scena e le istruzioni ci sono comunque.
        assertContains(prompt, "CURRENT SCENE")
        assertContains(prompt, "--- TAGS ---")
    }

    @Test
    fun laRigaEnemySiChiedeSoloSeCeUnNemico() {
        val senzaCombat = PromptBuilder().build(context())
        assertFalse(senzaCombat.contains("ENEMY|"))

        val combat = Combat(
            enemyName = "Warehouse Thugs",
            enemyCombatSkill = 16,
            enemyEndurance = 24,
            winSceneId = "6",
        )
        val conCombat = PromptBuilder().build(context(scene = scene(combat = combat)))
        assertContains(conCombat, "ENEMY|translated enemy name")
        assertContains(conCombat, "ENEMY|Warehouse Thugs")
    }

    @Test
    fun ilGenereDelGiocatoreArrivaAlModello() {
        assertContains(PromptBuilder().build(context(gender = Gender.FEMALE)), "female")
        assertContains(PromptBuilder().build(context(gender = Gender.MALE)), "male")
    }

    @Test
    fun leDisciplineSonoConsegnateNelFormatoDiRisposta() {
        val prompt = PromptBuilder().build(
            context(
                disciplineChoices = listOf(
                    DisciplineChoice("d1", "SIXTH_SENSE", "You sense the ambush", "5"),
                ),
            ),
        )

        assertContains(prompt, "DISCIPLINE|SIXTH_SENSE|You sense the ambush")
    }

    @Test
    fun toniAssenti_degradaSuNeutral() {
        val sceneSenzaToni = scene().copy(toneHints = emptyList())
        val prompt = PromptBuilder().build(context(scene = sceneSenzaToni).copy(toneHints = emptyList()))

        assertContains(prompt, "neutral")
    }

    @Test
    fun configRottaOAssente_usaIDefaultSenzaEccezioni() {
        assertEquals(PromptFragments.DEFAULTS, PromptFragments.fromConfig("{ non è json"))
        assertEquals(PromptFragments.DEFAULTS, PromptFragments.fromConfig(""))
        assertEquals(PromptFragments.DEFAULTS, PromptFragments.fromConfig("""{"tags":[]}"""))
    }

    @Test
    fun configValida_sovrascriveSoloIFrammentiPresenti() {
        val config = """
            {
              "tags": [
                {
                  "id": "start_adventure_prompt",
                  "parameters": [
                    { "name": "baseText", "value": "Sei il narratore." },
                    { "name": "closingText", "value": "" }
                  ]
                }
              ]
            }
        """.trimIndent()

        val fragments = PromptFragments.fromConfig(config)

        assertEquals("Sei il narratore.", fragments.baseText)
        // Frammento vuoto o assente -> default.
        assertEquals(PromptFragments.DEFAULTS.closingText, fragments.closingText)
        assertEquals(PromptFragments.DEFAULTS.sceneText, fragments.sceneText)
    }

    @Test
    fun ilConfigVeroDelProgettoSiLegge() {
        val configJson = requireNotNull(javaClass.classLoader.getResourceAsStream("config.json")) {
            "content/config.json non trovato sul classpath di test"
        }.bufferedReader().use { it.readText() }

        val fragments = PromptFragments.fromConfig(configJson)

        // Non è il default: i frammenti arrivano davvero dal file.
        assertTrue(fragments.baseText.isNotBlank())
        assertContains(fragments.outputFormatText, "--- TAGS ---")
        assertContains(fragments.constraintText, "{user_language}")
    }
}
