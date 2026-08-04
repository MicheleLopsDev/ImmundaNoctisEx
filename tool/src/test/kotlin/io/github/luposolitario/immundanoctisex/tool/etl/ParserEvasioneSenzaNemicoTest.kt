package io.github.luposolitario.immundanoctisex.tool.etl

import io.github.luposolitario.immundanoctisex.tool.defaultDisciplineDescriptors
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// "Combatti OPPURE evita", ma il combattimento è nella scena dopo
// (03/08/2026). Il ramo dell'evasione finiva in `Combat.evadeSceneId`;
// siccome in quella scena nessun nemico è dichiarato, il `Combat` non
// veniva mai costruito e la scelta spariva in silenzio — il giocatore
// si trovava solo "combatti", senza poter schivare uno scontro che il
// libro gli concede.
//
// Trovato confrontando le nostre conversioni col grafo ufficiale dei
// percorsi che Project Aon pubblica (`/en/svg/lw/01fftd.svgz`, idea di
// Michele): 35 archi mancanti sui 5 libri, 19 di questo tipo.
class ParserEvasioneSenzaNemicoTest {

    // Struttura copiata dal file vero (xhtml-simple), scena 262 di
    // "Flight from the Dark": h3 con l'ancora, poi i <p class="choice">.
    private fun libroConScena(corpoScena: String): File {
        val html = """
            <html><body>
            <div class="numbered">
            <h3><a id="sect262">262</a></h3>
            $corpoScena
            <h3><a id="sect191">191</a></h3>
            <p>Combatti.</p>
            <h3><a id="sect234">234</a></h3>
            <p>Salti giù dal carro.</p>
            </div>
            </body></html>
        """.trimIndent()
        val file = File.createTempFile("libro", ".htm")
        file.deleteOnExit()
        file.writeText(html)
        return file
    }

    private fun scene(corpoScena: String) = ProjectAonHtmlParser.parse(
        file = libroConScena(corpoScena),
        id = "test",
        title = "Prova",
        description = "",
        genre = "fantasy",
        disciplineDescriptions = defaultDisciplineDescriptors(),
    ).manifest.scenes

    @Test
    fun `la scelta di evadere resta anche se il nemico e nella scena dopo`() {
        val scena = scene(
            """
            <p>His bodyguard attacks you with his scimitar.</p>
            <p class="choice">If you wish to fight, <a href="#sect191">turn to 191</a>.</p>
            <p class="choice">If you wish to evade combat, jump clear by <a href="#sect234">turning to 234</a>.</p>
            """.trimIndent(),
        ).first { it.id == "262" }

        assertEquals(
            listOf("191", "234"),
            scena.choices.map { it.nextSceneId },
            "l'evasione senza nemico dichiarato non deve sparire",
        )
        assertTrue(scena.choices.any { it.choiceText.contains("evade") })
    }

    // Quando il nemico C'È, l'evasione deve restare dov'era: dentro il
    // combattimento, non come scelta libera — altrimenti si potrebbe
    // fuggire senza subire il round di danni previsto dalle regole.
    @Test
    fun `col nemico dichiarato l'evasione resta dentro il combattimento`() {
        val scena = scene(
            """
            <p>Il bruto ti attacca.</p>
            <p class="combat">Bodyguard: COMBAT SKILL 16 ENDURANCE 25</p>
            <p class="choice">If you wish to evade combat after two rounds of combat, <a href="#sect234">turn to 234</a>.</p>
            """.trimIndent(),
        ).first { it.id == "262" }

        assertEquals("234", scena.combat?.evadeSceneId)
        assertEquals(2, scena.combat?.evadeAfterRound)
        assertTrue(
            scena.choices.none { it.nextSceneId == "234" },
            "con un combattimento vero l'evasione non va duplicata fra le scelte",
        )
    }

    // Stesso ragionamento per "se vinci": senza nemico in questa scena
    // sarebbe un'uscita persa.
    @Test
    fun `anche il ramo della vittoria si recupera se non c'e' nemico`() {
        val scena = scene(
            """
            <p>Lo scontro è inevitabile.</p>
            <p class="choice">If you win the fight, <a href="#sect191">turn to 191</a>.</p>
            """.trimIndent(),
        ).first { it.id == "262" }

        assertEquals(listOf("191"), scena.choices.map { it.nextSceneId })
    }
}
