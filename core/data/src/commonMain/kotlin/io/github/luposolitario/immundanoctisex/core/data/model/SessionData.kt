package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// Fotografia completa di una partita (STATO.md Blocco 1). Il DM non è un
// personaggio e non esiste qui; il nemico di combattimento non si salva
// (transiente, idratato dal blocco combat della scena). flags/variables
// sono tipizzati e unificati a livello di sessione (mai più gameFlags
// per-personaggio né Map<String, Any>).
@Serializable
data class SessionData(
    val saveFormatVersion: Int,
    val packageId: String,
    val packageVersion: String,
    val difficulty: Difficulty,
    val currentSceneId: String,
    val characters: List<Character> = emptyList(),
    val journey: List<JourneyEntry> = emptyList(),
    val flags: Map<String, String> = emptyMap(),
    val variables: Map<String, Int> = emptyMap(),
    val checkpointsUsed: Int = 0,
    val lastUpdate: Long,
    // Quale personaggio trasportabile sta giocando questa partita
    // (03/08/2026): serve a fine libro per sapere QUALE file aggiornare
    // con i libri completati. In coda e con default, così i salvataggi
    // già su disco continuano a caricarsi — restano semplicemente senza
    // personaggio associato, e a fine libro non aggiornano nulla.
    val personaggioId: String? = null,
)
