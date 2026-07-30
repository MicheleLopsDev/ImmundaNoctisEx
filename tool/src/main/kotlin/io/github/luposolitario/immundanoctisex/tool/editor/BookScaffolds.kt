package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.EndingOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import io.github.luposolitario.immundanoctisex.tool.defaultDisciplineDescriptors

// Tre scaffold di partenza per un libro nuovo (doc/EDITOR.md §9.2) —
// esempi funzionanti di come si struttura un libro a scelte, non un
// foglio bianco. Tutti e tre includono già la rete di sicurezza (§9.3):
// una scena DEFEAT collegata a deathSceneId fin dall'inizio.
enum class Scaffold(val etichetta: String) {
    BASE("Base — solo inizio e fine, scrivi tu il resto"),
    LINEARE("Lineare — una sequenza di 5 scene"),
    RAMIFICATO("Ramificato — un bivio che confluisce in un finale comune"),
}

fun creaManifestNuovo(titolo: String, genere: String, toneHints: List<String>, scaffold: Scaffold): Manifest {
    val scene = when (scaffold) {
        Scaffold.BASE -> scaffoldBase()
        Scaffold.LINEARE -> scaffoldLineare()
        Scaffold.RAMIFICATO -> scaffoldRamificato()
    }
    return Manifest(
        id = idDaTitolo(titolo),
        version = "1.0.0",
        title = titolo,
        description = "",
        language = "en",
        genre = genere,
        toneHints = toneHints,
        disciplineChoices = defaultDisciplineDescriptors(),
        deathSceneId = "morte",
        scenes = scene + sceneMorte(),
    )
}

private fun idDaTitolo(titolo: String): String =
    titolo.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "libro-nuovo" }

// Rete di sicurezza (§9.3): non raggiungibile da nessuna scelta — non è
// un arco sulla mappa apposta, è un fallback implicito del motore
// (REGOLE.md, morte fuori combattimento). Appare come nodo isolato
// sulla mappa: previsto, non un errore da correggere.
private fun sceneMorte() = Scene(
    id = "morte",
    sceneType = SceneType.ENDING,
    genre = "FANTASY",
    narrativeText = "La tua avventura finisce qui.",
    outcome = EndingOutcome.DEFEAT,
)

private fun sceneVuota(id: String, tipo: SceneType, testo: String, vararg scelte: Choice) = Scene(
    id = id,
    sceneType = tipo,
    genre = "FANTASY",
    narrativeText = testo,
    choices = scelte.toList(),
)

private fun scaffoldBase(): List<Scene> = listOf(
    sceneVuota("1", SceneType.START, "Scrivi qui l'inizio della tua storia.", Choice("c1", "Vai avanti", nextSceneId = "2")),
    sceneVuota("2", SceneType.ENDING, "Scrivi qui come finisce la storia.").copy(outcome = EndingOutcome.VICTORY),
)

private fun scaffoldLineare(): List<Scene> {
    val intermedie = (2..4).map { n ->
        sceneVuota("$n", SceneType.TRANSITION, "Scrivi qui la scena $n.", Choice("c$n", "Vai avanti", nextSceneId = "${n + 1}"))
    }
    return listOf(
        sceneVuota("1", SceneType.START, "Scrivi qui l'inizio della tua storia.", Choice("c1", "Vai avanti", nextSceneId = "2")),
    ) + intermedie + sceneVuota("5", SceneType.ENDING, "Scrivi qui come finisce la storia.").copy(outcome = EndingOutcome.VICTORY)
}

private fun scaffoldRamificato(): List<Scene> = listOf(
    sceneVuota("1", SceneType.START, "Scrivi qui l'inizio della tua storia.", Choice("c1", "Vai avanti", nextSceneId = "2")),
    sceneVuota(
        "2", SceneType.TRANSITION, "Scrivi qui il bivio: due strade diverse si aprono.",
        Choice("c2a", "Percorso A", nextSceneId = "3"),
        Choice("c2b", "Percorso B", nextSceneId = "5"),
    ),
    sceneVuota("3", SceneType.TRANSITION, "Scrivi qui la prima scena del percorso A.", Choice("c3", "Vai avanti", nextSceneId = "4")),
    sceneVuota("4", SceneType.TRANSITION, "Scrivi qui la seconda scena del percorso A.", Choice("c4", "Vai avanti", nextSceneId = "7")),
    sceneVuota("5", SceneType.TRANSITION, "Scrivi qui la prima scena del percorso B.", Choice("c5", "Vai avanti", nextSceneId = "6")),
    sceneVuota("6", SceneType.TRANSITION, "Scrivi qui la seconda scena del percorso B.", Choice("c6", "Vai avanti", nextSceneId = "7")),
    sceneVuota("7", SceneType.ENDING, "Scrivi qui come finisce la storia, dopo la confluenza dei due percorsi.").copy(outcome = EndingOutcome.VICTORY),
)
