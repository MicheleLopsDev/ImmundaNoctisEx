package io.github.luposolitario.immundanoctisex.inference

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.Scene

// Ciò che la UI mostra dopo l'inferenza: prosa arricchita + testi delle
// scelte tradotti + nome del nemico tradotto. Ogni campo ha SEMPRE un
// valore: se il parsing perde un pezzo si degrada sull'originale del
// pacchetto (vincolo: il gioco non si blocca mai).
data class EnrichedScene(
    val narrative: String,
    val choiceTexts: Map<String, String>,
    val disciplineChoiceTexts: Map<String, String>,
    val enemyName: String?,
    // Nome dello sfondo scelto per la scena (SceneImageCatalog), non un
    // drawable: la UI risolve il nome in risorsa, il parser non sa nulla
    // di Android. Null = nessuna scelta valida, resta il default.
    val backgroundImage: String? = null,
)

// Parsing dell'output di Gemma (ARCHITETTURA §inference, ANALISI-FLUSSO-
// PROMPT-V1): il modello scrive la prosa, poi il separatore, poi una riga
// per scelta nel formato pipe. In v1 erano tag XML e il modello li
// sbagliava mandando in stallo il parser: il formato pipe è nato per
// questo, e il TESTO È SEMPRE L'ULTIMO CAMPO — si splitta a limite, così
// un pipe dentro la narrazione non rompe nulla.
object ResponseParser {

    const val SEPARATOR = "--- TAGS ---"

    // La sola parte da mostrare in streaming: tutto ciò che precede il
    // separatore (se il modello non lo scrive mai, è tutto narrativa).
    fun narrativeOf(raw: String): String =
        raw.split(SEPARATOR, limit = 2).first().trim()

    fun parse(raw: String, scene: Scene): EnrichedScene {
        val narrative = narrativeOf(raw).ifBlank { scene.narrativeText }
        val tagBlock = raw.split(SEPARATOR, limit = 2).getOrNull(1).orEmpty()
        val lines = tagBlock.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

        val parsedChoices = lines.mapNotNull(::parseChoiceLine)
        val parsedDisciplines = lines.mapNotNull(::parseDisciplineLine)

        return EnrichedScene(
            narrative = narrative,
            choiceTexts = resolveChoices(scene.choices, parsedChoices),
            disciplineChoiceTexts = resolveDisciplineChoices(scene.disciplineChoices, parsedDisciplines),
            enemyName = lines.firstNotNullOfOrNull(::parseEnemyLine) ?: scene.combat?.enemyName,
            // Il pacchetto vince SEMPRE se l'autore ha già dichiarato uno
            // sfondo: Gemma è un ripiego per le scene che ne sono prive,
            // mai una sovrascrittura di una scelta dell'autore.
            backgroundImage = scene.backgroundImage ?: lines.firstNotNullOfOrNull(::parseImageLine),
        )
    }

    // CHOICE|sceneId|progressivo|testo tradotto
    private fun parseChoiceLine(line: String): ParsedChoice? {
        if (!line.startsWith("CHOICE|")) return null
        val parts = line.split("|", limit = 4)
        if (parts.size < 4) return null
        val text = parts[3].trim()
        if (text.isEmpty()) return null
        return ParsedChoice(sceneId = parts[1].trim(), progressive = parts[2].trim(), text = text)
    }

    // DISCIPLINE|disciplineId|testo tradotto
    private fun parseDisciplineLine(line: String): ParsedDiscipline? {
        if (!line.startsWith("DISCIPLINE|")) return null
        val parts = line.split("|", limit = 3)
        if (parts.size < 3) return null
        val text = parts[2].trim()
        if (text.isEmpty()) return null
        return ParsedDiscipline(disciplineId = parts[1].trim(), text = text)
    }

    // ENEMY|nome tradotto
    private fun parseEnemyLine(line: String): String? {
        if (!line.startsWith("ENEMY|")) return null
        return line.split("|", limit = 2).getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    // IMAGE|nome — VOCABOLARIO CHIUSO: un nome che Gemma inventa (o
    // storpia) viene scartato qui, non arriva mai alla UI. Nessun
    // fallback rumoroso: il gioco resta sullo sfondo di default.
    private fun parseImageLine(line: String): String? {
        if (!line.startsWith("IMAGE|")) return null
        val name = line.split("|", limit = 2).getOrNull(1)?.trim() ?: return null
        return name.takeIf(SceneImageCatalog::isValid)
    }

    // Aggancio delle righe alle scelte vere. Prima per destinazione
    // (sceneId), che è il dato che il modello sbaglia meno; se non basta,
    // FALLBACK PER CONTEGGIO (ANALISI-FLUSSO-PROMPT-V1): righe in ordine
    // sulle scelte in ordine. Ogni scelta senza riga tiene il suo testo
    // originale.
    private fun resolveChoices(
        choices: List<Choice>,
        parsed: List<ParsedChoice>,
    ): Map<String, String> {
        if (choices.isEmpty()) return emptyMap()
        val byDestination = parsed.groupBy { it.sceneId }
        val unmatched = parsed.toMutableList()

        val resolved = mutableMapOf<String, String>()
        choices.forEach { choice ->
            val match = byDestination[choice.nextSceneId]?.firstOrNull { it in unmatched }
            if (match != null) {
                unmatched.remove(match)
                resolved[choice.id] = match.text
            }
        }
        // Fallback per conteggio sulle scelte rimaste scoperte.
        val leftovers = unmatched.iterator()
        choices.filterNot { resolved.containsKey(it.id) }.forEach { choice ->
            resolved[choice.id] = if (leftovers.hasNext()) leftovers.next().text else choice.choiceText
        }
        return resolved
    }

    private fun resolveDisciplineChoices(
        choices: List<DisciplineChoice>,
        parsed: List<ParsedDiscipline>,
    ): Map<String, String> {
        if (choices.isEmpty()) return emptyMap()
        val byDiscipline = parsed.associateBy { it.disciplineId }
        return choices.associate { choice ->
            choice.id to (byDiscipline[choice.disciplineId]?.text ?: choice.choiceText)
        }
    }

    private data class ParsedChoice(val sceneId: String, val progressive: String, val text: String)

    private data class ParsedDiscipline(val disciplineId: String, val text: String)
}
