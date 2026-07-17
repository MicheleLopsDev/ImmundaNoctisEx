package io.github.luposolitario.immundanoctisex.core.data.pkg

import io.github.luposolitario.immundanoctisex.core.data.StringPackageSource
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Milestone Fase 1 (doc/PIANO-SVILUPPO.md): il sample viene caricato e
// validato da un test JVM.
class PackageRepositoryTest {

    private fun sampleJson(): String {
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream("scenes.sample.json")) {
            "fixture scenes.sample.json non trovata sul classpath di test"
        }
        return stream.bufferedReader().use { it.readText() }
    }

    @Test
    fun ilSampleSiCaricaESiValida() {
        val repository = PackageRepository(StringPackageSource(sampleJson()))

        val result = repository.load()

        val success = assertIs<PackageLoadResult.Success>(result)
        assertTrue(success.warnings.isEmpty())
        assertEquals("sample-adventure", success.manifest.id)
        assertEquals(7, success.manifest.scenes.size)
    }

    @Test
    fun laScenaDiStartESeneRaggiungibiliSiNavigano() {
        val repository = PackageRepository(StringPackageSource(sampleJson()))
        repository.load()

        assertEquals("1", repository.startScene()?.id)
        assertEquals(SceneType.ENDING, repository.getSceneById("6")?.sceneType)
        assertEquals("6", repository.getSceneById("4")?.combat?.winSceneId)
        assertNull(repository.getSceneById("scena-inesistente"))
    }

    @Test
    fun unPacchettoConJsonRottoVieneRigettato() {
        val repository = PackageRepository(StringPackageSource("{ questo non e' json valido"))

        val result = repository.load()

        val failure = assertIs<PackageLoadResult.Failure>(result)
        assertTrue(failure.errors.single().contains("JSON malformato"))
    }
}
