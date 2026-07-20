package io.github.luposolitario.immundanoctisex.core.engine.inventory

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InventoryTest {

    private fun hero(vararg items: GameItem, equipped: String? = null) = Character(
        role = CharacterRole.HERO,
        name = "Eroe di prova",
        baseCombatSkill = 15,
        currentEndurance = 20,
        maxEndurance = 20,
        inventory = items.toList(),
        equippedWeapon = equipped,
    )

    private fun weapon(name: String) = GameItem(name = name, type = ItemType.WEAPON)
    private fun meal(quantity: Int = 1) = GameItem(name = "Meal", type = ItemType.BACKPACK_ITEM, quantity = quantity)
    private fun gold(quantity: Int) = GameItem(name = "Gold Crowns", type = ItemType.GOLD, quantity = quantity)

    @Test
    fun terzaArmaNonEntraSenzaErrore() {
        val character = hero(weapon("Sword"), weapon("Axe"))

        val result = Inventory.addItem(character, weapon("Mace"))

        assertEquals(2, result.inventory.size)
    }

    @Test
    fun zainoOltreGliOttoPostiEntraParziale() {
        val character = hero(meal(6))

        val result = Inventory.addItem(character, meal(5))

        assertEquals(8, Inventory.countOf(result, "Meal"))
    }

    @Test
    fun oroOltreLe50CoroneSiFermaAlTetto() {
        val character = hero(gold(45))

        val result = Inventory.addItem(character, gold(20))

        assertEquals(50, Inventory.countOf(result, "Gold Crowns"))
    }

    @Test
    fun oggettiSpecialiIllimitatiESiImpilano() {
        var character = hero(GameItem(name = "Map of Sommerlund", type = ItemType.SPECIAL_ITEM))

        character = Inventory.addItem(character, GameItem(name = "Map of Sommerlund", type = ItemType.SPECIAL_ITEM))

        assertEquals(1, character.inventory.size)
        assertEquals(2, Inventory.countOf(character, "Map of Sommerlund"))
    }

    @Test
    fun rimozioneTolleranteOltreIlPosseduto() {
        val character = hero(meal(2))

        val result = Inventory.removeItem(character, "Meal", 5)

        assertEquals(0, Inventory.countOf(result, "Meal"))
    }

    @Test
    fun rimuovereArmaImpugnataAzzeraEquippedWeapon() {
        val character = hero(weapon("Sword"), equipped = "Sword")

        val result = Inventory.removeItem(character, "Sword", 1)

        assertNull(result.equippedWeapon)
    }

    @Test
    fun removeAllOfTypeSvuotaSoloQuelTipo() {
        val character = hero(weapon("Sword"), meal(3))

        val result = Inventory.removeAllOfType(character, ItemType.BACKPACK_ITEM)

        assertEquals(0, Inventory.countOf(result, "Meal"))
        assertEquals(1, Inventory.countOf(result, "Sword"))
    }

    @Test
    fun elmoAlzaResistenzaCorrenteEMassimoEffettivo() {
        val helmet = GameItem(name = "Helmet", type = ItemType.SPECIAL_ITEM, effect = "ENDURANCE:2")
        val character = hero().copy(currentEndurance = 20, maxEndurance = 20)

        val withHelmet = Inventory.addItem(character, helmet)

        assertEquals(22, withHelmet.currentEndurance)
        assertEquals(
            22,
            io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveMaxEndurance(withHelmet),
        )
    }

    @Test
    fun perdereLElmoRiclampaLaResistenza() {
        val helmet = GameItem(name = "Helmet", type = ItemType.SPECIAL_ITEM, effect = "ENDURANCE:2")
        val withHelmet = Inventory.addItem(hero(), helmet)

        val without = Inventory.removeItem(withHelmet, "Helmet", 1)

        assertEquals(20, without.currentEndurance)
    }

    @Test
    fun equipaggiaSoloArmiPossedute() {
        val character = hero(weapon("Sword"))

        val equipped = Inventory.equipWeapon(character, "Sword")
        val ignored = Inventory.equipWeapon(character, "Axe")

        assertEquals("Sword", equipped.equippedWeapon)
        assertNull(ignored.equippedWeapon)
    }

    // canAdd (21/07/2026, Michele: "addItem non può funzionare in maniera
    // silenziosa" — serve sapere PRIMA se c'è spazio, per il pick
    // esplicito di ItemOffers): stessa regola di addCapped/addWeapon,
    // senza eseguire nulla.
    @Test
    fun canAddFalsoConDueArmiGiaPossedute() {
        val character = hero(weapon("Sword"), weapon("Axe"))

        assertEquals(false, Inventory.canAdd(character, weapon("Mace")))
    }

    @Test
    fun canAddVeroConUnoSoloSlotArmaLibero() {
        val character = hero(weapon("Sword"))

        assertEquals(true, Inventory.canAdd(character, weapon("Mace")))
    }

    @Test
    fun canAddFalsoConZainoPieno() {
        val character = hero(meal(8))

        assertEquals(false, Inventory.canAdd(character, meal(1)))
    }

    @Test
    fun canAddFalsoConCoroneAlTetto() {
        val character = hero(gold(50))

        assertEquals(false, Inventory.canAdd(character, gold(1)))
    }

    @Test
    fun canAddSempreVeroPerOggettiSpeciali() {
        val character = hero()
        val special = GameItem(name = "Map of Sommerlund", type = ItemType.SPECIAL_ITEM)

        assertEquals(true, Inventory.canAdd(character, special))
    }
}
