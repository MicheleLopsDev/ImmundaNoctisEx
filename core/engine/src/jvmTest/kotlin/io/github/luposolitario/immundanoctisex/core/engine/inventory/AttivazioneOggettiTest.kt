package io.github.luposolitario.immundanoctisex.core.engine.inventory

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.StatoAttivazione
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveCombatSkill
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveMaxEndurance
import kotlin.test.Test
import kotlin.test.assertEquals

// La meccanica [Attiva] (08/08/2026): un oggetto addosso al protagonista
// ma spento, che una scena sveglia. Il caso vero e' l'Astro di Giada del
// primo libro. Vedi GameItem.StatoAttivazione.
class AttivazioneOggettiTest {

    private fun eroe(vararg oggetti: GameItem) = Character(
        role = CharacterRole.HERO,
        name = "Eroe di prova",
        baseCombatSkill = 15,
        currentEndurance = 20,
        maxEndurance = 20,
        inventory = oggetti.toList(),
    )

    private fun amuleto(stato: StatoAttivazione) = GameItem(
        name = "Astro di Giada",
        type = ItemType.SPECIAL_ITEM,
        effect = "COMBAT_SKILL:2",
        attivazione = stato,
    )

    private fun elmo() = GameItem(
        name = "Helmet",
        type = ItemType.SPECIAL_ITEM,
        effect = "ENDURANCE:2",
        attivazione = StatoAttivazione.INATTIVO,
    )

    @Test
    fun unOggettoSpentoNonDaIlSuoBonus() {
        val personaggio = eroe(amuleto(StatoAttivazione.INATTIVO))

        assertEquals(15, effectiveCombatSkill(personaggio))
    }

    @Test
    fun attivarloAccendeIlBonus() {
        val personaggio = Inventory.setItemActive(
            eroe(amuleto(StatoAttivazione.INATTIVO)),
            "Astro di Giada",
            active = true,
        )

        assertEquals(17, effectiveCombatSkill(personaggio))
        assertEquals(StatoAttivazione.ATTIVO, personaggio.inventory.single().attivazione)
    }

    @Test
    fun spegnerloLoRiporta() {
        val acceso = Inventory.setItemActive(eroe(amuleto(StatoAttivazione.ATTIVO)), "Astro di Giada", active = false)

        assertEquals(15, effectiveCombatSkill(acceso))
    }

    @Test
    fun unOggettoSenzaInterruttoreNonSiPuoSpegnere() {
        // NON_RICHIESTA vuol dire "questo oggetto non ha un
        // interruttore": nessun comando deve poterglielo dare, o un
        // Elmo qualunque diventerebbe spegnibile.
        val scudo = GameItem(name = "Shield", type = ItemType.SPECIAL_ITEM, effect = "COMBAT_SKILL:2")

        val dopo = Inventory.setItemActive(eroe(scudo), "Shield", active = false)

        assertEquals(17, effectiveCombatSkill(dopo))
        assertEquals(StatoAttivazione.NON_RICHIESTA, dopo.inventory.single().attivazione)
    }

    @Test
    fun accendereUnOggettoDiResistenzaAlzaAncheLaCorrente() {
        // Stessa regola dell'acquisizione (canone: "aggiunge N punti al
        // tuo totale"), altrimenti attivare un Elmo alzerebbe il tetto
        // lasciando il giocatore ferito di due punti dal nulla.
        val personaggio = eroe(elmo()).copy(currentEndurance = 20)

        val dopo = Inventory.setItemActive(personaggio, "Helmet", active = true)

        assertEquals(22, effectiveMaxEndurance(dopo))
        assertEquals(22, dopo.currentEndurance)
    }

    @Test
    fun spegnerloRiportaLaCorrenteSottoIlNuovoMassimo() {
        val acceso = Inventory.setItemActive(eroe(elmo()), "Helmet", active = true)

        val spento = Inventory.setItemActive(acceso, "Helmet", active = false)

        assertEquals(20, effectiveMaxEndurance(spento))
        assertEquals(20, spento.currentEndurance)
    }

    @Test
    fun attivareUnOggettoCheNonSiHaNonFaNiente() {
        val personaggio = eroe(amuleto(StatoAttivazione.INATTIVO))

        val dopo = Inventory.setItemActive(personaggio, "Spada di Noxumbra", active = true)

        assertEquals(personaggio, dopo)
    }

    @Test
    fun duplicareUnOggettoConStatoDiversoNonLoFonde() {
        // Se le due copie si fondessero, quella che c'era gia' cambierebbe
        // stato senza che nessuna scena l'abbia detto.
        val personaggio = eroe(amuleto(StatoAttivazione.ATTIVO))

        val dopo = Inventory.addItem(personaggio, amuleto(StatoAttivazione.INATTIVO))

        assertEquals(2, dopo.inventory.size)
        assertEquals(17, effectiveCombatSkill(dopo))
    }
}
