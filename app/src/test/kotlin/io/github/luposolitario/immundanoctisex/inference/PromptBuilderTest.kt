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

    private fun scene(combat: Combat? = null, backgroundImage: String? = null) = Scene(
        id = "3",
        sceneType = SceneType.TRANSITION,
        genre = "FANTASY",
        toneHints = listOf("dark", "suspenseful"),
        narrativeText = "The alley narrows as the old quarter swallows the daylight.",
        combat = combat,
        backgroundImage = backgroundImage,
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
        syntheticEnding: Boolean = false,
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
        isSyntheticEnding = syntheticEnding,
    )

    // Il finale che il libro non ha: non c'è testo da arricchire, c'è un
    // finale da scrivere. Senza questo il prompt conterrebbe una scena
    // vuota e il modello inventerebbe a caso.
    @Test
    fun ilFinaleFabbricatoChiedeDiSCRIVERLONonDiTradurlo() {
        val vuota = scene().copy(
            id = "__ex_synthetic_defeat__",
            sceneType = SceneType.ENDING,
            narrativeText = "",
        )
        val prompt = PromptBuilder().build(context(scene = vuota, syntheticEnding = true))

        assertTrue(prompt.contains("FINAL SCENE"), "deve chiedere di scrivere il finale")
        assertTrue(prompt.contains("ends here in defeat"))
        // La storia fin qui resta: è l'unico appiglio che ha per non
        // inventare personaggi e luoghi nuovi.
        assertTrue(prompt.contains("You left the inn with the letter."))
        assertFalse(prompt.contains("CURRENT SCENE"), "non c'è nessuna scena sorgente da arricchire")
    }

    // Il finale chiude, ma non sbatte la porta (richiesta Michele).
    @Test
    fun ilFinaleChiudeMaLasciaUnFilo() {
        val vuota = scene().copy(
            id = "__ex_synthetic_defeat__",
            sceneType = SceneType.ENDING,
            narrativeText = "",
        )
        val prompt = PromptBuilder().build(context(scene = vuota, syntheticEnding = true))
        assertTrue(prompt.contains("faint thread"), "deve lasciare una possibilità di continuo")
        assertTrue(prompt.contains("read as an ENDING"), "ma deve restare un finale")
        // Genere e tono valgono anche qui: il finale è dell'eroe di QUESTA
        // partita, non di un eroe generico.
        assertTrue(prompt.contains("male"))
        assertTrue(prompt.contains("dark, suspenseful"))
    }

    // Le Discipline Kai sono poteri, non mestieri: solo i Kai le hanno.
    @Test
    fun conUnaDisciplinaInGiocoSiChiedeEnfasiSoprannaturale() {
        val prompt = PromptBuilder().build(
            context(
                disciplineChoices = listOf(
                    DisciplineChoice(id = "d1", disciplineId = "SIXTH_SENSE", choiceText = "Ascolta", nextSceneId = "5"),
                ),
            ),
        )
        assertTrue(prompt.contains("KAI DISCIPLINES"))
        assertTrue(prompt.contains("preternatural"))
        // L'enfasi non è licenza di inventare: resta il limite sui fatti.
        assertTrue(prompt.contains("do NOT invent effects"))
    }

    @Test
    fun senzaDiscipline_lEnfasiNonSpreccaContesto() {
        val prompt = PromptBuilder().build(context(disciplineChoices = emptyList()))
        assertFalse(prompt.contains("KAI DISCIPLINES"))
    }

    @Test
    fun unaScenaNormaleNonRiceveLIstruzioneDelFinale() {
        val prompt = PromptBuilder().build(context())
        assertFalse(prompt.contains("FINAL SCENE"))
        assertTrue(prompt.contains("CURRENT SCENE"))
    }

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

    // --- Sfondo di scena (vocabolario chiuso, esperimento 20/07/2026) ---

    @Test
    fun senzaSfondoDichiarato_siChiedeAGemmaDiSuggerirlo() {
        val prompt = PromptBuilder().build(context(scene = scene(backgroundImage = null)))
        assertContains(prompt, "IMAGE|location_id")
        // Il vocabolario è CHIUSO: i nomi veri devono comparire per intero,
        // non un placeholder generico.
        assertContains(prompt, "loc_tavern")
    }

    @Test
    fun conSfondoGiaDichiarato_nonSiSprecaContestoAChiederlo() {
        val prompt = PromptBuilder().build(context(scene = scene(backgroundImage = "loc_market")))
        assertFalse(prompt.contains("IMAGE|location_id"))
    }

    // BUG del 20/07/2026, trovato da Michele giocando: il sample dichiara
    // backgroundImage su TUTTE le scene con placeholder storici mai
    // risolti in un file ("inn", "city"...). La condizione era solo
    // "!= null", quindi il tag non veniva MAI chiesto — l'esperimento
    // era silenziosamente morto sul nascere. Un placeholder che non
    // esiste nel catalogo non è una scelta valida: si chiede comunque.
    @Test
    fun sfondoDichiaratoMaFuoriCatalogo_siChiedeComunqueAGemma() {
        val prompt = PromptBuilder().build(context(scene = scene(backgroundImage = "inn")))
        assertContains(prompt, "IMAGE|location_id")
    }
}
