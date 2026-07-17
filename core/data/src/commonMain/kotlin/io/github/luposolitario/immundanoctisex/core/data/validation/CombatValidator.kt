package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest

// winSceneId è già obbligatorio a livello di schema (campo non nullable):
// qui si intercetta il caso limite di una stringa vuota, che supererebbe
// il parsing ma non il senso della regola (REGOLE.md §1.5).
internal object CombatValidator {

    fun validate(manifest: Manifest): ValidationResult {
        val errors = mutableListOf<String>()

        manifest.scenes.forEach { scene ->
            val combat = scene.combat ?: return@forEach
            if (combat.winSceneId.isBlank()) {
                errors += "Scena '${scene.id}': combat.winSceneId è obbligatorio"
            }
        }

        return ValidationResult(errors = errors)
    }
}
