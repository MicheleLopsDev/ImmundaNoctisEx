package io.github.luposolitario.immundanoctisex.tool.editor

import org.eclipse.elk.alg.layered.options.LayeredOptions
import org.eclipse.elk.core.RecursiveGraphLayoutEngine
import org.eclipse.elk.core.data.LayoutMetaDataService
import org.eclipse.elk.core.options.CoreOptions
import org.eclipse.elk.core.options.Direction
import org.eclipse.elk.core.util.BasicProgressMonitor
import org.eclipse.elk.graph.ElkNode
import org.eclipse.elk.graph.util.ElkGraphUtil
import kotlin.test.Test
import kotlin.test.assertTrue

// Verifica di fattibilita' (06/08/2026, Michele: "forse potremmo usare
// una libreria che risolve il problema per noi, perche' inventare
// l'acqua calda?"). Risposta: si', e la libreria e' Eclipse Layout
// Kernel.
//
// Questi due test provano le uniche due cose che dovevamo sapere prima
// di impegnarci: che ELK giri fuori da Eclipse (nessun OSGi, nessun
// workbench) e che faccia l'INSTRADAMENTO degli archi — la fase che il
// nostro layout non ha, e per cui ogni collegamento attraversa la mappa
// in linea retta passando sopra i nodi di mezzo.
//
// Una trappola trovata qui: ELK usa `org.eclipse.xtext.xbase.lib` nel
// codice generato ma NON la dichiara nel POM. Senza, il ServiceLoader
// fallisce con un NoClassDefFoundError su CollectionLiterals.
class ProvaElkTest {

    @Test
    fun `elk dispone un grafo a livelli senza bisogno di Eclipse`() {
        // Registrazione degli algoritmi: fuori da OSGi va fatta a mano,
        // altrimenti ELK non trova "layered" e lascia tutto a zero.
        LayoutMetaDataService.getInstance()
            .registerLayoutMetaDataProviders(org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider())

        val radice = ElkGraphUtil.createGraph()
        radice.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered")
        radice.setProperty(CoreOptions.DIRECTION, Direction.DOWN)
        radice.setProperty(LayeredOptions.SPACING_NODE_NODE, 40.0)

        // Un diamante: 1 -> {2,3} -> 4.
        val nodi = (1..4).map { n ->
            ElkGraphUtil.createNode(radice).apply {
                identifier = "$n"
                width = 60.0
                height = 30.0
            }
        }
        ElkGraphUtil.createSimpleEdge(nodi[0], nodi[1])
        ElkGraphUtil.createSimpleEdge(nodi[0], nodi[2])
        ElkGraphUtil.createSimpleEdge(nodi[1], nodi[3])
        ElkGraphUtil.createSimpleEdge(nodi[2], nodi[3])

        RecursiveGraphLayoutEngine().layout(radice, BasicProgressMonitor())

        val perId = nodi.associateBy { it.identifier }
        fun y(id: String) = perId.getValue(id).y
        fun x(id: String) = perId.getValue(id).x

        // I livelli sono ordinati lungo Y...
        assertTrue(y("1") < y("2"), "1 doveva stare sopra 2, era ${y("1")} vs ${y("2")}")
        assertTrue(y("2") < y("4"), "2 doveva stare sopra 4")
        // ...2 e 3 stanno sullo stesso livello, affiancati...
        assertTrue(y("2") == y("3"), "2 e 3 dovevano stare allo stesso livello")
        assertTrue(x("2") != x("3"), "2 e 3 dovevano essere affiancati")
        // ...e il grafo ha una dimensione vera, non zero.
        assertTrue(radice.width > 0 && radice.height > 0, "grafo senza dimensioni: ${radice.width}x${radice.height}")
    }

    @Test
    fun `elk instrada gli archi con punti di passaggio`() {
        LayoutMetaDataService.getInstance()
            .registerLayoutMetaDataProviders(org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider())

        val radice = ElkGraphUtil.createGraph()
        radice.setProperty(CoreOptions.ALGORITHM, "org.eclipse.elk.layered")
        radice.setProperty(CoreOptions.DIRECTION, Direction.DOWN)

        // Una catena con una scorciatoia che scavalca: e' il caso in cui
        // un arco dritto attraverserebbe i nodi di mezzo.
        val nodi = (1..5).map { n ->
            ElkGraphUtil.createNode(radice).apply { identifier = "$n"; width = 60.0; height = 30.0 }
        }
        (0 until 4).forEach { ElkGraphUtil.createSimpleEdge(nodi[it], nodi[it + 1]) }
        val scorciatoia = ElkGraphUtil.createSimpleEdge(nodi[0], nodi[4])

        RecursiveGraphLayoutEngine().layout(radice, BasicProgressMonitor())

        // L'arco lungo ha una sezione con punti di passaggio: e' quello
        // che il nostro disegno a linea retta non puo' avere.
        val sezioni = scorciatoia.sections
        assertTrue(sezioni.isNotEmpty(), "l'arco doveva avere almeno una sezione")
        val punti = sezioni.first().bendPoints
        println("arco lungo: ${punti.size} punti di passaggio, sezione da (${sezioni.first().startX}, ${sezioni.first().startY}) a (${sezioni.first().endX}, ${sezioni.first().endY})")
    }
}
