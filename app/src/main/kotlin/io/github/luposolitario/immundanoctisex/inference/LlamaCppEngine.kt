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

    // CORREZIONE (27/07/2026, dal log di un test di Michele: 1,5 token/s,
    // 237s al primo token su Gemma 3 12B): il commento precedente qui
    // era sbagliato. `gpuLayers=99` NON scarica nulla su Adreno — il
    // README ufficiale di Llamatik documenta gpuLayers come "-1 = all
    // layers (Metal / CUDA)": il backend GPU esiste solo su iOS/Desktop,
    // su Android questa libreria gira SEMPRE su CPU, a prescindere dal
    // valore impostato qui. Non è un bug nostro né una regressione: è
    // un limite della libreria, coerente con l'aver scelto "niente JNI/
    // compilazione nostra" — un vero backend OpenCL/Vulkan per Adreno
    // richiederebbe di compilare llama.cpp da soli. `numThreads` usa
    // tutti i core meno 2 (lasciati a UI/sistema) invece del 4 fisso di
    // prima: lo Snapdragon 8 Elite del Razr ne ha 8, un margine concreto
    // di velocità senza cambiare libreria. contextLength segue
    // InferenceConfig.maxTokens (il budget di contesto di CRITICITA.md,
    // stesso concetto di maxNumTokens in LiteRtLmEngine) — è un campo
    // DIVERSO dal "maxTokens" di Llamatik, che qui indica invece quante
    // parole genera al massimo IN UN turno: valore fisso, da ritoccare
    // se la scena arriva tagliata.
    private fun applyParams(config: InferenceConfig) {
        LlamaBridge.updateGenerateParams(
            temperature = config.temperature,
            maxTokens = GENERATION_MAX_TOKENS,
            topP = config.topP,
            topK = config.topK,
            repeatPenalty = 1.1f,
            contextLength = config.maxTokens,
            numThreads = (Runtime.getRuntime().availableProcessors() - 2).coerceAtLeast(2),
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

    // Streaming RIABILITATO (27/07/2026, Michele: "adesso aspettiamo di
    // più perché finisca tutto il testo... non capisco perché su
    // literm funziona e su gguf no?"). Non era un limite del motore
    // GGUF in sé: era un bug di Llamatik 1.7.0, la stessa libreria usata
    // da LiteRtLmEngine (che infatti non l'ha mai avuto) non c'entra —
    // `generateStream`/`nativeGenerateStream` poteva tagliare a metà un
    // carattere UTF-8 multi-byte (à, è, ì...) tra due "delta" del
    // callback nativo, e `NewStringUTF` andava in crash (SIGABRT) su un
    // frammento con byte di continuazione mancante. Riprovato dopo aver
    // alzato la dipendenza a 1.9.1, il cui changelog ufficiale dichiara
    // "Solved generateStream emoji crash" — stessa famiglia di bug
    // (confine di carattere multi-byte), non ancora confermato sul
    // device per il caso specifico degli accenti italiani: da riverificare
    // con Michele alla prossima partita, pronti a tornare alla
    // generate() bloccante se il crash si ripresenta.
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
