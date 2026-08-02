package io.github.luposolitario.immundanoctisex.tool.inference

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import io.github.luposolitario.immundanoctisex.core.engine.inference.InferenceConfig
import io.github.luposolitario.immundanoctisex.tool.editor.EditorLog
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

    // Perché si è finiti su CPU invece che su GPU (02/08/2026). Prima il
    // ripiego era muto: l'interfaccia diceva "Caricato su CPU" e il
    // motivo restava nell'output nativo, dove nessuno lo cercava. Lo
    // stesso difetto che aveva reso illeggibile il download fermo al 19%.
    var motivoRipiegoCpu: String? = null
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
            motivoRipiegoCpu = null

            val cache = File(System.getProperty("java.io.tmpdir"), "immundanoctisex-litertlm")
                .apply { mkdirs() }

            // Va fatto PRIMA di costruire l'Engine: dopo, Dawn ha già
            // tentato e fallito la sua LoadLibrary.
            val esitoGpu = SupportoGpuWindows.preparaShaderCompiler()

            var ultimoErrore: Throwable? = null
            // Su CPU il default della libreria è 4 thread — pensato per
            // un telefono. Su un PC desktop è metà della macchina lasciata
            // ferma: si usano tutti i processori logici disponibili
            // (02/08/2026, misura: 1,6 token/s con 4 thread sul 4B).
            val threadCpu = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(4)
            for ((nome, backend) in listOf("GPU" to Backend.GPU(), "CPU" to Backend.CPU(threadCpu))) {
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
                    if (nome == "CPU") {
                        // Il messaggio del tentativo GPU fallito è quasi
                        // sempre generico: l'esito del precaricamento
                        // dice di più, perché distingue "manca il
                        // compilatore shader" da "la GPU c'è ma rifiuta".
                        motivoRipiegoCpu = esitoGpu
                        EditorLog.i(TAG, "Ripiego su CPU. $esitoGpu")
                    }
                    EditorLog.i(TAG, "Modello caricato su $nome: ${modelFile.name}")
                    return@withContext Result.success(Unit)
                }.onFailure { errore ->
                    ultimoErrore = errore
                    EditorLog.i(TAG, "Backend $nome non utilizzabile: ${errore.message ?: errore::class.simpleName}")
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
        motivoRipiegoCpu = null
    }

    private companion object {
        const val TAG = "EditorInferenceEngine"
    }
}
