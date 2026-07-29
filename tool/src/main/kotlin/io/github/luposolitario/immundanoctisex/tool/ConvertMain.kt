package io.github.luposolitario.immundanoctisex.tool

import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineDescriptor
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageLoadResult
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageRepository
import io.github.luposolitario.immundanoctisex.tool.etl.ProjectAonHtmlParser
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.exitProcess

// Conversione XHTML Project Aon -> JSON Ex (29/07/2026, Michele: prove
// personali per lui e suo figlio, libri lunghi da testare — MAI distribuiti
// nell'app né nel repo, vedi .gitignore doc/LIBRI/). Scrive il file, stampa
// i casi da rivedere a mano (ProjectAonHtmlParser.notes) e infine lo valida
// con lo stesso PackageRepository/PackageValidator del comando 'validate'.
//
// Uso: ./gradlew :tool:run --args="convert doc/LIBRI/01fftd.htm doc/LIBRI/01fftd.json 01fftd 'Flight from the Dark'"
fun runConvert(args: Array<String>) {
    if (args.size < 4) {
        println("Uso: convert <libro.htm> <output.json> <id> <titolo>")
        exitProcess(2)
    }
    val inputPath = args[0]
    val outputPath = args[1]
    val id = args[2]
    val title = args[3]

    val input = File(inputPath)
    if (!input.exists()) {
        println("File non trovato: $inputPath")
        exitProcess(2)
    }

    val result = ProjectAonHtmlParser.parse(
        file = input,
        id = id,
        title = title,
        description = "Convertito da $inputPath (Project Aon, Internet Edition) — uso personale, non distribuito.",
        genre = "FANTASY",
        disciplineDescriptions = defaultDisciplineDescriptors(),
    )

    val outputFile = File(outputPath)
    outputFile.parentFile?.mkdirs()
    val json = Json { prettyPrint = true }
    outputFile.writeText(json.encodeToString(Manifest.serializer(), result.manifest))
    println("Scritto $outputPath (${result.manifest.scenes.size} scene)")

    if (result.notes.isEmpty()) {
        println("Nessun caso da rivedere.")
    } else {
        println("${result.notes.size} casi da rivedere a mano:")
        result.notes.forEach { println("  - $it") }
    }

    println()
    when (val validated = PackageRepository(FilePackageSource(outputFile)).load()) {
        is PackageLoadResult.Success -> {
            println("VALIDO (${validated.warnings.size} avvisi)")
            validated.warnings.forEach { println("  avviso: $it") }
        }
        is PackageLoadResult.Failure -> {
            println("NON VALIDO:")
            validated.errors.forEach { println("  errore: $it") }
        }
    }
}

// Stesso testo di content/scenes.sample.json — le 10 discipline canoniche
// non cambiano da libro a libro, solo la prosa della singola avventura.
private fun defaultDisciplineDescriptors(): List<DisciplineDescriptor> = listOf(
    DisciplineDescriptor("WEAPONSKILL", "Weaponskill", "Mastery with a chosen weapon type, adding a bonus to Combat Skill when wielding it."),
    DisciplineDescriptor("CAMOUFLAGE", "Camouflage", "The art of blending into terrain and shadow to move unseen."),
    DisciplineDescriptor("HUNTING", "Hunting", "Skill at foraging and catching food in the wild, removing the need for meals."),
    DisciplineDescriptor("SIXTH_SENSE", "Sixth Sense", "A heightened intuition that warns of danger and hidden threats."),
    DisciplineDescriptor("TRACKING", "Tracking", "The ability to follow trails and read signs left by passage."),
    DisciplineDescriptor("HEALING", "Healing", "The gift of mending wounds and restoring endurance without herbs."),
    DisciplineDescriptor("MINDSHIELD", "Mindshield", "A mental ward against psychic intrusion and fear."),
    DisciplineDescriptor("MINDBLAST", "Mindblast", "The power to strike an enemy's mind directly, aiding in combat."),
    DisciplineDescriptor("ANIMAL_KINSHIP", "Animal Kinship", "An affinity with animals, calming beasts and reading their moods."),
    DisciplineDescriptor("MIND_OVER_MATTER", "Mind Over Matter", "Telekinetic control, allowing objects to be moved by will alone."),
)
