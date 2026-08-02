# Le classi del client Android — guida per chi arriva

Mappa del codice del **gioco**: `:core:data`, `:core:engine`, `:app`.
Per l'editor da scrivania c'è un documento gemello,
[`CLASSI-EDITOR.md`](CLASSI-EDITOR.md).

Non è un elenco completo (sono ~140 file): sono le classi che si
incontrano per prime e quelle che spiegano *perché* il resto è fatto
così. Le decisioni di design stanno in [`ARCHITETTURA.md`](ARCHITETTURA.md),
le regole di gioco in [`REGOLE.md`](REGOLE.md), il formato dei libri in
[`SCHEMA-JSON.md`](SCHEMA-JSON.md).

## Come stanno insieme i tre moduli

```
:core:data      lo SCHEMA del libro e i salvataggi — nessuna regola, nessuna UI
   ↑
:core:engine    le REGOLE: cosa succede quando il giocatore sceglie
   ↑
:app            Android: schermate, motore IA, suoni, immagini
```

La freccia va in un verso solo. `:core:data` e `:core:engine` **non
dipendono da Android**: si compilano e si testano da terminale, senza
emulatore, ed è il motivo per cui la suite gira in pochi secondi.

---

# `:core:data` — lo schema e i salvataggi

Qui vivono i dati e basta. Nessuna classe di questo modulo decide mai
*cosa succede*: descrive solo *cos'è*.

### `Manifest`
Un libro intero: metadati (titolo, lingua, genere, toni) e la lista di
tutte le `Scene`. È ciò che si ottiene aprendo un file JSON.

### `Scene`
Una scena: `narrativeText`, le `choices`, le `disciplineChoices`,
l'eventuale `combat`, le immagini, i comandi `mechanics` e i
`rollModifiers`. **Tutti i campi nuovi si aggiungono in coda con un
default**, così i libri e i salvataggi esistenti continuano a caricarsi.

### `Character`
L'eroe: `baseCombatSkill`, `currentEndurance`, `kaiDisciplines`,
`inventory`, `activeModifiers`. Contiene i **fatti**, mai i totali —
vedi `EffectiveStats` più avanti.

### `PackageRepository` / `PackageSource`
`PackageSource` è una delle quattro interfacce del progetto: dice
soltanto "dammi il testo del libro", senza sapere se arriva dagli asset
dell'APK o da un file scelto dall'utente. `PackageRepository.load()` lo
legge, lo deserializza e lo **valida**, restituendo un
`PackageLoadResult` (successo con eventuali avvisi, oppure errori).

### `PackageValidator` e i validatori specifici
`PackageValidator` compone i validatori di dettaglio
(`GraphValidator`, `CombatValidator`, `DisciplineValidator`,
`RollModifierValidator`, `ImageReferenceValidator`, …). Distinzione che
conta: **errore = il libro non si carica**, **avviso = si carica e si
segnala**. Un'immagine mancante è un avviso, una scena che punta al
nulla è un errore.

### `SessionStore` / `FileSessionStore`
Il salvataggio della partita. La scrittura è **atomica** (file
temporaneo e poi rinomina): un salvataggio interrotto non deve mai
lasciare un file mezzo scritto che sembra buono.

### `ImageReference`
Interpreta i valori `static:loc_tavern` e `url:https://…`. Un prefisso
sconosciuto non è un errore: degrada su "nessuna immagine".

---

# `:core:engine` — le regole

Logica pura, testabile da terminale. Se una funzione qui dentro ha
bisogno di Android, è nel modulo sbagliato.

### `GameState`
La **fonte di verità unica** della partita in corso: eroe, scena
attuale, flag di storia, diario. Tutto il resto lo legge, nessuno ne
tiene una copia.

### `TransitionEngine`
Il cuore: dato uno stato e una scelta, produce lo stato successivo.
Applica i comandi della scena, valuta le regole globali, decide se la
partita è finita.

### `EffectiveStats`
**Da leggere per primo se si toccano le statistiche.** Contiene la
regola più importante del progetto:

> Si serializzano i fatti, i bonus si calcolano.

- `effectiveCombatSkill(character)` — base + modificatori attivi +
  bonus della specializzazione + bonus degli oggetti posseduti;
- `effectiveEndurance(character)` / `effectiveMaxEndurance(character)`;
- `itemEnduranceBonus(item)` e `itemCombatSkillBonus(item)` — leggono
  l'effetto dichiarativo dell'oggetto (`ENDURANCE:2`, `COMBAT_SKILL:2`).

Nessun totale viene mai salvato: perdere un oggetto fa sparire il suo
bonus da sé. In v1 questo calcolo era duplicato altrove ed è stata la
fonte di bug ricorrenti — non si ripete.

### `CombatSession` e `CombatResultsTable`
Il combattimento round per round. `CombatResultsTable` è la tabella
canonica di Lupo Solitario (rapporto di forza da −10 a +10, danni per
ogni tiro): dato pregiato, ereditato da v1 e riusato integralmente.

### `ChoiceAvailability` e `RollModifiers`
Quali scelte sono aperte: per disciplina posseduta, oggetto, flag,
intervallo di tiro. `RollModifiers.totalFor(scene, state)` somma i
bonus condizionali al tiro (per esempio *"se hai il Sesto Senso puoi
aggiungere 2"*), e `forRoll()` riceve il totale **già modificato**.

### `DiceRoller` / `RandomDiceRoller`
Interfaccia (una delle quattro) più implementazione. Esiste per una
ragione sola: nei test si inietta un dado prevedibile e si verifica una
regola senza dipendere dalla fortuna.

### `Inventory`, `ItemMechanics`, `MealRules`, `StatMechanics`
Le meccaniche che i comandi della scena eseguono: aggiungere o
consumare oggetti, mangiare, modificare le statistiche. Ognuna piccola
e per conto suo.

### `PromptBuilder` (+ `PromptContext`)
Compone il prompt per il modello. **Frammenti di testo tutti qui e solo
qui**: se un giorno il modello inizia a comportarsi male, si guarda un
file solo. Due modalità — arricchimento e sola traduzione — scelte con
`translationMode`.

Vive in `:core:engine` (non in `:app`) perché **anche l'editor deve
costruire lo stesso identico prompt**: un'anteprima fatta con un prompt
diverso mostrerebbe un risultato che il giocatore non vedrà mai.

### `ResponseParser` (+ `EnrichedScene`)
Il contrario: prende la risposta del modello e ne ricava il testo
narrato, i testi tradotti delle scelte, il nome del nemico. Tutto ciò
che segue il separatore `--- TAGS ---` non si mostra mai a schermo.

### `InferenceConfig`, `ModelCatalog`, `LinguaOutput`
Parametri di generazione (`TRANSLATION_PRESET` è il preset fedele), i
modelli scaricabili con i loro link, e le lingue selezionabili (24
ufficiali UE più altre europee). Condivisi con l'editor per lo stesso
motivo di `PromptBuilder`.

### `AdventureEnding`
Vittoria e sconfitta globali — concetto che v1 non aveva. Se il libro
non prevede una scena per l'esito, il finale viene **fabbricato** e
scritto dal modello.

---

# `:app` — Android

### `AppContainer`
Il contenitore delle dipendenze: costruisce motori, repository e
preferenze una volta sola. Espone anche stato osservabile da Compose
(`isModelLoading`, `loadedModelId`) e serializza i caricamenti del
modello con un `Mutex` — due caricamenti insieme di un file da 3,7 GB
non finirebbero bene.

### `InferenceEngine` (interfaccia) e `LiteRtLmEngine`
Una delle quattro interfacce. Il resto dell'app **non sa che esiste
Gemma**: chiede di arricchire una scena e riceve testo.
`LiteRtLmEngine` è l'implementazione su LiteRT-LM: prova la GPU e
ripiega sulla CPU, apre una sessione nuova per ogni scena (l'inferenza
è **senza memoria**: il diario non entra mai nel prompt) e registra le
misure di prestazione nel log.

### `SceneNarrator`
Orchestra il giro di una scena: compone il prompt, apre la sessione,
genera in streaming, consegna il risultato parsato.

**Regola non negoziabile**: qualunque cosa vada storta — modello
assente, motore che non parte, risposta vuota — non blocca il gioco. Si
consegna il testo originale del libro. Il giocatore non resta mai
davanti a una schermata vuota.

### `AdventureState` e `AdventureScreen`
Lo stato della schermata di gioco e la sua UI. `AdventureState` tiene
insieme `GameState`, narrazione in corso, tiri di dado e combattimento;
`AdventureScreen` disegna e basta.

### `CharacterSheetScreen`
La scheda del personaggio. **Spiega** i numeri invece di ricalcolarli:
sotto la Combattività elenca base, modificatori, specializzazione e
oggetti — tutti letti da `EffectiveStats`.

### `ModelsRoute` / `ModelDownloadWorker`
Scelta e scaricamento dei modelli sul telefono, con ripresa e stato
persistente.

### I cataloghi delle immagini
`SceneImageCatalog` (sfondi, in `:core:engine` perché serve anche al
prompt), `NpcImageCatalog`, `EnemyImageCatalog`: **vocabolari chiusi**.
Un id sconosciuto non è mai un errore — si degrada sull'immagine di
default.

---

## Le quattro interfacce, e perché sono solo quattro

`RulesEngine`, `InferenceEngine`, `DiceRoller`, `PackageSource`.
Ognuna esiste perché ha **almeno due implementazioni reali o un
bisogno di test** che la giustifica. Non se ne aggiungono altre "per
flessibilità": un'astrazione senza un secondo caso d'uso è solo un
livello in più da attraversare quando si cerca un bug.

## Da dove cominciare a leggere

1. `Scene` e `Manifest` — cos'è un libro;
2. `GameState` e `TransitionEngine` — cosa succede quando si sceglie;
3. `EffectiveStats` — la regola dei fatti contro i totali;
4. `SceneNarrator` — come entra in scena il modello, e come si esce
   indenni quando non c'è.
