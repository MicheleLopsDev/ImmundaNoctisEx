package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// La geometria delle frecce fra i nodi: dove finisce la linea (sul bordo
// dell'ellisse, non al centro, perche' i nodi coprono il canvas degli
// archi) e da che lato passa quando la coppia e' collegata nei due versi.
class GeometriaArchiTest {

    private fun vicini(a: Offset, b: Offset, tolleranza: Float = 0.01f) =
        abs(a.x - b.x) < tolleranza && abs(a.y - b.y) < tolleranza

    @Test
    fun `il bordo a destra di un ellisse sta a un semiasse dal centro`() {
        val centro = Offset(100f, 100f)
        val bordo = GeometriaArchi.bordoEllisse(centro, Offset(500f, 100f), semiasseX = 95f, semiasseY = 36f)

        assertTrue(vicini(bordo, Offset(195f, 100f)), "atteso (195, 100), era $bordo")
    }

    @Test
    fun `il bordo in basso sta a un semiasse verticale dal centro`() {
        val centro = Offset(100f, 100f)
        val bordo = GeometriaArchi.bordoEllisse(centro, Offset(100f, 900f), semiasseX = 95f, semiasseY = 36f)

        assertTrue(vicini(bordo, Offset(100f, 136f)), "atteso (100, 136), era $bordo")
    }

    @Test
    fun `un punto in diagonale cade sull ellisse, non sul rettangolo`() {
        val centro = Offset(0f, 0f)
        val a = 95f
        val b = 36f
        val bordo = GeometriaArchi.bordoEllisse(centro, Offset(100f, 100f), a, b)

        // Verifica l'equazione dell'ellisse: (x/a)^2 + (y/b)^2 = 1.
        val eq = (bordo.x / a) * (bordo.x / a) + (bordo.y / b) * (bordo.y / b)
        assertTrue(abs(eq - 1f) < 0.001f, "il punto non sta sull'ellisse: eq = $eq")
    }

    @Test
    fun `le alette della punta stanno dietro la punta e sono simmetriche`() {
        val da = Offset(0f, 0f)
        val punta = Offset(100f, 0f)
        val (sinistra, destra) = GeometriaArchi.alettePunta(da, punta, lunghezza = 10f)

        // Entrambe indietro rispetto alla punta...
        assertTrue(sinistra.x < punta.x && destra.x < punta.x)
        // ...e a distanza uguale da essa.
        val dS = hypot(sinistra.x - punta.x, sinistra.y - punta.y)
        val dD = hypot(destra.x - punta.x, destra.y - punta.y)
        assertTrue(abs(dS - dD) < 0.01f, "alette asimmetriche: $dS e $dD")
        assertTrue(abs(dS - 10f) < 0.01f, "lunghezza attesa 10, era $dS")
        // Una sopra e una sotto l'asse.
        assertTrue(sinistra.y * destra.y < 0, "le alette dovevano stare da parti opposte")
    }

    @Test
    fun `i due versi di una coppia si scostano da lati opposti`() {
        val a = Offset(0f, 0f)
        val b = Offset(100f, 0f)

        val andata = GeometriaArchi.scostamento(a, b)
        val ritorno = GeometriaArchi.scostamento(b, a)

        // Verso opposto: e' proprio quello che li rende due linee
        // distinte invece di una sola.
        assertTrue(andata.y * ritorno.y < 0, "atteso scostamento opposto, erano $andata e $ritorno")
    }

    @Test
    fun `lo scostamento e perpendicolare all arco`() {
        val a = Offset(0f, 0f)
        val b = Offset(100f, 0f)

        val s = GeometriaArchi.scostamento(a, b, ampiezza = 7f)

        assertTrue(abs(s.x) < 0.01f, "su un arco orizzontale lo scostamento e' verticale, era $s")
        assertTrue(abs(abs(s.y) - 7f) < 0.01f, "ampiezza attesa 7, era ${s.y}")
    }

    @Test
    fun `si riconoscono le coppie collegate nei due versi`() {
        val archi = listOf("1" to "2", "2" to "1", "2" to "3", "3" to "4")

        val doppie = GeometriaArchi.coppieBidirezionali(archi)

        assertEquals(setOf("1" to "2", "2" to "1"), doppie)
    }

    @Test
    fun `un cappio su se stessa non e una coppia bidirezionale`() {
        assertEquals(emptySet(), GeometriaArchi.coppieBidirezionali(listOf("1" to "1")))
        assertTrue(GeometriaArchi.eCappio("1", "1"))
        assertTrue(!GeometriaArchi.eCappio("1", "2"))
    }

    @Test
    fun `sui collegamenti cortissimi la punta non si disegna`() {
        assertTrue(!GeometriaArchi.abbastanzaLungo(Offset(0f, 0f), Offset(10f, 0f)))
        assertTrue(GeometriaArchi.abbastanzaLungo(Offset(0f, 0f), Offset(200f, 0f)))
    }

    @Test
    fun `due nodi sovrapposti non fanno esplodere il calcolo`() {
        val stesso = Offset(50f, 50f)

        assertEquals(stesso, GeometriaArchi.bordoEllisse(stesso, stesso, 95f, 36f))
        assertEquals(Offset.Zero, GeometriaArchi.scostamento(stesso, stesso))
        assertEquals(stesso to stesso, GeometriaArchi.alettePunta(stesso, stesso))
    }
}
