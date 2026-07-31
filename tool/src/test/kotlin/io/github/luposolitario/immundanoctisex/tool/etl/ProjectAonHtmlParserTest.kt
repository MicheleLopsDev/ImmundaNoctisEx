package io.github.luposolitario.immundanoctisex.tool.etl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// BUG (31/07/2026, Michele: la scena della Tabella dei Numeri Casuali di
// "Flight from the Dark" appariva come 2 scelte manuali invece del tiro del
// dado a bottone singolo — DiceZone/requiresRoll lato client funzionano già,
// il problema era la regex del convertitore che non riconosceva la frase
// reale del libro). Testato direttamente sulla funzione estratta
// (rollRangeFor), senza dover costruire un intero file XHTML.
class ProjectAonHtmlParserTest {

    @Test
    fun ilTestoConPickedRiconosceLIntervallo() {
        // Testo reale, "Flight from the Dark" scena 275 (log di Michele).
        val range = ProjectAonHtmlParser.rollRangeFor(
            "If the number you have picked is 0–4, turn to 345.",
        )
        assertEquals(0 to 4, range)
    }

    @Test
    fun ilTestoSenzaPickedRiconosceComunqueLIntervallo() {
        // Stessa scena, seconda scelta: nessuna parola "picked" (si
        // riferisce a un "Pick a number" detto una sola volta nella prosa
        // sopra) — la vecchia regex non lo riconosceva affatto.
        val range = ProjectAonHtmlParser.rollRangeFor(
            "If the number is 5–9, turn to 74.",
        )
        assertEquals(5 to 9, range)
    }

    @Test
    fun unSingoloNumeroSenzaIntervalloUsaLoStessoValoreComeMinEMax() {
        val range = ProjectAonHtmlParser.rollRangeFor("If the number is 0, turn to 10.")
        assertEquals(0 to 0, range)
    }

    @Test
    fun testoSenzaTabellaDeiNumeriRestituisceNull() {
        assertNull(ProjectAonHtmlParser.rollRangeFor("If you wish to take the right path into the wood, turn to 85."))
    }
}
