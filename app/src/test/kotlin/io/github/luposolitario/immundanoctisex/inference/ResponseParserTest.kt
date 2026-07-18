package io.github.luposolitario.immundanoctisex.inference

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Il parser è la rete di sicurezza dell'inferenza: qui si verifica
// soprattutto che NON blocchi mai il gioco (output monco, righe
// malformate, modello che ignora il formato -> si degrada sull'originale
// del pacchetto).
class ResponseParserTest {

    private fun scene(
        choices: List<Choice> = emptyList(),
        disciplineChoices: List<DisciplineChoice> = emptyList(),
        combat: Combat? = null,
    ) = Scene(
        id = "3",
        sceneType = SceneType.TRANSITION,
        genre = "FANTASY",
        narrativeText = "The alley narrows as the old quarter swallows the daylight.",
        choices = choices,
        disciplineChoices = disciplineChoices,
        combat = combat,
    )

    private fun choice(id: String, next: String, text: String) =
        Choice(id = id, choiceText = text, nextSceneId = next)

    @Test
    fun estraeNarrativaEScelteTradotte() {
        val scene = scene(
            choices = listOf(
                choice("c1", "4", "Walk on, hand on your weapon"),
                choice("c2", "5", "Slip into the shadows"),
            ),
        )
        val raw = """
            Il vicolo si stringe mentre il quartiere vecchio inghiotte la luce.
            --- TAGS ---
            CHOICE|4|1|Avanza, la mano sull'arma
            CHOICE|5|2|Scivola tra le ombre
        """.trimIndent()

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Il vicolo si stringe mentre il quartiere vecchio inghiotte la luce.", result.narrative)
        assertEquals("Avanza, la mano sull'arma", result.choiceTexts["c1"])
        assertEquals("Scivola tra le ombre", result.choiceTexts["c2"])
    }

    @Test
    fun ilTestoEUltimoCampo_unPipeNelTestoNonRompeIlParsing() {
        val scene = scene(choices = listOf(choice("c1", "4", "Original")))
        val raw = "Prosa.\n--- TAGS ---\nCHOICE|4|1|Avanza | poi svolta a destra"

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Avanza | poi svolta a destra", result.choiceTexts["c1"])
    }

    @Test
    fun sceltaMancante_tieneIlTestoOriginaleDelPacchetto() {
        val scene = scene(
            choices = listOf(
                choice("c1", "4", "Walk on, hand on your weapon"),
                choice("c2", "5", "Slip into the shadows"),
            ),
        )
        // Il modello traduce solo la prima riga.
        val raw = "Prosa.\n--- TAGS ---\nCHOICE|4|1|Avanza, la mano sull'arma"

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Avanza, la mano sull'arma", result.choiceTexts["c1"])
        assertEquals("Slip into the shadows", result.choiceTexts["c2"])
    }

    @Test
    fun sceneIdSbagliato_ripiegaSulConteggio() {
        val scene = scene(
            choices = listOf(
                choice("c1", "4", "First"),
                choice("c2", "5", "Second"),
            ),
        )
        // Il modello inventa le destinazioni ma l'ORDINE è giusto.
        val raw = """
            Prosa.
            --- TAGS ---
            CHOICE|99|1|Prima tradotta
            CHOICE|98|2|Seconda tradotta
        """.trimIndent()

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Prima tradotta", result.choiceTexts["c1"])
        assertEquals("Seconda tradotta", result.choiceTexts["c2"])
    }

    @Test
    fun dueScelteVersoLaStessaScena_nonRicevonoLaStessaRiga() {
        val scene = scene(
            choices = listOf(
                choice("c1", "5", "First"),
                choice("c2", "5", "Second"),
            ),
        )
        val raw = """
            Prosa.
            --- TAGS ---
            CHOICE|5|1|Prima tradotta
            CHOICE|5|2|Seconda tradotta
        """.trimIndent()

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Prima tradotta", result.choiceTexts["c1"])
        assertEquals("Seconda tradotta", result.choiceTexts["c2"])
    }

    @Test
    fun senzaSeparatore_tuttoENarrativaEITestiRestanoOriginali() {
        val scene = scene(choices = listOf(choice("c1", "4", "Original text")))
        val raw = "Il modello ha ignorato il formato e ha scritto solo prosa."

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Il modello ha ignorato il formato e ha scritto solo prosa.", result.narrative)
        assertEquals("Original text", result.choiceTexts["c1"])
    }

    @Test
    fun rispostaVuota_degradaSulTestoOriginaleDellaScena() {
        val scene = scene(choices = listOf(choice("c1", "4", "Original text")))

        val result = ResponseParser.parse("   ", scene)

        assertEquals(scene.narrativeText, result.narrative)
        assertEquals("Original text", result.choiceTexts["c1"])
    }

    @Test
    fun righeMalformateIgnorateSenzaEccezioni() {
        val scene = scene(choices = listOf(choice("c1", "4", "Original")))
        val raw = """
            Prosa.
            --- TAGS ---
            CHOICE|4
            CHOICE|
            DISCIPLINE|
            rumore a caso
            CHOICE|4|1|
        """.trimIndent()

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Original", result.choiceTexts["c1"])
    }

    @Test
    fun disciplineTradotteEMancantiInFallback() {
        val scene = scene(
            disciplineChoices = listOf(
                DisciplineChoice("d1", "SIXTH_SENSE", "You sense the ambush", "5"),
                DisciplineChoice("d2", "CAMOUFLAGE", "You blend with the shadows", "5"),
            ),
        )
        val raw = "Prosa.\n--- TAGS ---\nDISCIPLINE|SIXTH_SENSE|Percepisci l'imboscata"

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Percepisci l'imboscata", result.disciplineChoiceTexts["d1"])
        assertEquals("You blend with the shadows", result.disciplineChoiceTexts["d2"])
    }

    @Test
    fun nomeNemicoTradottoConFallbackSulPacchetto() {
        val combat = Combat(
            enemyName = "Warehouse Thugs",
            enemyCombatSkill = 16,
            enemyEndurance = 24,
            winSceneId = "6",
        )
        val withEnemy = scene(combat = combat)

        val tradotto = ResponseParser.parse("Prosa.\n--- TAGS ---\nENEMY|Sgherri del magazzino", withEnemy)
        assertEquals("Sgherri del magazzino", tradotto.enemyName)

        val senzaRiga = ResponseParser.parse("Prosa.\n--- TAGS ---", withEnemy)
        assertEquals("Warehouse Thugs", senzaRiga.enemyName)

        // Scena senza combattimento: nessun nemico da nominare.
        assertNull(ResponseParser.parse("Prosa.", scene()).enemyName)
    }

    @Test
    fun narrativeOf_mostraSoloCioCheStaPrimaDelSeparatore() {
        val streaming = "Prima parte della prosa\n--- TAGS ---\nCHOICE|4|1|non mostrare"

        assertEquals("Prima parte della prosa", ResponseParser.narrativeOf(streaming))
    }
}
