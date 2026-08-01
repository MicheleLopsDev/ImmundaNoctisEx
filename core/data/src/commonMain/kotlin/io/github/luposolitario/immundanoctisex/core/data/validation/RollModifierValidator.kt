package io.github.luposolitario.immundanoctisex.core.data.validation

import io.github.luposolitario.immundanoctisex.core.data.model.Discipline
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.RollConditionType

// Scene.rollModifiers (01/08/2026): un modificatore scritto male non
// darebbe errore a runtime — verrebbe semplicemente ignorato, e il
// giocatore finirebbe nella scena sbagliata senza che nessuno se ne
// accorga. Meglio bloccare in fase di validazione, come per sfx.
internal object RollModifierValidator {

    private val canonicalIds = Discipline.entries.map { it.name }.toSet()

    fun validate(manifest: Manifest): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        manifest.scenes.forEach { scene ->
            val etichetta = "Scena '${scene.id}'"

            // Un modificatore su una scena senza tiro non verrebbe mai
            // applicato: probabile errore di scrittura, ma non rompe
            // nulla — avviso, non blocco (stesso criterio del "vicolo
            // cieco" in GraphValidator).
            if (scene.rollModifiers.isNotEmpty() &&
                scene.choices.none { it.minRoll != null && it.maxRoll != null }
            ) {
                warnings += "$etichetta: dichiara rollModifiers ma nessuna scelta con intervallo di tiro"
            }

            scene.rollModifiers.forEachIndexed { index, modifier ->
                val voce = "$etichetta: rollModifiers[$index]"
                val condition = modifier.condition ?: return@forEachIndexed

                when (condition.type) {
                    RollConditionType.ENDURANCE -> {
                        if (condition.operator == null || condition.threshold == null) {
                            errors += "$voce (ENDURANCE) richiede operator e threshold"
                        }
                    }
                    // Per gli altri tre la lista dei valori È la
                    // condizione: vuota significa "non si applica mai".
                    RollConditionType.DISCIPLINE,
                    RollConditionType.ITEM,
                    RollConditionType.FLAG,
                    -> {
                        if (condition.values.isEmpty()) {
                            errors += "$voce (${condition.type}) non elenca nessun valore"
                        }
                    }
                }

                if (condition.type == RollConditionType.DISCIPLINE) {
                    condition.values.filterNot { it in canonicalIds }.forEach { id ->
                        errors += "$voce usa disciplina '$id' non canonica (attese: $canonicalIds)"
                    }
                }
            }
        }

        return ValidationResult(errors = errors, warnings = warnings)
    }
}
