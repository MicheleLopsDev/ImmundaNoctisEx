package io.github.luposolitario.immundanoctisex.core.engine.sfx

import io.github.luposolitario.immundanoctisex.core.data.model.CustomResourceEntry
import io.github.luposolitario.immundanoctisex.core.data.model.CustomResources
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SceneSfxResolverTest {

    private fun scene(sfx: String? = null) = Scene(
        id = "1",
        sceneType = SceneType.TRANSITION,
        genre = "fantasy",
        narrativeText = "testo",
        sfx = sfx,
    )

    private fun manifest(sounds: List<CustomResourceEntry> = emptyList()) = Manifest(
        id = "libro",
        version = "1",
        title = "Libro di prova",
        description = "",
        language = "en",
        genre = "fantasy",
        customResources = CustomResources(sounds = sounds),
    )

    @Test
    fun senzaSfxRestituisceNull() {
        assertNull(SceneSfxResolver.resolve(scene(sfx = null), manifest()))
    }

    @Test
    fun conSfxRegistratoRestituisceIlValore() {
        val m = manifest(sounds = listOf(CustomResourceEntry("mio_suono", "url:https://example.invalid/a.mp3")))

        assertEquals("url:https://example.invalid/a.mp3", SceneSfxResolver.resolve(scene(sfx = "mio_suono"), m))
    }

    // Irraggiungibile in produzione (SfxValidator lo impedisce prima che
    // il Manifest arrivi qui), ma la funzione resta totale: mai
    // un'eccezione, solo null.
    @Test
    fun conSfxNonRegistratoRestituisceNullSenzaEccezioni() {
        assertNull(SceneSfxResolver.resolve(scene(sfx = "sconosciuto"), manifest()))
    }
}
