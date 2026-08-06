package io.github.luposolitario.immundanoctisex.tool.editor

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout
import com.mxgraph.view.mxGraph
import kotlin.test.Test
import kotlin.test.assertTrue

// Verifica di fattibilita' (06/08/2026, Michele: "forse potremmo usare
// una libreria che risolve il problema per noi, perche' inventare
// l'acqua calda?" — poi "approfondisci xtext prima di andare avanti").
//
// Approfondito: Eclipse ELK sarebbe stata la scelta migliore, ma
// trascina `org.eclipse.xtext.xbase.lib`, EPL-2.0 puro. Il licensing
// contact della Eclipse Foundation lo conferma incompatibile con la GPL
// (issue eclipse-xtext/xtext#2590), chiusa senza modifiche nel luglio
// 2023. Scartata.
//
// JGraphX: BSD, zero dipendenze, `mxHierarchicalLayout` e' Sugiyama
// completo. Questi test provano le due cose che dovevamo sapere prima
// di impegnarci: che giri headless e che INSTRADI gli archi — la fase
// che manca al nostro layout, e per cui ogni collegamento attraversa la
// mappa in linea retta passando sopra i nodi di mezzo.
class ProvaJGraphXTest {

    private fun grafo(costruisci: (mxGraph, Any) -> Unit): mxGraph {
        val g = mxGraph()
        val radice = g.defaultParent
        g.model.beginUpdate()
        try {
            costruisci(g, radice)
        } finally {
            g.model.endUpdate()
        }
        mxHierarchicalLayout(g).execute(radice)
        return g
    }

    @Test
    fun `dispone un grafo a livelli senza interfaccia grafica`() {
        lateinit var nodi: List<Any>
        val g = grafo { graph, radice ->
            nodi = (1..4).map { graph.insertVertex(radice, "$it", "$it", 0.0, 0.0, 60.0, 30.0) }
            // Un diamante: 1 -> {2,3} -> 4.
            graph.insertEdge(radice, null, "", nodi[0], nodi[1])
            graph.insertEdge(radice, null, "", nodi[0], nodi[2])
            graph.insertEdge(radice, null, "", nodi[1], nodi[3])
            graph.insertEdge(radice, null, "", nodi[2], nodi[3])
        }

        fun y(i: Int) = g.model.getGeometry(nodi[i]).y
        fun x(i: Int) = g.model.getGeometry(nodi[i]).x

        assertTrue(y(0) < y(1), "1 doveva stare sopra 2: ${y(0)} vs ${y(1)}")
        assertTrue(y(1) < y(3), "2 doveva stare sopra 4")
        assertTrue(y(1) == y(2), "2 e 3 dovevano stare allo stesso livello")
        assertTrue(x(1) != x(2), "2 e 3 dovevano essere affiancati")
    }

    @Test
    fun `instrada gli archi che scavalcano con punti di passaggio`() {
        lateinit var scorciatoia: Any
        val g = grafo { graph, radice ->
            val nodi = (1..5).map { graph.insertVertex(radice, "$it", "$it", 0.0, 0.0, 60.0, 30.0) }
            (0 until 4).forEach { graph.insertEdge(radice, null, "", nodi[it], nodi[it + 1]) }
            // L'arco che scavalca tre livelli: dritto, passerebbe sopra
            // i nodi di mezzo.
            scorciatoia = graph.insertEdge(radice, null, "", nodi[0], nodi[4])
        }

        val punti = g.model.getGeometry(scorciatoia)?.points.orEmpty()
        println("arco che scavalca: ${punti.size} punti di passaggio -> ${punti.map { "(${it.x.toInt()}, ${it.y.toInt()})" }}")
        assertTrue(punti.isNotEmpty(), "l'arco lungo doveva essere instradato, non lasciato dritto")
    }

    @Test
    fun `regge un libro intero senza impiegarci troppo`() {
        // 364 scene e 571 archi sono le dimensioni del primo Lupo
        // Solitario: se il layout ci mettesse secondi, non si potrebbe
        // ricalcolare al caricamento.
        val avvio = System.currentTimeMillis()
        val g = grafo { graph, radice ->
            val nodi = (1..364).map { graph.insertVertex(radice, "$it", "$it", 0.0, 0.0, 62.0, 34.0) }
            // Catena principale più rami che riconvergono, come un libro vero.
            nodi.zipWithNext().forEach { (a, b) -> graph.insertEdge(radice, null, "", a, b) }
            (0 until 200 step 3).forEach { graph.insertEdge(radice, null, "", nodi[it], nodi[minOf(it + 4, 363)]) }
        }
        val durata = System.currentTimeMillis() - avvio

        println("layout di 364 nodi: $durata ms")
        assertTrue(durata < 15_000, "troppo lento: $durata ms")
        assertTrue(g.model.getGeometry(g.model.getChildAt(g.defaultParent, 0)).y >= 0.0)
    }
}
