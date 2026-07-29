package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.ImageReference
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest

// Ogni riferimento a un'immagine (backgroundImage, npcImage,
// combat.enemyImage) deve avere il prefisso static: o url: (29/07/2026,
// Michele): un url: è sempre segnalato come avviso, anche se ben
// formato, perché introduce una dipendenza di rete da rivedere prima di
// pubblicare/distribuire il libro — vedi doc/SCHEMA-JSON.md.
internal object ImageReferenceValidator {

    fun validate(manifest: Manifest): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        fun check(sceneId: String, field: String, value: String?) {
            if (value == null) return
            when (val ref = ImageReference.parse(value)) {
                null -> errors += "Scena '$sceneId': $field '$value' non ha il prefisso static: o url:"
                is ImageReference.Static -> Unit
                is ImageReference.Url -> {
                    if (!ref.url.startsWith("http://") && !ref.url.startsWith("https://")) {
                        errors += "Scena '$sceneId': $field usa url: con schema non supportato (solo http/https)"
                    }
                    warnings += "Scena '$sceneId': $field punta a un link esterno (${ref.url}) — verifica prima di pubblicare il libro"
                }
            }
        }

        manifest.scenes.forEach { scene ->
            check(scene.id, "backgroundImage", scene.backgroundImage)
            check(scene.id, "npcImage", scene.npcImage)
            check(scene.id, "combat.enemyImage", scene.combat?.enemyImage)
        }

        return ValidationResult(errors = errors, warnings = warnings)
    }
}
