package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType

// Riferimenti in uscita di una scena verso altre scene — stessi campi che
// GraphValidator (core/data) già controlla per l'esistenza; qui riusati
// per costruire il grafo e per colorare i nodi (doc/EDITOR.md §6.2).
internal fun Scene.outgoingSceneIds(): List<String> = buildList {
    choices.forEach { add(it.nextSceneId) }
    disciplineChoices.forEach { add(it.nextSceneId) }
    combat?.let { combat ->
        add(combat.winSceneId)
        combat.loseSceneId?.let(::add)
        combat.evadeSceneId?.let(::add)
    }
}

// healthy = verde (§6.2): tutti i riferimenti in uscita puntano a scene
// che esistono davvero. Basta anche un solo riferimento a una scena non
// ancora creata per colorare il nodo di rosso — non blocca l'editing
// (§7.3), è solo un segnale visivo.
data class GraphNode(
    val sceneId: String,
    val level: Int,
    val healthy: Boolean,
)

data class GraphEdge(
    val fromSceneId: String,
    val toSceneId: String,
    val resolved: Boolean,
)

data class SceneGraph(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
)

// Auto-layout gerarchico (§6.1): livello = distanza minima da START via
// BFS. Scene non raggiungibili da START (nessun libro reale dovrebbe
// averne, ma un libro a metà scrittura sì) finiscono su un livello a
// parte, Int.MAX_VALUE — mai un crash su un grafo con orfani o senza
// START (libro appena creato/incompleto).
fun buildSceneGraph(manifest: Manifest): SceneGraph {
    val scenesById = manifest.scenes.associateBy { it.id }
    val start = manifest.scenes.firstOrNull { it.sceneType == SceneType.START }

    val levels = mutableMapOf<String, Int>()
    if (start != null) {
        val queue = ArrayDeque<String>()
        levels[start.id] = 0
        queue.add(start.id)
        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val currentLevel = levels.getValue(currentId)
            val current = scenesById[currentId] ?: continue
            current.outgoingSceneIds().forEach { nextId ->
                if (nextId !in levels && scenesById.containsKey(nextId)) {
                    levels[nextId] = currentLevel + 1
                    queue.add(nextId)
                }
            }
        }
    }

    val nodes = manifest.scenes.map { scene ->
        val healthy = scene.outgoingSceneIds().all { scenesById.containsKey(it) }
        GraphNode(sceneId = scene.id, level = levels[scene.id] ?: Int.MAX_VALUE, healthy = healthy)
    }

    val edges = manifest.scenes.flatMap { scene ->
        scene.outgoingSceneIds().map { targetId ->
            GraphEdge(fromSceneId = scene.id, toSceneId = targetId, resolved = scenesById.containsKey(targetId))
        }
    }

    return SceneGraph(nodes = nodes, edges = edges)
}
