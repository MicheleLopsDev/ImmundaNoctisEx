package io.github.luposolitario.immundanoctisex.inference

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import io.github.luposolitario.immundanoctisex.core.engine.inference.InferenceConfig
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

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

    // Contatori della diagnostica sull'accumulo. Se "vive" cresce oltre 1
    // le conversazioni non si stanno chiudendo, ed è quello il colpevole.
    private var conversazioniCreate = 0
    private var conversazioniChiuse = 0
    private var chiusureFallite = 0
    private var generazioni = 0

    // Contatori dei CICLI DI MOTORE (03/08/2026). Il log del 03/08
    // mostrava la memoria nativa ferma per cinque generazioni e poi su di
    // ~80 MB di colpo, due volte: un salto così non somiglia a un
    // accumulo per generazione, somiglia a un caricamento. Ma quel log
    // era filtrato su due soli tag e non conteneva `AppContainer`, quindi
    // non poteva né confermarlo né smentirlo. Ora il motore conta da sé
    // quante volte è stato creato e chiuso, e la riga MISURA lo dice.
    private var motoriCreati = 0
    private var motoriChiusi = 0
    private var chiusureMotoreFallite = 0

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
                    motoriCreati++
                    _tokenInfo.value = TokenInfo(used = 0, maxTokens = config.maxTokens)
                    Log.i(
                        TAG,
                        "Modello caricato su $name (motore #$motoriCreati) " +
                            "nativa=${nativeHeapMb()}MB pss=${pssTotaleMb()}MB",
                    )
                    return@withContext Result.success(Unit)
                }.onFailure { error ->
                    Log.w(TAG, "Backend $name non disponibile: ${error.message}")
                }
            }
            Result.failure(IllegalStateException("Impossibile inizializzare il motore su questo dispositivo."))
        }

    // Una conversazione NUOVA per ogni scena: l'inferenza è senza memoria
    // (CRITICITA.md). Non è un reset d'emergenza come in v1, è la norma.
    //
    // DIAGNOSTICA (20/07/2026): il decode rallenta del 38% in cinque scene
    // a telefono freddo, mentre il primo token resta stabile — cala solo
    // la fase che gira token per token, quindi qualcosa si ACCUMULA tra le
    // generazioni. Prima, l'esito di close() veniva scartato: se la
    // chiusura falliva, le conversazioni (e la loro KV cache sulla GPU) si
    // sarebbero sommate in silenzio. Ora si conta e si dice.
    override suspend fun newSession() = withContext(Dispatchers.IO) {
        val active = engine ?: return@withContext
        conversation?.let { vecchia ->
            runCatching { vecchia.close() }
                .onSuccess { conversazioniChiuse++ }
                .onFailure { errore ->
                    chiusureFallite++
                    Log.e(TAG, "CHIUSURA CONVERSAZIONE FALLITA (#$chiusureFallite): ${errore.message}", errore)
                }
        }
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
        }.onSuccess { conversazioniCreate++ }
            .onFailure { Log.e(TAG, "Creazione conversazione fallita: ${it.message}") }
            .getOrNull()
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
        val promptTokens = estimateTokens(prompt)
        var used = promptTokens
        _tokenInfo.value = TokenInfo(used = used, maxTokens = config.maxTokens)

        // MISURE della milestone di Fase 4 (CRITICITA.md): si raccolgono
        // da sole a ogni scena giocata, così i numeri vengono dall'uso
        // reale invece che da una prova artificiale.
        val startedAt = System.currentTimeMillis()
        var firstTokenAt: Long? = null
        var generatedTokens = 0

        active.sendMessageAsync(prompt).collect { message ->
            val chunk = message.text()
            if (chunk.isNotEmpty()) {
                if (firstTokenAt == null) firstTokenAt = System.currentTimeMillis()
                generatedTokens += estimateTokens(chunk)
                used += estimateTokens(chunk)
                _tokenInfo.value = TokenInfo(used = used, maxTokens = config.maxTokens)
                emit(chunk)
            }
        }

        logMeasurements(startedAt, firstTokenAt, promptTokens, generatedTokens)
    }.flowOn(Dispatchers.IO)

    // Una riga sola, leggibile con `adb logcat -s LiteRtLmEngine`, da
    // riportare nel diario come misura di CRITICITA.md.
    private fun logMeasurements(
        startedAt: Long,
        firstTokenAt: Long?,
        promptTokens: Int,
        generatedTokens: Int,
    ) {
        val now = System.currentTimeMillis()
        val firstTokenSec = firstTokenAt?.let { (it - startedAt) / 1000.0 }
        val decodeSeconds = firstTokenAt?.let { (now - it) / 1000.0 } ?: 0.0
        val tokensPerSecond = if (decodeSeconds > 0) generatedTokens / decodeSeconds else 0.0
        generazioni++
        // `gen` e `vive` servono a leggere il degrado: se la velocita' cala
        // mentre gen sale, e vive resta 1, l'accumulo NON e' nelle
        // conversazioni e va cercato dentro la libreria.
        val vive = conversazioniCreate - conversazioniChiuse
        Log.i(
            TAG,
            "MISURA gen=$generazioni backend=$activeBackend " +
                "primoToken=${firstTokenSec?.let { "%.2f s".format(it) } ?: "mai"} " +
                "totale=${"%.2f s".format((now - startedAt) / 1000.0)} " +
                "tokenPrompt~$promptTokens tokenGenerati~$generatedTokens " +
                "velocita~${"%.1f".format(tokensPerSecond)} token/s (stima) " +
                "conversazioni: create=$conversazioniCreate chiuse=$conversazioniChiuse " +
                "vive=$vive chiusureFallite=$chiusureFallite " +
                // Se la memoria sale di scatto e `motori` sale con lei, il
                // colpevole è il ciclo di caricamento, non le generazioni.
                "motori: creati=$motoriCreati chiusi=$motoriChiusi " +
                "vivi=${motoriCreati - motoriChiusi} falliti=$chiusureMotoreFallite " +
                "heap=${usedHeapMb()}MB nativa=${nativeHeapMb()}MB pss=${pssTotaleMb()}MB " +
                "batteria=${batteryPercent()}% temp=${"%.1f".format(batteryTempCelsius())}°C",
        )
    }

    // Percentuale e temperatura vengono dallo sticky broadcast di sistema
    // (nessun permesso richiesto): stessa riga MISURA di sempre, così un
    // run lungo copre in un colpo solo sia il termico (velocità/temp) sia
    // il drain di batteria (CRITICITA.md C3, osservazione di Michele
    // 20/07/2026: "su un gioco che tiene la GPU occupata conta più della
    // memoria" — 22/07/2026, "per controllare anche il drain cosa
    // dobbiamo fare?").
    private fun batteryIntent(): Intent? =
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    private fun batteryPercent(): Int {
        val intent = batteryIntent() ?: return -1
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) level * 100 / scale else -1
    }

    // EXTRA_TEMPERATURE è in decimi di grado Celsius (STATO.md: si
    // serializzano i fatti, qui si converte solo per leggibilità nel log).
    private fun batteryTempCelsius(): Double {
        val tenths = batteryIntent()?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        return if (tenths >= 0) tenths / 10.0 else -1.0
    }

    override suspend fun unload() = withContext(Dispatchers.IO) {
        unloadInternal()
        _tokenInfo.value = TokenInfo(used = 0, maxTokens = config.maxTokens)
    }

    // Stesso trattamento dato a `newSession()` il 20/07/2026, che qui non
    // era mai arrivato: l'esito di close() veniva SCARTATO. Se la
    // chiusura del motore falliva — ed è il motore, non una conversazione:
    // sono i megabyte del modello — non lo sapeva nessuno, e il posto in
    // cui il leak si vedrebbe è esattamente questo.
    private fun unloadInternal() {
        conversation?.let { vecchia ->
            runCatching { vecchia.close() }
                .onSuccess { conversazioniChiuse++ }
                .onFailure { errore ->
                    chiusureFallite++
                    Log.e(TAG, "CHIUSURA CONVERSAZIONE FALLITA (#$chiusureFallite): ${errore.message}", errore)
                }
        }
        engine?.let { vecchio ->
            val primaMb = nativeHeapMb()
            runCatching { vecchio.close() }
                .onSuccess {
                    motoriChiusi++
                    // Il prima/dopo attorno alla close: se il motore si
                    // chiude "bene" ma la nativa non scende, il leak è
                    // dentro la libreria e si vede qui in una riga sola.
                    Log.i(TAG, "Motore chiuso (#$motoriChiusi): nativa ${primaMb}MB -> ${nativeHeapMb()}MB")
                }
                .onFailure { errore ->
                    chiusureMotoreFallite++
                    Log.e(TAG, "CHIUSURA MOTORE FALLITA (#$chiusureMotoreFallite): ${errore.message}", errore)
                }
        }
        conversation = null
        engine = null
        activeBackend = "—"
    }

    // Il testo di un Message sta nei suoi Content di tipo Text.
    private fun com.google.ai.edge.litertlm.Message.text(): String =
        contents.contents.filterIsInstance<Content.Text>().joinToString("") { it.text }

    // La memoria è l'altra faccia dell'accumulo: una KV cache che non
    // viene liberata si vede crescere qui, e soprattutto nella memoria
    // NATIVA — il modello vive lì, non nell'heap Java.
    private fun usedHeapMb(): Long {
        val rt = Runtime.getRuntime()
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
    }

    private fun nativeHeapMb(): Long =
        android.os.Debug.getNativeHeapAllocatedSize() / (1024 * 1024)

    // Il PSS accanto alla nativa (03/08/2026). `getNativeHeapAllocatedSize`
    // conta SOLO l'heap nativo preso con malloc: quello che il driver
    // grafico mappa per la GPU — e il modello gira su GPU — può non
    // comparirci affatto. Se per settimane si è guardato quel numero, si
    // è potuta guardare la metà sbagliata del problema. Il PSS totale
    // comprende tutto quello che il processo occupa davvero.
    private fun pssTotaleMb(): Long =
        android.os.Debug.MemoryInfo().also { android.os.Debug.getMemoryInfo(it) }.totalPss / 1024L

    // STIMA, non conteggio: la libreria non espone un tokenizer pubblico.
    // Serve solo al semaforo (verde/giallo/rosso), che è un'indicazione
    // di massima. Da sostituire se l'API esporrà il conteggio vero.
    private fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    private companion object {
        const val TAG = "LiteRtLmEngine"
    }
}
