# Architettura dei moduli — ImmundaNoctisEx

Specifica n. 1 del piano (sessione 16/07/2026). Definisce moduli,
responsabilità e contratti. Le altre specifiche (regole, stato, UI, ETL,
criticità) vivono dentro i confini tracciati qui.

## Principi vincolanti

1. Separazione netta tra responsabilità di UI e di logica.
2. File più piccoli possibile; una classe = una responsabilità.
   Soglia indicativa di allarme: ~200 righe.
3. Nessun singleton con stato globale mutabile.
4. La logica di gioco è testabile da terminale, senza emulatore.
5. Lo stato di gioco ha UNA sola fonte di verità.

## Lezioni dall'esame di v1

**Da tenere:**
- Il pattern `GameRulesEngine` (interfaccia) + `LoneWolfRules`
  (implementazione) era giusto: piccolo, puro, senza Android.
- La Tabella dei Risultati di Combattimento in LoneWolfRules (rapporto
  forza -10..+10, danni per tiro, colpo mortale) è il dato più prezioso
  dopo il config: si riusa integralmente.
- Il bisogno che il singleton GameLogicManager risolveva (condividere
  dati tra più Activity senza ricaricarli) era legittimo: cambia la
  soluzione, non il bisogno.

**Da non ripetere:**
- MainViewModel da 1.634 righe che fa tutto (motori, prompt, parsing,
  comandi, combat, salvataggi): il bug del parsing duplicato è figlio
  di quella dimensione.
- Il multi-activity NON ha prodotto file piccoli: ModelActivity 824
  righe, AdventureActivity 683, CharacterSheetActivity 569,
  SetupActivity 451. La leggibilità di v1 sta invece nei componenti di
  ui/adventure/ (PlayerActionBar 154, ChoiceComponents 136...): è lo
  spacchettamento in package per schermata che rende leggibile il
  codice, non il numero di Activity. In più il multi-activity ha
  costretto a introdurre il singleton per condividere lo stato.
- GameLogicManager: nome ingannevole (è un repository di scene, non
  logica), `object` singleton con cache globale mutabile, dipendenza da
  Context nel layer dati.
- Stato duplicato: le scene usate vivevano sia in GameLogicManager
  (`usedScenesInSession`) sia in SessionData (`usedScenes`). Due fonti
  di verità = bug di salvataggio garantiti.
- `Random.nextInt` chiamato dentro `resolveCombatRound`: test
  deterministici impossibili.
- Testo UI dentro la logica: `getKaiRank` ritorna stringhe italiane
  hardcoded; il log di combattimento è LocalizedText composto
  nell'engine.

## Mappa dei moduli

Per ora package sotto un unico modulo Gradle; spezzabili in moduli
Gradle veri se servirà.

### `data` — modelli e pacchetti
- Modelli del pacchetto: Manifest, Scene, Choice, DisciplineChoice,
  CombatChoice, GameItem, ...
- `PackageRepository`: carica e valida un pacchetto libro, espone
  `getSceneById`, la scena di start, i metadati del manifest.
- Il file arriva da un'interfaccia `PackageSource` (asset APK, file
  side-load): il repository non conosce Context.
- REGOLA: nessuna logica di gioco, nessun import Android nei modelli.

### `engine` — stato e regole
- `GameState`: LA fonte di verità (eroe, statistiche, inventario, flag,
  scena corrente, scene usate, modificatori attivi). Tutte le mutazioni
  passano da qui.
- `RulesEngine` (interfaccia) + `LoneWolfRules` (implementazione con la
  Tabella dei Risultati): risoluzione round, uso discipline, gradi Kai.
- `CombatManager`: orchestrazione del combattimento (round, evasione,
  WIN/LOSE) sopra RulesEngine.
- I gradi Kai sono un enum/id: il nome localizzato lo mappa la UI.
- Il risultato di un round è dati puri (danni, tiro, rapporto): il testo
  lo compone chi lo mostra.
- **Il tiro entra dall'esterno**: `DiceRoller` è un'interfaccia iniettata.
  In produzione la implementa il Dado del Destino (la UI anima, l'utente
  ferma, il valore entra nell'engine); nei test è una sequenza fissa.
- REGOLA: zero import Android, zero riferimenti a UI o inference.
  Il modulo si testa da terminale.

### `inference` — narrazione IA
- `InferenceEngine` (interfaccia, erede di quella di v1): load,
  sendMessage come Flow, reset, token tracking.
- Implementazione LiteRT-LM per Gemma (pattern di GemmaEngine v1).
- `PromptBuilder`: compone i frammenti di promptDescription dal config
  (baseText, previousSceneText, sceneText, continuationsText,
  choicesText, constraintText, outputFormatText, closingText) riempiendo
  i placeholder a runtime.
- `ResponseParser`: split sul separatore, parsing righe pipe
  (CHOICE|...|...|testo con split a limite), validazione per conteggio,
  fallback sul testo originale del pacchetto. Un parsing fallito non
  blocca MAI il gioco.
- `TagParser` (erede di StringTagParser): tag meccanica config-driven,
  regex -> EngineCommand.
- REGOLA: il resto dell'app non sa che esiste Gemma. Chiede
  "arricchisci questa scena per questa lingua/tono", riceve testo e
  scelte tradotte (o i fallback).

### `ui` — presentazione
- Una sola Activity: un file da ~30 righe (setContent + NavHost), puro
  idraulico. Guardrail per non ricreare il marasma:
  1. il file di navigazione fa SOLO routing (elenca le destinazioni);
     sopra ~100 righe è un allarme
  2. un package per schermata (ui/adventure/, ui/setup/, ui/settings/...),
     ognuno con il suo Screen + ViewModel + componenti; nessuna schermata
     importa dal package di un'altra
  3. nessun ViewModel condiviso tra schermate: ciò che due schermate
     devono vedere entrambe vive nell'engine/repository via AppContainer
- Un ViewModel piccolo per schermata, solo orchestrazione: chiama
  engine/inference, espone StateFlow alla UI. MAI logica di regole.
- La UI è stupida: mostra stato, inoltra input.
- Qui vivono: scena teatrale con backgroundImage (fallback placeholder),
  pulsanti scelta, Dado del Destino, semaforo token, localizzazione dei
  testi di interfaccia (strings.xml).

### Regola sulle interfacce (igiene di leggibilità)
Interfacce SOLO dove esiste più di un'implementazione reale. Le quattro
motivate: RulesEngine (regolamenti diversi), InferenceEngine (motori
diversi), DiceRoller (dado UI / sequenza di test), PackageSource (asset
APK / file side-load). Tutto il resto: classi concrete, lettura diretta.
Convenzione: implementazione nello stesso package dell'interfaccia,
nome parlante (RulesEngine -> LoneWolfRules).

## Gestione delle istanze (il posto del vecchio singleton)

- Single-activity: sparisce il problema di condividere dati tra Activity.
- Dependency injection leggera (manuale, un piccolo AppContainer creato
  in Application): PackageRepository, GameSession (che possiede
  GameState), InferenceEngine sono istanze uniche a scope applicazione,
  create una volta e passate a chi le usa.
- Stessi benefici del singleton (un caricamento, una cache), ma
  costruttori espliciti, dipendenze visibili, tutto sostituibile nei test.

## Flusso di una scena (contratto tra moduli)

1. UI: il giocatore preme una scelta -> ViewModel
2. ViewModel -> engine: applica la scelta (GameState avanza, meccaniche
   della scena eseguite via TagParser -> EngineCommand)
3. ViewModel -> data: PackageRepository fornisce la scena successiva e
   le sue continuazioni
4. ViewModel -> inference: PromptBuilder compone, InferenceEngine
   genera, ResponseParser estrae narrativa + scelte (o fallback)
5. ViewModel espone il nuovo stato -> UI renderizza (testo in streaming
   fino al separatore, poi pulsanti)

Nessun modulo salta i livelli: la UI non tocca mai inference o data
direttamente.

## Criteri di verifica della specifica

- [ ] `data` ed `engine` compilano senza dipendenze Android
- [ ] Un test JVM esegue un combattimento completo con DiceRoller fisso
- [ ] Un test JVM carica un pacchetto da InputStream e naviga il grafo
- [ ] ResponseParser testato su fixture di output Gemma reali
      (ben formati e malformati)
- [ ] Nessun file sopra ~200 righe senza giustificazione scritta
