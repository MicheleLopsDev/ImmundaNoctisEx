package io.github.luposolitario.immundanoctisex.core.data.session

import io.github.luposolitario.immundanoctisex.core.data.model.SessionData

// Porta di persistenza della sessione (STATO.md §1.2-1.3, stesso pattern di
// PackageSource): l'app inietta l'implementazione su app-storage, i test una
// su directory temporanea. Chi la usa non conosce Context né percorsi.
interface SessionStore {

    // Auto-save: sovrascrive session_<packageId>.json in modo ATOMICO.
    fun saveSession(session: SessionData)

    fun loadSession(packageId: String): SessionData?

    // Tutte le sessioni salvate (per la Home: "quale salvataggio continuo?").
    fun listSessions(): List<SessionData>

    // Checkpoint (STATO.md Blocco 2): fotografia su file separato, scritta
    // UNA volta e mai sovrascrivibile — false se lo slot esiste già. Il
    // budget per difficoltà lo fa rispettare chi chiama, non lo store.
    fun saveCheckpoint(session: SessionData, slot: Int): Boolean

    fun loadCheckpoint(packageId: String, slot: Int): SessionData?

    // Un checkpoint SI CONSUMA quando lo si usa (decisione Michele
    // 20/07/2026): ricaricarlo lo brucia, così le vite sono davvero
    // finite. Prima era ricaricabile all'infinito e bastavano due
    // piazzamenti per rendere l'avventura innocua.
    fun deleteCheckpoint(packageId: String, slot: Int)

    // Cancella sessione E checkpoint del pacchetto: morte in IRON, oppure
    // "nuova avventura" che riparte da capo (i checkpoint di una partita
    // finita non devono sopravvivere alla successiva).
    fun deleteAdventure(packageId: String)
}
