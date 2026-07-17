package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType

// Grafo chiuso: ogni destinazione citata da qualunque parte del pacchetto
// deve esistere come id di scena. Include anche id di scena duplicati e
// l'assenza di una scena START, che romperebbero il caricamento a runtime
// in modo silenzioso.
internal object GraphValidator {

    fun validate(manifest: Manifest): ValidationResult {
        val errors = mutableListOf<String>()
        val sceneIds = manifest.scenes.map { it.id }
        val knownIds = sceneIds.toSet()

        sceneIds.groupingBy { it }.eachCount().filter { it.value > 1 }.forEach { (id, count) ->
            errors += "Scena '$id' duplicata ($count volte)"
        }

        if (manifest.scenes.none { it.sceneType == SceneType.START }) {
            errors += "Nessuna scena START trovata"
        }

        fun checkDestination(from: String, field: String, targetId: String?) {
            if (targetId != null && targetId !in knownIds) {
                errors += "Scena '$from': $field punta a '$targetId', che non esiste"
            }
        }

        checkDestination("manifest", "deathSceneId", manifest.deathSceneId)
        manifest.globalRules.forEachIndexed { index, rule ->
            checkDestination("manifest.globalRules[$index]", "targetSceneId", rule.targetSceneId)
        }

        manifest.scenes.forEach { scene ->
            scene.choices.forEach { choice ->
                checkDestination(scene.id, "choice '${choice.id}'.nextSceneId", choice.nextSceneId)
            }
            scene.disciplineChoices.forEach { choice ->
                checkDestination(scene.id, "disciplineChoice '${choice.id}'.nextSceneId", choice.nextSceneId)
            }
            scene.combat?.let { combat ->
                checkDestination(scene.id, "combat.winSceneId", combat.winSceneId)
                checkDestination(scene.id, "combat.loseSceneId", combat.loseSceneId)
                checkDestination(scene.id, "combat.evadeSceneId", combat.evadeSceneId)
            }
        }

        return ValidationResult(errors = errors)
    }
}
