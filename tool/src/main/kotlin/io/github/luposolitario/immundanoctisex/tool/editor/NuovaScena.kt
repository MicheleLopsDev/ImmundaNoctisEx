package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType

// Nuova scena vuota dalla mappa (§15.5, Michele: "aggiungere... le scene
// selezionate"). ID numerico successivo al più alto già usato — stessa
// convenzione delle scene create dagli scaffold (BookScaffolds.kt: "1",
// "2", "3"...). Se il libro non ha ancora scene numeriche (caso raro,
// solo la scena di sconfitta "morte"), riparte da "1".
fun nuovaScenaVuota(manifest: Manifest): Scene {
    val prossimoId = (manifest.scenes.mapNotNull { it.id.toIntOrNull() }.maxOrNull() ?: 0) + 1
    return Scene(
        id = prossimoId.toString(),
        sceneType = SceneType.TRANSITION,
        genre = manifest.genre,
        narrativeText = "",
    )
}

// Rete di sicurezza per scene nuove senza uscita (§15.5, Michele: "ogni
// nuova scena per default se non ha collegamenti deve collegarsi alla
// scena di sconfitta di default... questo sistema il problema in alcuni
// casi facendo passare la validazione"). Va richiamata SOLO al momento
// della creazione (vedi SceneEditorScreen.eNuova), mai come correttore
// retroattivo su scene già scritte: se la scena ha già un collegamento
// in uscita qualunque (scelta, scelta-disciplina o combattimento), o se
// il libro non ha ancora una scena di sconfitta dichiarata, non cambia
// nulla.
fun conReteDiSicurezza(scene: Scene, deathSceneId: String?): Scene {
    if (deathSceneId == null || scene.outgoingSceneIds().isNotEmpty()) return scene
    return scene.copy(
        choices = scene.choices + Choice(
            id = "uscita_default",
            choiceText = "Continua...",
            nextSceneId = deathSceneId,
        ),
    )
}
