# Analisi flusso del prompt e codice morto — v1 (ImmundaNoctis-master)

Sessione del 15/07/2026. Fonte: zip dei sorgenti Kotlin (56 file, 11.704 righe).

## Riferimento confermato

`buildGemmaPromptForScene` (MainViewModel, hardcoded) è **il prompt di
riferimento**: testato con Gemma 3, funzionava decisamente bene. Il frammento
`secretPrompt` visto in sessione precedente era un altro pezzo di codice
(composizione config-driven), complementare e non in conflitto. Per Ex il
prompt di riferimento va rivisto secondo le nuove decisioni di design, e i
suoi testi migreranno nei frammenti dichiarativi del config.

## Il flusso del prompt in v1 (4 tappe)

### Tappa 1 — Caricamento config
- `StringTagParser` (util/StringTagParser.kt, 121 righe) carica `assets/config.json`
  nel costruttore via Jackson → `TagsConfigWrapper` / `TagConfig`.
- In questa versione il config è usato SOLO per il parsing dei tag in risposta,
  NON per comporre il prompt.

### Tappa 2 — Composizione prompt
- `MainViewModel.buildGemmaPromptForScene(scene, lastMessageText)` (riga 1296):
  prompt in italiano, hardcoded, con queste sezioni:
  - Istruzioni numerate: traduci e armonizza col tono (`savePreferences.narrativeTone`),
    inizia direttamente con la traduzione, non ripetere il contesto precedente
  - Formato output: narrazione + separatore `--- TAGS ---` + tag
    `<choice_it scene=".." progressivo="..">` e `<discipline_it id="..">`
  - Guardia anti-allucinazione: "NON GENERARE MAI tag di meccaniche
    (`<ADD_ITEM>`, `<STAT_MOD>`)"
  - Dati: [METADATI SCENA] (sceneType + challengeLevel),
    [CONTESTO DELL'AZIONE PRECEDENTE] (ultimo messaggio in chat),
    [TESTO NARRATIVO DA TRADURRE] (narrativeText.english),
    [SCELTE DA TRADURRE] (choices + disciplineChoices formattate)

**Mappa diretta sul design Ex**: il prompt hardcoded di v1 è l'antenato esatto
dei frammenti pianificati — CONTESTO PRECEDENTE → `previousSceneText`,
SCELTE → `continuationsText`, istruzioni numerate + guardie → `constraintText`.
Le lezioni imparate (iniziare direttamente con la traduzione, non ripetere il
contesto, vietare i tag meccanica) vanno preservate nei nuovi frammenti.

### Tappa 3 — Invio al motore
- `dmEngine = GemmaEngine` (MediaPipe `LlmInference` + `LlmInferenceSession`),
  streaming via Flow.
- Durante lo streaming la UI riceve i token solo fino al separatore `---`:
  la parte tag non viene mai mostrata al giocatore.
- `playerEngine = LlamaCppEngine` esiste SOLO per la chat libera
  (`isChatEnabled`), feature che in Ex non esiste (interazione a scelte).

### Tappa 4 — Ritorno e parsing
- Risposta raw splittata su `--- TAGS ---`: narrativa → ChatMessage,
  parte tag → `StringTagParser.parseAndReplaceWithCommands()` → `EngineCommand`
  → `processCommands()`.
- Le `gameMechanics` della scena (dal JSON) passano dallo STESSO parser e
  vengono eseguite subito — conferma del design Ex: i tag sono dati per il
  motore, non passano da Gemma.
- 🐛 **BUG trovato**: il blocco di parsing di `tagsPart` è duplicato
  (MainViewModel ~righe 1426-1444) — i comandi delle scelte vengono
  eseguiti due volte.

## Classificazione dei 56 file

### Morto certo (zero riferimenti) — 4 file
| File | Note |
|---|---|
| data/SkillData.kt | Già identificato da Michele. Definisce `Skill` |
| ui/adventure/AdventureDialogs.kt | Catena morta: SkillDialog/SkillCard/RatingStars usano `Skill`, nessuno li chiama (WeaponSkillSelectionDialog sta in AdventureUtils.kt) |
| tools/Merger.kt | Script una tantum, nessun riferimento |
| tools/ValidationScript.kt | Script una tantum, nessun riferimento |

### Legato a feature abbandonate (vivo in v1, NON si porta in Ex) — ~13 file
| Gruppo | File | Motivo |
|---|---|---|
| Stable Diffusion | stdf/* (6 file), StdfGenerationActivity, StdfModelActivity, view/StdfViewModel, util/ImageGenerationPreferences | Generazione immagini dinamica abbandonata → backgroundImage statici |
| Chat libera | engine/LlamaCppEngine, util/LlamaPreferences | In Ex non c'è testo libero; del file si salva solo il token tracking (già annotato in ANALISI-RIUSO-V1) |
| Traduzione ML Kit | engine/TranslationEngine | In Ex la traduzione la fa Gemma nel prompt |
| Doppia lingua | `LocalizedText` in GameData.kt | Sostituita da stringhe singole |

### Vivo e riusabile per Ex (nucleo) 
| File | Verdetto |
|---|---|
| util/StringTagParser.kt | **Riuso quasi integrale**: config-driven, regex → EngineCommand, filtro per actor. In Ex affiancherà/precederà il function calling |
| engine/InferenceEngine.kt | Interfaccia astratta: riuso del pattern |
| engine/GemmaEngine.kt | Pattern load/sendMessage(Flow)/resetSession: si riscrive su LiteRT-LM mantenendo la struttura |
| engine/GameRulesEngine.kt + engine/rules/LoneWolfRules.kt | Regole Lupo Solitario: da esaminare in dettaglio, candidato riuso alto |
| engine/GameLogicManager.kt | Da esaminare |
| util/GameStateManager.kt, SavePreferences.kt | Pattern di persistenza sessione: riuso concettuale |
| data/GameData.kt | Modelli dati: base per lo schema Ex (già usato per la struttura scena) |
| Streaming split su separatore | Lezione di design da portare in Ex |

### Da esaminare nelle prossime sessioni
- GameRulesEngine + LoneWolfRules (combattimento, gradi Kai, Dado del Destino)
- GameLogicManager (cosa gestisce davvero rispetto a MainViewModel)
- MainViewModel per intero (1.634 righe: monolite da smontare — in Ex va
  spezzato in componenti)

## Note per il task 2 (frammenti prompt)
1. Base di partenza: `buildGemmaPromptForScene` (testato con Gemma 3).
   Revisioni necessarie per Ex:
   - lingua di output parametrica `{userLanguage}` (non più "italiano" fisso);
     lingua sorgente dal manifest del pacchetto
   - tono da `toneHints` di scena + default manifest (non più solo
     `savePreferences.narrativeTone`)
   - contesto precedente = narrativeText della scena precedente
     (non l'ultimo messaggio di chat)
   - continuazioni = narrativeText delle possibili scene successive
     (non solo i testi delle scelte)
   - arricchimento della prosa, non generazione da zero
2. I nuovi frammenti devono ereditare le guardie testate sul campo:
   - "inizia direttamente con il testo della scena"
   - "non ripetere il contesto precedente"
   - "non generare mai tag meccanica"
   - separatore esplicito tra narrativa e parte strutturata
3. Il tono in v1 arriva da `savePreferences.narrativeTone` (runtime):
   in Ex si somma ai `toneHints` di scena e al default del manifest
4. Punto di design DECISO in sessione 15/07: si mantiene la chiamata singola
   di v1 (narrativa + scelte + discipline in una inferenza — velocità sul
   Razr), ma con formato di output più robusto, perché in v1 Gemma sbagliava
   i tag XML e il parser si inceppava:
   - formato a righe delimitate al posto dell'XML con attributi:
     `CHOICE|scene|progressivo|testo` e `DISCIPLINE|id|testo`
     (definito nelle regex del config: resta dati, non codice)
   - validazione per conteggio con fallback: il motore sa quante scelte ha
     mandato; per quelle che non tornano dal parsing usa il choiceText
     originale del pacchetto. Il parsing che fallisce non blocca MAI il gioco
   - in prospettiva: function calling / output vincolato di LiteRT-LM
     (da valutare in fase integrazione)
5. `challengeLevel` (BASE/HARD): verificato nel codice — usato in un solo
   punto vivo (una riga di colore nel prompt), nessuna logica di motore.
   NON distingue combat da prova (il combat è segnalato da combatChoices).
   Verdetto per Ex: si elimina; il suo ruolo lo coprono i toneHints.
   Reintrodurre solo se un domani serve difficoltà meccanica reale.

## Test e validatori (nota per la roadmap, richiesta Michele 15/07)

Il punto 2 della roadmap (schema JSON + validazione) si estende:
- **Validatore pacchetti scene** (scenes.*.json): grafo chiuso, campi
  obbligatori, discipline solo tra le 10 canoniche, sceneType coerenti
  (ENDING senza uscite), toneHints presenti
- **Validatore config parser** (config.json): tutte le regex compilano,
  placeholder `{captured_value_from_regex_N}` coerenti coi gruppi della
  regex, tag id univoci, actor validi — in v1 una regex rotta falliva
  silenziosamente in gioco (v. Log.d in StringTagParser)
- **Test**: suite di test per parser e validatori con casi noti
  (tag ben formati, malformati, output Gemma reali salvati come fixture)
Lavoro delegabile a Claude Code una volta chiuso lo schema.
