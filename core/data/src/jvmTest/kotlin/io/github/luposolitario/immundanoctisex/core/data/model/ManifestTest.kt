package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// §19.12 (EDITOR.md, Michele: "una mappa in testa con id e posizioni
// che viene saltata dal client"): scenePositions è per l'editor, non
// per il motore — qui si verifica solo che sia opzionale (un libro
// vecchio senza il campo resta valido) e che sopravviva a un giro di
// serializzazione/deserializzazione.
class ManifestTest {

    private fun manifestMinimo() = Manifest(
        id = "test", version = "1.0.0", title = "Test", description = "Test",
        language = "en", genre = "FANTASY",
    )

    @Test
    fun senzaPosizioniMappaIlDefaultEVuoto() {
        assertTrue(manifestMinimo().scenePositions.isEmpty())
    }

    @Test
    fun posizioniMappaSopravviveAUnGiroDiSerializzazione() {
        val originale = manifestMinimo().copy(
            scenePositions = mapOf("1" to ScenePosition(10f, 20f), "2" to ScenePosition(-5f, 300f)),
        )

        val json = Json.encodeToString(Manifest.serializer(), originale)
        val decodificato = Json.decodeFromString(Manifest.serializer(), json)

        assertEquals(originale.scenePositions, decodificato.scenePositions)
    }

    @Test
    fun unJsonSenzaIlCampoPosizioniMappaRestaValido() {
        // Simula un libro scritto PRIMA di §19.12: il campo manca del
        // tutto nel JSON, non solo vuoto.
        val json = """
            {"id":"test","version":"1.0.0","title":"Test","description":"Test","language":"en","genre":"FANTASY"}
        """.trimIndent()

        val manifest = Json.decodeFromString(Manifest.serializer(), json)

        assertTrue(manifest.scenePositions.isEmpty())
    }
}
