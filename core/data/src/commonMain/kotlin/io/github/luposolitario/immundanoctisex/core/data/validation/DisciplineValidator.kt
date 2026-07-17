package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.Discipline
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest

// Ogni id di disciplina citato nel pacchetto deve essere una delle 10
// discipline Kai canoniche (nessuna disciplina inventata o mal scritta,
// es. residui di SHADOWSTEP).
internal object DisciplineValidator {

    private val canonicalIds = Discipline.entries.map { it.name }.toSet()

    fun validate(manifest: Manifest): ValidationResult {
        val errors = mutableListOf<String>()

        manifest.disciplineChoices.forEach { descriptor ->
            if (descriptor.id !in canonicalIds) {
                errors += "Manifest: disciplina '${descriptor.id}' non canonica (attese: $canonicalIds)"
            }
        }

        manifest.scenes.forEach { scene ->
            scene.disciplineChoices.forEach { choice ->
                if (choice.disciplineId !in canonicalIds) {
                    errors += "Scena '${scene.id}': disciplineChoice '${choice.id}' usa disciplina " +
                        "'${choice.disciplineId}' non canonica"
                }
            }
        }

        return ValidationResult(errors = errors)
    }
}
