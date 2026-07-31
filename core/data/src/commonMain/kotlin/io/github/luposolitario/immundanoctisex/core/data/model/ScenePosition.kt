package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// Posizione di un nodo sulla mappa dell'editor grafico (:tool,
// EDITOR.md §19.12) — SOLO per l'editor: il motore/client Android non
// la legge mai (PackageRepository carica con `ignoreUnknownKeys =
// true`, un pacchetto senza questo campo resta valido com'è oggi).
@Serializable
data class ScenePosition(val x: Float, val y: Float)
