package io.github.luposolitario.immundanoctisex.core.engine.inventory

import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.engine.mechanics.itemType
import io.github.luposolitario.immundanoctisex.core.engine.mechanics.stringParam
import io.github.luposolitario.immundanoctisex.core.engine.mechanics.weaponType
import kotlinx.serialization.json.JsonObject

// Oggetti "sul banco" di una scena (comando offerItem, Michele
// 21/07/2026: "il pick deve sempre essere di una singola cosa per
// volta, addItem non può funzionare in maniera silenziosa" — quando ci
// sono più oggetti di quanti se ne possano prendere, la scelta dev'essere
// del giocatore, non un cap automatico che scarta in silenzio quello che
// non entra). A differenza di addItem, MechanicsExecutor non esegue
// offerItem all'arrivo in scena (il comando non è nel suo `when`, cade
// nel ramo di default: nessun effetto) — resta disponibile finché la UI
// non lo mostra e il giocatore non lo sceglie esplicitamente
// (AdventureState.pickItem, Inventory.canAdd per abilitare il pulsante).
object ItemOffers {

    fun offeredItems(scene: Scene): List<GameItem> =
        scene.gameMechanics
            .filter { it.command == "offerItem" }
            .mapNotNull { parseItem(it.params) }

    private fun parseItem(params: JsonObject): GameItem? {
        val name = params.stringParam("itemName") ?: return null
        val type = itemType(params.stringParam("itemType")) ?: return null
        return GameItem(
            name = name,
            type = type,
            quantity = params.stringParam("quantity")?.toIntOrNull() ?: 1,
            combatUsable = params.stringParam("combatUsable")?.toBooleanStrictOrNull() ?: false,
            effect = params.stringParam("effect"),
            weaponType = weaponType(params.stringParam("weaponType")),
        )
    }
}
