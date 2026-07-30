package io.github.luposolitario.immundanoctisex.tool.etl

import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.CustomResourceEntry
import io.github.luposolitario.immundanoctisex.core.data.model.CustomResources
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BonificaRisorseTest {

    private fun scene(id: String, backgroundImage: String? = null, npcImage: String? = null, enemyImage: String? = null) =
        Scene(
            id = id,
            sceneType = SceneType.TRANSITION,
            genre = "FANTASY",
            narrativeText = "testo",
            backgroundImage = backgroundImage,
            npcImage = npcImage,
            combat = enemyImage?.let {
                Combat(enemyName = "nemico", enemyImage = it, enemyCombatSkill = 10, enemyEndurance = 10, winSceneId = "1")
            },
        )

    private fun manifest(scenes: List<Scene>, customResources: CustomResources = CustomResources()) = Manifest(
        id = "test", version = "1.0.0", title = "Test", description = "Test",
        language = "en", genre = "FANTASY", scenes = scenes, customResources = customResources,
    )

    @Test
    fun registraOgniUrlGiaUsatoConUnIdLeggibile() {
        val libro = manifest(
            listOf(
                scene("1", npcImage = "url:https://www.projectaon.org/en/xhtml/lw/01fftd/ill1.png"),
                scene("2", backgroundImage = "url:https://www.projectaon.org/en/xhtml/lw/01fftd/ill2.png"),
            ),
        )

        val risultato = bonificaRisorseImmagine(libro)

        assertEquals(
            setOf("ill1", "ill2"),
            risultato.customResources.images.map { it.id }.toSet(),
        )
    }

    @Test
    fun nonRegistraDueVolteLoStessoUrl() {
        val libro = manifest(
            listOf(
                scene("1", npcImage = "url:https://example.invalid/ill1.png"),
                scene("2", backgroundImage = "url:https://example.invalid/ill1.png"),
            ),
        )

        val risultato = bonificaRisorseImmagine(libro)

        assertEquals(1, risultato.customResources.images.size)
    }

    @Test
    fun ignoraLeImmaginiStatic() {
        val libro = manifest(listOf(scene("1", backgroundImage = "static:loc_alley")))

        val risultato = bonificaRisorseImmagine(libro)

        assertTrue(risultato.customResources.images.isEmpty())
    }

    @Test
    fun nonTocaLeVociGiaRegistrate() {
        val esistente = CustomResourceEntry("mio_villain", "https://example.invalid/gia-noto.png")
        val libro = manifest(
            listOf(scene("1", npcImage = "url:https://example.invalid/gia-noto.png")),
            customResources = CustomResources(images = listOf(esistente)),
        )

        val risultato = bonificaRisorseImmagine(libro)

        assertEquals(listOf(esistente), risultato.customResources.images)
    }

    @Test
    fun unNomeBaseDuplicatoDaUrlDiversiOttieneUnContatore() {
        val libro = manifest(
            listOf(
                scene("1", npcImage = "url:https://example.invalid/a/ill1.png"),
                scene("2", backgroundImage = "url:https://example.invalid/b/ill1.png"),
            ),
        )

        val risultato = bonificaRisorseImmagine(libro)

        assertEquals(setOf("ill1", "ill1-2"), risultato.customResources.images.map { it.id }.toSet())
    }

    @Test
    fun leggeAncheLImmagineDelNemicoDelCombattimento() {
        val libro = manifest(listOf(scene("1", enemyImage = "url:https://example.invalid/nemico.png")))

        val risultato = bonificaRisorseImmagine(libro)

        assertEquals(listOf("nemico"), risultato.customResources.images.map { it.id })
    }
}
