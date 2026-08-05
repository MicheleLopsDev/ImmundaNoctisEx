package io.github.luposolitario.immundanoctisex.tool

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.tool.etl.ConfrontoGrafo
import io.github.luposolitario.immundanoctisex.tool.etl.GrafoUfficiale
import io.github.luposolitario.immundanoctisex.tool.etl.TestiEsterni
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.exitProcess

// Confronta un libro convertito col grafo dei percorsi che Project Aon
// pubblica per quel volume (idea di Michele, 03/08/2026).
//
// È l'unico controllo che verifica la conversione contro una fonte
// ESTERNA: contare quante scelte sono state estratte non dice se sono
// quelle giuste, il grafo sì. Ha già trovato 28 archi persi che nessun
// altro controllo aveva visto.
//
// Uso: ./gradlew :tool:cli --args="verificaGrafo ../doc/LIBRI/01fftd.json"
//      ./gradlew :tool:verificaGrafo          (tutti i libri)
fun runVerificaGrafo(args: Array<String>) {
    if (args.isEmpty()) {
        println("Uso: verificaGrafo <libro.json> [idProjectAon]")
        println("  L'id si deduce da `textsFrom` se il libro ce l'ha.")
        exitProcess(2)
    }
    val file = File(args[0])
    if (!file.exists()) {
        println("File non trovato: ${args[0]}")
        exitProcess(2)
    }
    val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    val manifest = json.decodeFromString(Manifest.serializer(), file.readText())

    val idLibro = args.getOrNull(1)
        ?: TestiEsterni.idProjectAonDi(manifest)
        ?: file.nameWithoutExtension
    println("${file.name} -> grafo Project Aon di '$idLibro'")

    val archi = runCatching {
        GrafoUfficiale.scaricaArchi(idLibro, File("build/projectaon")) { println("  $it") }
    }.getOrElse {
        println("Impossibile leggere il grafo: ${it.message}")
        exitProcess(1)
    }

    val esito = ConfrontoGrafo.confronta(manifest, archi)
    // Trattino semplice e non em-dash: la console di Windows non è UTF-8
    // e i caratteri fuori ASCII escono a pezzi anche impostando
    // defaultCharacterEncoding sulla JVM figlia.
    println(
        "  archi ufficiali: ${esito.archiUfficiali} - nostri: ${esito.trovati} " +
            "(${"%.2f".format(esito.copertura)}%)",
    )

    // Un arco INVENTATO è più grave di uno mancante: manda il giocatore
    // dove il libro non lo manda. Si dice per primo.
    if (esito.inPiu.isNotEmpty()) {
        println("  ATTENZIONE, ${esito.inPiu.size} collegamenti che il libro NON ha:")
        esito.inPiu.take(20).forEach { (da, a) -> println("    $da -> $a") }
        if (esito.inPiu.size > 20) println("    ... e altri ${esito.inPiu.size - 20}")
    }
    if (esito.mancanti.isEmpty()) {
        println("  nessun collegamento mancante.")
    } else {
        println("  ${esito.mancanti.size} collegamenti mancanti:")
        esito.mancanti.take(20).forEach { (da, a) -> println("    $da -> $a") }
        if (esito.mancanti.size > 20) println("    ... e altri ${esito.mancanti.size - 20}")
    }

    // Esce 1 se manca qualcosa: così il task Gradle e un domani la CI
    // possono accorgersene da soli.
    if (esito.mancanti.isNotEmpty() || esito.inPiu.isNotEmpty()) exitProcess(1)
}
