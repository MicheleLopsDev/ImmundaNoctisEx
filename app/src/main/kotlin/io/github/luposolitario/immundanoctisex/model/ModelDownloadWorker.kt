package io.github.luposolitario.immundanoctisex.model

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.StatFs
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

// Scarica il modello in background con notifica (pattern DownloadWorker
// di v1, con i suoi difetti corretti — vedi commenti):
//  - il token è OPZIONALE: v1 falliva subito se mancava, ma i modelli
//    aperti non ne hanno bisogno;
//  - download RIPRENDIBILE: v1 cancellava il parziale a ogni errore e
//    rifaceva 3,6 GB da capo. Qui il .part sopravvive e si riprende con
//    una richiesta Range;
//  - un solo flusso invece di 8 connessioni parallele: su mobile la
//    ripresa vale più della velocità di picco, e HF non ama 8 range
//    simultanei;
//  - controllo dello SPAZIO DISCO prima di iniziare;
//  - verifica della dimensione finale prima di promuovere il file
//    (un 401 salvato come modello è il modo peggiore di fallire);
//  - stream chiusi davvero (v1 lasciava aperto un RandomAccessFile).
class ModelDownloadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val urlString = inputData.getString(KEY_URL) ?: return@withContext failure("URL mancante")
        val destinationPath = inputData.getString(KEY_DESTINATION)
            ?: return@withContext failure("destinazione mancante")
        val expectedSize = inputData.getLong(KEY_EXPECTED_SIZE, 0L)
        // Opzionale per costruzione: i repo aperti non lo richiedono.
        val token = inputData.getString(KEY_TOKEN)?.takeIf { it.isNotBlank() }

        val finalFile = File(destinationPath)
        val partFile = File("$destinationPath.part")
        finalFile.parentFile?.mkdirs()

        try {
            setForeground(foregroundInfo(0))

            val alreadyDownloaded = if (partFile.exists()) partFile.length() else 0L
            val connection = openConnection(urlString, token, resumeFrom = alreadyDownloaded)

            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                connection.disconnect()
                return@withContext failure(
                    "Il modello è su un repository riservato: serve un token Hugging Face " +
                        "valido e la licenza accettata.",
                )
            }
            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                connection.disconnect()
                return@withContext failure("Il server ha risposto $code")
            }
            // Un link alla PAGINA del repo (non al file, niente
            // "…resolve/main/…") risponde 200 con una paginetta HTML: senza
            // questo controllo veniva scaricata e promossa a "modello" in
            // meno di un secondo (bug 22/07, segnalato da Michele — "dice
            // già scaricato" dopo un download impossibilmente veloce).
            if (connection.contentType?.startsWith("text/html", ignoreCase = true) == true) {
                connection.disconnect()
                return@withContext failure(
                    "Il link punta a una pagina web, non al file del modello. Serve il link " +
                        "DIRETTO (pagina del repo → file → \"…resolve/main/…\").",
                )
            }

            // Se il server ignora la richiesta di ripresa (200 invece di
            // 206) si riparte da zero: meglio che scrivere in coda a un
            // parziale i byte sbagliati.
            val resuming = code == HttpURLConnection.HTTP_PARTIAL
            val startFrom = if (resuming) alreadyDownloaded else 0L
            val totalSize = when {
                expectedSize > 0L -> expectedSize
                connection.contentLengthLong > 0L -> connection.contentLengthLong + startFrom
                else -> 0L
            }

            if (!hasEnoughSpace(finalFile, totalSize - startFrom)) {
                connection.disconnect()
                return@withContext failure("Spazio insufficiente sul telefono per scaricare il modello.")
            }

            var downloaded = startFrom
            connection.inputStream.use { input ->
                FileOutputStream(partFile, resuming).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var read = input.read(buffer)
                    var lastNotified = 0L
                    while (read != -1) {
                        // La cancellazione lascia il .part dov'è: alla
                        // ripresa non si ricomincia da capo.
                        if (isStopped) return@withContext Result.failure()
                        output.write(buffer, 0, read)
                        downloaded += read

                        if (downloaded - lastNotified >= PROGRESS_STEP) {
                            lastNotified = downloaded
                            setProgress(
                                workDataOf(KEY_BYTES_DOWNLOADED to downloaded, KEY_TOTAL_BYTES to totalSize),
                            )
                            setForeground(foregroundInfo(percentOf(downloaded, totalSize)))
                        }
                        read = input.read(buffer)
                    }
                }
            }
            connection.disconnect()

            // Verifica prima di promuovere: un file troncato non deve mai
            // passare per un modello valido.
            if (totalSize > 0L && partFile.length() != totalSize) {
                return@withContext failure(
                    "Download incompleto (${partFile.length()} di $totalSize byte). Riprova per riprenderlo.",
                )
            }
            // Dimensione ignota (modelli personalizzati: sizeBytes=0 finché
            // non si scarica) -> il controllo sopra non scatta mai. Un vero
            // modello LLM pesa comunque centinaia di MB: qualunque cosa
            // sotto questa soglia è quasi certamente un errore scaricato
            // per sbaglio (pagina d'errore, redirect non seguito, ecc.),
            // non il file giusto.
            if (totalSize <= 0L && partFile.length() < MIN_PLAUSIBLE_MODEL_BYTES) {
                return@withContext failure(
                    "Il file scaricato è troppo piccolo (${partFile.length()} byte) per essere " +
                        "un modello: probabilmente il link non è quello giusto.",
                )
            }

            finalFile.delete()
            if (!partFile.renameTo(finalFile)) return@withContext failure("Impossibile completare il file")

            Result.success(workDataOf(KEY_DESTINATION to finalFile.absolutePath))
        } catch (e: Exception) {
            // Il parziale RESTA: il prossimo tentativo riprende da lì.
            Log.e(TAG, "Download interrotto: ${e.message}", e)
            failure("Download interrotto: ${e.message ?: "errore di rete"}")
        }
    }

    private fun openConnection(urlString: String, token: String?, resumeFrom: Long): HttpURLConnection =
        (URL(urlString).openConnection() as HttpURLConnection).apply {
            // Hugging Face redirige su CDN: va seguito.
            instanceFollowRedirects = true
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
            if (resumeFrom > 0L) setRequestProperty("Range", "bytes=$resumeFrom-")
            connect()
        }

    private fun hasEnoughSpace(target: File, needed: Long): Boolean {
        if (needed <= 0L) return true
        val stat = StatFs((target.parentFile ?: target).absolutePath)
        return stat.availableBytes > needed + SPACE_MARGIN
    }

    private fun percentOf(downloaded: Long, total: Long): Int =
        if (total <= 0L) 0 else ((downloaded * 100) / total).toInt().coerceIn(0, 100)

    private fun failure(message: String): Result {
        Log.e(TAG, message)
        return Result.failure(workDataOf(KEY_ERROR to message))
    }

    private fun foregroundInfo(percent: Int): ForegroundInfo {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Download modello", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Scaricamento del modello")
            .setContentText("$percent%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, percent, percent == 0)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val WORK_NAME = "model_download"
        const val KEY_URL = "url"
        const val KEY_DESTINATION = "destination"
        const val KEY_TOKEN = "token"
        const val KEY_EXPECTED_SIZE = "expected_size"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_ERROR = "error"

        private const val TAG = "ModelDownloadWorker"
        private const val CHANNEL_ID = "model_download_channel"
        private const val NOTIFICATION_ID = 1001
        private const val BUFFER_SIZE = 256 * 1024
        private const val PROGRESS_STEP = 2L * 1024 * 1024
        private const val TIMEOUT_MS = 30_000
        private const val SPACE_MARGIN = 200L * 1024 * 1024
        private const val MIN_PLAUSIBLE_MODEL_BYTES = 10L * 1024 * 1024
    }
}
