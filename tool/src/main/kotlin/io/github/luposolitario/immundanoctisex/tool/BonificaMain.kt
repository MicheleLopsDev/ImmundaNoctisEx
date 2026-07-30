package io.github.luposolitario.immundanoctisex.tool

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.tool.etl.bonificaRisorseImmagine
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.exitProcess

// Comando CLI per bonificaRisorseImmagine (30/07/2026, Michele): legge
// un libro JSON già esistente e registra in customResources.images ogni
// link url: già in uso nelle scene — non regenera il libro dall'HTML
// sorgente (rischierebbe di perdere modifiche fatte a mano dopo la
// conversione), lavora solo sul JSON già scritto.
//
// Uso: ./gradlew :tool:cli --args="bonifica doc/LIBRI/01fftd.json doc/LIBRI/01fftd.json"
fun runBonifica(args: Array<String>) {
    if (args.size < 2) {
        println("Uso: bonifica <libro.json> <output.json>")
        exitProcess(2)
    }
    val inputFile = File(args[0])
    if (!inputFile.exists()) {
        println("File non trovato: ${args[0]}")
        exitProcess(2)
    }
    val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    val manifest = json.decodeFromString(Manifest.serializer(), inputFile.readText())
    val bonificato = bonificaRisorseImmagine(manifest)

    val vociNuove = bonificato.customResources.images.size - manifest.customResources.images.size
    val outputFile = File(args[1])
    outputFile.writeText(json.encodeToString(Manifest.serializer(), bonificato))
    println("Scritto ${args[1]}: $vociNuove nuove voci in customResources.images (totale ${bonificato.customResources.images.size})")
}
