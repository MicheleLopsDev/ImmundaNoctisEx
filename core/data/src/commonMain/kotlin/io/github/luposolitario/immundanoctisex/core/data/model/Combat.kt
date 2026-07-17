package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// Blocco combat minimale della scena (REGOLE.md §1.5): l'autore scrive solo
// nome, due statistiche e destinazioni; a runtime il motore idrata un
// Character unico per il nemico. loseSceneId assente => fallback su
// deathSceneId del manifest; winSceneId è sempre obbligatorio.
@Serializable
data class Combat(
    val enemyName: String,
    val enemyCombatSkill: Int,
    val enemyEndurance: Int,
    val immuneToMindblast: Boolean = false,
    val evadeAfterRound: Int = 0,
    val winSceneId: String,
    val loseSceneId: String? = null,
    val evadeSceneId: String? = null,
)
