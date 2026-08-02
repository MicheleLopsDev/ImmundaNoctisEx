package io.github.luposolitario.immundanoctisex.tool.inference

import io.github.luposolitario.immundanoctisex.core.engine.inference.ModelCatalog
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Nessun test scarica davvero 3,7 GB: si verifica che i fallimenti
// siano puliti e che il catalogo condiviso col client sia coerente.
// I link veri sono stati provati a mano con richieste HEAD il
// 02/08/2026 — entrambi 200, `content-length` identico ai `sizeBytes`
// dichiarati, nessun token richiesto.
class ModelDownloaderTest {

    // La porta 1 non ascolta mai: il fallimento arriva subito, senza
    // dipendere dalla rete e senza attese.
    private val urlIrraggiungibile = "http://127.0.0.1:1/modello.litertlm"

    @Test
    fun unUrlIrraggiungibileFallisceSenzaLasciareFileParziali() = runBlocking<Unit> {
        val cartella = File(System.getProperty("java.io.tmpdir"), "prova-download-${System.nanoTime()}")
        val destinazione = File(cartella, "modello.litertlm")

        val esito = ModelDownloader().scarica(urlIrraggiungibile, destinazione) { _, _ -> }

        assertTrue(esito.isFailure)
        assertFalse(destinazione.exists(), "Non deve restare un modello monco")
        assertFalse(
            File(cartella, "modello.litertlm.parziale").exists(),
            "Il file parziale va rimosso: un residuo confonderebbe il tentativo successivo",
        )
        cartella.deleteRecursively()
    }

    @Test
    fun laDimensioneRemotaDiUnUrlIrraggiungibileENulla() = runBlocking {
        assertNull(ModelDownloader().dimensioneRemota(urlIrraggiungibile))
    }

    // Il catalogo NON è una lista parallela dell'editor: viene da
    // :core:engine, la stessa che usa il client. Se qualcuno lo cambiasse
    // lasciando un link vuoto o un modello che richiede un token, la
    // schermata offrirebbe un download destinato a fallire.
    @Test
    fun iModelliOffertiDallEditorSonoQuelliDiBaseDelClient() {
        val offerti = ModelCatalog.protected

        assertEquals(2, offerti.size, "Sono i due LiteRT-LM di base: 4B e 2B")
        offerti.forEach { modello ->
            assertTrue(modello.url.startsWith("https://"), "${modello.id}: link non valido")
            assertTrue(modello.fileName.endsWith(".litertlm"), "${modello.id}: formato inatteso")
            assertFalse(modello.requiresToken, "${modello.id}: l'editor non sa gestire un token")
            assertTrue(modello.sizeBytes > 1_000_000_000, "${modello.id}: dimensione non plausibile")
        }
    }
}
