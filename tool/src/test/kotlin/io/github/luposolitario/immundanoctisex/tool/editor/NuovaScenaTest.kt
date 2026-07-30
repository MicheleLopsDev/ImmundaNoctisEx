package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NuovaScenaTest {

    private fun scene(id: String, vararg destinazioni: String) = Scene(
        id = id,
        sceneType = SceneType.TRANSITION,
        genre = "FANTASY",
        narrativeText = "testo",
        choices = destinazioni.mapIndexed { i, dest -> Choice("c$i", "vai", nextSceneId = dest) },
    )

    private fun manifest(scenes: List<Scene>, deathSceneId: String? = "morte") = Manifest(
        id = "test", version = "1.0.0", title = "Test", description = "Test",
        language = "en", genre = "FANTASY", deathSceneId = deathSceneId, scenes = scenes,
    )

    @Test
    fun ilProssimoIdENumericoESuccessivoAlPiuAlto() {
        val m = manifest(listOf(scene("1", "2"), scene("2"), scene("7")))
        assertEquals("8", nuovaScenaVuota(m).id)
    }

    @Test
    fun senzaScenePartendoDaZeroIlProssimoIdE1() {
        val m = manifest(emptyList())
        assertEquals("1", nuovaScenaVuota(m).id)
    }

    @Test
    fun unaScenaSoloDiSconfittaNonBloccaLaNumerazione() {
        val m = manifest(listOf(Scene("morte", SceneType.ENDING, "FANTASY", narrativeText = "fine")))
        assertEquals("1", nuovaScenaVuota(m).id)
    }

    @Test
    fun laNuovaScenaEDiTipoTransizioneESenzaTesto() {
        val nuova = nuovaScenaVuota(manifest(emptyList()))
        assertEquals(SceneType.TRANSITION, nuova.sceneType)
        assertTrue(nuova.narrativeText.isEmpty())
    }

    @Test
    fun laReteDiSicurezzaAgganciaAllaSceneDiSconfittaSeNonCESonoUscite() {
        val vuota = scene("5")
        val risultato = conReteDiSicurezza(vuota, "morte")
        assertEquals(listOf("morte"), risultato.choices.map { it.nextSceneId })
    }

    @Test
    fun laReteDiSicurezzaNonTocaUnaScenaConGiaUnaScelta() {
        val conScelta = scene("5", "6")
        val risultato = conReteDiSicurezza(conScelta, "morte")
        assertEquals(conScelta, risultato)
    }

    @Test
    fun laReteDiSicurezzaNonFaNullaSenzaSceneDiSconfittaDichiarata() {
        val vuota = scene("5")
        val risultato = conReteDiSicurezza(vuota, deathSceneId = null)
        assertEquals(vuota, risultato)
    }
}
