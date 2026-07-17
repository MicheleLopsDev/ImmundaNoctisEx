package io.github.luposolitario.immundanoctisex.core.engine.stats

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.StatModifier
import io.github.luposolitario.immundanoctisex.core.data.model.StatType
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
