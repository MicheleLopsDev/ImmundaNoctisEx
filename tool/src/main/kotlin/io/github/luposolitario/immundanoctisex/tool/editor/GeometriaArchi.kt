package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

// La geometria delle frecce fra i nodi (06/08/2026, Michele: "aggiungerei
// le frecce per permettere anche situazioni in cui le chiamate sono
// bidirezionali e quelle in cui non lo sono").
//
// Due problemi da risolvere, entrambi geometrici e quindi qui, fuori dal
// disegno:
//
// 1. gli archi vanno da centro a centro, ma i nodi sono disegnati SOPRA
//    il canvas degli archi: una punta al centro del nodo di arrivo
//    sarebbe invisibile. Va fermata sul bordo dell'ellisse;
// 2. quando esistono sia A->B sia B->A, le due linee si sovrappongono
//    perfettamente e si vede un solo tratto con l'aria di essere
//    bidirezionale per caso. Vanno scostate di lato, ognuna con la sua
//    punta.
object GeometriaArchi {

    // Il punto in cui il segmento da `centro` verso `versoIlCentroDi`
    // esce dall'ellisse di semiassi (a, b). Nessuna approssimazione: e'
    // la soluzione dell'equazione dell'ellisse lungo quella direzione.
    fun bordoEllisse(centro: Offset, versoIlCentroDi: Offset, semiasseX: Float, semiasseY: Float): Offset {
        val dx = versoIlCentroDi.x - centro.x
        val dy = versoIlCentroDi.y - centro.y
        if (dx == 0f && dy == 0f) return centro
        // (t*dx/a)^2 + (t*dy/b)^2 = 1  ->  t = 1 / sqrt((dx/a)^2 + (dy/b)^2)
        val t = 1f / sqrt((dx / semiasseX) * (dx / semiasseX) + (dy / semiasseY) * (dy / semiasseY))
        return Offset(centro.x + dx * t, centro.y + dy * t)
    }

    // I due lati della V della punta, sul segmento che arriva in `punta`
    // provenendo da `da`. `apertura` e' la meta' dell'angolo, in radianti.
    fun alettePunta(
        da: Offset,
        punta: Offset,
        lunghezza: Float = 11f,
        apertura: Float = 0.42f,
    ): Pair<Offset, Offset> {
        val dx = punta.x - da.x
        val dy = punta.y - da.y
        val lung = hypot(dx, dy)
        if (lung < 0.001f) return punta to punta
        // Versore che punta ALL'INDIETRO lungo l'arco: le alette partono
        // dalla punta e tornano verso la provenienza.
        val ux = -dx / lung
        val uy = -dy / lung
        val cos = kotlin.math.cos(apertura)
        val sin = kotlin.math.sin(apertura)
        return Offset(
            punta.x + (ux * cos - uy * sin) * lunghezza,
            punta.y + (ux * sin + uy * cos) * lunghezza,
        ) to Offset(
            punta.x + (ux * cos + uy * sin) * lunghezza,
            punta.y + (-ux * sin + uy * cos) * lunghezza,
        )
    }

    // Scostamento perpendicolare all'arco, per non far coincidere A->B e
    // B->A.
    //
    // Basta la perpendicolare al vettore da->a: siccome i due archi di
    // una coppia hanno vettori opposti, le loro perpendicolari lo sono
    // gia'. Il primo tentativo moltiplicava anche per il confronto fra
    // gli id, e i due segni si annullavano mandando entrambe le linee
    // dallo stesso lato — trovato dal test, non a occhio.
    fun scostamento(da: Offset, a: Offset, ampiezza: Float = 7f): Offset {
        val dx = a.x - da.x
        val dy = a.y - da.y
        val lung = hypot(dx, dy)
        if (lung < 0.001f) return Offset.Zero
        return Offset(-dy / lung * ampiezza, dx / lung * ampiezza)
    }

    // Le coppie di scene collegate nei DUE versi. Sono da segnalare
    // perche' in un librogame sono rare: quasi sempre si va avanti, e un
    // "si torna indietro" e' una scelta di struttura, non un caso.
    fun coppieBidirezionali(archi: List<Pair<String, String>>): Set<Pair<String, String>> {
        val insieme = archi.toSet()
        return archi.filter { (da, a) -> da != a && (a to da) in insieme }.toSet()
    }

    // Un arco che torna sulla scena stessa: nessun segmento da disegnare,
    // e senza questo controllo la direzione sarebbe (0,0) e la punta
    // finirebbe in un punto qualunque.
    fun eCappio(da: String, a: String) = da == a

    // Quanto e' lungo un arco: serve a non disegnare la punta sui
    // collegamenti cortissimi, dove coprirebbe il nodo invece di
    // indicarlo.
    fun abbastanzaLungo(da: Offset, a: Offset, minimo: Float = 26f) =
        hypot(abs(a.x - da.x), abs(a.y - da.y)) > minimo
}
