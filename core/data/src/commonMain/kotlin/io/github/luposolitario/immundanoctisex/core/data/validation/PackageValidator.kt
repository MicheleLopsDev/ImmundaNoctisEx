package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest

// Rete di sicurezza finale prima che un pacchetto entri in gioco
// (doc/ETL.md): grafo chiuso, discipline canoniche, combat con winSceneId,
// intervalli rollOnItemTable, warning globalRules non-ENDING.
object PackageValidator {

    fun validate(manifest: Manifest): ValidationResult {
        return GraphValidator.validate(manifest) +
            DisciplineValidator.validate(manifest) +
            CombatValidator.validate(manifest) +
            GameMechanicValidator.validate(manifest) +
            GlobalRuleValidator.validate(manifest)
    }
}
