package io.github.luposolitario.immundanoctisex.tool.etl

import java.io.File
import java.net.HttpURLConnection
import java.net.URI

// Scarica un libro dalle Internet Edition di Project Aon, nel formato
// che `ProjectAonHtmlParser` sa leggere.
//
// Gira sulla macchina di CHI LO USA, su sua richiesta esplicita, e il
// file resta lì: la licenza Project Aon consente il download per uso
// personale e vieta la redistribuzione. Niente di tutto questo entra nel
// repository o nell'APK (README §15).
object ProjectAonDownloader {

    // Project Aon pubblica i libri in due forme (indicazione di Michele,
    // 03/08/2026 — la prima strada tentata era lo zip di `xhtml/`, che
    // contiene invece UNA PAGINA PER SEZIONE, 472 file da ricomporre):
    //
    //  - `xhtml/lw/01fftd/` — una pagina per sezione (sect1.htm, ...)
    //  - `xhtml-simple/lw/01fftd.htm` — IL LIBRO INTERO in un file solo
    //
    // Si usa la seconda: un unico `div.numbered` con tutte le sezioni,
    // esattamente ciò che il parser si aspetta, e **una sola richiesta**
    // al loro server invece di centinaia.
    private const val BASE_LIBRO = "https://www.projectaon.org/en/xhtml-simple/lw"

    // Le pagine per singola sezione servono solo come riferimento umano
    // (il campo `Scene.source`), mai per il download.
    private const val BASE_SEZIONE = "https://www.projectaon.org/en/xhtml/lw"

    fun urlPerLibro(idLibro: String): String = "$BASE_LIBRO/$idLibro.htm"

    fun urlPerSezione(idLibro: String, idScena: String): String =
        "$BASE_SEZIONE/$idLibro/sect$idScena.htm"

    class DownloadFallito(messaggio: String, causa: Throwable? = null) : Exception(messaggio, causa)

    // Scarica il libro in `destinazione` e restituisce il file scaricato.
    fun scarica(idLibro: String, destinazione: File, log: (String) -> Unit = {}): File {
        val url = urlPerLibro(idLibro)
        log("scarico $url")
        destinazione.mkdirs()

        val connessione = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            // Un User-Agent che dice chi siamo: è cortesia verso un sito
            // che regala il proprio lavoro, non un requisito tecnico.
            setRequestProperty(
                "User-Agent",
                "ImmundaNoctisEx/1.0 (uso personale; +https://github.com/MicheleLopsDev/ImmundaNoctisEx)",
            )
            connectTimeout = 30_000
            readTimeout = 60_000
        }
        val codice = runCatching { connessione.responseCode }
            .getOrElse { throw DownloadFallito("impossibile raggiungere projectaon.org: ${it.message}", it) }
        if (codice != HttpURLConnection.HTTP_OK) {
            throw DownloadFallito("projectaon.org ha risposto $codice per $url (l'id del libro è giusto?)")
        }

        val file = File(destinazione, "$idLibro.htm")
        connessione.inputStream.use { rete -> file.outputStream().use { rete.copyTo(it) } }
        if (file.length() == 0L) throw DownloadFallito("il file scaricato per $idLibro è vuoto")
        log("scaricati ${file.length() / 1024} KB in ${file.name}")
        return file
    }
}
