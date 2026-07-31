package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.ui.geometry.Offset

// §19.10 (Michele: "serve la multi selezione tramite rettangolo quando
// ci sono tante foglie, è più comodo"): geometria pura, separata da
// MapScreen.kt per essere testabile senza un ambiente Compose — stesso
// principio di SceneGraph.kt/NuovaScena.kt.

// Inverso della trasformazione del `graphicsLayer` di MapScreen.kt
// (scaleX/Y = zoom, translationX/Y = pan): porta un punto in coordinate
// SCHERMO (quelle del mouse) nello stesso spazio logico delle posizioni
// dei nodi (`posizioneEffettiva`).
fun schermoALogico(schermo: Offset, pan: Offset, zoom: Float): Offset =
    Offset((schermo.x - pan.x) / zoom, (schermo.y - pan.y) / zoom)

// Nodi che un rettangolo (in coordinate logiche, due angoli qualunque)
// tocca almeno in parte — criterio a intersezione, non a contenimento
// totale (Michele: più comodo quando il rettangolo non è preciso).
fun nodiNelRettangolo(
    nodi: List<GraphNode>,
    posizioni: Map<String, Offset>,
    angoloA: Offset,
    angoloB: Offset,
    larghezzaNodo: Float,
    altezzaNodo: Float,
): Set<String> {
    val minX = minOf(angoloA.x, angoloB.x)
    val maxX = maxOf(angoloA.x, angoloB.x)
    val minY = minOf(angoloA.y, angoloB.y)
    val maxY = maxOf(angoloA.y, angoloB.y)
    return nodi.mapNotNull { nodo ->
        val pos = posizioni[nodo.sceneId] ?: return@mapNotNull null
        val tocca = pos.x <= maxX && pos.x + larghezzaNodo >= minX &&
            pos.y <= maxY && pos.y + altezzaNodo >= minY
        nodo.sceneId.takeIf { tocca }
    }.toSet()
}
