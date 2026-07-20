package io.github.luposolitario.immundanoctisex.core.engine.mechanics

import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.WeaponType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Lettura tollerante dei parametri di un GameMechanic: parametro assente o
// malformato -> null, mai eccezione (il gioco non si blocca mai; il comando
// che non capisce i suoi parametri semplicemente non fa nulla).

internal fun JsonObject.stringParam(name: String): String? =
    runCatching { get(name)?.jsonPrimitive?.content }.getOrNull()

internal fun JsonObject.intParam(name: String): Int? =
    stringParam(name)?.toIntOrNull()

// outcomes: array di oggetti {minRoll, maxRoll, ...payload} — intervalli
// espliciti di tiro come nei libri veri (REGOLE.md §5.3).
internal fun JsonObject.outcomesParam(): List<JsonObject> =
    runCatching { get("outcomes")?.jsonArray?.map { it.jsonObject } }.getOrNull() ?: emptyList()

internal fun List<JsonObject>.matchRoll(roll: Int): JsonObject? =
    firstOrNull { outcome ->
        val min = outcome.intParam("minRoll") ?: return@firstOrNull false
        val max = outcome.intParam("maxRoll") ?: return@firstOrNull false
        roll in min..max
    }

// Confronto usato da checkStatAndJump/checkItemAndJump: accetta sia i
// simboli (formato GlobalRule) sia le parole di v1, così l'ETL può
// produrre l'uno o l'altro senza rompere nulla.
internal fun compare(left: Int, operator: String, right: Int): Boolean = when (operator) {
    "==", "EQUALS" -> left == right
    "!=", "NOT_EQUALS" -> left != right
    ">=", "GREATER_THAN_OR_EQUAL" -> left >= right
    "<=", "LESS_THAN_OR_EQUAL" -> left <= right
    ">", "GREATER_THAN" -> left > right
    "<", "LESS_THAN" -> left < right
    else -> false
}

// Spostate da ItemMechanics (erano private lì): servono anche a
// ItemOffers, che vive fuori da questo package (:core:engine.inventory)
// ma nello stesso modulo — internal basta, non serve renderle public.
internal fun itemType(raw: String?): ItemType? =
    ItemType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }

internal fun weaponType(raw: String?): WeaponType? =
    WeaponType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
