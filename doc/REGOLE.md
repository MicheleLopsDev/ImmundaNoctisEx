# Specifica 2 — Regole di gioco

Stato: **CHIUSA** (sessioni 16/07/2026, pomeriggio e sera).

Materiale di partenza: `doc/MATERIALE-REGOLE-V1.md` (estrazione da v1:
tabella CRT, algoritmo round, scoperta che l'orchestrazione combat di v1
era interamente commentata — il CombatManager di Ex nasce da zero).

---

## Blocco 1 — Combattimento (CHIUSO)

### 1.1 Due modalità, scelta del giocatore a inizio combattimento

- **Rapido**: un tocco, il motore itera i round internamente fino
  all'esito (stesso `DiceRoller`, stessa CRT del completo) e presenta il
  riepilogo: esito, round giocati, danni totali subiti/inflitti.
  Nessuna decisione intermedia: si combatte fino in fondo.
- **Completo**: round per round. Il giocatore tocca il Dado del Destino,
  vede il tiro e i danni del round; tra un round e l'altro il menu
  tattico: continua / usa oggetto / usa disciplina / fuggi (se
  disponibile).

Regola di confine: **evasione, oggetti e discipline esistono solo nel
completo**. Scegliere il rapido significa "vado fino in fondo": è la
modalità per i combattimenti dove non servono tattiche.

Nota architettura: il motore espone UNA sola operazione di round
(funzione pura); il rapido è un loop sopra di essa. Nessuna logica
duplicata.

### 1.2 Round di combattimento (algoritmo, da v1 con correzioni)

1. Combattività Effettiva di ciascun contendente = base + somma dei
   modificatori attivi su COMBATTIVITA (pattern `StatModifier`).
2. Rapporto di Forza = CS giocatore − CS nemico, coerceIn [-10, +10].
3. Tiro 0-9 via `DiceRoller` iniettato (MAI Random inline).
4. Lookup su `COMBAT_RESULTS_CHART` (riuso integrale da
   `LoneWolfRules` di v1). Attenzione al mapping: il tiro '0' della
   tabella ufficiale è l'ultimo indice della lista
   (`tableRollIndex = if (roll == 0) 9 else roll - 1`) — fixture di
   test dedicata a questo off-by-one.
5. `KILL_DAMAGE = 999` come sentinella di uccisione istantanea.
6. Risultato del round = **dati puri**: danni giocatore, danni nemico,
   tiro, rapporto. Il testo lo compone la UI. Niente `LocalizedText`.

### 1.3 Evasione

- Campo opzionale `evadeAfterRound` nel blocco combat della scena:
  la fuga si sblocca DOPO quel round (default 0 = subito disponibile).
  L'opzione appare solo se `evadeSceneId` esiste.
- **Costo della fuga (regola canonica Lupo Solitario)**: al momento
  della fuga si risolve un ultimo round in cui SOLO il giocatore
  subisce danni (si tira sulla CRT, si applica la sola colonna
  giocatore). Poi transizione a `evadeSceneId`.
- **Fuga via disciplina** (`disciplineChoices` della scena, es.
  CAMOUFLAGE): GRATIS, nessun danno — è il premio per possedere la
  disciplina giusta. Disponibile prima che il combattimento inizi
  (scelta di scena), non nel menu tattico.

### 1.4 Morte e priorità degli esiti

- Resistenza nemico ≤ 0 → transizione a `winSceneId`.
- Resistenza giocatore ≤ 0 in combattimento → transizione a
  `loseSceneId` della scena. **Lo specifico batte il globale**: il
  `deathSceneId` del manifest è il fallback se `loseSceneId` manca.
  (Motivo: l'autore che ha scritto "se perdi ti risvegli in cella"
  deve poterlo fare; la morte generica è la rete di sicurezza.)
- Fuori dal combattimento: Resistenza ≤ 0 → `deathSceneId` globale
  (vedi Blocco 2).

### 1.5 Il nemico nel JSON di scena (minimale)

```json
"combat": {
  "enemyName": "Warehouse Thug",
  "enemyCombatSkill": 14,
  "enemyEndurance": 22,
  "immuneToMindblast": false,
  "evadeAfterRound": 2,
  "winSceneId": "6",
  "loseSceneId": "7",
  "evadeSceneId": "5"
}
```

- **Authoring minimale, runtime uniforme**: nel JSON l'autore scrive
  solo nome + due statistiche + destinazioni; il motore idrata il
  blocco in un **`Character` unico** — lo stesso tipo per eroe,
  compagno, nemico, futuro multiplayer locale — con
  `role: HERO | COMPANION | ENEMY | NPC` (enum al posto dell'id
  magico `"hero"` di v1), campi mancanti a default. Eredita l'idea
  del `GameCharacter` + `CharacterType` di v1, ripulita dai campi di
  presentazione/persistenza che violavano la separazione (niente
  `@DrawableRes` nel modello: il ritratto è un id/path risolto dalla
  UI; niente campi di feature morte).
- Premio della simmetria: la funzione di round è
  `resolveRound(a: Character, b: Character)` — un nemico con
  MINDBLAST proprio (che rende utile MINDSHIELD del giocatore) o un
  duello tra due eroi non costano codice al motore.
- `immuneToMindblast`, `evadeAfterRound`, `evadeSceneId` opzionali
  (default: false / 0 / assente); `loseSceneId` opzionale (fallback
  `deathSceneId`); `winSceneId` obbligatorio.
- **`enemyName` viene tradotto da Gemma** nel giro normale della
  scena: nuova riga nel formato pipe `ENEMY|testo tradotto`, con
  fallback al nome originale del pacchetto se il parsing la perde
  (stessa filosofia di CHOICE/DISCIPLINE: il parsing fallito non
  blocca mai il gioco). Tag `enemy_line` nel config.

---

## Blocco 2 — Regole globali di esito (CHIUSO)

### 2.1 Morte built-in

La regola "Resistenza giocatore ≤ 0 → salta a `deathSceneId`" è
integrata nel motore. L'autore la attiva dichiarando `deathSceneId`
nel manifest; non si scrive come regola.

### 2.2 `globalRules` nel manifest

Lista opzionale di regole condizione → destinazione, definite
dall'autore. Meccanismo generico di "salto globale su condizione":
la vittoria dell'avventura è un caso, ma anche esiti intermedi
("se il sospetto sale troppo, le guardie ti arrestano") lo sono.
`victorySceneId` come campo dedicato NON esiste: la vittoria è una
globalRule come le altre.

### 2.2-bis L'esito dichiarato e la garanzia del finale (20/07/2026)

Aggiunto dopo una prova sul campo: Michele ha finito il libro di esempio
ed è tornato al menu **senza sapere se aveva vinto o perso**. Un'avventura
deve sempre dichiarare com'è andata.

- **`Scene.outcome`** (`VICTORY` | `DEFEAT` | `NEUTRAL`), solo sulle
  scene `ENDING`. **Lo dichiara l'autore, il motore non lo indovina**:
  un finale amaro raggiunto vivi e una vittoria si somigliano troppo
  perché si possa dedurli dallo stato del gioco. Assente = `NEUTRAL`
  (si dice che è finita, non si mente sull'esito).
- **La morte built-in batte la dichiarazione**: la scena `deathSceneId`
  è `DEFEAT` anche se l'autore ha scritto altro. Ci si arriva morendo.
- **Una scena di finale esiste SEMPRE**
  (`AdventureEnding.withGuaranteedEnding`): se `deathSceneId` manca o
  punta a una scena inesistente, il motore ne **fabbrica** una e la
  aggiunge al grafo. Prima di questo, un libro senza `deathSceneId`
  lasciava giocare con Resistenza ≤ 0 e una sconfitta in combattimento
  senza destinazione lasciava il giocatore fermo sul posto.
- **Il finale fabbricato nasce senza testo**: lo scrive il narratore
  (frammento `syntheticEndingText`). Se il modello non c'è o fallisce,
  la UI mette il testo fisso di `strings.xml` — il gioco non si blocca
  mai vale anche qui.

La regola vive in `:core:engine` (`AdventureEnding`), con test JVM.

```json
"deathSceneId": "7",
"globalRules": [
  { "type": "FLAG", "name": "traditore_smascherato",
    "operator": "==", "value": "true", "targetSceneId": "99" },
  { "type": "VAR", "name": "sospetto",
    "operator": ">=", "value": "10", "targetSceneId": "66" }
]
```

`type`: FLAG | VAR (estendibile). Operatori: ==, !=, >=, <=, >, <.

### 2.3 Quando si valutano

A ogni transizione di scena, **dopo** l'esecuzione dei `gameMechanics`
della scena di arrivo (un setFlag/statMod nei mechanics deve poter far
scattare la regola nella stessa transizione). Sequenza:

transizione → esegui gameMechanics → valuta morte built-in →
valuta globalRules in ordine → se una scatta, salta alla sua
destinazione (e sul nuovo arrivo si ripete il giro).

Il combattimento resta fuori: lì gli esiti sono gestiti dal Blocco 1
(specifico batte globale).

### 2.4 Priorità

- La morte built-in si valuta PRIMA di tutte le globalRules
  (morire batte vincere).
- Tra le globalRules: **prima regola che matcha vince**, nell'ordine
  di scrittura nel manifest.
- Raccomandazione da validatore (warning, non errore): le destinazioni
  delle globalRules dovrebbero essere scene ENDING.

---

## Blocco 3 — Gradi Kai (CHIUSO)

Puramente **cosmetici**, come nei libri canonici: titolo calcolato dal
numero di discipline possedute. Soglie da v1:

| Discipline | Grado |
|---|---|
| 0-4 | Novizio Kai |
| 5 | Iniziato Kai |
| 6 | Discepolo Kai |
| 7 | Viandante Kai |
| 8 | Guerriero Kai |
| 9 | Maestro Kai |
| 10 | Gran Maestro Kai |
| >10 | Gran Maestro Kai Supremo |

- `enum KaiRank` con soglie nell'engine; **nomi in `strings.xml`**
  (in v1 i nomi italiani erano hardcoded nell'engine — corretto).
- Nessun effetto meccanico. Predisposizione concettuale (zero codice
  oggi): se un libro futuro vorrà "serve grado X per questa scelta",
  si aggiungerà `requiredRank` accanto a `requiredItem`/`requiredFlag`.

---

## Blocco 4 — Discipline e oggetti in combattimento (CHIUSO)

### 4.1 MINDBLAST
+2 Combattività per tutto il combattimento. Si attiva dal menu tattico
(modalità completa), una volta, resta attiva fino a fine combattimento.
Il nemico può essere immune: `immuneToMindblast: true` nel blocco
combat (non-morti e certe creature di Magnamund). Se immune, l'opzione
non compare (o compare disabilitata con motivo — dettaglio da
specifica UI).

### 4.2 WEAPONSKILL (con specializzazione, incluso UNARMED)
Alla creazione del personaggio, chi sceglie WEAPONSKILL sceglie anche
una **specializzazione**: un tipo d'arma OPPURE `UNARMED` (arti
marziali). Il +2 CS scatta quando la condizione è vera:
- specializzazione arma: impugni un'arma di quel tipo;
- specializzazione UNARMED: combatti **senza** armi.

Il check "che arma stai impugnando" per il caso armato dipende dalla
specifica inventario/equipaggiamento (Specifica 3) — la regola è
definita, il collegamento dati arriva lì. Il caso UNARMED è già
completo (nessuna arma equipaggiata = bonus).
**Coda per la specifica UI**: la creazione del personaggio deve
includere la scelta della specializzazione WEAPONSKILL.

### 4.3 HEALING
Passiva: +1 Resistenza a ogni transizione verso una scena **senza**
combattimento, fino al massimo del personaggio. Non è un'azione: la
applica il motore. In combattimento non fa nulla.

### 4.4 Oggetti
Flag `combatUsable` sull'oggetto + effetto dichiarato (es. pozione =
heal). Nel menu tattico compaiono solo i `combatUsable`. Formato
esatto degli oggetti → Specifica 3 (inventario); qui si fissa il
principio.

### 4.5 Le altre discipline
SIXTH_SENSE, TRACKING, HUNTING, CAMOUFLAGE, MINDSHIELD,
ANIMAL_KINSHIP, MIND_OVER_MATTER: nessun effetto nel combattimento.
Agiscono a livello scena (`disciplineChoices`), già coperto dal
design. (MINDSHIELD: predisposizione concettuale per nemici con
attacco psichico — non specificato oggi, nessun campo riservato.)

---

## Blocco 5 — Comandi TO_IMPLEMENT (CHIUSO)

### 5.1 `removeItem`
Rimuove N unità dell'oggetto. Se il giocatore ne possiede meno di N,
rimuove quel che c'è **senza errore** (il gioco non si blocca mai).
Se l'assenza dell'oggetto deve avere conseguenze narrative, l'autore
usa `checkItemAndJump` prima.

### 5.2 `checkItemAndJump`
Valutato all'ingresso in scena insieme agli altri `gameMechanics`,
nell'ordine di scrittura (come `ifStat`): condizione sul possesso →
salto immediato a `nextSceneId_TRUE` / `nextSceneId_FALSE`.
Caso d'uso: "Se possiedi la Lanterna vai al 112, altrimenti al 87".

### 5.3 `rollOnItemTable`
Tiro 0-9 via `DiceRoller`; `outcomes` mappa **intervalli espliciti di
tiro → oggetto** (es. 0-4: niente, 5-7: Pugnale, 8-9: Corona d'oro).
Intervalli espliciti, non probabilità implicite: l'autore vede
esattamente cosa esce con quale tiro, come nei libri veri. Il
validatore verifica che gli intervalli coprano 0-9 senza
sovrapposizioni.

---

## Blocco 6 — Dado del Destino fuori dal combattimento (CHIUSO)

Criterio narrativo: **se il tiro decide il destino, tira il
giocatore; se decide una quantità, tira il motore.**

- **Tira il giocatore** (il gioco si ferma, appare il Dado del
  Destino, teatro): `skillCheck`, `randomChoiceTable` — tiri che
  determinano una biforcazione.
- **Tira il motore** (in silenzio, il risultato appare nel testo):
  `randomQuantity`, `rollOnItemTable` — tiri che determinano una
  quantità o un ritrovamento ("trovi 7 Corone d'oro").

Stesso `DiceRoller` sotto in entrambi i casi: cambia solo chi preme
il grilletto.

---

## Code generate da questa specifica (da fare altrove)

- [ ] `config.json`: tag `enemy_line` (`^ENEMY\|(.+)$`, comando
  `updateEnemyName`); rimuovere lo status TO_IMPLEMENT dai 3 tag
  quando implementati.
- [ ] Frammento prompt `outputFormatText`: riga ENEMY quando la scena
  ha blocco combat.
- [ ] `scenes.sample.json`: scena 4 (battle) adotta il blocco combat
  §1.5.
- [ ] Validatore scene: `winSceneId` obbligatorio se `combat`
  presente; destinazioni esistenti nel grafo; intervalli
  `rollOnItemTable` completi e disgiunti; warning se destinazione di
  globalRule non è ENDING.
- [ ] Specifica 3 (stato/inventario): equipaggiamento armi (per
  WEAPONSKILL), formato oggetti con `combatUsable` ed effetto.
  **Decisioni già prese stasera**: (a) `Character` unico a runtime
  per tutti i ruoli, con `role` enum; (b) principio "si serializzano
  i FATTI, i bonus si CALCOLANO": nel salvataggio vivono stats base,
  inventario, `equippedWeapon`, `weaponSkillType`, `StatModifier`
  narrativi (con sourceType/duration da v1) e flag; la CS effettiva
  è UNA funzione dell'engine, mai persistita. Base di riuso:
  `HeroDetails`/`ComputedStats`/`weaponSkillType` di v1 — difetto da
  non ripetere: calcolo duplicato (LoneWolfRules sommava i
  modificatori per conto suo invece di usare ComputedStats).
- [ ] Specifica UI: scelta specializzazione WEAPONSKILL alla
  creazione; presentazione menu tattico; opzione MINDBLAST
  disabilitata se nemico immune.
