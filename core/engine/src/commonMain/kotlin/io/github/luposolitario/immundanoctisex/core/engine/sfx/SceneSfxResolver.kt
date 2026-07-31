package io.github.luposolitario.immundanoctisex.core.engine.sfx

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene

// Scene.sfx (31/07/2026): un ID che punta a Manifest.customResources.sounds
// — sovrascrive il meccanismo automatico del client (un mp3 per ogni
// immagine risolta) con un suono scelto dall'autore. Qui solo la
// RISOLUZIONE (ID -> valore static:/url: registrato), non la riproduzione:
// il client (:app) sa suonare, non conosce lo schema del Manifest oltre
// quel che gli serve.
//
// Funzione totale anche se il caso "id non trovato" è già strutturalmente
// irraggiungibile in produzione: PackageRepository.load() esegue sempre
// PackageValidator.validate() (che compone SfxValidator) prima di esporre
// un Manifest, quindi uno Scene.sfx non nullo arriva qui SEMPRE già
// registrato correttamente. Testata comunque per documentare il contratto,
// non solo per il caso felice.
object SceneSfxResolver {
    fun resolve(scene: Scene, manifest: Manifest): String? =
        scene.sfx?.let { id -> manifest.customResources.sounds.firstOrNull { it.id == id }?.url }
}
