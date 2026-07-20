package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SceneType {
    START,
    TRANSITION,
    ENDING,
}

// Come è andata a finire. L'autore lo dichiara sulle scene ENDING:
// il motore NON lo indovina, perché un finale amaro raggiunto vivo e una
// vittoria si somigliano troppo per essere dedotti dallo stato del gioco
// (decisione di Michele, 20/07/2026 — vedi DIARIO).
//
// NEUTRAL è il default di chi non dichiara nulla: il gioco dice comunque
// che l'avventura è finita, senza mentire sull'esito.
@Serializable
enum class EndingOutcome {
    VICTORY,
    DEFEAT,
    NEUTRAL,
}

// Una scena del libro. locationName è opzionale e appiccicoso: se assente
// eredita dalla scena precedente nel percorso (decisione post-specifica 4),
// l'autore lo scrive solo quando il luogo cambia.
@Serializable
data class Scene(
    val id: String,
    val sceneType: SceneType,
    val genre: String,
    val toneHints: List<String> = emptyList(),
    val backgroundImage: String? = null,
    val locationName: String? = null,
    val narrativeText: String,
    val choices: List<Choice> = emptyList(),
    val disciplineChoices: List<DisciplineChoice> = emptyList(),
    val combat: Combat? = null,
    val gameMechanics: List<GameMechanic> = emptyList(),
    // Solo per sceneType ENDING: come si chiude l'avventura. Assente su
    // una scena di finale = NEUTRAL (l'esito non viene inventato).
    val outcome: EndingOutcome? = null,
)
