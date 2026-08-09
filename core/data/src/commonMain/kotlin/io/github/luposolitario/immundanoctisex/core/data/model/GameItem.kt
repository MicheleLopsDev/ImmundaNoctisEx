package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ItemType {
    WEAPON,
    BACKPACK_ITEM,
    SPECIAL_ITEM,
    GOLD,
}

// Un oggetto può essere addosso al protagonista SENZA essere ancora
// sveglio (08/08/2026, Michele: "la meccanica [Attiva] che rende un
// oggetto speciale abilitato e può permettere di fargli aggiungere
// bonus, altrimenti il suo stato è disabilitato per default").
//
// Nasce da un caso vero del primo libro: l'Astro di Giada è al collo del
// protagonista dalla prima pagina, ma è una scena precisa a stabilirlo
// come oggetto attivo — non un `[Ottieni:]`, perché non lo riceve lì.
// Senza questo stato l'unico modo di raccontarlo era darglielo in quel
// momento, cioè mentire su quando l'ha avuto.
//
// Tre stati e non un booleano: "non c'è niente da attivare" e "c'è da
// attivare e non è stato attivato" sono cose diverse, e confonderle
// spegnerebbe i bonus di ogni Elmo mai scritto. NON_RICHIESTA è il
// default: tutti i libri esistenti si comportano esattamente come prima.
@Serializable
enum class StatoAttivazione {
    // L'oggetto vale sempre, appena entra nell'inventario. Il caso
    // normale: un Elmo +2 non ha bisogno di cerimonie.
    NON_RICHIESTA,

    // Posseduto ma spento: i suoi bonus NON contano. È lo stato in cui
    // l'autore dichiara un artefatto che il libro sveglierà più avanti.
    INATTIVO,

    // Acceso dalla meccanica `activateItem`: da qui i bonus contano.
    ATTIVO,
}

// Formato oggetto canonico (STATO.md §4.2): effetto dichiarativo estensibile
// senza cambiare schema (v0.1 implementa solo HEAL:n). weaponType è il tipo
// canonico dell'arma, valorizzato solo per type == WEAPON (mai UNARMED,
// che è una specializzazione, non un'arma).
@Serializable
data class GameItem(
    val name: String,
    val type: ItemType,
    val quantity: Int = 1,
    val combatUsable: Boolean = false,
    val effect: String? = null,
    val weaponType: WeaponType? = null,
    val attivazione: StatoAttivazione = StatoAttivazione.NON_RICHIESTA,
) {
    // I bonus di un oggetto contano solo se è sveglio. Un'unica domanda,
    // qui, invece di ripetere il confronto in ogni punto che calcola.
    val effettiAttivi: Boolean get() = attivazione != StatoAttivazione.INATTIVO
}
