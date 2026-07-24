package io.github.luposolitario.immundanoctisex.util

// Le tracce composte da Michele (origina_res/), incluse nell'APK
// (assets/music/). Catalogo chiuso (22/07/2026, richiesta Michele: "una
// combo con le canzoni che ho fatto, non un picker") — niente file
// esterni per ora, solo queste.
data class BundledTrack(val id: String, val displayName: String, val assetPath: String)

object BundledMusicCatalog {
    // Nome per esteso (Michele 22/07/2026: "fai apparire il nome per
    // esteso dei file mp3"): categoria + titolo originale del brano.
    // "menu" in testa alla lista (24/07/2026, richiesta Michele: "il main
    // theme dell'app... mettila come default") — `default` è sempre la
    // prima della lista, nessun'altra riga da cambiare per farla tale.
    val TRACKS: List<BundledTrack> = listOf(
        BundledTrack("menu", "Main Theme — Destino Segnato", "music/menu_Destino_segnato.mp3"),
        BundledTrack("esplorazione", "Esplorazione — Where the Statues Kneel", "music/esplorazione_Where_The_Statues_Kneel.mp3"),
        BundledTrack("combattimento", "Combattimento — The Iron Vow", "music/combattimento_The_Iron_Vow.mp3"),
        BundledTrack("mercato", "Mercato — What Is the Fee?", "music/mercato_What_Is_the_Fee_.mp3"),
        BundledTrack("romantico", "Romantico — Breath and Bone", "music/romantico_Breath_and_Bone.mp3"),
        // Secondo giro di tracce (24/07/2026, Michele: "sto aggiungendo
        // altre musiche come quella del mercato"). Categoria assegnata a
        // orecchio dal titolo, non confermata da Michele — se sbagliata
        // basta cambiare la stringa qui, nessun'altra riga da toccare.
        BundledTrack("voto_di_ferro", "Combattimento — Il Voto di Ferro", "music/Il_Voto_di_Ferro.mp3"),
        BundledTrack("cuore_e_spada", "Romantico — Il Cuore e la Spada", "music/Il_cuore_e_la_spada.mp3"),
        BundledTrack("monete_per_un_fiore", "Mercato — Monete per un Fiore", "music/Monete_per_un_fiore.mp3"),
        BundledTrack("market_e_tower", "Mercato — Tra Market e Tower", "music/Tra_Market_e_Tower.mp3"),
        BundledTrack("eterno_ritorno", "Esplorazione — L'Eterno Ritorno", "music/L_eterno_ritorno.mp3"),
    )

    val default: BundledTrack = TRACKS.first()

    // Un id non riconosciuto (catalogo cambiato, preferenza vecchia)
    // degrada sul default, mai un crash.
    fun byId(id: String?): BundledTrack = TRACKS.firstOrNull { it.id == id } ?: default

    // Voce speciale del picker (24/07/2026, richiesta Michele: "un tasto
    // che se selezionato fa il random dei brani musicali") — NON è un
    // file vero (assetPath vuoto): MusicPlayer riconosce RANDOM_ID e
    // alterna le tracce vere di TRACKS invece di provare a caricarla.
    // Tenuta FUORI da TRACKS apposta, altrimenti playShuffle la
    // pescherebbe come se fosse un brano suonabile.
    const val RANDOM_ID = "random"
    val RANDOM_ENTRY = BundledTrack(RANDOM_ID, "🔀 Casuale (mescola tutte)", "")

    // Solo per il picker delle Opzioni: le tracce vere + Casuale in fondo.
    val PICKER_ENTRIES: List<BundledTrack> = TRACKS + RANDOM_ENTRY
}
