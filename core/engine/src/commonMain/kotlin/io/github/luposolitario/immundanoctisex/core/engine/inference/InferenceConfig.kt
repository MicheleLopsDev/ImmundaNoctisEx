package io.github.luposolitario.immundanoctisex.core.engine.inference

// I parametri di generazione. I default vengono da v1 (provati su Gemma),
// tranne maxTokens: CRITICITA.md fissa 10240 come contesto di riferimento.
//
// Vive in :core:engine dal 02/08/2026 (prima in :app, dentro
// InferenceEngine.kt): l'editor desktop deve caricare il modello con le
// STESSE identiche impostazioni del client, altrimenti "vedere il
// risultato mentre scrivi" mostrerebbe il risultato di un'altra
// configurazione. Nessuna dipendenza Android qui — sono quattro numeri.
data class InferenceConfig(
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val temperature: Float = DEFAULT_TEMPERATURE,
    val topK: Int = DEFAULT_TOP_K,
    val topP: Float = DEFAULT_TOP_P,
) {
    companion object {
        const val DEFAULT_MAX_TOKENS = 10240
        // Abbassata da 0,7 (27/07/2026, Michele: meno inventiva, in
        // accompagnamento alla regola 7 di PromptBuilder.CONSTRAINT_TEXT
        // contro le parole inventate — es. "bruffi" su Gemma 4B). Non
        // tocca chi ha già un valore personale salvato in
        // InferencePreferences, solo il default per chi non l'ha mai
        // toccato.
        const val DEFAULT_TEMPERATURE = 0.5f
        const val DEFAULT_TOP_K = 40
        const val DEFAULT_TOP_P = 0.9f

        // Preset fisso per la Modalità Traduzione (31/07/2026,
        // InferencePreferences.toConfig()): molto più bassa di
        // DEFAULT_TEMPERATURE, per restare il più possibile fedele al
        // testo sorgente invece di deviare in prosa creativa.
        // maxTokens invariato (DEFAULT_MAX_TOKENS): resta un tetto, non
        // una lunghezza imposta.
        val TRANSLATION_PRESET = InferenceConfig(
            temperature = 0.2f,
            topK = 20,
            topP = 0.85f,
        )
    }
}
