# Diario di progetto

## 17/07/2026

### Sessione — chiusura specifica 4 (UI)

**Specifica 4 CHIUSA** (`doc/UI.md`). Decisioni chiave:

- **Estetica di v1 CONSERVATA** (riferimento: screenshot "L'Ultimo dei
  Kai" — tema scuro, banner con ritratti sovrapposti, card personaggio
  con grado dorato e icone stats). Corretti solo i tre elementi
  dell'era-chatbot: via la barra di testo libero "Cosa fai?" (sostituita
  dalla zona scelte), via le bolle chat (il testo scorre come pagina di
  libro), il DM da personaggio salvato a **presenza visiva pura**
  (cerchio d'oro su chi parla, nessun `Character` nei dati).
- **7 schermate**: Home (continua/nuova/carica libro/modelli/opzioni),
  Setup avventura (scelta difficoltà), Creazione personaggio (lupo/lupa,
  tiro stat col Dado del Destino, 5 discipline, specializzazione
  WEAPONSKILL), Avventura (scena teatrale), Scheda personaggio a due tab
  (Stats e Discipline / Equipaggiamento e Zaino), Diario del viaggio
  (Racconto + Mappa logica, esportazione Markdown), Opzioni (TTS con
  voce per genere da `TtsPreferences` di v1, gestione modelli, lingua).
- **Scena teatrale**: header con titolo/scena/semaforo token sul
  pallino (verde/giallo/rosso, assorbe il contatore 0/10240 di v1);
  banner `backgroundImage` con ritratti circolari sovrapposti
  (narratore + eroe + compagno); flusso centrale come pagina di libro
  (serif, continuo) con fumetto narratore a tre icone (copia,
  originale/tradotto — toggle ex-icona "traduci", TTS) e le decisioni
  del giocatore incorporate nel flusso come vista live del diario-grafo;
  card di stato che apre la Scheda; zona scelte con pulsanti disciplina
  visivamente distinti.
- **Dado del Destino**: overlay modale animato, oggetto di scena
  rituale — appare SOLO per i tiri del giocatore (tiro stat, skillCheck,
  randomChoiceTable, ogni round del combattimento completo, il round di
  danno dell'evasione); i tiri del motore (randomQuantity,
  rollOnItemTable, combattimento rapido) restano in silenzio nel testo.
  **Combattimento dentro la scena**, nessuna schermata separata: la zona
  scelte si trasforma (scelta modalità rapido/completo, barre Resistenza
  e Rapporto di Forza, menu tattico continua/oggetto/disciplina/fuga).
- **Inventario OPERATIVO** nella Scheda: equipaggia/disequipaggia armi
  con effetto immediato sulle stat mostrate, consuma pasti/oggetti con
  effetto dichiarato (stesso gesto per EAT_MEAL, HUNTING auto-esente),
  zaino con gli 8 posti disegnati anche vuoti.
- **`gender`** con tre clienti: ritratto (lupo/lupa), voce TTS per
  genere, placeholder `{player_gender}` nel prompt (accordi grammaticali
  in italiano).

**Code generate**: elencate nella sezione finale di `doc/UI.md` (asset
mancanti — narratore, lupo/lupa, dado animato, icone discipline; nota:
molte icone vivono sul branch `develop` di v1, non su `master`@8b705b8
scansionato nell'inventario asset — da recuperare da lì; nuovo campo
opzionale `locationName` sulla scena, ereditato/appiccicoso, per la
Mappa logica del diario).

**Prossima specifica: 5 (ETL — conversione libri in pacchetti).**

### Aggiunta post-chiusura specifica 4

- Campo opzionale `locationName` sulla scena: **appiccicoso** (eredita
  dalla scena precedente se assente, l'autore lo scrive solo quando il
  luogo cambia).
- Diario del viaggio a due viste: **Racconto** (rilettura voce per
  voce) e **Mappa logica** dei luoghi visitati (nodi col nome,
  collegati nell'ordine del viaggio) — derivata dal diario-grafo
  raggruppando le voci per `locationName`. v0.1: solo il nome del
  luogo; predisposizione per annotare in futuro combattimenti (già
  derivabili dalle Transition WIN/LOSE), NPC importanti e oggetti
  trovati.
- `JourneyEntry` porta con sé il `locationName` già risolto (ereditato)
  al momento della visita: la Mappa logica non dipende dal ricalcolo
  dell'ereditarietà a lettura.

### Sessione — chiusura specifica 5 (ETL)

**Specifica 5 CHIUSA** (`doc/ETL.md`). Decisioni:

- **Scoperta chiave**: Kai Chronicles (GPL v3) ha le meccaniche dei
  libri 1-13 codificate a mano in `mechanics-X.xml` + `objects.xml`
  bilingue — il lavoro che v1 chiedeva all'LLM esiste già; la
  classificazione fuzzy diventa traduzione deterministica tra formati.
- **Tool desktop Kotlin Multiplatform** nello stesso repo: `:core:data`
  e `:core:engine` condivisi (validatori identici tra app e tool,
  simulazione col motore vero), `:tool` Compose Desktop.
- **Pipeline a 6 stadi**: struttura da XML Aon (deterministica) ->
  meccaniche da KC (deterministica) -> LLM solo rifinitore opzionale
  (`locationName`, `toneHints`; chiave dell'utente; il tool funziona
  anche senza) -> validatori condivisi -> simulazione -> report
  human-in-the-loop (tag + frase sorgente).
- **Modello legale**: si distribuisce lo strumento, mai il contenuto;
  downloader integrato nel tool come primo passo del wizard, AVVIATO
  DALL'UTENTE (precedente: Kai Chronicles scarica da Project Aon via
  HTTP) — solo i file dei libri 1-3, una richiesta alla volta, cache
  locale, fallback manuale; niente script esterni per OS; pacchetti
  solo uso personale.
- **Perimetro v0.1**: libro 1 pilota, poi 2-3 (esperienza diretta del
  primo tentativo: oltre il 3 le meccaniche divergono; dal 6 ciclo
  Magnakai — lavoro futuro).
- **Diagnosi del blocco storico di v1**: modello debole × fonte HTML ×
  zero validazione — tutti e tre i fattori ribaltati.

**Prossima e ULTIMA specifica: 6 (analisi criticità)** — poi
`PIANO-SVILUPPO.md` e si scrive codice.

### Sessione — chiusura specifica 6 (criticità) — DESIGN CONCLUSO

**Specifica 6 CHIUSA** (`doc/CRITICITA.md`) — **DESIGN CONCLUSO (6/6)**.
Decisioni:

- **Modello**: Gemma 3 4B via LiteRT-LM (lo stesso di v1, provato);
  contesto di riferimento 10240 token.
- **INFERENZA SENZA MEMORIA**: sessione nuova per ogni scena; contesto
  = frammenti fissi + coda scena precedente + scena + scelte; il
  diario non entra MAI nel prompt. È la taratura del motore di
  inferenza di Ex.
- **Criticità madre**: velocità inferenza su Razr (misure: primo token
  <3s, token/s vs velocità di lettura, termico su sessioni 30-45').
- **Obblighi di piano**: scrittura ATOMICA dei salvataggi
  (temp+rename), streaming Compose bufferizzato ~80-100ms, fixture con
  output Gemma reali.
- **Non-criticità liquidate con misura di conferma**: pacchetto 350
  scene, diario, auto-save, toggle, animazione dado.

**PROSSIMO E ULTIMO PASSO PRIMA DEL CODICE: `doc/PIANO-SVILUPPO.md`.**

### Sessione — piano di sviluppo: DESIGN CONCLUSO (6/6 + piano)

**`doc/PIANO-SVILUPPO.md` generato.** 8 fasi con milestone testabili
(Fondamenta → `:core:data` → `:core:engine` → **MILESTONE REGINA**:
il libro gira per intero senza Gemma → inference → UI funzionale
completa → `:tool` ETL → Abbellimento). Principio "**prima funziona,
poi è bello**": UI Material di default fino alla Fase 6, l'estetica di
`doc/UI.md` è tutta nella Fase 7 dedicata. Task **[MICHELE]** riservati
per fase (enum canonici, tabella CRT, fixture di test, strings.xml,
revisione ETL) — Claude Code non li implementa, al più impalca e
segnala se bloccano.

**Prossima sessione: SVILUPPO, Fase 0** (checklist in fondo al piano).

### Sessione — SVILUPPO: Fase 0 (Fondamenta) CHIUSA

Prima sessione di codice del progetto. 4 commit atomici + questo:

- **Progetto Gradle KMP**: 4 moduli (`:core:data`, `:core:engine` KMP
  puro con target `jvm()`+`androidTarget()`, zero codice Android nel
  common; `:app` Android/Compose; `:tool` placeholder Kotlin/JVM,
  Compose Desktop vero arriva in Fase 6). Versioni Kotlin 2.0.21 / AGP
  8.10.1 / Gradle 8.11.1, le stesse già in uso in v1 su questa
  macchina. Aggiunto `.gitattributes` (LF forzato su `gradlew`, jar
  wrapper come binario — altrimenti `core.autocrlf=true` lo rompe al
  checkout).
- **`kotlinx.serialization`** (1.7.3) aggiunta come dipendenza in
  entrambi i moduli core, non ancora usata (arriva con i modelli in
  Fase 1).
- **Test JVM segnaposto** in `commonTest` di entrambi i moduli core
  (kotlin.test): verificano sia `jvmTest` (puro JVM, zero SDK Android)
  sia `testDebugUnitTest`/`testReleaseUnitTest` (target Android) —
  doppia conferma che "zero dipendenze Android" nel codice comune è
  rispettato meccanicamente, non solo per convenzione.
- **`:app` scheletro**: single-activity (`MainActivity`) + Compose,
  un solo `Text` segnaposto, tema Android di sistema (niente
  `themes.xml`, coerente col principio "prima funziona poi è bello").
  Namespace/applicationId scelti: `io.github.luposolitario.immundanoctisex`
  (stesso prefisso di v1 + suffisso "ex") — da confermare con Michele,
  facile da cambiare ora.
- **Blocco ambiente (risolto)**: alla prima configurazione Gradle non
  risolveva NESSUN plugin (Google/MavenCentral/Gradle Plugin Portal):
  l'handshake TLS cadeva subito. Causa: due CA di intercettazione
  HTTPS installate nello store di Windows (`AVG Web/Mail Shield Root`,
  scansione SSL/TLS dell'antivirus, e `Cato Networks Root CA`) fidate
  da Windows/.NET (PowerShell funzionava) ma non dalla JVM (Gradle e
  curl fallivano). Sessione fermata e segnalata a Michele com da
  regola del piano; sbloccata disattivando la scansione HTTPS di AVG.
  Nota per sessioni future su questa macchina: se Gradle non risolve
  dipendenze con errori di rete "istantanei" (handshake che si
  interrompe in pochi ms), è questo — non un problema del progetto.
- **Milestone verificata**: `./gradlew test` verde su tutti e 4 i
  moduli; APK installato e avviato sull'AVD `Small_Desktop`
  (android-34), activity in foreground, nessun crash in logcat
  (verifica indiretta: `dumpsys window`/`ps`/`logcat`, lo screenshot
  non è disponibile su questo AVD). Non testato sul Razr fisico
  (non disponibile in questa sessione) — da fare alla prima occasione
  con l'hardware reale.

**Prossimo task: Fase 1 — `:core:data`** (modelli, `PackageSource`,
validatori — vedi `doc/ARCHITETTURA.md` e `doc/PIANO-SVILUPPO.md`).
Task [MICHELE] in coda per questa fase: enum `WeaponType` e `KaiRank`
(soglie da `doc/REGOLE.md` §Blocco 3).

### Sessione — SVILUPPO: Fase 1 (`:core:data`) CHIUSA

6 commit atomici (modelli pacchetto, modelli stato/sessione,
`PackageSource`+`PackageRepository`, validatori, aggiornamento sample,
test JVM).

- **Modelli pacchetto**: `Manifest`, `Scene`/`SceneType`, `Choice`,
  `DisciplineChoice`, `DisciplineDescriptor`, `Combat` (REGOLE.md
  §1.5), `GlobalRule`/`GlobalRuleType`/`ComparisonOperator` (REGOLE.md
  Blocco 2, operatori serializzati con `@SerialName` sui simboli
  `==`/`!=`/ecc.), `Discipline` (10 canoniche).
- **Modelli stato/sessione**: `Character`/`CharacterRole`, `GameItem`/
  `ItemType`, `StatModifier`/`StatType`, `Difficulty`, `SessionData`,
  `JourneyEntry` con `Transition` sealed (ChoiceTaken/DisciplineUsed/
  CombatResolved/AutoJump) — fedeli a STATO.md, nessun calcolo di
  bonus qui (si serializzano i fatti, si calcola in Fase 2).
- **`gameMechanics` generico**: `GameMechanic(command, params:
  JsonObject)` invece di una gerarchia tipizzata per i 18 comandi —
  quei tipi sono comportamento (:core:engine, Fase 2), qui serve solo
  a caricare/validare. I validatori che devono leggere i parametri
  (rollOnItemTable) lo fanno leggendo "params" direttamente.
- **`PackageSource` + `PackageRepository`**: quarta interfaccia
  motivata implementata; il repository carica da `InputStream`,
  valida, espone `getSceneById`/`startScene`/manifest; un pacchetto
  rotto (JSON malformato o che fallisce i validatori) non lascia mai
  l'istanza in stato parziale.
- **5 validatori** (uno per file, orchestrati da `PackageValidator`):
  grafo chiuso, discipline canoniche, `combat.winSceneId` obbligatorio,
  intervalli `rollOnItemTable` completi/disgiunti (0-9), warning
  globalRules verso scene non-ENDING. Aggiunti due controlli minimi
  non esplicitamente elencati nel piano ma necessari per "messaggi
  chiari": id di scena duplicati, assenza di una scena START.
- **`content/scenes.sample.json` disallineato, ora corretto**: era
  fermo al 14/07 (prima delle specifiche 2-4) — scena 4 usava ancora
  `combatChoices[]` legacy invece del blocco `combat` di REGOLE.md
  §1.5. Sostituito; aggiunto `deathSceneId` al manifest e
  `locationName` dove il luogo cambia davvero (scene 1, 2, 3, 6 — le
  altre ereditano, dimostra il comportamento "appiccicoso" della
  specifica 4).
- **Non implementato per scelta**: `WeaponType` è task [MICHELE].
  `GameItem.weaponType` e `Character.weaponSkillType` restano `String`
  segnaposto (impalcatura pronta per lo swap all'enum quando Michele
  lo scrive).
- **Milestone verificata**: 12 test JVM verdi (`PackageRepositoryTest`
  carica il sample reale da risorsa e naviga il grafo;
  `PackageValidatorTest`, 9 casi, isola una violazione per test —
  destinazione inesistente, id duplicato, scena START assente,
  disciplina non canonica, `winSceneId` vuoto, buco/sovrapposizione in
  `rollOnItemTable`, globalRule non-ENDING come warning non errore).
  `./gradlew test` verde su tutti i moduli.

**Prossimo task: Fase 2 — `:core:engine`** (`GameState`, funzione
unica stat effettive, i 18 comandi, `CombatManager`, `DiceRoller` —
vedi `doc/REGOLE.md` e `doc/PIANO-SVILUPPO.md`). Task [MICHELE] in
coda: trascrizione tabella CRT da `LoneWolfRules` di v1 + fixture di
test scritte a mano.

### Attività — DiceRoller e funzione stat effettive (Fase 2, in corso)

Primo pezzo di `:core:engine`: interfaccia `DiceRoller` (tiro 0-9,
iniettata, mai `Random` inline) e `effectiveCombatSkill(Character)`
(base + somma modificatori attivi su COMBATTIVITA, REGOLE.md §1.2).
**TODO**: il bonus WEAPONSKILL nella funzione di stat resta da fare,
in attesa dell'enum `WeaponType` [MICHELE] (STATO.md §4.3) — senza un
tipo canonico non si può confrontare `weaponSkillType` con l'arma
impugnata. Fase 2 resta APERTA: `GameState`, `CombatManager`, i 18
comandi non ancora iniziati.

## 16/07/2026

### Sessione notturna — chiusura specifica 3 (stato e salvataggio)

**Specifica 3 CHIUSA** (`doc/STATO.md`). Decisioni:

- **`SessionData`** con `currentSceneId` esplicito (in v1 era assente,
  ricostruito per vie traverse dentro `GameCharacter`); `flags`/
  `variables` tipizzati e unificati a livello di sessione — corregge tre
  difetti di v1 insieme: il DM salvato come personaggio (retaggio
  chatbot, in Ex non esiste nella sessione), lo stato spezzato tra
  `HeroDetails` e `SessionData`, e la trappola `Map<String, Any>` di Gson
  (i numeri tornano `Double`). Formato **kotlinx.serialization**, un file
  JSON per pacchetto (`session_<packageId>.json`), dietro porta
  iniettabile nel modulo `data` (stesso pattern di `PackageSource`, test
  su file temporanei).
- **Auto-save a ogni transizione di scena**, dopo `gameMechanics` +
  `globalRules` (stato consistente per costruzione); vale per tutte le
  difficoltà. **Il combattimento è atomico**: non si salva a metà, un
  crash a metà combat riprende dall'ingresso della scena.
- **Difficoltà come meta-regola sul salvataggio** (non inflazione di
  statistiche), scelta a inizio avventura e immutabile: NORMALE 2
  checkpoint, DIFFICILE 1, IRON 0. Checkpoint piazzati dal giocatore dal
  menu, fotografia completa della `SessionData` su file separato,
  **scritti una volta e mai spostabili/sovrascrivibili**, ricaricabili
  illimitatamente (la durezza sta nell'irrevocabilità del piazzamento,
  non nel numero di ricarichi); il ricaricamento tronca il diario al
  punto del checkpoint. Morte in IRON = sessione cancellata, libro da
  capo.
- **Diario-grafo**: ogni voce `{sceneId, enrichedText, transition}` — il
  testo generato da Gemma si salva e non si rigenera mai (costo
  inferenza, non-determinismo, coerenza con `previous_scene_text`); la
  sequenza delle voci è il percorso completo nel grafo (non solo dove sei
  stato, anche per quale porta sei uscito); `visitedScenes` non esiste
  come lista salvata, è derivato dal diario.
- **Inventario canonico**: WEAPON max 2, BACKPACK_ITEM 8 posti,
  SPECIAL_ITEM illimitati, GOLD 50 Corone; oggetto
  `{name, type, quantity, combatUsable, effect}` con solo `HEAL:n`
  implementato in v0.1 (formato dichiarativo estensibile senza cambiare
  schema); limiti fatti rispettare dal motore, oltre soglia l'oggetto non
  entra senza errore. `UNARMED` resta solo specializzazione WEAPONSKILL,
  mai arma vera. `HUNTING` auto-soddisfa `requireAction action="EAT_MEAL"`
  a costo zero (una riga di logica per l'effetto canonico della
  disciplina).
- **Nota di estensibilità**: il sistema è pensato per altri regolamenti
  futuri (CRT dentro `LoneWolfRules` non nel motore, effetti oggetto
  dichiarativi, globalRules generiche, difficoltà esterna alle regole) —
  adattarlo tocca le implementazioni, non i contratti.

**Prossima specifica: 4 (UI)** — eredita code precise già tracciate nelle
sezioni finali di `doc/REGOLE.md` (scelta specializzazione WEAPONSKILL
alla creazione, menu tattico, opzione MINDBLAST disabilitata se nemico
immune) e di `doc/STATO.md` (scelta difficoltà in setup con spiegazione
onesta di IRON, menu checkpoint con budget visibile, schermata
inventario con equip/unequip, schermata diario/rilettura del viaggio).

### Sessione serale — chiusura specifica 2 (regole di gioco)

**Specifica 2 CHIUSA** (`doc/REGOLE.md`, sostituisce la versione con solo
il blocco 1). Blocchi 2-6:

- **`globalRules` nel manifest**: lista di regole condizione ->
  destinazione (`type`: FLAG | VAR, operatori ==/!=/>=/<=/>/<), valutate
  a ogni transizione DOPO l'esecuzione dei `gameMechanics` della scena di
  arrivo. Ordine di valutazione: morte built-in (Resistenza ≤ 0 ->
  `deathSceneId`) prima di tutto, poi le `globalRules` in ordine di
  scrittura, prima regola che matcha vince. `victorySceneId` come campo
  dedicato eliminato: la vittoria è una `globalRule` come le altre, non
  un caso speciale.
- **Gradi Kai puramente cosmetici**: enum con soglie nell'engine, nomi in
  `strings.xml`, nessun effetto meccanico (predisposizione concettuale
  per un futuro `requiredRank`).
- **MINDBLAST**: +2 Combattività per tutto il combattimento, attivabile
  una volta dal menu tattico; il nemico può essere
  `immuneToMindblast`. **WEAPONSKILL**: specializzazione scelta alla
  creazione del personaggio, un tipo d'arma oppure `UNARMED` (bonus a
  mani nude); il check "arma impugnata" arriva con la specifica 3.
  **HEALING**: passiva, +1 Resistenza a ogni transizione verso una scena
  senza combattimento, fino al massimo del personaggio. **Oggetti**:
  flag `combatUsable` + effetto dichiarato, visibili solo nel menu
  tattico in modalità completa.
- **Comandi TO_IMPLEMENT chiusi**: `removeItem` tollerante (rimuove
  quel che c'è, mai errore se manca quantità); `checkItemAndJump`
  valutato come `ifStat` nell'ordine dei `gameMechanics`;
  `rollOnItemTable` a intervalli espliciti di tiro 0-9 (validati per
  copertura completa e assenza di sovrapposizioni).
- **Dado del Destino fuori combattimento**: criterio narrativo — se il
  tiro decide il destino tira il giocatore (`skillCheck`,
  `randomChoiceTable`, teatro visibile); se decide una quantità tira il
  motore in silenzio (`randomQuantity`, `rollOnItemTable`).

**Emendamento §1.5** (nemico nella scena): l'authoring resta minimale
(nome + due statistiche + destinazioni nel JSON), ma a RUNTIME il motore
idrata un **`Character` unico** per tutti i ruoli (`role: HERO |
COMPANION | ENEMY | NPC`, enum al posto dell'id magico `"hero"` di v1),
erede di `GameCharacter`+`CharacterType` di v1 ripulito dai campi
Android/presentazione e dalle feature morte. Premio della simmetria: la
funzione di round diventa `resolveRound(a: Character, b: Character)` —
nemici con MINDBLAST proprio o duelli eroe-contro-eroe non costano
codice aggiuntivo al motore.

**Input già decisi per la specifica 3** (stato/salvataggio): si
serializzano i FATTI (stats base, inventario, `equippedWeapon`,
`weaponSkillType`, `StatModifier` narrativi con sourceType/duration), i
bonus (CS effettiva) si CALCOLANO con una sola funzione dell'engine, mai
persistiti. Base di riuso: `HeroDetails`/`ComputedStats` di v1; difetto
da non ripetere: in v1 `LoneWolfRules` sommava i modificatori per conto
proprio invece di passare da `ComputedStats`, calcolo duplicato.

**Nota per la specifica 5 (ETL)**: il secondo blocco storico del
progetto v1 fu l'estrazione dei libri con un modello poco capace.
Piano nuovo: parsing deterministico dell'XML Project Aon per la
struttura (scene, collegamenti, sezioni) + LLM usato solo per
classificare le meccaniche nei tag del config, con i validatori come
rete di sicurezza finale (non come sostituto del parsing strutturale).

### Sessione serale — apertura specifica 2 (regole di gioco)

**Aperta specifica 2** (`doc/REGOLE.md`). **Blocco combattimento CHIUSO:**
- Due modalità a scelta del giocatore a inizio combattimento: rapido (il
  motore itera i round fino all'esito, un solo riepilogo) e completo
  (round per round, menu tattico continua/oggetto/disciplina/fuga).
  Evasione, oggetti e discipline esistono solo nel completo.
- Evasione con costo canonico Lupo Solitario (un ultimo round in cui solo
  il giocatore subisce danni) e sblocco `evadeAfterRound` (fuga disponibile
  solo dopo N round, default 0 = subito); fuga via disciplina (es.
  CAMOUFLAGE) gratuita, offerta come scelta di scena prima del combattimento.
- Priorità degli esiti: `loseSceneId` di scena batte `deathSceneId`
  globale (il globale è il fallback, non il default); `winSceneId`
  obbligatorio.
- Nemico minimale nel JSON di scena: nome + `enemyCombatSkill` +
  `enemyEndurance` + destinazioni (`winSceneId`/`loseSceneId`/
  `evadeSceneId`/`evadeAfterRound`) — niente `GameCharacter` completo
  come in v1.
- `enemyName` tradotto da Gemma via nuova riga pipe `ENEMY|testo`, stessa
  filosofia di fallback di CHOICE/DISCIPLINE (parsing fallito non blocca
  mai il gioco).

**Scoperta da v1** (`doc/MATERIALE-REGOLE-V1.md`): l'intera orchestrazione
del combattimento in `MainViewModel` (loop round, `CombatState`, testi
vittoria/sconfitta) è COMMENTATA, mai attiva in v1 — il combattimento non
ha mai girato in produzione. Riuso reale limitato a due pezzi vivi: la
tabella CRT (`LoneWolfRules.COMBAT_RESULTS_CHART`, con la trappola
off-by-one sul tiro 0) e l'interfaccia `GameRulesEngine`
(`canUseDiscipline`, `getKaiRank`). Il `CombatManager` di Ex nasce da
zero, senza debito di compatibilità con un'orchestrazione mai testata.

**Codice da generare** (tracciato in `doc/REGOLE.md` §1.6, non ancora
fatto): tag `enemy_line` nel config (`^ENEMY\|(.+)$` ->
`updateEnemyName`), riga `ENEMY` aggiunta a `outputFormatText`, blocco
`combat` nella scena 4 (battle) di `scenes.sample.json`, regola
validatore che rende `winSceneId` obbligatorio quando `combat` è presente.

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