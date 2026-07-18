package io.github.luposolitario.immundanoctisex.core.engine.choice

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.state.GameState

// Quali porte sono aperte in questa scena: sono REGOLE (UI.md §Zona
// scelte, REGOLE.md Blocco 6), non presentazione — quindi vivono qui e
// non nel modulo :app, e sono testabili da terminale.
object ChoiceAvailability {

    // Scelte normali mostrabili: condizioni soddisfatte e nessun tiro
    // richiesto (quelle a tiro passano dal Dado, sotto).
    // requiredFlag è un NOME di flag: soddisfatto se il flag è stato posto
    // a un valore diverso da "false" (un autore che scrive value="false"
    // intende negare la condizione, non soddisfarla).
    fun available(scene: Scene, state: GameState): List<Choice> =
        scene.choices.filter { choice ->
            val flagOk = choice.requiredFlag
                ?.let { state.flag(it)?.equals("false", ignoreCase = true) == false }
                ?: true
            val itemOk = choice.requiredItem
                ?.let { Inventory.countOf(state.hero, it) > 0 }
                ?: true
            flagOk && itemOk && !choice.requiresRoll()
        }

    // Scelte-disciplina: solo quelle che il giocatore possiede davvero
    // (l'altra metà del canUseDiscipline di v1 — che la scena la offra è
    // implicito, sono le scelte DI questa scena).
    fun disciplineChoices(scene: Scene, state: GameState): List<DisciplineChoice> =
        scene.disciplineChoices.filter { state.hero.kaiDisciplines.contains(it.disciplineId) }

    // Tabella dei numeri casuali: se la scena ha scelte con intervallo di
    // tiro, il giocatore non sceglie — tira (REGOLE.md Blocco 6).
    fun rollChoices(scene: Scene): List<Choice> = scene.choices.filter { it.requiresRoll() }

    // La porta che il tiro apre. Nessun intervallo copre il numero
    // (pacchetto scritto male): null, e chi chiama degrada senza bloccarsi.
    fun forRoll(scene: Scene, roll: Int): Choice? =
        rollChoices(scene).firstOrNull { roll in it.minRoll!!..it.maxRoll!! }

    private fun Choice.requiresRoll(): Boolean = minRoll != null && maxRoll != null
}
