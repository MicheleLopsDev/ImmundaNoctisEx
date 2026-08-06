package io.github.luposolitario.immundanoctisex.tool

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.tool.etl.FormaDelGrafo
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.exitProcess

// Misura la forma di un libro e la confronta con quella dei librogame
// pubblicati (doc/FORMA-DEI-GRAFI.md).
//
// Nasce per la seconda passata sui libri generati da un'IA remota
// (06/08/2026): `validate` dice se il grafo REGGE, questo dice se ha la
// forma di un librogame. Sono domande diverse — un racconto lineare con
// un bivio ogni tanto passa il validatore benissimo.
//
// Uso: ./gradlew :tool:cli --args="forma ../content/test-books/scenes.sample.json"
fun runForma(args: Array<String>) {
    if (args.isEmpty()) {
        println("Uso: forma <libro.json>")
        exitProcess(2)
    }
    val file = File(args[0])
    if (!file.exists()) {
        println("File non trovato: ${args[0]}")
        exitProcess(2)
    }
    val json = Json { ignoreUnknownKeys = true }
    val manifest = runCatching { json.decodeFromString(Manifest.serializer(), file.readText()) }
        .getOrElse {
            println("JSON non leggibile: ${it.message}")
            exitProcess(2)
        }

    val m = FormaDelGrafo.misura(manifest)
    // Trattini semplici e niente accenti nei simboli: la console di
    // Windows non e' UTF-8 (stesso motivo di VerificaGrafoMain).
    println("${file.name}: ${m.scene} scene, ${m.collegamenti} collegamenti, ${m.finali} finali")
    println("  uscite per scena     %.2f      (libri veri 1,45-2,18)".format(m.grado))
    println("  scene con piu' vie   %.0f%%       (libri veri 23-44%%)".format(m.riconvergenza))
    m.quotaViva?.let { println("  puo' ancora vincere  %.0f%%       (libri veri 86-100%%)".format(it)) }
    m.quotaObbligata?.let { println("  cammino obbligato    %.0f%%       (libri veri 42-50%%)".format(it)) }
    m.rientroMediano?.let { println("  rientro dei rami     $it tappe   (libri veri: mediana 3)") }
    println("  bivi                 ${m.bivi}, di cui ${m.rientriTardivi.size} a rientro lungo")
    println("  profondita'          ${m.profondita} tappe")
    val esiti = m.finaliPerEsito.entries.joinToString(", ") { "${it.value} ${it.key}" }
    println("  finali               ${esiti.ifBlank { "nessun esito dichiarato" }}")

    val rilievi = FormaDelGrafo.rilievi(m)
    if (rilievi.isEmpty()) {
        println("\nLa forma regge: niente da segnalare.")
        return
    }
    println()
    rilievi.forEach { println("  [${it.gravita}] ${it.messaggio}") }

    // Esce 1 solo sugli ERRORI: gli avvisi descrivono un libro che si
    // gioca comunque, e bloccare su quelli renderebbe il comando
    // inutilizzabile sui libri di prova a poche scene.
    if (rilievi.any { it.gravita == FormaDelGrafo.Gravita.ERRORE }) exitProcess(1)
}
