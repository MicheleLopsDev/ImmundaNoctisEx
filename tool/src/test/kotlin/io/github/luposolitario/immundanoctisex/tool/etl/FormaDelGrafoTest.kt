package io.github.luposolitario.immundanoctisex.tool.etl

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.EndingOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// La forma del grafo contro quella dei librogame pubblicati
// (doc/FORMA-DEI-GRAFI.md). Serve alla seconda passata sui libri
// generati da un'IA: il difetto tipico e' il racconto lineare
// travestito da librogame, che il validatore normale non vede.
class FormaDelGrafoTest {

    private fun scena(id: String, verso: List<String>, tipo: SceneType = SceneType.TRANSITION) = Scene(
        id = id,
        sceneType = tipo,
        genre = "fantasy",
        narrativeText = "testo",
        choices = verso.mapIndexed { i, a -> Choice(id = "${id}_c$i", choiceText = "vai", nextSceneId = a) },
    )

    private fun finale(id: String, esito: EndingOutcome?) = Scene(
        id = id, sceneType = SceneType.ENDING, genre = "fantasy", narrativeText = "testo", outcome = esito,
    )

    private fun libro(vararg scene: Scene) = Manifest(
        id = "prova", version = "1.0.0", title = "Prova", description = "",
        language = "it", genre = "fantasy", scenes = scene.toList(),
    )

    // La forma tipica misurata sui librogame veri: una catena di
    // "diamanti", cioe' un bivio i cui due rami rientrano due tappe
    // dopo. Ogni blocco aggiunge tre scene e quattro collegamenti.
    private fun libroDiDiamanti(blocchi: Int, vararg extra: Scene): Manifest {
        val scene = mutableListOf<Scene>()
        repeat(blocchi) { i ->
            val bivio = 1 + i * 3
            val tipo = if (i == 0) SceneType.START else SceneType.TRANSITION
            scene += scena("$bivio", listOf("${bivio + 1}", "${bivio + 2}"), tipo)
            scene += scena("${bivio + 1}", listOf("${bivio + 3}"))
            scene += scena("${bivio + 2}", listOf("${bivio + 3}"))
        }
        val ultimo = 1 + blocchi * 3
        return libro(
            *scene.toTypedArray(),
            scena("$ultimo", listOf("fine", "morte")),
            finale("fine", EndingOutcome.VICTORY),
            finale("morte", EndingOutcome.DEFEAT),
            *extra,
        )
    }

    // Un libro corto ma strutturalmente sano: sotto le venti scene le
    // percentuali non vogliono dire niente.
    private fun libroCorto() = libro(
        scena("1", listOf("2", "3"), SceneType.START),
        scena("2", listOf("4")),
        scena("3", listOf("4")),
        scena("4", listOf("5", "6")),
        finale("5", EndingOutcome.VICTORY),
        finale("6", EndingOutcome.DEFEAT),
    )

    @Test
    fun `un libro della forma giusta non ha rilievi`() {
        val rilievi = FormaDelGrafo.rilievi(FormaDelGrafo.misura(libroDiDiamanti(7)))
        assertTrue(rilievi.isEmpty(), "atteso nessun rilievo, ottenuti: ${rilievi.map { it.messaggio }}")
    }

    @Test
    fun `le uscite per scena escludono i finali dal denominatore`() {
        val m = FormaDelGrafo.misura(libroCorto())
        assertEquals(6, m.scene)
        assertEquals(6, m.collegamenti) // 2 + 1 + 1 + 2
        assertEquals(2, m.finali)
        // 6 collegamenti su 4 scene che POSSONO averne. Contando anche i
        // due finali verrebbe 1,0 e un libro sano sembrerebbe lineare.
        assertEquals(1.5, m.grado)
    }

    @Test
    fun `un libro sotto le venti scene si misura ma non si giudica`() {
        val m = FormaDelGrafo.misura(libroCorto())

        assertEquals(1.5, m.grado)
        assertEquals(100.0, m.quotaObbligata)
        // Su sei scene "il 17% riconverge" vuol dire "una scena":
        // giudicarlo sarebbe rumore garantito sui libri di prova.
        assertTrue(FormaDelGrafo.rilievi(m).isEmpty())
    }

    @Test
    fun `i controlli strutturali valgono anche sui libri piccoli`() {
        val m = FormaDelGrafo.misura(
            libro(
                scena("1", listOf("2"), SceneType.START),
                finale("2", EndingOutcome.VICTORY),
                finale("99", EndingOutcome.DEFEAT), // orfana
            ),
        )

        assertEquals(listOf("99"), m.irraggiungibili)
        val rilievo = FormaDelGrafo.rilievi(m).first()
        assertEquals(FormaDelGrafo.Gravita.ERRORE, rilievo.gravita)
        assertTrue(rilievo.messaggio.contains("99"))
    }

    @Test
    fun `una catena senza bivi viene segnalata come racconto lineare`() {
        val catena = (1..24).map { n ->
            val tipo = if (n == 1) SceneType.START else SceneType.TRANSITION
            scena("$n", listOf(if (n == 24) "fine" else "${n + 1}"), tipo)
        }
        val m = FormaDelGrafo.misura(
            libro(*catena.toTypedArray(), finale("fine", EndingOutcome.VICTORY)),
        )

        val messaggi = FormaDelGrafo.rilievi(m).map { it.messaggio }
        assertTrue(messaggi.any { it.contains("racconto lineare") }, "ottenuti: $messaggi")
    }

    @Test
    fun `i rami che non rientrano mai abbassano la riconvergenza`() {
        // Un albero che si apre e non si richiude piu': ogni scena ha un
        // solo ingresso, la riconvergenza e' zero.
        val scene = mutableListOf(scena("1", listOf("2", "3"), SceneType.START))
        var prossimo = 4
        (2..11).forEach { n ->
            scene += scena("$n", listOf("$prossimo", "${prossimo + 1}"))
            prossimo += 2
        }
        (12..23).forEach { scene += finale("$it", EndingOutcome.DEFEAT) }
        val m = FormaDelGrafo.misura(libro(*scene.toTypedArray(), finale("fine", EndingOutcome.VICTORY)))

        assertEquals(0.0, m.riconvergenza)
        assertTrue(FormaDelGrafo.rilievi(m).any { it.messaggio.contains("non rientrano") })
    }

    @Test
    fun `una scena non finale senza uscite e un errore`() {
        val m = FormaDelGrafo.misura(
            libro(
                scena("1", listOf("2", "3"), SceneType.START),
                scena("2", emptyList()), // TRANSITION senza sbocchi
                finale("3", EndingOutcome.VICTORY),
            ),
        )

        assertEquals(listOf("2"), m.vicoliCiechi)
        assertTrue(
            FormaDelGrafo.rilievi(m).any {
                it.gravita == FormaDelGrafo.Gravita.ERRORE && it.messaggio.contains("senza uscita")
            },
        )
    }

    @Test
    fun `un libro senza vittoria e un errore e senza sconfitta un avviso`() {
        val m = FormaDelGrafo.misura(
            libro(
                scena("1", listOf("2", "3"), SceneType.START),
                finale("2", EndingOutcome.NEUTRAL),
                finale("3", EndingOutcome.NEUTRAL),
            ),
        )

        val rilievi = FormaDelGrafo.rilievi(m)
        assertTrue(
            rilievi.any { it.gravita == FormaDelGrafo.Gravita.ERRORE && it.messaggio.contains("vittoria") },
        )
        assertTrue(
            rilievi.any { it.gravita == FormaDelGrafo.Gravita.AVVISO && it.messaggio.contains("sconfitta") },
        )
    }

    @Test
    fun `il rientro si misura dal bivio al primo punto in comune`() {
        // 1 -> {2,3}; 2 -> 4; 3 -> 4: i rami si ritrovano in 4, che dista
        // 1 da ognuno dei due. Rientro 2.
        assertEquals(2, FormaDelGrafo.misura(libroCorto()).rientroMediano)
    }

    // Aggiunge alla catena di diamanti dei rami lunghi: partono da un
    // bivio e rientrano solo sette tappe dopo.
    private fun conRamiLunghi(daBivi: List<String>): Manifest {
        val base = libroDiDiamanti(7)
        val lunghi = daBivi.flatMap { bivio ->
            (1..6).map { i ->
                scena("l${bivio}_$i", listOf(if (i == 6) "10" else "l${bivio}_${i + 1}"))
            }
        }
        return libro(
            *base.scenes.map { s ->
                if (s.id in daBivi) {
                    scena(s.id, s.choices.map { it.nextSceneId } + "l${s.id}_1", s.sceneType)
                } else {
                    s
                }
            }.toTypedArray(),
            *lunghi.toTypedArray(),
        )
    }

    @Test
    fun `un bivio a rientro lungo viene identificato col numero della scena`() {
        val m = FormaDelGrafo.misura(conRamiLunghi(listOf("1")))

        assertEquals(listOf("1"), m.rientriTardivi)
    }

    @Test
    fun `i rientri lunghi si segnalano in proporzione, non a uno a uno`() {
        // Uno solo su otto bivi (12%) sta sotto soglia: anche i libri
        // veri ne hanno fra il 5% e il 12%, segnalarlo sarebbe rumore.
        val pochi = FormaDelGrafo.misura(conRamiLunghi(listOf("1")))
        assertTrue(FormaDelGrafo.rilievi(pochi).none { it.messaggio.contains("rientra dopo piu'") })

        // Tre su otto (37%) e' un'altra cosa.
        val molti = FormaDelGrafo.misura(conRamiLunghi(listOf("1", "4", "7")))
        assertEquals(3, molti.rientriTardivi.size)
        assertTrue(FormaDelGrafo.rilievi(molti).any { it.messaggio.contains("rientra dopo piu'") })
    }

    @Test
    fun `la quota obbligata conta le scene che ogni percorso attraversa`() {
        // Verso la vittoria "5" ogni strada passa da 1, 4 e 5: tre scene
        // obbligate su una profondita' di 3.
        assertEquals(100.0, FormaDelGrafo.misura(libroCorto()).quotaObbligata)
    }

    @Test
    fun `senza una vittoria dichiarata la quota obbligata non si misura`() {
        val m = FormaDelGrafo.misura(
            libro(
                scena("1", listOf("2"), SceneType.START),
                finale("2", EndingOutcome.DEFEAT),
            ),
        )

        assertNull(m.quotaObbligata)
    }
}
