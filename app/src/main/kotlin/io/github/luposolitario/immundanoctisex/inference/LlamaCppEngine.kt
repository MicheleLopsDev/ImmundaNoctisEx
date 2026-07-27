package io.github.luposolitario.immundanoctisex.inference

import android.util.Log
import com.llamatik.library.platform.GenStream
import com.llamatik.library.platform.LlamaBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

// Implementazione di InferenceEngine su Llamatik (GGUF via llama.cpp,
// 27/07/2026, Michele: "introdurrei la possibilità di caricare i gguf" —
// un motore vero e selezionabile, non solo lo spike che l'ha preceduta:
// LlamaCppSpike.kt aveva già validato che la libreria carica e genera
// sul Razr). Stesso schema di LiteRtLmEngine: degrada sempre, mai
// un'eccezione verso l'alto (il gioco non si blocca mai).
//
// Un solo modello alla volta: LlamaBridge è un singleton nativo (non un
// oggetto per istanza come Engine di LiteRT-LM) — AppContainer scarica
// l'altro motore prima di caricare questo, non i due insieme.
class LlamaCppEngine : InferenceEngine {

    private var loaded = false
    private var config: InferenceConfig = InferenceConfig()

    private val _tokenInfo = MutableStateFlow(TokenInfo())
    override val tokenInfo: StateFlow<TokenInfo> = _tokenInfo.asStateFlow()

    override val isLoaded: Boolean get() = loaded

    override suspend fun load(modelFile: File, config: InferenceConfig): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!modelFile.exists()) {
                return@withContext Result.failure(
                    IllegalStateException("Modello non trovato: scaricalo da Modelli LLM."),
                )
            }
            runCatching {
                if (loaded) LlamaBridge.shutdown()
                val ok = LlamaBridge.initGenerateModel(modelFile.absolutePath)
                check(ok) { "Impossibile caricare il modello." }
                this@LlamaCppEngine.config = config
                applyParams(config)
                loaded = true
                _tokenInfo.value = TokenInfo(used = 0, maxTokens = config.maxTokens)
            }
        }

    // BUG trovato dal primo test di Michele (27/07, log del device):
    // senza gpuLayers il default è 0 = tutto su CPU, mai la GPU Adreno —
    // un modello grande sembrava bloccato invece di essere solo lento.
    // 99 (convenzione comune in llama.cpp) scarica tutti i livelli sulla
    // GPU. contextLength segue InferenceConfig.maxTokens (il budget di
    // contesto di CRITICITA.md, stesso concetto di maxNumTokens in
    // LiteRtLmEngine) — è un campo DIVERSO dal "maxTokens" di Llamatik,
    // che qui indica invece quante parole genera al massimo IN UN
    // turno: valore fisso, da ritoccare se la scena arriva tagliata.
    private fun applyParams(config: InferenceConfig) {
        LlamaBridge.updateGenerateParams(
            temperature = config.temperature,
            maxTokens = GENERATION_MAX_TOKENS,
            topP = config.topP,
            topK = config.topK,
            repeatPenalty = 1.1f,
            contextLength = config.maxTokens,
            numThreads = 4,
            useMmap = true,
            flashAttention = false,
            batchSize = 512,
            gpuLayers = 99,
        )
    }

    // Un reset per ogni scena, non un modello ricaricato: l'inferenza è
    // senza memoria (CRITICITA.md) — stessa regola di
    // LiteRtLmEngine.newSession(), qui è solo più economica (nessun
    // oggetto Conversation da chiudere e riaprire).
    override suspend fun newSession() = withContext(Dispatchers.IO) {
        if (!loaded) return@withContext
        runCatching { LlamaBridge.sessionReset() }
            .onFailure { Log.e(TAG, "Reset sessione fallito: ${it.message}") }
        _tokenInfo.value = TokenInfo(used = 0, maxTokens = config.maxTokens)
    }

    override fun generate(prompt: String): Flow<String> = callbackFlow {
        if (!loaded) {
            Log.e(TAG, "Nessun modello caricato: generazione saltata.")
            close()
            return@callbackFlow
        }
        val promptTokens = estimateTokens(prompt)
        var used = promptTokens
        _tokenInfo.value = TokenInfo(used = used, maxTokens = config.maxTokens)

        val startedAt = System.currentTimeMillis()
        var firstTokenAt: Long? = null
        var generatedTokens = 0

        LlamaBridge.generateStream(
            prompt,
            object : GenStream {
                override fun onDelta(text: String) {
                    if (text.isEmpty()) return
                    if (firstTokenAt == null) firstTokenAt = System.currentTimeMillis()
                    generatedTokens += estimateTokens(text)
                    used += estimateTokens(text)
                    _tokenInfo.value = TokenInfo(used = used, maxTokens = config.maxTokens)
                    trySend(text)
                }

                override fun onComplete() {
                    logMeasurements(startedAt, firstTokenAt, promptTokens, generatedTokens)
                    close()
                }

                override fun onError(message: String) {
                    Log.e(TAG, "Generazione fallita: $message")
                    close()
                }
            },
        )
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    // Stessa riga MISURA di LiteRtLmEngine, per confrontare i due motori
    // ad occhio nei log (`adb logcat -s LlamaCppEngine`).
    private fun logMeasurements(startedAt: Long, firstTokenAt: Long?, promptTokens: Int, generatedTokens: Int) {
        val now = System.currentTimeMillis()
        val firstTokenSec = firstTokenAt?.let { (it - startedAt) / 1000.0 }
        val decodeSeconds = firstTokenAt?.let { (now - it) / 1000.0 } ?: 0.0
        val tokensPerSecond = if (decodeSeconds > 0) generatedTokens / decodeSeconds else 0.0
        Log.i(
            TAG,
            "MISURA primoToken=${firstTokenSec?.let { "%.2f s".format(it) } ?: "mai"} " +
                "totale=${"%.2f s".format((now - startedAt) / 1000.0)} " +
                "tokenPrompt~$promptTokens tokenGenerati~$generatedTokens " +
                "velocita~${"%.1f".format(tokensPerSecond)} token/s (stima)",
        )
    }

    override suspend fun unload() = withContext(Dispatchers.IO) {
        if (loaded) runCatching { LlamaBridge.shutdown() }
        loaded = false
        _tokenInfo.value = TokenInfo(used = 0, maxTokens = config.maxTokens)
    }

    // STIMA, non conteggio: stessa scelta di LiteRtLmEngine, la libreria
    // non espone un tokenizer pubblico qui.
    private fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    private companion object {
        const val TAG = "LlamaCppEngine"
        const val GENERATION_MAX_TOKENS = 2048
    }
}
