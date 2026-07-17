package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class StatType {
    COMBAT_SKILL,
    ENDURANCE,
}

// Modificatore narrativo attivo (es. ferita, benedizione). Si serializza il
// fatto (chi, quanto, per quanto), la Combattività/Resistenza effettiva la
// calcola una sola funzione dell'engine (Fase 2) sommando i modificatori.
@Serializable
data class StatModifier(
    val stat: StatType,
    val amount: Int,
    val sourceType: String,
    val duration: Int? = null,
)
