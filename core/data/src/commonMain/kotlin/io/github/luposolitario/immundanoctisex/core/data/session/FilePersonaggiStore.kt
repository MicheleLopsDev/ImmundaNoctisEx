package io.github.luposolitario.immundanoctisex.core.data.session

import io.github.luposolitario.immundanoctisex.core.data.model.PersonaggioSalvato
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

// I personaggi che possono passare da un libro all'altro: un JSON per
// personaggio, in una cartella tutta loro (03/08/2026).
//
// Stesso stampo di `FileSessionStore`, comprese le due regole che qui
// contano ancora di più — un personaggio sopravvive a tutte le partite:
//  - **scrittura atomica**: si scrive su un temporaneo e si rinomina,
//    così un crash a metà non lascia mai un personaggio corrotto;
//  - **lettura che degrada**: un file illeggibile sparisce dall'elenco
//    invece di far fallire l'apertura di tutti gli altri.
//
// Nessuna interfaccia sopra: il progetto ne dichiara quattro e solo
// quattro, e qui non c'è una seconda implementazione da servire — nei
// test basta una cartella temporanea.
class FilePersonaggiStore(private val directory: File) {

    private val json = Json {
        ignoreUnknownKeys = true // un campo nuovo non rompe i file già scritti
        encodeDefaults = true
    }

    init {
        directory.mkdirs()
    }

    fun salva(personaggio: PersonaggioSalvato) {
        val destinazione = fileDi(personaggio.id)
        val temporaneo = File(directory, destinazione.name + ".tmp")
        temporaneo.writeText(json.encodeToString(PersonaggioSalvato.serializer(), personaggio))
        Files.move(
            temporaneo.toPath(),
            destinazione.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    }

    fun carica(id: String): PersonaggioSalvato? = leggiOppureNull(fileDi(id))

    // Il più recente per primo: chi ha appena finito un libro è quello
    // che con più probabilità si vuole riprendere.
    fun elenco(): List<PersonaggioSalvato> =
        directory.listFiles { file -> file.name.startsWith(PREFISSO) && file.name.endsWith(ESTENSIONE) }
            .orEmpty()
            .mapNotNull { leggiOppureNull(it) }
            .sortedByDescending { it.libriCompletati.lastOrNull()?.completatoIl ?: it.creatoIl }

    // Creandone dieci per prova restano dieci file: si devono poter
    // buttare dall'elenco.
    fun elimina(id: String) {
        fileDi(id).delete()
    }

    private fun fileDi(id: String) = File(directory, "$PREFISSO$id$ESTENSIONE")

    private fun leggiOppureNull(file: File): PersonaggioSalvato? {
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString(PersonaggioSalvato.serializer(), file.readText())
        }.getOrNull()
    }

    private companion object {
        const val PREFISSO = "personaggio_"
        const val ESTENSIONE = ".json"
    }
}
