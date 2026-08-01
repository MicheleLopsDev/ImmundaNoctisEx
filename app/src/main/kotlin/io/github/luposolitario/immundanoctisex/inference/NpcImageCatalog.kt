package io.github.luposolitario.immundanoctisex.inference

// Vocabolario CHIUSO dei ritratti personaggio (Scene.npcImage): ID
// scritto SOLO dall'autore del libro, mai da Gemma. Mostrato sotto il
// testo della scena (Michele 22/07/2026: "npc o beast se sono
// amichevoli" vanno lì, non nel combattimento).
//
// Le beast_* compaiono ANCHE qui (oltre che in EnemyImageCatalog): la
// stessa immagine — un lupo, un cavallo — può essere un nemico in una
// scena e un incontro pacifico in un'altra. È l'autore a scegliere in
// quale campo mettere l'ID (Combat.enemyImage se ostile, Scene.npcImage
// se no), non l'immagine stessa a deciderlo.
object NpcImageCatalog {
    val NPCS: List<String> = listOf(
        "npc_countess",
        "npc_fortune_teller",
        "npc_king",
        "npc_peasant_female",
        "npc_peasant_male",
        "npc_princess",
        "npc_royal_mage",
        "npc_traveler",
        "npc_valkyrie",
        "npc_mage",
        "npc_battlemage",
        "hero_female",
        "hero_male",
        "misc_battle_clash",
        "beast_wolves",
        "beast_stallion",
        "beast_cat",
        "beast_anaconda",
        "beast_familiar",
        "beast_rats",
    )

    fun isValid(name: String?): Boolean = name != null && name in NPCS
}
