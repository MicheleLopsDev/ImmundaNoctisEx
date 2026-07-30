package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneCodeTest {

    private fun scene(id: String, type: SceneType, locationName: String? = null) = Scene(
        id = id,
        sceneType = type,
        genre = "FANTASY",
        narrativeText = "testo",
        locationName = locationName,
    )

    @Test
    fun combinaLocationTipoEId() {
        val codice = codiceScena(scene("42", SceneType.TRANSITION, "Taverna"))

        assertEquals("TAV-TRANS-42", codice)
    }

    @Test
    fun senzaLocationUsaIlGenericoSc() {
        val codice = codiceScena(scene("7", SceneType.START, null))

        assertEquals("SC-START-7", codice)
    }

    @Test
    fun ignoraSpaziEPunteggiaturaNellaLocation() {
        val codice = codiceScena(scene("1", SceneType.ENDING, "Bosco Incantato!"))

        assertEquals("BOS-END-1", codice)
    }
}
