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
        val warnings = mutableListOf<String>()
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
                checkDestination(scene.id, "combat.winSceneIdRapido", combat.winSceneIdRapido)
                checkDestination(scene.id, "combat.loseSceneId", combat.loseSceneId)
                checkDestination(scene.id, "combat.evadeSceneId", combat.evadeSceneId)
            }
        }

        // §19.7 (Michele, editor: "segnalare i vicoli ciechi"): una scena
        // TRANSITION senza scelte, scelte-disciplina né combattimento
        // non ha modo di continuare — il giocatore ci resta bloccato,
        // salvo un salto d'ufficio da gameMechanics/globalRules che
        // questo controllo (come il grafo visivo dell'editor) non
        // modella. Avviso, non errore: può essere intenzionale.
        manifest.scenes.forEach { scene ->
            if (scene.sceneType == SceneType.TRANSITION &&
                scene.choices.isEmpty() &&
                scene.disciplineChoices.isEmpty() &&
                scene.combat == null
            ) {
                warnings += "Scena '${scene.id}': vicolo cieco (TRANSITION senza scelte né combattimento)"
            }
        }

        return ValidationResult(errors = errors, warnings = warnings)
    }
}
