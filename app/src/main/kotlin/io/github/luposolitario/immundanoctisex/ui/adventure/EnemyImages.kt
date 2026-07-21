package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.annotation.DrawableRes
import io.github.luposolitario.immundanoctisex.R

// Risolve l'ID dichiarato dall'autore (Combat.enemyImage, vedi
// EnemyImageCatalog) nel drawable vero. Null o non riconosciuto -> null:
// a differenza dello sfondo di scena, qui NON c'è un default silenzioso
// da mostrare al suo posto — se l'autore non lo dichiara, il combattimento
// resta solo testo, come prima di questa funzione.
@DrawableRes
fun enemyImageRes(name: String?): Int? = when (name) {
    "enemy_bandits_city" -> R.drawable.enemy_bandits_city
    "enemy_bandits_forest" -> R.drawable.enemy_bandits_forest
    "enemy_bears" -> R.drawable.enemy_bears
    "enemy_doomwolf" -> R.drawable.enemy_doomwolf
    "enemy_flying_beasts" -> R.drawable.enemy_flying_beasts
    "enemy_giak" -> R.drawable.enemy_giak
    "enemy_helgast" -> R.drawable.enemy_helgast
    "enemy_toads" -> R.drawable.enemy_toads
    "beast_wolves" -> R.drawable.beast_wolves
    "beast_stallion" -> R.drawable.beast_stallion
    "beast_cat" -> R.drawable.beast_cat
    "beast_anaconda" -> R.drawable.beast_anaconda
    "beast_familiar" -> R.drawable.beast_familiar
    "beast_rats" -> R.drawable.beast_rats
    else -> null
}
