package io.github.luposolitario.immundanoctisex.core.data.session

import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

// Implementazione su file system (STATO.md §1.2): un JSON per pacchetto.
// SCRITTURA ATOMICA obbligatoria (vincolo non negoziabile): si scrive su un
// file temporaneo nella stessa directory e poi si fa rename atomico — un
// crash a metà scrittura non corrompe mai il salvataggio precedente.
class FileSessionStore(private val directory: File) : SessionStore {

    private val json = Json {
        ignoreUnknownKeys = true // un campo nuovo non rompe i salvataggi vecchi
        encodeDefaults = true
    }

    init {
        directory.mkdirs()
    }

    override fun saveSession(session: SessionData) {
        writeAtomically(sessionFile(session.packageId), session)
    }

    override fun loadSession(packageId: String): SessionData? =
        readOrNull(sessionFile(packageId))

    override fun listSessions(): List<SessionData> =
        directory.listFiles { file -> file.name.startsWith(SESSION_PREFIX) && file.name.endsWith(EXT) }
            .orEmpty()
            .mapNotNull { readOrNull(it) }
            .sortedByDescending { it.lastUpdate }

    override fun saveCheckpoint(session: SessionData, slot: Int): Boolean {
        val file = checkpointFile(session.packageId, slot)
        if (file.exists()) return false // scritto una volta, mai sovrascrivibile
        writeAtomically(file, session)
        return true
    }

    override fun loadCheckpoint(packageId: String, slot: Int): SessionData? =
        readOrNull(checkpointFile(packageId, slot))

    // Il checkpoint si CONSUMA all'uso: sparisce il file, così non è più
    // né ricaricabile né elencabile.
    override fun deleteCheckpoint(packageId: String, slot: Int) {
        checkpointFile(packageId, slot).delete()
    }

    override fun deleteAdventure(packageId: String) {
        sessionFile(packageId).delete()
        directory.listFiles { file ->
            file.name.startsWith("$CHECKPOINT_PREFIX${packageId}_") && file.name.endsWith(EXT)
        }.orEmpty().forEach { it.delete() }
    }

    private fun writeAtomically(target: File, session: SessionData) {
        val temp = File(directory, target.name + ".tmp")
        temp.writeText(json.encodeToString(SessionData.serializer(), session))
        Files.move(
            temp.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    }

    // Un salvataggio illeggibile (corrotto, versione incompatibile) degrada
    // a "nessun salvataggio": il gioco non si blocca mai.
    private fun readOrNull(file: File): SessionData? {
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString(SessionData.serializer(), file.readText())
        }.getOrNull()
    }

    private fun sessionFile(packageId: String) = File(directory, "$SESSION_PREFIX$packageId$EXT")

    private fun checkpointFile(packageId: String, slot: Int) =
        File(directory, "$CHECKPOINT_PREFIX${packageId}_$slot$EXT")

    private companion object {
        const val SESSION_PREFIX = "session_"
        const val CHECKPOINT_PREFIX = "checkpoint_"
        const val EXT = ".json"
    }
}
