package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// Scelta ordinaria di scena. Campi predisposti (minRoll/maxRoll/requiredItem/
// requiredFlag) restano nello schema anche se vuoti: decisione sessione 14/07.
@Serializable
data class Choice(
    val id: String,
    val choiceText: String,
    val nextSceneId: String,
    val minRoll: Int? = null,
    val maxRoll: Int? = null,
    val requiredItem: String? = null,
    val requiredFlag: String? = null,
)
