package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.TipoNota

// Le note di lavorazione diventano avvisi di validazione (08/08/2026,
// Michele: "la Nota sulla forma non la metterei nelle domande per lo
// scrittore, la implementerei nel sistema di warning dell'editor così
// da far risolvere poi il tutto in fase di normalizzazione, quando ci
// sarà la revisione completa del file JSON").
//
// Il punto è che una nota per scena si vede solo aprendo quella scena,
// e in un libro da 50 scene non si aprono tutte. Come avvisi, invece,
// il pulsante "Valida libro" le mette **tutte in un elenco**: è quella
// la lista di cose da chiudere prima di dire che il libro è finito.
//
// Mai errori: una nota non è un difetto del file, è una cosa da
// decidere. Un libro pieno di note resta un libro valido.
internal object NoteDiLavorazioneValidator {

    fun validate(manifest: Manifest): ValidationResult {
        val warnings = manifest.scenes.flatMap { scene ->
            scene.noteDiLavorazione.map { nota ->
                "Scena '${scene.id}' [${etichetta(nota.tipo)}]: ${nota.testo}"
            }
        }
        return ValidationResult(warnings = warnings)
    }

    // Il tipo per esteso: chi rilegge l'elenco deve capire cosa gli si
    // chiede senza andare a cercare cosa vuol dire TESTO_RITOCCATO.
    private fun etichetta(tipo: TipoNota): String = when (tipo) {
        TipoNota.TESTO_RITOCCATO -> "testo ritoccato dal modello"
        TipoNota.FORMA -> "forma narrativa"
        TipoNota.DA_CHIARIRE -> "da chiarire"
    }
}
