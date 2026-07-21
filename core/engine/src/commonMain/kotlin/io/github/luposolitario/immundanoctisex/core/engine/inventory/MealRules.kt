package io.github.luposolitario.immundanoctisex.core.engine.inventory

// Nome canonico e cura del Pasto (REGOLE.md §4.4). Pubblica apposta
// (22/07/2026, Michele: "anche fuori puoi consumarli con questo
// effetto" — stesso valore sia nel consumo OBBLIGATORIO,
// StatMechanics.requireAction in :core:engine, sia in quello MANUALE
// dalla scheda, AdventureState.consumeItem in :app): un solo posto da
// cambiare se in futuro cambia quanto cura un pasto.
object MealRules {
    const val ITEM_NAME = "Meal"
    const val HEAL_AMOUNT = 1
}
