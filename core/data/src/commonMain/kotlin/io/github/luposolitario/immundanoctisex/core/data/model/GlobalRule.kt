package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Regola condizione -> destinazione valutata a ogni transizione, dopo i
// gameMechanics della scena di arrivo (REGOLE.md Blocco 2). La vittoria
// dell'avventura è una globalRule come le altre: non esiste un campo dedicato.
@Serializable
data class GlobalRule(
    val type: GlobalRuleType,
    val name: String,
    val operator: ComparisonOperator,
    val value: String,
    val targetSceneId: String,
)

@Serializable
enum class GlobalRuleType {
    FLAG,
    VAR,
}

@Serializable
enum class ComparisonOperator {
    @SerialName("==") EQ,
    @SerialName("!=") NEQ,
    @SerialName(">=") GTE,
    @SerialName("<=") LTE,
    @SerialName(">") GT,
    @SerialName("<") LT,
}
