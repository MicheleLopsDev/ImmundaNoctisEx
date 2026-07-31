package io.github.luposolitario.immundanoctisex.sfx

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

// Solo il percorso "cache-hit" (nessuna richiesta di rete reale, il
// progetto non ha Robolectric/mock per :app): un download vero va
// verificato a mano su device.
class SfxDownloadCacheTest {

    private lateinit var dir: File

    @BeforeTest
    fun setup() {
        dir = createTempDirectory("sfx-cache-test").toFile()
    }

    @AfterTest
    fun cleanup() {
        dir.deleteRecursively()
    }

    @Test
    fun cacheKeyForEDeterministicaPerLoStessoUrl() {
        val url = "https://example.invalid/a.mp3"
        assertEquals(SfxDownloadCache.cacheKeyFor(url), SfxDownloadCache.cacheKeyFor(url))
    }

    @Test
    fun cacheKeyForDistinguesUrlDiversi() {
        assertNotEquals(
            SfxDownloadCache.cacheKeyFor("https://example.invalid/a.mp3"),
            SfxDownloadCache.cacheKeyFor("https://example.invalid/b.mp3"),
        )
    }

    @Test
    fun unFileGiaInCacheVieneRitornatoSenzaScaricareDiNuovo() = runBlocking {
        val cache = SfxDownloadCache(dir)
        val url = "https://example.invalid/gia-in-cache.mp3"
        val key = SfxDownloadCache.cacheKeyFor(url)
        val giaPresente = File(dir, "$key.mp3").apply { writeText("finto mp3") }

        val risultato = cache.localFileFor(url)

        assertEquals(giaPresente.absolutePath, risultato?.absolutePath)
        assertEquals("finto mp3", risultato?.readText())
    }

    @Test
    fun unUrlInvalidoDegradaSuNullSenzaEccezioni() = runBlocking {
        val cache = SfxDownloadCache(dir)

        val risultato = cache.localFileFor("non-e-un-url-valido")

        assertNull(risultato)
    }
}
