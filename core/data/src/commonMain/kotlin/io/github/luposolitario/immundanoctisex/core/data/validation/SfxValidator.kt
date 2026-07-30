package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest

// Scene.sfx (30/07/2026, Michele: "un id_url presente nella sezione
// risorse che corrisponde ad un mp3"): a differenza di backgroundImage/
// npcImage/combat.enemyImage (url: diretto, §15.7 ImageReferenceValidator),
// qui NON è ammesso un url: scritto a mano — deve essere l'ID di una voce
// già registrata in customResources.sounds. Un ID che non esiste è un
// ERRORE bloccante (stesso peso di "Nessuna scena START trovata"): un
// campo silenziosamente ignorato dal client sarebbe peggio di un blocco
// esplicito in fase di validazione.
internal object SfxValidator {

    fun validate(manifest: Manifest): ValidationResult {
        val errors = mutableListOf<String>()
        val idSuoniRegistrati = manifest.customResources.sounds.map { it.id }.toSet()

        manifest.scenes.forEach { scene ->
            val sfx = scene.sfx ?: return@forEach
            if (sfx.isBlank()) {
                errors += "Scena '${scene.id}': sfx non può essere una stringa vuota"
            } else if (sfx !in idSuoniRegistrati) {
                errors += "Scena '${scene.id}': sfx punta a '$sfx', non registrato in customResources.sounds"
            }
        }

        return ValidationResult(errors = errors)
    }
}
