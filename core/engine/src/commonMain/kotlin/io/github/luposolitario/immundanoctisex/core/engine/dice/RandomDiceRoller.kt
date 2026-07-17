package io.github.luposolitario.immundanoctisex.core.engine.dice

import kotlin.random.Random

// Implementazione di produzione del DiceRoller: la Random Number Table dei
// libri è un tiro uniforme 0-9. La UI del Dado del Destino (Fase 5/7) è solo
// teatro sopra questo stesso tiro.
class RandomDiceRoller(private val random: Random = Random.Default) : DiceRoller {
    override fun roll(): Int = random.nextInt(0, 10)
}
