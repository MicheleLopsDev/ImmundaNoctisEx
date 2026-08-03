package io.github.luposolitario.immundanoctisex.core.engine.character

import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.Discipline
import io.github.luposolitario.immundanoctisex.core.data.model.LibroCompletato
import io.github.luposolitario.immundanoctisex.core.data.model.PersonaggioSalvato

// Le regole per portare un personaggio da un libro al successivo
// (03/08/2026, Michele: "tutti come il canone").
//
// Il canone di Lupo Solitario, applicato alla lettera:
//
//  1. **Combattività e Resistenza base si tirano UNA VOLTA SOLA** per
//     tutta la serie. Importando un personaggio la creazione salta il
//     tiro dei dadi: quei due numeri sono suoi per sempre.
//  2. **Si riparte con la Resistenza al massimo.** Fra un'avventura e
//     l'altra l'eroe si è riposato.
//  3. **L'inventario passa intero** — armi, oggetti speciali, zaino,
//     Corone. Gli oggetti non referenziano il libro (l'effetto è
//     dichiarativo, `ENDURANCE:2`), quindi un Elmo del libro 1 funziona
//     identico nel libro 3.
//  4. **Una Disciplina Kai nuova per ogni libro completato**, fino a
//     dieci. La scelta si fa iniziando il libro successivo: è l'unico
//     momento in cui si ha davanti l'elenco di quelle che mancano.
object TrasportoPersonaggio {

    // L'eroe pronto per una nuova avventura. Niente modificatori attivi
    // (venivano dalla partita finita) e Resistenza piena; le statistiche
    // base e tutto il resto restano intatti.
    fun preparaPerNuovoLibro(salvato: PersonaggioSalvato): Character {
        val eroe = salvato.personaggio
        return eroe.copy(
            currentEndurance = eroe.maxEndurance,
            activeModifiers = emptyList(),
        )
    }

    // Le discipline che il personaggio NON ha ancora: è fra queste che
    // sceglie quella guadagnata. Ordine canonico dell'enum, stabile.
    fun disciplineMancanti(personaggio: Character): List<Discipline> =
        Discipline.entries.filterNot { it.name in personaggio.kaiDisciplines }

    // Aggiunge le discipline scelte. Non si accettano doppioni né più
    // di quante ne spettano: un errore qui darebbe un eroe fuori regola
    // che poi resta salvato per sempre.
    fun conDisciplineNuove(
        salvato: PersonaggioSalvato,
        scelte: List<Discipline>,
    ): Result<PersonaggioSalvato> {
        val spettanti = salvato.disciplineDaAssegnare
        val distinte = scelte.distinct()
        return when {
            distinte.size != scelte.size ->
                Result.failure(IllegalArgumentException("Disciplina scelta due volte."))
            distinte.size > spettanti ->
                Result.failure(
                    IllegalArgumentException(
                        "Spettano $spettanti discipline, ne sono state scelte ${distinte.size}.",
                    ),
                )
            distinte.any { it.name in salvato.personaggio.kaiDisciplines } ->
                Result.failure(IllegalArgumentException("Il personaggio possiede già una delle discipline scelte."))
            else -> Result.success(
                salvato.copy(
                    personaggio = salvato.personaggio.copy(
                        kaiDisciplines = salvato.personaggio.kaiDisciplines + distinte.map { it.name },
                    ),
                ),
            )
        }
    }

    // A libro finito: si registra il libro e si aggiorna l'eroe con
    // com'è arrivato alla fine (inventario e discipline guadagnate
    // durante l'avventura).
    //
    // Rigiocare un libro già completato NON aggiunge una seconda voce:
    // il canone dà una disciplina per libro, non per partita.
    fun conLibroCompletato(
        salvato: PersonaggioSalvato,
        eroeAllaFine: Character,
        packageId: String,
        titolo: String,
        quando: Long,
    ): PersonaggioSalvato {
        val giaFatto = salvato.haCompletato(packageId)
        return salvato.copy(
            personaggio = eroeAllaFine,
            libriCompletati = if (giaFatto) {
                salvato.libriCompletati
            } else {
                salvato.libriCompletati + LibroCompletato(packageId, titolo, quando)
            },
        )
    }
}
