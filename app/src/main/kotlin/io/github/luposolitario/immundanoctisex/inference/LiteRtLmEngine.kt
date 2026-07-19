package io.github.luposolitario.immundanoctisex.inference

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

// Implementazione di InferenceEngine su LiteRT-LM (com.google.ai.edge.
// litertlm). Sostituisce il GemmaEngine/MediaPipe di v1: i modelli nuovi
// escono in formato .litertlm e MediaPipe non li legge.
//
// Backend: si prova la GPU e si ripiega su CPU. Non è un vezzo — i
// benchmark pubblicati del modello danno primo token 0,8 s su GPU contro
// 5,3 s su CPU, e CRITICITA.md fissa la soglia a 3 s: senza GPU
// l'obiettivo non si raggiunge. La memoria segue la stessa direzione
// (710 MB contro 3283 MB).
class LiteRtLmEngine(private val context: Context) : InferenceEngine {

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var config: InferenceConfig = InferenceConfig()

    // Backend effettivamente in uso: serve alle misure di Fase 4, perché
    // un numero senza sapere se girava su GPU o CPU non dice nulla.
    var activeBackend: String = "—"
        private set

    private val _tokenInfo = MutableStateFlow(TokenInfo())
    override val tokenInfo: StateFlow<TokenInfo> = _tokenInfo.asStateFlow()

    override val isLoaded: Boolean get() = engine?.isInitialized() == true

    override suspend fun load(modelFile: File, config: InferenceConfig): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!modelFile.exists()) {
                return@withContext Result.failure(
                    IllegalStateException("Modello non trovato: scaricalo da Modelli LLM."),
                )
            }
            this@LiteRtLmEngine.config = config
            unloadInternal()

            // Prima la GPU, poi la CPU: se il device non ha OpenCL
            // utilizzabile l'inizializzazione fallisce e si degrada invece
            // di lasciare il gioco senza narratore.
            val attempts = listOf<Pair<String, Backend>>(
                "GPU" to Backend.GPU(),
                "CPU" to Backend.CPU(),
            )
            for ((name, backend) in attempts) {
                val outcome = runCatching {
                    val created = Engine(
                        EngineConfig(
                            modelPath = modelFile.absolutePath,
                            backend = backend,
                            maxNumTokens = config.maxTokens,
                            // Cache scrivibile: accorcia i caricamenti
                            // successivi al primo.
                            cacheDir = context.cacheDir.absolutePath,
                        ),
                    )
                    created.initialize()
                    created
                }
                outcome.onSuccess { created ->
                    engine = created
                    activeBackend = name
                    _tokenInfo.value = TokenInfo(used = 0, maxTokens = config.maxTokens)
                    Log.i(TAG, "Modello caricato su $name")
                    return@withContext Result.success(Unit)
                }.onFailure { error ->
                    Log.w(TAG, "Backend $name non disponibile: ${error.message}")
                }
            }
            Result.failure(IllegalStateException("Impossibile inizializzare il motore su questo dispositivo."))
        }

    // Una conversazione NUOVA per ogni scena: l'inferenza è senza memoria
    // (CRITICITA.md). Non è un reset d'emergenza come in v1, è la norma.
    override suspend fun newSession() = withContext(Dispatchers.IO) {
        val active = engine ?: return@withContext
        runCatching { conversation?.close() }
        conversation = runCatching {
            active.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        topK = config.topK,
                        topP = config.topP.toDouble(),
                        temperature = config.temperature.toDouble(),
                    ),
                ),
            )
        }.getOrNull()
        _tokenInfo.value = TokenInfo(used = 0, maxTokens = config.maxTokens)
    }

    override fun generate(prompt: String): Flow<String> = flow {
        val active = conversation
        if (active == null || !active.isAlive) {
            // Nessuna eccezione verso l'alto: chi chiama degrada sul testo
            // originale del pacchetto.
            Log.e(TAG, "Nessuna conversazione attiva: generazione saltata.")
            return@flow
        }

        // Il prompt speso è già contesto consumato: si conta subito, così
        // il semaforo dice la verità anche prima della risposta.
        var used = estimateTokens(prompt)
        _tokenInfo.value = TokenInfo(used = used, maxTokens = config.maxTokens)

        active.sendMessageAsync(prompt).collect { message ->
            val chunk = message.text()
            if (chunk.isNotEmpty()) {
                used += estimateTokens(chunk)
                _tokenInfo.value = TokenInfo(used = used, maxTokens = config.maxTokens)
                emit(chunk)
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun unload() = withContext(Dispatchers.IO) {
        unloadInternal()
        _tokenInfo.value = TokenInfo(used = 0, maxTokens = config.maxTokens)
    }

    private fun unloadInternal() {
        runCatching { conversation?.close() }
        runCatching { engine?.close() }
        conversation = null
        engine = null
        activeBackend = "—"
    }

    // Il testo di un Message sta nei suoi Content di tipo Text.
    private fun com.google.ai.edge.litertlm.Message.text(): String =
        contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }

    // STIMA, non conteggio: la libreria non espone un tokenizer pubblico.
    // Serve solo al semaforo (verde/giallo/rosso), che è un'indicazione
    // di massima. Da sostituire se l'API esporrà il conteggio vero.
    private fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    private companion object {
        const val TAG = "LiteRtLmEngine"
    }
}
