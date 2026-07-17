package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class CharacterRole {
    HERO,
    COMPANION,
    ENEMY,
    NPC,
}

// Genere del personaggio (UI.md §Convenzioni): tre clienti — ritratto
// lupo/lupa, voce TTS per genere, placeholder {player_gender} nel prompt.
@Serializable
enum class Gender {
    MALE,
    FEMALE,
}

// Character unico per tutti i ruoli (REGOLE.md §1.5): eroe, compagni, nemici
// e futuri duelli eroe-contro-eroe condividono lo stesso tipo, mai un id
// magico "hero". Si serializzano i fatti: kaiDisciplines sono ID canonici
// (mai nomi display), la CS/Resistenza effettiva la calcola l'engine
// (base + activeModifiers + WEAPONSKILL + eventuale MINDBLAST di
// combattimento), mai persistita. weaponSkillType è la specializzazione
// WEAPONSKILL scelta alla creazione (un tipo d'arma oppure UNARMED),
// valorizzata solo se il personaggio possiede la disciplina.
@Serializable
data class Character(
    val role: CharacterRole,
    val name: String,
    val gender: Gender = Gender.MALE,
    val baseCombatSkill: Int,
    val currentEndurance: Int,
    val maxEndurance: Int,
    val kaiDisciplines: List<String> = emptyList(),
    val weaponSkillType: WeaponType? = null,
    val inventory: List<GameItem> = emptyList(),
    val equippedWeapon: String? = null,
    val activeModifiers: List<StatModifier> = emptyList(),
)
