package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

class AllineamentoGruppoTest {

    @Test
    fun ordinaPerIdNumericoConLIdPiuBassoComeAncora() {
        // ID "10" prima di "3" per ordine di stringa, ma per ID
        // numerico "3" viene prima — verifica che non sia un ordine
        // lessicografico.
        val posizioni = mapOf("10" to Offset(500f, 500f), "3" to Offset(10f, 20f))

        val nuove = allineaGruppo(setOf("10", "3"), posizioni, orizzontale = true, spaziatura = 100f)

        assertEquals(Offset(10f, 20f), nuove.getValue("3"))
        assertEquals(Offset(110f, 20f), nuove.getValue("10"))
    }

    @Test
    fun inRigaVarialAsseXMantenendoYCostante() {
        val posizioni = mapOf("1" to Offset(0f, 50f), "2" to Offset(300f, 300f), "3" to Offset(600f, 10f))

        val nuove = allineaGruppo(setOf("1", "2", "3"), posizioni, orizzontale = true, spaziatura = 230f)

        assertEquals(Offset(0f, 50f), nuove.getValue("1"))
        assertEquals(Offset(230f, 50f), nuove.getValue("2"))
        assertEquals(Offset(460f, 50f), nuove.getValue("3"))
    }

    @Test
    fun inColonnaVarialAsseYMantenendoXCostante() {
        val posizioni = mapOf("1" to Offset(50f, 0f), "2" to Offset(300f, 300f))

        val nuove = allineaGruppo(setOf("1", "2"), posizioni, orizzontale = false, spaziatura = 130f)

        assertEquals(Offset(50f, 0f), nuove.getValue("1"))
        assertEquals(Offset(50f, 130f), nuove.getValue("2"))
    }

    @Test
    fun unaSelezioneVuotaNonProduceNulla() {
        val nuove = allineaGruppo(emptySet(), emptyMap(), orizzontale = true, spaziatura = 100f)

        assertEquals(emptyMap(), nuove)
    }

    @Test
    fun unNodoSenzaPosizioneNotaUsaLOrigineComeAncora() {
        val nuove = allineaGruppo(setOf("1", "2"), emptyMap(), orizzontale = true, spaziatura = 100f)

        assertEquals(Offset(0f, 0f), nuove.getValue("1"))
        assertEquals(Offset(100f, 0f), nuove.getValue("2"))
    }
}
