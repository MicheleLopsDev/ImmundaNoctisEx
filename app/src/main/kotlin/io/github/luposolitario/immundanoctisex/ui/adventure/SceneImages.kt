package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.annotation.DrawableRes
import io.github.luposolitario.immundanoctisex.R

// Risolve il nome scelto (pacchetto o Gemma, vedi SceneImageCatalog) nel
// drawable vero. `when` esplicito invece di resources.getIdentifier():
// stesso motivo di weaponTypeIcon/disciplineIcon — un nome che non
// corrisponde a nulla degrada sul default, mai un lookup fragile a
// runtime che R8 può rompere in minificazione.
@DrawableRes
fun sceneBackgroundRes(name: String?): Int = when (name) {
    "loc_black_gate" -> R.drawable.loc_black_gate
    "loc_caves" -> R.drawable.loc_caves
    "loc_crypt" -> R.drawable.loc_crypt
    "loc_cursed_castle" -> R.drawable.loc_cursed_castle
    "loc_forest" -> R.drawable.loc_forest
    "loc_forest_prey" -> R.drawable.loc_forest_prey
    "loc_graveyard" -> R.drawable.loc_graveyard
    "loc_harbor" -> R.drawable.loc_harbor
    "loc_helgedad" -> R.drawable.loc_helgedad
    "loc_helgedad_gate" -> R.drawable.loc_helgedad_gate
    "loc_infernal_city" -> R.drawable.loc_infernal_city
    "loc_kai_monastery" -> R.drawable.loc_kai_monastery
    "loc_market" -> R.drawable.loc_market
    "loc_monastery_dawn" -> R.drawable.loc_monastery_dawn
    "loc_mountain" -> R.drawable.loc_mountain
    "loc_mountain_pass" -> R.drawable.loc_mountain_pass
    "loc_smithy_exterior" -> R.drawable.loc_smithy_exterior
    "loc_smithy_interior" -> R.drawable.loc_smithy_interior
    "loc_standing_stones" -> R.drawable.loc_standing_stones
    "loc_storm_tower" -> R.drawable.loc_storm_tower
    "loc_tavern" -> R.drawable.loc_tavern
    "loc_tomb_exterior" -> R.drawable.loc_tomb_exterior
    "loc_tomb_interior" -> R.drawable.loc_tomb_interior
    "loc_warehouse" -> R.drawable.loc_warehouse
    // Secondo giro di location (24/07/2026) — vedi commento in
    // SceneImageCatalog.kt su loc_temple (rifatta da Michele).
    "loc_temple" -> R.drawable.loc_temple
    "loc_abandoned_keep" -> R.drawable.loc_abandoned_keep
    "loc_ancient_ruins" -> R.drawable.loc_ancient_ruins
    "loc_battlefield" -> R.drawable.loc_battlefield
    "loc_dungeon" -> R.drawable.loc_dungeon
    "loc_haunted_house" -> R.drawable.loc_haunted_house
    "loc_swamp" -> R.drawable.loc_swamp
    "loc_volcano" -> R.drawable.loc_volcano
    "loc_waterfall" -> R.drawable.loc_waterfall
    "loc_wizard_cove" -> R.drawable.loc_wizard_cove
    "loc_wizard_tower" -> R.drawable.loc_wizard_tower
    else -> R.drawable.map_dungeon
}
