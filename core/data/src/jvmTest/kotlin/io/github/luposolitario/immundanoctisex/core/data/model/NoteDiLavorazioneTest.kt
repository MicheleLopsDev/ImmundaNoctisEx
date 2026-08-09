package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Le note di lavorazione (07/08/2026): quello che il modello deve dire a
// chi rivede il libro, e che il gioco non legge mai. Vedi
// NotaDiLavorazione.kt e doc/DA-ROMANZO-A-LIBROGAME.md.
class NoteDiLavorazioneTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun scena(note: List<NotaDiLavorazione> = emptyList()) = Scene(
        id = "1",
        sceneType = SceneType.START,
        genre = "FANTASY",
        narrativeText = "testo",
        noteDiLavorazione = note,
    )

    @Test
    fun `una scena senza note non porta il campo nel JSON`() {
        // Il caso normale e' non averne: un libro scritto direttamente per
        // il gioco non ha niente da dichiarare, e il suo JSON non deve
        // ingrassare di campi vuoti.
        val testo = json.encodeToString(Scene.serializer(), scena())

        assertTrue("noteDiLavorazione" !in testo, "campo vuoto serializzato: $testo")
    }

    @Test
    fun `le note sopravvivono al giro JSON`() {
        val originale = scena(
            listOf(
                NotaDiLavorazione(TipoNota.TESTO_RITOCCATO, "aggiunta in coda «Ti volti verso la porta.»"),
                NotaDiLavorazione(TipoNota.FORMA, "due paragrafi descrittivi in terza persona"),
            ),
        )

        val riletta = json.decodeFromString(
            Scene.serializer(),
            json.encodeToString(Scene.serializer(), originale),
        )

        assertEquals(originale.noteDiLavorazione, riletta.noteDiLavorazione)
        assertEquals("aggiunta in coda «Ti volti verso la porta.»", riletta.noteDiLavorazione[0].testo)
    }

    @Test
    fun `un libro scritto prima di questo campo si carica lo stesso`() {
        // Retrocompatibilita': i cinque libri Project Aon e i libri di
        // prova sono stati scritti quando il campo non esisteva.
        val vecchio = """
            {"id":"1","sceneType":"START","genre":"FANTASY","narrativeText":"testo"}
        """.trimIndent()

        val scena = json.decodeFromString(Scene.serializer(), vecchio)

        assertTrue(scena.noteDiLavorazione.isEmpty())
    }

    @Test
    fun `modificare una scena nell'editor non cancella le sue note`() {
        // L'editor ricostruisce la scena con `copy()`: le note non sono
        // fra i campi della maschera, quindi devono sopravvivere da sole.
        // Se un domani qualcuno costruisse una Scene da zero al
        // salvataggio, questo test se ne accorgerebbe.
        val originale = scena(listOf(NotaDiLavorazione(TipoNota.TESTO_RITOCCATO, "aggiunta una frase")))

        val modificata = originale.copy(narrativeText = "testo riscritto dall'autore")

        assertEquals(1, modificata.noteDiLavorazione.size)
        assertEquals(TipoNota.TESTO_RITOCCATO, modificata.noteDiLavorazione.first().tipo)
    }
}
