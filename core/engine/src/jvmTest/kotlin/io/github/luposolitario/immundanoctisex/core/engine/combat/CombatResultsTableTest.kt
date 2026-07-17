package io.github.luposolitario.immundanoctisex.core.engine.combat

import kotlin.test.Test
import kotlin.test.assertEquals

// Fixture CRT richieste dal piano (Fase 2), incluso il caso del tiro 0:
// nella tabella ufficiale il tiro 0 è il colpo MIGLIORE (in v1 era una
// trappola di indici; qui il tiro è la chiave reale della riga).
class CombatResultsTableTest {

    @Test
    fun tiroZeroEIlColpoMigliore() {
        assertEquals(RoundDamage(enemyLoss = 12, playerLoss = 0), CombatResultsTable.damageFor(0, 0))
        assertEquals(RoundDamage(enemyLoss = 3, playerLoss = 5), CombatResultsTable.damageFor(0, 1))
    }

    @Test
    fun leBandeAccoppianoIRapporti() {
        // -1 e -2 sono la stessa colonna, come nel libro.
        assertEquals(CombatResultsTable.damageFor(-1, 4), CombatResultsTable.damageFor(-2, 4))
        assertEquals(RoundDamage(enemyLoss = 5, playerLoss = 4), CombatResultsTable.damageFor(-2, 4))
        // +5 e +6 idem.
        assertEquals(CombatResultsTable.damageFor(5, 5), CombatResultsTable.damageFor(6, 5))
        assertEquals(RoundDamage(enemyLoss = 10, playerLoss = 2), CombatResultsTable.damageFor(5, 5))
    }

    @Test
    fun morteIstantaneaDelGiocatoreAiRapportiPeggiori() {
        assertEquals(CombatResultsTable.KILL_DAMAGE, CombatResultsTable.damageFor(-9, 1).playerLoss)
        assertEquals(CombatResultsTable.KILL_DAMAGE, CombatResultsTable.damageFor(-11, 2).playerLoss)
    }

    @Test
    fun morteIstantaneaDelNemicoAiRapportiMigliori() {
        assertEquals(CombatResultsTable.KILL_DAMAGE, CombatResultsTable.damageFor(11, 0).enemyLoss)
        assertEquals(CombatResultsTable.KILL_DAMAGE, CombatResultsTable.damageFor(7, 0).enemyLoss)
    }

    @Test
    fun iRapportiOltreLeBandeEstremeUsanoLaColonnaEstrema() {
        assertEquals(CombatResultsTable.damageFor(-11, 0), CombatResultsTable.damageFor(-25, 0))
        assertEquals(RoundDamage(enemyLoss = 6, playerLoss = 0), CombatResultsTable.damageFor(-25, 0))
        assertEquals(CombatResultsTable.damageFor(11, 3), CombatResultsTable.damageFor(30, 3))
    }

    @Test
    fun tuttaLaTabellaRispondeSenzaBuchi() {
        for (ratio in -30..30) {
            for (roll in 0..9) {
                val damage = CombatResultsTable.damageFor(ratio, roll)
                // Danni mai negativi; la sentinella è l'unico valore oltre 22.
                val valid = { loss: Int -> loss in 0..22 || loss == CombatResultsTable.KILL_DAMAGE }
                assertEquals(true, valid(damage.enemyLoss) && valid(damage.playerLoss), "ratio=$ratio roll=$roll -> $damage")
            }
        }
    }

    @Test
    fun campioniPuntualiDallaTabellaUfficiale() {
        // Verifica a campione contro la tabella di Project Aon/Kai Chronicles.
        assertEquals(RoundDamage(9, 1), CombatResultsTable.damageFor(0, 7))
        assertEquals(RoundDamage(0, 6), CombatResultsTable.damageFor(-6, 1))
        assertEquals(RoundDamage(0, 8), CombatResultsTable.damageFor(-8, 1))
        assertEquals(RoundDamage(7, 2), CombatResultsTable.damageFor(-8, 9))
        assertEquals(RoundDamage(4, 5), CombatResultsTable.damageFor(1, 1))
        assertEquals(RoundDamage(16, 1), CombatResultsTable.damageFor(12, 6))
        assertEquals(RoundDamage(18, 0), CombatResultsTable.damageFor(5, 0))
    }
}
