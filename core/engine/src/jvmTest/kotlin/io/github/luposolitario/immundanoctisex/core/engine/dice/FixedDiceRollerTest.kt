package io.github.luposolitario.immundanoctisex.core.engine.dice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FixedDiceRollerTest {

    @Test
    fun restituisceITiriNellOrdineConfigurato() {
        val roller = FixedDiceRoller(listOf(3, 7, 0))

        assertEquals(3, roller.roll())
        assertEquals(7, roller.roll())
        assertEquals(0, roller.roll())
    }

    @Test
    fun segnalaSequenzaEsaurita() {
        val roller = FixedDiceRoller(listOf(5))
        roller.roll()

        assertFailsWith<IllegalStateException> { roller.roll() }
    }
}
