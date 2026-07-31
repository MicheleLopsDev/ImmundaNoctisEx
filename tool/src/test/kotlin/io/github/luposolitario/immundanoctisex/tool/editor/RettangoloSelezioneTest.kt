package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

class RettangoloSelezioneTest {

    private fun nodo(id: String) = GraphNode(sceneId = id, level = 0, healthy = true)

    @Test
    fun schermoALogicoAnnullaPanEZoom() {
        val logico = schermoALogico(Offset(120f, 220f), pan = Offset(20f, 20f), zoom = 2f)
        assertEquals(Offset(50f, 100f), logico)
    }

    @Test
    fun schermoALogicoSenzaPanNeZoomERestaInvariato() {
        val logico = schermoALogico(Offset(30f, 40f), pan = Offset.Zero, zoom = 1f)
        assertEquals(Offset(30f, 40f), logico)
    }

    @Test
    fun unNodoInteramenteDentroIlRettangoloVieneSelezionato() {
        val nodi = listOf(nodo("1"))
        val posizioni = mapOf("1" to Offset(10f, 10f))

        val selezionati = nodiNelRettangolo(nodi, posizioni, Offset(0f, 0f), Offset(300f, 300f), larghezzaNodo = 190f, altezzaNodo = 72f)

        assertEquals(setOf("1"), selezionati)
    }

    @Test
    fun unNodoToccatoSoloParzialmenteVieneSelezionato() {
        // Rettangolo che copre solo l'angolo in alto a sinistra del nodo
        // (nodo da (10,10) a (200,82)) — criterio a intersezione, non a
        // contenimento totale.
        val nodi = listOf(nodo("1"))
        val posizioni = mapOf("1" to Offset(10f, 10f))

        val selezionati = nodiNelRettangolo(nodi, posizioni, Offset(0f, 0f), Offset(50f, 50f), larghezzaNodo = 190f, altezzaNodo = 72f)

        assertEquals(setOf("1"), selezionati)
    }

    @Test
    fun unNodoFuoriDalRettangoloNonVieneSelezionato() {
        val nodi = listOf(nodo("1"))
        val posizioni = mapOf("1" to Offset(500f, 500f))

        val selezionati = nodiNelRettangolo(nodi, posizioni, Offset(0f, 0f), Offset(50f, 50f), larghezzaNodo = 190f, altezzaNodo = 72f)

        assertEquals(emptySet(), selezionati)
    }

    @Test
    fun gliAngoliDelRettangoloPossonoEssereInQualunqueOrdine() {
        val nodi = listOf(nodo("1"))
        val posizioni = mapOf("1" to Offset(10f, 10f))

        // Angolo "inizio" in basso a destra, "fine" in alto a sinistra —
        // il rettangolo di selezione lo si disegna trascinando in
        // qualunque direzione, non solo verso il basso-destra.
        val selezionati = nodiNelRettangolo(nodi, posizioni, Offset(300f, 300f), Offset(0f, 0f), larghezzaNodo = 190f, altezzaNodo = 72f)

        assertEquals(setOf("1"), selezionati)
    }

    @Test
    fun unNodoSenzaPosizioneNotaVieneIgnorato() {
        val nodi = listOf(nodo("1"), nodo("2"))
        val posizioni = mapOf("1" to Offset(10f, 10f))

        val selezionati = nodiNelRettangolo(nodi, posizioni, Offset(0f, 0f), Offset(300f, 300f), larghezzaNodo = 190f, altezzaNodo = 72f)

        assertEquals(setOf("1"), selezionati)
    }
}
