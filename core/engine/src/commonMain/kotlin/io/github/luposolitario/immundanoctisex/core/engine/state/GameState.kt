package io.github.luposolitario.immundanoctisex.core.engine.state

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.JourneyEntry
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData

// Unica fonte di verità della partita in corso (ARCHITETTURA.md): mai stato
// sparso tra manager come in v1. Internamente tiene una SessionData
// immutabile e la evolve per copia; il salvataggio è la fotografia
// restituita da snapshot() — chi persiste (modulo data/app) non conosce
// questa classe, riceve solo SessionData.
class GameState(initial: SessionData) {

    var session: SessionData = initial
        private set

    val currentSceneId: String get() = session.currentSceneId

    // L'eroe è unico per costruzione (SetupActivity ne crea uno solo);
    // qui si pretende che esista: una sessione senza eroe è un pacchetto
    // rotto, intercettato a monte dai validatori.
    val hero: Character
        get() = session.characters.first { it.role == CharacterRole.HERO }

    fun updateHero(transform: (Character) -> Character) {
        session = session.copy(
            characters = session.characters.map {
                if (it.role == CharacterRole.HERO) transform(it) else it
            },
        )
    }

    fun setFlag(name: String, value: String) {
        session = session.copy(flags = session.flags + (name to value))
    }

    fun flag(name: String): String? = session.flags[name]

    fun setVariable(name: String, value: Int) {
        session = session.copy(variables = session.variables + (name to value))
    }

    fun updateVariable(name: String, delta: Int) {
        setVariable(name, variable(name) + delta)
    }

    fun variable(name: String): Int = session.variables[name] ?: 0

    fun moveTo(sceneId: String) {
        session = session.copy(currentSceneId = sceneId)
    }

    fun addJourneyEntry(entry: JourneyEntry) {
        session = session.copy(journey = session.journey + entry)
    }

    // Contabilità del budget checkpoint (STATO.md Blocco 2): il numero
    // di piazzamenti spesi è un fatto della sessione.
    fun incrementCheckpointsUsed() {
        session = session.copy(checkpointsUsed = session.checkpointsUsed + 1)
    }

    fun snapshot(): SessionData = session
}
