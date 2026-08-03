package io.github.luposolitario.immundanoctisex.core.engine.inference

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
//
// UNICA responsabilità del prompt in QUESTA classe (31/07/2026, Michele:
// "separa bene le responsabilità del prompt in un singolo file kt in modo
// che se un domani ci debba mettere mano so che devo guardare solo una
// classe"): prima i frammenti di testo vivevano in una classe a parte
// (`PromptFragments`) CON un livello di override letto a runtime da
// `content/config.json`. Rimosso lo stesso giorno, insieme al file:
// analisi del codice (non solo intuizione) ha confermato che quel livello
// non è mai stato la fonte reale delle modifiche — le tarature di prompt
// fatte finora sono sempre state dirette sul Kotlin (vedi le date nei
// commenti sotto), e `config.json` era una copia identica dei default,
// mantenuta sincronizzata a mano senza che nulla lo imponesse. È anche
// un'unica configurazione GLOBALE (non varia per libro): non vale la
// duplicazione con relativo rischio di disallineamento. Se in futuro
// serve affinare il prompt senza toccare Kotlin, si può reintrodurre un
// override esterno da qui — ma non prima che serva davvero.
class PromptBuilder(
    // Se Gemma può scegliere lo sfondo quando il pacchetto non ne ha uno
    // valido (InferencePreferences.askImageInPrompt, Opzioni avanzate
    // Modelli LLM) — SPENTO di default dal 26/07/2026 (prompt più
    // semplice), ora una preferenza dell'utente invece di un ramo di
    // codice tolto: utile per confrontare modelli diversi senza dover
    // toccare il codice ogni volta.
    private val askImageInPrompt: Boolean = false,
    // Modalità Traduzione (31/07/2026, Michele: "non è tradci parola per
    // parola ma traduci con i minori cambiamenti possibili mantenendo il
    // senso"): Gemma non arricchisce più la scena, si limita a tradurla
    // restando il più vicino possibile al testo sorgente. Sostituisce
    // BASE_TEXT/CONSTRAINT_TEXT/CLOSING_TEXT con le varianti dedicate e
    // salta l'enfasi sulle discipline (anch'essa un invito ad arricchire);
    // il finale fabbricato (isSyntheticEnding) resta SEMPRE creativo in
    // entrambe le modalità, non ha testo sorgente da cui restare vicini.
    private val translationMode: Boolean = false,
) {

    fun build(context: PromptContext): String {
        val sections = buildList {
            add(if (translationMode) BASE_TEXT_TRANSLATION else BASE_TEXT)
            // Le sezioni vuote NON si scrivono: un "[THE STORY SO FAR]"
            // seguito dal nulla confonde il modello e spreca contesto.
            if (!context.previousSceneText.isNullOrBlank()) add(PREVIOUS_SCENE_TEXT)
            // Un finale fabbricato non ha testo da arricchire: si chiede al
            // narratore di SCRIVERLO. Senza questo, il prompt conterrebbe
            // una scena vuota e il modello inventerebbe a caso.
            if (context.isSyntheticEnding) {
                add(SYNTHETIC_ENDING_TEXT)
            } else {
                add(SCENE_TEXT)
            }
            // Le continuazioni sono il testo SORGENTE, non tradotto, delle
            // scene raggiungibili: servono al narratore per non
            // contraddire il seguito MENTRE INVENTA. Traducendo non si
            // inventa nulla, quindi sono peso morto — e non poco: sono
            // il 16% del prompt (misurato il 03/08/2026 su una scena
            // vera, 2527 caratteri contro 2136).
            //
            // La scena PRECEDENTE invece resta anche in traduzione, ed è
            // una differenza voluta: quello è testo GIÀ TRADOTTO, che il
            // giocatore ha appena letto. Mostra al modello come ha reso
            // prima i nomi propri e i termini ricorrenti — toglierla
            // rischierebbe "Foresta di Fryelund" in una scena e "Bosco
            // di Fryelund" in quella dopo.
            if (context.continuations.isNotEmpty() && !translationMode) add(CONTINUATIONS_TEXT)
            if (context.choices.isNotEmpty() || context.disciplineChoices.isNotEmpty()) {
                add(CHOICES_TEXT)
            }
            // L'enfasi sul soprannaturale si spende solo quando c'è
            // davvero una disciplina in gioco: contesto sprecato altrimenti.
            // Saltata anche in modalità traduzione: è un invito a dare
            // "peso"/"ultraterreno" alla scena, cioè arricchimento.
            if (context.disciplineChoices.isNotEmpty() && !translationMode) add(DISCIPLINE_EMPHASIS_TEXT)
            add(
                when {
                    context.isSyntheticEnding -> SYNTHETIC_ENDING_CONSTRAINT_TEXT
                    translationMode -> CONSTRAINT_TEXT_TRANSLATION
                    else -> CONSTRAINT_TEXT
                },
            )
            add(outputFormat(context))
            add(if (translationMode) CLOSING_TEXT_TRANSLATION else CLOSING_TEXT)
        }
        return fill(sections.joinToString("\n\n"), context)
    }

    // La riga ENEMY si chiede solo se c'è davvero un nemico da nominare.
    // La riga IMAGE si chiede solo se askImageInPrompt è attivo (Opzioni
    // avanzate Modelli LLM) E il pacchetto non ha già uno sfondo valido —
    // l'autore ha sempre priorità, Gemma è il ripiego.
    private fun outputFormat(context: PromptContext): String = buildString {
        append(OUTPUT_FORMAT_TEXT)
        if (context.scene.combat != null) append("\n$ENEMY_FORMAT_TEXT")
        if (askImageInPrompt && !SceneImageCatalog.isValid(context.scene.backgroundImage)) {
            append("\n$IMAGE_FORMAT_TEXT")
        }
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
            .replace("{available_locations}", SceneImageCatalog.PROMPT_DICTIONARY)
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

    // Frammenti di testo del prompt — QUI e SOLO qui (vedi commento sopra
    // la classe). In inglese anche per output italiano: scelta provata su
    // Gemma 3.
    private companion object {
        val BASE_TEXT = "You are the narrator of an interactive gamebook. " +
            "Your task is to enrich and translate a scene for the player."

        // Modalità Traduzione: niente "enrich", il compito è restare
        // fedele al testo sorgente.
        val BASE_TEXT_TRANSLATION = "You are the translator of an interactive gamebook. " +
            "Your task is to translate a scene for the player, staying as close as possible " +
            "to the source text."

        val PREVIOUS_SCENE_TEXT = "[THE STORY SO FAR]\n{previous_scene_text}"

        val SCENE_TEXT = "[CURRENT SCENE — source text in {source_language}]\n{scene_narrative_text}"

        // Il libro non ha una scena di finale: la scrive il narratore.
        // Poche righe e definitive — questa schermata chiude la partita.
        val SYNTHETIC_ENDING_TEXT = "[FINAL SCENE — the source book has no ending for this outcome]\n" +
            "Write a short, final scene in which the hero's journey ends here in defeat. " +
            "It must read as an ENDING — closed, with weight — but leave one faint thread " +
            "open: a hint that this might not be the last word. Do not promise a sequel and " +
            "do not offer the player a choice. " +
            "Base it only on THE STORY SO FAR: do not invent new characters or places. " +
            "Three or four sentences, no more."

        val CONTINUATIONS_TEXT = "[POSSIBLE CONTINUATIONS — for consistency only, do NOT reveal them]\n" +
            "{continuations_text}"

        val CHOICES_TEXT = "[CHOICES TO TRANSLATE]\n{choices_text}"

        // Le Discipline Kai non sono abilità comuni: le possiedono SOLO i
        // Kai (richiesta Michele 20/07/2026). Si aggiunge quando la scena
        // mette in gioco una disciplina, per non sprecare contesto quando
        // non serve.
        val DISCIPLINE_EMPHASIS_TEXT = "[KAI DISCIPLINES]\n" +
            "The hero is a Kai. Kai Disciplines are NOT ordinary skills: they are " +
            "preternatural gifts that only the Kai possess. When the scene shows one being " +
            "used, give it weight — the sense of something beyond common ability, uncanny to " +
            "anyone watching. Keep this to the telling: do NOT invent effects, powers or " +
            "outcomes beyond what the source text states."

        // Regola 7 (27/07/2026, Michele: Gemma 4B a volte inventa parole
        // inesistenti, es. "bruffi" — vincolo esplicito invece che
        // sperare nel solo abbassamento della temperatura).
        val CONSTRAINT_TEXT = "Follow these instructions EXACTLY:\n" +
            "1. Rewrite the CURRENT SCENE text in {user_language}, enriching it with details " +
            "consistent with the '{genre}' genre and this tone: {tone_hints}. Keep all facts, " +
            "characters, items and events of the source text unchanged. Do NOT invent new events, " +
            "items or characters.\n" +
            "2. Your answer must start DIRECTLY with the scene text. Do NOT repeat the story so far. " +
            "Do NOT anticipate the continuations.\n" +
            "3. Character speech goes between single quotes ' ', never between \".\n" +
            "4. Never use the | character in the narrative text.\n" +
            "5. NEVER generate game mechanics tags such as <ADD_ITEM> or <STAT_MOD>.\n" +
            "6. The player character is {player_gender}: use the correct grammatical agreement.\n" +
            "7. Use only real words that exist in {user_language}. Do NOT invent, distort or " +
            "make up words that do not exist in that language."

        // Modalità Traduzione (31/07/2026): stesse regole 2-7 di
        // CONSTRAINT_TEXT (sono vincoli di formato/meccanica, non di
        // stile), cambia solo la regola 1 — non "riscrivi arricchendo" ma
        // "traduci col minimo di modifiche". Niente genere/tono qui: non
        // ha senso "tradurre col tono cupo", la modalità li ignora per
        // costruzione.
        val CONSTRAINT_TEXT_TRANSLATION = "Follow these instructions EXACTLY:\n" +
            "1. Translate the CURRENT SCENE text into {user_language}, staying as close as " +
            "possible to the original wording and sentence structure. Make the MINIMUM changes " +
            "needed for a natural, grammatically correct translation — do NOT translate word " +
            "for word if that would sound unnatural, but do NOT enrich, elaborate, add " +
            "descriptive details or rephrase freely either. Do NOT omit any information present " +
            "in the source text. Keep all facts, characters, items and events of the source text " +
            "unchanged. Do NOT invent new events, items or characters.\n" +
            "2. Your answer must start DIRECTLY with the scene text. Do NOT repeat the story so far. " +
            "Do NOT anticipate the continuations.\n" +
            "3. Character speech goes between single quotes ' ', never between \".\n" +
            "4. Never use the | character in the narrative text.\n" +
            "5. NEVER generate game mechanics tags such as <ADD_ITEM> or <STAT_MOD>.\n" +
            "6. The player character is {player_gender}: use the correct grammatical agreement.\n" +
            "7. Use only real words that exist in {user_language}. Do NOT invent, distort or " +
            "make up words that do not exist in that language."

        // Il vincolo normale ordina di RISCRIVERE la scena sorgente: per un
        // finale fabbricato non esiste scena sorgente, e chiederglielo lo
        // metterebbe in contraddizione. Stesse regole di forma, compito
        // diverso.
        val SYNTHETIC_ENDING_CONSTRAINT_TEXT = "Follow these instructions EXACTLY:\n" +
            "1. Write the final scene in {user_language}. Match the '{genre}' genre and " +
            "above all this tone: {tone_hints} — the ending must sound like the rest of " +
            "THIS adventure, not like a generic one.\n" +
            "2. Your answer must start DIRECTLY with the scene text.\n" +
            "3. Character speech goes between single quotes ' ', never between \".\n" +
            "4. Never use the | character in the narrative text.\n" +
            "5. NEVER generate game mechanics tags such as <ADD_ITEM> or <STAT_MOD>.\n" +
            "6. The player character is {player_gender}: use the correct grammatical agreement."

        val OUTPUT_FORMAT_TEXT = "After the scene text, write the separator: --- TAGS ---\n" +
            "Below the separator, one line per choice, exactly in this format:\n" +
            "CHOICE|scene_id|progressive|translated text\n" +
            "DISCIPLINE|discipline_id|translated text"

        // Aggiunta solo quando la scena ha un combattimento (REGOLE.md
        // §1.5: enemyName tradotto nel giro normale).
        val ENEMY_FORMAT_TEXT = "ENEMY|translated enemy name"

        // Aggiunta solo quando la scena non ha già uno sfondo VALIDO
        // dichiarato dal pacchetto (outputFormat sopra). OPTIONAL e
        // parsimonioso di proposito: un tag sbagliato non rompe nulla
        // (vocabolario chiuso, il parser scarta ciò che non riconosce),
        // ma un tag forzato su ogni scena sarebbe rumore, non aiuto.
        // Vincolo STRINGENTE (21/07/2026, richiesta Michele): il parser
        // già scarta un id inventato in silenzio, ma un'istruzione
        // debole spreca comunque la "scelta" di Gemma su qualcosa che
        // verrà buttato via — meglio dirle chiaro che il dizionario è
        // chiuso e non deve inventare, prima ancora che scriva la riga.
        //
        // ESEMPIO CONCRETO (21/07/2026, primo test sul device: Gemma
        // ignorava del tutto la riga IMAGE, blocco tag con la sola
        // CHOICE — vedi DIARIO.md). CHOICE/DISCIPLINE arrivano al
        // modello già in formato dimostrato (le scelte da tradurre
        // sono nella stessa forma richiesta in output); IMAGE era
        // solo descritta a parole. L'esempio usa un id vero del
        // dizionario ma dice esplicitamente di non copiarlo: mostra
        // la SINTASSI, non suggerisce la scelta.
        //
        // "OPTIONAL" TOLTO (21/07/2026, stesso giorno, prova di
        // Michele su LM Studio): con "OPTIONAL" all'inizio Gemma
        // saltava la riga; riformulata a mano in modo imperativo
        // ("decidi ORA") ha funzionato.
        //
        // SEMPRE UNA RIGA, MAI "OMETTI" (21/07/2026, idea di Michele
        // — "così evitiamo che sbagli, per lui è più facile prendere
        // sempre una decisione, se poi troviamo xxx lo ignoriamo"):
        // "decidi se scrivere la riga" è un giudizio in più (quanto
        // sono sicuro?) sopra quello vero (quale location?). Tolto:
        // la riga si scrive SEMPRE, e quando nessuna location calza
        // si scrive un id-spazzatura (`xxx`) invece di inventarne uno
        // plausibile. Non serve toccare il parser: `SceneImageCatalog
        // .isValid` scarta già in silenzio qualunque id fuori dal
        // catalogo, `xxx` compreso — stesso esito finale
        // dell'omissione, compito più semplice per il modello.
        val IMAGE_FORMAT_TEXT = "IMAGE|location_id — ALWAYS write this line, choosing from the " +
            "CLOSED dictionary of location ids below (each with a short description of what " +
            "it depicts). Decide now: does one of them strongly match THIS scene? If yes, " +
            "write its id EXACTLY as written below — do not modify, abbreviate, translate or " +
            "combine it. You MUST NOT invent a new id that is not in this dictionary, even if " +
            "you think it would fit better. If none of them is a good match, write IMAGE|xxx " +
            "instead of guessing. " +
            "Example of the exact line to write (illustrating the syntax only — pick " +
            "whichever id from the list below actually matches THIS scene, not necessarily " +
            "this one): IMAGE|loc_tavern.\n" +
            "{available_locations}"

        val CLOSING_TEXT = "NARRATOR (in {user_language}, tone: {tone_hints}):"

        // Niente tono in modalità traduzione: coerente con CONSTRAINT_TEXT_TRANSLATION sopra.
        val CLOSING_TEXT_TRANSLATION = "NARRATOR (in {user_language}):"
    }
}
