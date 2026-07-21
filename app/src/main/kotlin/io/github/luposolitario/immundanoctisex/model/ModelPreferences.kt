package io.github.luposolitario.immundanoctisex.model

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    // BUG (22/07/2026, Michele: "scarica il modello personalizzato ma non
    // lo usa"): cercava SOLO nel catalogo fisso. Un modello personalizzato
    // selezionato non ci sta mai dentro -> ModelCatalog.byId() tornava
    // null -> si ricadeva silenziosamente su ModelCatalog.default (sempre
    // Gemma 4 E4B ufficiale), qualunque cosa l'utente avesse scelto.
    val selectedModel: DownloadableModel
        get() = customModels.firstOrNull { it.id == selectedModelId }
            ?: ModelCatalog.byId(selectedModelId)
            ?: ModelCatalog.default

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

    // I "preferiti" (ModelCatalog.all) restano fissi e decisi in fase di
    // sviluppo; questi sono i modelli che Michele aggiunge da sé incollando
    // un link Hugging Face, per provare alternative a Gemma 4 senza
    // toccare il catalogo. Salvati come lista JSON nelle preferenze.
    var customModels: List<DownloadableModel>
        get() = prefs.getString(KEY_CUSTOM_MODELS, null)
            ?.let { runCatching { Json.decodeFromString<List<DownloadableModel>>(it) }.getOrNull() }
            ?: emptyList()
        set(value) = prefs.edit().putString(KEY_CUSTOM_MODELS, Json.encodeToString(value)).apply()

    fun addCustomModel(model: DownloadableModel) {
        customModels = customModels.filterNot { it.id == model.id } + model
    }

    fun removeCustomModel(id: String) {
        customModels = customModels.filterNot { it.id == id }
    }

    private companion object {
        const val PREFS_NAME = "model_preferences"
        const val KEY_SELECTED_MODEL = "selected_model_id"
        const val KEY_HF_TOKEN = "hugging_face_token"
        const val KEY_CUSTOM_MODELS = "custom_models"
    }
}
