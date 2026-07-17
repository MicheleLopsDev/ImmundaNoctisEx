package io.github.luposolitario.immundanoctisex.core.engine.stats

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.StatType

// Si serializzano i fatti (base, modificatori, arma equipaggiata), i bonus
// si calcolano qui: unica funzione dell'engine per la Combattività
// effettiva (STATO.md §1.1, REGOLE.md §1.2). Mai persistita, mai
// ricalcolata altrove (difetto di v1 da non ripetere: LoneWolfRules
// sommava i modificatori per conto suo).
//
// TODO(WEAPONSKILL): manca il bonus +2 quando character.weaponSkillType
// coincide con l'arma impugnata (equippedWeapon), o quando la
// specializzazione è UNARMED e non c'è arma equipaggiata (REGOLE.md §4.2).
// In attesa dell'enum WeaponType (task [MICHELE], STATO.md §4.3): senza un
// tipo canonico non si può confrontare weaponSkillType con l'arma
// impugnata in modo affidabile.
fun effectiveCombatSkill(character: Character): Int {
    val modifiersBonus = character.activeModifiers
        .filter { it.stat == StatType.COMBAT_SKILL }
        .sumOf { it.amount }

    return character.baseCombatSkill + modifiersBonus
}

// Resistenza effettiva: currentEndurance + modificatori attivi su
// ENDURANCE, clampata tra 0 e maxEndurance (mai sopra il massimo del
// personaggio, mai sotto zero — la morte a Resistenza 0 la valuta
// l'engine, non questa funzione).
fun effectiveEndurance(character: Character): Int {
    val modifiersBonus = character.activeModifiers
        .filter { it.stat == StatType.ENDURANCE }
        .sumOf { it.amount }

    return (character.currentEndurance + modifiersBonus)
        .coerceIn(0, character.maxEndurance)
}
