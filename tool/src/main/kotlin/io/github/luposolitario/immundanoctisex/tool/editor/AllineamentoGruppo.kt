package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.ui.geometry.Offset

// §19.11 (Michele: "un'opzione per riarrangiare solo i nodi selezionati
// in verticale o in orizzontale"): geometria pura, testabile senza un
// ambiente Compose — stesso principio di RettangoloSelezione.kt.
//
// Ordine deterministico per ID numerico (Michele, 31/07/2026: "l'id è
// sempre quello che qualifica la posizione più basso è più su... è una
// condizione deterministica pensata fin dall'inizio per evitare
// problemi" — stessa regola già in uso altrove nell'editor, es.
// l'ordine di "Lega scena X→Y"/"Duplica gruppo"): l'ID più basso resta
// ancorato alla sua posizione attuale, gli altri si dispongono in
// riga/colonna a partire da lì, nello stesso ordine.
fun allineaGruppo(
    idsSelezionati: Set<String>,
    posizioni: Map<String, Offset>,
    orizzontale: Boolean,
    spaziatura: Float,
): Map<String, Offset> {
    val ordinati = idsSelezionati.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
    if (ordinati.isEmpty()) return emptyMap()
    val ancora = posizioni[ordinati.first()] ?: Offset.Zero
    return ordinati.mapIndexed { indice, id ->
        val pos = if (orizzontale) {
            Offset(ancora.x + indice * spaziatura, ancora.y)
        } else {
            Offset(ancora.x, ancora.y + indice * spaziatura)
        }
        id to pos
    }.toMap()
}
