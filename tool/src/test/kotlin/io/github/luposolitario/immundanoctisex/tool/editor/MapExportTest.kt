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

    // §19.8 (Michele, 31/07/2026: "scrivi nei riquadri il testo narrato
    // visibile"): verifica che l'estratto venga disegnato per davvero,
    // non solo che il PNG sia valido — conta i pixel "scuri" (testo)
    // nella fascia sotto l'etichetta id+codice, deve essercene di più
    // con un testo narrato lungo che con uno vuoto.
    @Test
    fun esportaConTestoNarratoDisegnaPiuTestoDiUnaSceneSenzaTesto() {
        val conTesto = scene("1", SceneType.START).copy(
            narrativeText = "Un testo narrato abbastanza lungo da riempire almeno due righe nel riquadro del nodo esportato",
        )
        val senzaTesto = scene("1", SceneType.START).copy(narrativeText = "")
        val posizioni = mapOf("1" to Offset(0f, 0f))

        val destConTesto = File(dir, "con-testo.png")
        val destSenzaTesto = File(dir, "senza-testo.png")
        val mConTesto = manifest(listOf(conTesto))
        val mSenzaTesto = manifest(listOf(senzaTesto))

        esportaMappaComeImmagine(
            destConTesto, buildSceneGraph(mConTesto).nodes, buildSceneGraph(mConTesto).edges,
            mConTesto.scenes.associateBy { it.id }, posizioni,
            temaScuro = false, font = FontEditor.ALMENDRA, scalaTesto = ScalaTesto.MEDIO,
        )
        esportaMappaComeImmagine(
            destSenzaTesto, buildSceneGraph(mSenzaTesto).nodes, buildSceneGraph(mSenzaTesto).edges,
            mSenzaTesto.scenes.associateBy { it.id }, posizioni,
            temaScuro = false, font = FontEditor.ALMENDRA, scalaTesto = ScalaTesto.MEDIO,
        )

        fun pixelScuriNellaFasciaDelTesto(file: File): Int {
            val immagine = ImageIO.read(file)
            var conteggio = 0
            for (y in 63..92) {
                for (x in 48..214) {
                    val c = Color(immagine.getRGB(x, y))
                    if (c.red + c.green + c.blue < 500) conteggio++
                }
            }
            return conteggio
        }

        assertTrue(pixelScuriNellaFasciaDelTesto(destConTesto) > pixelScuriNellaFasciaDelTesto(destSenzaTesto))
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
