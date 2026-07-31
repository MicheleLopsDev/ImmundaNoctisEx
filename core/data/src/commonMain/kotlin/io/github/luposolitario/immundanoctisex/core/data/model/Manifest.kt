package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// Il pacchetto libro intero: metadati, catalogo discipline, grafo di scene.
// deathSceneId e globalRules sono opzionali (REGOLE.md Blocco 2); la
// morte built-in (Resistenza <= 0 fuori combattimento) usa deathSceneId,
// la vittoria è una globalRule come le altre, nessun campo dedicato.
// L'ESITO mostrato al giocatore è invece dichiarato sulla scena
// (Scene.outcome, REGOLE.md §2.2-bis): la scena di finale è garantita
// dal motore anche quando il pacchetto non ce l'ha.
@Serializable
data class Manifest(
    // Posizioni dei nodi sulla mappa dell'editor grafico (:tool,
    // EDITOR.md §19.12, Michele: "una mappa in testa con id e
    // posizioni che viene saltata dal client... in testa come elemento
    // separato... evita problemi con le diff"). Primo campo apposta:
    // in un JSON serializzato in ordine di dichiarazione, resta un
    // blocco isolato in cima al file invece di mescolarsi coi campi
    // di contenuto — chi guarda un diff del libro lo riconosce a
    // colpo d'occhio e può ignorarlo. Il motore/client non la legge
    // mai (§19.12).
    val scenePositions: Map<String, ScenePosition> = emptyMap(),
    val id: String,
    val version: String,
    val title: String,
    val description: String,
    val language: String,
    val genre: String,
    val toneHints: List<String> = emptyList(),
    val disciplineChoices: List<DisciplineDescriptor> = emptyList(),
    val deathSceneId: String? = null,
    val globalRules: List<GlobalRule> = emptyList(),
    val scenes: List<Scene> = emptyList(),
    // Risorse url: dell'autore (§15.7, EDITOR.md) — comodità per
    // l'editor, il motore non le legge mai: le scene salvano sempre il
    // link risolto direttamente in backgroundImage/npcImage/enemyImage.
    val customResources: CustomResources = CustomResources(),
)
