package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import io.github.luposolitario.immundanoctisex.core.data.validation.PackageValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookScaffoldsTest {

    @Test
    fun ogniScaffoldProduceUnLibroValido() {
        Scaffold.entries.forEach { scaffold ->
            val manifest = creaManifestNuovo("Titolo di prova", "FANTASY", listOf("avventuroso"), scaffold)

            val risultato = PackageValidator.validate(manifest)

            assertTrue(risultato.errors.isEmpty(), "$scaffold non valido: ${risultato.errors}")
        }
    }

    @Test
    fun ogniScaffoldHaUnaScenaStartEUnaSceneMorteCollegataADeathSceneId() {
        Scaffold.entries.forEach { scaffold ->
            val manifest = creaManifestNuovo("Titolo di prova", "FANTASY", emptyList(), scaffold)

            assertTrue(manifest.scenes.any { it.sceneType == SceneType.START }, "$scaffold senza START")
            assertEquals("morte", manifest.deathSceneId)
            assertTrue(manifest.scenes.any { it.id == "morte" }, "$scaffold senza scena morte")
        }
    }

    @Test
    fun ilTitoloDiventaUnIdLeggibile() {
        val manifest = creaManifestNuovo("La Torre di Vetro!", "FANTASY", emptyList(), Scaffold.BASE)

        assertEquals("la-torre-di-vetro", manifest.id)
    }
}
