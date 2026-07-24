package io.github.luposolitario.immundanoctisex.inference

// Vocabolario CHIUSO delle immagini di sfondo disponibili (richiesta
// Michele 20/07/2026, esperimento — vedi DIARIO.md): stessa disciplina
// degli effetti sonori pensati in UPGRADE.md, "il modello sceglie tra ID
// che esistono davvero... ID sconosciuto = silenzio, mai un errore".
//
// UNICA FONTE DI VERITÀ per tre usi diversi: il prompt la mostra a Gemma
// come dizionario di scelta, il parser la usa per scartare nomi
// inventati, la UI la usa per risolvere il drawable. Se cambiano i nomi
// o i contenuti delle risorse in drawable-nodpi, si aggiorna solo qui.
//
// Ogni voce ha una DESCRIZIONE (21/07/2026, richiesta Michele: "un
// dizionario delle scene spiegando ogni scena a cosa può corrispondere"
// — prima Gemma aveva solo il nome del file, es. "loc_black_gate" contro
// "loc_helgedad_gate": due portali di pietra molto simili, indistinguibili
// dal nome nudo). Descrizioni scritte guardando le immagini vere, non a
// memoria del nome — un dizionario sbagliato confonde più di nessun
// dizionario.
//
// Solo LOCAZIONI (loc_*): npc_*/enemy_* sono ritratti di personaggi, non
// sfondi di scena — semanticamente un'altra cosa, fuori da questo giro.
object SceneImageCatalog {
    // Ordine STABILE (LinkedHashMap implicito di mapOf con chiavi String):
    // serve a prompt e test riproducibili, non solo a un elenco qualsiasi.
    private val DESCRIPTIONS: Map<String, String> = linkedMapOf(
        "loc_black_gate" to "a dark stone gate flanked by two skull-horned guardian statues, " +
            "a staircase descending into darkness beyond, deep in a forest",
        "loc_caves" to "a cave entrance in rocky mountains, wild vegetation around the opening",
        "loc_crypt" to "a crypt entrance with a half-open door, runes and skulls carved into " +
            "the stone archway",
        "loc_cursed_castle" to "a gothic castle gate guarded by carved gargoyles, thorny vines " +
            "creeping over cursed stonework, a skeleton lying at the threshold",
        "loc_forest" to "a forest path winding through trees toward distant mountains",
        "loc_forest_prey" to "a dense forest of ancient gnarled trees with wild deer grazing " +
            "among the roots",
        "loc_graveyard" to "a graveyard at night under a crescent moon, a hooded figure walking " +
            "among the gravestones",
        "loc_harbor" to "a coastal harbor town, a castle overlooking ships and city walls by the water",
        "loc_helgedad" to "the dark skyline of a black city at night, smoke rising over ominous " +
            "spires, a crescent moon above",
        "loc_helgedad_gate" to "an archway entrance guarded by two horned warriors, runes carved " +
            "in the stone above, stairs leading down into a lit passage",
        "loc_infernal_city" to "an infernal walled city under a stormy sky, dark spires and a " +
            "castle silhouette looming over narrow streets",
        "loc_kai_monastery" to "a mountaintop monastery of towers and walls, a road climbing up " +
            "to its gate",
        "loc_market" to "a bustling town marketplace with merchants, stalls and townsfolk trading",
        "loc_monastery_dawn" to "a sunrise over distant mountains with a castle silhouette on the " +
            "horizon, a hopeful dawn",
        "loc_mountain" to "a tall rocky mountain peak rising above clouds, pine trees at its base",
        "loc_mountain_pass" to "a rocky mountain forest path, an armored knight on horseback " +
            "leading soldiers with banners toward a distant castle on a cliff",
        "loc_smithy_exterior" to "the exterior of a blacksmith's shop on a town street at night",
        "loc_smithy_interior" to "the interior of a smithy, a blacksmith working at a glowing " +
            "forge, weapons on display",
        "loc_standing_stones" to "a circle of ancient standing stones in a moonlit forest " +
            "clearing, a robed figure approaching",
        "loc_storm_tower" to "a lightning storm over an ancient rune-carved stone tower in open " +
            "moorland, standing stones and a lone tree nearby, a dark spired castle in the distance",
        "loc_tavern" to "the interior of a crowded tavern, patrons drinking and talking by the fireplace",
        "loc_tomb_exterior" to "a tomb entrance decorated with a skull and hanging chains, " +
            "demonic guardian statues on each side",
        "loc_tomb_interior" to "inside a tomb, a stone sarcophagus with demonic guardians standing watch",
        "loc_warehouse" to "the interior of a storeroom lined with shelves of crates, potions " +
            "and scrolls, robed figures trading goods near a stone archway",
        // Secondo giro di location (24/07/2026, Michele: arrivate insieme ai
        // suoni loc_* già agganciati in SUONI-IMMAGINI.md) — descrizioni
        // scritte guardando le immagini vere consegnate, non il nome nudo.
        // loc_temple: la prima versione aveva un'iconografia cristiana
        // esplicita (Cristo in trono, croce sul campanile), sostituita da
        // Michele con un Tempio del Sole di Kai, coerente con l'ambientazione.
        "loc_temple" to "Kai's Sun Temple, a fantasy stone temple facade with carved suns, " +
            "guardian lion and ram statues flanking the entrance, an animal skull relief above " +
            "the doorway",
        "loc_abandoned_keep" to "a ruined stone keep on a rocky outcrop, crumbling towers " +
            "overgrown with ivy, rubble and boulders below",
        "loc_ancient_ruins" to "ancient collapsed temple ruins, broken marble columns overgrown " +
            "with vines and creeping roots",
        "loc_battlefield" to "an aftermath battlefield littered with broken swords, spears and " +
            "shields, a flock of crows circling overhead",
        "loc_dungeon" to "a stone dungeon corridor lined with barred cells and lit torches, " +
            "chains hanging on the walls",
        "loc_haunted_house" to "a decrepit abandoned thatched cottage in a dead swamp forest, " +
            "broken windows and a sagging roof",
        "loc_swamp" to "a misty swamp with a crumbling wooden cottage, twisted dead trees and a " +
            "half-sunken plank path",
        "loc_volcano" to "an erupting volcano with lava flowing down its slope, smoke billowing " +
            "from the crater, ruins in the distance",
        "loc_waterfall" to "a waterfall cascading down a rocky cliff into a forest stream, trees " +
            "and boulders on both banks",
        "loc_wizard_cove" to "a wizard's hidden study carved into rock, shelves of potions and " +
            "books, a cauldron over a fire, a spiral staircase",
        "loc_wizard_tower" to "a tall spired wizard's tower on a rocky outcrop, arcane runes " +
            "carved in stone, gnarled trees around its base",
    )

    val LOCATIONS: List<String> = DESCRIPTIONS.keys.toList()

    // Il testo pronto per il prompt: "nome: descrizione" una riga per
    // location. Costruito qui (non nel PromptBuilder) perché il formato
    // è parte del vocabolario, non della composizione del prompt.
    val PROMPT_DICTIONARY: String = DESCRIPTIONS.entries.joinToString("\n") { (name, desc) -> "$name: $desc" }

    // Nullable apposta: sia "non dichiarato" (null) sia "dichiarato con
    // un placeholder morto" (una stringa che non è nel catalogo, es. i
    // vecchi "inn"/"city" del sample) devono contare come "non valido" —
    // stessa domanda, la stessa risposta.
    fun isValid(name: String?): Boolean = name != null && name in LOCATIONS
}
