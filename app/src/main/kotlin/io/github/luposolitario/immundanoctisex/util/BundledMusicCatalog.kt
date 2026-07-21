package io.github.luposolitario.immundanoctisex.util

// Le 4 tracce composte da Michele (origina_res/), incluse nell'APK
// (assets/music/). Catalogo chiuso (22/07/2026, richiesta Michele: "una
// combo con le canzoni che ho fatto, non un picker") — niente file
// esterni per ora, solo queste.
data class BundledTrack(val id: String, val displayName: String, val assetPath: String)

object BundledMusicCatalog {
    val TRACKS: List<BundledTrack> = listOf(
        BundledTrack("esplorazione", "Esplorazione", "music/esplorazione_Where_The_Statues_Kneel.mp3"),
        BundledTrack("combattimento", "Combattimento", "music/combattimento_The_Iron_Vow.mp3"),
        BundledTrack("mercato", "Mercato", "music/mercato_What_Is_the_Fee_.mp3"),
        BundledTrack("romantico", "Romantico", "music/romantico_Breath_and_Bone.mp3"),
    )

    val default: BundledTrack = TRACKS.first()

    // Un id non riconosciuto (catalogo cambiato, preferenza vecchia)
    // degrada sul default, mai un crash.
    fun byId(id: String?): BundledTrack = TRACKS.firstOrNull { it.id == id } ?: default
}
