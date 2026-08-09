package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.GameMechanic
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Una violazione per test, come nel resto dei validatori.
class AttivazioneValidatorTest {

    private fun meccanica(command: String, vararg params: Pair<String, String>) = GameMechanic(
        command = command,
        params = JsonObject(params.associate { it.first to JsonPrimitive(it.second) }),
    )

    private fun libro(vararg meccaniche: GameMechanic) = Manifest(
        id = "test", version = "1.0.0", title = "Test", description = "Test",
        language = "it", genre = "FANTASY",
        scenes = listOf(
            Scene(
                id = "1",
                sceneType = SceneType.START,
                genre = "FANTASY",
                narrativeText = "testo",
                gameMechanics = meccaniche.toList(),
            ),
        ),
    )

    private fun daAttivare(nome: String) = meccanica(
        "addItem",
        "itemName" to nome,
        "itemType" to "SPECIAL_ITEM",
        "attivazione" to "INATTIVO",
    )

    @Test
    fun unLibroCheDichiaraEPoiAttivaNonSegnalaNiente() {
        val result = AttivazioneValidator.validate(
            libro(daAttivare("Astro di Giada"), meccanica("activateItem", "itemName" to "Astro di Giada")),
        )

        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertTrue(result.warnings.isEmpty(), result.warnings.toString())
    }

    @Test
    fun attivareSenzaNomeEUnErrore() {
        val result = AttivazioneValidator.validate(libro(meccanica("activateItem")))

        assertEquals(1, result.errors.size)
        assertTrue("itemName" in result.errors.single())
    }

    @Test
    fun attivareUnOggettoCheIlLibroNonDichiaraEUnAvviso() {
        // Avviso e non errore: l'oggetto puo' arrivare dal personaggio
        // importato dal libro precedente della serie.
        val result = AttivazioneValidator.validate(libro(meccanica("activateItem", "itemName" to "Spada di Noxumbra")))

        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.warnings.size)
        assertTrue("Spada di Noxumbra" in result.warnings.single())
    }

    @Test
    fun unArtefattoSpentoCheNessunoAccendeEUnAvviso() {
        val result = AttivazioneValidator.validate(libro(daAttivare("Astro di Giada")))

        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.warnings.size)
        assertTrue("nessuna scena lo attiva" in result.warnings.single())
    }

    @Test
    fun spegnereNonContaComeAccendere() {
        // deactivateItem su un artefatto mai acceso lascia comunque il
        // bonus irraggiungibile: l'avviso deve restare.
        val result = AttivazioneValidator.validate(
            libro(daAttivare("Astro di Giada"), meccanica("deactivateItem", "itemName" to "Astro di Giada")),
        )

        assertEquals(1, result.warnings.size)
        assertTrue("nessuna scena lo attiva" in result.warnings.single())
    }

    @Test
    fun unLibroSenzaAttivazioniNonDiceNiente() {
        val result = AttivazioneValidator.validate(
            libro(meccanica("addItem", "itemName" to "Helmet", "itemType" to "SPECIAL_ITEM")),
        )

        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }
}
