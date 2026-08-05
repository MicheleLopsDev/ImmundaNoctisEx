package io.github.luposolitario.immundanoctisex.tool.etl

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.ComparisonOperator
import io.github.luposolitario.immundanoctisex.core.data.model.Discipline
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineDescriptor
import io.github.luposolitario.immundanoctisex.core.data.model.EndingOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.GameMechanic
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.RollCondition
import io.github.luposolitario.immundanoctisex.core.data.model.RollConditionType
import io.github.luposolitario.immundanoctisex.core.data.model.RollModifier
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File

// Conversione deterministica delle XHTML "Internet Edition" di Project Aon
// (es. lo zip https://www.projectaon.org/en/xhtml/lw/01fftd/01fftd.zip, il
// file titolo.htm dentro) nel nostro Manifest/Scene — vedi doc/SCHEMA-JSON.md
// per lo schema di destinazione. Nessuna IA: struttura e regole del gioco
// sono già scritte in prosa fissa e ripetuta in tutto il libro (stessa
// frase per ogni combattimento, stessa frase per ogni scelta di
// disciplina...), si riconoscono con pattern deterministici (29/07/2026,
// mappatura concordata con Michele dopo un'ispezione di 01fftd.htm — vedi
// doc/DIARIO.md). Uso PERSONALE (Michele, stessa sessione: "non voglio
// distribuire queste parti"), coerente con la licenza Project Aon già
// documentata in doc/ETL.md.
//
// Quello che il testo NON marca in modo riconoscibile (tipicamente
// ritrovamenti di oggetti/oro narrati in prosa libera) non viene indovinato:
// finisce in `notes`, non in un GameMechanic inventato — coerente con
// ItemMechanics/Params.kt (com/core/engine): meglio un comando assente che
// uno sbagliato in silenzio.
object ProjectAonHtmlParser {

    // Una scena che nel libro cartaceo originale aveva un'illustrazione
    // (30/07/2026, Michele: vuole un elenco per generarne di nuove,
    // indipendenti, con l'IA — le Internet Edition segnano SOLO dove
    // c'era un disegno, "[Illustration N]", senza dire cosa raffigurasse).
    data class IllustrationMarker(val sceneId: String, val label: String)

    data class ParseResult(
        val manifest: Manifest,
        val notes: List<String>,
        val illustrations: List<IllustrationMarker>,
    )

    fun parse(
        file: File,
        id: String,
        title: String,
        description: String,
        genre: String,
        disciplineDescriptions: List<DisciplineDescriptor>,
    ): ParseResult {
        val doc = Jsoup.parse(file, "UTF-8")
        // Alcuni libri (es. "Shadow on the Sand") hanno PIÙ di un
        // div.numbered — "Part I"/"Part II" separati, ciascuno con le sue
        // sezioni — bug trovato il 29/07/2026: prendendo solo il primo, le
        // sezioni della Parte II sparivano in silenzio (referenziate da
        // choice.nextSceneId ma mai generate, errore di validazione a
        // valle). Vanno processati TUTTI.
        val numberedDivs = doc.select("div.numbered")
        if (numberedDivs.isEmpty()) {
            error(
                "div.numbered non trovato in ${file.name}: non sembra una Internet Edition Project Aon " +
                    "(atteso il file titolo.htm dentro lo zip del libro)",
            )
        }

        val notes = mutableListOf<String>()
        val illustrations = mutableListOf<IllustrationMarker>()
        val scenes = numberedDivs.flatMap { splitIntoRawScenes(it) }.flatMap { toScenes(it, notes, illustrations) }

        val manifest = Manifest(
            id = id,
            version = "1.0.0",
            title = title,
            description = description,
            language = "en",
            genre = genre,
            disciplineChoices = disciplineDescriptions,
            scenes = scenes,
        )
        return ParseResult(manifest, notes, illustrations)
    }

    // --- Divisione in scene grezze --------------------------------------

    private data class RawScene(val id: String, val elements: List<Element>)

    // Ogni sezione numerata è <h3><a id="sectN">N</a></h3> seguito da
    // paragrafi fino al prossimo <h3> di sezione — non ci sono contenitori
    // per singola scena nell'HTML originale, solo una sequenza piatta.
    private fun splitIntoRawScenes(numberedDiv: Element): List<RawScene> {
        val scenes = mutableListOf<RawScene>()
        var currentId: String? = null
        var currentElements = mutableListOf<Element>()
        for (child in numberedDiv.children()) {
            val sectId = sectionIdOf(child)
            if (sectId != null) {
                currentId?.let { scenes += RawScene(it, currentElements) }
                currentId = sectId
                currentElements = mutableListOf()
            } else {
                currentElements += child
            }
        }
        currentId?.let { scenes += RawScene(it, currentElements) }
        return scenes
    }

    private fun sectionIdOf(element: Element): String? {
        if (element.tagName() != "h3") return null
        val rawId = element.selectFirst("a[id]")?.id() ?: return null
        if (!rawId.startsWith("sect")) return null
        return rawId.removePrefix("sect").takeIf { it.toIntOrNull() != null }
    }

    // --- Riconoscimento pattern (stessa prosa fissa in tutto il libro) --

    // "Discipline of " seguito subito da uno dei 10 nomi canonici veri
    // (29/07/2026, bug trovato: un capture-fino-alla-punteggiatura prendeva
    // anche il resto della frase, es. "Healing on this man" invece di solo
    // "Healing" — controllare contro i nomi VERI invece di indovinare dove
    // finisce il nome è più robusto).
    // "Disciplines" al plurale esiste davvero ("the Kai Disciplines of
    // either Hunting or Camouflage", 05sots scena 282): senza la `s?`
    // quella scena finiva nel report dei casi non riconosciuti.
    private val disciplineOfRegex = Regex("""(?:Kai )?Disciplines? of """, RegexOption.IGNORE_CASE)
    // "win"/"kill"/"defeat"/"slay": varianti reali trovate nel libro per lo
    // stesso concetto (vittoria in combattimento) — es. "If you win the
    // fight" ma anche "If you kill all three of them", "If you kill the
    // creature".
    private val winRegex = Regex("""\bif you (?:win|kill|defeat|slay)\b""", RegexOption.IGNORE_CASE)
    private val evadeRegex = Regex("""\bevade\b""", RegexOption.IGNORE_CASE)

    // "If you lose ANY ENDURANCE points during this combat ... turn
    // immediately to 66": uscita immediata per chi subisce danno, negli
    // scontri in cui il libro premia solo chi ne esce illeso. Va provata
    // PRIMA di `evadeRegex`, perché la frase contiene quasi sempre anche
    // "even when attempting to evade" e finirebbe scambiata per una fuga.
    private val colpitoRegex = Regex(
        """\blose\s+any\s+ENDURANCE\b""",
        RegexOption.IGNORE_CASE,
    )

    // La vittoria RAPIDA (05/08/2026). Due famiglie di frasi, entrambe
    // copiate dai 5 libri veri:
    //
    //   "If you win the combat in seven rounds or less"     (03tcok 200)
    //   "If you win the combat within four rounds"          (03tcok 208)
    //   "If you win and the fight lasts for 3 rounds ... or less" (04tcod 56)
    //   "If you win and the fight lasts 4 rounds ... or less"     (05sots 91)
    //
    // I rami opposti — "in MORE than seven rounds", "lasts LONGER than
    // 3 rounds" — non devono matchare: quelli sono la vittoria normale.
    // Da qui i due lookahead negativi.
    private val vittoriaRapidaRegex = Regex(
        """\b(?:in|within|lasts(?:\s+for)?)\s+(?!more\b)(?!longer\b)(\w+)\s+rounds?\b""",
        RegexOption.IGNORE_CASE,
    )
    private val evadeRoundsRegex = Regex("""after (\w+) rounds? of combat""", RegexOption.IGNORE_CASE)
    // Tabella dei Numeri Casuali (31/07/2026, Michele: la scena appariva
    // come scelte manuali invece del tiro del dado a bottone singolo —
    // DiceZone/requiresRoll lato client funzionano già, il problema è
    // sempre stato qui). Il testo di Project Aon usa MOLTE forme per la
    // stessa cosa, verificate una per una sui 5 libri convertiti
    // (01/08/2026, controllo esaustivo su tutte le scene che citano la
    // tabella):
    //   "If you have picked a number 0–4"      -> ramo "picked"
    //   "If you have picked 0–1"               -> ramo "picked"
    //   "If the number you have picked is 0–4" -> ramo "number ... is"
    //   "If the number is 5–9"                 -> ramo "number ... is"
    //   "If it is 2–4"                         -> ramo "it is"
    // Il secondo giro di correzioni è servito perché la versione del
    // 31/07, ancorata solo su "number ... is", aveva RIMOSSO il supporto
    // per le prime due (regressione: prendeva 27 scelte su 01fftd ma ne
    // perdeva altre 11).
    private val pickedRangeRegex = Regex(
        """(?:\bpicked\b(?:\s+a\s+number)?\s+|\bnumber\b.{0,25}?\bis\s+|\bit\s+is\s+)(\d+)(?:\s*[-–—−]\s*(\d+))?\b""",
        RegexOption.IGNORE_CASE,
    )

    // "Pick a number... se hai la Disciplina X puoi aggiungere 2... se il
    // TOTALE è 0–3": il numero estratto viene modificato da un bonus
    // CONDIZIONALE (una disciplina, un oggetto) prima del confronto.
    // minRoll/maxRoll del nostro schema si confrontano col tiro GREZZO
    // (ChoiceAvailability.forRoll), quindi convertirle qui sarebbe
    // peggio che non convertirle: il gioco ignorerebbe il bonus e
    // manderebbe il giocatore nella scena sbagliata. Restano scelte
    // manuali finché lo schema non saprà esprimere un modificatore
    // (92 casi sui 5 libri, vedi doc/DIARIO.md 01/08/2026).
    private val totaleModificatoRegex = Regex("""\btotal\b""", RegexOption.IGNORE_CASE)

    // Estratta per essere testabile senza dover passare da un intero file
    // XHTML (il parser vero lavora su Jsoup.parse(File), qui serve solo la
    // logica della regex).
    //
    // `tiroModificato` (01/08/2026): la scena dichiara un
    // Scene.rollModifiers che il motore sa applicare. In quel caso gli
    // intervalli espressi come TOTALE ("if your total is 0–3", "if it is
    // 7–11") diventano corretti da convertire, perché il confronto
    // avverrà sul tiro già modificato — è esattamente il punto della
    // feature. Senza modificatore restano scelte manuali, come prima.
    internal fun rollRangeFor(text: String, tiroModificato: Boolean = false): Pair<Int, Int>? {
        if (!tiroModificato && totaleModificatoRegex.containsMatchIn(text)) return null
        val range = pickedRangeRegex.find(text) ?: totaleRangeRegex.takeIf { tiroModificato }?.find(text) ?: return null
        val min = range.groupValues[1].toIntOrNull() ?: return null
        val max = range.groupValues[2].toIntOrNull() ?: min
        // Il tiro grezzo della Tabella è 0-9: un intervallo che ne esce
        // (es. "7–11") è per forza un totale già modificato da un bonus.
        // Con un modificatore dichiarato è legittimo; senza, no.
        if (!tiroModificato && (min > 9 || max > 9)) return null
        if (min > max) return null
        return min to max
    }

    // Un modificatore ha senso solo dove si tira: senza questa frase la
    // scena parla d'altro (es. "add 2 to your COMBAT SKILL").
    private val marcatoreTabellaRegex = Regex(
        """random number table|pick a number""",
        RegexOption.IGNORE_CASE,
    )

    // "If your total (score) is (now) 0–3" — riconosciuta SOLO quando la
    // scena ha un modificatore estratto (vedi rollRangeFor).
    private val totaleRangeRegex = Regex(
        """\b(?:total|score)\b[^.]{0,20}?\bis\s+(?:now\s+)?(\d+)(?:\s*[-–—−]\s*(\d+))?\b""",
        RegexOption.IGNORE_CASE,
    )
    private val combatLineRegex = Regex("""^(.+?):\s*COMBAT SKILL\s*(\d+)\s*ENDURANCE\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val deductRegex = Regex("""Deduct (\d+) points? from your COMBAT SKILL""", RegexOption.IGNORE_CASE)
    private val exactSectHrefRegex = Regex("""^#sect(\d+)$""")
    // Si fermava a "five" e bastava per l'evasione ("after two rounds").
    // La vittoria rapida arriva più in là — "in seven rounds or less"
    // (03tcok 200) — e una soglia non riconosciuta faceva perdere il
    // ramo in silenzio.
    private val numberWords = mapOf(
        "one" to 1, "first" to 1, "two" to 2, "second" to 2,
        "three" to 3, "third" to 3, "four" to 4, "fourth" to 4,
        "five" to 5, "fifth" to 5, "six" to 6, "sixth" to 6,
        "seven" to 7, "seventh" to 7, "eight" to 8, "eighth" to 8,
        "nine" to 9, "ninth" to 9, "ten" to 10, "tenth" to 10,
    )

    private data class EnemyStats(val name: String, val combatSkill: Int, val endurance: Int)

    // Restituisce PIÙ scene quando l'unica sezione originale non si
    // rappresenta con un solo `combat` (29/07/2026, dopo aver incrociato
    // mechanics-1.xml di Kai Chronicles — solo per capire la struttura,
    // nessun codice/dato loro riusato, vedi doc/DIARIO.md):
    //
    // 1. Più nemici in sequenza nella stessa sezione ("fight them one at a
    //    time"): una catena di scene sintetiche "{id}-nemico2",
    //    "{id}-nemico3"... ciascuna con un solo combat, l'ultima con la
    //    vera destinazione di vittoria — mai un nemico perso o inventato.
    // 2. Vittoria che porta a più uscite invece di una sola (un tiro di
    //    dado, o una scelta libera "adesso decidi tu"): quelle uscite
    //    (già estratte correttamente come Choice/DisciplineChoice) si
    //    spostano in UNA scena sintetica "{id}-vittoria", `combat
    //    .winSceneId` punta lì.
    //
    // In entrambi i casi: nessun testo o numero inventato, solo dati già
    // estratti dalla prosa spostati in nodi di grafo aggiuntivi — e ogni
    // volta che succede, `notes` lo segnala esplicitamente.
    private fun toScenes(raw: RawScene, notes: MutableList<String>, illustrations: MutableList<IllustrationMarker>): List<Scene> {
        val narrative = StringBuilder()
        val choices = mutableListOf<Choice>()
        val disciplineChoices = mutableListOf<DisciplineChoice>()
        val gameMechanics = mutableListOf<GameMechanic>()
        val enemies = mutableListOf<EnemyStats>()
        var winSceneId: String? = null
        var evadeSceneId: String? = null
        var evadeAfterRound = 0
        // Il testo delle righe "se vinci" / "se eviti": serve solo se poi
        // si scopre che in questa scena un combattimento non c'è (vedi
        // `scelteOrfane` più sotto).
        var testoVittoria: String? = null
        // La vittoria rapida e la sua soglia di round, quando il libro
        // ne prevede una.
        var winSceneIdRapido: String? = null
        var winEntroRound: Int? = null
        var seColpitoSceneId: String? = null
        // Tutte le uscite di vittoria e di fuga, con il loro testo: il
        // libro a volte ne offre più d'una fra cui scegliere.
        val vittorie = mutableListOf<Pair<String, String>>()
        val evasioni = mutableListOf<Pair<String, String>>()
        var testoEvasione: String? = null
        var isDeadend = false
        var choiceCounter = 0

        fun label() = "Scena ${raw.id}"

        // I modificatori del tiro si estraggono PRIMA del loop, dal testo
        // intero della sezione (01/08/2026): la frase che li dichiara
        // ("...add 2 to this number") sta nella prosa, le scelte che ne
        // dipendono arrivano dopo, e non si può dipendere dall'ordine in
        // cui il loop le incontra. Sapere già qui se la scena ha un
        // modificatore è anche ciò che permette a rollRangeFor di
        // accettare gli intervalli espressi come TOTALE ("if your total
        // is 0–3"), che senza modificatore sarebbero sbagliati.
        val testoIntero = raw.elements.joinToString(" ") { it.text() }
        val rollModifiers = estraiRollModifiers(testoIntero, notes, ::label)
        val tiroModificato = rollModifiers.isNotEmpty()

        for (element in raw.elements) {
            if (element.tagName() == "div" && element.hasClass("illustration")) {
                // "[Illustration N]" — nessuna descrizione di cosa
                // raffiguri, solo la conferma che QUESTA scena ne aveva
                // una nel libro cartaceo (30/07/2026, Michele: elenco per
                // generarne di nuove e indipendenti con l'IA).
                illustrations += IllustrationMarker(raw.id, element.text().trim())
                continue
            }
            if (element.tagName() != "p") continue

            val text = element.text().trim()
            if (text.isEmpty()) continue

            if (element.hasClass("combat")) {
                val match = combatLineRegex.find(text)
                if (match == null) {
                    notes += "${label()}: paragrafo 'combat' non riconosciuto: \"$text\""
                } else {
                    val cs = match.groupValues[2].toIntOrNull()
                    val end = match.groupValues[3].toIntOrNull()
                    if (cs == null || end == null) {
                        notes += "${label()}: paragrafo 'combat' con statistiche non numeriche: \"$text\""
                    } else {
                        enemies += EnemyStats(match.groupValues[1].trim(), cs, end)
                    }
                }
                continue
            }

            if (element.hasClass("deadend")) {
                isDeadend = true
                narrative.append(text).append("\n\n")
                continue
            }

            // Match ESATTO su "#sectN": alcuni paragrafi narrativi contengono
            // link di nota a piè di pagina tipo "#sect113-1-foot" (bug
            // trovato il 29/07/2026 — con un prefix-match questi venivano
            // scambiati per un "turn to" verso la scena "113-1-foot").
            val sceneLinks = element.select("a[href]").mapNotNull { a ->
                exactSectHrefRegex.find(a.attr("href"))?.groupValues?.get(1)
            }
            if (sceneLinks.size > 1) {
                notes += "${label()}: paragrafo con più link a scene (${sceneLinks.size}) — preso solo il primo: \"$text\""
            }
            val linkedSceneId = sceneLinks.firstOrNull()

            if (linkedSceneId == null) {
                applyInlineStatModifierIfAny(text, gameMechanics, notes) { label() }
                narrative.append(text).append("\n\n")
                continue
            }

            when {
                // Prima di tutto il resto: la frase contiene sia "lose"
                // sia "evade", e andrebbe a finire nel ramo sbagliato.
                colpitoRegex.containsMatchIn(text) -> seColpitoSceneId = linkedSceneId

                winRegex.containsMatchIn(text) -> {
                    // Due righe "se vinci" nella stessa scena: il libro
                    // distingue la vittoria RAPIDA da quella lenta
                    // ("in seven rounds or less" / "in more than seven
                    // rounds"). Prima la seconda riga sovrascriveva la
                    // prima e il ramo rapido spariva.
                    val entro = vittoriaRapidaRegex.find(text)
                    if (entro != null) {
                        winSceneIdRapido = linkedSceneId
                        val parola = entro.groupValues[1].lowercase()
                        winEntroRound = numberWords[parola] ?: parola.toIntOrNull()
                        if (winEntroRound == null) {
                            notes += "${label()}: soglia di round non riconosciuta in \"$text\""
                            winSceneIdRapido = null
                        }
                    } else {
                        // Vittorie senza condizione di durata: se ce n'è
                        // più d'una il libro sta offrendo una SCELTA dopo
                        // lo scontro — 05sots 357: "decide to search the
                        // sentry's body" (207) oppure "ignore the body"
                        // (224). Vedi `vittorie` sotto.
                        vittorie += linkedSceneId to text
                        winSceneId = linkedSceneId
                        testoVittoria = text
                    }
                }

                evadeRegex.containsMatchIn(text) -> {
                    // Un libro può offrire PIÙ MODI di fuggire — 05sots
                    // 20: "jumping into the sea" (142) oppure
                    // "surrendering" (176). Si tengono tutti: se sono più
                    // d'uno diventano una scelta, vedi `evasioni` sotto.
                    evasioni += linkedSceneId to text
                    evadeSceneId = linkedSceneId
                    testoEvasione = text
                    val roundWord = evadeRoundsRegex.find(text)?.groupValues?.get(1)?.lowercase()
                    evadeAfterRound = roundWord?.let { numberWords[it] ?: it.toIntOrNull() } ?: 0
                }

                disciplineOfRegex.containsMatchIn(text) -> {
                    // "Discipline of either X or Y" (trovato nel libro 3):
                    // il nostro schema non ha un OR di discipline su una
                    // sola scelta, ma permette più DisciplineChoice verso
                    // la STESSA destinazione — una per disciplina, stesso
                    // effetto ("basta averne una delle due").
                    val disciplineIds = findDisciplineMentions(text)
                    choiceCounter++
                    if (disciplineIds.isEmpty()) {
                        notes += "${label()}: disciplina non riconosciuta in \"$text\""
                        choices += Choice(id = "choice_${raw.id}_$choiceCounter", choiceText = text, nextSceneId = linkedSceneId)
                    } else {
                        if (disciplineIds.size > 1) {
                            notes += "${label()}: scelta con più discipline alternative (${disciplineIds.joinToString(" o ")}) — create ${disciplineIds.size} DisciplineChoice separate verso la stessa destinazione"
                        }
                        disciplineIds.forEachIndexed { index, disciplineId ->
                            disciplineChoices += DisciplineChoice(
                                id = "dchoice_${raw.id}_${choiceCounter}_$index",
                                disciplineId = disciplineId,
                                choiceText = text,
                                nextSceneId = linkedSceneId,
                            )
                        }
                    }
                }

                else -> {
                    choiceCounter++
                    val range = rollRangeFor(text, tiroModificato)
                    if (range == null) {
                        choices += Choice(id = "choice_${raw.id}_$choiceCounter", choiceText = text, nextSceneId = linkedSceneId)
                    } else {
                        choices += Choice(
                            id = "choice_${raw.id}_$choiceCounter",
                            choiceText = text,
                            nextSceneId = linkedSceneId,
                            minRoll = range.first,
                            maxRoll = range.second,
                        )
                    }
                }
            }
        }

        val hasLeftoverChoices = choices.isNotEmpty() || disciplineChoices.isNotEmpty()
        val extraScenes = mutableListOf<Scene>()

        // PIÙ USCITE DELLO STESSO TIPO (05/08/2026). Il libro a volte
        // offre due modi di uscire dallo stesso scontro:
        //
        //   "If you win and decide to search the body, turn to 207.
        //    If you win but decide to ignore it, turn to 224."   (357)
        //   "...evade by jumping into the sea, turn to 142.
        //    ...evade by surrendering, turn to 176."             (20)
        //
        // Il nostro `Combat` ha un campo solo per esito, e l'ultima riga
        // letta sovrascriveva la precedente. Invece di allargare lo
        // schema (e il motore, e la UI), si manda l'esito a una scena
        // FABBRICATA che porta le due alternative come scelte normali:
        // stesso meccanismo già usato per le catene di nemici, e per il
        // giocatore è persino più chiaro — prima decide di vincere o
        // fuggire, poi come.
        fun sceneDiScelta(suffisso: String, alternative: List<Pair<String, String>>, testo: String): String {
            val id = "${raw.id}-$suffisso"
            extraScenes += Scene(
                id = id,
                sceneType = SceneType.TRANSITION,
                genre = "FANTASY",
                narrativeText = testo,
                choices = alternative.mapIndexed { indice, (destinazione, etichetta) ->
                    Choice(id = "${id}_c$indice", choiceText = etichetta, nextSceneId = destinazione)
                },
            )
            return id
        }

        val vittoriaFinale = if (vittorie.size > 1) {
            notes += "${label()}: ${vittorie.size} esiti di vittoria fra cui scegliere — creata la scena ${raw.id}-vittoria"
            sceneDiScelta("vittoria", vittorie, "The fight is over. What do you do?")
        } else {
            null
        }
        val evasioneFinale = if (evasioni.size > 1) {
            notes += "${label()}: ${evasioni.size} modi di fuggire — creata la scena ${raw.id}-fuga"
            sceneDiScelta("fuga", evasioni, "You break away from the fight. How?")
        } else {
            null
        }
        if (vittoriaFinale != null) winSceneId = vittoriaFinale
        if (evasioneFinale != null) evadeSceneId = evasioneFinale


        // La vera destinazione dopo l'ULTIMO nemico della catena: quella
        // già trovata in prosa ("if you win/kill...") se c'è, altrimenti —
        // se restano scelte/discipline senza una singola destinazione — una
        // scena sintetica con quelle uscite (nessun testo o numero
        // inventato, solo spostato).
        val finalWinSceneId: String = when {
            enemies.isEmpty() -> winSceneId ?: ""
            winSceneId != null -> winSceneId
            hasLeftoverChoices -> {
                val syntheticId = "${raw.id}-vittoria"
                notes += "${label()}: la vittoria porta a più uscite (scelta o tiro) invece che a una sola destinazione — creata la scena sintetica '$syntheticId' con quelle uscite"
                extraScenes += Scene(
                    id = syntheticId,
                    sceneType = SceneType.TRANSITION,
                    genre = "FANTASY",
                    narrativeText = "You have won the fight.",
                    choices = choices.toList(),
                    disciplineChoices = disciplineChoices.toList(),
                )
                choices.clear()
                disciplineChoices.clear()
                syntheticId
            }
            else -> {
                notes += "${label()}: blocco combat senza una riga \"if you win\" riconosciuta — winSceneId mancante, da correggere a mano"
                ""
            }
        }

        if (enemies.size > 1) {
            notes += "${label()}: ${enemies.size} nemici in sequenza nella stessa sezione " +
                "(${enemies.joinToString(", ") { it.name }}) — creata una catena di scene sintetiche, un nemico alla volta"
        }

        // Catena "{id}" -> "{id}-nemico2" -> "{id}-nemico3" -> ... -> finalWinSceneId.
        // La stessa evasione (evadeSceneId/evadeAfterRound) si applica a
        // ogni anello: nel libro si può fuggire in qualunque momento della
        // sequenza, non solo dal primo nemico.
        fun chainIdFor(enemyIndex: Int) = if (enemyIndex == 0) raw.id else "${raw.id}-nemico${enemyIndex + 1}"

        for (index in 1 until enemies.size) {
            val nextWin = if (index == enemies.lastIndex) finalWinSceneId else chainIdFor(index + 1)
            extraScenes += Scene(
                id = chainIdFor(index),
                sceneType = SceneType.TRANSITION,
                genre = "FANTASY",
                narrativeText = "The next opponent steps forward.",
                combat = Combat(
                    enemyName = enemies[index].name,
                    enemyCombatSkill = enemies[index].combatSkill,
                    enemyEndurance = enemies[index].endurance,
                    evadeAfterRound = evadeAfterRound,
                    winSceneId = nextWin,
                    evadeSceneId = evadeSceneId,
                    // La condizione "esci illeso" vale per l'intero
                    // scontro, non solo per il primo nemico.
                    seColpitoSceneId = seColpitoSceneId,
                ),
            )
        }

        val combat = enemies.firstOrNull()?.let { first ->
            Combat(
                enemyName = first.name,
                enemyCombatSkill = first.combatSkill,
                enemyEndurance = first.endurance,
                evadeAfterRound = evadeAfterRound,
                winSceneId = if (enemies.size > 1) chainIdFor(1) else finalWinSceneId,
                // Solo sul combattimento vero, non sugli anelli
                // intermedi di una catena di nemici: la "vittoria
                // rapida" si giudica sullo scontro intero.
                winSceneIdRapido = if (enemies.size > 1) null else winSceneIdRapido,
                winEntroRound = if (enemies.size > 1) null else winEntroRound,
                evadeSceneId = evadeSceneId,
                // Vale su OGNI anello della catena: nei due scontri coi
                // Kalkoth (03tcok 138 e 263) i nemici sono tre e basta un
                // colpo preso da uno qualunque.
                seColpitoSceneId = seColpitoSceneId,
            )
        }

        // "Combatti OPPURE evita", ma il combattimento è nella scena dopo
        // (03/08/2026). Il libro scrive:
        //
        //   "If you wish to fight, turn to 191.
        //    If you wish to evade combat, ... turn to 234."
        //
        // e i valori del nemico stanno in 191, non qui. `evadeSceneId` e
        // `winSceneId` finiscono allora in un `Combat` che non viene mai
        // costruito (`enemies` è vuoto) e sparivano in silenzio, con la
        // loro scelta: il giocatore si trovava il solo ramo "combatti",
        // senza poter schivare uno scontro che il libro gli concede.
        //
        // Trovato confrontando la nostra conversione col grafo ufficiale
        // dei percorsi che Project Aon pubblica per ogni libro
        // (`/en/svg/lw/01fftd.svgz`, idea di Michele): 4 archi mancanti su
        // 555 in `01fftd`, tutti con questa firma.
        if (combat == null) {
            listOfNotNull(
                evadeSceneId?.let { it to testoEvasione },
                winSceneId?.let { it to testoVittoria },
            ).forEach { (destinazione, testo) ->
                if (choices.none { it.nextSceneId == destinazione }) {
                    choiceCounter++
                    choices += Choice(
                        id = "choice_${raw.id}_$choiceCounter",
                        choiceText = testo ?: "",
                        nextSceneId = destinazione,
                    )
                    notes += "${label()}: \"$testo\" sarebbe l'uscita di un combattimento, ma qui " +
                        "nessun nemico è dichiarato — tenuta come scelta normale verso $destinazione"
                }
            }
        }

        val hasAnyExit = choices.isNotEmpty() || disciplineChoices.isNotEmpty() || combat != null
        val sceneType = when {
            raw.id == "1" -> SceneType.START
            !hasAnyExit -> SceneType.ENDING
            else -> SceneType.TRANSITION
        }
        val outcome = when {
            sceneType != SceneType.ENDING -> null
            isDeadend -> EndingOutcome.DEFEAT
            else -> {
                notes += "${label()}: finale senza scelte ma senza marcatore 'deadend' — esito assunto NEUTRAL, " +
                    "correggi a VICTORY se è la conclusione vittoriosa del libro"
                EndingOutcome.NEUTRAL
            }
        }

        val mainScene = Scene(
            id = raw.id,
            sceneType = sceneType,
            genre = "FANTASY",
            narrativeText = narrative.toString().trim(),
            choices = choices,
            disciplineChoices = disciplineChoices,
            combat = combat,
            gameMechanics = gameMechanics,
            outcome = outcome,
            rollModifiers = rollModifiers,
        )
        return listOf(mainScene) + extraScenes
    }

    // "Deduct N points from your COMBAT SKILL[ unless you have the Kai
    // Discipline of X]" — frase fissa che precede quasi ogni combattimento,
    // in un paragrafo puramente narrativo (nessun link di scena).
    private inline fun applyInlineStatModifierIfAny(
        text: String,
        gameMechanics: MutableList<GameMechanic>,
        notes: MutableList<String>,
        label: () -> String,
    ) {
        val match = deductRegex.find(text) ?: return
        val amount = match.groupValues[1].toIntOrNull() ?: return
        val statMod = GameMechanic(
            command = "applyStatModifier",
            params = buildJsonObject {
                put("statName", "COMBAT_SKILL")
                put("amount", (-amount).toString())
            },
        )
        // "unless you have [the Kai] Discipline of X" — condizione opzionale
        // sulla stessa frase del deduct.
        if (!text.contains("unless", ignoreCase = true)) {
            gameMechanics += statMod
            return
        }
        val disciplineId = findDisciplineMentions(text).firstOrNull()
        if (disciplineId == null) {
            notes += "${label()}: \"unless\" con disciplina non riconosciuta in \"$text\""
            gameMechanics += statMod
            return
        }
        gameMechanics += GameMechanic(
            command = "handleConditionalAction",
            params = buildJsonObject {
                put("condition", "NOT_HAS_DISCIPLINE")
                put("disciplineName", disciplineId)
                put(
                    "action",
                    buildJsonObject {
                        put("command", statMod.command)
                        put("params", statMod.params)
                    },
                )
            },
        )
    }

    // Nomi inglesi canonici più lunghi prima: "mind over matter" deve
    // essere provato prima di eventuali sotto-match più corti (nessuno dei
    // 10 è prefisso di un altro oggi, ma l'ordine resta una garanzia
    // economica contro future aggiunte).
    private val disciplineByEnglishName = Discipline.entries
        .associateBy { it.name.replace('_', ' ').lowercase() }
        .entries.sortedByDescending { it.key.length }

    // Cerca "Discipline of " e cerca i nomi canonici ESATTI dentro la sola
    // clausola che segue (fino alla prossima virgola/punto) — non "cattura
    // fino alla punteggiatura", che prendeva anche il resto della frase
    // quando il libro continua con "to fare qualcosa" prima della virgola
    // (bug trovato il 29/07/2026 su 5 scene di 01fftd.htm). Restituisce PIÙ
    // di un nome per "Discipline of either X or Y" (libro 3: basta averne
    // una delle due) — quasi sempre una lista con un solo elemento.
    // --- Modificatori del tiro (01/08/2026, Scene.rollModifiers) ---
    // "Pick a number... If you have the Kai Discipline of Sixth Sense,
    // you may add 2 to this number. If your total is 0–3, turn to 58."
    // Forme raccolte dai 5 libri veri, 57 casi: Disciplina 37, ENDURANCE
    // 10, Rango Kai 7 (FUORI copertura, vedi sotto), oggetto 1, flag 1,
    // incondizionato 1.

    // La frase che modifica: "add N to (this|the) number", "deduct N
    // from...". Cattura anche la clausola condizionale che la precede.
    private val modificatoreRegex = Regex(
        """([^.]{0,170}?)\b(add|subtract|deduct)\s+(\d+)\s+(?:to|from)\s+(?:this|the|it|that)\b[^.]{0,70}""",
        RegexOption.IGNORE_CASE,
    )
    // "is less than 10", "is greater than 20", ma anche "is above 25" /
    // "is below 6" (senza "than"): forme tutte presenti nei libri.
    // Due forme, ed è la seconda ad averci sorpreso (03/08/2026, dal
    // controllo di TUTTE le scene con un modificatore nel testo):
    //  - "is less than 20"  → comparatore prima, numero dopo;
    //  - "is 20 or more"    → **numero prima**, comparatore dopo.
    // La seconda è inglese comunissimo e sfuggiva del tutto: in
    // `04tcod` scena 343 costava i DUE modificatori della stessa scena
    // ("se ENDURANCE è 20 o più, +3; se è 12 o meno, −2").
    // Gruppi nominati apposta: con l'ordine che cambia fra le due forme,
    // leggerli per posizione sarebbe un invito a sbagliare.
    private val enduranceCondRegex = Regex(
        """endurance\b[^.]{0,40}?\bis\s+(?:""" +
            """(?<op>less|fewer|below|greater|more|above)\s+(?:than\s+)?(?<num>\d+)""" +
            """|(?<num2>\d+)\s+or\s+(?<op2>less|fewer|lower|more|greater|higher)""" +
            """)""",
        RegexOption.IGNORE_CASE,
    )
    private val rangoRegex = Regex("""\bkai rank\b|\brank of\b""", RegexOption.IGNORE_CASE)
    private val possiedeRegex = Regex("""\bpossess\b|\bhave a\b|\bcarry\b""", RegexOption.IGNORE_CASE)

    // Il testo qui è la sezione INTERA (vedi il chiamante): una scena può
    // dichiarare più modificatori ("add 2 se hai X" + "deduct 1 se hai Y").
    // `internal` per lo stesso motivo di `rollRangeFor` sopra: è una
    // regola di lettura del testo inglese, e va verificata sulle frasi
    // vere dei libri senza dover costruire un HTML intero attorno.
    internal fun estraiRollModifiers(
        testoIntero: String,
        notes: MutableList<String> = mutableListOf(),
        label: () -> String = { "prova" },
    ): List<RollModifier> {
        if (!marcatoreTabellaRegex.containsMatchIn(testoIntero)) return emptyList()
        val modificatori = mutableListOf<RollModifier>()

        modificatoreRegex.findAll(testoIntero).forEach { match ->
            val clausola = match.groupValues[1]
            val verbo = match.groupValues[2].lowercase()
            val quantita = match.groupValues[3].toIntOrNull() ?: return@forEach
            val amount = if (verbo == "add") quantita else -quantita

            // Il rango Kai è fuori copertura per decisione esplicita
            // (i titoli dei libri — Guardian, Savant — non esistono nel
            // nostro KaiRank, dichiarato cosmetico): si segnala e si
            // lascia la scena a scelte manuali, non si indovina.
            if (rangoRegex.containsMatchIn(clausola)) {
                notes += "${label()}: modificatore del tiro legato al RANGO Kai, non supportato — " +
                    "la scena resta a scelte manuali (\"${clausola.trim().take(90)}\")"
                return@forEach
            }

            val condizione = when {
                // Nessun "if" nella clausola: si applica sempre.
                !clausola.contains("if", ignoreCase = true) -> null

                disciplineOfRegex.containsMatchIn(clausola) -> {
                    val ids = findDisciplineMentions(clausola)
                    if (ids.isEmpty()) {
                        notes += "${label()}: modificatore con disciplina non riconosciuta (\"${clausola.trim().take(90)}\")"
                        return@forEach
                    }
                    RollCondition(RollConditionType.DISCIPLINE, values = ids)
                }

                enduranceCondRegex.containsMatchIn(clausola) -> {
                    val m = enduranceCondRegex.find(clausola)!!
                    // Una sola delle due forme aggancia: si prende quella
                    // che ha prodotto i gruppi.
                    val soglia = (m.groups["num"] ?: m.groups["num2"])?.value?.toIntOrNull()
                        ?: return@forEach
                    val comparatore = (m.groups["op"] ?: m.groups["op2"])?.value?.lowercase()
                        ?: return@forEach
                    val operatore = when (comparatore) {
                        "less", "fewer", "below", "lower" -> ComparisonOperator.LT
                        else -> ComparisonOperator.GT
                    }
                    RollCondition(RollConditionType.ENDURANCE, operator = operatore, threshold = soglia)
                }

                possiedeRegex.containsMatchIn(clausola) -> {
                    val oggetti = oggettiCitati(clausola)
                    if (oggetti.isEmpty()) {
                        notes += "${label()}: modificatore legato a un oggetto non riconosciuto (\"${clausola.trim().take(90)}\")"
                        return@forEach
                    }
                    RollCondition(RollConditionType.ITEM, values = oggetti)
                }

                else -> {
                    notes += "${label()}: condizione del modificatore non riconosciuta, scena a scelte manuali " +
                        "(\"${clausola.trim().take(90)}\")"
                    return@forEach
                }
            }
            modificatori += RollModifier(amount = amount, condition = condizione)
        }
        return modificatori
    }

    // "If you possess either a Pick or a Shovel": i nomi propri sono in
    // maiuscolo nel testo dei libri, è l'unico appiglio affidabile.
    private val nomeOggettoRegex = Regex("""\b(?:a|an|the)\s+([A-Z][a-zA-Z]+)""")

    private fun oggettiCitati(clausola: String): List<String> =
        nomeOggettoRegex.findAll(clausola).map { it.groupValues[1] }.distinct().toList()

    private fun findDisciplineMentions(text: String): List<String> {
        val match = disciplineOfRegex.find(text) ?: return emptyList()
        val remainder = text.substring(match.range.last + 1)
        val clauseEnd = remainder.indexOfFirst { it == ',' || it == '.' }.let { if (it == -1) remainder.length else it }
        val clause = remainder.substring(0, clauseEnd)
        return disciplineByEnglishName
            .filter { (englishName, _) -> clause.contains(englishName, ignoreCase = true) }
            .map { it.value.name }
            .distinct()
    }
}
