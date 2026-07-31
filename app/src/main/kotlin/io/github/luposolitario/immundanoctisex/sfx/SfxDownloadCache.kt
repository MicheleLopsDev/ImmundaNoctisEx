package io.github.luposolitario.immundanoctisex.sfx

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

// Scene.sfx con un valore url: (31/07/2026, doc/UPGRADE.md §7): a
// differenza delle immagini url: (Coil scarica e cacha da solo in
// streaming per AsyncImage), SoundPool richiede un FILE locale — questa
// classe scarica e mette in cache un mp3 da rete, molto più semplice di
// ModelDownloadWorker.kt (che gestisce file da GB con resume/notifica
// foreground): qui i file sono di poche centinaia di KB, niente resume
// serve.
//
// Qualunque fallimento (rete assente, link morto, timeout) degrada in
// silenzio (getOrNull) — decisione esplicita di Michele: se un url: non
// scarica, quella scena resta senza sfx personalizzato, nessun fallback.
//
// Costruttore su una `File` di destinazione, non su un `Context` Android
// (chi la costruisce passa `File(context.cacheDir, "sfx-cache")",
// SoundEffectPlayer.kt): testabile in JVM puro senza Robolectric/mock,
// stesso principio già usato altrove nel progetto per il codice di I/O su
// file (es. BookStorage.kt lato :tool).
class SfxDownloadCache(private val cacheDir: File) {

    init {
        cacheDir.mkdirs()
    }

    // Un Mutex per chiave di cache: evita due download paralleli identici
    // se la stessa scena (stesso url) viene rivisitata rapidamente prima
    // che il primo download finisca.
    private val locks = ConcurrentHashMap<String, Mutex>()

    // Ritorna il file locale pronto da suonare, scaricandolo se necessario.
    // null se il download fallisce per qualunque motivo.
    suspend fun localFileFor(url: String): File? = withContext(Dispatchers.IO) {
        val key = cacheKeyFor(url)
        val target = File(cacheDir, "$key.mp3")
        if (target.exists()) return@withContext target
        locks.getOrPut(key) { Mutex() }.withLock {
            if (target.exists()) return@withLock target
            runCatching { download(url, target) }.getOrNull()
        }
    }

    private fun download(url: String, target: File): File {
        val temp = File(cacheDir, "${target.name}.tmp")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            connect()
        }
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "HTTP ${connection.responseCode} per $url"
            }
            connection.inputStream.use { input ->
                temp.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var total = 0L
                    var read = input.read(buffer)
                    while (read != -1) {
                        total += read
                        // Guardia difensiva: un url: fornito dall'autore
                        // di un libro di terze parti non deve poter
                        // riempire lo storage del device.
                        check(total <= MAX_BYTES) { "sfx troppo grande (> ${MAX_BYTES / 1024 / 1024} MB)" }
                        output.write(buffer, 0, read)
                        read = input.read(buffer)
                    }
                }
            }
            // Scrittura atomica (stesso pattern di FileSessionStore.kt):
            // mai un mp3 a metà se il processo muore a metà scrittura.
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            return target
        } finally {
            connection.disconnect()
            temp.delete()
        }
    }

    companion object {
        // Hash dell'URL, MAI l'ID scelto dall'autore: due libri diversi
        // potrebbero riusare lo stesso ID di customResources.sounds per
        // url diversi, l'hash dell'URL non collide mai per costruzione.
        fun cacheKeyFor(url: String): String =
            MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
                .joinToString("") { "%02x".format(it) }

        private const val TIMEOUT_MS = 15_000
        private const val BUFFER_SIZE = 64 * 1024
        private const val MAX_BYTES = 5L * 1024 * 1024
    }
}
