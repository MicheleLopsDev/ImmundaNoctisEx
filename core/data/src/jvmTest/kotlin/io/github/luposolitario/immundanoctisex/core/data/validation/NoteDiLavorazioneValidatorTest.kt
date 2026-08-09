package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.NotaDiLavorazione
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import io.github.luposolitario.immundanoctisex.core.data.model.TipoNota
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoteDiLavorazioneValidatorTest {

    private fun libro(vararg scene: Scene) = Manifest(
        id = "test", version = "1.0.0", title = "Test", description = "Test",
        language = "it", genre = "FANTASY", scenes = scene.toList(),
    )

    private fun scena(id: String, vararg note: NotaDiLavorazione) = Scene(
        id = id,
        sceneType = SceneType.TRANSITION,
        genre = "FANTASY",
        narrativeText = "testo",
        noteDiLavorazione = note.toList(),
    )

    @Test
    fun ogniNotaDiventaUnAvvisoConLaSuaScena() {
        val result = NoteDiLavorazioneValidator.validate(
            libro(
                scena("12", NotaDiLavorazione(TipoNota.FORMA, "due paragrafi in terza persona")),
                scena("13", NotaDiLavorazione(TipoNota.TESTO_RITOCCATO, "aggiunta una frase in coda")),
            ),
        )

        assertEquals(2, result.warnings.size)
        assertTrue(result.warnings.any { "'12'" in it && "forma narrativa" in it })
        assertTrue(result.warnings.any { "'13'" in it && "testo ritoccato" in it })
    }

    @Test
    fun leNoteNonSonoMaiErrori() {
        // Un libro pieno di note resta un libro valido: una nota non e'
        // un difetto del file, e' una cosa da decidere.
        val result = NoteDiLavorazioneValidator.validate(
            libro(scena("1", NotaDiLavorazione(TipoNota.DA_CHIARIRE, "qui non ho capito chi parla"))),
        )

        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.warnings.size)
    }

    @Test
    fun unLibroSenzaNoteNonDiceNiente() {
        val result = NoteDiLavorazioneValidator.validate(libro(scena("1")))

        assertTrue(result.warnings.isEmpty())
    }
}
