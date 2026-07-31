package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import kotlin.test.Test
import kotlin.test.assertEquals

// §17.4/§19.8: `riepilogoLibro()` è condivisa tra "Libri recenti" e
// l'intestazione dell'esportazione PNG — un solo posto da testare
// copre entrambi gli usi.
class EditorMainTest {

    private fun manifest(description: String) = Manifest(
        id = "test", version = "1.0.0", title = "La lama nera", description = description,
        language = "it", genre = "FANTASY",
    )

    @Test
    fun riepilogoConDescrizioneCortaLaMostraPerIntero() {
        val riepilogo = riepilogoLibro("rampo.json", manifest("Un'avventura breve"))

        assertEquals("rampo.json - La lama nera - FANTASY - it - Un'avventura breve", riepilogo)
    }

    @Test
    fun riepilogoConDescrizioneLungaLaTronca30CaratteriConEllissi() {
        val descrizioneLunga = "Una descrizione molto più lunga di trenta caratteri, scritta apposta per il test"
        val riepilogo = riepilogoLibro("rampo.json", manifest(descrizioneLunga))

        assertEquals("rampo.json - La lama nera - FANTASY - it - ${descrizioneLunga.take(30)}…", riepilogo)
    }
}
