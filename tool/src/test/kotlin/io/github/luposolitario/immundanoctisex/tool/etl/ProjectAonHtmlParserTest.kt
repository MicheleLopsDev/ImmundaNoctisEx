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

    // Forme raccolte dal controllo esaustivo sui 5 libri convertiti
    // (01/08/2026): la versione precedente della regex, ancorata solo su
    // "number ... is", le perdeva tutte e tre.

    @Test
    fun laFormaConPickedANumberVieneRiconosciuta() {
        assertEquals(0 to 4, ProjectAonHtmlParser.rollRangeFor("If you have picked a number 0–4, turn to 343."))
    }

    @Test
    fun laFormaConPickedSenzaLaParolaNumberVieneRiconosciuta() {
        assertEquals(0 to 1, ProjectAonHtmlParser.rollRangeFor("If you have picked 0–1, turn to 53."))
    }

    @Test
    fun laFormaAbbreviataItIsVieneRiconosciuta() {
        // Si riferisce al numero estratto, nominato nella frase precedente.
        assertEquals(2 to 4, ProjectAonHtmlParser.rollRangeFor("If it is 2–4, turn to 274."))
    }

    // --- Casi che NON vanno convertiti: tiro modificato da un bonus ---
    // "Pick a number... se hai la Disciplina X aggiungi 2... se il totale
    // è 0–3": minRoll/maxRoll si confrontano col tiro GREZZO, convertirli
    // ignorerebbe il bonus e manderebbe il giocatore nella scena sbagliata.

    @Test
    fun unTotaleConBonusNonDiventaUnTiro() {
        assertNull(ProjectAonHtmlParser.rollRangeFor("If your total score is now 0–3, turn to 58."))
        assertNull(ProjectAonHtmlParser.rollRangeFor("If your total is now 4–6, turn to 167."))
    }

    @Test
    fun unIntervalloFuoriDaZeroNoveNonDiventaUnTiro() {
        // 7–11 è impossibile con un tiro grezzo 0-9: c'è per forza un
        // modificatore, anche senza la parola "total" nella frase.
        assertNull(ProjectAonHtmlParser.rollRangeFor("If it is 7–11, turn to 329."))
    }
}
