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

// Budget di checkpoint per difficoltà (STATO.md Blocco 2) — funzione
// condivisa (24/07/2026) invece di duplicare lo stesso `when` in
// AdventureState e in SetupRoute (che deve sapere quanti slot
// controllare per offrire "carica l'ultimo checkpoint" al resume).
fun Difficulty.checkpointBudget(): Int = when (this) {
    Difficulty.NORMAL -> 2
    Difficulty.HARD -> 1
    Difficulty.IRON -> 0
}
