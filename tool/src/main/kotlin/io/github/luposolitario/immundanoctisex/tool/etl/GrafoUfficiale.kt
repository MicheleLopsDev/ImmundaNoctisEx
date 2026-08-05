package io.github.luposolitario.immundanoctisex.tool.etl

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.GZIPInputStream

// Il grafo dei percorsi che Project Aon pubblica per ogni libro:
// `/en/svg/lw/01fftd.svgz`, un SVG compresso generato con Graphviz che
// disegna tutti i collegamenti fra i paragrafi.
//
// Serve a verificare le nostre conversioni contro una fonte che non
// siamo noi (idea di Michele, 03/08/2026). Come per i testi, il file si
// scarica sulla macchina di chi esegue il comando e resta lì.
object GrafoUfficiale {

    private const val BASE = "https://www.projectaon.org/en/svg/lw"

    fun urlPerLibro(idLibro: String): String = "$BASE/$idLibro.svgz"

    class DownloadFallito(messaggio: String, causa: Throwable? = null) : Exception(messaggio, causa)

    // Graphviz scrive un arco come `<title>001&#45;&gt;141</title>`,
    // cioè "001->141" con i caratteri sfuggiti. Gli id hanno lo zero
    // davanti: "001" è il paragrafo 1.
    private val arcoRegex = Regex("""<title>(\d+)&#45;&gt;(\d+)</title>""")

    internal fun archiDa(svg: String): Set<Pair<String, String>> =
        arcoRegex.findAll(svg)
            .map { it.groupValues[1].trimStart('0').ifEmpty { "0" } to it.groupValues[2].trimStart('0').ifEmpty { "0" } }
            .toSet()

    // Scarica (o riusa, se già presente) il grafo e ne estrae gli archi.
    fun scaricaArchi(idLibro: String, cartella: File, log: (String) -> Unit = {}): Set<Pair<String, String>> {
        cartella.mkdirs()
        val locale = File(cartella, "$idLibro.svgz")
        if (!locale.exists()) {
            val url = urlPerLibro(idLibro)
            log("scarico $url")
            val connessione = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
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
                throw DownloadFallito("projectaon.org ha risposto $codice per $url")
            }
            connessione.inputStream.use { rete -> locale.outputStream().use { rete.copyTo(it) } }
        } else {
            log("uso la copia già scaricata (${locale.name})")
        }

        val svg = runCatching {
            GZIPInputStream(locale.inputStream()).bufferedReader().use { it.readText() }
        }.getOrElse { throw DownloadFallito("il grafo di $idLibro non è un file .svgz valido: ${it.message}", it) }

        val archi = archiDa(svg)
        if (archi.isEmpty()) throw DownloadFallito("nessun arco trovato nel grafo di $idLibro")
        return archi
    }
}
