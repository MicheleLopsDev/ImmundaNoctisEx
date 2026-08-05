package io.github.luposolitario.immundanoctisex.tool.etl

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Il confronto col grafo ufficiale dei percorsi che Project Aon
// pubblica per ogni libro (idea di Michele, 03/08/2026). È l'unico
// controllo che verifica la conversione contro una fonte ESTERNA.
class ConfrontoGrafoTest {

    private fun scena(
        id: String,
        verso: List<String> = emptyList(),
        combat: Combat? = null,
    ) = Scene(
        id = id,
        sceneType = SceneType.TRANSITION,
        genre = "fantasy",
        narrativeText = "testo",
        choices = verso.mapIndexed { i, a -> Choice(id = "c$i", choiceText = "vai", nextSceneId = a) },
        combat = combat,
    )

    private fun libro(vararg scene: Scene) = Manifest(
        id = "01fftd", version = "1.0.0", title = "Prova", description = "",
        language = "en", genre = "fantasy", scenes = scene.toList(),
    )

    @Test
    fun `un libro che combacia col grafo ha copertura piena`() {
        val esito = ConfrontoGrafo.confronta(
            libro(scena("1", listOf("2", "3")), scena("2", listOf("3")), scena("3")),
            setOf("1" to "2", "1" to "3", "2" to "3"),
        )
        assertEquals(3, esito.archiUfficiali)
        assertTrue(esito.mancanti.isEmpty())
        assertTrue(esito.inPiu.isEmpty())
        assertEquals(100.0, esito.copertura)
    }

    @Test
    fun `un collegamento che il libro ha e noi no risulta mancante`() {
        val esito = ConfrontoGrafo.confronta(
            libro(scena("1", listOf("2")), scena("2"), scena("3")),
            setOf("1" to "2", "1" to "3"),
        )
        assertEquals(listOf("1" to "3"), esito.mancanti)
        assertEquals(50.0, esito.copertura)
    }

    // Più grave del mancante: manda il giocatore dove il libro non lo
    // manda. Sui 5 libri veri non è mai successo.
    @Test
    fun `un collegamento inventato da noi risulta in piu`() {
        val esito = ConfrontoGrafo.confronta(
            libro(scena("1", listOf("2", "99")), scena("2"), scena("99")),
            setOf("1" to "2"),
        )
        assertEquals(listOf("1" to "99"), esito.inPiu)
        assertTrue(esito.mancanti.isEmpty())
    }

    // Il caso che rendeva il confronto inutile se fatto ingenuamente: un
    // combattimento con più nemici diventa una catena di scene
    // sintetiche ("112" -> "112-nemico2" -> "33"), e un finale
    // fabbricato prende un id come "17-vittoria". Il grafo ufficiale
    // conosce solo i paragrafi numerati: l'arco 112 -> 33 c'è, va solo
    // cercato saltando i nodi intermedi.
    @Test
    fun `una catena di scene sintetiche conta come collegamento diretto`() {
        val esito = ConfrontoGrafo.confronta(
            libro(
                scena("112", listOf("112-nemico2")),
                scena("112-nemico2", listOf("33")),
                scena("33"),
            ),
            setOf("112" to "33"),
        )
        assertTrue(esito.mancanti.isEmpty(), "atteso nessun mancante, trovato ${esito.mancanti}")
        assertTrue(esito.inPiu.isEmpty(), "i nodi sintetici non sono archi inventati: ${esito.inPiu}")
    }

    @Test
    fun `le uscite del combattimento contano come collegamenti`() {
        val esito = ConfrontoGrafo.confronta(
            libro(
                scena(
                    "5",
                    combat = Combat(
                        enemyName = "Giak", enemyCombatSkill = 10, enemyEndurance = 10,
                        winSceneId = "6", winSceneIdRapido = "7", winEntroRound = 3,
                        loseSceneId = "8", evadeSceneId = "9",
                    ),
                ),
                scena("6"), scena("7"), scena("8"), scena("9"),
            ),
            setOf("5" to "6", "5" to "7", "5" to "8", "5" to "9"),
        )
        assertTrue(esito.mancanti.isEmpty(), "mancanti: ${esito.mancanti}")
    }

    @Test
    fun `gli archi di Graphviz si leggono con lo zero davanti`() {
        val svg = """
            <title>001&#45;&gt;141</title>
            <title>085&#45;&gt;099</title>
            <title>12</title>
        """.trimIndent()
        assertEquals(setOf("1" to "141", "85" to "99"), GrafoUfficiale.archiDa(svg))
    }
}
