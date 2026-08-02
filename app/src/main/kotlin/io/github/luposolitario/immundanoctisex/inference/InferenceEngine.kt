package io.github.luposolitario.immundanoctisex.inference

import io.github.luposolitario.immundanoctisex.core.engine.inference.InferenceConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

// Consumo del contesto, per il semaforo nell'header della scena
// (UI.md §Header: verde pronto / giallo / rosso quasi pieno).
data class TokenInfo(
    val used: Int = 0,
    val maxTokens: Int = InferenceConfig.DEFAULT_MAX_TOKENS,
) {
    val percentage: Int get() = if (maxTokens <= 0) 0 else ((used * 100) / maxTokens).coerceIn(0, 100)

    // Soglie ereditate da v1.
    val status: TokenStatus
        get() = when {
            percentage >= 95 -> TokenStatus.CRITICAL
            percentage > 60 -> TokenStatus.RED
            percentage > 33 -> TokenStatus.YELLOW
            else -> TokenStatus.GREEN
        }
}

enum class TokenStatus { GREEN, YELLOW, RED, CRITICAL }

// Una delle quattro interfacce motivate (ARCHITETTURA.md), erede di
// quella di v1 con due differenze volute:
//  - niente `chatbotPersonality` nel load: era dell'era-chatbot;
//  - `newSession()` invece di `resetSession(systemPrompt)`: in Ex
//    l'inferenza è SENZA MEMORIA (CRITICITA.md), una sessione nuova per
//    ogni scena è la norma, non un rimedio a un contesto pieno.
// Il resto dell'app non sa che esiste Gemma: chiede di arricchire una
// scena e riceve testo.
interface InferenceEngine {

    val tokenInfo: StateFlow<TokenInfo>

    val isLoaded: Boolean

    // Carica il modello. Fallisce con un messaggio leggibile invece di
    // lanciare: il gioco non si blocca mai, degrada sul testo originale.
    suspend fun load(modelFile: File, config: InferenceConfig): Result<Unit>

    // Apre una sessione pulita per la scena che sta per essere generata.
    suspend fun newSession()

    // Genera in streaming: ogni emissione è un pezzo di testo da
    // accodare. Chi mostra taglia a `--- TAGS ---` (ResponseParser).
    fun generate(prompt: String): Flow<String>

    suspend fun unload()
}
