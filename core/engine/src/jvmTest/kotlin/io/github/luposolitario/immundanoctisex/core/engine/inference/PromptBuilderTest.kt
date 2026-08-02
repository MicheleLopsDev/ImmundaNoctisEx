package io.github.luposolitario.immundanoctisex.core.engine.inference

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

    // --- Sfondo di scena: askImageInPrompt (27/07/2026) ---
    // L'esperimento del 20/07/2026 ("Gemma suggerisce lo sfondo quando il
    // pacchetto non ne ha uno valido") era stato disattivato il 26/07
    // ("rendiamo il prompt più semplice"), ora è una PREFERENZA
    // (InferencePreferences.askImageInPrompt, Opzioni avanzate Modelli
    // LLM) invece che un ramo di codice tolto — Michele: "vorrei che
    // fosse una cosa configurabile e magari deselezionabile dal menu
    // LLM". Di default resta spenta (stesso comportamento del 26/07).

    @Test
    fun conAskImageSpento_laRigaImageNonSiChiedeMai() {
        assertFalse(
            PromptBuilder(askImageInPrompt = false)
                .build(context(scene = scene(backgroundImage = null)))
                .contains("IMAGE|location_id"),
        )
        assertFalse(
            PromptBuilder(askImageInPrompt = false)
                .build(context(scene = scene(backgroundImage = "inn")))
                .contains("IMAGE|location_id"),
        )
    }

    @Test
    fun conAskImageAcceso_senzaSfondoDichiarato_siChiedeAGemmaDiSuggerirlo() {
        val prompt = PromptBuilder(askImageInPrompt = true).build(context(scene = scene(backgroundImage = null)))
        assertContains(prompt, "IMAGE|location_id")
        // Il vocabolario è CHIUSO: i nomi veri devono comparire per intero.
        assertContains(prompt, "loc_tavern")
    }

    @Test
    fun conAskImageAcceso_sfondoFuoriCatalogo_siChiedeComunqueAGemma() {
        // BUG del 20/07/2026: un placeholder morto ("inn") non è una
        // scelta valida, si chiede comunque.
        val prompt = PromptBuilder(askImageInPrompt = true).build(context(scene = scene(backgroundImage = "inn")))
        assertContains(prompt, "IMAGE|location_id")
    }

    @Test
    fun conAskImageAcceso_sfondoGiaValido_nonSiSprecaContestoAChiederlo() {
        val prompt = PromptBuilder(askImageInPrompt = true).build(context(scene = scene(backgroundImage = "loc_market")))
        assertFalse(prompt.contains("IMAGE|location_id"))
    }

    // --- Modalità Traduzione (31/07/2026) ---
    // Michele: "non è tradci parola per parola ma traduci con i minori
    // cambiamenti possibili mantenendo il senso" — Gemma non arricchisce
    // più la scena, si limita a tradurla restando fedele al testo
    // sorgente. Preset fisso dei parametri di generazione gestito da
    // InferencePreferences, non da PromptBuilder: qui si verifica solo il
    // testo del prompt.

    @Test
    fun modalitaTraduzioneNonChiedeDiArricchire() {
        val prompt = PromptBuilder(translationMode = true).build(context())

        // "enrich" compare comunque nel divieto esplicito ("do NOT
        // enrich"): quello che non deve MAI comparire è l'istruzione
        // positiva di CONSTRAINT_TEXT che chiede di arricchire.
        assertFalse(
            prompt.contains("enriching it with details"),
            "non deve chiedere di arricchire:\n$prompt",
        )
        assertTrue(prompt.contains("do NOT enrich"))
        assertTrue(prompt.contains("MINIMUM changes"))
        assertTrue(prompt.contains("translator", ignoreCase = true))
    }

    @Test
    fun modalitaTraduzioneOmetteLenfasiSulleDiscipline() {
        val prompt = PromptBuilder(translationMode = true).build(
            context(
                disciplineChoices = listOf(
                    DisciplineChoice(id = "d1", disciplineId = "SIXTH_SENSE", choiceText = "Ascolta", nextSceneId = "5"),
                ),
            ),
        )

        assertFalse(prompt.contains("KAI DISCIPLINES"), "l'enfasi è un invito ad arricchire, va saltata")
        // La disciplina resta comunque da tradurre, come una scelta qualunque.
        assertContains(prompt, "DISCIPLINE|SIXTH_SENSE|Ascolta")
    }

    @Test
    fun ilFinaleFabbricatoRestaCreativoAncheInModalitaTraduzione() {
        val vuota = scene().copy(
            id = "__ex_synthetic_defeat__",
            sceneType = SceneType.ENDING,
            narrativeText = "",
        )
        val prompt = PromptBuilder(translationMode = true).build(context(scene = vuota, syntheticEnding = true))

        // Nessun testo sorgente da cui restare fedeli: il finale fabbricato
        // resta l'unico caso creativo, in ENTRAMBE le modalità.
        assertTrue(prompt.contains("FINAL SCENE"))
        assertTrue(prompt.contains("ends here in defeat"))
    }

    @Test
    fun modalitaTraduzioneNonMenzionaIlTono() {
        val prompt = PromptBuilder(translationMode = true).build(context())

        assertFalse(prompt.contains("tone:"), "la modalità traduzione ignora tono e genere per costruzione")
    }

    @Test
    fun modalitaTraduzioneSpentaSiComportaComeOggi() {
        // Di default (translationMode = false) il comportamento non cambia
        // rispetto a prima di questa feature.
        val prompt = PromptBuilder().build(context())

        assertTrue(prompt.contains("enrich", ignoreCase = true))
        assertTrue(prompt.contains("tone:"))
    }
}
