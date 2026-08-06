package io.github.luposolitario.immundanoctisex.core.engine.choice

import io.github.luposolitario.immundanoctisex.core.data.model.GameMechanic
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// L'enigma numerico: il libro chiede un numero che il giocatore deve
// DEDURRE e lo manda al paragrafo che quel numero indica. Due scene su
// 2984 nei libri convertiti (05sots 58 e 331), ed erano gli ultimi due
// archi che mancavano al 100% sul grafo ufficiale.
class NumberPuzzleTest {

    private fun scena(vararg parametri: Pair<String, String>) = Scene(
        id = "58",
        sceneType = SceneType.TRANSITION,
        genre = "FANTASY",
        narrativeText = "Una porta di bronzo con una serratura a combinazione.",
        gameMechanics = listOf(
            GameMechanic(
                command = NumberPuzzle.COMMAND,
                params = buildJsonObject { parametri.forEach { (k, v) -> put(k, v) } },
            ),
        ),
    )

    // 05sots 58: risposta 67, chi sbaglia va a 98, chi rinuncia a 156.
    @Test
    fun ilNumeroGiustoPortaAllaScenaDellaRisposta() {
        val puzzle = NumberPuzzle.di(
            scena("answerSceneId" to "67", "wrongSceneId" to "98", "giveUpSceneId" to "156"),
        )!!
        assertTrue(puzzle.giocabile)
        assertEquals("67", puzzle.destinazionePer("67"))
        assertEquals("98", puzzle.destinazionePer("12"))
        assertEquals("156", puzzle.sceneRinuncia)
    }

    // Il giocatore scrive con le dita su un telefono: uno spazio di
    // troppo non è una risposta sbagliata.
    @Test
    fun gliSpaziAttornoAlNumeroNonContano() {
        val puzzle = NumberPuzzle.di(scena("answerSceneId" to "67", "wrongSceneId" to "98"))!!
        assertEquals("67", puzzle.destinazionePer("  67 "))
    }

    // 05sots 331: non c'è dove mandare chi sbaglia — o risolvi, o resti
    // lì a rileggere, come col libro di carta in mano.
    @Test
    fun senzaSceneSbagliataChiSbagliaNonSiMuove() {
        val puzzle = NumberPuzzle.di(scena("answerSceneId" to "373"))!!
        assertEquals("373", puzzle.destinazionePer("373"))
        assertNull(puzzle.destinazionePer("100"))
        assertNull(puzzle.sceneRinuncia)
    }

    // La risposta non è scritta nel libro: il convertitore la lascia
    // vuota invece di indovinarla. Senza, l'enigma non è giocabile e la
    // scena deve degradare sulle scelte normali — il gioco non si
    // blocca mai.
    @Test
    fun senzaRispostaLEnigmaNonEGiocabile() {
        val puzzle = NumberPuzzle.di(scena("wrongSceneId" to "98", "giveUpSceneId" to "156"))!!
        assertFalse(puzzle.giocabile)
    }

    @Test
    fun unaScenaSenzaEnigmaNonNeHaUno() {
        val semplice = Scene(
            id = "1",
            sceneType = SceneType.TRANSITION,
            genre = "FANTASY",
            narrativeText = "Nessun enigma qui.",
        )
        assertNull(NumberPuzzle.di(semplice))
    }
}
