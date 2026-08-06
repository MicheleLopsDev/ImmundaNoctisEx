package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Il layout della mappa, ora affidato a JGraphX (vedi LayoutDelGrafo.kt
// e DIARIO 06/08). Si verifica quello che conta per l'editor: che i
// livelli escano in ordine, che gli archi lunghi vengano INSTRADATI
// invece di attraversare la mappa in linea retta, e che su un libro
// vero da 364 scene il calcolo resti nell'ordine del secondo — si fa a
// ogni caricamento.
class LayoutDelGrafoTest {

    private fun calcola(
        ids: List<String>,
        archi: List<Pair<String, String>>,
        orizzontale: Boolean = false,
    ) = LayoutDelGrafo.calcola(
        sceneIds = ids,
        archi = archi,
        larghezzaNodo = 190f,
        altezzaNodo = 72f,
        spazioFraNodi = 110f,
        spazioFraLivelli = 70f,
        orizzontale = orizzontale,
    )

    @Test
    fun `i livelli scendono e i fratelli si affiancano`() {
        val d = calcola(
            listOf("1", "2", "3", "4"),
            listOf("1" to "2", "1" to "3", "2" to "4", "3" to "4"),
        )

        val p = d.posizioni
        assertTrue(p.getValue("1").y < p.getValue("2").y, "1 doveva stare sopra 2")
        assertTrue(p.getValue("2").y < p.getValue("4").y, "2 doveva stare sopra 4")
        assertEquals(p.getValue("2").y, p.getValue("3").y, "2 e 3 sullo stesso livello")
        assertTrue(p.getValue("2").x != p.getValue("3").x, "2 e 3 dovevano affiancarsi")
    }

    // Il bug del 06/08: in orizzontale i nodi si SOVRAPPONEVANO, perche'
    // la spaziatura veniva ricavata sottraendo sempre la larghezza del
    // nodo anche dove l'asse trasversale era l'altezza — 130 - 190 = -60,
    // spaziatura negativa. Questo controllo vale per entrambi gli
    // orientamenti proprio perche' l'errore stava in uno solo.
    private fun verificaNessunaSovrapposizione(orizzontale: Boolean) {
        val ids = (1..9).map { "$it" }
        // Un ventaglio: 1 si apre in quattro rami, che riconvergono.
        val archi = listOf("1" to "2", "1" to "3", "1" to "4", "1" to "5") +
            listOf("2" to "6", "3" to "6", "4" to "7", "5" to "7") +
            listOf("6" to "8", "7" to "8", "8" to "9")
        val d = calcola(ids, archi, orizzontale)

        val riquadri = d.posizioni.mapValues { (_, p) ->
            listOf(p.x, p.y, p.x + 190f, p.y + 72f)
        }
        riquadri.entries.toList().let { voci ->
            for (i in voci.indices) {
                for (j in i + 1 until voci.size) {
                    val (a, b) = voci[i].value to voci[j].value
                    val sovrapposti = a[0] < b[2] && b[0] < a[2] && a[1] < b[3] && b[1] < a[3]
                    assertTrue(
                        !sovrapposti,
                        "${voci[i].key} e ${voci[j].key} si sovrappongono " +
                            "(orizzontale=$orizzontale): $a e $b",
                    )
                }
            }
        }
    }

    @Test
    fun `in verticale nessun nodo si sovrappone a un altro`() {
        verificaNessunaSovrapposizione(orizzontale = false)
    }

    @Test
    fun `in orizzontale nessun nodo si sovrappone a un altro`() {
        verificaNessunaSovrapposizione(orizzontale = true)
    }

    @Test
    fun `in orizzontale i livelli vanno di lato invece che in giu`() {
        val d = calcola(listOf("1", "2", "3"), listOf("1" to "2", "2" to "3"), orizzontale = true)

        val p = d.posizioni
        assertTrue(p.getValue("1").x < p.getValue("2").x, "in orizzontale i livelli crescono in x")
        assertTrue(p.getValue("2").x < p.getValue("3").x)
    }

    @Test
    fun `gli archi vengono instradati, e quello che scavalca piu degli altri`() {
        val ids = (1..5).map { "$it" }
        val d = calcola(
            ids,
            (1..4).map { "$it" to "${it + 1}" } + listOf("1" to "5"),
        )

        // Il percorso comprende sempre i due centri: piu' di due punti
        // significa che il layout l'ha fatto aggirare qualcosa.
        // mxHierarchicalLayout instrada TUTTI gli archi, anche i brevi
        // (li fa uscire dritti dal nodo prima di piegare) — non solo
        // quelli che scavalcano, come mi aspettavo scrivendo il test.
        val lungo = d.percorsi.getValue("1" to "5")
        val corto = d.percorsi.getValue("1" to "2")
        assertTrue(lungo.size > 2, "l'arco che scavalca doveva essere instradato, aveva ${lungo.size} punti")
        assertTrue(
            lungo.size >= corto.size,
            "chi scavalca tre livelli non puo' avere meno deviazioni di chi ne scavalca zero: " +
                "${lungo.size} contro ${corto.size}",
        )
    }

    @Test
    fun `ogni percorso comincia e finisce nei centri dei suoi due nodi`() {
        val d = calcola(listOf("1", "2"), listOf("1" to "2"))

        val percorso = d.percorsi.getValue("1" to "2")
        val centro1 = d.posizioni.getValue("1").let { androidx.compose.ui.geometry.Offset(it.x + 95f, it.y + 36f) }
        assertEquals(centro1, percorso.first())
    }

    @Test
    fun `un cappio non entra nel layout`() {
        // Una scena che punta a se stessa non ha niente da instradare e
        // confonderebbe il calcolo dei livelli.
        val d = calcola(listOf("1", "2"), listOf("1" to "1", "1" to "2"))

        assertTrue(("1" to "1") !in d.percorsi)
        assertTrue(("1" to "2") in d.percorsi)
    }

    @Test
    fun `un grafo vuoto non fa esplodere niente`() {
        val d = calcola(emptyList(), emptyList())

        assertTrue(d.posizioni.isEmpty())
        assertTrue(d.percorsi.isEmpty())
    }

    @Test
    fun `il primo libro vero si dispone in tempo utile`() {
        val file = File("../doc/LIBRI/01fftd.json")
        if (!file.exists()) return // i libri Project Aon non sono su ogni macchina

        val manifest = Json { ignoreUnknownKeys = true }
            .decodeFromString(Manifest.serializer(), file.readText())
        val noti = manifest.scenes.map { it.id }.toSet()
        val archi = manifest.scenes.flatMap { scena ->
            scena.outgoingSceneIds().filter { it in noti }.map { scena.id to it }
        }

        val avvio = System.currentTimeMillis()
        val d = calcola(manifest.scenes.map { it.id }, archi)
        val durata = System.currentTimeMillis() - avvio

        val instradati = d.percorsi.count { it.value.size > 2 }
        println(
            "01fftd: ${manifest.scenes.size} scene, ${archi.size} archi, " +
                "$instradati instradati (${100 * instradati / archi.size}%), $durata ms",
        )
        assertEquals(manifest.scenes.size, d.posizioni.size, "ogni scena deve avere una posizione")
        assertTrue(durata < 20_000, "layout troppo lento: $durata ms")
    }
}
