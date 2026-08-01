package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// Bonus/malus applicato al tiro della Tabella dei Numeri Casuali PRIMA
// che si guardi quale intervallo lo copre (01/08/2026, misurato su 57
// scene dei 5 libri Project Aon convertiti):
//
//   "Pick a number from the Random Number Table. If you have the Kai
//    Discipline of Sixth Sense, you may add 2 to this number.
//    If your total is 0–3, turn to 58; 4–6, turn to 167; 7–11, turn to 329."
//
// Sta sulla SCENA e non sulla Choice perché il tiro è UNO solo per
// scena: gli intervalli stanno sulle scelte, il modificatore no —
// metterlo per scelta obbligherebbe a ripeterlo identico su ogni ramo.
//
// Il tiro grezzo resta 0-9 (DiceRoller): col modificatore il TOTALE può
// uscire da quell'intervallo, ed è normale — i libri hanno scelte tipo
// "7–11" proprio per questo.
@Serializable
data class RollModifier(
    // Quanto si somma al tiro: positivo o negativo ("deduct 3" = -3).
    val amount: Int,
    // Quando si applica. null = SEMPRE (esiste davvero: "Pick a number
    // from the Random Number Table and add 5 to it", senza condizioni).
    val condition: RollCondition? = null,
)

// I quattro tipi coprono 50 dei 57 casi reali. Fuori copertura per
// decisione esplicita (01/08/2026): il Rango Kai ("if you have reached
// the Kai rank of Guardian or higher", 7 casi) — i titoli dei libri non
// corrispondono al nostro `KaiRank`, che è dichiarato puramente
// cosmetico. Quelle scene restano scelte manuali finché non si decide
// se dare al rango un effetto meccanico.
@Serializable
enum class RollConditionType {
    DISCIPLINE,
    ITEM,
    FLAG,
    ENDURANCE,
}

// Vocabolario CHIUSO, come il resto delle condizioni del progetto
// (GlobalRule, conditionalAction): niente espressioni libere da
// interpretare a runtime.
@Serializable
data class RollCondition(
    val type: RollConditionType,
    // In OR fra loro: "the Kai Discipline of either Mind Over Matter or
    // Mindblast" sono due voci, basta possederne una. Per DISCIPLINE
    // sono ID canonici di `Discipline`, per ITEM nomi di oggetto, per
    // FLAG nomi di flag. Vuota (e ignorata) per ENDURANCE.
    val values: List<String> = emptyList(),
    // Solo per ENDURANCE ("if your current ENDURANCE point total is
    // less than 10"): si riusa ComparisonOperator di GlobalRule.kt
    // invece di introdurne un secondo dialetto.
    val operator: ComparisonOperator? = null,
    val threshold: Int? = null,
)
