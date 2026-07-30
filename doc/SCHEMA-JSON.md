# Formato JSON dei libri (manifest + scene)

Questo documento spiega come è fatto il file JSON di un libro
ImmundaNoctisEx: tutti gli attributi possibili, cosa sono obbligatori
e cosa opzionali, e degli esempi concreti per ognuno. È lo schema che
userai per scrivere libri a mano o per un futuro editor grafico.

**Fuori dallo scopo di questo documento**: il formato di salvataggio di
una partita in corso (`SessionData`, `Character`, `JourneyEntry` —
vedi `doc/STATO.md`) non è qui. Qui c'è solo il **contenuto del
libro**: quello che un autore scrive, non quello che il motore produce
giocando.

Fonte di verità: le data class Kotlin in
`core/data/src/commonMain/kotlin/.../core/data/model/` (tutte
`@Serializable`, kotlinx.serialization). Non esiste un file
`.schema.json` formale nel repository — questo documento e il codice
sono l'unica fonte, vanno tenuti allineati.

---

## 1. Struttura generale

Un libro è **un solo file JSON**, con questa forma:

```json
{
  "id": "...",
  "version": "...",
  "title": "...",
  "description": "...",
  "language": "en",
  "genre": "FANTASY",
  "toneHints": ["..."],
  "deathSceneId": "...",
  "disciplineChoices": [ ... ],
  "globalRules": [ ... ],
  "scenes": [ ... ]
}
```

Il livello radice è il **manifest**: metadati del libro + il catalogo
delle discipline disponibili + l'elenco di TUTTE le scene. Non ci sono
file separati per scena: tutto vive in `scenes`, un array piatto.

Il file di esempio completo, giocabile, è `content/scenes.sample.json`
(7 scene: intro, due transizioni, un combattimento, una via
alternativa via disciplina, un finale di vittoria, uno di sconfitta).
Altri esempi mirati, ognuno pensato per un solo aspetto dello schema,
sono in `content/test-books/` (citati dove serve nel resto del
documento, elenco completo in §11).

---

## 2. Il manifest (livello radice)

| Campo | Tipo | Obbligatorio | Default | Note |
|---|---|---|---|---|
| `id` | stringa | sì | — | Identificativo del libro (es. `"sample-adventure"`). |
| `version` | stringa | sì | — | Versione del pacchetto (es. `"1.0.0"`), libera. |
| `title` | stringa | sì | — | Titolo mostrato all'utente. |
| `description` | stringa | sì | — | Descrizione breve del libro. |
| `language` | stringa | sì | — | Lingua del testo SORGENTE nel file (es. `"en"`) — è la lingua da cui il narratore traduce, non quella mostrata al giocatore. |
| `genre` | stringa | sì | — | Genere narrativo (es. `"FANTASY"`), passato al narratore per il tono di scrittura. Ogni scena ha il suo `genre`, di norma uguale a questo. |
| `toneHints` | array di stringhe | no | `[]` | Suggerimenti di tono generali del libro (es. `["adventurous"]`). Ogni scena può avere i propri `toneHints` più specifici. |
| `deathSceneId` | stringa o `null` | no | `null` | ID della scena a cui saltare quando la Resistenza del giocatore scende a 0 **fuori da un combattimento**. Se assente o punta a una scena inesistente, il motore ne fabbrica una al volo (vedi §4.3). |
| `disciplineChoices` | array di `DisciplineDescriptor` | no | `[]` | Il catalogo delle discipline Kai proposte in creazione personaggio (vedi §3). |
| `globalRules` | array di `GlobalRule` | no | `[]` | Regole condizione → destinazione valutate a ogni cambio scena (vedi §7). |
| `scenes` | array di `Scene` | no | `[]` | Tutte le scene del libro (vedi §4). In pratica sempre presente e non vuoto. |
| `customResources` | `CustomResources` | no | `{"images":[],"sounds":[]}` | Risorse `url:` registrate dall'autore per QUESTO libro (§15.7, `doc/EDITOR.md`) — comodità per l'editor grafico, il motore di gioco non lo legge mai: una scena salva sempre `"url:https://..."` per intero, questo campo esiste solo per non dover riscrivere lo stesso link più volte durante la scrittura. `images`/`sounds` sono liste di `{"id": "...", "url": "..."}`; un ID duplicato all'interno della stessa lista è un avviso di validazione, non un errore. |

Esempio minimo:

```json
{
  "id": "sample-adventure",
  "version": "1.0.0",
  "title": "The Warehouse Letter",
  "description": "A short fantasy vignette...",
  "language": "en",
  "genre": "FANTASY",
  "toneHints": ["adventurous"],
  "deathSceneId": "7",
  "disciplineChoices": [ ... ],
  "scenes": [ ... ]
}
```

---

## 3. Discipline Kai

### 3.1 `DisciplineDescriptor` (nel manifest)

Una voce del catalogo mostrato in creazione personaggio:

| Campo | Tipo | Obbligatorio |
|---|---|---|
| `id` | stringa | sì — deve essere uno dei 10 ID canonici sotto |
| `name` | stringa | sì — nome mostrato |
| `description` | stringa | sì — testo mostrato in creazione personaggio |

```json
{
  "id": "SIXTH_SENSE",
  "name": "Sixth Sense",
  "description": "A heightened intuition that warns of danger and hidden threats."
}
```

### 3.2 I 10 ID canonici

Solo questi valori sono validi per `id` (qui) e per `disciplineId`
(nelle `disciplineChoices` di scena, §5.2). Un ID diverso viene
rifiutato dal validatore (errore bloccante).

```
WEAPONSKILL, CAMOUFLAGE, HUNTING, SIXTH_SENSE, TRACKING,
HEALING, MINDSHIELD, MINDBLAST, ANIMAL_KINSHIP, MIND_OVER_MATTER
```

(`SHADOWSTEP` dei libri canonici NON esiste in Ex — decisione di
design.) Effetti meccanici di ciascuna: `doc/REGOLE.md` Blocco 4.

---

## 4. Scena (`Scene`)

Campi comuni a ogni scena, indipendentemente dal tipo:

| Campo | Tipo | Obbligatorio | Default | Note |
|---|---|---|---|---|
| `id` | stringa | sì | — | ID univoco nel libro. Convenzione: una stringa numerica progressiva (`"1"`, `"2"`, ...), ma è libera. |
| `sceneType` | enum | sì | — | `START` \| `TRANSITION` \| `ENDING` (vedi §4.1). |
| `genre` | stringa | sì | — | Genere per questa scena (di norma uguale a quello del manifest). |
| `toneHints` | array di stringhe | no | `[]` | Tono specifico di questa scena (es. `["dark", "suspenseful"]`), passato al narratore. |
| `backgroundImage` | stringa o `null` | no | `null` | Riferimento a un'immagine di ambientazione, con prefisso `static:`/`url:` (vedi §4.2). |
| `npcImage` | stringa o `null` | no | `null` | Riferimento a un ritratto NPC, con prefisso `static:`/`url:` (vedi §4.2) — per incontri **non ostili**. Un NPC che poi combatte usa invece `combat.enemyImage` (§6), non questo campo. |
| `locationName` | stringa o `null` | no | `null` | Nome del luogo mostrato in UI. **"Appiccicoso"**: se assente, la scena eredita il `locationName` dell'ultima scena che lo aveva dichiarato lungo il percorso del giocatore. Scrivilo solo quando il luogo CAMBIA davvero. |
| `narrativeText` | stringa | sì | — | Il testo sorgente della scena, nella lingua di `manifest.language`. Il narratore lo riscrive/traduce per il giocatore — questo campo resta il testo originale, invariato. |
| `choices` | array di `Choice` | no | `[]` | Scelte ordinarie (vedi §5.1). |
| `disciplineChoices` | array di `DisciplineChoice` | no | `[]` | Scelte visibili solo a chi ha una certa disciplina (vedi §5.2). |
| `combat` | `Combat` o `null` | no | `null` | Blocco di combattimento, se la scena ne ha uno (vedi §6). |
| `gameMechanics` | array di `GameMechanic` | no | `[]` | Comandi eseguiti all'ingresso in scena, in ordine (vedi §8). |
| `outcome` | enum o `null` | no | `null` | Solo per `sceneType: "ENDING"` — come finisce l'avventura (vedi §4.3). |
| `sfx` | stringa o `null` | no | `null` | Effetto sonoro personalizzato che SOVRASCRIVE quello automatico ricavato dal nome dell'immagine `static:` (vedi §4.4). |

### 4.1 `sceneType`

- `START` — la scena iniziale del libro (di norma solo una).
- `TRANSITION` — una scena normale, con scelte e/o combattimento.
- `ENDING` — chiude l'avventura. Nessuna scelta ha senso qui (`choices`
  vuoto), ma il campo non è vietato a livello di schema.

### 4.2 Immagini: `static:` (catalogo bundle) o `url:` (link esterno)

`backgroundImage`, `npcImage` e `combat.enemyImage` (§6) non sono ID
nudi né testo libero: portano **sempre** uno dei due prefissi
(29/07/2026, vedi `ImageReference.kt` in `core/data`):

- **`static:<id>`** — `<id>` è un ID preso da un **catalogo chiuso già
  incluso nell'app** (`SceneImageCatalog.kt`, `NpcImageCatalog.kt`,
  `EnemyImageCatalog.kt`). L'elenco completo di tutti gli ID esistenti,
  con descrizione, è in **`doc/SUONI-IMMAGINI.md`** (organizzato per
  categoria: luoghi, nemici/bestie, NPC) — non duplicato qui. Un `id`
  che non è nel catalogo **non causa un errore**: semplicemente non
  compare nessuna immagine (degrado silenzioso, il gioco non si blocca
  mai). **Forma raccomandata**: bundle nell'APK, funziona offline,
  nessuna dipendenza esterna.
- **`url:<http/https>`** — link diretto a un file immagine esterno
  (solo `http://`/`https://`, mai `file://` o altri schemi). Pensato
  per libri di uso personale mai distribuiti (`doc/LIBRI/`), dove creare
  arte nuova per ogni scena non è pratico e si vuole invece linkare
  l'illustrazione originale (o una generata) senza doverla impacchettare
  nell'app. Il `PackageValidator` segnala **sempre** un avviso quando
  incontra un `url:`, anche se il link è valido e raggiungibile: è una
  dipendenza di rete che va rivista prima di pubblicare o distribuire il
  libro — un libro con zero avvisi è garantito autosufficiente.

Se l'autore non sa quale scegliere o il libro ne ha bisogno di una
nuova, si può lasciare `null`: niente immagine, solo testo. (Il
narratore IA, quando genera la scena, prova anche da solo a indovinare
un `backgroundImage` dal catalogo `static:` se l'autore non l'ha
scritto — meccanismo separato, non riguarda l'autoring manuale, e non
propone mai un `url:`.)

```json
"backgroundImage": "static:loc_tavern",
"npcImage": "url:https://example.com/mio-npc.png"
```

### 4.3 `outcome` (solo scene `ENDING`)

| Valore | Significato |
|---|---|
| `VICTORY` | L'avventura è stata vinta. |
| `DEFEAT` | L'avventura è stata persa. |
| `NEUTRAL` | Finita, ma né vittoria né sconfitta netta. |

**Va dichiarato dall'autore, il motore non lo indovina mai**: un
finale amaro raggiunto da vivi e una vittoria si somigliano troppo
perché si possano dedurre dallo stato di gioco. Se una scena `ENDING`
non dichiara `outcome`, vale `NEUTRAL` di default (il gioco dice
comunque che è finita, senza mentire su come).

**Eccezione**: la scena raggiunta tramite `deathSceneId` (morte fuori
combattimento) è **sempre** trattata come `DEFEAT`, anche se l'autore
ha scritto un `outcome` diverso su quella scena — la morte batte
qualunque dichiarazione.

Se il libro non ha affatto un finale raggiungibile per un certo esito
(es. nessun `deathSceneId` dichiarato, o punta a una scena
inesistente), il motore ne **fabbrica uno al volo** (testo generato al
momento, o un testo fisso se il narratore non è disponibile) — un
libro non lascia mai il giocatore bloccato senza sapere come è
andata.

### 4.4 `sfx` (effetto sonoro personalizzato, 30/07/2026)

A differenza di `backgroundImage`/`npcImage`/`combat.enemyImage`
(§4.2), **non è un `url:` scritto a mano**: è l'`id` di una voce già
registrata in `manifest.customResources.sounds` (§2). Questa
asimmetria è voluta (Michele: "aggiungere risorse deve essere una
cosa seria e voluta") — registrare un suono richiede un passo
esplicito nell'editor prima di poterlo usare in una scena, e riusare
lo stesso `id` su più scene evita di duplicare inutilmente la stessa
risorsa in cache lato client.

```json
"customResources": {
  "sounds": [ { "id": "campana_a_morto", "url": "https://..." } ]
},
"scenes": [
  { "id": "12", "sfx": "campana_a_morto", "...": "..." }
]
```

**Comportamento**:
- `null` (default, tutti i libri di oggi) — nessun cambiamento: il
  gioco continua a cercare un suono ambientale associato al nome
  dell'immagine `static:` di sfondo, come sempre.
- Valorizzato — **sostituisce del tutto** quella ricerca automatica,
  anche se la scena usa comunque un'immagine `static:`.

**Validazione**: un `sfx` che non corrisponde a nessun `id` in
`customResources.sounds` è un **errore bloccante** (`SfxValidator`,
`core:data`), non un avviso — un riferimento silenziosamente ignorato
sarebbe peggiore di un blocco esplicito in fase di validazione.

**Nota**: al momento della scrittura di questa sezione, il motore di
gioco (`:app`) non sa ancora scaricare/mettere in cache un mp3 da
`url:` a runtime (sa solo suonare file già dentro l'APK) — il supporto
lato client è rimandato, vedi `doc/UPGRADE.md` §7.

---

## 5. Scelte

### 5.1 `Choice` (scelta ordinaria)

| Campo | Tipo | Obbligatorio | Default | Note |
|---|---|---|---|---|
| `id` | stringa | sì | — | ID univoco della scelta nella scena. |
| `choiceText` | stringa | sì | — | Testo sorgente della scelta (tradotto/riscritto dal narratore come il resto della scena). |
| `nextSceneId` | stringa | sì | — | ID della scena raggiunta scegliendo questa opzione. |
| `minRoll` | intero o `null` | no | `null` | Se presente insieme a `maxRoll`: la scelta è visibile solo se l'ultimo tiro di dado rientra in questo intervallo. |
| `maxRoll` | intero o `null` | no | `null` | Vedi sopra. |
| `requiredItem` | stringa o `null` | no | `null` | Nome di un oggetto che il giocatore deve possedere perché la scelta sia visibile. |
| `requiredFlag` | stringa o `null` | no | `null` | Nome di un flag di sessione che deve essere impostato perché la scelta sia visibile. |

**Comportamento in UI**: una scelta i cui requisiti (`minRoll`/
`maxRoll`/`requiredItem`/`requiredFlag`) non sono soddisfatti non
compare affatto nella lista — non è mostrata disabilitata, è proprio
assente.

```json
{
  "id": "choice_1_1",
  "choiceText": "Head down into the town",
  "nextSceneId": "2",
  "minRoll": null,
  "maxRoll": null,
  "requiredItem": null,
  "requiredFlag": null
}
```

### 5.2 `DisciplineChoice` (scelta legata a una disciplina)

Stessi campi di `Choice`, con l'aggiunta di `disciplineId` (uno dei 10
ID canonici, §3.2) al posto centrale:

| Campo | Tipo | Obbligatorio | Default |
|---|---|---|---|
| `id` | stringa | sì | — |
| `disciplineId` | stringa | sì | — deve essere un ID disciplina canonico |
| `choiceText` | stringa | sì | — |
| `nextSceneId` | stringa | sì | — |
| `minRoll` / `maxRoll` / `requiredItem` / `requiredFlag` | vedi sopra | no | `null` |

Visibile SOLO a chi possiede quella disciplina. Uso tipico: una via
d'uscita alternativa e gratuita da un pericolo/combattimento (es.
CAMOUFLAGE per evitare un'imboscata) — vedi §6 (nota finale) per
l'evasione gratuita via disciplina in combattimento.

```json
{
  "id": "dchoice_3_1",
  "disciplineId": "SIXTH_SENSE",
  "choiceText": "You sense the ambush before they see you...",
  "nextSceneId": "5",
  "minRoll": null,
  "maxRoll": null,
  "requiredItem": null,
  "requiredFlag": null
}
```

---

## 6. Combattimento (`Combat`)

Nel JSON l'autore scrive **solo** nome, due statistiche e le
destinazioni; a runtime il motore idrata un personaggio completo da
questi dati.

| Campo | Tipo | Obbligatorio | Default | Note |
|---|---|---|---|---|
| `enemyName` | stringa | sì | — | Nome sorgente del nemico. Viene tradotto dal narratore come il resto del testo (riga `ENEMY\|testo tradotto` nel formato d'uscita); se la traduzione fallisce, resta il nome originale. |
| `enemyImage` | stringa o `null` | no | `null` | Riferimento immagine con prefisso `static:`/`url:` (§4.2). |
| `enemyCombatSkill` | intero | sì | — | Combattività base del nemico. |
| `enemyEndurance` | intero | sì | — | Resistenza del nemico. |
| `immuneToMindblast` | booleano | no | `false` | Se `true`, il giocatore non può usare la disciplina MINDBLAST contro questo nemico (non-morti e simili). |
| `evadeAfterRound` | intero | no | `0` | Numero di round dopo cui l'opzione "fuggi" diventa disponibile nel menu tattico. `0` = disponibile subito. Conta solo se `evadeSceneId` è presente. |
| `winSceneId` | stringa | **sì, sempre** | — | Scena raggiunta vincendo (Resistenza nemico ≤ 0). |
| `loseSceneId` | stringa o `null` | no | `null` | Scena raggiunta perdendo (Resistenza giocatore ≤ 0 in combattimento). Se assente, si usa `deathSceneId` del manifest come rete di sicurezza — **lo specifico batte il globale**. |
| `evadeSceneId` | stringa o `null` | no | `null` | Scena raggiunta fuggendo dal menu tattico. Se assente, l'opzione "fuggi" non compare affatto. |

```json
"combat": {
  "enemyName": "Warehouse Thugs",
  "enemyImage": "static:enemy_bandits_city",
  "enemyCombatSkill": 16,
  "enemyEndurance": 24,
  "immuneToMindblast": false,
  "evadeAfterRound": 0,
  "winSceneId": "6",
  "loseSceneId": "7"
}
```

Nota: **fuggire via disciplina** (es. CAMOUFLAGE, SIXTH_SENSE) è
tutt'altro meccanismo — è una `disciplineChoice` sulla scena stessa
(§5.2, disponibile PRIMA che il combattimento inizi, gratis, nessun
danno), non un campo del blocco `combat`. Fuggire dal menu tattico
invece costa un ultimo round in cui solo il giocatore subisce danni.

---

## 7. Regole globali (`globalRules`, nel manifest)

Regole "condizione → salto a una scena", valutate a ogni cambio scena
(dopo i `gameMechanics` della scena di arrivo, prima regola che matcha
vince, nell'ordine di scrittura). Non esiste un campo dedicato per la
"vittoria": è una `globalRule` come le altre.

| Campo | Tipo | Obbligatorio | Note |
|---|---|---|---|
| `type` | enum | sì | `FLAG` (controlla un flag di sessione) \| `VAR` (controlla una variabile numerica) |
| `name` | stringa | sì | Nome del flag o della variabile da controllare. |
| `operator` | stringa (simbolo) | sì | Uno tra `==`, `!=`, `>=`, `<=`, `>`, `<` |
| `value` | stringa | sì | Valore di confronto — per `FLAG` di norma `"true"`/`"false"`, per `VAR` un numero. |
| `targetSceneId` | stringa | sì | Scena raggiunta se la condizione è vera. |

```json
"globalRules": [
  { "type": "FLAG", "name": "traditore_smascherato",
    "operator": "==", "value": "true", "targetSceneId": "99" },
  { "type": "VAR", "name": "sospetto",
    "operator": ">=", "value": "10", "targetSceneId": "66" }
]
```

Il validatore segnala (solo un **avviso**, non blocca il caricamento)
se `targetSceneId` non punta a una scena `ENDING` — di norma una
globalRule porta a un finale, ma non è un obbligo rigido.

---

## 8. Comandi di scena (`gameMechanics`)

Ogni elemento di `gameMechanics` ha questa forma generica:

```json
{ "command": "nomeComando", "params": { ... } }
```

`params` è un oggetto libero: ogni comando legge le chiavi che gli
servono, **un parametro mancante o scritto male non genera mai un
errore** — il comando semplicemente non fa nulla (il gioco non si
blocca mai). I comandi si eseguono **in ordine di scrittura** quando
il giocatore entra nella scena; se un comando produce un salto di
scena, i comandi successivi non vengono eseguiti.

Questi comandi normalmente li scrive **Gemma** (l'IA narratore), non
l'autore — l'IA li genera dentro tag tipo `<addItem .../>` nel testo,
che il motore converte in questa forma `command`+`params` (vedi
`content/config.json` per la mappa completa tag→comando). Ma
**nel JSON del libro l'autore può scriverli anche a mano**, esattamente
in questo formato — è quello che fanno già i libri di test in
`content/test-books/`.

### 8.1 Tabella comandi

| `command` | Parametri in `params` | Effetto |
|---|---|---|
| `addItem` | `itemName` (str), `itemType` (str: `WEAPON`\|`BACKPACK_ITEM`\|`SPECIAL_ITEM`\|`GOLD`), `quantity` (str numerica, default 1), `combatUsable` (str `"true"`/`"false"`, default false), `effect` (str, es. `"HEAL:4"`), `weaponType` (str, solo se `itemType=WEAPON`, vedi §9) | Aggiunge l'oggetto all'inventario dell'eroe. Se lo zaino/le armi sono già pieni, l'oggetto **si scarta in silenzio** (nessun errore) — per lasciare scegliere al giocatore cosa prendere, usa `offerItem` (ultima riga di questa tabella) invece. |
| `removeItem` | `itemName` (str), `quantity` (str numerica, default 1) | Rimuove N unità. Se il giocatore ne ha meno di N, rimuove solo quel che c'è, senza errore. *(status: TO_IMPLEMENT in config.json — non ancora generato da Gemma, ma già eseguito dal motore se scritto a mano.)* |
| `removeAllItems` | `type` (str: uno dei 4 `ItemType`) | Svuota tutti gli oggetti di quel tipo. |
| `healStat` | `statName` (str, solo `"ENDURANCE"` ha effetto), `amount` (str numerica oppure `"FULL"`) | Cura la Resistenza fino al massimo (coerceIn 0..massimo effettivo). `"FULL"` = riporta al massimo. |
| `applyStatModifier` | `statName` (`"ENDURANCE"` \| `"COMBAT_SKILL"`), `amount` (intero, può essere negativo) | `ENDURANCE`: modifica subito `currentEndurance` (un fatto). `COMBAT_SKILL`: aggiunge un modificatore narrativo attivo (`StatModifier`), sommato dal motore quando serve — non un valore diretto. |
| `requireAction` | `action` (str, solo `"EAT_MEAL"` ha effetto), `penaltyStat` (str), `penaltyValue` (str, es. `"-3"`) | Se l'eroe ha la disciplina HUNTING: nessun effetto (si sfama gratis). Altrimenti, se possiede almeno un Pasto: lo consuma e cura +1 Resistenza. Altrimenti: applica la penalità dichiarata come un `applyStatModifier`. |
| `setFlag` | `flagName` (str), `value` (str) | Imposta un flag di sessione (usato da `globalRules`/`requiredFlag`/`checkItemAndJump` ecc). |
| `rollForQuantity` | `item` (str), `baseValue` (str numerica, default 0), `itemType` (str, opzionale, default `GOLD`) | Tira il dado (0-9) **in silenzio** (il motore, non il giocatore) e aggiunge `baseValue + tiro` unità dell'oggetto. Se il totale è ≤ 0, non aggiunge nulla. |
| `rollOnItemTable` | `outcomes` (array di oggetti, vedi sotto) | Tira il dado (0-9) **in silenzio** e assegna l'oggetto dell'intervallo che copre il tiro. **Vincolo validato**: gli intervalli devono coprire 0-9 per intero, senza sovrapposizioni (vedi §10). *(status: TO_IMPLEMENT in config.json.)* |
| `checkStatAndJump` | `statName` (str: `"ENDURANCE"` \| `"COMBAT_SKILL"` \| nome di una variabile di sessione), `operator` (simbolo o parola, §7), `value` (intero), `targetScene` (str) | Se la condizione è vera, salta subito a `targetScene`. |
| `checkItemAndJump` | `itemName` (str), `quantity` (str numerica, default 1), `operator` (`"HAS"` default \| `"NOT_HAS"`), `nextSceneId_TRUE` (str), `nextSceneId_FALSE` (str, opzionale) | Controlla il possesso e salta al ramo giusto. Se il ramo falso non è dichiarato, nessun salto quando la condizione è falsa. *(status: TO_IMPLEMENT in config.json.)* |
| `handleRandomChoice` | `outcomes` (array, stessa forma di `rollOnItemTable` ma con `nextSceneId` invece di un oggetto) | **Tira il giocatore** (appare il Dado del Destino in UI): il tiro sceglie a quale scena saltare tra gli intervalli dichiarati. |
| `handleSkillCheck` | `checkType` (str libera), `discipline` (str, opzionale), `modifier` (intero, opzionale), `outcomes` (array come sopra) | **Tira il giocatore**: come `handleRandomChoice`, ma se `discipline` è dichiarata e l'eroe la possiede, il tiro riceve `+modifier` prima di cercare l'esito. |
| `handleConditionalAction` | `condition` (`"HAS_ITEM"` \| `"NOT_HAS_ITEM"` \| `"HAS_DISCIPLINE"` \| `"NOT_HAS_DISCIPLINE"`), `itemName` (str, se la condizione riguarda un oggetto), `disciplineName` (str, se la condizione riguarda una disciplina), `action` (oggetto annidato `{ "command": ..., "params": {...} }`) | Se la condizione è vera, esegue il comando annidato in `action` come se fosse scritto direttamente in `gameMechanics`. `itemName`/`disciplineName` sono parametri allo stesso livello di `condition`, non dentro `action`. |
| `setGlobalVar` | `varName` (str), `value` (str), `operation` (solo `"SET"` ha effetto) | Imposta una variabile numerica di sessione a `value` (se `value` non è un numero, diventa un flag testuale invece). |
| `updateGlobalVar` | `varName` (str), `value` (intero), `operation` (solo `"ADD"` ha effetto) | Somma `value` (può essere negativo) alla variabile esistente. |
| `offerItem` | `itemName` (str), `itemType` (str), `quantity` (str, default 1), `combatUsable` (str, default false), `effect` (str, opzionale), `weaponType` (str, opzionale) | **Non eseguito automaticamente all'ingresso in scena.** Mette l'oggetto "sul banco": il giocatore lo vede in UI e lo prende lui stesso, uno alla volta, col pulsante "Prendi" — mai un `addItem` silenzioso che scarta ciò che eccede la capienza. **Mai generato da Gemma**, solo scritto a mano dall'autore. Non è nemmeno registrato in `content/config.json` per questo motivo. |

*(I comandi marcati "TO_IMPLEMENT" in `content/config.json` non
vengono ancora generati automaticamente da Gemma dentro le scene, ma
il motore li esegue regolarmente se un autore li scrive a mano nel
JSON — la marcatura riguarda solo la generazione IA, non l'esecuzione.)*

### 8.2 Forma di un `outcome` in `rollOnItemTable`/`handleRandomChoice`/`handleSkillCheck`

Ogni elemento dell'array `outcomes` è un oggetto con **sempre**
`minRoll`/`maxRoll` (intervallo di tiro 0-9, inclusivo) più un payload
diverso a seconda del comando:

```json
// rollOnItemTable — il payload è un oggetto da aggiungere all'inventario
{ "minRoll": 0, "maxRoll": 4, "itemName": "niente" }
{ "minRoll": 5, "maxRoll": 7, "itemName": "Pugnale", "itemType": "WEAPON", "weaponType": "DAGGER" }
{ "minRoll": 8, "maxRoll": 9, "itemName": "Corone d'oro", "itemType": "GOLD", "quantity": 7 }
```

```json
// handleRandomChoice / handleSkillCheck — il payload è una destinazione
{ "minRoll": 0, "maxRoll": 5, "nextSceneId": "12" }
{ "minRoll": 6, "maxRoll": 9, "nextSceneId": "13" }
```

Un intervallo senza `itemName` in `rollOnItemTable` significa "non
trovi niente" (nessun oggetto aggiunto); un tiro che non ricade in
nessun intervallo, per gli altri due comandi, significa "nessun
salto" (degrado, si resta sulla scena).

### 8.3 Esempi da `content/test-books/`

```json
// offerItem — oggetti "sul banco", il giocatore sceglie (test_items_and_weapons.json)
{ "command": "offerItem", "params": { "itemName": "Broadsword", "itemType": "WEAPON", "weaponType": "BROADSWORD" } },
{ "command": "offerItem", "params": { "itemName": "Laumspur Potion", "itemType": "BACKPACK_ITEM", "effect": "HEAL:4", "combatUsable": "true" } },
{ "command": "offerItem", "params": { "itemName": "Gold Crowns", "itemType": "GOLD", "quantity": 15 } }
```

```json
// applyStatModifier e requireAction (test_eat_meal_sound.json)
{ "command": "applyStatModifier", "params": { "statName": "ENDURANCE", "amount": "-5" } },
{ "command": "requireAction", "params": { "action": "EAT_MEAL", "penaltyStat": "ENDURANCE", "penaltyValue": "-3" } }
```

---

## 9. Oggetti e armi (per `addItem`/`offerItem`/inventario)

### 9.1 `ItemType` e limiti di capienza (`doc/STATO.md` §4.1)

| Valore | Limite |
|---|---|
| `WEAPON` | massimo 2 armi impugnabili |
| `BACKPACK_ITEM` | massimo 8 posti zaino |
| `SPECIAL_ITEM` | illimitati |
| `GOLD` | massimo 50 Corone |

Un `addItem` oltre il limite scarta in silenzio; per questo, quando la
scelta deve essere del giocatore, si usa `offerItem` (§8.1) invece.

### 9.2 `WeaponType` — solo per `itemType: "WEAPON"`

I 9 tipi d'arma canonici del libro 1 più `UNARMED` (non è un'arma,
è la specializzazione WEAPONSKILL a mani nude — non usarlo mai su un
oggetto reale):

```
DAGGER, SPEAR, MACE, SHORT_SWORD, WARHAMMER, SWORD, AXE,
QUARTERSTAFF, BROADSWORD, UNARMED
```

### 9.3 `effect`

Stringa dichiarativa libera, pensata per estendersi senza cambiare lo
schema. Oggi il motore implementa solo il pattern `"HEAL:n"` (cura n
punti Resistenza quando l'oggetto viene consumato in combattimento, se
`combatUsable: true`).

---

## 10. Regole di validazione

Un pacchetto che viola una di queste regole viene **rifiutato al
caricamento** (errore bloccante):

- **Nessun ID di scena duplicato.**
- **Deve esistere almeno una scena `START`.**
- **Ogni riferimento a un ID di scena deve esistere davvero**:
  `deathSceneId`, `globalRules[].targetSceneId`,
  `choice.nextSceneId`, `disciplineChoice.nextSceneId`,
  `combat.winSceneId`, `combat.loseSceneId`, `combat.evadeSceneId`.
- **Ogni `id` di disciplina** (nel manifest o in `disciplineId` di una
  scelta) deve essere uno dei 10 canonici (§3.2).
- **`combat.winSceneId` non può essere una stringa vuota.**
- **Gli intervalli di `rollOnItemTable`** (`outcomes[].minRoll/maxRoll`)
  devono coprire **esattamente** i valori 0-9, senza buchi e senza
  sovrapposizioni.
- **Ogni `backgroundImage`/`npcImage`/`combat.enemyImage` valorizzato**
  deve avere il prefisso `static:` o `url:` (§4.2) — un valore senza
  prefisso riconosciuto è rifiutato.
- **Un `url:` accetta solo schema `http://` o `https://`** — mai
  `file://` o altri schemi.
- **`sfx` valorizzato** deve corrispondere a un `id` presente in
  `customResources.sounds` (§4.4) — mai vuoto, mai un ID inesistente.

Solo un **avviso**, non blocca il caricamento:

- La destinazione di una `globalRule` dovrebbe essere una scena
  `ENDING` (di norma lo è, ma non è un obbligo rigido).
- **Ogni `url:` in un'immagine** genera sempre un avviso, anche se il
  link è ben formato e raggiungibile — è una dipendenza di rete da
  rivedere prima di pubblicare o distribuire il libro (§4.2).
- **Un ID duplicato dentro `customResources.images` o
  `customResources.sounds`** (§2) — probabile errore di scrittura, ma
  non rompe nulla: le scene salvano comunque l'url risolto per intero.

---

## 11. Riferimenti

- `doc/REGOLE.md` — semantica completa di combattimento, discipline,
  regole globali, dado del destino (criterio narrativo su quando tira
  il giocatore e quando tira il motore in silenzio).
- `doc/STATO.md` — sessione di gioco, inventario, checkpoint/difficoltà
  (fuori dallo scopo di questo documento, ma utile per capire cosa
  succede DOPO che lo schema qui descritto viene caricato).
- `doc/ETL.md` — pipeline di conversione di libri Project Aon in
  questo formato.
- `content/config.json` — la mappa completa tag-XML-like-di-Gemma →
  comando `gameMechanics` (regex, parametri, per chi deve capire cosa
  genera l'IA prima che diventi questo schema).
- `content/scenes.sample.json` — libro di esempio completo e
  giocabile.
- `content/test-books/` — libri minimi, uno per ogni caratteristica
  dello schema (oggetti/armi, pasto obbligatorio, immagini
  location/nemico/npc, combattimento, discipline).
- `doc/SUONI-IMMAGINI.md` — elenco completo di tutti gli ID immagine
  del catalogo `static:` (luoghi, nemici/bestie, NPC), con lo stato
  dell'effetto sonoro abbinato.
