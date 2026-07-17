package io.github.luposolitario.immundanoctisex.core.engine.mechanics

import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.WeaponType
import io.github.luposolitario.immundanoctisex.core.engine.dice.DiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import kotlinx.serialization.json.JsonObject

// Comandi di scena che toccano l'inventario. Ogni parametro mancante o
// non riconosciuto degrada in silenzio (REGOLE.md §5.1: mai errori).
internal object ItemMechanics {

    fun addItem(state: GameState, params: JsonObject) {
        val name = params.stringParam("itemName") ?: return
        val type = itemType(params.stringParam("itemType")) ?: return
        val quantity = params.intParam("quantity") ?: 1
        val item = GameItem(
            name = name,
            type = type,
            quantity = quantity,
            combatUsable = params.stringParam("combatUsable")?.toBooleanStrictOrNull() ?: false,
            effect = params.stringParam("effect"),
            weaponType = weaponType(params.stringParam("weaponType")),
        )
        state.updateHero { Inventory.addItem(it, item) }
    }

    fun removeItem(state: GameState, params: JsonObject) {
        val name = params.stringParam("itemName") ?: return
        val quantity = params.intParam("quantity") ?: 1
        state.updateHero { Inventory.removeItem(it, name, quantity) }
    }

    fun removeAllItems(state: GameState, params: JsonObject) {
        val type = itemType(params.stringParam("type")) ?: return
        state.updateHero { Inventory.removeAllOfType(it, type) }
    }

    // Tiro del motore, in silenzio (REGOLE.md Blocco 6): quantità = base +
    // tiro 0-9. v1 aggiungeva sempre Corone d'oro: il tipo resta GOLD di
    // default, l'autore può dichiararne un altro con itemType.
    fun rollForQuantity(state: GameState, params: JsonObject, dice: DiceRoller) {
        val name = params.stringParam("item") ?: return
        val base = params.intParam("baseValue") ?: 0
        val quantity = base + dice.roll()
        if (quantity <= 0) return
        val type = itemType(params.stringParam("itemType")) ?: ItemType.GOLD
        state.updateHero { Inventory.addItem(it, GameItem(name = name, type = type, quantity = quantity)) }
    }

    // Tiro del motore su tabella a intervalli espliciti (REGOLE.md §5.3):
    // l'outcome senza itemName è "non trovi niente".
    fun rollOnItemTable(state: GameState, params: JsonObject, dice: DiceRoller) {
        val outcome = params.outcomesParam().matchRoll(dice.roll()) ?: return
        val name = outcome.stringParam("itemName") ?: return
        val type = itemType(outcome.stringParam("itemType")) ?: ItemType.BACKPACK_ITEM
        val quantity = outcome.intParam("quantity") ?: 1
        state.updateHero { Inventory.addItem(it, GameItem(name = name, type = type, quantity = quantity)) }
    }

    // REGOLE.md §5.2: condizione sul possesso -> salto immediato.
    // operator: HAS (default, possiede almeno quantity) | NOT_HAS.
    // nextSceneId_FALSE assente = nessun salto sul ramo falso.
    fun checkItemAndJump(state: GameState, params: JsonObject): String? {
        val name = params.stringParam("itemName") ?: return null
        val quantity = params.intParam("quantity") ?: 1
        val owned = Inventory.countOf(state.hero, name)
        val has = owned >= quantity
        val met = if (params.stringParam("operator") == "NOT_HAS") !has else has
        return if (met) params.stringParam("nextSceneId_TRUE") else params.stringParam("nextSceneId_FALSE")
    }

    private fun itemType(raw: String?): ItemType? =
        ItemType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }

    private fun weaponType(raw: String?): WeaponType? =
        WeaponType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
}
