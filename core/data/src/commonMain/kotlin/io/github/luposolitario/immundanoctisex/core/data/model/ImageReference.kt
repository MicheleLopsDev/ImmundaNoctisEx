package io.github.luposolitario.immundanoctisex.core.data.model

// Riferimento a un'immagine dichiarata da una scena (backgroundImage,
// npcImage) o da un combattimento (Combat.enemyImage). Il prefisso è
// l'UNICO discriminante fra le due forme possibili, niente lookup
// implicito (29/07/2026, Michele):
// - "static:<id>" -> ID del catalogo bundle nell'APK (SceneImageCatalog/
//   NpcImageCatalog/EnemyImageCatalog), risolto a un drawable.
// - "url:<http/https>" -> link diretto a un file esterno, per libri di
//   uso personale mai distribuiti (doc/LIBRI/): solo http/https, mai
//   file:// o altri schemi, per non aprire un accesso a percorsi locali
//   arbitrari.
sealed class ImageReference {
    data class Static(val catalogId: String) : ImageReference()
    data class Url(val url: String) : ImageReference()

    companion object {
        const val STATIC_PREFIX = "static:"
        const val URL_PREFIX = "url:"

        fun parse(raw: String): ImageReference? = when {
            raw.startsWith(STATIC_PREFIX) -> Static(raw.removePrefix(STATIC_PREFIX))
            raw.startsWith(URL_PREFIX) -> Url(raw.removePrefix(URL_PREFIX))
            else -> null
        }
    }
}
