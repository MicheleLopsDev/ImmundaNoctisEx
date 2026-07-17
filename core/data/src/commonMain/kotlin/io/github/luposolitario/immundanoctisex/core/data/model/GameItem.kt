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
// senza cambiare schema (v0.1 implementa solo HEAL:n). weaponType è il tipo
// canonico dell'arma, valorizzato solo per type == WEAPON (mai UNARMED,
// che è una specializzazione, non un'arma).
@Serializable
data class GameItem(
    val name: String,
    val type: ItemType,
    val quantity: Int = 1,
    val combatUsable: Boolean = false,
    val effect: String? = null,
    val weaponType: WeaponType? = null,
)
