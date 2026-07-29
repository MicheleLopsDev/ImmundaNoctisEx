package io.github.luposolitario.immundanoctisex.tool

import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageLoadResult
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageRepository
import java.io.File
import kotlin.system.exitProcess

// Validatore a riga di comando (29/07/2026, Michele: "un processo che ci
// dice se un json è compatibile col nostro formato" — servirà sempre,
// prima ancora che esista una UI per il tool). Nessuna logica di
// validazione qui: solo la lettura del file e la stampa del risultato di
// PackageRepository/PackageValidator, esattamente lo stesso codice usato
// dall'app per caricare un libro.
//
// Uso: ./gradlew :tool:run --args="validate content/scenes.sample.json [altro.json ...]"
fun runValidate(args: Array<String>) {
    if (args.isEmpty()) {
        println("Uso: validate <file.json> [altro.json ...]")
        exitProcess(2)
    }

    var tuttiValidi = true
    args.forEach { path ->
        val file = File(path)
        if (!file.exists()) {
            println("$path: FILE NON TROVATO")
            tuttiValidi = false
            return@forEach
        }

        when (val result = PackageRepository(FilePackageSource(file)).load()) {
            is PackageLoadResult.Success -> {
                println("$path: VALIDO (${result.manifest.scenes.size} scene)")
                result.warnings.forEach { println("  avviso: $it") }
            }
            is PackageLoadResult.Failure -> {
                println("$path: NON VALIDO")
                result.errors.forEach { println("  errore: $it") }
                tuttiValidi = false
            }
        }
    }

    exitProcess(if (tuttiValidi) 0 else 1)
}
