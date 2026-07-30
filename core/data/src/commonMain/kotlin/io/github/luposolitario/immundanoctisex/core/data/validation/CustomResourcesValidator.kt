package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.CustomResourceEntry
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest

// Risorse url: dell'autore (§15.7, EDITOR.md) — un registro di comodità
// per l'editor, non letto dal motore: un ID duplicato non rompe nulla
// (la scena salva comunque l'url risolto), ma segnala un errore di
// scrittura probabile — quindi avviso, non blocco.
internal object CustomResourcesValidator {

    fun validate(manifest: Manifest): ValidationResult {
        val warnings = mutableListOf<String>()

        fun controllaDuplicati(categoria: String, voci: List<CustomResourceEntry>) {
            voci.groupBy { it.id }
                .filterValues { it.size > 1 }
                .keys
                .forEach { id -> warnings += "Risorse personalizzate ($categoria): ID '$id' usato più di una volta" }
        }

        controllaDuplicati("immagini", manifest.customResources.images)
        controllaDuplicati("suoni", manifest.customResources.sounds)

        return ValidationResult(errors = emptyList(), warnings = warnings)
    }
}
