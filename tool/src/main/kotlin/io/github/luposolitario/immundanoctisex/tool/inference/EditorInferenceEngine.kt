package io.github.luposolitario.immundanoctisex.tool.inference

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import io.github.luposolitario.immundanoctisex.core.engine.inference.InferenceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

// Lo stesso motore del client, sul PC (02/08/2026, Michele: "voglio
// proprio avere una simulazione di quello che succede sul client").
//
// È il gemello desktop di LiteRtLmEngine (:app) e ne ricalca la sequenza
// riga per riga — stessa libreria alla stessa versione (litertlm-jvm
// contro litertlm-android), stesse classi, stesso ordine di tentativi
// sui backend, stesso file .litertlm scaricato da HuggingFace. Ciò che
// manca è solo ciò che è Android e qui non esiste: Context, cacheDir di
// sistema, logcat, batteria e heap nativo (quelle misure servivano alla
// Fase 4 sul telefono, sul PC non dicono nulla di utile).
//
// Non implementa InferenceEngine: quell'interfaccia vive in :app con
// TokenInfo e il semaforo del contesto, roba di partita che l'editor non
// ha. Qui servono tre gesti — carica, chiedi, scarica.
class EditorInferenceEngine {

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var config: InferenceConfig = InferenceConfig()

    // Quale backend ha accettato il modello. Va mostrato in UI: una
    // generazione lenta su CPU e una veloce su GPU sono due prove
    // diverse, e senza saperlo si trarrebbero conclusioni sbagliate.
    var activeBackend: String = "—"
        private set

    val isLoaded: Boolean get() = engine?.isInitialized() == true

    // Carica il modello. Stesso ordine del client: prima la GPU, poi la
    // CPU — non per prestazioni ma per fedeltà, perché sul telefono gira
    // su GPU e un'anteprima su CPU quando la GPU c'era sarebbe una prova
    // fatta in condizioni diverse da quelle vere.
    suspend fun load(modelFile: File, config: InferenceConfig): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!modelFile.exists()) {
                return@withContext Result.failure(
                    IllegalStateException("File del modello non trovato: ${modelFile.absolutePath}"),
                )
            }
            this@EditorInferenceEngine.config = config
            unloadInternal()

            val cache = File(System.getProperty("java.io.tmpdir"), "immundanoctisex-litertlm")
                .apply { mkdirs() }

            var ultimoErrore: Throwable? = null
            for ((nome, backend) in listOf("GPU" to Backend.GPU(), "CPU" to Backend.CPU())) {
                val esito = runCatching {
                    val creato = Engine(
                        EngineConfig(
                            modelPath = modelFile.absolutePath,
                            backend = backend,
                            maxNumTokens = config.maxTokens,
                            cacheDir = cache.absolutePath,
                        ),
                    )
                    creato.initialize()
                    creato
                }
                esito.onSuccess { creato ->
                    engine = creato
                    activeBackend = nome
                    return@withContext Result.success(Unit)
                }.onFailure { errore ->
                    ultimoErrore = errore
                }
            }
            // Il messaggio dell'ultimo tentativo arriva fino alla UI: un
            // "non funziona" muto costringerebbe a indovinare se manca la
            // GPU, se il file è corrotto o se il modello è di un'altra
            // versione.
            Result.failure(
                IllegalStateException(
                    "Nessun backend disponibile per questo modello: ${ultimoErrore?.message ?: "causa sconosciuta"}",
                    ultimoErrore,
                ),
            )
        }

    // Una conversazione NUOVA per ogni richiesta, come nel client:
    // l'inferenza è senza memoria (CRITICITA.md). Nell'editor conta
    // doppio — due anteprime della stessa scena devono partire dalle
    // stesse condizioni, altrimenti la seconda risentirebbe della prima
    // e non sarebbe confrontabile.
    suspend fun newSession() = withContext(Dispatchers.IO) {
        val attivo = engine ?: return@withContext
        runCatching { conversation?.close() }
        conversation = runCatching {
            attivo.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        topK = config.topK,
                        topP = config.topP.toDouble(),
                        temperature = config.temperature.toDouble(),
                    ),
                ),
            )
        }.getOrNull()
    }

    // Il testo a pezzi, come arriva: l'editor lo mostra mentre si forma,
    // così si capisce subito se il modello sta prendendo una direzione
    // sbagliata senza aspettare la fine.
    fun generate(prompt: String): Flow<String> = flow {
        val attiva = conversation
        if (attiva == null || !attiva.isAlive) return@flow

        attiva.sendMessageAsync(prompt).collect { messaggio ->
            val pezzo = messaggio.contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString("") { it.text }
            if (pezzo.isNotEmpty()) emit(pezzo)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun unload() = withContext(Dispatchers.IO) { unloadInternal() }

    private fun unloadInternal() {
        runCatching { conversation?.close() }
        runCatching { engine?.close() }
        conversation = null
        engine = null
        activeBackend = "—"
    }
}
