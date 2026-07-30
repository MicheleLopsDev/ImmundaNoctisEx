package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import kotlinx.serialization.json.Json
import java.io.File

// Backup automatico su disco prima di ogni salvataggio (doc/EDITOR.md
// §10): rotazione a N livelli, bak1 il più recente prima dell'ultimo
// salvataggio, bakN il più vecchio. Nessuna integrazione Git (§10),
// nessun sistema di versioning completo — solo una rete di sicurezza
// contro un salvataggio andato storto.
private const val MAX_BACKUP = 5

internal fun ruotaBackup(file: File, maxBackup: Int = MAX_BACKUP) {
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
fun salvaLibro(file: File, contenutoJson: String) {
    ruotaBackup(file)
    file.writeText(contenutoJson)
}

// Comodo per non ripetere ovunque lo stesso encode+salvaLibro.
fun salvaManifest(file: File, manifest: Manifest) {
    salvaLibro(file, Json { prettyPrint = true }.encodeToString(Manifest.serializer(), manifest))
}
