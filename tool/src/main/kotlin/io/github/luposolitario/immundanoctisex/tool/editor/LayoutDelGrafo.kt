package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout
import com.mxgraph.view.mxGraph
import javax.swing.SwingConstants

// I nodi ovali di Graphviz (06/08/2026, Michele: "sarebbe bello invece
// di usare un quadrato usare degli ovali"). Un'ellisse vera inscritta
// nel riquadro del nodo, non una capsula.
object FormaOvale : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(Path().apply { addOval(Rect(0f, 0f, size.width, size.height)) })
}

// Il layout della mappa, affidato a una libreria invece che scritto a
// mano (06/08/2026, Michele: "forse potremmo usare una libreria che
// risolve il problema per noi, perche' inventare l'acqua calda?").
//
// `mxHierarchicalLayout` di JGraphX e' Sugiyama completo: assegna i
// livelli, ordina i nodi per ridurre gli incroci, li posiziona e — la
// fase che mancava del tutto al nostro — **instrada gli archi**, cioe'
// li fa aggirare i nodi invece di attraversare la mappa in linea retta
// passando sopra tutto quello che incontrano. Era quella l'unica vera
// causa del groviglio: l'ordine dei nodi l'avevamo gia' sistemato.
//
// Perche' JGraphX e non Eclipse ELK, che sarebbe migliore: ELK trascina
// `org.eclipse.xtext.xbase.lib`, EPL-2.0 puro, incompatibile con la
// nostra GPL v3 — confermato dal licensing contact della Eclipse
// Foundation in eclipse-xtext/xtext#2590, chiusa senza modifiche.
// JGraphX e' BSD e non ha nessuna dipendenza. Vedi README §15.
object LayoutDelGrafo {

    data class Disposizione(
        val posizioni: Map<String, Offset>,
        // Il percorso di ogni arco, estremi compresi: due punti se e'
        // dritto, di piu' se il layout l'ha fatto aggirare qualcosa.
        val percorsi: Map<Pair<String, String>, List<Offset>>,
    )

    fun calcola(
        sceneIds: List<String>,
        archi: List<Pair<String, String>>,
        larghezzaNodo: Float,
        altezzaNodo: Float,
        spazioFraNodi: Float,
        spazioFraLivelli: Float,
        orizzontale: Boolean,
    ): Disposizione {
        if (sceneIds.isEmpty()) return Disposizione(emptyMap(), emptyMap())

        val grafo = mxGraph()
        val radice = grafo.defaultParent
        val vertici = mutableMapOf<String, Any>()
        val archiPerChiave = mutableMapOf<Pair<String, String>, Any>()

        grafo.model.beginUpdate()
        try {
            sceneIds.forEach { id ->
                vertici[id] = grafo.insertVertex(
                    radice, id, id, 0.0, 0.0, larghezzaNodo.toDouble(), altezzaNodo.toDouble(),
                )
            }
            archi.forEach { (da, a) ->
                val partenza = vertici[da] ?: return@forEach
                val arrivo = vertici[a] ?: return@forEach
                // Un cappio non ha nulla da instradare e confonde il
                // calcolo dei livelli.
                if (da == a) return@forEach
                archiPerChiave[da to a] = grafo.insertEdge(radice, "$da->$a", "", partenza, arrivo)
            }
        } finally {
            grafo.model.endUpdate()
        }

        // NORTH = i livelli scendono; WEST = i livelli vanno verso
        // destra. Sono i due orientamenti che l'editor gia' offriva.
        val layout = mxHierarchicalLayout(
            grafo,
            if (orizzontale) SwingConstants.WEST else SwingConstants.NORTH,
        )
        layout.intraCellSpacing = spazioFraNodi.toDouble()
        layout.interRankCellSpacing = spazioFraLivelli.toDouble()
        layout.execute(radice)

        val posizioni = vertici.mapValues { (_, v) ->
            val g = grafo.model.getGeometry(v)
            Offset(g.x.toFloat(), g.y.toFloat())
        }
        val percorsi = archiPerChiave.mapValues { (chiave, e) ->
            val centroDa = centro(posizioni.getValue(chiave.first), larghezzaNodo, altezzaNodo)
            val centroA = centro(posizioni.getValue(chiave.second), larghezzaNodo, altezzaNodo)
            val intermedi = grafo.model.getGeometry(e)?.points.orEmpty()
                .map { Offset(it.x.toFloat(), it.y.toFloat()) }
            listOf(centroDa) + intermedi + listOf(centroA)
        }
        return Disposizione(posizioni, percorsi)
    }

    private fun centro(angolo: Offset, larghezza: Float, altezza: Float) =
        Offset(angolo.x + larghezza / 2, angolo.y + altezza / 2)
}
