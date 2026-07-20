package io.github.luposolitario.immundanoctisex.core.engine.ending

import io.github.luposolitario.immundanoctisex.core.data.model.EndingOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType

// UN'AVVENTURA FINISCE SEMPRE DICHIARANDO COM'È ANDATA (richiesta di
// Michele, 20/07/2026: "non può finire così, deve darti sempre una scena
// di vittoria o morte anche se non c'è la scena").
//
// Prima di questo, un libro senza `deathSceneId` lasciava il giocatore a
// vagare con Resistenza <= 0, e un ENDING qualunque non diceva né vinto
// né perso: si tornava al menu senza sapere com'era andata.
//
// Qui non si INDOVINA l'esito: lo dichiara l'autore con Scene.outcome.
// L'unica deduzione ammessa è la morte built-in, che il motore conosce
// già per conto suo (REGOLE.md §2.1).
object AdventureEnding {

    // ID canonico della scena di finale fabbricata quando il pacchetto non
    // ne ha una. Canonico e non localizzato: il TESTO lo mette la UI da
    // strings.xml, perché :core:engine non dipende da Android.
    const val SYNTHETIC_DEFEAT_SCENE_ID = "__ex_synthetic_defeat__"

    // Come è andata a finire. Ordine dei controlli:
    // 1. la morte built-in batte tutto (ci si arriva morendo, punto);
    // 2. poi quello che ha dichiarato l'autore;
    // 3. in mancanza, NEUTRAL: l'avventura è finita, e non si mente.
    fun outcomeOf(manifest: Manifest, scene: Scene): EndingOutcome = when {
        scene.sceneType != SceneType.ENDING -> EndingOutcome.NEUTRAL
        scene.id == SYNTHETIC_DEFEAT_SCENE_ID -> EndingOutcome.DEFEAT
        scene.id == manifest.deathSceneId -> EndingOutcome.DEFEAT
        else -> scene.outcome ?: EndingOutcome.NEUTRAL
    }

    // Il manifest con la GARANZIA che una scena di morte esista sempre.
    // Si applica una volta al caricamento: da lì in poi tutto il motore
    // lavora su un grafo dove `deathSceneId` punta a qualcosa di vero, e
    // nessun'altra regola ha bisogno di sapere che la scena è fabbricata.
    //
    // Interviene in due casi, entrambi visti sul campo:
    // - `deathSceneId` assente -> la morte built-in non era attiva e si
    //   giocava da morti;
    // - `deathSceneId` che punta a una scena inesistente -> il salto non
    //   trovava destinazione e il gioco restava fermo dov'era.
    fun withGuaranteedEnding(manifest: Manifest): Manifest {
        val declared = manifest.deathSceneId
        val exists = declared != null && manifest.scenes.any { it.id == declared }
        if (exists) return manifest
        return manifest.copy(
            deathSceneId = SYNTHETIC_DEFEAT_SCENE_ID,
            scenes = manifest.scenes + syntheticDefeatScene(manifest),
        )
    }

    // La scena fabbricata NASCE SENZA TESTO, di proposito: il narratore
    // proverà a scriverlo, e se non può (modello assente o generazione
    // fallita) la UI mette il testo fisso di strings.xml. Un finale si
    // vede comunque — il gioco non si blocca mai.
    private fun syntheticDefeatScene(manifest: Manifest): Scene = Scene(
        id = SYNTHETIC_DEFEAT_SCENE_ID,
        sceneType = SceneType.ENDING,
        genre = manifest.genre,
        toneHints = manifest.toneHints,
        narrativeText = "",
        outcome = EndingOutcome.DEFEAT,
    )
}
