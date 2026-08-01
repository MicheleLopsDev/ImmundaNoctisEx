package io.github.luposolitario.immundanoctisex.core.engine.stats

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.StatModifier
import io.github.luposolitario.immundanoctisex.core.data.model.StatType
import io.github.luposolitario.immundanoctisex.core.data.model.WeaponType
import kotlin.test.Test
import kotlin.test.assertEquals

class EffectiveStatsTest {

    private fun hero(vararg modifiers: StatModifier) = Character(
        role = CharacterRole.HERO,
        name = "Eroe di prova",
        baseCombatSkill = 15,
        currentEndurance = 20,
        maxEndurance = 20,
        activeModifiers = modifiers.toList(),
    )

    private fun swordsman(
        specialization: WeaponType?,
        equipped: String?,
        disciplines: List<String> = listOf("WEAPONSKILL"),
    ) = hero().copy(
        kaiDisciplines = disciplines,
        weaponSkillType = specialization,
        equippedWeapon = equipped,
        inventory = listOf(
            GameItem(name = "Sword", type = ItemType.WEAPON, weaponType = WeaponType.SWORD),
            GameItem(name = "Axe", type = ItemType.WEAPON, weaponType = WeaponType.AXE),
        ),
    )

    @Test
    fun senzaModificatoriERisultatoBase() {
        assertEquals(15, effectiveCombatSkill(hero()))
    }

    @Test
    fun sommaIModificatoriAttiviSullaCombattivita() {
        val character = hero(
            StatModifier(StatType.COMBAT_SKILL, amount = 2, sourceType = "MINDBLAST"),
            StatModifier(StatType.COMBAT_SKILL, amount = -3, sourceType = "ferita"),
        )

        assertEquals(14, effectiveCombatSkill(character))
    }

    @Test
    fun ignoraIModificatoriSullaResistenza() {
        val character = hero(StatModifier(StatType.ENDURANCE, amount = 5, sourceType = "pozione"))

        assertEquals(15, effectiveCombatSkill(character))
    }

    @Test
    fun weaponskillBonusConArmaDellaSpecializzazione() {
        assertEquals(17, effectiveCombatSkill(swordsman(WeaponType.SWORD, equipped = "Sword")))
    }

    @Test
    fun weaponskillNienteBonusConArmaSbagliata() {
        assertEquals(15, effectiveCombatSkill(swordsman(WeaponType.SWORD, equipped = "Axe")))
    }

    @Test
    fun weaponskillUnarmedBonusSoloSenzaArma() {
        assertEquals(17, effectiveCombatSkill(swordsman(WeaponType.UNARMED, equipped = null)))
        assertEquals(15, effectiveCombatSkill(swordsman(WeaponType.UNARMED, equipped = "Sword")))
    }

    @Test
    fun weaponskillSenzaDisciplinaNonValeNulla() {
        val character = swordsman(WeaponType.SWORD, equipped = "Sword", disciplines = emptyList())

        assertEquals(15, effectiveCombatSkill(character))
    }

    // Scudo (01/08/2026): +2 Combattività finché è nell'inventario, come
    // l'Elmo fa con la Resistenza. Stesso formato dichiarativo, stat diversa.
    @Test
    fun scudoAggiungeIlSuoBonusAllaCombattivita() {
        val character = hero().copy(
            inventory = listOf(
                GameItem(name = "Shield", type = ItemType.SPECIAL_ITEM, effect = "COMBAT_SKILL:2"),
            ),
        )

        assertEquals(17, effectiveCombatSkill(character))
    }

    @Test
    fun ilBonusDegliOggettiSiSommaAQuelloDellaSpecializzazione() {
        val character = swordsman(WeaponType.SWORD, equipped = "Sword").let {
            it.copy(
                inventory = it.inventory +
                    GameItem(name = "Shield", type = ItemType.SPECIAL_ITEM, effect = "COMBAT_SKILL:2"),
            )
        }

        assertEquals(19, effectiveCombatSkill(character))
    }

    @Test
    fun unOggettoSenzaEffettoNonCambiaLaCombattivita() {
        val character = hero().copy(
            inventory = listOf(
                GameItem(name = "Rope", type = ItemType.SPECIAL_ITEM),
                GameItem(name = "Helmet", type = ItemType.SPECIAL_ITEM, effect = "ENDURANCE:2"),
            ),
        )

        assertEquals(15, effectiveCombatSkill(character))
    }

    @Test
    fun ilBonusValeUnaVoltaPerCopiaPosseduta() {
        val item = GameItem(name = "Shield", type = ItemType.SPECIAL_ITEM, effect = "COMBAT_SKILL:2", quantity = 2)

        assertEquals(4, itemCombatSkillBonus(item))
    }

    @Test
    fun enduranceSenzaModificatoriERisultatoCorrente() {
        assertEquals(20, effectiveEndurance(hero()))
    }

    @Test
    fun enduranceClampataAlMassimoDelPersonaggio() {
        val character = hero(StatModifier(StatType.ENDURANCE, amount = 7, sourceType = "pozione"))

        assertEquals(20, effectiveEndurance(character))
    }

    @Test
    fun enduranceClampataAZero() {
        val character = hero(StatModifier(StatType.ENDURANCE, amount = -25, sourceType = "veleno"))

        assertEquals(0, effectiveEndurance(character))
    }

    @Test
    fun ciascunaFunzioneIgnoraIModificatoriDellAltraStat() {
        val character = hero(
            StatModifier(StatType.ENDURANCE, amount = -4, sourceType = "ferita"),
            StatModifier(StatType.COMBAT_SKILL, amount = 2, sourceType = "MINDBLAST"),
        )

        assertEquals(17, effectiveCombatSkill(character))
        assertEquals(16, effectiveEndurance(character))
    }
}
