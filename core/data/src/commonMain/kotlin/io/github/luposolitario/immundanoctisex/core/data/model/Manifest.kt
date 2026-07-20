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
)
