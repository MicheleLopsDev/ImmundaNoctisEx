package io.github.luposolitario.immundanoctisex.ui.creation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.Gender
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.model.WeaponType
import io.github.luposolitario.immundanoctisex.core.engine.dice.DiceRoller

// Stato della creazione personaggio: classe semplice (niente ViewModel
// androidx: nessuna dipendenza da lifecycle, testabile e previewabile).
// Il gating canProceed eredita la logica di SetupActivity v1.
class CreationState(private val dice: DiceRoller) {

    var gender: Gender by mutableStateOf(Gender.MALE)

    // Tiro canonico: CS = 10 + tiro, Resistenza = 20 + tiro, Corone = tiro.
    var combatSkill: Int by mutableStateOf(0)
        private set
    var endurance: Int by mutableStateOf(0)
        private set
    var gold: Int by mutableStateOf(0)
        private set

    val selectedDisciplines: SnapshotStateList<String> = emptyList<String>().toMutableStateList()

    var weaponSkillType: WeaponType? by mutableStateOf(null)

    var selectedWeapon: GameItem? by mutableStateOf(null)
        private set

    // Arti marziali: si parte SENZA armi (richiesta Michele). Alternativa
    // esclusiva alla scelta dell'arma.
    var fightsUnarmed: Boolean by mutableStateOf(false)
        private set

    val statsRolled: Boolean get() = combatSkill > 0

    val needsWeaponSkillChoice: Boolean
        get() = selectedDisciplines.contains("WEAPONSKILL")

    val canProceed: Boolean
        get() = statsRolled &&
            selectedDisciplines.size == 5 &&
            (selectedWeapon != null || fightsUnarmed) &&
            (!needsWeaponSkillChoice || weaponSkillType != null)

    fun selectWeapon(weapon: GameItem) {
        selectedWeapon = weapon
        fightsUnarmed = false
    }

    fun selectUnarmed() {
        fightsUnarmed = true
        selectedWeapon = null
    }

    fun rollStats() {
        combatSkill = 10 + dice.roll()
        endurance = 20 + dice.roll()
        gold = dice.roll()
    }

    fun toggleDiscipline(id: String) {
        when {
            selectedDisciplines.contains(id) -> {
                selectedDisciplines.remove(id)
                if (id == "WEAPONSKILL") weaponSkillType = null
            }
            selectedDisciplines.size < 5 -> selectedDisciplines.add(id)
        }
    }

    // "Scegli a caso" (decisione Michele: la scelta resta del giocatore,
    // il caso è un'opzione in più): stesso DiceRoller del resto del gioco.
    fun rollRandomWeaponSkill() {
        val types = WeaponType.entries.filter { it != WeaponType.UNARMED }
        weaponSkillType = types[dice.roll() % types.size]
    }

    // La fotografia iniziale della partita: eroe con equipaggiamento
    // scelto (o a mani nude), Corone tirate; scena = start del libro.
    fun buildSession(manifest: Manifest, difficulty: Difficulty, startSceneId: String): SessionData {
        val weapon = selectedWeapon
        val inventory = buildList {
            weapon?.let { add(it) }
            if (gold > 0) add(GameItem(name = "Gold Crowns", type = ItemType.GOLD, quantity = gold))
        }
        val hero = Character(
            role = CharacterRole.HERO,
            name = if (gender == Gender.MALE) "Lupo Solitario" else "Lupa Solitaria",
            gender = gender,
            baseCombatSkill = combatSkill,
            currentEndurance = endurance,
            maxEndurance = endurance,
            kaiDisciplines = selectedDisciplines.toList(),
            weaponSkillType = if (needsWeaponSkillChoice) weaponSkillType else null,
            inventory = inventory,
            equippedWeapon = weapon?.name,
        )
        return SessionData(
            saveFormatVersion = 1,
            packageId = manifest.id,
            packageVersion = manifest.version,
            difficulty = difficulty,
            currentSceneId = startSceneId,
            characters = listOf(hero),
            lastUpdate = System.currentTimeMillis(),
        )
    }
}
