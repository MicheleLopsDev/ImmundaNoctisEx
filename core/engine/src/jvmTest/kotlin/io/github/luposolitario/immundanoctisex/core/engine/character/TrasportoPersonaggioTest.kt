package io.github.luposolitario.immundanoctisex.core.engine.character

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.Discipline
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.LibroCompletato
import io.github.luposolitario.immundanoctisex.core.data.model.PersonaggioSalvato
import io.github.luposolitario.immundanoctisex.core.data.model.StatModifier
import io.github.luposolitario.immundanoctisex.core.data.model.StatType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrasportoPersonaggioTest {

    private fun eroe(
        discipline: List<String> = listOf("WEAPONSKILL", "CAMOUFLAGE", "HUNTING", "SIXTH_SENSE", "TRACKING"),
        resistenza: Int = 12,
        massimo: Int = 25,
        modificatori: List<StatModifier> = emptyList(),
        inventario: List<GameItem> = emptyList(),
    ) = Character(
        role = CharacterRole.HERO,
        name = "Ferro",
        baseCombatSkill = 18,
        currentEndurance = resistenza,
        maxEndurance = massimo,
        kaiDisciplines = discipline,
        activeModifiers = modificatori,
        inventory = inventario,
    )

    private fun salvato(
        personaggio: Character = eroe(),
        completati: List<LibroCompletato> = emptyList(),
    ) = PersonaggioSalvato(
        id = "abc",
        creatoIl = 1_000L,
        libroOrigineId = "01fftd",
        libroOrigineTitolo = "Flight from the Dark",
        libriCompletati = completati,
        personaggio = personaggio,
    )

    // Canone: fra un'avventura e l'altra l'eroe si è riposato.
    @Test
    fun siRipartConLaResistenzaAlMassimo() {
        val pronto = TrasportoPersonaggio.preparaPerNuovoLibro(salvato(eroe(resistenza = 3, massimo = 25)))

        assertEquals(25, pronto.currentEndurance)
        assertEquals(25, pronto.maxEndurance)
    }

    // I modificatori venivano dalla partita finita: una ferita del libro
    // scorso non si trascina nel successivo.
    @Test
    fun iModificatoriDellaVecchiaPartitaNonSiTrascinano() {
        val ferito = eroe(modificatori = listOf(StatModifier(StatType.COMBAT_SKILL, -2, sourceType = "ferita")))

        val pronto = TrasportoPersonaggio.preparaPerNuovoLibro(salvato(ferito))

        assertTrue(pronto.activeModifiers.isEmpty())
    }

    // Canone: le statistiche base si tirano una volta sola per tutta la
    // serie, e l'inventario passa intero.
    @Test
    fun statisticheBaseEInventarioRestanoIntatti() {
        val conRoba = eroe(
            inventario = listOf(
                GameItem(name = "Sword", type = ItemType.WEAPON),
                GameItem(name = "Helmet", type = ItemType.SPECIAL_ITEM, effect = "ENDURANCE:2"),
                GameItem(name = "Gold Crowns", type = ItemType.GOLD, quantity = 17),
            ),
        )

        val pronto = TrasportoPersonaggio.preparaPerNuovoLibro(salvato(conRoba))

        assertEquals(18, pronto.baseCombatSkill)
        assertEquals(3, pronto.inventory.size)
        assertEquals(17, pronto.inventory.first { it.type == ItemType.GOLD }.quantity)
    }

    @Test
    fun senzaLibriCompletatiNonSpettaNessunaDisciplina() {
        assertEquals(0, salvato().disciplineDaAssegnare)
    }

    @Test
    fun unLibroCompletatoDaDirittoAUnaDisciplina() {
        val dopoUnLibro = salvato(completati = listOf(LibroCompletato("01fftd", "Flight from the Dark", 2_000L)))

        assertEquals(1, dopoUnLibro.disciplineDaAssegnare)
    }

    // Una volta scelta, non spetta più: il conto guarda quante
    // discipline ha in più rispetto alle cinque iniziali.
    @Test
    fun dopoAverlaScelaNonNeSpettaUnAltra() {
        val dopoUnLibro = salvato(completati = listOf(LibroCompletato("01fftd", "Flight from the Dark", 2_000L)))

        val aggiornato = TrasportoPersonaggio
            .conDisciplineNuove(dopoUnLibro, listOf(Discipline.HEALING))
            .getOrThrow()

        assertEquals(6, aggiornato.personaggio.kaiDisciplines.size)
        assertEquals(0, aggiornato.disciplineDaAssegnare)
    }

    @Test
    fun nonSiPuoScegliereUnaDisciplinaGiaPosseduta() {
        val dopoUnLibro = salvato(completati = listOf(LibroCompletato("01fftd", "Flight from the Dark", 2_000L)))

        val esito = TrasportoPersonaggio.conDisciplineNuove(dopoUnLibro, listOf(Discipline.WEAPONSKILL))

        assertTrue(esito.isFailure)
    }

    @Test
    fun nonSiPossonoPrenderePiuDisciplineDiQuanteNeSpettano() {
        val dopoUnLibro = salvato(completati = listOf(LibroCompletato("01fftd", "Flight from the Dark", 2_000L)))

        val esito = TrasportoPersonaggio.conDisciplineNuove(
            dopoUnLibro,
            listOf(Discipline.HEALING, Discipline.MINDBLAST),
        )

        assertTrue(esito.isFailure)
    }

    @Test
    fun leDisciplineMancantiSonoQuelleCheNonHa() {
        val mancanti = TrasportoPersonaggio.disciplineMancanti(eroe())

        assertEquals(5, mancanti.size)
        assertFalse(mancanti.contains(Discipline.WEAPONSKILL))
        assertTrue(mancanti.contains(Discipline.HEALING))
    }

    @Test
    fun completareUnLibroLoRegistraEAggiornaLEroe() {
        val allaFine = eroe(inventario = listOf(GameItem(name = "Sommerswerd", type = ItemType.WEAPON)))

        val aggiornato = TrasportoPersonaggio.conLibroCompletato(
            salvato(), allaFine, "01fftd", "Flight from the Dark", 5_000L,
        )

        assertEquals(1, aggiornato.libriCompletati.size)
        assertEquals("Flight from the Dark", aggiornato.libriCompletati.first().titolo)
        assertEquals("Sommerswerd", aggiornato.personaggio.inventory.first().name)
        assertEquals(1, aggiornato.disciplineDaAssegnare)
    }

    // Il canone dà una disciplina per LIBRO, non per partita: rigiocare
    // lo stesso libro non deve moltiplicare le ricompense.
    @Test
    fun rigiocareLoStessoLibroNonLoRegistraDueVolte() {
        val giaFatto = TrasportoPersonaggio.conLibroCompletato(
            salvato(), eroe(), "01fftd", "Flight from the Dark", 5_000L,
        )

        val diNuovo = TrasportoPersonaggio.conLibroCompletato(
            giaFatto, eroe(), "01fftd", "Flight from the Dark", 9_000L,
        )

        assertEquals(1, diNuovo.libriCompletati.size)
        assertEquals(1, diNuovo.disciplineDaAssegnare)
    }

    // Dieci è il tetto: finiti tutti i libri non si va oltre.
    @Test
    fun oltreLeDieciDisciplineNonSiVa() {
        val completo = eroe(discipline = Discipline.entries.map { it.name })
        val moltiLibri = salvato(
            personaggio = completo,
            completati = (1..8).map { LibroCompletato("libro$it", "Libro $it", it * 1_000L) },
        )

        assertEquals(0, moltiLibri.disciplineDaAssegnare)
    }
}
