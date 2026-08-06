package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.EndingOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType

// Un libro senza rischio e un libro senza premio (06/08/2026, dalla
// misura dei 29 grafi ufficiali — doc/FORMA-DEI-GRAFI.md).
//
// Nei librogame pubblicati i finali sono in media 12,6 per libro e
// quasi tutti sconfitte SCRITTE: nel primo Lupo Solitario, sedici morti
// narrative con la loro prosa e una sola vittoria. Un libro che arriva
// qui con zero sconfitte non è un libro benevolo, è un racconto lineare
// in cui nessuna scelta costa niente — il difetto tipico di un testo
// generato da un modello, che tende a non punire mai il lettore.
//
// Avviso e non errore: un libro può legittimamente non avere morte (un
// giallo, un libro per bambini) e il gioco gira lo stesso. Ma chi lo
// pubblica deve sapere di aver fatto quella scelta.
//
// Si applica SOLO ai pacchetti che dichiarano dei finali: su un
// frammento senza nessuna scena ENDING la domanda non ha senso, e
// AdventureEnding.withGuaranteedEnding copre già quel caso a runtime.
internal object EndingsValidator {

    fun validate(manifest: Manifest): ValidationResult {
        val finali = manifest.scenes.filter { it.sceneType == SceneType.ENDING }
        if (finali.isEmpty()) return ValidationResult.EMPTY

        val warnings = mutableListOf<String>()
        if (finali.none { it.outcome == EndingOutcome.DEFEAT }) {
            warnings += "Nessun finale di sconfitta: ${finali.size} scene ENDING, " +
                "nessuna con outcome DEFEAT — chi gioca non rischia mai niente"
        }
        if (finali.none { it.outcome == EndingOutcome.VICTORY }) {
            warnings += "Nessun finale di vittoria: ${finali.size} scene ENDING, " +
                "nessuna con outcome VICTORY — l'avventura non si può vincere"
        }
        return ValidationResult(warnings = warnings)
    }
}
