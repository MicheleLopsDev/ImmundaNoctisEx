package io.github.luposolitario.immundanoctisex.core.engine.combat

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.engine.dice.FixedDiceRoller
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CombatSessionTest {

    private fun hero(
        combatSkill: Int = 15,
        endurance: Int = 20,
        disciplines: List<String> = emptyList(),
        items: List<GameItem> = emptyList(),
    ) = Character(
        role = CharacterRole.HERO,
        name = "Eroe di prova",
        baseCombatSkill = combatSkill,
        currentEndurance = endurance,
        maxEndurance = endurance,
        kaiDisciplines = disciplines,
        inventory = items,
    )

    private fun combat(
        enemySkill: Int = 14,
        enemyEndurance: Int = 10,
        immune: Boolean = false,
        evadeAfterRound: Int = 0,
        evadeSceneId: String? = null,
        loseSceneId: String? = "7",
        winSceneIdRapido: String? = null,
        winEntroRound: Int? = null,
        seColpitoSceneId: String? = null,
    ) = Combat(
        enemyName = "Warehouse Thug",
        enemyCombatSkill = enemySkill,
        enemyEndurance = enemyEndurance,
        immuneToMindblast = immune,
        evadeAfterRound = evadeAfterRound,
        winSceneId = "6",
        winSceneIdRapido = winSceneIdRapido,
        winEntroRound = winEntroRound,
        loseSceneId = loseSceneId,
        evadeSceneId = evadeSceneId,
        seColpitoSceneId = seColpitoSceneId,
    )

    private fun session(player: Character, combat: Combat, vararg rolls: Int) =
        CombatSession(player, combat, FixedDiceRoller(rolls.toList()))

    @Test
    fun ilNemicoEIdratatoComeCharacter() {
        val session = session(hero(), combat())

        assertEquals(CharacterRole.ENEMY, session.enemy.role)
        assertEquals("Warehouse Thug", session.enemy.name)
        assertEquals(10, session.enemy.maxEndurance)
    }

    @Test
    fun unRoundApplicaLaCrtAlRapportoGiusto() {
        // CS 15 vs 14 -> rapporto +1, tiro 5 -> banda +1/+2: nemico 8, giocatore 2.
        val session = session(hero(), combat(), 5)

        val result = session.fightRound()

        assertEquals(RoundResult(roll = 5, combatRatio = 1, playerLoss = 2, enemyLoss = 8), result)
        assertEquals(18, session.player.currentEndurance)
        assertEquals(2, session.enemy.currentEndurance)
        assertEquals(CombatStatus.ONGOING, session.status)
    }

    @Test
    fun quickResolveArrivaAllEsito() {
        // Rapporto +1, tiri 5,5: 8+8=16 > 10 Resistenza nemico -> WIN in 2 round.
        val session = session(hero(), combat(), 5, 5)

        val chronicle = session.quickResolve()

        assertEquals(2, chronicle.size)
        assertEquals(CombatStatus.WIN, session.status)
        assertEquals("6", session.destinationSceneId)
    }

    // I libri premiano spesso chi chiude in fretta con una scena diversa
    // (05/08/2026): "If you win the combat in seven rounds or less, turn
    // to 272. If you win the combat in more than seven rounds, turn to
    // 324." Misurato: 15 combattimenti sui 5 libri Project Aon.
    @Test
    fun laVittoriaRapidaUsaLaSuaScenaSoloEntroLaSoglia() {
        // Stessi tiri di sopra: WIN in 2 round, soglia 2 -> vittoria rapida.
        val entro = session(hero(), combat(winSceneIdRapido = "9", winEntroRound = 2), 5, 5)
        entro.quickResolve()
        assertEquals(CombatStatus.WIN, entro.status)
        assertEquals("9", entro.destinationSceneId)
    }

    @Test
    fun oltreLaSogliaLaVittoriaResta_QuellaNormale() {
        // Nemico più resistente: servono più round della soglia di 1.
        val oltre = session(hero(), combat(enemyEndurance = 20, winSceneIdRapido = "9", winEntroRound = 1), 5, 5, 5)
        oltre.quickResolve()
        assertEquals(CombatStatus.WIN, oltre.status)
        assertEquals("6", oltre.destinationSceneId)
    }

    // Gli scontri in cui il libro premia solo chi ne esce ILLESO
    // (05/08/2026): "If you lose any ENDURANCE points during this
    // combat, even when attempting to evade, turn immediately to 66".
    // Quattro scene sui 5 libri.
    @Test
    fun unColpoPresoChiudeIlCombattimentoQuandoIlLibroLoChiede() {
        // Rapporto +1, tiro 5: il giocatore perde 2 punti.
        val session = session(hero(), combat(seColpitoSceneId = "66"), 5)
        session.fightRound()

        assertEquals(CombatStatus.COLPITO, session.status)
        assertEquals("66", session.destinationSceneId)
    }

    // Senza quel campo il combattimento prosegue come sempre: il danno
    // subito è normale, non un'uscita.
    @Test
    fun senzaQuelCampoIlDannoSubitoNonChiudeNulla() {
        val session = session(hero(), combat(), 5)
        session.fightRound()

        assertEquals(CombatStatus.ONGOING, session.status)
    }

    // "even when attempting to evade": il libro lo dice esplicitamente.
    @Test
    fun ancheIlDannoPresoInFugaConta() {
        val session = session(hero(), combat(evadeSceneId = "9", seColpitoSceneId = "66"), 5)
        session.evade()

        assertEquals(CombatStatus.COLPITO, session.status)
        assertEquals("66", session.destinationSceneId)
    }

    // Chi ne esce senza un graffio prende la vittoria normale.
    @Test
    fun senzaDanniLaVittoriaRestaQuellaNormale() {
        // Rapporto +11 (CS 25 vs 14), tiro 9: il nemico muore, il
        // giocatore non perde nulla.
        val session = session(hero(combatSkill = 25), combat(seColpitoSceneId = "66"), 9)
        session.fightRound()

        assertEquals(CombatStatus.WIN, session.status)
        assertEquals("6", session.destinationSceneId)
    }

    // Una soglia senza scena (o viceversa) non dice nulla: si degrada
    // sulla vittoria normale invece di inventare una destinazione.
    @Test
    fun unaVittoriaRapidaIncompletaNonCambiaNulla() {
        val soloSoglia = session(hero(), combat(winEntroRound = 5), 5, 5)
        soloSoglia.quickResolve()
        assertEquals("6", soloSoglia.destinationSceneId)

        val soloScena = session(hero(), combat(winSceneIdRapido = "9"), 5, 5)
        soloScena.quickResolve()
        assertEquals("6", soloScena.destinationSceneId)
    }

    @Test
    fun laSconfittaUsaLoseSceneIdEIlFallbackENull() {
        // Rapporto -11 (CS 3 vs 14), tiro 1 -> morte istantanea del giocatore.
        val conLose = session(hero(combatSkill = 3), combat(), 1)
        conLose.quickResolve()
        assertEquals(CombatStatus.LOSE, conLose.status)
        assertEquals("7", conLose.destinationSceneId)

        val senzaLose = session(hero(combatSkill = 3), combat(loseSceneId = null), 1)
        senzaLose.quickResolve()
        assertNull(senzaLose.destinationSceneId) // il chiamante degrada su deathSceneId
    }

    @Test
    fun laMorteDelGiocatoreBatteQuellaDelNemico() {
        // Rapporto 0 (14 vs 14), tiro 1: giocatore -5, nemico -3. Entrambi a
        // Resistenza bassissima: muoiono nello stesso round -> LOSE.
        val session = session(hero(combatSkill = 14, endurance = 5), combat(enemyEndurance = 3), 1)

        session.fightRound()

        assertEquals(CombatStatus.LOSE, session.status)
    }

    @Test
    fun mindblastDaPiuDueENonSiAttivaDueVolte() {
        val session = session(hero(disciplines = listOf("MINDBLAST")), combat(), 5)

        assertTrue(session.activateMindblast())
        assertEquals(false, session.activateMindblast())
        // CS 15+2 vs 14 -> rapporto +3, tiro 5 -> banda +3/+4: nemico 9, giocatore 2.
        val result = session.fightRound()
        assertEquals(3, result.combatRatio)
        assertEquals(9, result.enemyLoss)
    }

    @Test
    fun mindblastNegatoDaImmunitaODisciplinaMancante() {
        assertEquals(false, session(hero(disciplines = listOf("MINDBLAST")), combat(immune = true)).activateMindblast())
        assertEquals(false, session(hero(), combat()).activateMindblast())
    }

    @Test
    fun mindblastDecadeAFineCombattimento() {
        val session = session(hero(disciplines = listOf("MINDBLAST")), combat(), 5, 5)
        session.activateMindblast()
        session.quickResolve()

        assertEquals(0, session.playerAfterCombat.activeModifiers.size)
    }

    @Test
    fun evasioneConCostoCanonico() {
        // evadeAfterRound=1: al round 0 la fuga non è disponibile.
        val session = session(
            hero(),
            combat(evadeAfterRound = 1, evadeSceneId = "5"),
            5, 3,
        )
        assertEquals(false, session.canEvade)
        assertNull(session.evade())

        session.fightRound()
        assertTrue(session.canEvade)
        // Rapporto +1, tiro 3: colonna +1/+2 -> giocatore perderebbe 3, nemico 0 (solo il giocatore paga).
        val evadeResult = session.evade()!!
        assertEquals(0, evadeResult.enemyLoss)
        assertEquals(3, evadeResult.playerLoss)
        assertEquals(CombatStatus.EVADED, session.status)
        assertEquals("5", session.destinationSceneId)
    }

    @Test
    fun ilDannoDellEvasionePuoUccidere() {
        // Rapporto -11, tiro 1 -> KILL sul giocatore anche in fuga.
        val session = session(hero(combatSkill = 3, endurance = 20), combat(evadeSceneId = "5"), 1)

        session.evade()

        assertEquals(CombatStatus.LOSE, session.status)
    }

    @Test
    fun pozioneCombatUsableCuraESiConsuma() {
        val potion = GameItem(
            name = "Laumspur Potion",
            type = ItemType.BACKPACK_ITEM,
            combatUsable = true,
            effect = "HEAL:4",
        )
        val session = session(hero(endurance = 20, items = listOf(potion)), combat(), 5)
        session.fightRound() // giocatore a 18

        assertTrue(session.useItem("Laumspur Potion"))
        assertEquals(20, session.player.currentEndurance)
        assertEquals(false, session.useItem("Laumspur Potion")) // consumata
    }

    @Test
    fun oggettoNonCombatUsableRifiutato() {
        val meal = GameItem(name = "Meal", type = ItemType.BACKPACK_ITEM, effect = "HEAL:2")
        val session = session(hero(items = listOf(meal)), combat())

        assertEquals(false, session.useItem("Meal"))
    }
}
