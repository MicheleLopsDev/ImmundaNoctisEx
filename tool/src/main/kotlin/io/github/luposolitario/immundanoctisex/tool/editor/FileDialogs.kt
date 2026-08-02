package io.github.luposolitario.immundanoctisex.tool.editor

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

// java.awt.FileDialog invece di un file picker Compose: nessuna libreria
// aggiuntiva, usa le finestre di selezione native di Windows. Condivise
// tra la schermata di avvio (apri libro), la mappa (salva con nome) e
// la creazione di un libro nuovo (primo salvataggio obbligatorio).

fun scegliFileJsonDaAprire(): File? {
    val dialog = FileDialog(null as Frame?, "Scegli un libro JSON", FileDialog.LOAD)
    dialog.file = "*.json"
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val fileName = dialog.file ?: return null
    return File(directory, fileName)
}

fun scegliPercorsoSalvataggio(nomeSuggerito: String): File? {
    val dialog = FileDialog(null as Frame?, "Salva libro come...", FileDialog.SAVE)
    dialog.file = nomeSuggerito
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val fileName = dialog.file ?: return null
    return File(directory, fileName)
}

// Il file del modello (02/08/2026): lo stesso `.litertlm` che il client
// scarica da HuggingFace, ma preso dal disco — 3,7 GB per il 4B, non è
// roba che l'editor possa portarsi dentro.
fun scegliFileModello(): File? {
    val dialog = FileDialog(null as Frame?, "Scegli il modello (.litertlm)", FileDialog.LOAD)
    dialog.file = "*.litertlm"
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val fileName = dialog.file ?: return null
    return File(directory, fileName)
}

// Dove mettere i modelli scaricati (02/08/2026). java.awt.FileDialog non
// sa selezionare cartelle su Windows: si usa JFileChooser (anch'esso
// nella JDK) in modalità sole-directory, l'unico caso in cui l'editor
// si scosta dai dialoghi nativi.
fun scegliCartellaModelli(): File? {
    val chooser = javax.swing.JFileChooser().apply {
        dialogTitle = "Cartella dove scaricare i modelli"
        fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
    }
    return if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile
    } else {
        null
    }
}

// §19.8 (Michele: "esportare la mappa come immagine"): stesso dialogo
// nativo di salvataggio, titolo diverso — l'estensione .png viene
// garantita a valle (vedi MapExport.kt), non qui.
fun scegliPercorsoEsportazioneImmagine(nomeSuggerito: String): File? {
    val dialog = FileDialog(null as Frame?, "Esporta mappa come immagine...", FileDialog.SAVE)
    dialog.file = nomeSuggerito
    dialog.isVisible = true
    val directory = dialog.directory ?: return null
    val fileName = dialog.file ?: return null
    return File(directory, fileName)
}
