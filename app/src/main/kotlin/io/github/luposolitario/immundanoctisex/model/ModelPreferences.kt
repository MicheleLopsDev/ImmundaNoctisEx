package io.github.luposolitario.immundanoctisex.model

import android.content.Context
import android.content.SharedPreferences
import java.io.File

// Quale modello si usa, dove sta il file, e il token Hugging Face per i
// repo riservati (in v1 il token viveva dentro ThemePreferences: un
// segreto in casa d'altri, qui ha il suo posto).
// Il token NON si stampa mai nei log.
class ModelPreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var selectedModelId: String
        get() = prefs.getString(KEY_SELECTED_MODEL, null) ?: ModelCatalog.default.id
        set(value) = prefs.edit().putString(KEY_SELECTED_MODEL, value).apply()

    val selectedModel: DownloadableModel
        get() = ModelCatalog.byId(selectedModelId) ?: ModelCatalog.default

    // Token personale dell'utente: si salva solo se lo inserisce lui.
    var huggingFaceToken: String?
        get() = prefs.getString(KEY_HF_TOKEN, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().apply {
            if (value.isNullOrBlank()) remove(KEY_HF_TOKEN) else putString(KEY_HF_TOKEN, value.trim())
        }.apply()

    // I modelli vivono in app-storage: niente permessi, e si cancellano
    // con l'app (sono GB, non devono restare orfani sul telefono).
    fun modelsDirectory(): File = File(context.filesDir, "models").apply { mkdirs() }

    fun fileFor(model: DownloadableModel): File = File(modelsDirectory(), model.fileName)

    // Scaricato davvero = il file esiste ed è della dimensione attesa.
    // Un file troncato (download interrotto, pagina d'errore salvata per
    // sbaglio) NON conta come presente.
    fun isDownloaded(model: DownloadableModel): Boolean {
        val file = fileFor(model)
        if (!file.exists()) return false
        return model.sizeBytes <= 0L || file.length() == model.sizeBytes
    }

    fun deleteModel(model: DownloadableModel): Boolean = fileFor(model).delete()

    private companion object {
        const val PREFS_NAME = "model_preferences"
        const val KEY_SELECTED_MODEL = "selected_model_id"
        const val KEY_HF_TOKEN = "hugging_face_token"
    }
}
