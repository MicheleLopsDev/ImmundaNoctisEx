# Diario di progetto

## 16/07/2026

### Sessione lampo — inventario asset v1

**Inventario asset v1 completato** (`doc/INVENTARIO-ASSET.md`): scansionato
`app/src/main/res/drawable*` + `app/src/main/assets/` del repo
`ImmundaNoctis-master` al commit `8b705b8` (branch `master`). Numeri
chiave:
- 18 file, 20,47 MB totali (19,84 MB in `res/drawable/`, 0,63 MB in
  `assets/`).
- Nessuna icona di gioco, nessuno sfondo ambiente, nessun `portrait_elara`/
  `lupo_solitario.png`: questo commit è molto più magro del branch
  `develop` fotografato in `ANALISI-RIUSO-V1.md` il 14/07 (43 MB in
  drawable) — quegli asset sono stati aggiunti su `develop` dopo il merge
  in `master` del 30/06/2025. Da tenere presente per un secondo giro di
  inventario su `develop` se servirà.
- Classificazione: 3 file RIUSABILI (ritratti eroe m/f, ritratto DM,
  boilerplate launcher icon), 8 file LEGATI A FEATURE MORTA (ritratti
  classi D&D sage/thief/warrior/witch — in Lupo Solitario non c'è
  selezione di classe, sostituita dalle Discipline Kai), 4 DA RIFARE
  (cleric/mage generici, map_dungeon non pertinente, scenes.json/
  config.json vecchio schema già superati).
- Nessun candidato v1 per i `backgroundImage` richiesti dal sample
  (inn/city/alley/battle/warehouse): da produrre ex novo.
- Stima conversione WebP: ~79% di risparmio sui 14 JPEG (da 19,84 MB a
  ~4,2 MB), in linea con la stima già fatta il 14/07 sul set più ampio.

### Sessione (secondo task)

**Fatto — audit e revisione `content/config.json`:**
- Trovato che il commit `39030c8` (base di partenza fornita per l'audit)
  aveva sovrascritto l'intero file e perso la sezione `start_adventure_prompt`
  scritta e chiusa il 15/07 (commit `2d3f830`); recuperata da lì e reinserita.
- 2 tag duplicati rimossi (`victory_text_translation`, `defeat_text_translation`
  comparivano due volte identici).
- `victory_text_translation`/`defeat_text_translation` eliminati del tutto
  (anche la prima occorrenza): in Ex gli esiti sono scene ENDING con prosa
  propria; l'esito globale è regola motore + `deathSceneId`/`victorySceneId`
  nel manifest, non tag di traduzione.
- `narrative_choice_translation` -> `choice_line`, regex convertita al
  formato pipe (`^CHOICE\|([^|]+)\|([^|]+)\|(.+)$`), parametro rinominato
  `translatedText` (il formato pipe è neutro rispetto alla lingua).
- `discipline_choice_translation` -> `discipline_line`, stesso trattamento
  (`^DISCIPLINE\|([^|]+)\|(.+)$`); risolve anche il bug di case della
  vecchia regex XML (`<DISCIPLINE_IT ... </discipline_it>`).
- `end_guff_tag`: `replacement` da oggetto `{italian, english}` a stringa
  singola `""` (decisione lingua singola).
- 3 tag orfani (comando assente in v1, meccanica voluta) mantenuti e
  marcati `"status": "TO_IMPLEMENT"`: `remove_item_tag` (removeItem),
  `if_item_choice_tag` (checkItemAndJump), `random_item_table_tag`
  (rollOnItemTable).
- Confermati invariati i comandi vivi in v1: `add_item_tag`,
  `require_action_tag`, `remove_all_tag`, `heal_tag`, `set_flag_tag`,
  `random_quantity_tag`, `stat_mod_tag`, `if_stat_tag`,
  `random_choice_table_tag`, `skill_check_tag`, `conditional_action_tag`,
  `set_global_var_tag`, `update_global_var_tag`.

**Conferma di design:** i tag `gameMechanic` sono scelte dell'autore del
libro, parsati SOLO dai dati del pacchetto (mai generati da Gemma);
dall'output di Gemma si estraggono solo le righe `CHOICE|`/`DISCIPLINE|`.

**Input per la specifica 2 (regole):** comandi da implementare in Ex:
`removeItem`, `checkItemAndJump`, `rollOnItemTable`, regola globale
Resistenza<=0 -> `deathSceneId`.

**Nota schema manifest:** aggiungere `deathSceneId` e `victorySceneId`
(opzionali) — da riportare in `doc/SCHEMA-PACCHETTO.md` quando si farà.

### Sessione (primo task)

**Fatto:**
- Esame `GameRulesEngine` + `LoneWolfRules` + `GameLogicManager` di v1.
- SPECIFICA 1 CHIUSA: `doc/ARCHITETTURA.md` (moduli data/engine/inference/ui).

**Decisioni:**
- Tiro del dado iniettato nell'engine (interfaccia `DiceRoller`): il Dado
  del Destino UI e i test deterministici usano la stessa porta.
- `GameState` nel modulo engine come unica fonte di verità; il salvataggio
  è una fotografia dello stato (mai stato sparso tra manager come in v1).
- Single-activity + DI leggera (`AppContainer`) al posto dei singleton.
  Verificato sui numeri: il multi-activity di v1 non ha prodotto file
  piccoli (`ModelActivity` 824 righe, `AdventureActivity` 683,
  `CharacterSheetActivity` 569) e ha costretto a introdurre il singleton.
  La leggibilità viene dal package per schermata, non dal numero di
  Activity.
- Guardrail UI: file di navigazione solo routing (~100 righe max), un
  package per schermata senza import incrociati, nessun ViewModel
  condiviso tra schermate.
- Interfacce solo dove esiste più di un'implementazione reale: le
  quattro motivate sono `RulesEngine`, `InferenceEngine`, `DiceRoller`,
  `PackageSource`. Tutto il resto classi concrete.
- Gradi Kai come enum/id nell'engine, nomi localizzati in UI.
- Risultato del round di combattimento = dati puri; il testo lo compone
  chi lo mostra.
- La Tabella dei Risultati di Combattimento di `LoneWolfRules` si riusa
  integralmente.
- `GameLogicManager` di v1 riclassificato: è un repository di scene, in
  Ex diventa `PackageRepository` nel modulo data con `PackageSource`
  iniettato (niente Context).

**Nota aggiuntiva:**
- Verificato nel codice: `victory_text`/`defeat_text` di v1 erano campi di
  `CombatState` (esito del singolo combattimento), non condizioni globali.
  La condizione di esito globale (vittoria/sconfitta dell'avventura da
  qualsiasi scena, su stat/flag/variabili) è un concetto NUOVO di Ex, da
  progettare nella specifica 2 con `deathSceneId`/`victorySceneId` nel
  manifest e regole globali valutate dall'engine a ogni transizione.

**Prossime sessioni (piano specifiche, in ordine):**
- Specifica 2: regole di gioco (combat, Dado del Destino, gradi Kai,
  Resistenza/Combattività, modificatori, esiti globali
  vittoria/sconfitta) — dentro il modulo engine.
- Poi: stato e salvataggio (3), UI (4), ETL (5), criticità (6).

## 14/07/2026

### Sessione serale

**Fatto:**
- Analisi di `LlamaCppEngine.kt` di v1: architettura chat conversazionale,
  non adatta al nuovo modello di narrazione. Si riusa solo il token tracking
  a soglie e l'idea di interfaccia `InferenceEngine`.
- Analisi di `config.json` di v1: sistema `promptDescription` (tag dichiarativi
  con prompt come dati), riuso integrale deciso.
- README allineato alle decisioni (commit `6a702a0`).
- Creata `content/`: `config.json` spostato lì; `scenes.json` (Project Aon)
  reso locale, non versionato via `.gitignore`.
- Creato `content/scenes.sample.json`: riferimento strutturale pubblico dello
  schema E futuro libro incluso nell'APK — da espandere a mini-avventura
  completa (~10-20 scene) con nomenclatura originale prima del rilascio.

**Decisioni** (vedi changelog README §15 per il dettaglio):
Prosa finita mantenuta nei pacchetti al posto dei canovacci; Gemma arricchisce
e raccorda scena precedente/attuale/continuazioni con prompt stateless senza
chat history; riuso integrale del `config.json` di v1 (sistema
`promptDescription`), con i tag D&D da sostituire con le Discipline Kai.

**Fatto (seconda sessione serale):**
- Struttura scena definitiva chiusa in `content/scenes.sample.json`: eliminata
  la doppia lingua (`narrativeText`/`choiceText` ora stringhe inglesi
  semplici), aggiunto manifest in radice (id, version, title, description,
  language, genre, toneHints di libro), `toneHints` per-scena, campo
  `backgroundImage`, campi predisposti per-scelta (`minRoll`, `maxRoll`,
  `requiredItem`, `requiredFlag`) e `gameMechanics` per-scena.
- Discipline ricondotte alle sole 10 canoniche UPPER_SNAKE (WEAPONSKILL,
  CAMOUFLAGE, HUNTING, SIXTH_SENSE, TRACKING, HEALING, MINDSHIELD,
  MINDBLAST, ANIMAL_KINSHIP, MIND_OVER_MATTER); `SHADOWSTEP` rimosso.
- Grafo di esempio riscritto: 7 scene chiuse ("The Warehouse Letter"),
  START -> ... -> due ENDING (vittoria/morte), con un bivio a disciplina
  (SIXTH_SENSE/CAMOUFLAGE) che evita il combattimento e un ramo di
  combattimento (WIN/LOSE) sulla struttura combat già presente nel file.

**Decisioni chiuse (sessione serale):**
- `narrativeText` e `choiceText`: stringhe semplici, lingua singola (inglese).
- `language` nel manifest del pacchetto; `userLanguage` a runtime (da Android)
  passato a Gemma per la lingua di output.
- Campi predisposti (`minRoll`, `maxRoll`, `requiredItem`, `requiredFlag`)
  restano nello schema anche se vuoti.
- `toneHints` obbligatorio per scena, con un default a livello di manifest.
- `backgroundImage`: nuovo campo per asset statici per ambiente (inn/city/
  alley/battle/warehouse), fallback su placeholder; distinto da `sceneType`,
  che resta START/TRANSITION/ENDING.
- ID scena numerici (corrispondenza con le pagine dei libri-game).
- ID disciplina in UPPER_SNAKE, solo le 10 canoniche di `GameData.kt`;
  `SHADOWSTEP` rimosso.
- Grafo sample: 1→2→3→(4|5)→6, 7=morte.

**Altro (sessione serale):**
- Cancellato `content/scenes.json` locale (schema vecchio, disallineato dal
  sample): verrà rigenerato dal task ETL libro 1 con lo schema nuovo. La riga
  in `.gitignore` resta invariata.
- Decisione UI registrata: il libro completo si carica da file
  nell'applicazione (side-load con picker); l'APK include solo
  `scenes.sample.json`. Da dettagliare in fase UI.

**Prossimi task** (sostituiscono i precedenti):
1. [MICHELE] Bozza dell'estensione di `start_adventure_prompt` in
   `config.json`: i nuovi frammenti (`previousSceneText`, `continuationsText`,
   `constraintText`) — testo dei prompt, niente codice
2. Copia di `config.json` da v1 a Ex + sostituzione tag D&D con Discipline Kai
   (delegabile a Claude Code dopo il task 1)
3. Script ottimizzazione immagini v1 (invariato, delegabile quando si vuole)
4. Creare in `content/` una serie di file di test per meccanica (es.
   `test_choices.json`, `test_skillcheck.json`, `test_combat.json`,
   `test_disciplines.json`, `test_mechanics.json`), scene minime 2-3 l'uno,
   sul modello dei `test_*.json` di v1 — ora possibile: la struttura scena
   definitiva è chiusa (`content/scenes.sample.json`), nascono già nel
   formato giusto
5. Analisi codice morto v1: scansione di tutti i file `.kt` del repo v1
   (`ImmundaNoctis-master`), mappa dei riferimenti incrociati (chi importa
   cosa, chi usa quali simboli), output in `doc/ANALISI-CODICE-MORTO.md`
   con tre categorie: morto certo (zero riferimenti), sospetto
   (referenziato solo da codice morto o solo da test), vivo ma legato a
   feature abbandonate (es. generazione immagini dinamica, doppia lingua).
   Nota: `SkillData.kt` già identificato come morto da Michele. Da fare
   nella stessa sessione di delega dell'inventario asset v1.

### Sessione mattutina 15/07

**Fatto:**
- Esame classi v1 completato: flusso del prompt ricostruito in 4 tappe
  (dettagli in `doc/ANALISI-FLUSSO-PROMPT-V1.md`).
- Censimento 56 file `.kt`: 4 morti certi (`SkillData`, `AdventureDialogs`,
  `tools/Merger`, `tools/ValidationScript`), ~13 legati a feature abbandonate
  (`stdf/*`, `LlamaCppEngine`/chat libera, `TranslationEngine`, doppia
  lingua), nucleo riusabile identificato (`StringTagParser` quasi
  integrale, `InferenceEngine`, pattern `GemmaEngine`).
- Bug trovato in v1: parsing di `tagsPart` duplicato in `MainViewModel`
  (~righe 1426-1444), comandi eseguiti due volte.
- TASK 2 CHIUSO: frammenti prompt scritti e inseriti in `config.json`.

**Decisioni:**
- `buildGemmaPromptForScene` di v1 (testato con Gemma 3) è il riferimento;
  rivisto per Ex con: lingue parametriche (`source_language` dal manifest,
  `user_language` da Android), tono da `toneHints` scena + default
  manifest, contesto = `narrativeText` scena precedente, continuazioni =
  `narrativeText` scene successive, arricchimento non generazione.
- Chiamata singola confermata (narrativa + scelte + discipline in una
  inferenza, velocità sul Razr).
- Formato output a righe con pipe (`CHOICE|...|...|testo`) al posto dei tag
  XML: in v1 Gemma sbagliava i tag e il parser si inceppava. Il testo è
  sempre l'ultimo campo: il motore splitta con limit, un pipe nel testo
  non rompe il parsing.
- Fallback per conteggio: se dal parsing tornano meno scelte di quelle
  inviate, per le mancanti si usa il `choiceText` originale del pacchetto.
  Il parsing che fallisce non blocca MAI il gioco.
- Prompt in inglese (l'italiano era comunque testato e funzionante su
  Gemma 3).
- Discorsi dei personaggi tra apici singoli, mai doppi apici.
- `challengeLevel` eliminato dallo schema Ex: verificato nel codice,
  nessuna logica di motore dietro (una sola riga di colore nel prompt);
  il ruolo lo coprono i `toneHints`.
- Output reali di Gemma da salvare come fixture di test durante lo
  sviluppo.

**Prossimi task** (aggiunti ai delegabili):
- Validatore config parser (`config.json`): regex compilano, placeholder
  coerenti coi gruppi, tag id univoci, actor validi — in v1 le regex rotte
  fallivano silenziosamente.
- Suite test per parser e validatori con fixture di output Gemma reali.
- (già a diario) validatore pacchetti scene, inventario asset v1,
  analisi codice morto v1 — quest'ultima ora ha la base in
  `doc/ANALISI-FLUSSO-PROMPT-V1.md`.

**Decisione di metodo:**
Lo sviluppo è l'ULTIMA fase. Prima si definiscono tutte le specifiche e si
analizzano le criticità; solo a specifiche complete si passa al codice.
Principi architetturali vincolanti per Ex:
- separazione netta tra responsabilità di UI e di logica;
- file più piccoli possibile, moduli con responsabilità singola;
- anti-modello dichiarato: il `MainViewModel` di v1 (1.634 righe, fa tutto:
  motori, prompt, parsing, comandi, combat, salvataggi, traduzione). Il bug
  del parsing duplicato è conseguenza diretta di quella dimensione.

**Piano specifiche** (sostituisce i punti di sviluppo della roadmap, che
slittano a valle; la specifica narrazione è GIÀ CHIUSA con struttura scena
+ frammenti prompt + formato output + fallback):
1. Architettura moduli (`doc/ARCHITETTURA.md`) — strati: data (modelli +
   caricamento pacchetti, zero Android), engine (regole, stato, combat —
   zero Android e zero UI, testabile da terminale), inference (dietro
   interfaccia `InferenceEngine`: prompt builder, parser, fallback), ui
   (Compose, solo presentazione), ViewModel piccoli, uno per schermata,
   soli punti di raccordo. Definire i contratti tra moduli.
2. Specifica regole di gioco — combat, Dado del Destino, gradi Kai,
   Resistenza/Combattività, modificatori. Base: esame di
   `GameRulesEngine` + `LoneWolfRules` + `GameLogicManager` di v1.
3. Specifica stato e salvataggio — contenuto sessione, quando si salva,
   formato. Base: `GameStateManager`/`SessionData` di v1.
4. Specifica UI — schermate, scena teatrale con `backgroundImage`,
   pulsanti scelta, semaforo token. Solo il cosa, non il come.
5. Specifica ETL — conversione libro -> formato pacchetto.
6. Analisi criticità (trasversale) — prestazioni inferenza su Razr 70
   Ultra, limiti contesto Gemma, memoria, tempi caricamento modello,
   assenza modello, degradazioni.

**Prossima sessione di design:**
Architettura moduli (punto 1), partendo dall'esame di `GameRulesEngine`,
`LoneWolfRules` e `GameLogicManager` di v1 per mappare come v1 mischiava le
responsabilità. Output atteso: `doc/ARCHITETTURA.md`.