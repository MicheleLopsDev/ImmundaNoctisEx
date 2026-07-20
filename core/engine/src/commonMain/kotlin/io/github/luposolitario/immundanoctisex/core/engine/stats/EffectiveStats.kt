package io.github.luposolitario.immundanoctisex.core.engine.stats

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.StatType
import io.github.luposolitario.immundanoctisex.core.data.model.WeaponType

private const val WEAPONSKILL_BONUS = 2

// Si serializzano i fatti (base, modificatori, arma equipaggiata), i bonus
// si calcolano qui: unica funzione dell'engine per la Combattività
// effettiva (STATO.md §1.1, REGOLE.md §1.2). Mai persistita, mai
// ricalcolata altrove (difetto di v1 da non ripetere: LoneWolfRules
// sommava i modificatori per conto suo).
fun effectiveCombatSkill(character: Character): Int {
    val modifiersBonus = character.activeModifiers
        .filter { it.stat == StatType.COMBAT_SKILL }
        .sumOf { it.amount }

    return character.baseCombatSkill + modifiersBonus + weaponskillBonus(character)
}

// Bonus Resistenza degli oggetti POSSEDUTI (effetto dichiarativo
// "ENDURANCE:n", es. Elmo +2, Gilet di maglia +4 — canone libro 1): vale
// finché l'oggetto è nell'inventario, mai persistito nelle stat.
fun itemEnduranceBonus(item: GameItem): Int =
    item.effect?.takeIf { it.startsWith("ENDURANCE:") }
        ?.substringAfter(":")?.toIntOrNull()?.times(item.quantity) ?: 0

// Il massimo EFFETTIVO di Resistenza: base del personaggio + bonus degli
// oggetti posseduti. È il tetto usato da cure, HEALING passiva e clamp.
fun effectiveMaxEndurance(character: Character): Int =
    character.maxEndurance + character.inventory.sumOf { itemEnduranceBonus(it) }

// WEAPONSKILL (REGOLE.md §4.2): +2 se la specializzazione coincide con il
// tipo dell'arma impugnata; con specializzazione UNARMED, +2 se si combatte
// SENZA arma. La specializzazione senza la disciplina non vale nulla.
// Pubblica (non più privata, 20/07/2026): la Scheda deve poter SPIEGARE
// il numero di effectiveCombatSkill scomponendolo — leggere questa unica
// fonte di verità invece di duplicare l'if/else lato UI (l'errore di v1,
// dove LoneWolfRules sommava i modificatori per conto suo).
fun weaponskillBonus(character: Character): Int {
    val specialization = character.weaponSkillType ?: return 0
    if (!character.kaiDisciplines.contains("WEAPONSKILL")) return 0

    val equippedType = character.equippedWeapon?.let { name ->
        character.inventory.firstOrNull {
            it.type == ItemType.WEAPON && it.name.equals(name, ignoreCase = true)
        }?.weaponType
    }

    return when {
        specialization == WeaponType.UNARMED && equippedType == null -> WEAPONSKILL_BONUS
        specialization != WeaponType.UNARMED && equippedType == specialization -> WEAPONSKILL_BONUS
        else -> 0
    }
}

// Resistenza effettiva: currentEndurance + modificatori attivi su
// ENDURANCE, clampata tra 0 e il massimo EFFETTIVO (base + bonus oggetti
// come l'Elmo — mai sopra il tetto, mai sotto zero; la morte a Resistenza
// 0 la valuta l'engine, non questa funzione).
fun effectiveEndurance(character: Character): Int {
    val modifiersBonus = character.activeModifiers
        .filter { it.stat == StatType.ENDURANCE }
        .sumOf { it.amount }

    return (character.currentEndurance + modifiersBonus)
        .coerceIn(0, effectiveMaxEndurance(character))
}
