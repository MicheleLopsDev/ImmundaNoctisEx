package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.CustomResourceEntry
import io.github.luposolitario.immundanoctisex.core.data.model.ImageReference
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URL

// Istantanea delle risorse statiche del client Android (30/07/2026,
// Michele: "creiamo un file json con solo la lista delle risorse
// statiche... e facciamo che l'editor lo carica"). Oggi la fonte di
// verità resta SceneImageCatalog.kt/NpcImageCatalog.kt/
// EnemyImageCatalog.kt/NarrativeTonePreferences.kt in :app (invariati,
// l'editor non li tocca) — questo file legge una COPIA
// (`static-resources.json`, impacchettata
// come risorsa di :tool insieme alle immagini stesse in
// `resources/images/`, così funziona anche nell'.exe standalone senza
// bisogno del repository intorno). Quando si deciderà il passaggio a
// un registro dinamico anche per il client, questo sarà il punto da
// rivedere.
@Serializable
data class LocationResource(val id: String, val description: String = "")

// Vocabolario chiuso dei toni (§15.2, Michele: "i toni devono essere
// fissi tra quelli che dispone l'app") — stessi valori di
// NarrativeTone (:app, util/NarrativeTonePreferences.kt), SENZA la voce
// AUTHOR (che significa "nessuna sovrascrittura", non ha senso come
// scelta di scrittura per l'autore del libro). `hints` sono le parole
// grezze che finiscono per davvero in Scene.toneHints/Manifest.
// toneHints (viste dal prompt come "tone_hints") — l'autore sceglie il
// tono per NOME, l'editor scrive le parole al posto suo.
@Serializable
data class ToneResource(val id: String, val displayName: String, val hints: List<String> = emptyList())

@Serializable
data class StaticResourceRegistry(
    val tones: List<ToneResource> = emptyList(),
    val locations: List<LocationResource> = emptyList(),
    val npcs: List<String> = emptyList(),
    val enemies: List<String> = emptyList(),
)

object StaticResourceCatalog {
    val registry: StaticResourceRegistry by lazy { caricaRegistro() }

    // Nessun errore se il file manca o è malformato: un catalogo vuoto
    // degrada silenziosamente (niente anteprime/liste a discesa), mai un
    // crash — stessa filosofia di tutto il resto del progetto.
    private fun caricaRegistro(): StaticResourceRegistry {
        val flusso = javaClass.classLoader.getResourceAsStream("static-resources.json")
            ?: return StaticResourceRegistry()
        val testo = flusso.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return runCatching {
            Json { ignoreUnknownKeys = true }.decodeFromString<StaticResourceRegistry>(testo)
        }.getOrDefault(StaticResourceRegistry())
    }

    // URL della risorsa bundled (funziona sia da classpath di sviluppo
    // sia da dentro il jar/distribuibile impacchettato) per un dato ID
    // catalogo — null se l'immagine non è (ancora) presente in
    // `resources/images/`.
    fun percorsoImmagine(id: String): URL? = javaClass.classLoader.getResource("images/$id.jpg")

    // §15.6 (Michele: "gestire anche i file audio associati alle
    // risorse... e dai la possibilità di essere ascoltati"): un suono
    // ambientale per location (`assets/sfx/images/loc_*.mp3` in :app,
    // stessa convenzione di nome — copia bundled in `resources/sounds/`).
    // Non tutte le location ne hanno uno (23 su 36 al momento della
    // copia): null è il caso normale, non un errore.
    fun percorsoSuono(id: String): URL? = javaClass.classLoader.getResource("sounds/$id.mp3")
}

// Libreria di suoni pronta all'uso (§18.4, Michele: "mi crei per default
// già tutti gli id per i suoni statici presenti nel apk così c'è già una
// libreria di suoni e si devono creare solo quelli custom legati agli
// url"): una voce di customResources.sounds per ogni location che ha
// DAVVERO un mp3 bundlato — id uguale all'ID della location stessa
// (nessuno schema di nomi nuovo da inventare: è già l'ID canonico,
// prevedibile per chiunque legga il JSON), valore "static:<stesso id>".
// Usata sia alla creazione di un libro nuovo (BookScaffolds.kt) sia dal
// pulsante "+ Aggiungi tutti i suoni del catalogo" nel pannello risorse
// (MapScreen.kt) per i libri già esistenti.
fun suoniStaticiDiDefault(): List<CustomResourceEntry> =
    StaticResourceCatalog.registry.locations
        .filter { StaticResourceCatalog.percorsoSuono(it.id) != null }
        .map { CustomResourceEntry(id = it.id, url = "${ImageReference.STATIC_PREFIX}${it.id}") }
