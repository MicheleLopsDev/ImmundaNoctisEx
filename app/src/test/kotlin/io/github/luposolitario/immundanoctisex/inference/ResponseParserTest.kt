package io.github.luposolitario.immundanoctisex.inference

import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.DisciplineChoice
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Il parser è la rete di sicurezza dell'inferenza: qui si verifica
// soprattutto che NON blocchi mai il gioco (output monco, righe
// malformate, modello che ignora il formato -> si degrada sull'originale
// del pacchetto).
class ResponseParserTest {

    private fun scene(
        choices: List<Choice> = emptyList(),
        disciplineChoices: List<DisciplineChoice> = emptyList(),
        combat: Combat? = null,
        backgroundImage: String? = null,
    ) = Scene(
        id = "3",
        sceneType = SceneType.TRANSITION,
        genre = "FANTASY",
        narrativeText = "The alley narrows as the old quarter swallows the daylight.",
        choices = choices,
        disciplineChoices = disciplineChoices,
        combat = combat,
        backgroundImage = backgroundImage,
    )

    private fun choice(id: String, next: String, text: String) =
        Choice(id = id, choiceText = text, nextSceneId = next)

    @Test
    fun estraeNarrativaEScelteTradotte() {
        val scene = scene(
            choices = listOf(
                choice("c1", "4", "Walk on, hand on your weapon"),
                choice("c2", "5", "Slip into the shadows"),
            ),
        )
        val raw = """
            Il vicolo si stringe mentre il quartiere vecchio inghiotte la luce.
            --- TAGS ---
            CHOICE|4|1|Avanza, la mano sull'arma
            CHOICE|5|2|Scivola tra le ombre
        """.trimIndent()

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Il vicolo si stringe mentre il quartiere vecchio inghiotte la luce.", result.narrative)
        assertEquals("Avanza, la mano sull'arma", result.choiceTexts["c1"])
        assertEquals("Scivola tra le ombre", result.choiceTexts["c2"])
    }

    @Test
    fun ilTestoEUltimoCampo_unPipeNelTestoNonRompeIlParsing() {
        val scene = scene(choices = listOf(choice("c1", "4", "Original")))
        val raw = "Prosa.\n--- TAGS ---\nCHOICE|4|1|Avanza | poi svolta a destra"

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Avanza | poi svolta a destra", result.choiceTexts["c1"])
    }

    @Test
    fun sceltaMancante_tieneIlTestoOriginaleDelPacchetto() {
        val scene = scene(
            choices = listOf(
                choice("c1", "4", "Walk on, hand on your weapon"),
                choice("c2", "5", "Slip into the shadows"),
            ),
        )
        // Il modello traduce solo la prima riga.
        val raw = "Prosa.\n--- TAGS ---\nCHOICE|4|1|Avanza, la mano sull'arma"

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Avanza, la mano sull'arma", result.choiceTexts["c1"])
        assertEquals("Slip into the shadows", result.choiceTexts["c2"])
    }

    @Test
    fun sceneIdSbagliato_ripiegaSulConteggio() {
        val scene = scene(
            choices = listOf(
                choice("c1", "4", "First"),
                choice("c2", "5", "Second"),
            ),
        )
        // Il modello inventa le destinazioni ma l'ORDINE è giusto.
        val raw = """
            Prosa.
            --- TAGS ---
            CHOICE|99|1|Prima tradotta
            CHOICE|98|2|Seconda tradotta
        """.trimIndent()

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Prima tradotta", result.choiceTexts["c1"])
        assertEquals("Seconda tradotta", result.choiceTexts["c2"])
    }

    @Test
    fun dueScelteVersoLaStessaScena_nonRicevonoLaStessaRiga() {
        val scene = scene(
            choices = listOf(
                choice("c1", "5", "First"),
                choice("c2", "5", "Second"),
            ),
        )
        val raw = """
            Prosa.
            --- TAGS ---
            CHOICE|5|1|Prima tradotta
            CHOICE|5|2|Seconda tradotta
        """.trimIndent()

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Prima tradotta", result.choiceTexts["c1"])
        assertEquals("Seconda tradotta", result.choiceTexts["c2"])
    }

    @Test
    fun senzaSeparatore_tuttoENarrativaEITestiRestanoOriginali() {
        val scene = scene(choices = listOf(choice("c1", "4", "Original text")))
        val raw = "Il modello ha ignorato il formato e ha scritto solo prosa."

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Il modello ha ignorato il formato e ha scritto solo prosa.", result.narrative)
        assertEquals("Original text", result.choiceTexts["c1"])
    }

    @Test
    fun rispostaVuota_degradaSulTestoOriginaleDellaScena() {
        val scene = scene(choices = listOf(choice("c1", "4", "Original text")))

        val result = ResponseParser.parse("   ", scene)

        assertEquals(scene.narrativeText, result.narrative)
        assertEquals("Original text", result.choiceTexts["c1"])
    }

    @Test
    fun righeMalformateIgnorateSenzaEccezioni() {
        val scene = scene(choices = listOf(choice("c1", "4", "Original")))
        val raw = """
            Prosa.
            --- TAGS ---
            CHOICE|4
            CHOICE|
            DISCIPLINE|
            rumore a caso
            CHOICE|4|1|
        """.trimIndent()

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Original", result.choiceTexts["c1"])
    }

    @Test
    fun disciplineTradotteEMancantiInFallback() {
        val scene = scene(
            disciplineChoices = listOf(
                DisciplineChoice("d1", "SIXTH_SENSE", "You sense the ambush", "5"),
                DisciplineChoice("d2", "CAMOUFLAGE", "You blend with the shadows", "5"),
            ),
        )
        val raw = "Prosa.\n--- TAGS ---\nDISCIPLINE|SIXTH_SENSE|Percepisci l'imboscata"

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Percepisci l'imboscata", result.disciplineChoiceTexts["d1"])
        assertEquals("You blend with the shadows", result.disciplineChoiceTexts["d2"])
    }

    @Test
    fun nomeNemicoTradottoConFallbackSulPacchetto() {
        val combat = Combat(
            enemyName = "Warehouse Thugs",
            enemyCombatSkill = 16,
            enemyEndurance = 24,
            winSceneId = "6",
        )
        val withEnemy = scene(combat = combat)

        val tradotto = ResponseParser.parse("Prosa.\n--- TAGS ---\nENEMY|Sgherri del magazzino", withEnemy)
        assertEquals("Sgherri del magazzino", tradotto.enemyName)

        val senzaRiga = ResponseParser.parse("Prosa.\n--- TAGS ---", withEnemy)
        assertEquals("Warehouse Thugs", senzaRiga.enemyName)

        // Scena senza combattimento: nessun nemico da nominare.
        assertNull(ResponseParser.parse("Prosa.", scene()).enemyName)
    }

    @Test
    fun narrativeOf_mostraSoloCioCheStaPrimaDelSeparatore() {
        val streaming = "Prima parte della prosa\n--- TAGS ---\nCHOICE|4|1|non mostrare"

        assertEquals("Prima parte della prosa", ResponseParser.narrativeOf(streaming))
    }

    // Streaming token per token (Michele 22/07/2026: "devo vedere per
    // forza --TAGS?"): finché il separatore non è scritto per intero, un
    // suo pezzo iniziale in coda al testo non deve comparire a schermo.
    @Test
    fun narrativeOf_nascondeUnSeparatoreAncoraAMeta() {
        assertEquals("Prima parte della prosa", ResponseParser.narrativeOf("Prima parte della prosa\n--"))
        assertEquals("Prima parte della prosa", ResponseParser.narrativeOf("Prima parte della prosa\n--- TAG"))
        assertEquals("Prima parte della prosa", ResponseParser.narrativeOf("Prima parte della prosa\n--- TAGS -"))
    }

    // --- Sfondo di scena (vocabolario chiuso, esperimento 20/07/2026) ---

    @Test
    fun immagineScelta_seValidaEDentroIlCatalogo() {
        val tradotto = ResponseParser.parse("Prosa.\n--- TAGS ---\nIMAGE|loc_tavern", scene())
        assertEquals("loc_tavern", tradotto.backgroundImage)
    }

    @Test
    fun immagineInventata_scartataInSilenzio() {
        // Un nome che NON esiste nel catalogo: vocabolario chiuso, il
        // parser non deve mai far arrivare alla UI qualcosa che non
        // corrisponde a una risorsa vera.
        val tradotto = ResponseParser.parse("Prosa.\n--- TAGS ---\nIMAGE|loc_wizard_tower_that_does_not_exist", scene())
        assertNull(tradotto.backgroundImage)
    }

    @Test
    fun sfondoGiaDichiaratoDalPacchetto_battelSempreQuelloDiGemma() {
        // L'autore ha gia' deciso: Gemma e' un ripiego, mai una
        // sovrascrittura di una scelta gia' fatta.
        val conSfondo = scene(backgroundImage = "loc_market")
        val tradotto = ResponseParser.parse("Prosa.\n--- TAGS ---\nIMAGE|loc_crypt", conSfondo)
        assertEquals("loc_market", tradotto.backgroundImage)
    }

    // BUG del 20/07/2026, trovato da Michele giocando: qui vinceva
    // scene.backgroundImage per la sola presenza (!= null). Il sample
    // dichiara backgroundImage su TUTTE le scene con placeholder storici
    // mai risolti ("inn", "city"...): quel valore bloccava per sempre
    // anche il tag di Gemma, coerente col bug gemello in PromptBuilder
    // (che per lo stesso motivo non chiedeva mai la riga IMAGE).
    @Test
    fun sfondoDichiaratoMaFuoriCatalogo_vinceIlTagDiGemmaSeValido() {
        val placeholder = scene(backgroundImage = "inn")
        val tradotto = ResponseParser.parse("Prosa.\n--- TAGS ---\nIMAGE|loc_tavern", placeholder)
        assertEquals("loc_tavern", tradotto.backgroundImage)
    }

    @Test
    fun sfondoDichiaratoMaFuoriCatalogo_senzaTagRestaIlPlaceholderOriginale() {
        // Né l'autore né Gemma hanno un nome che risolve a un file vero:
        // si tiene comunque "inn" invece di perderlo con un null secco —
        // la UI degrada comunque sul default, ma l'intento dell'autore
        // (una locanda, anche se l'asset manca) non sparisce dal dato.
        val placeholder = scene(backgroundImage = "inn")
        val tradotto = ResponseParser.parse("Prosa.\n--- TAGS ---", placeholder)
        assertEquals("inn", tradotto.backgroundImage)
    }

    @Test
    fun nessunaRigaImage_backgroundImageRestaNull() {
        assertNull(ResponseParser.parse("Prosa.\n--- TAGS ---", scene()).backgroundImage)
    }

    // --- Fixture da output reali di Gemma (log del device, 22/07/2026) ---
    // Non testo scritto a mano: blocchi tag copiati dai log "IMAGE risolto=
    // ... — blocco tag" mandati da Michele, sui due motori provati quella
    // sera (Gemma 4 E4B abliterated e Gemma 4 E2B abliterated). Scene e ID
    // ricalcano content/scenes.sample.json (il libro campione usato in
    // quei run), non inventati.

    @Test
    fun fixtureE4B_bloccoTagBenFormato_conUnRefusoGrammaticaleNelTesto() {
        // sample-adventure scena "3": choice_3_1 -> "4", dchoice_3_1
        // SIXTH_SENSE -> "5", dchoice_3_2 CAMOUFLAGE -> "5".
        val scene = scene(
            choices = listOf(choice("choice_3_1", "4", "Walk on, hand on your weapon")),
            disciplineChoices = listOf(
                DisciplineChoice("dchoice_3_1", "SIXTH_SENSE", "You sense the ambush before they see you...", "5"),
                DisciplineChoice("dchoice_3_2", "CAMOUFLAGE", "You blend with the shadows...", "5"),
            ),
        )
        // "sul tuo'arma" è un refuso grammaticale vero del modello (doveva
        // essere "sulla tua arma"): il parser non valida la grammatica,
        // solo la struttura — deve passare comunque.
        val raw = "Prosa.\n--- TAGS ---\n" +
            "CHOICE|4|1|Procedi, mano sul tuo'arma\n" +
            "DISCIPLINE|SIXTH_SENSE|Senti l'imboscata prima che lo vedano loro...\n" +
            "DISCIPLINE|CAMOUFLAGE|Ti confondi con le ombre...\n" +
            "IMAGE|loc_warehouse"

        val result = ResponseParser.parse(raw, scene)

        assertEquals("Procedi, mano sul tuo'arma", result.choiceTexts["choice_3_1"])
        assertEquals("Senti l'imboscata prima che lo vedano loro...", result.disciplineChoiceTexts["dchoice_3_1"])
        assertEquals("Ti confondi con le ombre...", result.disciplineChoiceTexts["dchoice_3_2"])
        assertEquals("loc_warehouse", result.backgroundImage)
    }

    @Test
    fun fixtureE4B_allucinazioneSullaScenaFinale_scartataDelTutto() {
        // sample-adventure scena "6" (ENDING, VICTORY): nessuna scelta e
        // nessuna disciplina vera. Il modello ha comunque scritto i
        // SEGNAPOSTO del formato alla lettera ("scene_id", "discipline_id"
        // come stringhe letterali, non ID veri) e mischiato inglese e
        // italiano nella stessa riga — nessun crash, tutto scartato perché
        // la scena reale non ha scelte con cui far combaciare nulla.
        val scene = scene(choices = emptyList(), disciplineChoices = emptyList())
        val raw = "Prosa.\n--- TAGS ---\n" +
            "CHOICE|scene_id|progressive|The player can now open the chest.\n" +
            "DISCIPLINE|discipline_id|Esplorare il cofano|Examine the chest"

        val result = ResponseParser.parse(raw, scene)

        assertEquals(emptyMap(), result.choiceTexts)
        assertEquals(emptyMap(), result.disciplineChoiceTexts)
    }

    @Test
    fun fixtureE2B_immagineSenzaPrefissoLoc_scartataDalCatalogoChiuso() {
        // sample-adventure scena "4" (combattimento, senza scelte proprie):
        // il modello più piccolo (2B) ha scritto "warehouse" invece di
        // "loc_warehouse" — il vocabolario è chiuso per uguaglianza esatta,
        // non per somiglianza: niente immagine invece di una scelta quasi
        // giusta ma sbagliata.
        val scene = scene(choices = emptyList(), disciplineChoices = emptyList())
        val raw = "Prosa.\n--- TAGS ---\n" +
            "CHOICE|scene_01|progressive|Chiudi la porta e cerca un riparo\n" +
            "DISCIPLINE|discipline_basic|Migliora le tue abilità di combattimento\n" +
            "ENEMY|Thug_1\n" +
            "IMAGE|warehouse"

        val result = ResponseParser.parse(raw, scene)

        assertNull(result.backgroundImage)
        assertEquals("Thug_1", result.enemyName)
    }

    @Test
    fun fixtureE2B_treScelteInventateDiSanaPianta_scartateDelTutto() {
        // Stessa scena finale del fixture E4B sopra, ma il 2B è andato
        // oltre: ha inventato TRE scelte fittizie con ID a caso
        // (scene_001/002/003) e le ha ripetute pari pari come DISCIPLINE.
        // Nessuna delle due liste reali della scena ne contiene una vera.
        val scene = scene(choices = emptyList(), disciplineChoices = emptyList())
        val raw = "Prosa.\n--- TAGS ---\n" +
            "CHOICE|scene_001|progressive|Apri la cassaforte\n" +
            "CHOICE|scene_002|progressive|Osserva il marchio sulla cassaforte\n" +
            "CHOICE|scene_003|progressive|Ricorda la promessa del testo\n" +
            "DISCIPLINE|scene_001|Apri la cassaforte\n" +
            "DISCIPLINE|scene_002|Osserva il marchio sulla cassaforte\n" +
            "DISCIPLINE|scene_003|Ricorda la promessa del testo"

        val result = ResponseParser.parse(raw, scene)

        assertEquals(emptyMap(), result.choiceTexts)
        assertEquals(emptyMap(), result.disciplineChoiceTexts)
    }

    @Test
    fun fixtureE2B_caratteriGiapponesiNellaProsa_nonRomponoIlParsing() {
        // Il 2B ha mescolato caratteri giapponesi dentro una parola
        // italiana ("strarつきate") nel testo VISIBILE — un problema di
        // qualità del modello, non del parser: la narrativa passa
        // comunque, senza eccezioni né filtri sui caratteri.
        val raw = "Lo acciaio strascia liberando due brutti che emergono dalle ombre del magazzino, " +
            "le lame già strarつきate, i loro sguardi piatti di violenza addestrata.\n" +
            "--- TAGS ---\n" +
            "CHOICE|4|1|Procedi, mano sulla tua arma"

        val result = ResponseParser.parse(raw, scene())

        assertEquals(
            "Lo acciaio strascia liberando due brutti che emergono dalle ombre del magazzino, " +
                "le lame già strarつきate, i loro sguardi piatti di violenza addestrata.",
            result.narrative,
        )
    }
}
