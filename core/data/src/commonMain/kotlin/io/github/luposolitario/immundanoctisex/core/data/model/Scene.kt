package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class SceneType {
    START,
    TRANSITION,
    ENDING,
}

// Come è andata a finire. L'autore lo dichiara sulle scene ENDING:
// il motore NON lo indovina, perché un finale amaro raggiunto vivo e una
// vittoria si somigliano troppo per essere dedotti dallo stato del gioco
// (decisione di Michele, 20/07/2026 — vedi DIARIO).
//
// NEUTRAL è il default di chi non dichiara nulla: il gioco dice comunque
// che l'avventura è finita, senza mentire sull'esito.
@Serializable
enum class EndingOutcome {
    VICTORY,
    DEFEAT,
    NEUTRAL,
}

// Una scena del libro. locationName è opzionale e appiccicoso: se assente
// eredita dalla scena precedente nel percorso (decisione post-specifica 4),
// l'autore lo scrive solo quando il luogo cambia.
@Serializable
data class Scene(
    val id: String,
    val sceneType: SceneType,
    val genre: String,
    val toneHints: List<String> = emptyList(),
    val backgroundImage: String? = null,
    // ID canonico dal catalogo NPC (NpcImageCatalog), stesso pattern di
    // backgroundImage: dichiarato dall'autore, mai da Gemma. Dato pronto
    // all'uso (22/07/2026); l'aggancio UI (dove mostrarlo) resta da
    // decidere — vedi DIARIO.md.
    val npcImage: String? = null,
    val locationName: String? = null,
    val narrativeText: String,
    val choices: List<Choice> = emptyList(),
    val disciplineChoices: List<DisciplineChoice> = emptyList(),
    val combat: Combat? = null,
    val gameMechanics: List<GameMechanic> = emptyList(),
    // Solo per sceneType ENDING: come si chiude l'avventura. Assente su
    // una scena di finale = NEUTRAL (l'esito non viene inventato).
    val outcome: EndingOutcome? = null,
    // Effetto sonoro personalizzato (30/07/2026, Michele: "il creatore
    // del libro può scegliere un sfx con un suo mp3 da un url... dobbiamo
    // capire come semplificare la cosa"): se valorizzato, SOVRASCRIVE il
    // suono automatico ricavato dal nome dell'immagine static: (invariato
    // quando è null — nessun libro esistente cambia comportamento).
    // Non un url: diretto come le immagini: un ID che DEVE corrispondere
    // a una voce già registrata in `Manifest.customResources.sounds`
    // (§15.7) — stesso principio del vocabolario chiuso dei toni (§15.2):
    // "aggiungere risorse deve essere una cosa seria e voluta", e riusare
    // lo stesso ID su più scene evita di duplicare inutilmente la stessa
    // risorsa in cache lato client. Validato da SfxValidator.
    val sfx: String? = null,
    // Bonus/malus condizionali sul tiro della Tabella dei Numeri Casuali
    // (01/08/2026): si sommano al tiro PRIMA di cercare quale intervallo
    // minRoll/maxRoll lo copre. Vuota = comportamento di sempre, il tiro
    // grezzo decide da solo — nessun libro esistente cambia. Vedi
    // RollModifier.kt per il perché stiano sulla scena e non sulle
    // scelte, e RollModifiers.kt (:core:engine) per la valutazione.
    val rollModifiers: List<RollModifier> = emptyList(),
)
