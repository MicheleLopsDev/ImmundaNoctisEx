package io.github.luposolitario.immundanoctisex.tool.inference

import io.github.luposolitario.immundanoctisex.tool.editor.EditorLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

// Scaricamento dei modelli dentro l'editor (02/08/2026, Michele:
// "aggiungi le stesse impostazioni che permettono di scaricare anche 2b
// e che puoi mettere l'url o il default di link da huggingface").
//
// Nessuna libreria nuova: `java.net.http` è nel JDK, e da quando :tool
// gira su Java 21 c'è tutto il necessario. I link sono quelli del
// client (ModelCatalog in :core:engine): un modello scaricato qui è
// byte per byte quello che gira sul telefono.
class ModelDownloader {

    // I redirect vanno seguiti: gli URL `resolve/main/...` di
    // HuggingFace rimandano sempre alla CDN, e senza questo si
    // scaricherebbe una paginetta HTML da qualche centinaio di byte
    // scambiandola per un modello.
    private val client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    // Byte totali dichiarati dal server, senza scaricare nulla: una
    // barra onesta invece di una che si muove a caso.
    suspend fun dimensioneRemota(url: String): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val richiesta = HttpRequest.newBuilder(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build()
            client.send(richiesta, HttpResponse.BodyHandlers.discarding())
                .headers().firstValueAsLong("content-length").orElse(-1L).takeIf { it > 0 }
        }.getOrNull()
    }

    // Scarica su un file `.parziale` e rinomina solo alla fine: un
    // `.litertlm` monco che sembra buono e fallisce misteriosamente al
    // caricamento è il guasto peggiore possibile. Stessa disciplina
    // della scrittura atomica dei salvataggi nel client.
    //
    // Il `.parziale` viene CONSERVATO quando l'interruzione non è
    // volontaria (02/08/2026, Michele: "si blocca al 19%"): al tentativo
    // dopo si riparte da lì con una richiesta `Range` invece di buttare
    // via 700 MB. Su file da gigabyte una connessione che cade a metà è
    // la norma, non l'eccezione.
    suspend fun scarica(
        url: String,
        destinazione: File,
        onProgresso: (scaricati: Long, totale: Long) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        val parziale = File(destinazione.parentFile, destinazione.name + ".parziale")
        var completato = false

        val esito = runCatching {
            destinazione.parentFile?.mkdirs()
            val giaPresenti = if (parziale.exists()) parziale.length() else 0L
            EditorLog.i(
                TAG,
                "Download di ${destinazione.name} da $url" +
                    if (giaPresenti > 0) " — ripresa da ${mega(giaPresenti)} MB" else "",
            )

            val costruttore = HttpRequest.newBuilder(URI.create(url)).GET()
            if (giaPresenti > 0) costruttore.header("Range", "bytes=$giaPresenti-")
            val risposta = client.send(costruttore.build(), HttpResponse.BodyHandlers.ofInputStream())

            val codice = risposta.statusCode()
            EditorLog.i(TAG, "Risposta HTTP $codice da ${risposta.uri().host}")
            if (codice !in 200..299) {
                error(
                    "Il server ha risposto $codice. Se il modello richiede un token di accesso, " +
                        "scaricalo dal browser.",
                )
            }
            // 206 = ripresa accettata; 200 con un Range chiesto = il
            // server l'ha ignorato e rimanda tutto, quindi quel che c'era
            // non vale più.
            val riprende = codice == 206
            if (giaPresenti > 0 && !riprende) {
                EditorLog.i(TAG, "Ripresa ignorata dal server (HTTP 200): si ricomincia da capo")
            }
            val partenza = if (riprende) giaPresenti else 0L
            val dichiarati = risposta.headers().firstValueAsLong("content-length").orElse(-1L)
            val totale = if (dichiarati > 0) partenza + dichiarati else -1L

            val scaricati = trasferisci(risposta.body(), parziale, riprende, partenza, totale, destinazione.name, onProgresso)

            // Un HTML di errore travestito da modello pesa qualche KB.
            if (scaricati < MINIMO_PLAUSIBILE) {
                error("Scaricati solo $scaricati byte: il link non punta a un modello.")
            }
            // Un file troncato non deve diventare un .litertlm valido
            // all'apparenza: se il server aveva dichiarato una dimensione,
            // deve tornare.
            if (totale > 0 && scaricati < totale) {
                error(
                    "Ricevuti ${mega(scaricati)} MB dei ${mega(totale)} MB attesi: connessione " +
                        "interrotta. Riprova: il download riprende da qui.",
                )
            }
            destinazione.delete()
            check(parziale.renameTo(destinazione)) {
                "Impossibile rinominare il file scaricato in ${destinazione.name}"
            }
            completato = true
            EditorLog.i(TAG, "Completato: ${destinazione.absolutePath} (${mega(scaricati)} MB)")
            destinazione
        }

        esito.onFailure { errore ->
            // Alcune eccezioni di rete (ConnectException in testa) hanno
            // `message` nullo: senza il nome della classe il log direbbe
            // solo "non riuscito: null", cioè niente.
            EditorLog.e(TAG, "Download di ${destinazione.name} non riuscito: ${descriviErrore(errore)}", errore)
        }
        // Il `.parziale` resta apposta: è il punto da cui ripartire. Si
        // butta solo se è troppo piccolo per far risparmiare qualcosa.
        if (!completato && parziale.exists() && parziale.length() < MINIMO_PLAUSIBILE) parziale.delete()
        esito
    }

    // Il ciclo di lettura, con il sorvegliante che rende accorgibile uno
    // stallo. `read()` su uno stream bloccante NON torna mai 0: se la CDN
    // smette di mandare byte senza chiudere, resta appesa in silenzio
    // per sempre — ed è esattamente il "si blocca al 19%". L'unico modo
    // di sbloccarla dall'esterno è chiudere lo stream sotto di lei.
    private suspend fun trasferisci(
        ingresso: InputStream,
        parziale: File,
        riprende: Boolean,
        partenza: Long,
        totale: Long,
        nome: String,
        onProgresso: (Long, Long) -> Unit,
    ): Long = coroutineScope {
        val scaricati = AtomicLong(partenza)
        val ultimoAvanzamento = AtomicLong(System.currentTimeMillis())

        val sorvegliante = launch {
            while (isActive) {
                delay(CONTROLLO_STALLO_MS)
                if (System.currentTimeMillis() - ultimoAvanzamento.get() > STALLO_MS) {
                    EditorLog.e(TAG, "Nessun dato per ${STALLO_MS / 1000}s a ${mega(scaricati.get())} MB: chiudo la connessione")
                    runCatching { ingresso.close() }
                    break
                }
            }
        }

        try {
            ingresso.use { flusso ->
                FileOutputStream(parziale, riprende).buffered().use { uscita ->
                    val buffer = ByteArray(1 shl 20) // 1 MB: file da GB, letture grandi
                    var prossimoLog = partenza + PASSO_LOG
                    while (true) {
                        // L'annullamento dell'utente arriva come
                        // cancellazione della coroutine.
                        coroutineContext.ensureActive()
                        val letti = flusso.read(buffer)
                        if (letti < 0) break
                        uscita.write(buffer, 0, letti)
                        val fatti = scaricati.addAndGet(letti.toLong())
                        ultimoAvanzamento.set(System.currentTimeMillis())
                        onProgresso(fatti, totale)
                        if (fatti >= prossimoLog) {
                            EditorLog.i(TAG, "$nome: ${mega(fatti)} MB" + if (totale > 0) " di ${mega(totale)} MB" else "")
                            prossimoLog = fatti + PASSO_LOG
                        }
                    }
                }
            }
        } finally {
            sorvegliante.cancel()
        }
        scaricati.get()
    }

    companion object {
        const val TAG = "ModelDownloader"
        const val PASSO_LOG = 100L * 1024 * 1024 // una riga di log ogni 100 MB
        const val STALLO_MS = 90_000L
        const val CONTROLLO_STALLO_MS = 15_000L
        const val MINIMO_PLAUSIBILE = 1_000_000L

        fun mega(byte: Long): Long = byte / (1024 * 1024)

        // Un messaggio sempre dicibile a voce, anche quando l'eccezione
        // non ne porta uno.
        fun descriviErrore(errore: Throwable): String =
            errore.message?.takeIf { it.isNotBlank() } ?: when (errore) {
                is java.net.ConnectException -> "impossibile connettersi al server"
                is java.net.UnknownHostException -> "indirizzo non risolto: controlla la connessione"
                is java.io.IOException -> "connessione interrotta durante il trasferimento"
                else -> errore::class.simpleName ?: "errore sconosciuto"
            }
    }
}
