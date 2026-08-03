package io.github.luposolitario.immundanoctisex.ui.journal

import io.github.luposolitario.immundanoctisex.core.data.model.AutoJumpReason
import io.github.luposolitario.immundanoctisex.core.data.model.CombatOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.JourneyEntry
import io.github.luposolitario.immundanoctisex.core.data.model.Transition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// La mappa del viaggio raggruppa le voci del diario per luogo
// consecutivo. Qui si verifica solo il raggruppamento: il disegno (filo,
// pallini, icone) si guarda dalla @Preview.
class JournalMapTest {

    private fun voce(
        id: String,
        luogo: String? = null,
        transizione: Transition = Transition.ChoiceTaken("c$id"),
    ) = JourneyEntry(sceneId = id, enrichedText = "", transition = transizione, locationName = luogo)

    @Test
    fun `un diario vuoto non ha tappe`() {
        assertTrue(tappeDi(emptyList()).isEmpty())
    }

    @Test
    fun `scene di luoghi diversi fanno tappe diverse`() {
        val tappe = tappeDi(
            listOf(
                voce("1", "Riverside Inn"),
                voce("2", "Harbour Town"),
                voce("3", "Ruanon"),
            ),
        )
        assertEquals(listOf("Riverside Inn", "Harbour Town", "Ruanon"), tappe.map { it.luogo })
        assertTrue(tappe.all { it.scene == 1 })
    }

    @Test
    fun `scene consecutive nello stesso luogo fanno una tappa sola`() {
        val tappe = tappeDi(
            listOf(
                voce("1", "Ruanon"),
                voce("2", "Ruanon"),
                voce("3", "Ruanon"),
            ),
        )
        assertEquals(1, tappe.size)
        assertEquals(3, tappe.single().scene)
    }

    // Il caso che prima era sbagliato: la vecchia mappa scartava del tutto
    // le voci senza luogo, quindi il conto delle scene diceva il falso.
    @Test
    fun `una scena senza luogo resta nella tappa in corso`() {
        val tappe = tappeDi(
            listOf(
                voce("1", "Ruanon"),
                voce("2", luogo = null),
                voce("3", luogo = null),
            ),
        )
        assertEquals(1, tappe.size)
        assertEquals("Ruanon", tappe.single().luogo)
        assertEquals(3, tappe.single().scene)
    }

    @Test
    fun `tornare in un luogo gia visitato apre una tappa nuova`() {
        val tappe = tappeDi(
            listOf(
                voce("1", "Ruanon"),
                voce("2", "Harbour Town"),
                voce("3", "Ruanon"),
            ),
        )
        assertEquals(3, tappe.size)
        assertEquals(listOf("Ruanon", "Harbour Town", "Ruanon"), tappe.map { it.luogo })
    }

    // Il viaggio comincia prima che il libro dichiari un luogo: la tappa
    // c'è lo stesso, senza nome, e la schermata scrive "Luogo senza nome".
    @Test
    fun `un viaggio che parte senza luogo ha una prima tappa anonima`() {
        val tappe = tappeDi(listOf(voce("1", luogo = null), voce("2", "Ruanon")))
        assertEquals(2, tappe.size)
        assertNull(tappe.first().luogo)
        assertEquals("Ruanon", tappe.last().luogo)
    }

    @Test
    fun `l'uscita di una tappa e quella dell'ultima scena passata li`() {
        val tappe = tappeDi(
            listOf(
                voce("1", "Ruanon", Transition.ChoiceTaken("c1")),
                voce("2", luogo = null, transizione = Transition.CombatResolved(CombatOutcome.WIN)),
                voce("3", "Harbour Town", Transition.AutoJump(AutoJumpReason.RANDOM_CHOICE)),
            ),
        )
        assertEquals(Transition.CombatResolved(CombatOutcome.WIN), tappe.first().uscita)
    }

    // Dall'ultima tappa non si è ancora usciti: è dove il giocatore si
    // trova adesso, e la schermata ci scrive "Sei qui" invece di un'icona.
    @Test
    fun `dall'ultima tappa non si e usciti`() {
        val tappe = tappeDi(listOf(voce("1", "Ruanon"), voce("2", "Harbour Town")))
        assertNull(tappe.last().uscita)
        assertEquals(Transition.ChoiceTaken("c1"), tappe.first().uscita)
    }
}
