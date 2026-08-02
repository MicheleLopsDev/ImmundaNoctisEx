package io.github.luposolitario.immundanoctisex.tool.inference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
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

    // Byte totali dichiarati dal server, senza scaricare nulla. Serve a
    // mostrare una barra onesta invece di una che si muove a caso: il
    // catalogo porta una dimensione attesa, ma è quella misurata a luglio
    // e un file ricaricato a monte potrebbe non combaciare più.
    suspend fun dimensioneRemota(url: String): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val richiesta = HttpRequest.newBuilder(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build()
            val risposta = client.send(richiesta, HttpResponse.BodyHandlers.discarding())
            risposta.headers().firstValueAsLong("content-length").orElse(-1L).takeIf { it > 0 }
        }.getOrNull()
    }

    // Scarica su un file `.parziale` e rinomina solo alla fine: un
    // download interrotto (rete caduta, editor chiuso, annullamento) non
    // deve MAI lasciare un `.litertlm` incompleto che sembra buono e
    // fallisce misteriosamente al caricamento. Stessa disciplina della
    // scrittura atomica dei salvataggi nel client.
    suspend fun scarica(
        url: String,
        destinazione: File,
        onProgresso: (scaricati: Long, totale: Long) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        val parziale = File(destinazione.parentFile, destinazione.name + ".parziale")
        runCatching {
            destinazione.parentFile?.mkdirs()
            parziale.delete()

            val richiesta = HttpRequest.newBuilder(URI.create(url)).GET().build()
            val risposta = client.send(richiesta, HttpResponse.BodyHandlers.ofInputStream())
            if (risposta.statusCode() !in 200..299) {
                error("Il server ha risposto ${risposta.statusCode()}. Se il modello richiede un token di accesso, scaricalo dal browser.")
            }
            val totale = risposta.headers().firstValueAsLong("content-length").orElse(-1L)

            var scaricati = 0L
            risposta.body().use { ingresso ->
                parziale.outputStream().buffered().use { uscita ->
                    val buffer = ByteArray(1 shl 20) // 1 MB: file da GB, letture grandi
                    while (true) {
                        // L'annullamento dell'utente arriva come
                        // cancellazione della coroutine: si controlla a
                        // ogni blocco, non solo alla fine.
                        coroutineContext.ensureActive()
                        val letti = ingresso.read(buffer)
                        if (letti < 0) break
                        uscita.write(buffer, 0, letti)
                        scaricati += letti
                        onProgresso(scaricati, totale)
                    }
                }
            }

            // Un HTML di errore travestito da modello pesa qualche KB:
            // meglio accorgersene ora che al primo caricamento.
            if (scaricati < 1_000_000) {
                error("Scaricati solo $scaricati byte: il link non punta a un modello.")
            }
            destinazione.delete()
            check(parziale.renameTo(destinazione)) { "Impossibile rinominare il file scaricato in ${destinazione.name}" }
            destinazione
        }.onFailure {
            // Vale anche per l'annullamento: nessun residuo da cui
            // ripartire, ma nemmeno un file monco in giro.
            parziale.delete()
        }
    }
}
