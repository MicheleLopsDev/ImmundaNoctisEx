package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SceneGraphTest {

    private fun scene(id: String, type: SceneType = SceneType.TRANSITION, vararg destinations: String) = Scene(
        id = id,
        sceneType = type,
        genre = "FANTASY",
        narrativeText = "testo",
        choices = destinations.mapIndexed { index, dest -> Choice("c$index", "vai", nextSceneId = dest) },
    )

    private fun manifest(scenes: List<Scene>) = Manifest(
        id = "test", version = "1.0.0", title = "Test", description = "Test",
        language = "en", genre = "FANTASY", scenes = scenes,
    )

    @Test
    fun iLivelliSeguonoLaDistanzaDaStart() {
        val scenes = listOf(
            scene("1", SceneType.START, "2"),
            scene("2", destinations = arrayOf("3")),
            scene("3", SceneType.ENDING),
        )

        val graph = buildSceneGraph(manifest(scenes))

        assertEquals(0, graph.nodes.single { it.sceneId == "1" }.level)
        assertEquals(1, graph.nodes.single { it.sceneId == "2" }.level)
        assertEquals(2, graph.nodes.single { it.sceneId == "3" }.level)
    }

    @Test
    fun unaScenaOrfanaFinisceSuUnLivelloAParte() {
        val scenes = listOf(
            scene("1", SceneType.START, "2"),
            scene("2", SceneType.ENDING),
            scene("99", SceneType.ENDING), // nessuno ci punta
        )

        val graph = buildSceneGraph(manifest(scenes))

        assertEquals(Int.MAX_VALUE, graph.nodes.single { it.sceneId == "99" }.level)
    }

    @Test
    fun unaScenaConDestinazioneEsistenteEVerde() {
        val scenes = listOf(scene("1", SceneType.START, "2"), scene("2", SceneType.ENDING))

        val graph = buildSceneGraph(manifest(scenes))

        assertTrue(graph.nodes.single { it.sceneId == "1" }.healthy)
    }

    @Test
    fun unaScenaConDestinazioneMancanteERossa() {
        val scenes = listOf(scene("1", SceneType.START, "99"))

        val graph = buildSceneGraph(manifest(scenes))

        assertTrue(!graph.nodes.single { it.sceneId == "1" }.healthy)
        assertTrue(graph.edges.single().let { !it.resolved && it.toSceneId == "99" })
    }

    @Test
    fun nessunCrashSenzaSceneStart() {
        val scenes = listOf(scene("5", SceneType.ENDING))

        val graph = buildSceneGraph(manifest(scenes))

        assertEquals(Int.MAX_VALUE, graph.nodes.single().level)
    }
}
