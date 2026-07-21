package io.github.luposolitario.immundanoctisex.inference

// Vocabolario CHIUSO dei ritratti nemico/creatura (Combat.enemyImage),
// stessa disciplina di SceneImageCatalog ma per un uso diverso: qui
// l'ID lo scrive SOLO l'autore del libro nel JSON (decisione Michele
// 22/07/2026: niente tag Gemma, niente dizionario in più nel prompt).
// Per questo non serve un PROMPT_DICTIONARY come per le location.
object EnemyImageCatalog {
    val ENEMIES: List<String> = listOf(
        "enemy_bandits_city",
        "enemy_bandits_forest",
        "enemy_bears",
        "enemy_doomwolf",
        "enemy_flying_beasts",
        "enemy_giak",
        "enemy_helgast",
        "enemy_toads",
        "beast_wolves",
        "beast_stallion",
        "beast_cat",
        "beast_anaconda",
        "beast_familiar",
        "beast_rats",
    )

    fun isValid(name: String?): Boolean = name != null && name in ENEMIES
}
