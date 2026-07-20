package io.github.luposolitario.immundanoctisex.inference

// Vocabolario CHIUSO delle immagini di sfondo disponibili (richiesta
// Michele 20/07/2026, esperimento — vedi DIARIO.md): stessa disciplina
// degli effetti sonori pensati in UPGRADE.md, "il modello sceglie tra ID
// che esistono davvero... ID sconosciuto = silenzio, mai un errore".
//
// UNICA FONTE DI VERITÀ per tre usi diversi: il prompt la mostra a Gemma
// come elenco di scelta, il parser la usa per scartare nomi inventati, la
// UI la usa per risolvere il drawable. Se cambiano i nomi delle risorse
// in drawable-nodpi, si aggiorna solo qui.
//
// Solo LOCAZIONI (loc_*): npc_*/enemy_* sono ritratti di personaggi, non
// sfondi di scena — semanticamente un'altra cosa, fuori da questo giro.
object SceneImageCatalog {
    val LOCATIONS: List<String> = listOf(
        "loc_black_gate",
        "loc_caves",
        "loc_crypt",
        "loc_cursed_castle",
        "loc_forest",
        "loc_forest_prey",
        "loc_graveyard",
        "loc_harbor",
        "loc_helgedad",
        "loc_helgedad_gate",
        "loc_infernal_city",
        "loc_kai_monastery",
        "loc_market",
        "loc_monastery_dawn",
        "loc_mountain",
        "loc_smithy_exterior",
        "loc_smithy_interior",
        "loc_standing_stones",
        "loc_tavern",
        "loc_tomb_exterior",
        "loc_tomb_interior",
    )

    // Nullable apposta: sia "non dichiarato" (null) sia "dichiarato con
    // un placeholder morto" (una stringa che non è nel catalogo, es. i
    // vecchi "inn"/"city" del sample) devono contare come "non valido" —
    // stessa domanda, la stessa risposta.
    fun isValid(name: String?): Boolean = name != null && name in LOCATIONS
}
