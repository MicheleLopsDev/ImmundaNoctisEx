package io.github.luposolitario.immundanoctisex.tool

import io.github.luposolitario.immundanoctisex.tool.etl.ProjectAonHtmlParser
import java.io.File
import kotlin.system.exitProcess

// Report delle illustrazioni originali (29/07/2026, Michele: vuole un
// elenco — scena per scena, con un estratto della prosa già ripulita —
// per poter riconoscere di persona il disegno originale del libro
// cartaceo, descriverlo a un modello di IA e farsene generare uno
// indipendente. Il documento finisce sotto doc/LIBRI/ (mai in doc/
// direttamente): come per gli .htm sorgente e i .json convertiti, deriva
// da contenuto Project Aon e resta uso personale, non distribuito.
//
// Uso: ./gradlew :tool:run --args="illustrazioni doc/LIBRI/ILLUSTRAZIONI.md doc/LIBRI/01fftd.htm 01fftd Flight_from_the_Dark doc/LIBRI/02fotw.htm 02fotw Fire_on_the_Water ..."
fun runIllustrazioni(args: Array<String>) {
    if (args.size < 4 || (args.size - 1) % 3 != 0) {
        println("Uso: illustrazioni <output.md> <libro1.htm> <id1> <titolo1> [<libro2.htm> <id2> <titolo2> ...]")
        exitProcess(2)
    }
    val outputPath = args[0]
    val libri = args.drop(1).chunked(3)

    val sb = StringBuilder()
    sb.appendLine("# Illustrazioni originali — elenco per generazione IA indipendente")
    sb.appendLine()
    sb.appendLine(
        "Elenco automatico delle scene che, nel libro cartaceo Project Aon, avevano " +
            "un'illustrazione (marcatore `[Illustration N]` nell'XHTML sorgente). L'XHTML " +
            "non contiene alcuna descrizione del disegno: qui c'è solo la conferma che la " +
            "scena ne aveva una, più l'estratto della prosa già ripulita per riconoscerla a " +
            "colpo d'occhio. La colonna \"Originale\" linka la PNG vera sul sito di Project " +
            "Aon (`ill{N}.png`, N = numero romano del marcatore convertito in arabo): clicca " +
            "per vedere il disegno, descrivilo al modello e genera un'immagine indipendente; " +
            "il nome file segnaposto è solo un suggerimento, cambialo pure. Uso personale — " +
            "non distribuito, non fa parte dell'APK."
    )
    sb.appendLine()

    var totale = 0
    for ((htmPath, id, title) in libri) {
        val input = File(htmPath)
        if (!input.exists()) {
            println("File non trovato: $htmPath")
            exitProcess(2)
        }
        val result = ProjectAonHtmlParser.parse(
            file = input,
            id = id,
            title = title,
            description = "Convertito da $htmPath (Project Aon, Internet Edition) — uso personale, non distribuito.",
            genre = "FANTASY",
            disciplineDescriptions = emptyList(),
        )
        val narrativeById = result.manifest.scenes.associateBy { it.id }
        totale += result.illustrations.size

        sb.appendLine("## $title ($id) — ${result.illustrations.size} illustrazioni")
        sb.appendLine()
        sb.appendLine("| Scena | Estratto narrativo | Originale | File segnaposto | Fatto |")
        sb.appendLine("|---|---|---|---|---|")
        for (marker in result.illustrations.sortedBy { it.sceneId.toIntOrNull() ?: Int.MAX_VALUE }) {
            val excerpt = narrativeById[marker.sceneId]?.narrativeText
                ?.replace("\n", " ")
                ?.trim()
                ?.take(140)
                ?.let { if (it.length == 140) "$it…" else it }
                ?: "(scena non trovata)"
            val paddedId = marker.sceneId.toIntOrNull()?.let { "%03d".format(it) } ?: marker.sceneId
            val filename = "${id}_${paddedId}.jpg"
            val romano = marker.label.removePrefix("[").removeSuffix("]").removePrefix("Illustration").trim()
            val numero = romanoInArabo(romano)
            val originale = if (numero != null) {
                "[ill$numero.png](https://www.projectaon.org/en/xhtml/lw/$id/ill$numero.png)"
            } else {
                "(numero non riconosciuto: '$romano')"
            }
            sb.appendLine("| ${marker.sceneId} | $excerpt | $originale | `$filename` | [ ] |")
        }
        sb.appendLine()
    }

    sb.insert(0, "\n")
    sb.insert(0, "Totale: $totale illustrazioni su ${libri.size} libri.\n")

    val outputFile = File(outputPath)
    outputFile.parentFile?.mkdirs()
    outputFile.writeText(sb.toString())
    println("Scritto $outputPath ($totale illustrazioni totali su ${libri.size} libri)")
}

// Il nome file reale su Project Aon (ill{N}.png) usa il numero arabo, ma
// l'etichetta HTML riporta il numero romano — necessaria la conversione.
// L'ordine dei numeri romani nel testo NON è un contatore affidabile:
// almeno un libro (01fftd, sezione 267) ha una "Illustration XV" nel suo
// indice ma nessun marcatore inline in questo file, quindi un contatore
// avrebbe sfasato tutti i numeri successivi (XVI in poi).
private val romanValues = listOf(
    1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
    100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
    10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
)

private fun romanoInArabo(romano: String): Int? {
    if (romano.isEmpty()) return null
    var resto = romano.uppercase()
    var totale = 0
    for ((valore, simbolo) in romanValues) {
        while (resto.startsWith(simbolo)) {
            totale += valore
            resto = resto.removePrefix(simbolo)
        }
    }
    return if (resto.isEmpty()) totale else null
}
