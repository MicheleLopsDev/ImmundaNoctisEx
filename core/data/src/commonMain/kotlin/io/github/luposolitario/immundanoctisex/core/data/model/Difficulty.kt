package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// Meta-regola sul salvataggio (STATO.md Blocco 2), non inflazione di
// statistiche: scelta a inizio avventura, immutabile per la partita.
// NORMAL 2 checkpoint, HARD 1, IRON 0 (morte = sessione cancellata).
@Serializable
enum class Difficulty {
    NORMAL,
    HARD,
    IRON,
}
