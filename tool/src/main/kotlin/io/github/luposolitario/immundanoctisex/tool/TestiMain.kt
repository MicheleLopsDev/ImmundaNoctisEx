package io.github.luposolitario.immundanoctisex.tool

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.tool.etl.ProjectAonDownloader
import io.github.luposolitario.immundanoctisex.tool.etl.ProjectAonHtmlParser
import io.github.luposolitario.immundanoctisex.tool.etl.TestiEsterni
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.exitProcess

private val jsonLibro = Json { prettyPrint = true; ignoreUnknownKeys = true }

private fun leggiLibro(percorso: String): Manifest {
    val file = File(percorso)
    if (!file.exists()) {
        println("File non trovato: $percorso")
        exitProcess(2)
    }
    return jsonLibro.decodeFromString(Manifest.serializer(), file.readText())
}

// Toglie la prosa da un libro convertito, lasciando le meccaniche
// (03/08/2026). Serve UNA VOLTA per libro, per preparare il file da
// versionare; da lì in poi si lavora sempre sullo scheletro.
//
// Uso: ./gradlew :tool:cli --args="svuotaTesti doc/LIBRI/01fftd.json 01fftd"
fun runSvuotaTesti(args: Array<String>) {
    if (args.size < 2) {
        println("Uso: svuotaTesti <libro.json> <idProjectAon> [output.json]")
        println("  es. svuotaTesti doc/LIBRI/01fftd.json 01fftd")
        exitProcess(2)
    }
    val manifest = leggiLibro(args[0])
    val scheletro = TestiEsterni.scheletro(manifest, idProjectAon = args[1])
    val uscita = File(args.getOrElse(2) { args[0] })
    uscita.writeText(jsonLibro.encodeToString(Manifest.serializer(), scheletro))

    val caratteriPrima = manifest.scenes.sumOf { it.narrativeText.length }
    println("Scritto ${uscita.path}")
    println("  scene: ${scheletro.scenes.size}, testi rimossi: ~$caratteriPrima caratteri di prosa")
    println("  textsFrom: ${scheletro.textsFrom}")
    println("  restano meccaniche, salti, tiri, combattimenti e modificatori.")
}

// Scarica il libro da Project Aon e riempie lo scheletro (03/08/2026).
// Il download avviene sulla macchina di chi esegue il comando e i file
// restano lì: licenza Project Aon, uso personale sì, redistribuzione no.
//
// Uso: ./gradlew :tool:cli --args="riempiTesti doc/LIBRI/01fftd.json build/libri/01fftd.json"
fun runRiempiTesti(args: Array<String>) {
    if (args.isEmpty()) {
        println("Uso: riempiTesti <scheletro.json> [output.json]")
        println("  L'output NON va versionato: contiene i testi di Project Aon.")
        exitProcess(2)
    }
    val scheletro = leggiLibro(args[0])
    val idLibro = TestiEsterni.idProjectAonDi(scheletro)
    if (idLibro == null) {
        println("${args[0]} non è uno scheletro: manca 'textsFrom' (atteso \"projectaon:<id>\").")
        println("Se è un libro tuo o di prova, i testi ce li ha già e non c'è niente da riempire.")
        exitProcess(2)
    }

    println("Libro: ${scheletro.title} (Project Aon: $idLibro)")
    println("I testi restano su questa macchina e non vanno redistribuiti (licenza Project Aon).")

    val cartella = File("build/projectaon")
    val xhtml = runCatching {
        ProjectAonDownloader.scarica(idLibro, cartella) { println("  $it") }
    }.getOrElse { errore ->
        println("Download fallito: ${errore.message}")
        exitProcess(1)
    }

    println("  leggo ${xhtml.name}")
    val conTesti = ProjectAonHtmlParser.parse(
        file = xhtml,
        id = scheletro.id,
        title = scheletro.title,
        description = scheletro.description,
        genre = scheletro.genre,
        disciplineDescriptions = scheletro.disciplineChoices,
    )

    val esito = TestiEsterni.riempi(scheletro, conTesti.manifest)
    val uscita = File(args.getOrElse(1) { "build/libri/${scheletro.id}.json" })
    uscita.parentFile?.mkdirs()
    uscita.writeText(jsonLibro.encodeToString(Manifest.serializer(), esito.manifest))

    val piene = esito.manifest.scenes.count { it.narrativeText.isNotBlank() }
    println("Scritto ${uscita.path}")
    println("  scene con testo: $piene su ${esito.manifest.scenes.size}")
    if (esito.note.isEmpty()) {
        println("  nessuna discrepanza.")
    } else {
        println("  ${esito.note.size} cose da rivedere a mano:")
        esito.note.take(20).forEach { println("    - $it") }
        if (esito.note.size > 20) println("    ... e altre ${esito.note.size - 20}")
    }
}
