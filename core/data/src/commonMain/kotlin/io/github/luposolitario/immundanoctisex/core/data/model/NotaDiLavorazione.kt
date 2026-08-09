package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// Cosa c'è da sapere su una scena **mentre la si costruisce**, e che il
// gioco non deve mai leggere (07/08/2026, Michele: "sono informazioni
// che sono utili quando revisioni e devono essere associate a dei
// warning sulle scene dove impattano").
//
// Nascono dal metodo di conversione da romanzo (`DA-ROMANZO-A-LIBROGAME.md`):
// quando il modello taglia un romanzo in scene, alcune cose vanno dette
// a chi rivede e a nessun altro — che una frase l'ha aggiunta lui, che
// quel paragrafo è in terza persona mentre il libro è in seconda.
//
// **Il punto non è il problema, è la sua visibilità.** Una frase
// aggiunta di nascosto è pericolosa; la stessa frase dichiarata è una
// proposta che l'autore accetta o cancella.
//
// Il motore di gioco le ignora del tutto: sono dati di lavorazione, non
// di partita. L'editor le mostra a chi rivede (§warning per scena).
@Serializable
enum class TipoNota {
    // Il modello ha aggiunto una frase per chiudere una scena che il
    // taglio lasciava monca. È l'unica libertà che il metodo gli
    // concede sul testo altrui, e va sempre dichiarata.
    TESTO_RITOCCATO,

    // La scena si discosta dalla forma narrativa del libro: un
    // paragrafo in terza persona dove il resto è in seconda, uno stacco
    // di scena che il protagonista non vive. Non è per forza un
    // difetto — succede anche nei libri pubblicati — ma chi rivede deve
    // poterlo decidere invece di scoprirlo per caso.
    FORMA,

    // Qualunque cosa il modello non abbia saputo risolvere da solo e
    // non volesse indovinare in silenzio.
    DA_CHIARIRE,
}

@Serializable
data class NotaDiLavorazione(
    val tipo: TipoNota,
    // Il testo per esteso, non un codice: deve bastare a decidere senza
    // aprire altro. «aggiunta in coda "Ti volti verso la porta."» dice
    // all'autore esattamente cosa approvare o cancellare.
    val testo: String,
)
