package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SceneType {
    START,
    TRANSITION,
    ENDING,
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
)
