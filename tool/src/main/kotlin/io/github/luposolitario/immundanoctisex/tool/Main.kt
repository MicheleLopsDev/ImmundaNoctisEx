package io.github.luposolitario.immundanoctisex.tool

import kotlin.system.exitProcess

// Punto d'ingresso unico del tool (29/07/2026): un solo mainClass per
// Gradle, i comandi veri sono sottocomandi — così aggiungere un pezzo
// nuovo (validate, convert, in futuro import/simula/...) non richiede
// toccare application{} in build.gradle.kts ogni volta.
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Uso: <validate|forma|convert|illustrazioni|bonifica|svuotaTesti|riempiTesti|verificaGrafo> ...")
        exitProcess(2)
    }
    val rest = args.drop(1).toTypedArray()
    when (args[0]) {
        "validate" -> runValidate(rest)
        // La forma del grafo contro quella dei librogame pubblicati
        // (06/08/2026): validate dice se il grafo regge, questo se ha
        // la forma di un librogame. Vedi doc/FORMA-DEI-GRAFI.md.
        "forma" -> runForma(rest)
        "convert" -> runConvert(rest)
        "illustrazioni" -> runIllustrazioni(rest)
        "bonifica" -> runBonifica(rest)
        // Separazione meccaniche/testi per i libri Project Aon
        // (03/08/2026, README §15).
        "svuotaTesti" -> runSvuotaTesti(rest)
        "riempiTesti" -> runRiempiTesti(rest)
        // Verifica della conversione contro il grafo ufficiale
        // (05/08/2026): l'unico controllo con una fonte esterna.
        "verificaGrafo" -> runVerificaGrafo(rest)
        else -> {
            println(
                "Comando sconosciuto: '${args[0]}' (usa 'validate', 'forma', 'convert', 'illustrazioni', " +
                    "'bonifica', 'svuotaTesti', 'riempiTesti' o 'verificaGrafo')",
            )
            exitProcess(2)
        }
    }
}
