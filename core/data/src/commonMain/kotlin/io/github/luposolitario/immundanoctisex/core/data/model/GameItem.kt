package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ItemType {
    WEAPON,
    BACKPACK_ITEM,
    SPECIAL_ITEM,
    GOLD,
}

// Formato oggetto canonico (STATO.md §4.2): effetto dichiarativo estensibile
// senza cambiare schema (v0.1 implementa solo HEAL:n). weaponType è l'ID
// canonico dell'arma solo per type == WEAPON: resta String finché l'enum
// WeaponType (task [MICHELE], STATO.md §4.3) non è scritto.
@Serializable
data class GameItem(
    val name: String,
    val type: ItemType,
    val quantity: Int = 1,
    val combatUsable: Boolean = false,
    val effect: String? = null,
    val weaponType: String? = null,
)
