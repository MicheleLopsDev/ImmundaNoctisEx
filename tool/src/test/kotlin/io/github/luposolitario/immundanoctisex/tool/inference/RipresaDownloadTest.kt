package io.github.luposolitario.immundanoctisex.tool.inference

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// La ripresa dei download (02/08/2026, Michele: "si blocca al 19%") si
// verifica per davvero, non a parole: un server HTTP in-process
// (`com.sun.net.httpserver`, nella JDK — nessuna dipendenza nuova) che
// prima tronca la risposta e poi onora l'header Range.
//
// Il file di prova è di 1,5 MB: sopra il minimo che il downloader
// considera plausibile, abbastanza piccolo da non pesare sui test.
class RipresaDownloadTest {

    private lateinit var server: HttpServer
    private lateinit var cartella: File
    private val contenuto = ByteArray(1_500_000) { (it % 251).toByte() }

    // Quando > 0, la prima risposta si interrompe dopo questi byte: è la
    // connessione che cade a metà.
    private var troncaDopo = 0

    @BeforeTest
    fun avviaServer() {
        cartella = File(System.getProperty("java.io.tmpdir"), "ripresa-${System.nanoTime()}")
        cartella.mkdirs()
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/modello.litertlm") { scambio -> rispondi(scambio) }
        server.start()
    }

    @AfterTest
    fun fermaServer() {
        server.stop(0)
        cartella.deleteRecursively()
    }

    private fun rispondi(scambio: HttpExchange) {
        val range = scambio.requestHeaders.getFirst("Range")
        val da = range?.removePrefix("bytes=")?.substringBefore('-')?.toIntOrNull() ?: 0
        val corpo = contenuto.copyOfRange(da, contenuto.size)
        val daInviare = if (troncaDopo > 0) minOf(troncaDopo, corpo.size) else corpo.size

        // 206 alla ripresa, 200 alla prima richiesta: le stesse risposte
        // che dà HuggingFace.
        scambio.sendResponseHeaders(if (da > 0) 206 else 200, corpo.size.toLong())
        scambio.responseBody.use { it.write(corpo, 0, daInviare) }
    }

    private fun url() = "http://127.0.0.1:${server.address.port}/modello.litertlm"

    @Test
    fun unDownloadInterrottoConservaIlTronconeEPoiRiprendeDaLi() = runBlocking {
        val destinazione = File(cartella, "modello.litertlm")
        val parziale = File(cartella, "modello.litertlm.parziale")
        val scaricatore = ModelDownloader()

        // Primo tentativo: il server chiude dopo 1 MB dei 1,5 attesi.
        troncaDopo = 1_000_000
        val primo = scaricatore.scarica(url(), destinazione) { _, _ -> }

        assertTrue(primo.isFailure, "Un file troncato non deve passare per completo")
        assertTrue(!destinazione.exists(), "Il .litertlm non si crea da un download monco")
        assertTrue(parziale.exists(), "Il troncone va conservato: è il punto da cui ripartire")
        assertEquals(1_000_000L, parziale.length())

        // Secondo tentativo: il server risponde per intero e onora il Range.
        troncaDopo = 0
        val secondo = scaricatore.scarica(url(), destinazione) { _, _ -> }

        assertTrue(secondo.isSuccess, "La ripresa deve completare: ${secondo.exceptionOrNull()?.message}")
        assertTrue(!parziale.exists(), "A lavoro finito il parziale è stato rinominato")
        // La prova vera: i byte ricuciti da due connessioni diverse
        // devono essere identici all'originale, senza buchi né doppioni.
        assertContentEquals(contenuto, destinazione.readBytes())
    }

    @Test
    fun unDownloadPulitoArrivaInFondoAlPrimoColpo() = runBlocking {
        val destinazione = File(cartella, "intero.litertlm")

        val esito = ModelDownloader().scarica(url(), destinazione) { _, _ -> }

        assertTrue(esito.isSuccess, esito.exceptionOrNull()?.message.orEmpty())
        assertContentEquals(contenuto, destinazione.readBytes())
    }

    @Test
    fun ilProgressoArrivaFinoAlTotaleDichiarato() = runBlocking {
        val destinazione = File(cartella, "progresso.litertlm")
        var ultimiScaricati = 0L
        var ultimoTotale = 0L

        ModelDownloader().scarica(url(), destinazione) { fatti, quanti ->
            ultimiScaricati = fatti
            ultimoTotale = quanti
        }

        assertEquals(contenuto.size.toLong(), ultimiScaricati)
        assertEquals(contenuto.size.toLong(), ultimoTotale, "Il totale va dichiarato, altrimenti la barra mente")
    }
}
