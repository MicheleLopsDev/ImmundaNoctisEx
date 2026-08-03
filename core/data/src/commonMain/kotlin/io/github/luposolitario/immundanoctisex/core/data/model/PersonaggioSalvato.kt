package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// Un libro portato a termine da un personaggio.
//
// Si registra anche il TITOLO, non solo l'id (03/08/2026, decisione di
// Michele): l'elenco dei personaggi deve poter scrivere "Flight from
// the Dark" senza aprire quel libro — che potrebbe non essere più sul
// telefono, o essere stato rinominato. Il personaggio deve raccontare
// la propria storia da solo.
@Serializable
data class LibroCompletato(
    val packageId: String,
    val titolo: String,
    val completatoIl: Long,
)

// Un personaggio che può passare da un libro all'altro, come nella
// serie originale di Lupo Solitario (03/08/2026, Michele: "invece di
// iniziare con un nuovo pg importo il pg di un'altra avventura").
//
// È un file a sé, accanto ai salvataggi di partita e indipendente da
// essi: una partita è legata a un libro, un personaggio no. Si salva da
// solo **alla creazione** e si aggiorna **a fine libro vinto** — niente
// esporta/importa a mano.
//
// Cosa NON c'è qui, di proposito:
//  - il **rango Kai**: è già derivato dal numero di discipline
//    (`KaiRank.fromDisciplineCount`). Salvarlo darebbe due fonti per lo
//    stesso dato, e il giorno che una disciplina viene aggiunta senza
//    aggiornare l'altra si vedrebbe un "Iniziato Kai" con sette
//    discipline. Stesso principio dei bonus delle statistiche.
//  - la **partita**: scena corrente, flag e diario restano in
//    `SessionData`. Qui c'è l'eroe, non dove si trovava.
@Serializable
data class PersonaggioSalvato(
    // Stabile per tutta la vita del personaggio: rigiocare un libro
    // aggiorna il file esistente invece di crearne un secondo.
    val id: String,
    val creatoIl: Long,
    val libroOrigineId: String,
    val libroOrigineTitolo: String,
    // In ordine di completamento: il primo dice da dove è partito,
    // l'ultimo dove riprendere.
    val libriCompletati: List<LibroCompletato> = emptyList(),
    val personaggio: Character,
) {
    val nome: String get() = personaggio.name

    // Quante Discipline Kai spettano in più rispetto a quelle che ha:
    // nel canone se ne guadagna una per ogni libro portato a termine,
    // fino al massimo di dieci. È un numero, non una scelta: quale
    // disciplina prendere lo decide il giocatore iniziando il libro
    // nuovo (è l'unico momento in cui ha davanti quelle che gli
    // mancano).
    val disciplineDaAssegnare: Int
        get() = (libriCompletati.size - (personaggio.kaiDisciplines.size - DISCIPLINE_INIZIALI))
            .coerceIn(0, DISCIPLINE_TOTALI - personaggio.kaiDisciplines.size)

    // Ha già completato questo libro? Serve a non riproporre come
    // "nuovo" un libro che il personaggio ha già finito.
    fun haCompletato(packageId: String): Boolean = libriCompletati.any { it.packageId == packageId }

    companion object {
        const val DISCIPLINE_INIZIALI = 5
        const val DISCIPLINE_TOTALI = 10
    }
}
