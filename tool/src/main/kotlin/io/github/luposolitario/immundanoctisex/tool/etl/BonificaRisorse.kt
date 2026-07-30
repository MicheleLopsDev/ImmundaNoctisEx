package io.github.luposolitario.immundanoctisex.tool.etl

import io.github.luposolitario.immundanoctisex.core.data.model.CustomResourceEntry
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene

// "Bonifica" delle risorse url: già in uso (30/07/2026, Michele: "vorrei
// poter anche vedere la lista delle risorse già presenti... sistemare i
// file json... bonificando i file in modo da usare il nuovo standard").
// Non tocca le scene (nessun campo backgroundImage/npcImage/enemyImage
// viene riscritto): registra soltanto in `customResources.images` ogni
// link url: GIÀ presente nel libro, con un ID leggibile derivato
// dall'ultimo pezzo dell'URL — così l'editor mostra un vero elenco di
// risorse invece di link sparsi senza nome. Additiva: le voci già
// registrate (per url) non vengono duplicate né toccate.
fun bonificaRisorseImmagine(manifest: Manifest): Manifest {
    val urlGiaUsati = manifest.scenes.flatMap { it.urlImmagini() }.distinct()
    val urlGiaRegistrati = manifest.customResources.images.map { it.url }.toSet()
    val idGiaUsati = manifest.customResources.images.map { it.id }.toMutableSet()

    val nuoveVoci = urlGiaUsati
        .filter { it !in urlGiaRegistrati }
        .map { url -> CustomResourceEntry(idLeggibile(url, idGiaUsati), url) }

    if (nuoveVoci.isEmpty()) return manifest
    return manifest.copy(
        customResources = manifest.customResources.copy(images = manifest.customResources.images + nuoveVoci),
    )
}

private fun Scene.urlImmagini(): List<String> =
    listOfNotNull(backgroundImage, npcImage, combat?.enemyImage)
        .filter { it.startsWith("url:") }
        .map { it.removePrefix("url:") }

// Ultimo pezzo del percorso, senza estensione (es.
// ".../01fftd/ill17.png" -> "ill17") — leggibile per un umano, a
// differenza dell'URL intero. In caso di collisione (stesso nome base
// da libri/percorsi diversi, raro ma possibile) aggiunge un contatore.
private fun idLeggibile(url: String, idGiaUsati: MutableSet<String>): String {
    val base = url.substringAfterLast('/').substringBeforeLast('.').ifBlank { "risorsa" }
    var candidato = base
    var contatore = 2
    while (candidato in idGiaUsati) {
        candidato = "$base-$contatore"
        contatore++
    }
    idGiaUsati += candidato
    return candidato
}
