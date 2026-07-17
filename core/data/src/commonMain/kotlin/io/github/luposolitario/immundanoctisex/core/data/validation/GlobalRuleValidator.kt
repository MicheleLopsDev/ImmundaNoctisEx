package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType

// Raccomandazione, non errore (REGOLE.md §2.4): le destinazioni delle
// globalRules dovrebbero essere scene ENDING. Richiede che il grafo sia
// già chiuso: se targetSceneId non esiste, GraphValidator lo segnala già
// come errore e qui non si duplica la segnalazione.
internal object GlobalRuleValidator {

    fun validate(manifest: Manifest): ValidationResult {
        val warnings = mutableListOf<String>()
        val scenesById = manifest.scenes.associateBy { it.id }

        manifest.globalRules.forEachIndexed { index, rule ->
            val target = scenesById[rule.targetSceneId] ?: return@forEachIndexed
            if (target.sceneType != SceneType.ENDING) {
                warnings += "globalRules[$index]: destinazione '${rule.targetSceneId}' non è una scena ENDING"
            }
        }

        return ValidationResult(warnings = warnings)
    }
}
