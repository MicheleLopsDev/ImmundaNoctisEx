package io.github.luposolitario.immundanoctisex.tool.etl

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.EndingOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Le sonde semaforiche della barra in cima all'editor. Si testa il
// CALCOLO della salute (0 = fermo alla partenza, 1 = sul bersaglio),
// non il colore: quello e' una funzione della salute, e sta nel
// composable.
class SondeDiFormaTest {

    private fun scena(id: String, verso: List<String>, tipo: SceneType = SceneType.TRANSITION) = Scene(
        id = id, sceneType = tipo, genre = "fantasy", narrativeText = "testo",
        choices = verso.mapIndexed { i, a -> Choice(id = "${id}_c$i", choiceText = "vai", nextSceneId = a) },
    )

    private fun finale(id: String, esito: EndingOutcome) = Scene(
        id = id, sceneType = SceneType.ENDING, genre = "fantasy", narrativeText = "t", outcome = esito,
    )

    private fun libro(vararg scene: Scene) = Manifest(
        id = "p", version = "1.0.0", title = "P", description = "", language = "it",
        genre = "fantasy", scenes = scene.toList(),
    )

    private fun sonda(m: Manifest, etichetta: String) =
        SondeDiForma.di(FormaDelGrafo.misura(m)).first { it.etichetta == etichetta }

    // Una catena di 30 scene con un'unica uscita ciascuna: il caso
    // peggiore possibile per la ramificazione.
    private fun catenaLineare(): Manifest {
        val scene = (1..30).map { n ->
            scena("$n", listOf(if (n == 30) "fine" else "${n + 1}"), if (n == 1) SceneType.START else SceneType.TRANSITION)
        }
        return libro(*scene.toTypedArray(), finale("fine", EndingOutcome.VICTORY))
    }

    @Test
    fun `una catena lineare accende di rosso le uscite per scena`() {
        val s = sonda(catenaLineare(), "uscite per scena")

        // 30 collegamenti su 30 scene che possono averne.
        assertTrue(s.valore.startsWith("1,0") || s.valore.startsWith("1.0"), "valore inatteso: ${s.valore}")
        // Partenza 1,0 e bersaglio 1,65: una catena e' esattamente a zero.
        assertEquals(0f, s.salute)
    }

    @Test
    fun `una catena lineare accende di rosso anche la riconvergenza`() {
        val s = sonda(catenaLineare(), "scene con piu' vie")

        assertEquals("0%", s.valore)
        assertEquals(0f, s.salute)
    }

    @Test
    fun `senza sconfitte la sonda dei finali e a zero`() {
        val s = sonda(catenaLineare(), "finali di sconfitta")

        assertEquals("0", s.valore)
        assertEquals(0f, s.salute)
    }

    @Test
    fun `un valore sopra il bersaglio resta verde, non torna indietro`() {
        // Un libro molto ramificato: il grado supera 1,65. La salute si
        // ferma a 1, non ridiscende — sforare in su non e' un difetto
        // che questa sonda debba segnalare (ci pensa il rilievo).
        val scene = (1..24).map { n ->
            val dove = if (n >= 22) listOf("fine", "morte") else listOf("${n + 1}", "${n + 2}", "fine")
            scena("$n", dove, if (n == 1) SceneType.START else SceneType.TRANSITION)
        }
        val m = libro(*scene.toTypedArray(), finale("fine", EndingOutcome.VICTORY), finale("morte", EndingOutcome.DEFEAT))

        assertEquals(1f, sonda(m, "uscite per scena").salute)
    }

    @Test
    fun `il rientro dei rami peggiora allontanandosi dal bersaglio da entrambi i lati`() {
        // Bersaglio 3 tappe, scarto massimo 3: a 3 tappe la salute e' 1.
        val diamanti = mutableListOf<Scene>()
        repeat(7) { i ->
            val b = 1 + i * 3
            diamanti += scena("$b", listOf("${b + 1}", "${b + 2}"), if (i == 0) SceneType.START else SceneType.TRANSITION)
            diamanti += scena("${b + 1}", listOf("${b + 3}"))
            diamanti += scena("${b + 2}", listOf("${b + 3}"))
        }
        val m = libro(
            *diamanti.toTypedArray(),
            scena("22", listOf("fine", "morte")),
            finale("fine", EndingOutcome.VICTORY),
            finale("morte", EndingOutcome.DEFEAT),
        )

        val s = sonda(m, "rientro dei rami")
        assertEquals("2 tappe", s.valore)
        // Una tappa di scarto su tre: due terzi di salute.
        assertTrue(s.salute in 0.6f..0.7f, "salute attesa ~0,67, era ${s.salute}")
    }

    @Test
    fun `sotto le venti scene le sonde non mentono, semplicemente non ci sono`() {
        // La barra non le disegna affatto: qui si verifica che la misura
        // resti calcolabile, cosi' il composable puo' decidere da solo.
        val m = libro(
            scena("1", listOf("2", "3"), SceneType.START),
            scena("2", listOf("4")),
            scena("3", listOf("4")),
            scena("4", listOf("5")),
            finale("5", EndingOutcome.VICTORY),
        )

        assertTrue(FormaDelGrafo.misura(m).scene < FormaDelGrafo.SCENE_MINIME)
        assertTrue(SondeDiForma.di(FormaDelGrafo.misura(m)).isNotEmpty())
    }
}
