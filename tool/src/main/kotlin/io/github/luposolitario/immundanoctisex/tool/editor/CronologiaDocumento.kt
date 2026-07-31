package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.ui.geometry.Offset
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest

// §19.13 (Michele: "implementiamo ctrl-z e ctrl-y... gli spostamenti e
// le modifiche devono essere sempre serializzati prima"): contenuto
// (Manifest) E posizioni dei nodi INSIEME, un solo scatto di cronologia
// per entrambi — oggi sono due pezzi di stato separati
// (Schermata.Mappa.manifest, MapViewState.posizioniManuali,
// sincronizzati solo al salvataggio, §19.12), ma un'azione dell'utente
// può toccare l'uno, l'altro o entrambi: la cronologia li tratta come
// un'unica cosa.
data class Documento(val manifest: Manifest, val posizioni: Map<String, Offset>)

// Cronologia LINEARE, non ad albero (Michele, confermato esplicitamente:
// "si perde, ctrl+y può tornare a quel esatto istante solo se non hai
// fatto nulla altrimenti si sostituisce"): una nuova azione dopo un
// annulla scarta sempre quello che c'era nella pila "avanti", non lo
// conserva per un ramo alternativo. Non serializza nulla su disco né in
// JSON (modalità più semplice, confermata da Michele): uno "scatto" è
// solo un riferimento a un `Manifest`/una mappa di posizioni già
// immutabili, non una copia o una conversione — costa quanto tenere un
// puntatore.
class CronologiaDocumento {
    private val indietro = mutableListOf<Documento>()
    private val avanti = mutableListOf<Documento>()

    // Chiamata SUBITO PRIMA di applicare una modifica (contenuto o
    // posizioni), con lo stato COM'ERA fino a un attimo fa — mai dopo.
    // Per un gesto continuo (trascinamento) va chiamata una volta sola,
    // all'inizio (`onDragStart`), non a ogni fotogramma: altrimenti un
    // solo trascinamento produrrebbe centinaia di scatti in cronologia.
    fun registraCheckpoint(statoPrecedente: Documento) {
        indietro.add(statoPrecedente)
        avanti.clear()
    }

    // Ritorna lo stato a cui tornare, o null se non c'è nulla da
    // annullare. Il chiamante passa lo stato ATTUALE, che finisce nella
    // pila "avanti" (disponibile per un eventuale ripeti).
    fun annulla(statoAttuale: Documento): Documento? {
        val precedente = indietro.removeLastOrNull() ?: return null
        avanti.add(statoAttuale)
        return precedente
    }

    fun ripeti(statoAttuale: Documento): Documento? {
        val successivo = avanti.removeLastOrNull() ?: return null
        indietro.add(statoAttuale)
        return successivo
    }

    // Azzera tutto: un libro diverso (o lo stesso ricaricato da un
    // backup) non deve ereditare la cronologia di quello precedente
    // nella stessa sessione dell'editor — stesso principio di
    // `MapViewState.caricaPosizioniDa` (§19.12).
    fun reimposta() {
        indietro.clear()
        avanti.clear()
    }
}
