package io.github.luposolitario.immundanoctisex.inference

import android.util.Log
import com.llamatik.library.platform.LlamaBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
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
                this@LlamaCppEngine.config = config
                // BUG (27/07/2026, secondo test di Michele: gpu_layers=0
                // ancora nel log nonostante gpuLayers=99): lo scarico
                // sulla GPU si decide al CARICAMENTO dei pesi del
                // modello, quindi va impostato PRIMA di initGenerateModel,
                // non dopo — troppo tardi per spostare pesi già letti in
                // RAM di sistema. Nell'esempio originale della libreria
                // updateGenerateParams viene per primo: qui invertivamo
                // l'ordine.
                applyParams(config)
                val ok = LlamaBridge.initGenerateModel(modelFile.absolutePath)
                check(ok) { "Impossibile caricare il modello." }
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

    // BLOCCANTE, non streaming (27/07/2026, crash trovato da Michele col
    // modello piccolo — log del device): `generateStream`/
    // `nativeGenerateStream` ha un bug nella libreria stessa. Un
    // carattere accentato italiano (UTF-8 multi-byte: à, è, ì...) può
    // finire tagliato a metà tra due "delta" del callback nativo, e
    // `NewStringUTF` va in crash su un byte di continuazione mancante
    // ("JNI DETECTED ERROR: illegal continuation byte"). `generate()`
    // decodifica il testo intero in un colpo solo: nessun confine a
    // metà carattere possibile. Si perde l'effetto token-per-token per
    // QUESTO motore soltanto (LiteRT-LM lo mantiene, non ha lo stesso
    // bug) finché Llamatik non sistema lo streaming a monte.
    override fun generate(prompt: String): Flow<String> = flow {
        if (!loaded) {
            Log.e(TAG, "Nessun modello caricato: generazione saltata.")
            return@flow
        }
        val promptTokens = estimateTokens(prompt)
        _tokenInfo.value = TokenInfo(used = promptTokens, maxTokens = config.maxTokens)

        val startedAt = System.currentTimeMillis()
        val text = LlamaBridge.generate(prompt)
        val elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000.0
        val generatedTokens = estimateTokens(text)
        _tokenInfo.value = TokenInfo(used = promptTokens + generatedTokens, maxTokens = config.maxTokens)

        // Non c'è un "primo token" da misurare qui (una sola chiamata
        // bloccante, non uno stream) — solo il tempo totale, a
        // differenza della riga MISURA di LiteRtLmEngine.
        Log.i(
            TAG,
            "MISURA totale=${"%.2f s".format(elapsedSeconds)} " +
                "tokenPrompt~$promptTokens tokenGenerati~$generatedTokens " +
                "velocita~${"%.1f".format(if (elapsedSeconds > 0) generatedTokens / elapsedSeconds else 0.0)} " +
                "token/s (stima, non-streaming)",
        )
        emit(text)
    }.flowOn(Dispatchers.IO)

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
