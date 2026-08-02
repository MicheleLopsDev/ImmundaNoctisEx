package io.github.luposolitario.immundanoctisex.tool.inference

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import io.github.luposolitario.immundanoctisex.core.engine.inference.InferenceConfig
import io.github.luposolitario.immundanoctisex.core.engine.inference.ripulisciTokenDiServizio
import io.github.luposolitario.immundanoctisex.tool.editor.EditorLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock

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

    // Ricordati per poter RICARICARE dopo che la GPU è stata persa
    // (02/08/2026): con un prefill lungo su GPU integrata Windows può
    // far scattare il TDR e resettare il driver — l'Engine resta lì ma
    // è morto, e ogni chiamata successiva fallisce. Senza questi due
    // valori l'unico rimedio sarebbe chiudere l'editor.
    private var ultimoFile: File? = null
    private var ultimoSoloCpu: Boolean = false

    // Tenuti aperti finché il modello è caricato: chiudere il canale
    // rilascia il lock, e la cartella tornerebbe libera per un altro
    // processo mentre la stiamo ancora usando.
    private var lockCache: FileLock? = null
    private var canaleLock: FileChannel? = null

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
    suspend fun load(
        modelFile: File,
        config: InferenceConfig,
        // Solo CPU (02/08/2026, Michele: "non possiamo usare cpu, è più
        // lento ma è stabile, alla fine è un tool non il client"). Sulla
        // GPU integrata il TDR di Windows resetta il driver a metà di un
        // prompt lungo, in modo intermittente: per un editor vale più la
        // certezza di arrivare in fondo che il tempo risparmiato. Il
        // client resta su GPU — là i prompt girano su Adreno via OpenCL,
        // dove il problema non esiste.
        soloCpu: Boolean = false,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!modelFile.exists()) {
                return@withContext Result.failure(
                    IllegalStateException("File del modello non trovato: ${modelFile.absolutePath}"),
                )
            }
            this@EditorInferenceEngine.config = config
            this@EditorInferenceEngine.ultimoFile = modelFile
            this@EditorInferenceEngine.ultimoSoloCpu = soloCpu
            unloadInternal()
            motivoRipiegoCpu = null

            // Una cartella cache PER BACKEND, e riservata a un processo
            // solo (02/08/2026). Prima era una sola per tutti: dentro
            // finivano insieme la cache XNNPACK della CPU e quelle
            // mldrift della GPU, e con due processi che caricavano lo
            // stesso modello insieme la libreria nativa moriva di
            // EXCEPTION_ACCESS_VIOLATION dentro nativeCreateEngine —
            // un crash della JVM intera, non un'eccezione da catturare.
            val cache = cartellaCache(if (soloCpu) "cpu" else "gpu")

            // Va fatto PRIMA di costruire l'Engine: dopo, Dawn ha già
            // tentato e fallito la sua LoadLibrary.
            val esitoGpu = SupportoGpuWindows.preparaShaderCompiler()

            var ultimoErrore: Throwable? = null
            // Su CPU il default della libreria è 4 thread — pensato per
            // un telefono. Su un PC desktop è metà della macchina lasciata
            // ferma: si usano tutti i processori logici disponibili
            // (02/08/2026, misura: 1,6 token/s con 4 thread sul 4B).
            val threadCpu = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(4)
            val tentativi = if (soloCpu) {
                listOf("CPU" to Backend.CPU(threadCpu))
            } else {
                listOf("GPU" to Backend.GPU(), "CPU" to Backend.CPU(threadCpu))
            }
            for ((nome, backend) in tentativi) {
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
                    // Solo quando la CPU è un RIPIEGO: se è stata scelta
                    // apposta non c'è niente da spiegare, e chiamarla
                    // ripiego suonerebbe come un guasto.
                    if (nome == "CPU" && !soloCpu) {
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
            // I token di servizio (<pad> in testa alla risposta, visto
            // alla prima traduzione vera) non devono arrivare a chi
            // legge: stessa pulizia del client, funzione condivisa.
            val pulito = ripulisciTokenDiServizio(pezzo)
            if (pulito.isNotEmpty()) emit(pulito)
        }
    }.flowOn(Dispatchers.IO)

    // La cartella cache da usare per questo backend, tenendo fuori gli
    // altri processi. Il lock è un file dentro la cartella: se un'altra
    // istanza dell'editor la sta già usando non si aspetta e non si
    // fallisce — si lavora su una cartella propria, che costa solo un
    // caricamento più lento la prima volta. Meglio lento che corrotto.
    private fun cartellaCache(backend: String): File {
        val base = File(System.getProperty("java.io.tmpdir"), "immundanoctisex-litertlm")
        val condivisa = File(base, backend).apply { mkdirs() }

        rilasciaLockCache()
        val esito = runCatching {
            val canale = RandomAccessFile(File(condivisa, ".lock"), "rw").channel
            val lock = canale.tryLock()
            if (lock == null) canale.close()
            lock?.also { canaleLock = canale; lockCache = it }
        }.getOrNull()

        if (esito != null) return condivisa

        val privata = File(base, "$backend-pid${ProcessHandle.current().pid()}").apply { mkdirs() }
        EditorLog.i(TAG, "Cache $backend già in uso da un altro processo: uso ${privata.name}")
        return privata
    }

    private fun rilasciaLockCache() {
        runCatching { lockCache?.release() }
        runCatching { canaleLock?.close() }
        lockCache = null
        canaleLock = null
    }

    // Ricarica il modello com'era: serve dopo che la GPU è stata persa,
    // quando l'Engine esiste ancora ma non risponde più.
    suspend fun ricarica(): Result<Unit> {
        val file = ultimoFile
            ?: return Result.failure(IllegalStateException("Nessun modello da ricaricare."))
        EditorLog.i(TAG, "Ricarico ${file.name} dopo la perdita del dispositivo")
        return load(file, config, ultimoSoloCpu)
    }

    suspend fun unload() = withContext(Dispatchers.IO) { unloadInternal() }

    private fun unloadInternal() {
        runCatching { conversation?.close() }
        runCatching { engine?.close() }
        conversation = null
        engine = null
        activeBackend = "—"
        motivoRipiegoCpu = null
    }

    companion object {
        private const val TAG = "EditorInferenceEngine"

        // Riconosce il guasto in cui Windows resetta il driver grafico
        // (TDR) perché un'operazione GPU ha impiegato troppo: il
        // messaggio arriva dal codice nativo in inglese, e cambia forma
        // a seconda di dove viene intercettato — device hung, device
        // removed, o il timeout sul readback dei risultati.
        //
        // Non è un errore del libro né del prompt: capita con prefill
        // lunghi su GPU integrata, in modo INTERMITTENTE (la stessa
        // scena può passare al secondo tentativo). Per questo si
        // riconosce: è l'unico caso in cui ritentare ha senso.
        fun eDispositivoPerso(errore: Throwable?): Boolean {
            val messaggio = generateSequence(errore) { it.cause }
                .mapNotNull { it.message }
                .joinToString(" ")
                .lowercase()
            return listOf(
                "device_hung",
                "device_removed",
                "device removed",
                "reading back data",
                "0x887a",
            ).any { it in messaggio }
        }
    }
}
