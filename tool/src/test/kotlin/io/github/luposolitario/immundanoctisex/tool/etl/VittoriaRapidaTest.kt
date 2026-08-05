package io.github.luposolitario.immundanoctisex.tool.etl

import io.github.luposolitario.immundanoctisex.tool.defaultDisciplineDescriptors
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

// I libri distinguono due esiti di vittoria a seconda di quanto è
// durato lo scontro (05/08/2026, misurato: 10 scene sui 5 libri):
//
//   "If you win the combat in seven rounds or less, turn to 272.
//    If you win the combat in more than seven rounds, turn to 324."
//
// Prima la seconda riga sovrascriveva la prima e chi vinceva in fretta
// finiva comunque nella scena "lenta". Le frasi qui sotto sono COPIATE
// dai libri veri, una forma per riga.
class VittoriaRapidaTest {

    private fun combatDi(scelte: String): Pair<String?, Int?> {
        val html = """
            <html><body><div class="numbered">
            <h3><a id="sect200">200</a></h3>
            <p>Il nemico attacca.</p>
            <p class="combat">Vonotar: COMBAT SKILL 20 ENDURANCE 30</p>
            $scelte
            <h3><a id="sect272">272</a></h3><p>Rapido.</p>
            <h3><a id="sect324">324</a></h3><p>Lento.</p>
            </div></body></html>
        """.trimIndent()
        val file = File.createTempFile("libro", ".htm").apply { deleteOnExit(); writeText(html) }
        val combat = ProjectAonHtmlParser.parse(
            file = file,
            id = "test",
            title = "Prova",
            description = "",
            genre = "fantasy",
            disciplineDescriptions = defaultDisciplineDescriptors(),
        ).manifest.scenes.first { it.id == "200" }.combat
        return combat?.winSceneIdRapido to combat?.winEntroRound
    }

    // 03tcok sect200
    @Test
    fun `in sette round o meno`() {
        val (rapido, entro) = combatDi(
            """
            <p class="choice">If you win the combat in seven rounds or less, <a href="#sect272">turn to 272</a>.</p>
            <p class="choice">If you win the combat in more than seven rounds, <a href="#sect324">turn to 324</a>.</p>
            """.trimIndent(),
        )
        assertEquals("272", rapido)
        assertEquals(7, entro)
    }

    // 03tcok sect208
    @Test
    fun `entro quattro round`() {
        val (rapido, entro) = combatDi(
            """
            <p class="choice">If you win the combat within four rounds, <a href="#sect272">turn to 272</a>.</p>
            <p class="choice">If you win the fight in more than four rounds, <a href="#sect324">turn to 324</a>.</p>
            """.trimIndent(),
        )
        assertEquals("272", rapido)
        assertEquals(4, entro)
    }

    // 04tcod sect56 — forma diversa: "the fight lasts for N rounds"
    @Test
    fun `se il combattimento dura N round o meno`() {
        val (rapido, entro) = combatDi(
            """
            <p class="choice">If you win and the fight lasts for 3 rounds of combat or less, <a href="#sect272">turn to 272</a>.</p>
            <p class="choice">If you win but the fight lasts longer than 3 rounds, <a href="#sect324">turn to 324</a>.</p>
            """.trimIndent(),
        )
        assertEquals("272", rapido)
        assertEquals(3, entro)
    }

    // 05sots sect91 — come sopra, senza "for"
    @Test
    fun `se il combattimento dura N round senza for`() {
        val (rapido, entro) = combatDi(
            """
            <p class="choice">If you win and the fight lasts 4 rounds of combat or less, <a href="#sect272">turn to 272</a>.</p>
            <p class="choice">If you win and the fight lasts longer than 4 rounds of combat, <a href="#sect324">turn to 324</a>.</p>
            """.trimIndent(),
        )
        assertEquals("272", rapido)
        assertEquals(4, entro)
    }

    // Una vittoria semplice non deve diventare "rapida" per sbaglio.
    @Test
    fun `una vittoria senza condizioni di durata non e rapida`() {
        val (rapido, entro) = combatDi(
            """<p class="choice">If you win the fight, <a href="#sect272">turn to 272</a>.</p>""",
        )
        assertEquals(null, rapido)
        assertEquals(null, entro)
    }
}
