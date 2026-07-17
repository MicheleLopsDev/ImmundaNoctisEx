package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// Scelta disponibile solo a chi possiede una certa disciplina Kai.
// disciplineId è un ID canonico (validato contro Discipline).
@Serializable
data class DisciplineChoice(
    val id: String,
    val disciplineId: String,
    val choiceText: String,
    val nextSceneId: String,
    val minRoll: Int? = null,
    val maxRoll: Int? = null,
    val requiredItem: String? = null,
    val requiredFlag: String? = null,
)
