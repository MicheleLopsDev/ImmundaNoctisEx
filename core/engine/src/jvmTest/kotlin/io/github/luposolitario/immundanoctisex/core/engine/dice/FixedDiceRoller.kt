package io.github.luposolitario.immundanoctisex.core.engine.dice

// DiceRoller di test: sequenza fissa e deterministica, come previsto da
// ARCHITETTURA.md ("nei test è una sequenza fissa").
class FixedDiceRoller(private val rolls: List<Int>) : DiceRoller {
    private var index = 0

    override fun roll(): Int {
        check(index < rolls.size) { "sequenza di tiri esaurita" }
        return rolls[index++]
    }
}
