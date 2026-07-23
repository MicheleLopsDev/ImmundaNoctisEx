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

// Icona dell'eroe nella card di stato (24/07/2026, richiesta Michele:
// "facciamogli scegliere l'icona... devono essere tutti animali") — ID
// canonico qui (zero Android, zero dipendenza da drawable), la mappa
// verso il drawable vero e il nome localizzato vivono in `:app`
// (stesso schema di `WeaponType` -> `weaponTypeIcon()`). WOLF è
// l'unico con un asset pronto oggi: gli altri sono impalcatura per il
// giorno in cui arrivano le illustrazioni dal grafico, e nel
// frattempo degradano sul lupo (mai uno schermo rotto per un'icona
// mancante).
@Serializable
enum class HeroIcon {
    WOLF,
    FALCON,
    EAGLE,
    BEAR,
    FOX,
    RAVEN,
    OWL,
    LION,
    TIGER,
    PANTHER,
    LYNX,
    BOAR,
    STAG,
    SNAKE,
    DRAGON,
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
    val icon: HeroIcon = HeroIcon.WOLF,
)
