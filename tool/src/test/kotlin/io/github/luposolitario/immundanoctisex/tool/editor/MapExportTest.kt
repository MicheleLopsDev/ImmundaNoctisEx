package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.ui.geometry.Offset
import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import java.awt.Color
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

        esportaMappaComeImmagine(
            destinazione, graph.nodes, graph.edges, m.scenes.associateBy { it.id }, posizioni,
            temaScuro = false, font = FontEditor.ALMENDRA, scalaTesto = ScalaTesto.MEDIO,
        )

        assertTrue(destinazione.exists())
        val immagine = ImageIO.read(destinazione)
        assertTrue(immagine.width > 0 && immagine.height > 0)
    }

    @Test
    fun esportaConTemaScuroScriveComunqueUnPngValido() {
        val scenes = listOf(scene("1", SceneType.START, "2"), scene("2", SceneType.ENDING))
        val m = manifest(scenes)
        val graph = buildSceneGraph(m)
        val posizioni = mapOf("1" to Offset(0f, 0f), "2" to Offset(300f, 0f))
        val destinazione = File(dir, "mappa-scura.png")

        esportaMappaComeImmagine(
            destinazione, graph.nodes, graph.edges, m.scenes.associateBy { it.id }, posizioni,
            temaScuro = true, font = FontEditor.CINZEL, scalaTesto = ScalaTesto.GRANDE,
        )

        assertTrue(destinazione.exists())
        val immagine = ImageIO.read(destinazione)
        assertTrue(immagine.width > 0 && immagine.height > 0)
    }

    // §19.8, bug 31/07/2026 (Michele: "non si legge"): un nodo START/
    // ENDING usava `coloreTipo` sia per il riempimento sia per il
    // bordo — sullo schermo aveva senso solo sopra un'immagine di
    // copertina, ma l'esportazione non disegna mai immagini, quindi il
    // bordo era sempre identico al riempimento (invisibile). Verifica
    // diretta sui pixel: il bordo deve essere sensibilmente più scuro
    // del riempimento, non un confronto sull'aspetto generale.
    @Test
    fun ilBordoDiUnNodoStartRestaDistinguibileDalRiempimento() {
        val m = manifest(listOf(scene("1", SceneType.START)))
        val graph = buildSceneGraph(m)
        val posizioni = mapOf("1" to Offset(0f, 0f))
        val destinazione = File(dir, "start.png")

        esportaMappaComeImmagine(
            destinazione, graph.nodes, graph.edges, m.scenes.associateBy { it.id }, posizioni,
            temaScuro = false, font = FontEditor.ALMENDRA, scalaTesto = ScalaTesto.MEDIO,
        )

        val immagine = ImageIO.read(destinazione)
        // Nodo logico a (0,0) -> pixel (40,40) per via del margine.
        // Bordo: proprio sul contorno superiore, lontano dagli angoli
        // arrotondati. Riempimento: centro del nodo.
        val pixelBordo = Color(immagine.getRGB(40 + 95, 40))
        val pixelRiempimento = Color(immagine.getRGB(40 + 95, 40 + 36))
        val luminositaBordo = pixelBordo.red + pixelBordo.green + pixelBordo.blue
        val luminositaRiempimento = pixelRiempimento.red + pixelRiempimento.green + pixelRiempimento.blue

        assertTrue(luminositaBordo < luminositaRiempimento - 200)
    }

    @Test
    fun esportaSenzaScenePosizionateNonScriveNulla() {
        val m = manifest(listOf(scene("1", SceneType.START)))
        val graph = buildSceneGraph(m)
        val destinazione = File(dir, "mappa.png")

        esportaMappaComeImmagine(
            destinazione, graph.nodes, graph.edges, m.scenes.associateBy { it.id }, emptyMap(),
            temaScuro = false, font = FontEditor.ALMENDRA, scalaTesto = ScalaTesto.MEDIO,
        )

        assertTrue(!destinazione.exists())
    }
}
