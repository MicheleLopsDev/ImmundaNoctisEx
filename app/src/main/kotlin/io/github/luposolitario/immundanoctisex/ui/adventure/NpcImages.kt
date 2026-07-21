package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.annotation.DrawableRes
import io.github.luposolitario.immundanoctisex.R

// Risolve l'ID dichiarato dall'autore (Scene.npcImage, vedi
// NpcImageCatalog) nel drawable vero. Mostrata sotto il testo della
// scena (AdventureScreen): un incontro pacifico, non un avversario.
@DrawableRes
fun npcImageRes(name: String?): Int? = when (name) {
    "npc_countess" -> R.drawable.npc_countess
    "npc_fortune_teller" -> R.drawable.npc_fortune_teller
    "npc_king" -> R.drawable.npc_king
    "npc_peasant_female" -> R.drawable.npc_peasant_female
    "npc_peasant_male" -> R.drawable.npc_peasant_male
    "npc_princess" -> R.drawable.npc_princess
    "npc_royal_mage" -> R.drawable.npc_royal_mage
    "npc_traveler" -> R.drawable.npc_traveler
    "npc_valkyrie" -> R.drawable.npc_valkyrie
    "npc_mage" -> R.drawable.npc_mage
    "npc_battlemage" -> R.drawable.npc_battlemage
    "hero_female" -> R.drawable.hero_female
    "hero_male" -> R.drawable.hero_male
    "misc_battle_clash" -> R.drawable.misc_battle_clash
    // Stesse immagini di EnemyImageCatalog, usate qui quando la
    // creatura non è ostile in QUESTA scena (vedi NpcImageCatalog).
    "beast_wolves" -> R.drawable.beast_wolves
    "beast_stallion" -> R.drawable.beast_stallion
    "beast_cat" -> R.drawable.beast_cat
    "beast_anaconda" -> R.drawable.beast_anaconda
    "beast_familiar" -> R.drawable.beast_familiar
    "beast_rats" -> R.drawable.beast_rats
    else -> null
}
