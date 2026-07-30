package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import kotlinx.serialization.json.Json
import java.io.File

// Backup automatico su disco prima di ogni salvataggio (doc/EDITOR.md
// §10): rotazione a N livelli, bak1 il più recente prima dell'ultimo
// salvataggio, bakN il più vecchio. Nessuna integrazione Git (§10),
// nessun sistema di versioning completo — solo una rete di sicurezza
// contro un salvataggio andato storto.
//
// Configurabile 3-9 (§17.5, Impostazioni): il valore di default resta
// questo se l'autore non lo cambia mai.
const val MAX_BACKUP_DEFAULT = 5
const val MAX_BACKUP_MIN = 3
const val MAX_BACKUP_MAX = 9

internal fun ruotaBackup(file: File, maxBackup: Int = MAX_BACKUP_DEFAULT) {
    if (!file.exists()) return
    for (i in maxBackup downTo 2) {
        val precedente = File(file.parentFile, "${file.name}.bak${i - 1}")
        val corrente = File(file.parentFile, "${file.name}.bak$i")
        if (precedente.exists()) precedente.copyTo(corrente, overwrite = true)
    }
    val bak1 = File(file.parentFile, "${file.name}.bak1")
    file.copyTo(bak1, overwrite = true)
}

// Scrive il libro sul file originale, ruotando prima i backup — mai una
// sovrascrittura "a perdere" del contenuto precedente.
fun salvaLibro(file: File, contenutoJson: String, maxBackup: Int = MAX_BACKUP_DEFAULT) {
    ruotaBackup(file, maxBackup)
    file.writeText(contenutoJson)
}

// Comodo per non ripetere ovunque lo stesso encode+salvaLibro.
fun salvaManifest(file: File, manifest: Manifest, maxBackup: Int = MAX_BACKUP_DEFAULT) {
    salvaLibro(file, Json { prettyPrint = true }.encodeToString(Manifest.serializer(), manifest), maxBackup)
}

// "↩ Annulla" (§17.5, Michele: "l'annulla ti riporta al backup -1"):
// riporta il file all'ultimo backup, cioè lo stato immediatamente
// precedente all'ultimo salvataggio. Distruttivo per lo stato corrente
// (non salvato o appena scritto) — chi chiama deve confermare con
// l'autore prima, questa funzione esegue e basta.
fun esisteBackup(file: File): Boolean = File(file.parentFile, "${file.name}.bak1").exists()

fun ripristinaUltimoBackup(file: File): Boolean {
    val bak1 = File(file.parentFile, "${file.name}.bak1")
    if (!bak1.exists()) return false
    bak1.copyTo(file, overwrite = true)
    return true
}
