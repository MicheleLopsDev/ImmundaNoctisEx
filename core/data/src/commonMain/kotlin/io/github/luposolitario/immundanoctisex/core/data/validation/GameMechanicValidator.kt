package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// rollOnItemTable (REGOLE.md §5.3): intervalli espliciti di tiro 0-9 in
// params.outcomes[].{minRoll,maxRoll}. Il validatore verifica copertura
// completa e assenza di sovrapposizioni, non probabilità implicite.
internal object GameMechanicValidator {

    fun validate(manifest: Manifest): ValidationResult {
        val errors = mutableListOf<String>()

        manifest.scenes.forEach { scene ->
            scene.gameMechanics.forEachIndexed { index, mechanic ->
                if (mechanic.command != "rollOnItemTable") return@forEachIndexed
                val label = "Scena '${scene.id}': gameMechanics[$index] (rollOnItemTable)"

                val outcomes = mechanic.params["outcomes"]?.jsonArray
                if (outcomes == null) {
                    errors += "$label: manca 'outcomes'"
                    return@forEachIndexed
                }

                val covered = BooleanArray(10)
                var malformed = false
                outcomes.forEach { outcome ->
                    val obj = outcome.jsonObject
                    val min = obj["minRoll"]?.jsonPrimitive?.int
                    val max = obj["maxRoll"]?.jsonPrimitive?.int
                    if (min == null || max == null || min !in 0..9 || max !in 0..9 || min > max) {
                        errors += "$label: intervallo non valido ($min-$max)"
                        malformed = true
                        return@forEach
                    }
                    for (roll in min..max) {
                        if (covered[roll]) {
                            errors += "$label: tiro $roll coperto da più intervalli"
                        }
                        covered[roll] = true
                    }
                }

                if (!malformed) {
                    val missing = covered.indices.filterNot { covered[it] }
                    if (missing.isNotEmpty()) {
                        errors += "$label: tiri non coperti: $missing"
                    }
                }
            }
        }

        return ValidationResult(errors = errors)
    }
}
