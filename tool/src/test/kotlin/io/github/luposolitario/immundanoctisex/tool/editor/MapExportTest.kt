package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.ui.geometry.Offset
import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class MapExportTest {

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

    private lateinit var dir: File

    @BeforeTest
    fun setup() {
        dir = createTempDirectory("editor-export-test").toFile()
    }

    @AfterTest
    fun cleanup() {
        dir.deleteRecursively()
    }

    @Test
    fun esportaScriveUnPngLeggibileConUnNodoPerScena() {
        val scenes = listOf(scene("1", SceneType.START, "2"), scene("2", SceneType.ENDING))
        val m = manifest(scenes)
        val graph = buildSceneGraph(m)
        val posizioni = mapOf("1" to Offset(0f, 0f), "2" to Offset(300f, 0f))
        val destinazione = File(dir, "mappa.png")

        esportaMappaComeImmagine(destinazione, graph.nodes, graph.edges, m.scenes.associateBy { it.id }, posizioni)

        assertTrue(destinazione.exists())
        val immagine = ImageIO.read(destinazione)
        assertTrue(immagine.width > 0 && immagine.height > 0)
    }

    @Test
    fun esportaSenzaScenePosizionateNonScriveNulla() {
        val m = manifest(listOf(scene("1", SceneType.START)))
        val graph = buildSceneGraph(m)
        val destinazione = File(dir, "mappa.png")

        esportaMappaComeImmagine(destinazione, graph.nodes, graph.edges, m.scenes.associateBy { it.id }, emptyMap())

        assertTrue(!destinazione.exists())
    }
}
