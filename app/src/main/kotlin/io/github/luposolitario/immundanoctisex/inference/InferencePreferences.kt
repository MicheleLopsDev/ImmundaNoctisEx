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

    // La fotografia da passare al motore al caricamento.
    fun toConfig(): InferenceConfig = InferenceConfig(
        maxTokens = maxTokens,
        temperature = temperature,
        topK = topK,
        topP = topP,
    )

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
    }
}
