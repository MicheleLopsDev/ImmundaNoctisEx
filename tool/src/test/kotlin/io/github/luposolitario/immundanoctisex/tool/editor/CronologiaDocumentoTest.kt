package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.ui.geometry.Offset
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// §19.13 (Michele: "implementiamo ctrl-z e ctrl-y"): cronologia
// LINEARE — le regole verificate qui sono quelle confermate a parole
// da Michele: annulla torna allo stato precedente, ripeti torna avanti
// SOLO se non è successo nient'altro nel frattempo, altrimenti la
// pila "avanti" viene scartata da una nuova azione.
class CronologiaDocumentoTest {

    private fun manifest(id: String) = Manifest(
        id = id, version = "1.0.0", title = "Test", description = "Test", language = "en", genre = "FANTASY",
    )

    private fun documento(id: String, x: Float = 0f) = Documento(manifest(id), mapOf("1" to Offset(x, 0f)))

    @Test
    fun annullareSenzaCronologiaNonFaNulla() {
        val cronologia = CronologiaDocumento()

        assertNull(cronologia.annulla(documento("attuale")))
    }

    @Test
    fun ripetereSenzaCronologiaNonFaNulla() {
        val cronologia = CronologiaDocumento()

        assertNull(cronologia.ripeti(documento("attuale")))
    }

    @Test
    fun annullareTornaAlloStatoRegistrato() {
        val cronologia = CronologiaDocumento()
        cronologia.registraCheckpoint(documento("prima"))

        val risultato = cronologia.annulla(documento("dopo"))

        assertEquals(documento("prima"), risultato)
    }

    @Test
    fun dopoUnAnnullaSiPuoRipetereTornandoAvanti() {
        val cronologia = CronologiaDocumento()
        cronologia.registraCheckpoint(documento("prima"))
        cronologia.annulla(documento("dopo"))

        val risultato = cronologia.ripeti(documento("prima"))

        assertEquals(documento("dopo"), risultato)
    }

    @Test
    fun tornoIndietroDiDuePassiEPoiAvantiDiDue() {
        val cronologia = CronologiaDocumento()
        // Tre stati in fila: A -> B -> C (C è quello "attuale" a fine catena).
        cronologia.registraCheckpoint(documento("A"))
        cronologia.registraCheckpoint(documento("B"))

        val dopoUnAnnulla = cronologia.annulla(documento("C"))
        assertEquals(documento("B"), dopoUnAnnulla)
        val dopoDueAnnulla = cronologia.annulla(dopoUnAnnulla!!)
        assertEquals(documento("A"), dopoDueAnnulla)

        val dopoUnRipeti = cronologia.ripeti(dopoDueAnnulla!!)
        assertEquals(documento("B"), dopoUnRipeti)
        val dopoDueRipeti = cronologia.ripeti(dopoUnRipeti!!)
        assertEquals(documento("C"), dopoDueRipeti)
    }

    // Il punto confermato esplicitamente da Michele: una nuova azione
    // dopo un annulla scarta sempre la pila "avanti", non la conserva.
    @Test
    fun unaNuovaAzioneDopoUnAnnullaScartaLaPilaAvanti() {
        val cronologia = CronologiaDocumento()
        cronologia.registraCheckpoint(documento("A"))
        cronologia.registraCheckpoint(documento("B"))
        val dopoAnnulla = cronologia.annulla(documento("C"))!! // torna a B, "C" ora in pila avanti

        // Azione nuova da "B": invece di ripetere verso C, si fa
        // qualcos'altro (documento "D").
        cronologia.registraCheckpoint(dopoAnnulla)

        assertNull(cronologia.ripeti(documento("D")))
    }

    @Test
    fun reimpostareSvuotaEntrambeLePile() {
        val cronologia = CronologiaDocumento()
        cronologia.registraCheckpoint(documento("A"))
        cronologia.annulla(documento("B")) // "B" ora in pila avanti

        cronologia.reimposta()

        assertNull(cronologia.annulla(documento("qualunque")))
        assertNull(cronologia.ripeti(documento("qualunque")))
    }
}
