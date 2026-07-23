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
import io.github.luposolitario.immundanoctisex.core.data.model.HeroIcon
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.model.WeaponType
import io.github.luposolitario.immundanoctisex.core.engine.dice.DiceRoller
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.mechanics.MechanicsExecutor
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState
import io.github.luposolitario.immundanoctisex.core.engine.transition.TransitionEngine

// Stato della creazione personaggio: classe semplice (niente ViewModel
// androidx: nessuna dipendenza da lifecycle, testabile e previewabile).
// Il gating canProceed eredita la logica di SetupActivity v1.
class CreationState(private val dice: DiceRoller) {

    var gender: Gender by mutableStateOf(Gender.MALE)

    // Nome personalizzabile (24/07/2026, richiesta Michele): vuoto per
    // default, si risolve in "Lupo"/"Lupa" solo alla creazione della
    // sessione (buildSession) — un segnaposto ANONIMO apposta, non il
    // nome canonico "Lupo Solitario"/"Lupa Solitaria" del protagonista
    // dei libri, che sembrerebbe già un nome scelto da qualcuno.
    var heroName: String by mutableStateOf("")

    // Icona dell'eroe (24/07/2026, richiesta Michele: "facciamogli
    // scegliere l'icona... devono essere tutti animali"): WOLF di
    // default, coerente col nome di default (Lupo/Lupa).
    var heroIcon: HeroIcon by mutableStateOf(HeroIcon.WOLF)

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

    // UN oggetto speciale a scelta (come v1/canone): Mappa, Elmo o Gilet.
    var selectedSpecialItem: SpecialItemUi? by mutableStateOf(null)

    val statsRolled: Boolean get() = combatSkill > 0

    val needsWeaponSkillChoice: Boolean
        get() = selectedDisciplines.contains("WEAPONSKILL")

    val canProceed: Boolean
        get() = statsRolled &&
            selectedDisciplines.size == 5 &&
            (selectedWeapon != null || fightsUnarmed) &&
            selectedSpecialItem != null &&
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
    // scelto (o a mani nude), oggetto speciale, comuni e Corone tirate;
    // scena = start del libro. Gli oggetti entrano tramite l'engine
    // (Inventory.addItem): l'Elmo/Gilet applica il suo bonus Resistenza
    // esattamente come lo farà ogni addItem futuro del libro.
    fun buildSession(manifest: Manifest, difficulty: Difficulty, startSceneId: String): SessionData {
        val weapon = selectedWeapon
        var hero = Character(
            role = CharacterRole.HERO,
            name = heroName.trim().ifBlank { if (gender == Gender.MALE) "Lupo" else "Lupa" },
            gender = gender,
            baseCombatSkill = combatSkill,
            currentEndurance = endurance,
            maxEndurance = endurance,
            kaiDisciplines = selectedDisciplines.toList(),
            weaponSkillType = if (needsWeaponSkillChoice) weaponSkillType else null,
            icon = heroIcon,
        )
        val startingItems = buildList {
            weapon?.let { add(it) }
            selectedSpecialItem?.let { add(it.item) }
            addAll(INITIAL_COMMON_ITEMS)
            if (gold > 0) add(GameItem(name = "Gold Crowns", type = ItemType.GOLD, quantity = gold))
        }
        startingItems.forEach { hero = Inventory.addItem(hero, it) }
        weapon?.let { hero = Inventory.equipWeapon(hero, it.name) }
        val rawSession = SessionData(
            saveFormatVersion = 1,
            packageId = manifest.id,
            packageVersion = manifest.version,
            difficulty = difficulty,
            currentSceneId = startSceneId,
            characters = listOf(hero),
            lastUpdate = System.currentTimeMillis(),
        )
        // BUG (21/07/2026, Michele: "non mi ha aggiunto nulla all'inventario"
        // — un libro di test dava oggetti via gameMechanics sulla scena
        // START): la sessione nasceva già "dentro" quella scena
        // (currentSceneId = startSceneId), senza mai passare da
        // TransitionEngine.transitionTo — l'UNICO punto che esegue
        // gameMechanics/HEALING/regole globali (REGOLE.md §2.3). Un
        // libro non poteva MAI dare oggetti extra o applicare regole
        // sulla propria scena d'apertura, in nessun caso, non solo in
        // questo test. Si fa girare la stessa pipeline UNA VOLTA qui,
        // alla nascita della sessione — non in AdventureState (quello
        // gira anche alla ripresa di un checkpoint, dove rieseguire i
        // gameMechanics darebbe gli oggetti una seconda volta).
        val gameState = GameState(rawSession)
        TransitionEngine(manifest, MechanicsExecutor(dice)).transitionTo(gameState, startSceneId)
        return gameState.snapshot()
    }
}
