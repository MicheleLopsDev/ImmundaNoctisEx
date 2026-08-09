package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.StatoAttivazione
import kotlinx.serialization.json.jsonPrimitive

// La meccanica [Attiva] (08/08/2026): un oggetto posseduto ma spento, che
// una scena sveglia. Gli sbagli qui non danno errore a runtime — il
// comando semplicemente non fa niente, e il giocatore arriva allo
// scontro finale senza il bonus che il libro gli aveva promesso.
//
// Sono quasi tutti AVVISI e non errori, per una ragione precisa: un
// oggetto può arrivare dal personaggio importato da un libro
// precedente, e questo libro non ha modo di saperlo. Bloccare la
// validazione perché un `activateItem` non trova l'oggetto *in questo
// file* impedirebbe di scrivere il secondo libro di una serie.
internal object AttivazioneValidator {

    private val comandiDiAttivazione = setOf("activateItem", "deactivateItem")

    fun validate(manifest: Manifest): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Gli oggetti che QUESTO libro dichiara attivabili, con addItem.
        val attivabili = mutableSetOf<String>()
        manifest.scenes.forEach { scene ->
            scene.gameMechanics.filter { it.command == "addItem" }.forEach { mechanic ->
                val stato = mechanic.params["attivazione"]?.jsonPrimitive?.content
                if (stato != null && !stato.equals(StatoAttivazione.NON_RICHIESTA.name, ignoreCase = true)) {
                    mechanic.params["itemName"]?.jsonPrimitive?.content?.let { attivabili += it.lowercase() }
                }
            }
        }

        val svegliati = mutableSetOf<String>()
        manifest.scenes.forEach { scene ->
            scene.gameMechanics.forEachIndexed { index, mechanic ->
                if (mechanic.command !in comandiDiAttivazione) return@forEachIndexed
                val voce = "Scena '${scene.id}': gameMechanics[$index] (${mechanic.command})"
                val nome = mechanic.params["itemName"]?.jsonPrimitive?.content
                if (nome.isNullOrBlank()) {
                    // Questo sì è un errore: senza nome il comando non
                    // può fare nulla in nessuno scenario.
                    errors += "$voce: manca 'itemName'"
                    return@forEachIndexed
                }
                if (mechanic.command == "activateItem") svegliati += nome.lowercase()
                if (nome.lowercase() !in attivabili) {
                    warnings += "$voce: '$nome' non è dichiarato attivabile in questo libro " +
                        "(nessun addItem con attivazione INATTIVO/ATTIVO) — se non arriva da un libro " +
                        "precedente della serie, il comando non farà niente"
                }
            }
        }

        // Un artefatto spento che il libro non accende mai è un bonus
        // scritto e mai raggiungibile: quasi sempre una svista.
        (attivabili - svegliati).sorted().forEach { nome ->
            warnings += "L'oggetto '$nome' è dichiarato attivabile ma nessuna scena lo attiva"
        }

        return ValidationResult(errors = errors, warnings = warnings)
    }
}
