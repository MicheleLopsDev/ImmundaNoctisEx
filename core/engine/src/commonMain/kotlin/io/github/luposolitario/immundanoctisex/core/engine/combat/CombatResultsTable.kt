package io.github.luposolitario.immundanoctisex.core.engine.combat

// Danni di un lookup sulla CRT: dati puri, il testo lo compone la UI
// (REGOLE.md §1.2). KILL_DAMAGE è la sentinella di uccisione istantanea.
data class RoundDamage(
    val enemyLoss: Int,
    val playerLoss: Int,
)

// Tabella dei Risultati di Combattimento UFFICIALE di Lupo Solitario.
// Fonte: Kai Chronicles (GPL v3, combatTable.ts), che la trascrive dalla
// tabella di Project Aon (libro 1, Combat Results Table). La tabella di
// LoneWolfRules di v1 è stata CONFRONTATA E SCARTATA: valori non conformi
// (es. rapporto 0/tiro 0: 16 invece di 12), nessuna morte istantanea del
// giocatore ai rapporti molto negativi, bande a coppie non rispettate.
//
// Struttura: colonne per banda di Rapporto di Forza (a coppie, come nel
// libro), righe per tiro 0-9. Il tiro qui è la chiave REALE: niente
// aritmetica di indici (la trappola off-by-one del tiro 0 di v1 è
// strutturalmente impossibile). Tiro 0 = colpo migliore.
object CombatResultsTable {

    const val KILL_DAMAGE = 999
    private const val K = KILL_DAMAGE

    // Bande rapporto <= 0: colonne [0]=0, [1]=-1/-2, [2]=-3/-4, [3]=-5/-6,
    // [4]=-7/-8, [5]=-9/-10, [6]=-11 o meno. Coppie (danno nemico, danno giocatore).
    private val belowOrEqual: Map<Int, List<RoundDamage>> = mapOf(
        1 to row(3 to 5, 2 to 5, 1 to 6, 0 to 6, 0 to 8, 0 to K, 0 to K),
        2 to row(4 to 4, 3 to 5, 2 to 5, 1 to 6, 0 to 7, 0 to 8, 0 to K),
        3 to row(5 to 4, 4 to 4, 3 to 5, 2 to 5, 1 to 6, 0 to 7, 0 to 8),
        4 to row(6 to 3, 5 to 4, 4 to 4, 3 to 5, 2 to 6, 1 to 7, 0 to 8),
        5 to row(7 to 2, 6 to 3, 5 to 4, 4 to 4, 3 to 5, 2 to 6, 1 to 7),
        6 to row(8 to 2, 7 to 2, 6 to 3, 5 to 4, 4 to 5, 3 to 6, 2 to 6),
        7 to row(9 to 1, 8 to 2, 7 to 2, 6 to 3, 5 to 4, 4 to 5, 3 to 5),
        8 to row(10 to 0, 9 to 1, 8 to 1, 7 to 2, 6 to 3, 5 to 4, 4 to 4),
        9 to row(11 to 0, 10 to 0, 9 to 0, 8 to 0, 7 to 2, 6 to 3, 5 to 3),
        0 to row(12 to 0, 11 to 0, 10 to 0, 9 to 0, 8 to 0, 7 to 0, 6 to 0),
    )

    // Bande rapporto > 0: colonne [0]=+1/+2, [1]=+3/+4, [2]=+5/+6,
    // [3]=+7/+8, [4]=+9/+10, [5]=+11 o più (CRT standard, non estesa).
    private val above: Map<Int, List<RoundDamage>> = mapOf(
        1 to row(4 to 5, 5 to 4, 6 to 4, 7 to 4, 8 to 3, 9 to 3),
        2 to row(5 to 4, 6 to 3, 7 to 3, 8 to 3, 9 to 3, 10 to 2),
        3 to row(6 to 3, 7 to 3, 8 to 3, 9 to 2, 10 to 2, 11 to 2),
        4 to row(7 to 3, 8 to 2, 9 to 2, 10 to 2, 11 to 2, 12 to 2),
        5 to row(8 to 2, 9 to 2, 10 to 2, 11 to 2, 12 to 2, 14 to 1),
        6 to row(9 to 2, 10 to 2, 11 to 1, 12 to 1, 14 to 1, 16 to 1),
        7 to row(10 to 1, 11 to 1, 12 to 0, 14 to 0, 16 to 0, 18 to 0),
        8 to row(11 to 0, 12 to 0, 14 to 0, 16 to 0, 18 to 0, K to 0),
        9 to row(12 to 0, 14 to 0, 16 to 0, 18 to 0, K to 0, K to 0),
        0 to row(14 to 0, 16 to 0, 18 to 0, K to 0, K to 0, K to 0),
    )

    // Lookup: rapporto qualsiasi (le bande estreme assorbono i valori oltre
    // ±11), tiro 0-9. Tiro fuori intervallo: coercizione difensiva, mai
    // eccezione (il gioco non si blocca mai).
    fun damageFor(combatRatio: Int, roll: Int): RoundDamage {
        val safeRoll = roll.coerceIn(0, 9)
        return if (combatRatio <= 0) {
            val column = ((-combatRatio + 1) / 2).coerceAtMost(6)
            belowOrEqual.getValue(safeRoll)[column]
        } else {
            val column = ((combatRatio + 1) / 2).coerceAtMost(6) - 1
            above.getValue(safeRoll)[column]
        }
    }

    private fun row(vararg damages: Pair<Int, Int>): List<RoundDamage> =
        damages.map { RoundDamage(enemyLoss = it.first, playerLoss = it.second) }
}
