package io.github.luposolitario.immundanoctisex.core.engine.inference

import kotlin.test.Test
import kotlin.test.assertEquals

class TestoGeneratoTest {

    // Il caso vero, preso dalla prima traduzione fatta nell'editor
    // (02/08/2026): la risposta è arrivata con un `<pad>` incollato
    // davanti alla prima parola.
    @Test
    fun toglieIlPadDavantiAlTesto() {
        val grezzo = "<pad>Stai in piedi sul bordo della Foresta di Fryelund."

        assertEquals(
            "Stai in piedi sul bordo della Foresta di Fryelund.",
            ripulisciTokenDiServizio(grezzo),
        )
    }

    @Test
    fun toglieIMarcatoriDiTurnoOvunqueSiTrovino() {
        val grezzo = "<|turn>model\nIl sentiero è stretto.<end_of_turn>"

        assertEquals("model\nIl sentiero è stretto.", ripulisciTokenDiServizio(grezzo))
    }

    // La pulizia si applica sia ai pezzi dello stream sia al testo
    // completo: passarla due volte non deve cambiare nulla.
    @Test
    fun applicarlaDueVolteDaLoStessoRisultato() {
        val unaVolta = ripulisciTokenDiServizio("<pad>Testo<eos>")

        assertEquals(unaVolta, ripulisciTokenDiServizio(unaVolta))
    }

    @Test
    fun nonToccaUnTestoPulito() {
        val testo = "Il sentiero davanti è stretto e incolto, e la luce sta svanendo."

        assertEquals(testo, ripulisciTokenDiServizio(testo))
    }

    // Il testo narrato contiene virgolette, apostrofi e trattini: non
    // deve perdere nulla di ciò che l'autore ha scritto.
    @Test
    fun lasciaIntattaLaPunteggiatura() {
        val testo = "'Fermo lì!' — gridò l'uomo, e la porta si chiuse."

        assertEquals(testo, ripulisciTokenDiServizio(testo))
    }
}
