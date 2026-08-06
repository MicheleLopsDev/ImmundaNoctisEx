package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

// I nodi ovali di Graphviz (06/08/2026, Michele: "sarebbe bello invece
// di usare un quadrato usare degli ovali"). Un'ellisse vera inscritta
// nel riquadro del nodo, non una capsula: e' la forma con cui `dot`
// disegna i grafi di Project Aon, e a colpo d'occhio distingue una
// mappa di storia da un diagramma di flusso tecnico.
object FormaOvale : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(
        Path().apply { addOval(Rect(0f, 0f, size.width, size.height)) },
    )
}

// Come Graphviz dispone i grafi dei percorsi che Project Aon pubblica
// per ogni libro (06/08/2026, Michele: "sarebbe bello che l'editor
// potesse riportare il grafo come il file svg del libro, separando bene
// i rami e permettendo a un umano di avere un'idea a occhio").
//
// I livelli li calcola gia' `buildSceneGraph` (distanza da START). Cio'
// che mancava e' il passo successivo dell'algoritmo di Sugiyama:
// **in che ORDINE stanno i nodi dentro un livello**. Finora era l'ordine
// numerico dell'id — deterministico, ma cieco al grafo: due scene
// consecutive per numero possono stare ai due capi opposti della storia,
// e ogni collegamento fra loro attraversa tutta la mappa. Da li' il
// groviglio di linee.
//
// Il metodo e' il baricentro: un nodo va messo sopra la media delle
// posizioni dei nodi a cui e' collegato nel livello accanto. Ripetuto
// alternando le direzioni, scioglie i grovigli. Non e' un ottimo — il
// minimo assoluto degli incroci e' NP-difficile — ma e' quello che usa
// `dot`, ed e' il motivo per cui i suoi grafi si leggono.
object LayoutGerarchico {

    // `livelli`: id delle scene raggruppati per livello, nell'ordine di
    // partenza. Torna gli stessi id riordinati dentro ogni livello.
    fun ordina(
        livelli: Map<Int, List<String>>,
        archi: List<Pair<String, String>>,
        passate: Int = PASSATE,
    ): Map<Int, List<String>> {
        if (livelli.size < 2) return livelli

        val indici = livelli.keys.sorted()
        val successori = archi.groupBy({ it.first }, { it.second })
        val predecessori = archi.groupBy({ it.second }, { it.first })

        var corrente = livelli.mapValues { it.value }
        var migliore = corrente
        var incrociMigliori = incroci(corrente, indici, successori)

        repeat(passate) { passata ->
            // Si alterna: una passata guarda in su (ordina per i
            // predecessori), la successiva in giu'. Guardare sempre dallo
            // stesso lato lascia irrisolti i grovigli dell'altro.
            corrente = if (passata % 2 == 0) {
                unaPassata(corrente, indici, predecessori)
            } else {
                unaPassata(corrente, indici.reversed(), successori)
            }
            val incroci = incroci(corrente, indici, successori)
            if (incroci < incrociMigliori) {
                incrociMigliori = incroci
                migliore = corrente
            }
        }
        return migliore
    }

    // Riordina ogni livello per il baricentro dei vicini nel livello
    // gia' sistemato. I nodi senza vicini di quel lato non hanno un
    // baricentro: restano dove sono, invece di finire tutti in testa.
    private fun unaPassata(
        livelli: Map<Int, List<String>>,
        ordineDiVisita: List<Int>,
        vicini: Map<String, List<String>>,
    ): Map<Int, List<String>> {
        val posizioni = mutableMapOf<String, Double>()
        livelli.forEach { (_, nodi) -> nodi.forEachIndexed { i, id -> posizioni[id] = i.toDouble() } }

        val nuovo = livelli.toMutableMap()
        ordineDiVisita.forEach { livello ->
            val nodi = nuovo[livello] ?: return@forEach
            val conBaricentro = nodi.mapIndexed { indice, id ->
                val posizioniVicini = vicini[id].orEmpty().mapNotNull { posizioni[it] }
                // Chiave secondaria = posizione attuale: mantiene stabile
                // l'ordine fra nodi a pari baricentro, cosi' due riordini
                // di fila danno lo stesso risultato.
                Triple(id, posizioniVicini.average().takeUnless { it.isNaN() }, indice)
            }
            val (collegati, isolati) = conBaricentro.partition { it.second != null }
            val ordinati = collegati.sortedWith(compareBy({ it.second!! }, { it.third }))
            // Gli isolati tornano alle loro posizioni originali: si
            // reinseriscono dove stavano, non in coda.
            val risultato = MutableList(nodi.size) { "" }
            isolati.forEach { risultato[it.third] = it.first }
            val liberi = risultato.indices.filter { risultato[it].isEmpty() }
            ordinati.forEachIndexed { i, voce -> risultato[liberi[i]] = voce.first }
            nuovo[livello] = risultato
            risultato.forEachIndexed { i, id -> posizioni[id] = i.toDouble() }
        }
        return nuovo
    }

    // Incroci fra tutte le coppie di livelli adiacenti. Due archi si
    // incrociano quando l'ordine delle partenze e quello degli arrivi
    // sono invertiti.
    fun incroci(
        livelli: Map<Int, List<String>>,
        indici: List<Int>,
        successori: Map<String, List<String>>,
    ): Int {
        var totale = 0
        indici.zipWithNext().forEach { (sopra, sotto) ->
            val posSopra = livelli[sopra].orEmpty().withIndex().associate { it.value to it.index }
            val posSotto = livelli[sotto].orEmpty().withIndex().associate { it.value to it.index }
            val coppie = posSopra.keys.flatMap { da ->
                successori[da].orEmpty().mapNotNull { a -> posSotto[a]?.let { posSopra.getValue(da) to it } }
            }
            for (i in coppie.indices) {
                for (j in i + 1 until coppie.size) {
                    val (a1, b1) = coppie[i]
                    val (a2, b2) = coppie[j]
                    if ((a1 < a2 && b1 > b2) || (a2 < a1 && b2 > b1)) totale++
                }
            }
        }
        return totale
    }

    // Sei passate: sui cinque libri Project Aon (350+ scene) il conteggio
    // degli incroci smette di calare fra la quarta e la sesta, e il costo
    // resta sotto i pochi millisecondi. Il layout si ricalcola solo al
    // caricamento e su "Riordina", non a ogni fotogramma.
    private const val PASSATE = 6
}
