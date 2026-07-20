# Specifica 3 — Stato e salvataggio

Stato: **CHIUSA** (sessione 16/07/2026, notte).

Base di partenza: `GameStateManager`/`SessionData` di v1. Difetti rilevati
e corretti: il DM salvato come personaggio (retaggio chatbot),
`currentSceneId` assente (ricostruito per vie traverse; il path del libro
stava dentro `GameCharacter`), stato spezzato (flag in `HeroDetails`,
variabili in `SessionData`), `globalVariables: Map<String, Any>` (trappola
Gson: i numeri tornano Double), discipline salvate come nomi display
("Sixth Sense") invece che ID canonici, singleton con Context.

---

## Blocco 1 — La sessione

### 1.1 Contenuto

```
SessionData:
  saveFormatVersion: Int          // per migrazioni future
  packageId: String               // il libro caricato
  packageVersion: String
  difficulty: NORMAL | HARD | IRON
  currentSceneId: String          // ESPLICITO
  characters: List<Character>     // eroe + eventuali compagni
  journey: List<JourneyEntry>     // diario-grafo (Blocco 3)
  flags: Map<String, String>      // stato autore, unificato qui
  variables: Map<String, Int>     // tipizzato: niente Any
  checkpointsUsed: Int            // budget speso (Blocco 2)
  lastUpdate: Long
```

- Il DM **non è un personaggio**: non esiste nella sessione.
- Il nemico di combattimento **non si salva**: è transiente, idratato
  dal blocco combat della scena, vive e muore lì.
- I personaggi sono `Character` unico (specifica 2 §1.5): role, name,
  stats base, kaiDisciplines (**ID canonici UPPER_SNAKE** — mai nomi
  display), weaponSkillType (incluso UNARMED), inventory,
  equippedWeapon, activeModifiers (`StatModifier` con
  sourceType/duration da v1), gameFlags personali NON esistono più
  (tutto in `flags` di sessione).
- **Si serializzano i fatti, i bonus si calcolano**: CS/Resistenza
  effettive non sono mai nel file; le calcola una sola funzione
  dell'engine (base + modificatori attivi + WEAPONSKILL(spec,
  equippedWeapon) + MINDBLAST di combattimento).

### 1.2 Formato e collocazione

- **kotlinx.serialization** (non Gson): Kotlin nativo, tipi espliciti,
  zero Android — coerente con "engine testabile da terminale".
- JSON su file in app-storage: `session_<packageId>.json` — un
  auto-save per pacchetto. Slot multipli: estensione futura
  compatibile (cambia solo il nome file).
- Lettura/scrittura nel modulo `data` dietro porta iniettabile
  (pattern `PackageSource`): i test scrivono su file temporanei.

### 1.3 Quando si salva

**Auto-save a ogni transizione di scena**, dopo l'esecuzione di
gameMechanics + globalRules (lo stato è consistente per costruzione).
Vale per tutte le difficoltà: serve a riprendere l'app dov'eri.

**Il combattimento è atomico**: non si salva a metà. Se l'app muore
durante un combat, si riprende all'ingresso della scena con lo stato
pre-combattimento. (Serializzare round e nemico a metà: beneficio
minimo, complessità sproporzionata.)

---

## Blocco 2 — Difficoltà e checkpoint

La difficoltà è una **meta-regola sul salvataggio**, non
un'inflazione di statistiche: stesso libro, stessi nemici, cambia
quanto la storia perdona. Si sceglie a inizio avventura ed è
**immutabile** per quella partita.

| Difficoltà | Vite (checkpoint) | Alla morte |
|---|---|---|
| NORMALE | 2 | ricarichi un checkpoint **e lo consumi**; finiti → **sessione cancellata** |
| DIFFICILE | 1 | ricarichi il checkpoint **e lo consumi**; finito → **sessione cancellata** |
| IRON | 0 | **sessione cancellata** subito, libro da capo |

### Regole dei checkpoint

- Li piazza **il giocatore**, dal menu, quando vuole — budget per
  avventura secondo la tabella.
- Un checkpoint è la **fotografia completa della SessionData** in
  quel momento, su file separato:
  `checkpoint_<packageId>_<n>.json`. (Solo l'ID non basta: si
  porterebbe indietro inventario e flag del futuro.)
- **Scritto una volta, mai spostabile né sovrascrivibile.**
- **RICARICARLO LO CONSUMA** (rivisto 20/07/2026, decisione di Michele):
  ogni ritorno a un checkpoint lo **brucia** — il file sparisce e quel
  punto non è più raggiungibile. Su NORMALE hai 2 vite, su DIFFICILE 1,
  su IRON nessuna.

  > La versione precedente diceva "si può ricaricare illimitatamente" e
  > affidava la durezza alla sola irrevocabilità del piazzamento. Alla
  > prova dei fatti bastavano due checkpoint per rendere l'avventura
  > innocua: si moriva quante volte si voleva tornando sempre allo
  > stesso punto. Ora le vite sono davvero finite.

- **Esaurite le vite, la morte è definitiva**: la sessione si cancella e
  il libro riparte da capo, esattamente come in IRON. IRON resta il caso
  limite della stessa regola (zero vite dall'inizio), non un'eccezione a
  parte.
- Il ricaricamento ripristina la fotografia e **tronca il diario** a
  quel punto (coerenza del percorso).
- L'auto-save di ripresa (Blocco 1.3) esiste sempre, anche in IRON:
  in IRON è l'unico accompagnamento — riapri l'app e sei all'ultimo
  capitolo visitato, ma se muori si cancella tutto.

---

## Blocco 3 — Il diario-grafo

Ogni voce del diario è **un passo del viaggio**:

```
JourneyEntry:
  sceneId: String
  enrichedText: String       // il testo che Gemma ha generato
  transition: Transition     // come te ne sei andato
```

`Transition`: la scelta toccata | la disciplina usata | l'esito del
combattimento (WIN/LOSE/EVADE) | il salto d'ufficio (globalRule,
checkItemAndJump, ifStat). La sequenza ordinata delle voci è il
**percorso completo nel grafo del libro**: non solo dove sei stato,
ma per quale porta sei uscito.

- **Il testo generato da Gemma si salva, non si rigenera.** Tre
  ragioni: la rigenerazione costa decine di secondi di inferenza sul
  device a ogni ricaricamento; è non-deterministica (riapri e "il
  tuo" testo è diverso — rompe la continuità); e `previous_scene_text`
  del prompt successivo dev'essere ciò che il giocatore ha letto, non
  una variante.
- Si tiene **tutto** il diario (il testo pesa poco).
- `visitedScenes` come lista non esiste: è **derivabile** dal diario
  ("si salvano fatti, non conclusioni").
- Usi futuri già intravisti: rilettura del viaggio in UI, mappa del
  percorso, statistiche di fine libro, replay, debug dell'ETL.

### Salvataggio della narrazione: sempre automatico (nessuna opzione)

Nota del 17/07/2026: valutata e SCARTATA nella stessa giornata
un'opzione automatico/manuale per la persistenza dell'`enrichedText`
(icona salva per-blocco). Decisione finale di Michele: **si salva
sempre tutto, automaticamente** — è il comportamento già descritto
sopra in questo blocco, che resta invariato. Nessuna icona salva nei
blocchi del narratore (UI.md §Flusso centrale); l'auto-save della
SessionData (Blocco 1.3) era e resta automatico e atomico.

---

## Blocco 4 — Inventario e oggetti

### 4.1 Categorie e limiti (canone Lupo Solitario)

| Tipo | Limite |
|---|---|
| WEAPON | 2 |
| BACKPACK_ITEM | 8 posti |
| SPECIAL_ITEM | illimitati |
| GOLD | 50 Corone |

I limiti li fa rispettare il motore: `addItem` oltre il limite →
l'oggetto **non entra, senza errore** (v0.1; in futuro la UI potrà
offrire "scarta qualcosa"). Solito principio: il gioco non si blocca
mai.

### 4.2 Formato oggetto

```
GameItem:
  name: String
  type: WEAPON | BACKPACK_ITEM | SPECIAL_ITEM | GOLD
  quantity: Int
  combatUsable: Boolean = false
  effect: String? = null       // dichiarativo, es. "HEAL:4"
```

In v0.1 l'unico effetto implementato è `HEAL:n` (Pozione di Laumspur:
HEAL:4). Il formato è estensibile senza cambiare schema. Nel menu
tattico del combat compaiono solo i `combatUsable`.

### 4.3 Armi ed equipaggiamento

- `equippedWeapon` punta a una delle (max 2) armi portate;
  equip/unequip dalla schermata inventario.
- `WeaponType` enum canonico: SWORD, AXE, DAGGER, SPEAR, MACE,
  WARHAMMER, QUARTERSTAFF, BROADSWORD, SHORT_SWORD, BOW.
- `UNARMED` esiste solo come **specializzazione WEAPONSKILL**, non
  come arma: il bonus scatta combattendo senza armi.

### 4.4 HUNTING e i Pasti (regola canonica a costo zero)

`requireAction` con `action="EAT_MEAL"` è **auto-soddisfatto se il
personaggio possiede HUNTING**: chi ha la Caccia non consuma Pasti
quando il testo impone di mangiare. Una riga di logica, e HUNTING
acquista il suo effetto meccanico canonico.

---

## Estensibilità (nota di indirizzo)

Il sistema è volutamente adattabile ad altri regolamenti in futuro:
`RulesEngine` è un'interfaccia (la CRT è dentro `LoneWolfRules`, non
nel motore), gli effetti oggetto sono dichiarativi, le globalRules
sono condizioni generiche, la difficoltà è una meta-regola esterna
alle regole di gioco. Adattare il sistema "ad altre cose" tocca le
implementazioni, non i contratti.

---

## Code generate da questa specifica (da fare altrove)

- [ ] Specifica UI: scelta difficoltà nel setup avventura (con
  spiegazione onesta di IRON); menu checkpoint (piazza/ricarica,
  budget visibile); schermata inventario con equip/unequip;
  schermata diario/rilettura del viaggio.
- [ ] Validatore config/scene: `effect` degli oggetti nel formato
  `VERBO:valore` noto; `EAT_MEAL` documentato tra le azioni di
  requireAction.
- [ ] Specifica 6 (criticità): dimensione del diario su libri lunghi
  (stima: trascurabile, verificare); tempo di scrittura auto-save a
  ogni transizione (atteso irrilevante, misurare).
- [ ] Manifest: nessun campo nuovo richiesto da questa specifica
  (difficoltà è scelta del giocatore, non del libro).
