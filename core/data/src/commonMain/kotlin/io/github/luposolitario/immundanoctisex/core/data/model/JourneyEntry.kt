package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class CombatOutcome {
    WIN,
    LOSE,
    EVADE,
}

@Serializable
enum class AutoJumpReason {
    GLOBAL_RULE,
    CHECK_ITEM_AND_JUMP,
    IF_STAT,
    // Aggiunti in Fase 2: anche skillCheck e randomChoiceTable spostano il
    // giocatore di scena, e il diario-grafo deve dire per quale porta.
    SKILL_CHECK,
    RANDOM_CHOICE,
    BUILT_IN_DEATH,
}

// Come il giocatore ha lasciato la scena: non solo dove è stato, anche per
// quale porta è uscito (STATO.md Blocco 3).
@Serializable
sealed interface Transition {
    @Serializable
    data class ChoiceTaken(val choiceId: String) : Transition

    @Serializable
    data class DisciplineUsed(val disciplineId: String, val choiceId: String) : Transition

    @Serializable
    data class CombatResolved(val outcome: CombatOutcome) : Transition

    @Serializable
    data class AutoJump(val reason: AutoJumpReason) : Transition
}

// Un passo del diario-grafo. enrichedText è il testo generato da Gemma:
// si salva e non si rigenera mai (costo inferenza, non-determinismo,
// coerenza con previous_scene_text). La sequenza ordinata delle voci È
// il percorso completo nel grafo: visitedScenes non esiste come lista
// salvata, è derivabile da qui. locationName è il luogo GIÀ RISOLTO
// (ereditato dalla scena precedente se la scena non lo dichiara,
// decisione post-specifica 4): la Mappa logica non deve ricalcolare
// l'ereditarietà a lettura.
@Serializable
data class JourneyEntry(
    val sceneId: String,
    val enrichedText: String,
    val transition: Transition,
    val locationName: String? = null,
    // L'ID canonico dello sfondo della scena (SceneImageCatalog), per la
    // mappa del viaggio nel diario (05/08/2026): una miniatura per tappa
    // invece del solo nome del luogo.
    //
    // Si SALVA invece di risalire alla scena a ogni apertura perché il
    // diario deve restare leggibile anche se il libro cambia sotto —
    // stesso principio per cui si salva `enrichedText` invece di
    // rigenerarlo (STATO.md Blocco 3). In coda con default: i
    // salvataggi fatti prima di oggi si caricano senza miniatura.
    val backgroundImage: String? = null,
)
