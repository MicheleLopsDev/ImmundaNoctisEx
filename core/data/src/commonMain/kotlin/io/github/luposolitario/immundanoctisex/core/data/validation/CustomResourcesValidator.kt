package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.CustomResourceEntry
import io.github.luposolitario.immundanoctisex.core.data.model.ImageReference
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest

// Risorse url: dell'autore (§15.7, EDITOR.md) — un registro di comodità
// per l'editor, non letto dal motore: un ID duplicato non rompe nulla
// (la scena salva comunque l'url risolto), ma segnala un errore di
// scrittura probabile — quindi avviso, non blocco.
internal object CustomResourcesValidator {

    fun validate(manifest: Manifest): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        fun controllaDuplicati(categoria: String, voci: List<CustomResourceEntry>) {
            voci.groupBy { it.id }
                .filterValues { it.size > 1 }
                .keys
                .forEach { id -> warnings += "Risorse personalizzate ($categoria): ID '$id' usato più di una volta" }
        }

        controllaDuplicati("immagini", manifest.customResources.images)
        controllaDuplicati("suoni", manifest.customResources.sounds)

        // §18.4 (Scene.sfx, 30/07/2026, Michele: "puoi associare un url
        // vero e proprio oppure una risorsa statica presente nel apk"):
        // a differenza delle immagini (url nudo, risolto subito nel
        // campo della scena), un suono personalizzato è referenziato per
        // ID da Scene.sfx e risolto solo a runtime — la voce del
        // registro deve quindi portare l'informazione completa,
        // "static:<id>" o "url:<link>" (stesso parser di
        // ImageReference, riusato qui: la logica del prefisso non
        // riguarda solo le immagini nonostante il nome della classe).
        manifest.customResources.sounds.forEach { voce ->
            when (val ref = ImageReference.parse(voce.url)) {
                null -> errors += "Risorse personalizzate (suoni): '${voce.id}' non ha il prefisso static: o url:"
                is ImageReference.Static -> Unit
                is ImageReference.Url -> {
                    if (!ref.url.startsWith("http://") && !ref.url.startsWith("https://")) {
                        errors += "Risorse personalizzate (suoni): '${voce.id}' usa url: con schema non supportato (solo http/https)"
                    }
                }
            }
        }

        return ValidationResult(errors = errors, warnings = warnings)
    }
}
