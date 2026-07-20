# Proposte di upgrade dell'app

**Cosa NON è questo documento**: non è un piano e non è un impegno.
Niente di quanto sta qui è schedulato in `doc/PIANO-SVILUPPO.md`; niente
di qui si implementa senza una decisione esplicita di Michele.

**Cosa è**: il posto dove le idee emerse durante lo sviluppo vengono
scritte per bene invece di essere perse o, peggio, infilate di soppiatto
in una fase in corso. Ogni voce dice **cosa**, **perché**, **cosa
costerebbe** e soprattutto **cosa va verificato prima**.

Distinzione utile: alcune voci sono già **predisposte** nel design
chiuso (i contratti reggono, manca l'implementazione), altre sono
**feature nuove** che allargherebbero il perimetro.

---

## 1. Audio narrativo: ambiente di scena ed effetti puntuali

**Origine**: idea di Michele (17/07/2026), discussa e progettata durante
la Fase 4. **Rinviata da Michele**: prima serve misurare Gemma sul
device (vedi §Rischio).

Sono **due feature distinte** che convivono:

### 1a. Ambience di scena (tappeto sonoro)
Un campo opzionale `ambience` sulla scena (simmetrico a
`backgroundImage`), es. `tavern_crowd`, `rain_storm`, `dungeon_drip`.
Loop di sottofondo per tutta la scena.

- **Chi lo genera**: il **rifinitore LLM dell'ETL** (Fase 6), che già
  produce `locationName` e `toneHints`. Costo runtime ZERO, risultato
  deterministico tra le rigiocate, e passa dalla review
  human-in-the-loop già prevista.
- **Impatto**: un campo opzionale su `Scene` (default null, nessun
  salvataggio rotto) + il player audio.

### 1b. Effetti puntuali inline (il pezzo che solo l'LLM può fare)
Marcatori dentro la prosa generata: la porta cigola *in quel punto del
testo*. Es. `[[sound:door_creak]]`, e col tono comico la stessa porta
può diventare `[[sound:crowd_laugh]]`.

- **Chi li genera**: **Gemma a runtime**, ed è l'unico modo possibile —
  non perché l'ETL non conosca il tono (`toneHints` ce l'ha), ma perché
  **la prosa su cui ancorarli non esiste finché il modello non la
  scrive**. Gemma riscrive la scena ogni volta con parole e ordine
  suoi: solo lei sa dove, nella *sua* versione, la porta cigola.
- È anche l'argomento di Michele a favore: l'inferenza la stai già
  pagando in latenza, tanto vale estrarne più valore.

### Regole di formato (decise, se e quando si farà)
1. **Delimitatore `[[sound:id]]` doppio.** Le parentesi SINGOLE sono già
   usate dal prompt come marcatori di sezione (`[THE STORY SO FAR]`,
   `[CURRENT SCENE …]`): chiedere `[sound:x]` mentre si mostrano
   `[SEZIONE]` invita il modello a confondere i due livelli.
2. **Mai il carattere `|`.** La regola 4 del prompt vieta le pipe nella
   prosa proprio per proteggere il parser delle scelte: i tag audio non
   devono romperla.
3. **Vocabolario CHIUSO**: il modello sceglie tra ID che esistono
   davvero, come per le discipline. ID sconosciuto = **silenzio**, mai
   un errore (il gioco non si blocca mai).
4. **Parsimonia esplicita**: al massimo 2 effetti per scena, solo quando
   l'effetto è ovvio. Meglio zero suoni che una prosa peggiore.
5. **Nel diario si salva il testo GIÀ RIPULITO.** I marcatori sono
   istruzioni di riproduzione effimere, non parte della storia:
   `JourneyEntry.enrichedText` non deve contenerli, altrimenti
   spuntano nel Racconto e nell'export Markdown.

### Rischio che ha motivato il rinvio (parole di Michele)
Istruire Gemma su **tutto** il vocabolario possibile è istruzione che
compete con il compito principale (tradurre e arricchire bene). Gemma 3
4B è piccola e in v1 già sbagliava i formati. **Non si decide prima di
aver misurato**: la milestone della Fase 4 produce i numeri (primo
token, token/s, prompt token, termico). Con quelli in mano si sa quanto
output extra ci si può permettere; senza, è una scommessa.

**Aggiornamento 20/07/2026**: primo token (1,43-1,88 s su GPU) e
velocità (12,1 token/s stabile) sono misurati — vedi DIARIO.md. Restano
**termico su 30-45'** e **drain della batteria**: Michele ha scelto
esplicitamente di aspettare quei due prima di riconsiderare la musica
("meglio saperlo prima di investirci lavoro"). La condizione del rinvio
resta quindi ANCORA APERTA, solo più vicina alla chiusura.

### Costo stimato se approvata (tracciato sul codice reale)
Additivo quasi ovunque: frammento in `config.json` + `PromptFragments` +
sezione in `PromptBuilder` (~15 righe totali, stesso schema di
`enemyFormatText`); 1 campo su `Scene`. L'**unica modifica vera** è in
`ResponseParser` (~30 righe + test): `narrativeOf()` e `parse()` devono
estrarre i marcatori e restituire la prosa ripulita. Servono poi i file
audio e un player (roba di Fase 7).

---

## 2. Altre proposte raccolte

### Già PREDISPOSTE nel design chiuso (i contratti reggono)
- **Effetti oggetto oltre `HEAL:n`**: il formato è dichiarativo ed
  estensibile senza cambiare schema (STATO.md §4.2). Già usato per
  `ENDURANCE:n` (Elmo/Gilet).
- **`requiredRank` sulle scelte**: gradi Kai oggi puramente cosmetici;
  se un libro volesse "serve grado X", si affiancherebbe a
  `requiredItem`/`requiredFlag` (REGOLE.md Blocco 3).
- **MINDSHIELD contro nemici psichici**: predisposizione concettuale,
  nessun campo riservato oggi (REGOLE.md §4.5).
- **Slot multipli di salvataggio**: `SessionStore` salva un file per
  pacchetto; slot multipli cambierebbero solo il nome file
  (STATO.md §1.2).
- **Compagni di viaggio**: il ruolo `COMPANION` esiste già nel
  `Character` unico e la funzione di round è simmetrica; servirebbe la
  seconda inferenza per i commenti, posticipata per design (UI.md).
- **Altri regolamenti oltre Lupo Solitario**: CRT dentro le regole e non
  nel motore, effetti dichiarativi, difficoltà esterna alle regole —
  adattarlo toccherebbe le implementazioni, non i contratti
  (STATO.md §Estensibilità).

### FEATURE NUOVE (allargano il perimetro)
- **Scambio a inventario pieno**: oggi l'oggetto oltre soglia non entra
  in silenzio (STATO.md §4.1). v1 aveva un `InventoryFullDialog` che
  chiedeva cosa scartare: pattern già analizzato e riusabile.
- **Tono narrativo scelto dall'utente**: v1 aveva in Opzioni un menu
  (originale/horror/epico/…) iniettato nel prompt. In Ex i toni sono
  dell'autore via `toneHints`: darlo all'utente è una scelta di design
  diversa, non una svista.
- **Mappa logica del diario più ricca**: oggi v0.1 mostra i soli nomi
  dei luoghi. Annotabile in futuro con combattimenti (già derivabili
  dalle Transition WIN/LOSE), NPC importanti e oggetti trovati (UI.md).
- **Scudo come oggetto iniziale**: in v1 era solo un valore dell'enum
  `ItemType`, senza nessun oggetto reale dietro. Se lo si vuole serve
  che Michele decida il bonus.
