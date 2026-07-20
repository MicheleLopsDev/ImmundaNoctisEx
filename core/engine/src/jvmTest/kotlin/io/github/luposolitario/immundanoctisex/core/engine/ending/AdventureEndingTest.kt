package io.github.luposolitario.immundanoctisex.core.engine.ending

import io.github.luposolitario.immundanoctisex.core.data.model.EndingOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdventureEndingTest {

    private fun scene(
        id: String,
        type: SceneType = SceneType.ENDING,
        outcome: EndingOutcome? = null,
    ) = Scene(
        id = id,
        sceneType = type,
        genre = "fantasy",
        narrativeText = "testo",
        outcome = outcome,
    )

    private fun manifest(
        deathSceneId: String? = null,
        scenes: List<Scene> = emptyList(),
    ) = Manifest(
        id = "libro",
        version = "1",
        title = "Libro di prova",
        description = "",
        language = "en",
        genre = "fantasy",
        deathSceneId = deathSceneId,
        scenes = scenes,
    )

    @Test
    fun laVittoriaDichiarataDallAutoreSiRispetta() {
        val fine = scene("10", outcome = EndingOutcome.VICTORY)
        val m = manifest(deathSceneId = "99", scenes = listOf(fine))
        assertEquals(EndingOutcome.VICTORY, AdventureEnding.outcomeOf(m, fine))
    }

    @Test
    fun laScenaDiMorteEsconfittaAncheSenzaCampoDichiarato() {
        // La morte built-in il motore la conosce gia': non serve che
        // l'autore la ridichiari.
        val morte = scene("99")
        val m = manifest(deathSceneId = "99", scenes = listOf(morte))
        assertEquals(EndingOutcome.DEFEAT, AdventureEnding.outcomeOf(m, morte))
    }

    @Test
    fun laMorteBattelEsitoDichiaratoDallAutore() {
        // Se l'autore marca VICTORY la scena che e' anche deathSceneId,
        // vince la morte: ci si arriva morendo.
        val morte = scene("99", outcome = EndingOutcome.VICTORY)
        val m = manifest(deathSceneId = "99", scenes = listOf(morte))
        assertEquals(EndingOutcome.DEFEAT, AdventureEnding.outcomeOf(m, morte))
    }

    @Test
    fun senzaDichiarazioneEneutraleNonSiInventaUnaVittoria() {
        val fine = scene("10")
        val m = manifest(deathSceneId = "99", scenes = listOf(fine))
        assertEquals(EndingOutcome.NEUTRAL, AdventureEnding.outcomeOf(m, fine))
    }

    @Test
    fun unaScenaNormaleNonHaEsito() {
        val normale = scene("2", type = SceneType.TRANSITION)
        val m = manifest(scenes = listOf(normale))
        assertEquals(EndingOutcome.NEUTRAL, AdventureEnding.outcomeOf(m, normale))
    }

    // --- La garanzia: una scena di morte esiste SEMPRE ---

    @Test
    fun senzaDeathSceneIdSeNeFabbricaUna() {
        val m = manifest(deathSceneId = null, scenes = listOf(scene("1", SceneType.START)))
        val completo = AdventureEnding.withGuaranteedEnding(m)

        assertEquals(AdventureEnding.SYNTHETIC_DEFEAT_SCENE_ID, completo.deathSceneId)
        val fabbricata = completo.scenes.firstOrNull { it.id == completo.deathSceneId }
        assertNotNull(fabbricata, "la scena di morte deve esistere davvero nel grafo")
        assertEquals(SceneType.ENDING, fabbricata.sceneType)
        assertEquals(EndingOutcome.DEFEAT, AdventureEnding.outcomeOf(completo, fabbricata))
    }

    @Test
    fun unDeathSceneIdCheNonEsisteVieneRimpiazzato() {
        // Il caso che lasciava il gioco fermo: il salto non trovava
        // destinazione e non succedeva nulla.
        val m = manifest(deathSceneId = "non-esisto", scenes = listOf(scene("1", SceneType.START)))
        val completo = AdventureEnding.withGuaranteedEnding(m)

        assertEquals(AdventureEnding.SYNTHETIC_DEFEAT_SCENE_ID, completo.deathSceneId)
        assertTrue(completo.scenes.any { it.id == completo.deathSceneId })
    }

    @Test
    fun unPacchettoGiaCompletoNonSiTocca() {
        val morte = scene("99")
        val m = manifest(deathSceneId = "99", scenes = listOf(scene("1", SceneType.START), morte))
        assertEquals(m, AdventureEnding.withGuaranteedEnding(m))
    }

    @Test
    fun laScenaFabbricataNasceSenzaTestoPerLasciarloAlNarratore() {
        val m = manifest(deathSceneId = null, scenes = listOf(scene("1", SceneType.START)))
        val completo = AdventureEnding.withGuaranteedEnding(m)
        val fabbricata = completo.scenes.first { it.id == completo.deathSceneId }
        assertEquals("", fabbricata.narrativeText)
    }

    @Test
    fun applicarlaDueVolteNonAggiungeDoppioni() {
        val m = manifest(deathSceneId = null, scenes = listOf(scene("1", SceneType.START)))
        val unaVolta = AdventureEnding.withGuaranteedEnding(m)
        val dueVolte = AdventureEnding.withGuaranteedEnding(unaVolta)
        assertEquals(unaVolta, dueVolte)
    }
}
