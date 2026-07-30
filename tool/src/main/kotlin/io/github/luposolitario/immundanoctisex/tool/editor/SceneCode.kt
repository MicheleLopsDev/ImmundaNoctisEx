package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType

// Codice leggibile calcolato al volo per riconoscere una scena a colpo
// d'occhio (30/07/2026, Michele: "TAV-TRANS-ID... mentre scrivo penso
// che sono in una taverna, voglio agganciare quella di passaggio").
// Deliberatamente NON un campo dello schema: si ricalcola sempre da
// sceneType/locationName/id, quindi i libri già convertiti restano
// identici e non c'è nulla da tenere sincronizzato a mano nel JSON.
//
// locationName è "appiccicoso" a runtime (eredita dalla scena
// precedente nel percorso del giocatore, REGOLE.md) ma qui, nell'editor,
// non c'è "il percorso del giocatore" — solo il grafo — quindi il
// codice usa "SC" (generico) per le scene dove l'autore non l'ha
// dichiarato esplicitamente su QUESTA scena.
fun codiceScena(scene: Scene): String {
    val location = scene.locationName
        ?.filter { it.isLetter() }
        ?.uppercase()
        ?.take(3)
        ?.ifBlank { null }
        ?: "SC"
    val tipo = when (scene.sceneType) {
        SceneType.START -> "START"
        SceneType.TRANSITION -> "TRANS"
        SceneType.ENDING -> "END"
    }
    return "$location-$tipo-${scene.id}"
}
