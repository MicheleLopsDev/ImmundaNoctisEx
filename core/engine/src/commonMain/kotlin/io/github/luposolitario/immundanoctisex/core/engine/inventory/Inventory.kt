package io.github.luposolitario.immundanoctisex.core.engine.inventory

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveMaxEndurance
import io.github.luposolitario.immundanoctisex.core.engine.stats.itemEnduranceBonus

// Inventario con i limiti canonici di Lupo Solitario (STATO.md §4.1):
// WEAPON 2, BACKPACK_ITEM 8 posti, SPECIAL_ITEM illimitati, GOLD 50 Corone.
// I limiti li fa rispettare il motore: oltre soglia l'oggetto non entra
// (o entra parziale fino al limite), SENZA errore — il gioco non si blocca
// mai. Funzioni pure: Character dentro, Character nuovo fuori.
object Inventory {

    const val MAX_WEAPONS = 2
    const val MAX_BACKPACK_SLOTS = 8
    const val MAX_GOLD = 50

    // Un "posto" dello zaino è un'unità di quantità (8 posti disegnati
    // anche vuoti, UI.md); le Corone si contano per quantità totale.
    // Gli oggetti con bonus Resistenza (effetto ENDURANCE:n, es. Elmo)
    // alzano anche la Resistenza corrente al momento dell'acquisizione
    // (canone: "aggiunge N punti al tuo totale").
    fun addItem(character: Character, item: GameItem): Character {
        if (item.quantity <= 0) return character
        val updated = when (item.type) {
            ItemType.WEAPON -> addWeapon(character, item)
            ItemType.SPECIAL_ITEM -> character.copy(inventory = merge(character.inventory, item))
            ItemType.BACKPACK_ITEM -> addCapped(character, item, MAX_BACKPACK_SLOTS)
            ItemType.GOLD -> addCapped(character, item, MAX_GOLD)
        }
        val gainedBonus = itemEnduranceBonus(item)
        if (updated === character || gainedBonus == 0) return updated
        return updated.copy(
            currentEndurance = (updated.currentEndurance + gainedBonus)
                .coerceAtMost(effectiveMaxEndurance(updated)),
        )
    }

    // Rimozione tollerante (REGOLE.md §5.1): rimuove quel che c'è, mai
    // errore se manca quantità. Se l'arma impugnata esce dall'inventario,
    // equippedWeapon si azzera; se esce un oggetto con bonus Resistenza,
    // la corrente si riclampa al nuovo massimo effettivo.
    fun removeItem(character: Character, itemName: String, quantity: Int): Character {
        if (quantity <= 0) return character
        var toRemove = quantity
        val remaining = character.inventory.mapNotNull { item ->
            if (toRemove <= 0 || !item.name.equals(itemName, ignoreCase = true)) return@mapNotNull item
            val removed = minOf(item.quantity, toRemove)
            toRemove -= removed
            if (item.quantity - removed > 0) item.copy(quantity = item.quantity - removed) else null
        }
        return character.copy(inventory = remaining).clearEquipIfMissing().reclampEndurance()
    }

    fun removeAllOfType(character: Character, type: ItemType): Character =
        character.copy(inventory = character.inventory.filterNot { it.type == type })
            .clearEquipIfMissing()
            .reclampEndurance()

    fun countOf(character: Character, itemName: String): Int =
        character.inventory
            .filter { it.name.equals(itemName, ignoreCase = true) }
            .sumOf { it.quantity }

    // Equipaggia solo un'arma posseduta; nome sconosciuto = nessun effetto.
    fun equipWeapon(character: Character, itemName: String): Character {
        val owned = character.inventory.any {
            it.type == ItemType.WEAPON && it.name.equals(itemName, ignoreCase = true)
        }
        return if (owned) character.copy(equippedWeapon = itemName) else character
    }

    fun unequipWeapon(character: Character): Character =
        character.copy(equippedWeapon = null)

    private fun addWeapon(character: Character, item: GameItem): Character {
        val weapons = character.inventory.count { it.type == ItemType.WEAPON }
        if (weapons >= MAX_WEAPONS) return character
        // Le armi non si impilano: una voce per arma, quantità sempre 1.
        return character.copy(inventory = character.inventory + item.copy(quantity = 1))
    }

    private fun addCapped(character: Character, item: GameItem, cap: Int): Character {
        val used = character.inventory.filter { it.type == item.type }.sumOf { it.quantity }
        val space = cap - used
        if (space <= 0) return character
        val accepted = minOf(item.quantity, space)
        return character.copy(inventory = merge(character.inventory, item.copy(quantity = accepted)))
    }

    private fun merge(inventory: List<GameItem>, item: GameItem): List<GameItem> {
        val existing = inventory.firstOrNull {
            it.type == item.type && it.name.equals(item.name, ignoreCase = true)
        } ?: return inventory + item
        return inventory.map {
            if (it === existing) it.copy(quantity = it.quantity + item.quantity) else it
        }
    }

    private fun Character.reclampEndurance(): Character {
        val max = effectiveMaxEndurance(this)
        return if (currentEndurance > max) copy(currentEndurance = max) else this
    }

    private fun Character.clearEquipIfMissing(): Character {
        val equipped = equippedWeapon ?: return this
        val stillOwned = inventory.any {
            it.type == ItemType.WEAPON && it.name.equals(equipped, ignoreCase = true)
        }
        return if (stillOwned) this else copy(equippedWeapon = null)
    }
}
