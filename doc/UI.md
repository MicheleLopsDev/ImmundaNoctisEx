# Specifica 4 — Interfaccia utente

Stato: **CHIUSA** (sessione 17/07/2026).

Riferimento visivo: screenshot della UI di v1 ("L'Ultimo dei Kai") —
l'estetica di v1 si CONSERVA (tema scuro, banner con ritratti
sovrapposti, card personaggio con grado dorato e icone stats). Si
correggono solo i tre elementi dell'era chatbot: via la barra di testo
libero "Cosa fai?" (sostituita dalla zona scelte), via le bolle chat
(il testo scorre come pagina di libro), via il DM come personaggio
(resta come presenza visiva, vedi sotto).

Vincoli di architettura (doc/ARCHITETTURA.md): single-activity Compose,
un package per schermata senza import incrociati, un ViewModel per
schermata, file di navigazione solo routing (~100 righe max).

---

## Mappa delle schermate (7)

1. **Home** — menu a riquadri come la Home di v1 (deciso 17/07/2026,
   riferimento: screenshot v1), SENZA le tile STDF (Genera Immagini e
   Modelli STDF: feature morta, non esiste in Ex). Tre riquadri:
   - **Avventura**: carica l'avventura. Se esistono più salvataggi
     (uno per pacchetto, `session_<packageId>.json`) chiede quale
     continuare; altrimenti/in alternativa si parte con una nuova
     avventura (→ Setup avventura → Creazione personaggio → scena di
     gioco). Qui vive anche il side-load del libro (picker di sistema).
   - **Modelli LLM**: download di Gemma e configurazione del motore
     di inferenza. È una sezione, non un mondo a parte (in v1
     ModelActivity era 824 righe).
   - **Impostazioni**: le Opzioni (schermata 7).
2. **Setup avventura** — scelta libro caricato, **scelta difficoltà**
   (NORMALE/DIFFICILE/IRON, con spiegazione onesta di cosa comporta
   IRON), avvio creazione.
3. **Creazione personaggio** — scelta **lupo o lupa** (ritratto
   dedicato per genere), tiro delle stat col Dado del Destino (prima
   apparizione teatrale), scelta delle 5 discipline (dalle 10
   canoniche), **specializzazione WEAPONSKILL** se scelta (tipo arma
   o UNARMED), oggetti iniziali.
4. **Avventura** — la scena teatrale (dettaglio sotto).
5. **Scheda personaggio** — due tab: *Stats e Discipline* (valori
   effettivi calcolati, grado Kai, elenco discipline) ed
   *Equipaggiamento e Zaino* (dettaglio sotto). Vi si accede toccando
   la card di stato nella scena o dall'icona header.
6. **Diario del viaggio** — due viste: *Racconto* (rilettura del
   percorso voce per voce: testo arricchito + scelta fatta) e
   **Mappa logica** (i luoghi visitati come nodi col nome, collegati
   nell'ordine del viaggio — derivata dal diario-grafo raggruppando
   le voci per `locationName`; v0.1: solo il nome del luogo; in
   futuro annotabile con combattimenti — già derivabili dalle
   Transition WIN/LOSE — NPC importanti e oggetti trovati). Più
   **esportazione** dell'avventura in testo/Markdown condivisibile
   (il diario-grafo è già un generatore di racconto).
7. **Opzioni** — **tema chiaro/scuro** dell'app (riuso del pattern
   `ThemePreferences` di v1); **abilitazione TTS** (auto-lettura,
   velocità, pitch, **voce per genere**: una maschile e una femminile
   tra quelle di sistema, come TtsPreferences di v1; con auto-lettura
   ACCESA l'icona leggi nei blocchi del narratore è grigia);
   gestione modelli, lingua. Il salvataggio della narrazione è sempre
   automatico: NON è un'opzione (deciso 17/07/2026).

---

## La scena teatrale (schermata Avventura)

Layout a fasce, dall'alto:

### Header
Titolo libro, "Scena N" (ID = pagina del libro), **semaforo di stato
motore** (pallino: verde pronto / giallo generazione / rosso contesto
quasi pieno; il tocco mostra il dettaglio token — assorbe il contatore
0/10240 di v1), icone scheda personaggio e diario.

### Banner (il palcoscenico)
`backgroundImage` della scena a tutto schermo in larghezza; default
(mappa di Magnamund) se la scena non ne dichiara uno. Ritratti
circolari sovrapposti al bordo inferiore: **narratore** + eroe +
compagno (quando esisterà).

- **Il narratore è una presenza visiva, NON un Character nei dati**:
  puro elemento UI, come il sipario di un teatro.
- **Cerchio d'oro su chi parla**: sul narratore mentre Gemma streama
  la narrazione; sul compagno quando commenterà (seconda inferenza,
  posticipata). Convenzione ereditata da v1.

### Flusso centrale (la pagina del libro)
Il testo scorre come pagina di libro (serif, continuo), NON come chat:

- Il testo del narratore arriva in streaming (solo fino al separatore
  `--- TAGS ---`) dentro un **fumetto/blocco del narratore** con tre
  icone: **copia**, **originale/tradotto** (toggle: mostra il
  narrativeText inglese del pacchetto / il testo arricchito — entrambi
  già in memoria, costo zero; l'icona "traduci" di v1 reinterpretata),
  **leggi (TTS)** — attiva solo se l'auto-lettura è SPENTA in Opzioni;
  con auto-lettura accesa è grigia/disattivata (legge già tutto da
  sé). NESSUNA icona salva: il salvataggio della narrazione è sempre
  automatico (deciso 17/07/2026, vedi STATO.md §Blocco 3). Riferimento
  v1 (`ChatComponents.kt`, MessageBubble): copia sempre, traduci solo
  sui messaggi del narratore con spinner durante la traduzione, leggi
  sempre — la regola dello spinner si eredita sul toggle
  originale/tradotto durante lo streaming.
- **Le decisioni del giocatore entrano nel flusso** come righe
  distinte (corsivo, rientrate, con icona della scelta): il flusso è
  la vista live del diario-grafo — stessa struttura dati.
- Durante la generazione: indicatore "il narratore scrive…" discreto.

### Card di stato (in basso, sopra le scelte)
Ritratto, nome, grado Kai (dorato), CS / Resistenza / Corone con
icone. **Il tocco apre la Scheda personaggio** (tab Stats). Le icone
discipline vivono nella scheda, non nella card.

### Zona scelte
- Pulsanti pieni per le scelte normali (choiceText tradotto, con
  fallback all'originale se il parsing pipe perde la riga).
- Pulsanti distinti (colore + icona della disciplina) per le
  **scelte-disciplina**, visibili solo se `canUseDiscipline` è vero.
- Scelte con `requiredItem`/`requiredFlag`/`minRoll` non soddisfatti:
  non mostrate (default; alternative per-scena in futuro).

---

## Il Dado del Destino

**Oggetto di scena speciale, con animazioni** — non un bottone: il
momento rituale del libro-game. Overlay modale sopra la scena: appare
quando serve un tiro del giocatore, si tocca, il dado rotola
(animazione), il risultato si mostra con enfasi, l'overlay si
dissolve e l'esito si applica.

Appare per (specifiche 2/3): tiro stat in creazione, `skillCheck`,
`randomChoiceTable`, ogni round del combattimento completo, il round
di danno dell'evasione. NON appare per i tiri del motore
(`randomQuantity`, `rollOnItemTable`, combattimento rapido): lì il
risultato emerge nel testo.

---

## Il combattimento nella scena

Nessuna schermata separata: **la zona scelte si trasforma**.

1. La scena ha il blocco `combat` → la zona scelte mostra prima le
   eventuali scelte-disciplina di evasione (fuga gratis), poi la
   scelta di modalità: **Rapido / Completo**.
2. **Rapido**: il motore risolve tutto; appare il riepilogo (esito,
   round, danni totali) e la transizione.
3. **Completo**: la zona diventa il quadro di combattimento — nome
   nemico (tradotto, riga ENEMY), **barre Resistenza** tua e del
   nemico, Rapporto di Forza visibile; Dado del Destino al tocco per
   ogni round; tra i round il **menu tattico**: continua / usa
   oggetto (solo `combatUsable`) / usa disciplina (MINDBLAST —
   disabilitata con motivo se il nemico è immune) / fuggi (se
   `evadeSceneId` esiste e il round supera `evadeAfterRound`).
4. Esito (WIN/LOSE/EVADE) → transizione, la zona torna alle scelte.

---

## Inventario operativo (Scheda personaggio, tab Equipaggiamento)

Non solo consultazione — **azioni sugli oggetti**:

- **Equipaggia / disequipaggia** armi (max 2 portate, una impugnata:
  `equippedWeapon`); il bonus WEAPONSKILL si riflette subito nelle
  stat effettive mostrate.
- **Consuma / usa** oggetti con effetto dichiarato: la pozione
  (HEAL:n) cura; il **Pasto si consuma** dal medesimo gesto — è lo
  stesso flusso con cui si soddisfa `requireAction EAT_MEAL` (chi ha
  HUNTING è auto-esente).
- Zaino con gli **8 posti disegnati, anche i vuoti** (molto
  libro-game); armi (2 slot), oggetti speciali, borsa Corone (max 50).

---

## Convenzioni trasversali

- Il campo `gender` del Character ha tre clienti: ritratto
  (lupo/lupa), **voce TTS per genere**, placeholder `{player_gender}`
  nel prompt Gemma (accordi grammaticali in italiano).
- Ritratti e background dinamici: scelti alla creazione / dichiarati
  dalla scena.
- Testi di feedback di combattimento e UI: `strings.xml`
  (localizzazione classica), mai contenuto del libro.
- Il gioco non si blocca mai: ogni fallimento di parsing/generazione
  degrada su testo originale del pacchetto.

---

## Code generate da questa specifica

- [ ] Asset: ritratto narratore, ritratti lupo/lupa, dado animato
  (sprite o Lottie — decidere in specifica 6/piano), icone discipline
  (inventario asset v1: molte icone vivono sul branch develop, non su
  master — recuperare da lì).
- [ ] Prompt: aggiungere `{player_gender}` ai placeholder disponibili
  (constraintText o closingText).
- [ ] Specifica 6 (criticità): costo del toggle originale/tradotto in
  memoria (banale, verificare); streaming Compose con testo lungo;
  animazione dado su Razr.
- [ ] Manifest/schema: nuovo campo opzionale `locationName` sulla
  scena (stringa, es. "Porto di Ragadorn"), **appiccicoso**: se
  assente, la scena eredita il luogo della precedente — l'autore lo
  scrive solo quando il luogo cambia. Aggiungerlo a
  scenes.sample.json (almeno 2-3 luoghi per testare la mappa) e al
  futuro validatore (nessun vincolo: opzionale puro).
- [ ] STATO.md / JourneyEntry: la voce del diario deve portare con sé
  il locationName risolto (ereditato) al momento della visita, così
  la mappa non dipende dal ricalcolo dell'ereditarietà.
