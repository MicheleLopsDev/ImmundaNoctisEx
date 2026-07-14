# Immunda Noctis Ex — Documento di Progetto

> Motore GDR/libro-game per Android ambientato nell'universo di Lupo Solitario,
> narrato da IA locale, interamente offline.
> Progetto nuovo (repo separato), nato dall'esperienza di Immunda Noctis v1.

Repository: https://github.com/MicheleLopsDev/ImmundaNoctisEx.git

Ultimo aggiornamento: 14 luglio 2026

---

## 1. Visione

Un motore di mini-avventure testuali in stile libro-game, basato sulle regole e
sull'ambientazione di **Lupo Solitario** (Magnamund), giocato in solitaria.

Il modello è **editoriale**: i "libri" (pacchetti avventura) vengono creati e
curati dall'autore in un flusso di lavoro separato ("master"), e rilasciati come
contenuti che l'app di gioco consuma. I libri sono **grafi di scene ramificate**
(struttura da libro-game classico: ogni scelta porta a una scena successiva via
`nextSceneId`), non sequenze randomizzate. La rigiocabilità nasce da due fonti
distinte: i bivi del grafo (percorsi diversi ad ogni partita) **e** la prosa
rigenerata da un modello IA locale (Gemma 4) a ogni partita, così che lo stesso
libro non si legga mai due volte con le stesse parole.

Il motore Kotlin è l'autorità assoluta su regole, stato e esiti. Gemma narra.
Tutto gira sul dispositivo, senza connessione richiesta durante il gioco.

## 2. Continuità con Immunda Noctis v1

Questo non è un fork: è un progetto nuovo per non "sporcare" quanto già fatto.
La lezione principale di v1: il progetto è collassato per **eccesso di
complessità simultanea** (doppio motore IA, bridge C++, generazione immagini,
traduzione ML Kit, ViewModel monolitico), non per problemi delle singole parti.

### Riusato (concettualmente, non necessariamente come codice)
- **`GameRulesEngine` (branch develop di v1)** — interfaccia che astrae il sistema
  di regole (`resolveCombatRound`, `canUseDiscipline`, ...). Da riprendere pari pari.
- **`LoneWolfRules` (branch develop di v1)** — implementazione completa e collaudata
  della meccanica di Lupo Solitario: Tabella dei Risultati di Combattimento,
  rapporto di forza, tiro 0–9, modificatori. **È la meccanica ufficiale di Ex.**
- **Il DSL dei tag di gioco** (TAGS.MD + DOC/ di v1) — `<ADD_ITEM>`, `<STAT_MOD>`,
  `<COMBAT>`, `<EVADE_COMBAT>`, `<SET_FLAG>`, `<IF_STAT>`, `<skillCheck>`,
  `<randomChoiceTable>`, `<conditionalAction>`, variabili globali, scelte
  condizionali (`requiredItem`, `requiredFlag`, `choiceCondition`,
  `minRoll`/`maxRoll`). Mappa quasi uno-a-uno sul **function calling nativo**
  di Gemma 4: da tag testuali parsati via regex a chiamate di funzione strutturate.
- **La struttura scene di develop** — campi `gameMechanics`, `combatChoices`
  (con esiti WIN/LOSE), `itemChoices`, `disciplineChoices`, `location`. Pensata
  per scene, non per libri: resta valida.
- **Il design del combattimento** già delineato in
  `DOC/check implementazioni meccaniche di gioco.md`: `CombatState`, loop di round,
  regole pure, esiti via tag, morte a Resistenza ≤ 0, effetti speciali.
- **La centralizzazione via `StatModifier`**: ogni modifica alle statistiche passa
  da modificatori tracciabili e reversibili (`sourceType`, `duration`), mai
  toccando direttamente le statistiche base.
- **`items.json` separato** come database unico degli oggetti (fonte unica di
  verità), dall'idea della pipeline di estrazione di v1.
- **Il "Dado del Destino"** — il ritratto dell'eroe con bordo oro/argento come
  pulsante di tiro contestuale (vedi §9).
- Scheda personaggio di v1 (layout: statistiche, slot armi, discipline, zaino,
  oggetti speciali, oro, pasti) — da riportare con lo stesso design.
- `TtsService.kt` — TTS di sistema Android, selezione voce per genere/lingua.
  Riusabile quasi invariato.

### Abbandonato
- Architettura a doppio motore (MediaPipe + llama.cpp/GGUF con bridge C++).
- **Generazione immagini (moduli STDF/Stable Diffusion)** e tutto il suo indotto:
  backend C++, servizi in foreground, gestione modelli multipli. I ritratti e le
  illustrazioni di Ex sono **asset statici** curati.
- `TranslationEngine.kt` (ML Kit). Il multilingua è gestito da Gemma 4: la lingua
  del giocatore è un parametro del prompt. Le stringhe fisse dell'interfaccia
  restano su `strings.xml` standard.
- **Solo il doppio testo** en/it dello `scenes.json` di v1 con *Flight from the
  Dark* pre-tradotto (350 scene, 13k righe): la conversione ora è il modello
  (grafo con prosa originale in una lingua sola), non viene abbandonata.
- **Input di testo libero verso l'LLM.** L'interazione è esclusivamente a scelte
  strutturate (vedi §8). Sparisce l'intera classe di problemi "il giocatore scrive
  qualcosa di imprevisto e il modello improvvisa rompendo le regole".
- Documenti-fotografia stile `ANALISI.MD` (analisi statiche che invecchiano male):
  questo DESIGN.md è l'unico documento vivo, aggiornato a ogni decisione.

## 3. Hardware di riferimento

Dispositivo di test: **Motorola Razr 70 Ultra** — Snapdragon 8 Elite (2x Oryon
Prime 4,32 GHz + 6x Oryon Performance 3,53 GHz), 16 GB RAM, GPU Adreno 830.
Flagship pieghevole: schermo interno alto e stretto, perfetto per la scena
teatrale verticale. Nota: con 16 GB di RAM il candidato primario diventa
Gemma 4 E4B.

## 4. Stack tecnico

| Area | Scelta |
|---|---|
| Linguaggio | Kotlin |
| Motore IA | Gemma 4 E4B candidato primario (confermare con test su device) |
| Runtime IA | **LiteRT-LM** (non MediaPipe LLM Inference API, in maintenance-only) |
| Ponte narrazione↔motore | Function calling nativo di Gemma 4 / LiteRT-LM |
| UI | Jetpack Compose (decisione chiusa: v1 era già interamente Compose, componenti riusabili; supporto nativo ai pieghevoli via WindowSizeClass) |
| TTS | `android.speech.tts` nativo di sistema |
| Traduzione | Nessuna libreria — multilingua via prompt Gemma 4 |
| Immagini | Asset statici (nessuna generazione runtime) |

## 5. Architettura logica

Separazione netta di responsabilità:

- **Motore di gioco (Kotlin)** — autorità assoluta. Stato, personaggio, inventario,
  regole, tiri, esiti di sfide e combattimenti, navigazione tra scene, filtro
  delle scelte disponibili.
- **Narratore (Gemma 4)** — riceve dal motore un contesto strutturato e restituisce
  solo prosa. Non decide mai un esito, non genera mai scelte: le scelte sono
  pulsanti costruiti dal motore a partire dai dati del libro.
- **Combattimento in componente dedicato** (`CombatManager` o simile) con propria
  macchina a stati: `Inizio → Round → (Evasione?) → Esito → Narrazione`. Il
  ViewModel lo osserva, non lo implementa. Lezione diretta dal post-mortem di v1
  (vedi §10).

### Ciclo di gioco
1. Il motore determina la scena corrente → costruisce il prompt per il DM
   (canovaccio + seme narrativo + contesto + esiti già risolti dal motore)
2. Gemma narra → il testo appare attribuito al DM (anello di parola acceso)
3. Il motore mostra i pulsanti delle scelte, filtrati per condizioni
   (oggetti, discipline, flag); se serve un tiro, appare il Dado del Destino
4. Il giocatore tocca → il motore risolve (navigazione / tiro / combattimento)
   → si torna al punto 1

L'LLM non riceve mai input arbitrario dell'utente: solo dati strutturati dal motore.

## 6. Modello editoriale: i "libri"

### Due fonti, stesso formato
I libri arrivano da due fonti distinte, ma nello stesso formato di pacchetto:
- **(a) Conversione ETL dei libri originali di Lupo Solitario** — pipeline
  `PROMPT.txt` di v1, raffinata ed eseguita con Claude Code, che converte il
  libro in grafo **mantenendo la prosa originale** (una lingua sola), non la
  comprime in canovaccio. **Uso personale, MAI distribuiti** (materiale
  Project Aon, non redistribuibile).
- **(b) Libri originali dell'autore**, scritti/generati con assistenza IA.
  **Distribuibili.**

### Flusso master → gioco
- I libri (pacchetti avventura) vengono creati e validati **fuori dall'app di
  gioco**, in un flusso di lavoro "master", e rilasciati come contenuti.
- **Fase 1 (subito):** la "master app" è il *formato* + gli *strumenti*: schema
  JSON del pacchetto, script di validazione (riferimenti tra scene, vincoli,
  campi obbligatori), creazione assistita dei libri (es. con Claude Code come
  assistente d'autore). Nessuna GUI.
- **Fase 2 (eventuale, dopo):** un editor grafico vero (es. Compose Desktop,
  riusando il modello dati Kotlin). Solo quando il gioco funziona — il rilascio
  del gioco non deve mai dipendere dall'editor.
- **Visione futura (Fase 2 estesa):** "app generatrice" — un editor/generatore
  di scene assistito da IA che produce pacchetti validi contro lo schema.

### Modello di distribuzione (decisione chiusa)
L'app esce con **un libro originale incluso nell'APK**; gli altri pacchetti si
caricano da file (side-load). L'app distribuita **non contiene materiale
Project Aon** — i libri convertiti via ETL restano d'uso personale.

### Nomenclatura
I contenuti distribuibili useranno ambientazione e nomi propri **originali**
(refactor dei nomi previsto, in seguito); le meccaniche restano quelle di Lupo
Solitario. I nomi vivono solo nei pacchetti JSON: il motore è agnostico rispetto
all'ambientazione.

### Contenuto di un pacchetto libro (bozza)
- **Manifest**: id, versione, titolo, descrizione
- **Grafo di scene** tipizzate (canovacci, vedi §7), navigazione via
  `nextSceneId`. Le dimensioni del libro sono libere (il libro 1 convertito ha
  ~350 scene; il libro originale incluso nell'APK sarà breve, indicativamente
  ~10-20 scene)
- **items.json**: database oggetti del libro
- **Bestiario**: nemici con Combattività/Resistenza/immunità
- Scena iniziale e scene finali fisse

## 7. Narrazione generativa: prosa arricchita, non canovacci

**Decisione ribaltata** rispetto alla versione precedente di questo documento:
le scene **contengono la prosa finita** (`narrativeText`, una sola lingua,
preferibilmente inglese), scritta da autori umani. Niente compressione in
canovaccio.

Motivazione: gli autori dei libri devono poter scrivere normalmente. Un
formato a canovaccio (`beats`/`mustMention`) richiederebbe competenza logica,
non narrativa — sbagliato per lo strumento di scrittura futuro.

### Ruolo di Gemma
Gemma **non genera da zero**: **arricchisce** la prosa esistente adattandola
a genere e tono della partita (fantasy/horror/comico/sci-fi...), mantenendo
fatti, struttura ed esiti inalterati.

### Contesto passato a Gemma
Il minimo indispensabile, deciso in questa sessione:
- `narrativeText` della scena **precedente**
- `narrativeText` della scena **attuale**
- `narrativeText` delle possibili **continuazioni** (esiti delle scelte/combattimenti)
- **genere** e **toneHints**

Serve solo a raccordare, non a inventare. Niente tag, niente statistiche,
niente meccaniche nel prompt.

### Prompt stateless
Nessuna chat history che si accumula: l'architettura conversazionale di
`LlamaCppEngine` di v1 non si riusa per questo. La "memoria" è il grafo
stesso, non la conversazione con il modello.

### Sacro (il motore lo impone, Gemma non può alterarlo)
- I fatti della scena: chi c'è, cosa accade, cosa si trova
- Nomi propri, statistiche dei nemici, oggetti, esiti dei tiri
- Le scelte disponibili (sempre generate dal motore dai dati, mai dal testo)
- **La struttura della prosa originale**

### Rigenerabile (Gemma lo reinventa a ogni partita)
- Solo il **colore/atmosfera/registro** applicato da Gemma sopra la prosa
  originale

### Sistema promptDescription (da v1)
Il `config.json` di v1 si riusa **integralmente**: registro dichiarativo di
tag (`regex` + `type` + `parameters` + `command` + `replace`), con i prompt
definiti come **dati** nel JSON (tag `start_adventure_prompt`, tipo
`promptDescription`, frammenti parametrici con placeholder tipo `{genre}` e
`{scene_narrative_text}` riempiti a runtime).

Per Ex: si estende `start_adventure_prompt` con i frammenti per scena
precedente/continuazioni; i `gameMechanic` D&D (`strength`/`dexterity`/
`intelligence`/`spellcraft`) si sostituiscono con le Discipline Kai;
prospettiva futura: ogni pacchetto libro può portare i propri prompt.

### Seme narrativo
A inizio avventura il motore estrae un piccolo insieme di variabili casuali
(es. stagione, meteo dominante, ora prevalente, umore del mondo) che entra nel
system prompt del DM per tutta la partita. Dà coerenza interna alla singola
partita e differenzia percepibilmente le partite tra loro — non ci si affida
alla sola temperatura del modello.

### Multilingua
Resta via Gemma: la lingua del giocatore è parametro del prompt, il pacchetto
ha una sola lingua. Sparisce il doppio testo en/it di v1.

## 8. Interazione: struttura da libro-game

- **Nessun campo di testo libero.** Il DM narra; sotto compaiono le scelte come
  pulsanti (i bivi della scena). Icone contestuali appaiono solo quando servono:
  discipline utilizzabili nella scena, Dado del Destino, uso oggetti.
- Le scelte sono filtrate dal motore per condizioni: `requiredItem`,
  `requiredFlag`, `choiceCondition`, possesso discipline.
- La chat è di sola lettura: ogni messaggio ha un autore preciso (DM o Compagno),
  mai testo anonimo.

## 9. Il Dado del Destino

Ripreso da v1: il ritratto dell'eroe nella barra del giocatore è il pulsante di
tiro. Bordo **oro** e cliccabile quando la scena richiede un tiro (le scelte
normali vengono nascoste finché non si tira); bordo argento e inerte altrimenti.
Tiro 0–9, fedele alla Tabella del Numero Casuale di Lupo Solitario.

Per Ex:
- **Un solo canale di risoluzione**: il tiro è risolto dal motore, che poi passa
  il risultato al DM per la narrazione (in v1 convivevano due strade parallele:
  `minRoll`/`maxRoll` sulle choices e i tag `<randomChoiceTable>`/`<skillCheck>`).
- Due usi della stessa meccanica: tiro secco per i bivi casuali, e **prova di
  abilità** con modificatore da disciplina (es. +2 se possiedi Hunting).
- Piccola cerimonia visiva del risultato (numero grande, breve animazione) prima
  della narrazione dell'esito: è il momento di tensione del libro-game.
  (Era in roadmap v1, mai completato.)

## 10. Combattimento

### Post-mortem di v1 (branch develop)
- ✅ Le regole pure funzionavano: `LoneWolfRules.resolveCombatRound()` implementava
  correttamente l'intera Tabella dei Risultati di Combattimento.
- ✅ Esisteva l'inizio della gestione di evasione e scelte contestuali.
- ✅ Le scene di test (`test_combat.json`, `test_combat_evasion.json`) definivano
  già la struttura dati: `combatChoices` con esiti WIN/LOSE, evasione via
  disciplina.
- ❌ Il punto di rottura era **l'orchestrazione**: in `MainViewModel.kt` (~1400
  righe) tutta la gestione dello stato del combattimento è finita commentata.
  Il ViewModel faceva tutto ed è collassato sotto il proprio peso.

### Direzione per Ex
- **Meccanica: Lupo Solitario** (tabella, rapporto di forza, tiro 0–9). Decisione
  chiusa — niente d20/5 caratteristiche.
- Componente dedicato e isolato con macchina a stati; regole pure senza stato
  dietro `GameRulesEngine`, testabili senza Android né LLM.
- L'LLM narra l'esito di round/combattimento dal log strutturato del motore;
  non partecipa mai alla risoluzione.
- Tutte le modifiche a statistiche via `StatModifier` (tracciabili, reversibili).

## 11. Personaggi

### Sistema a "scena teatrale"
Header con i ritratti degli attori: **DM, Eroe, Compagno**. Chi sta parlando ha
un **anello evidenziato** attorno al ritratto (stato `speakingCharacterId`
osservato dalla UI). Ogni testo in chat è attribuito.

### Eroe
- Creazione: tiro di Combattività/Resistenza, scelta arma iniziale, discipline
  Kai, **scelta maschio/femmina** (ritratto dedicato; il campo `gender` guida
  anche la voce TTS), scelta della taglia dell'avventura.
- Scheda personaggio ripresa dal layout di v1.

### Compagno — versione iniziale (predisposto ora, esteso dopo)
- **Bonus statico** finché è nel gruppo, implementato come
  `StatModifier(sourceType = COMPANION)` — quando se ne va, si rimuove il
  modificatore. Zero logica speciale nel motore.
- **Commenti occasionali**: dopo certe scelte (non tutte — probabilità o scene
  marcate), seconda chiamata breve a Gemma con il "cappello" del compagno.
  L'anello si accende sul suo ritratto. Il commento parte **dopo** che la
  narrazione del DM è già visualizzata, così l'attesa non si percepisce.
- **Nessuna interazione diretta**: non gli si parla; commenta e basta.
- Predisposizione a costo quasi zero: lista `companions` nella sessione (vuota
  all'inizio), slot ritratto che non si mostra se assente, campo "compagni
  presenti" nel system prompt del DM.

### Doppio prompt (stesso modello Gemma 4, due "cappelli")
- **Prompt DM**: narratore onnisciente, seconda persona, descrive scene/esiti/
  combattimenti, non parla mai come personaggio, non decide mai esiti (li riceve
  risolti dal motore).
- **Prompt Compagno**: prima persona, personalità definita, 1–2 frasi, reagisce
  all'ultima scelta/evento, non narra mai, non anticipa la trama.

Nota di realismo: il commento del compagno è una seconda inferenza — per questo
è occasionale e posticipato rispetto alla narrazione.

## 12. UI (direzione)

- Menu principale ridotto a tre voci: **Avventura, Modello IA, Impostazioni**
  (in v1 erano cinque, con Genera Immagini e Modelli STDF).
- Schermata avventura a tre fasce come v1: header ritratti / narrazione / barra
  del giocatore (statistiche, pasti, grado Kai, discipline, Dado del Destino).
- Indicatore di progresso "Scena N di M" al posto del "Paragrafo: N" di v1
  (informazione utile: quanto manca alla fine della mini-avventura).
- Tema scuro e semaforo token: da tenere.
- Possibile evoluzione: la mappa di Magnamund nell'header che riflette la
  `location` della scena corrente.

## 13. Decisioni aperte

- [x] Compose vs XML per la UI — **chiusa: Jetpack Compose** (vedi §4)
- [x] E2B vs E4B — **chiusa: Gemma 4 E4B candidato primario**, resta solo la
  conferma con test prestazionali reali sul device (vedi §3)
- [x] Distribuzione dei libri — **chiusa: un libro originale incluso nell'APK
  + pacchetti aggiuntivi via side-load** (vedi §6)
- [ ] Schema JSON definitivo del pacchetto libro (manifest, scene, vincoli) —
  la struttura scena riparte da quella di v1 quasi invariata (`narrativeText`
  a lingua singola + `genre` + `toneHints`), non serve disegnare un formato
  canovaccio

Il seme narrativo (§7) resta il meccanismo di variazione tra le partite.

## 14. Roadmap

1. Documento delle regole di gioco consolidato (questa base + dettagli fini) —
   **in corso**
2. Schema JSON del pacchetto libro (manifest, canovacci, items, bestiario) +
   script di validazione
3. Conversione ETL del libro 1 (*Flight from the Dark*) in pacchetto — con
   Claude Code, partendo da `PROMPT.txt` di v1
4. Scheletro progetto Android (dipendenze LiteRT-LM, struttura moduli)
5. Motore di gioco Kotlin (stato, regole, randomizzatore, CombatManager) —
   senza IA, testabile da solo
6. Integrazione Gemma 4 via LiteRT-LM + function calling
7. UI (scena teatrale, scelte a pulsanti, Dado del Destino)
8. Compagno (bonus + commenti)
9. TTS
10. Test prestazionali reali su Motorola Razr 70 Ultra

## 15. Changelog

- **13/07/2026** — Creazione documento. Prima bozza basata sull'analisi di
  fattibilità e sulla revisione del codice di Immunda Noctis v1.
- **13/07/2026** — Analisi del branch develop di v1: post-mortem del combattimento,
  `GameRulesEngine`/`LoneWolfRules` tra i riusabili, URL del repository.
- **13/07/2026** — Revisione completa dopo lettura di tutti i documenti MD e JSON
  di v1 e definizione della direzione di prodotto: ambientazione Lupo Solitario
  senza conversione dei libri; modello editoriale (master → pacchetti libro →
  app) con randomizzazione delle sequenze; taglie 6/12/18; canovacci con
  distinzione sacro/rigenerabile e seme narrativo; niente input libero (struttura
  da libro-game a scelte); sistema personaggi a scena teatrale con anello di
  parola; compagno v1 (bonus statico + commenti occasionali); doppio prompt
  DM/Compagno; Dado del Destino; meccanica di combattimento Lupo Solitario
  confermata; tagli definitivi di STDF/generazione immagini e multi-modello.
- **14/07/2026** — Libro come grafo di scene ramificate al posto della
  randomizzazione della sequenza; introdotta la pipeline ETL (da v1, via
  Claude Code) per convertire i libri originali di Lupo Solitario, d'uso
  personale e mai distribuiti; modello di distribuzione chiuso (un libro
  originale incluso nell'APK, altri pacchetti via side-load); nomenclatura
  neutra/originale per i contenuti distribuibili, da definire in un refactor
  futuro; UI chiusa su Jetpack Compose; Gemma 4 E4B candidato primario;
  dispositivo di riferimento aggiornato a Motorola Razr 70 Ultra; aggiunta la
  visione di un'"app generatrice" di scene assistita da IA come fase 2 estesa.
- **14/07/2026 (sera)** — Prosa finita mantenuta nei pacchetti (ribaltata la
  scelta canovacci); Gemma arricchisce e raccorda (scena precedente + attuale
  + continuazioni); prompt stateless senza chat history; riuso integrale del
  `config.json` di v1 (sistema `promptDescription`); tag D&D da sostituire
  con Discipline Kai.
