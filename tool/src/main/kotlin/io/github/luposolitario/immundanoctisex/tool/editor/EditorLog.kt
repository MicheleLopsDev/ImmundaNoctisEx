package io.github.luposolitario.immundanoctisex.tool.editor

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Il logcat che l'editor non ha (02/08/2026, Michele: "come si vede il
// log su windows?"). Il client scrive in logcat e si legge con `adb`;
// qui `println` finisce sulla console di `:tool:run` — che però non
// esiste quando l'editor parte dall'`.exe` col doppio click, cioè
// proprio quando serve capire perché qualcosa si è piantato.
//
// Quindi: sempre su console E sempre su file, in un percorso fisso e
// dicibile a voce.
object EditorLog {

    // %LOCALAPPDATA%\ImmundaNoctisEx\editor.log su Windows; sotto la
    // home altrove (e se la variabile manca).
    val file: File by lazy {
        val base = System.getenv("LOCALAPPDATA")?.let(::File)
            ?: File(System.getProperty("user.home"))
        File(base, "ImmundaNoctisEx").apply { mkdirs() }.resolve("editor.log")
    }

    private val orario = DateTimeFormatter.ofPattern("HH:mm:ss")
    private const val DIMENSIONE_MASSIMA = 5L * 1024 * 1024

    fun i(tag: String, messaggio: String) = scrivi("INFO", tag, messaggio)

    fun e(tag: String, messaggio: String, errore: Throwable? = null) {
        scrivi("ERRORE", tag, messaggio)
        // Lo stacktrace solo su file: in console sarebbe rumore, sul
        // file è la sola cosa che permette di capire un guasto a
        // posteriori, quando la finestra è già stata chiusa.
        errore?.let { runCatching { file.appendText(it.stackTraceToString()) } }
    }

    private fun scrivi(livello: String, tag: String, messaggio: String) {
        val riga = "${LocalDateTime.now().format(orario)} $livello $tag: $messaggio"
        println(riga)
        runCatching {
            // Rotazione minima: un log che cresce senza fine su un
            // programma che scarica file da gigabyte diventerebbe lui
            // stesso un problema.
            if (file.exists() && file.length() > DIMENSIONE_MASSIMA) {
                file.copyTo(File(file.parentFile, "editor.log.1"), overwrite = true)
                file.writeText("")
            }
            file.appendText(riga + System.lineSeparator())
        }
    }
}
