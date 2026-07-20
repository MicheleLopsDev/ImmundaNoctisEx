package io.github.luposolitario.immundanoctisex.inference

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.Gender
import io.github.luposolitario.immundanoctisex.core.data.model.Scene

// Tutto ciò che serve per arricchire UNA scena. Nota: NON c'è il diario.
// L'inferenza è SENZA MEMORIA (CRITICITA.md): una sessione Gemma nuova
// per scena, contesto = frammenti fissi + coda della scena precedente +
// scena + continuazioni. Il diario non entra MAI nel prompt.
data class PromptContext(
    val scene: Scene,
    val previousSceneText: String?,
    val continuations: List<String>,
    val choices: List<Choice>,
    val disciplineChoices: List<DisciplineChoice>,
    val sourceLanguage: String,
    val userLanguage: String,
    val genre: String,
    val toneHints: List<String>,
    val playerGender: Gender,
    // La scena è il finale fabbricato dal motore (AdventureEnding): non
    // c'è testo sorgente, il narratore deve scrivere il finale.
    val isSyntheticEnding: Boolean = false,
)

// Compone il prompt riempiendo i placeholder dei frammenti (v1:
// buildGemmaPromptForScene, provato su Gemma 3 — qui con lingue
// parametriche e toni dalla scena invece che hardcoded).
class PromptBuilder(private val fragments: PromptFragments = PromptFragments.DEFAULTS) {

    fun build(context: PromptContext): String {
        val sections = buildList {
            add(fragments.baseText)
            // Le sezioni vuote NON si scrivono: un "[THE STORY SO FAR]"
            // seguito dal nulla confonde il modello e spreca contesto.
            if (!context.previousSceneText.isNullOrBlank()) add(fragments.previousSceneText)
            // Un finale fabbricato non ha testo da arricchire: si chiede al
            // narratore di SCRIVERLO. Senza questo, il prompt conterrebbe
            // una scena vuota e il modello inventerebbe a caso.
            if (context.isSyntheticEnding) {
                add(fragments.syntheticEndingText)
            } else {
                add(fragments.sceneText)
            }
            if (context.continuations.isNotEmpty()) add(fragments.continuationsText)
            if (context.choices.isNotEmpty() || context.disciplineChoices.isNotEmpty()) {
                add(fragments.choicesText)
            }
            // L'enfasi sul soprannaturale si spende solo quando c'è
            // davvero una disciplina in gioco: contesto sprecato altrimenti.
            if (context.disciplineChoices.isNotEmpty()) add(fragments.disciplineEmphasisText)
            add(
                if (context.isSyntheticEnding) {
                    fragments.syntheticEndingConstraintText
                } else {
                    fragments.constraintText
                },
            )
            add(outputFormat(context))
            add(fragments.closingText)
        }
        return fill(sections.joinToString("\n\n"), context)
    }

    // La riga ENEMY si chiede solo se c'è davvero un nemico da nominare.
    private fun outputFormat(context: PromptContext): String =
        if (context.scene.combat != null) {
            "${fragments.outputFormatText}\n${fragments.enemyFormatText}"
        } else {
            fragments.outputFormatText
        }

    private fun fill(template: String, context: PromptContext): String {
        val toneHints = context.toneHints.takeIf { it.isNotEmpty() }
            ?.joinToString(", ")
            ?: "neutral"
        return template
            .replace("{previous_scene_text}", context.previousSceneText.orEmpty())
            .replace("{source_language}", context.sourceLanguage)
            .replace("{scene_narrative_text}", context.scene.narrativeText)
            .replace("{continuations_text}", context.continuations.joinToString("\n"))
            .replace("{choices_text}", choicesBlock(context))
            .replace("{user_language}", context.userLanguage)
            .replace("{genre}", context.genre)
            .replace("{tone_hints}", toneHints)
            .replace("{player_gender}", playerGender(context.playerGender))
    }

    // Le scelte si consegnano NELLA STESSA FORMA che il modello deve
    // restituire: tradurre diventa un lavoro meccanico riga per riga, e
    // sceneId/progressivo tornano indietro corretti per l'aggancio.
    private fun choicesBlock(context: PromptContext): String {
        val choiceLines = context.choices.mapIndexed { index, choice ->
            "CHOICE|${choice.nextSceneId}|${index + 1}|${choice.choiceText}"
        }
        val disciplineLines = context.disciplineChoices.map { choice ->
            "DISCIPLINE|${choice.disciplineId}|${choice.choiceText}"
        }
        val enemyLine = context.scene.combat?.let { listOf("ENEMY|${it.enemyName}") }.orEmpty()
        return (choiceLines + disciplineLines + enemyLine).joinToString("\n")
    }

    // In inglese perché il prompt è in inglese: serve al modello per gli
    // accordi grammaticali dell'italiano (UI.md §Convenzioni).
    private fun playerGender(gender: Gender): String = when (gender) {
        Gender.MALE -> "male"
        Gender.FEMALE -> "female"
    }
}
