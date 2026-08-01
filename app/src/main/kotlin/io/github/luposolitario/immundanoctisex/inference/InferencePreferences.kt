package io.github.luposolitario.immundanoctisex.inference

import android.content.Context
import android.content.SharedPreferences

// I parametri di generazione modificabili dall'utente (sezione
// "Impostazioni avanzate" della schermata Modelli, ereditata da v1).
// In Ex NON serve riavviare la partita come in v1: si apre una sessione
// nuova a ogni scena, quindi un parametro cambiato vale dalla prossima.
class InferencePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var maxTokens: Int
        get() = prefs.getInt(KEY_MAX_TOKENS, InferenceConfig.DEFAULT_MAX_TOKENS)
        set(value) = prefs.edit().putInt(KEY_MAX_TOKENS, value.coerceIn(MIN_TOKENS, MAX_TOKENS)).apply()

    var temperature: Float
        get() = prefs.getFloat(KEY_TEMPERATURE, InferenceConfig.DEFAULT_TEMPERATURE)
        set(value) = prefs.edit().putFloat(KEY_TEMPERATURE, value.coerceIn(0f, 1f)).apply()

    var topK: Int
        get() = prefs.getInt(KEY_TOP_K, InferenceConfig.DEFAULT_TOP_K)
        set(value) = prefs.edit().putInt(KEY_TOP_K, value.coerceIn(1, 100)).apply()

    var topP: Float
        get() = prefs.getFloat(KEY_TOP_P, InferenceConfig.DEFAULT_TOP_P)
        set(value) = prefs.edit().putFloat(KEY_TOP_P, value.coerceIn(0f, 1f)).apply()

    // Se Gemma sceglie lo sfondo quando il pacchetto non ne ha uno valido
    // (PromptBuilder.imageFormatText) — DISATTIVATO di default (26/07/2026:
    // "riattiveremo se troviamo un modello più intelligente"). Ora è una
    // preferenza vera, non più un ramo di codice tolto (27/07/2026, Michele:
    // "vorrei che fosse una cosa configurabile e magari deselezionabile dal
    // menu LLM") — utile per confrontare modelli diversi (es. GGUF) senza
    // che serva una modifica di codice ogni volta.
    var askImageInPrompt: Boolean
        get() = prefs.getBoolean(KEY_ASK_IMAGE, false)
        set(value) = prefs.edit().putBoolean(KEY_ASK_IMAGE, value).apply()

    // Modalità Traduzione (31/07/2026, Michele: niente arricchimento, solo
    // traduzione fedele) — quando attiva, toConfig() sotto ignora i 4
    // valori salvati e usa un preset fisso: i valori restano comunque
    // salvati, pronti per quando si torna alla modalità libera.
    var translationMode: Boolean
        get() = prefs.getBoolean(KEY_TRANSLATION_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_TRANSLATION_MODE, value).apply()

    // Interruttore esplicito (01/08/2026, Michele: "dovresti darmi la
    // possibilità di disattivare il modello") — con l'auto-load
    // all'apertura dell'app (AppNavigation.kt), il caso "motore spento,
    // chiedo" era di fatto irraggiungibile: appena c'è un modello
    // scaricato riparte da solo. Questa preferenza è la fonte di verità
    // che AppContainer.ensureModelLoaded() controlla per PRIMA cosa: se
    // falsa, non tocca il motore, né all'avvio né entrando in avventura.
    // Persistente (resta spenta finché non la riaccendi tu, non solo per
    // la sessione corrente) — attivare un modello a mano la rimette a
    // true da sé (un gesto esplicito vince sempre sull'ultima preferenza).
    var engineEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENGINE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENGINE_ENABLED, value).apply()

    // La fotografia da passare al motore al caricamento.
    fun toConfig(): InferenceConfig = if (translationMode) {
        InferenceConfig.TRANSLATION_PRESET
    } else {
        InferenceConfig(
            maxTokens = maxTokens,
            temperature = temperature,
            topK = topK,
            topP = topP,
        )
    }

    // Serve dopo una sessione di misure andata storta: si torna ai valori
    // provati senza dover ricordare quali fossero.
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    companion object {
        const val MIN_TOKENS = 512
        const val MAX_TOKENS = 32768

        private const val PREFS_NAME = "inference_preferences"
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_TOP_K = "top_k"
        private const val KEY_TOP_P = "top_p"
        private const val KEY_ASK_IMAGE = "ask_image_in_prompt"
        private const val KEY_TRANSLATION_MODE = "translation_mode"
        private const val KEY_ENGINE_ENABLED = "engine_enabled"
    }
}
