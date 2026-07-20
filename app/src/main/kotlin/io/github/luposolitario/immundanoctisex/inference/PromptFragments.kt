package io.github.luposolitario.immundanoctisex.inference

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// I frammenti del prompt (ARCHITETTURA §inference): testo come DATO, non
// come codice — vivono in content/config.json (tag promptDescription
// "start_adventure_prompt") così si ritoccano senza ricompilare.
// I DEFAULTS qui sotto sono la rete: config assente, illeggibile o con un
// frammento mancante -> si usa il default, il gioco non si blocca mai.
data class PromptFragments(
    val baseText: String,
    val previousSceneText: String,
    val sceneText: String,
    // Sostituisce sceneText quando la scena è il finale FABBRICATO dal
    // motore (il pacchetto non ne aveva uno): non c'è testo da arricchire,
    // c'è un finale da scrivere. Vedi AdventureEnding.
    val syntheticEndingText: String,
    val continuationsText: String,
    val choicesText: String,
    // Le Discipline Kai non sono abilità comuni: le possiedono SOLO i Kai
    // (richiesta Michele 20/07/2026). Si aggiunge quando la scena mette in
    // gioco una disciplina, per non sprecare contesto quando non serve.
    val disciplineEmphasisText: String,
    val constraintText: String,
    // Il vincolo normale ordina di RISCRIVERE la scena sorgente: per un
    // finale fabbricato non esiste scena sorgente, e chiederglielo lo
    // metterebbe in contraddizione. Stesse regole di forma, compito
    // diverso.
    val syntheticEndingConstraintText: String,
    val outputFormatText: String,
    val enemyFormatText: String,
    // Esperimento (Michele 20/07/2026, vedi DIARIO.md): Gemma può
    // suggerire lo sfondo SOLO quando la scena non ne ha già uno
    // dichiarato (PromptBuilder lo aggiunge in quel caso soltanto —
    // l'autore ha sempre priorità). {available_locations} è il
    // vocabolario chiuso di SceneImageCatalog.
    val imageFormatText: String,
    val closingText: String,
) {
    companion object {
        // Allineati a content/config.json (in inglese: il prompt è in
        // inglese anche per output italiano, scelta provata su Gemma 3).
        val DEFAULTS = PromptFragments(
            baseText = "You are the narrator of an interactive gamebook. " +
                "Your task is to enrich and translate a scene for the player.",
            previousSceneText = "[THE STORY SO FAR]\n{previous_scene_text}",
            sceneText = "[CURRENT SCENE — source text in {source_language}]\n{scene_narrative_text}",
            // Il libro non ha una scena di finale: la scrive il narratore.
            // Poche righe e definitive — questa schermata chiude la partita.
            syntheticEndingText = "[FINAL SCENE — the source book has no ending for this outcome]\n" +
                "Write a short, final scene in which the hero's journey ends here in defeat. " +
                "It must read as an ENDING — closed, with weight — but leave one faint thread " +
                "open: a hint that this might not be the last word. Do not promise a sequel and " +
                "do not offer the player a choice. " +
                "Base it only on THE STORY SO FAR: do not invent new characters or places. " +
                "Three or four sentences, no more.",
            continuationsText = "[POSSIBLE CONTINUATIONS — for consistency only, do NOT reveal them]\n" +
                "{continuations_text}",
            choicesText = "[CHOICES TO TRANSLATE]\n{choices_text}",
            disciplineEmphasisText = "[KAI DISCIPLINES]\n" +
                "The hero is a Kai. Kai Disciplines are NOT ordinary skills: they are " +
                "preternatural gifts that only the Kai possess. When the scene shows one being " +
                "used, give it weight — the sense of something beyond common ability, uncanny to " +
                "anyone watching. Keep this to the telling: do NOT invent effects, powers or " +
                "outcomes beyond what the source text states.",
            constraintText = "Follow these instructions EXACTLY:\n" +
                "1. Rewrite the CURRENT SCENE text in {user_language}, enriching it with details " +
                "consistent with the '{genre}' genre and this tone: {tone_hints}. Keep all facts, " +
                "characters, items and events of the source text unchanged. Do NOT invent new events, " +
                "items or characters.\n" +
                "2. Your answer must start DIRECTLY with the scene text. Do NOT repeat the story so far. " +
                "Do NOT anticipate the continuations.\n" +
                "3. Character speech goes between single quotes ' ', never between \".\n" +
                "4. Never use the | character in the narrative text.\n" +
                "5. NEVER generate game mechanics tags such as <ADD_ITEM> or <STAT_MOD>.\n" +
                "6. The player character is {player_gender}: use the correct grammatical agreement.",
            syntheticEndingConstraintText = "Follow these instructions EXACTLY:\n" +
                "1. Write the final scene in {user_language}. Match the '{genre}' genre and " +
                "above all this tone: {tone_hints} — the ending must sound like the rest of " +
                "THIS adventure, not like a generic one.\n" +
                "2. Your answer must start DIRECTLY with the scene text.\n" +
                "3. Character speech goes between single quotes ' ', never between \".\n" +
                "4. Never use the | character in the narrative text.\n" +
                "5. NEVER generate game mechanics tags such as <ADD_ITEM> or <STAT_MOD>.\n" +
                "6. The player character is {player_gender}: use the correct grammatical agreement.",
            outputFormatText = "After the scene text, write the separator: --- TAGS ---\n" +
                "Below the separator, one line per choice, exactly in this format:\n" +
                "CHOICE|scene_id|progressive|translated text\n" +
                "DISCIPLINE|discipline_id|translated text",
            // Aggiunta solo quando la scena ha un combattimento
            // (REGOLE.md §1.5: enemyName tradotto nel giro normale).
            enemyFormatText = "ENEMY|translated enemy name",
            // Aggiunta solo quando la scena non ha già uno sfondo
            // dichiarato dal pacchetto (PromptBuilder). OPTIONAL e
            // parsimonioso di proposito: un tag sbagliato non rompe nulla
            // (vocabolario chiuso, il parser scarta ciò che non riconosce),
            // ma un tag forzato su ogni scena sarebbe rumore, non aiuto.
            imageFormatText = "IMAGE|location_id — OPTIONAL. Below is a dictionary of available " +
                "locations, each with a short description of what it depicts. If one of them is " +
                "a strong, obvious match for THIS scene, add this line with its exact id. If none " +
                "fits well, omit the line entirely — do not guess.\n" +
                "{available_locations}",
            closingText = "NARRATOR (in {user_language}, tone: {tone_hints}):",
        )

        private val json = Json { ignoreUnknownKeys = true }

        // Legge i parametri del tag start_adventure_prompt. Qualunque
        // problema (JSON rotto, tag assente, parametro mancante) degrada
        // sul default corrispondente, senza eccezioni.
        fun fromConfig(configJson: String): PromptFragments {
            val params = runCatching {
                json.parseToJsonElement(configJson)
                    .jsonObject["tags"]!!.jsonArray
                    .map { it.jsonObject }
                    .first { it["id"]?.jsonPrimitive?.content == "start_adventure_prompt" }["parameters"]!!
                    .jsonArray
                    .associate { p ->
                        val obj = p.jsonObject
                        obj["name"]!!.jsonPrimitive.content to obj["value"]!!.jsonPrimitive.content
                    }
            }.getOrNull() ?: return DEFAULTS

            fun value(name: String, default: String) = params[name]?.takeIf { it.isNotBlank() } ?: default

            return PromptFragments(
                baseText = value("baseText", DEFAULTS.baseText),
                previousSceneText = value("previousSceneText", DEFAULTS.previousSceneText),
                sceneText = value("sceneText", DEFAULTS.sceneText),
                syntheticEndingText = value("syntheticEndingText", DEFAULTS.syntheticEndingText),
                continuationsText = value("continuationsText", DEFAULTS.continuationsText),
                choicesText = value("choicesText", DEFAULTS.choicesText),
                disciplineEmphasisText = value("disciplineEmphasisText", DEFAULTS.disciplineEmphasisText),
                constraintText = value("constraintText", DEFAULTS.constraintText),
                syntheticEndingConstraintText = value(
                    "syntheticEndingConstraintText",
                    DEFAULTS.syntheticEndingConstraintText,
                ),
                outputFormatText = value("outputFormatText", DEFAULTS.outputFormatText),
                enemyFormatText = value("enemyFormatText", DEFAULTS.enemyFormatText),
                imageFormatText = value("imageFormatText", DEFAULTS.imageFormatText),
                closingText = value("closingText", DEFAULTS.closingText),
            )
        }
    }
}
