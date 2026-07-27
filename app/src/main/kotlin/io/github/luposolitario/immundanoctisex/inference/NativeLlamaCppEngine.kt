package io.github.luposolitario.immundanoctisex.inference

import android.llama.cpp.LLamaAndroid
import android.llama.cpp.LLamaAndroid.Companion.State
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

// Terzo InferenceEngine, sperimentale (branch feature/llama-cpp-adreno,
// 27/07/2026, Michele: "usiamo gemma 3-12b" per il primo test reale col
// backend OpenCL/Adreno vero): llama.cpp compilato da noi (:llama), non
// Llamatik. Stesso schema di LlamaCppEngine (degrada sempre, riga MISURA
// nei log), ma qui lo scarico GPU è reale — vedi doc/LLAMA-CPP-ADRENO-SETUP.md
// e il fix di model_params.n_gpu_layers in llama-android.cpp.
//
// LLamaAndroid è un singleton nativo interno (RunLoop dedicato): un solo
// modello alla volta, stesso principio di LlamaBridge in LlamaCppEngine.
class NativeLlamaCppEngine : InferenceEngine {

    private val llamaAndroid = LLamaAndroid.instance()
    private var config: InferenceConfig = InferenceConfig()

    private val _tokenInfo = MutableStateFlow(TokenInfo())
    override val tokenInfo: StateFlow<TokenInfo> = _tokenInfo.asStateFlow()

    override val isLoaded: Boolean
        get() = LLamaAndroid.currentState is State.Loaded

    override suspend fun load(modelFile: File, config: InferenceConfig): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!modelFile.exists()) {
                return@withContext Result.failure(
                    IllegalStateException("Modello non trovato: scaricalo da Modelli LLM."),
                )
            }
            runCatching {
                // BUG (27/07/2026, secondo modello caricato da Michele:
                // nessun log, nessun errore, il vecchio restava attivo):
                // LLamaAndroid.load() ha un guard `if (!isLoad)` che, una
                // volta caricato UN modello, blocca silenziosamente
                // qualunque caricamento successivo — è un singleton nativo,
                // non un oggetto per istanza. Va scaricato esplicitamente
                // prima, stesso principio di LlamaCppEngine.load() con
                // LlamaBridge.
                if (isLoaded) llamaAndroid.unload()
                this@NativeLlamaCppEngine.config = config
                Log.i(TAG, "load(): avvio caricamento di ${modelFile.name}")
                llamaAndroid.load(
                    pathToModel = modelFile.absolutePath,
                    temperature = config.temperature,
                    repeatPenalty = 1.1f,
                    topK = config.topK,
                    topP = config.topP,
                    nCtx = config.maxTokens,
                )
                _tokenInfo.value = TokenInfo(used = 0, maxTokens = config.maxTokens)
            }
        }

    // No-op voluto: LLamaAndroid.send() chiama kv_cache_clear() da sé al
    // termine di ogni generazione (llama-android.cpp), quindi la sessione
    // è già pulita prima della scena successiva — stessa regola
    // dell'inferenza SENZA MEMORIA (CRITICITA.md) delle altre due engine.
    override suspend fun newSession() {
        _tokenInfo.value = TokenInfo(used = 0, maxTokens = config.maxTokens)
    }

    override fun generate(prompt: String): Flow<String> = flow {
        if (!isLoaded) {
            Log.e(TAG, "Nessun modello caricato: generazione saltata.")
            return@flow
        }
        val promptTokens = estimateTokens(prompt)
        var generatedTokens = 0
        _tokenInfo.value = TokenInfo(used = promptTokens, maxTokens = config.maxTokens)

        val startedAt = System.currentTimeMillis()
        var firstTokenAt: Long? = null

        llamaAndroid.send(prompt).collect { delta ->
            if (delta.isEmpty()) return@collect
            if (firstTokenAt == null) firstTokenAt = System.currentTimeMillis()
            generatedTokens += estimateTokens(delta)
            _tokenInfo.value = TokenInfo(used = promptTokens + generatedTokens, maxTokens = config.maxTokens)
            emit(delta)
        }

        val now = System.currentTimeMillis()
        val firstTokenSec = firstTokenAt?.let { (it - startedAt) / 1000.0 }
        val decodeSeconds = firstTokenAt?.let { (now - it) / 1000.0 } ?: 0.0
        val tokensPerSecond = if (decodeSeconds > 0) generatedTokens / decodeSeconds else 0.0
        Log.i(
            TAG,
            "MISURA primoToken=${firstTokenSec?.let { "%.2f s".format(it) } ?: "mai"} " +
                "totale=${"%.2f s".format((now - startedAt) / 1000.0)} " +
                "tokenPrompt~$promptTokens tokenGenerati~$generatedTokens " +
                "velocita~${"%.1f".format(tokensPerSecond)} token/s (stima, GPU Adreno via OpenCL)",
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun unload() = withContext(Dispatchers.IO) {
        llamaAndroid.unload()
        _tokenInfo.value = TokenInfo(used = 0, maxTokens = config.maxTokens)
    }

    // STIMA, non conteggio: stessa scelta delle altre due engine, nessun
    // tokenizer pubblico esposto qui.
    private fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    private companion object {
        const val TAG = "NativeLlamaCppEngine"
    }
}
