package io.github.luposolitario.immundanoctisex.tool

import kotlin.system.exitProcess

// Punto d'ingresso unico del tool (29/07/2026): un solo mainClass per
// Gradle, i comandi veri sono sottocomandi — così aggiungere un pezzo
// nuovo (validate, convert, in futuro import/simula/...) non richiede
// toccare application{} in build.gradle.kts ogni volta.
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Uso: <validate|convert|illustrazioni> ...")
        exitProcess(2)
    }
    val rest = args.drop(1).toTypedArray()
    when (args[0]) {
        "validate" -> runValidate(rest)
        "convert" -> runConvert(rest)
        "illustrazioni" -> runIllustrazioni(rest)
        else -> {
            println("Comando sconosciuto: '${args[0]}' (usa 'validate', 'convert' o 'illustrazioni')")
            exitProcess(2)
        }
    }
}
