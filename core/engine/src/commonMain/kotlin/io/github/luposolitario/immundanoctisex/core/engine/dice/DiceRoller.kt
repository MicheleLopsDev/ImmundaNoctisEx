package io.github.luposolitario.immundanoctisex.core.engine.dice

// Una delle quattro interfacce motivate (ARCHITETTURA.md): il tiro entra
// dall'esterno. In produzione lo implementa il Dado del Destino (la UI
// anima, l'utente ferma, il valore entra nell'engine); nei test è una
// sequenza fissa. MAI Random inline nel motore.
interface DiceRoller {
    // Tiro 0-9 (Random Number Table di Lupo Solitario).
    fun roll(): Int
}
