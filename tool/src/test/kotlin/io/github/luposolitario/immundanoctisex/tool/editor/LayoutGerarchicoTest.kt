package io.github.luposolitario.immundanoctisex.tool.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// L'ordinamento dei nodi dentro un livello per ridurre gli incroci,
// come fa Graphviz nei grafi di Project Aon. Il metodo e' il baricentro
// (Sugiyama): un nodo va sopra la media delle posizioni dei suoi vicini.
class LayoutGerarchicoTest {

    private fun incrociDi(livelli: Map<Int, List<String>>, archi: List<Pair<String, String>>) =
        LayoutGerarchico.incroci(livelli, livelli.keys.sorted(), archi.groupBy({ it.first }, { it.second }))

    @Test
    fun `due archi paralleli non si incrociano`() {
        val livelli = mapOf(0 to listOf("a", "b"), 1 to listOf("x", "y"))
        assertEquals(0, incrociDi(livelli, listOf("a" to "x", "b" to "y")))
    }

    @Test
    fun `due archi invertiti si incrociano una volta`() {
        val livelli = mapOf(0 to listOf("a", "b"), 1 to listOf("x", "y"))
        assertEquals(1, incrociDi(livelli, listOf("a" to "y", "b" to "x")))
    }

    @Test
    fun `un ordine invertito viene raddrizzato`() {
        // a va a x, b va a y, ma il secondo livello e' scritto al
        // contrario: un incrocio che si toglie scambiando due nodi.
        val livelli = mapOf(0 to listOf("a", "b"), 1 to listOf("y", "x"))
        val archi = listOf("a" to "x", "b" to "y")

        val ordinato = LayoutGerarchico.ordina(livelli, archi)

        assertEquals(0, incrociDi(ordinato, archi))
        assertEquals(listOf("x", "y"), ordinato.getValue(1))
    }

    @Test
    fun `il groviglio di un libro finto si scioglie`() {
        // Tre livelli in cui ogni nodo punta al "gemello" dell'ordine
        // opposto: il caso peggiore, e si risolve tutto.
        val livelli = mapOf(
            0 to listOf("a1", "a2", "a3", "a4"),
            1 to listOf("b1", "b2", "b3", "b4"),
            2 to listOf("c1", "c2", "c3", "c4"),
        )
        val archi = listOf(
            "a1" to "b4", "a2" to "b3", "a3" to "b2", "a4" to "b1",
            "b1" to "c4", "b2" to "c3", "b3" to "c2", "b4" to "c1",
        )

        val prima = incrociDi(livelli, archi)
        val dopo = incrociDi(LayoutGerarchico.ordina(livelli, archi), archi)

        assertTrue(prima > 0, "il caso di partenza doveva avere incroci, ne aveva $prima")
        assertEquals(0, dopo)
    }

    @Test
    fun `i nodi senza collegamenti restano al loro posto`() {
        // "solo" non ha vicini: non deve finire in testa solo perche' il
        // suo baricentro non esiste.
        val livelli = mapOf(0 to listOf("a"), 1 to listOf("x", "solo", "y"))
        val archi = listOf("a" to "x", "a" to "y")

        val ordinato = LayoutGerarchico.ordina(livelli, archi)

        assertEquals(1, ordinato.getValue(1).indexOf("solo"))
    }

    @Test
    fun `riordinare due volte da lo stesso risultato`() {
        val livelli = mapOf(
            0 to listOf("a", "b", "c"),
            1 to listOf("x", "y", "z"),
        )
        val archi = listOf("a" to "z", "b" to "x", "c" to "y")

        val uno = LayoutGerarchico.ordina(livelli, archi)
        val due = LayoutGerarchico.ordina(livelli, archi)

        assertEquals(uno, due)
    }

    @Test
    fun `un grafo con un solo livello resta com e`() {
        val livelli = mapOf(0 to listOf("a", "b"))
        assertEquals(livelli, LayoutGerarchico.ordina(livelli, emptyList()))
    }

    @Test
    fun `l ordinamento non perde ne duplica nodi`() {
        val livelli = mapOf(
            0 to listOf("a", "b"),
            1 to listOf("x", "y", "z"),
            2 to listOf("k"),
        )
        val archi = listOf("a" to "z", "b" to "x", "a" to "y", "y" to "k", "z" to "k")

        val ordinato = LayoutGerarchico.ordina(livelli, archi)

        assertEquals(livelli.mapValues { it.value.toSet() }, ordinato.mapValues { it.value.toSet() })
    }
}
