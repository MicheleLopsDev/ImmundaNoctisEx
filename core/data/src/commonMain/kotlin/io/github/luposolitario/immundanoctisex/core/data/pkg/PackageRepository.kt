package io.github.luposolitario.immundanoctisex.core.data.pkg

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import io.github.luposolitario.immundanoctisex.core.data.validation.PackageValidator
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

// Carica e valida un pacchetto libro, espone getSceneById, la scena di
// start e i metadati del manifest (ARCHITETTURA.md). Un pacchetto rotto
// (JSON malformato o che non supera i validatori) non lascia mai
// l'istanza in uno stato parzialmente caricato.
class PackageRepository(private val source: PackageSource) {

    private val json = Json { ignoreUnknownKeys = true }

    var manifest: Manifest? = null
        private set

    fun load(): PackageLoadResult {
        val text = source.open().bufferedReader().use { it.readText() }
        val parsed = try {
            json.decodeFromString(Manifest.serializer(), text)
        } catch (e: SerializationException) {
            return PackageLoadResult.Failure(listOf("JSON malformato: ${e.message}"))
        } catch (e: IllegalArgumentException) {
            return PackageLoadResult.Failure(listOf("JSON malformato: ${e.message}"))
        }

        val result = PackageValidator.validate(parsed)
        if (result.errors.isNotEmpty()) {
            return PackageLoadResult.Failure(result.errors)
        }

        manifest = parsed
        return PackageLoadResult.Success(parsed, result.warnings)
    }

    fun getSceneById(id: String): Scene? = manifest?.scenes?.find { it.id == id }

    fun startScene(): Scene? = manifest?.scenes?.find { it.sceneType == SceneType.START }
}
