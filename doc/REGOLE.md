# Specifica 2 — Regole di gioco

Stato: IN CORSO. Blocco combattimento CHIUSO (sessione 16/07/2026 sera).
Blocchi aperti: regole globali di esito, gradi Kai, discipline e oggetti
in combattimento, comandi TO_IMPLEMENT (removeItem, checkItemAndJump,
rollOnItemTable), Dado del Destino fuori dal combattimento.

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
  (regola valutata dall'engine a ogni transizione — dettagli nel
  blocco regole globali, aperto).

### 1.5 Il nemico nel JSON di scena (minimale)

```json
"combat": {
  "enemyName": "Warehouse Thug",
  "enemyCombatSkill": 14,
  "enemyEndurance": 22,
  "evadeAfterRound": 2,
  "winSceneId": "6",
  "loseSceneId": "7",
  "evadeSceneId": "5"
}
```

- Niente `GameCharacter` completo (errore di v1): il nemico è nome +
  due statistiche + destinazioni.
- `evadeAfterRound` e `evadeSceneId` opzionali; `loseSceneId`
  opzionale (fallback `deathSceneId`); `winSceneId` obbligatorio.
- **`enemyName` viene tradotto da Gemma** nel giro normale della
  scena: nuova riga nel formato pipe `ENEMY|testo tradotto`, con
  fallback al nome originale del pacchetto se il parsing la perde
  (stessa filosofia di CHOICE/DISCIPLINE: il parsing fallito non
  blocca mai il gioco). Da riflettere nel config: tag `enemy_line`.

### 1.6 Impatti su altri artefatti (da fare)

- [ ] `config.json`: aggiungere tag `enemy_line`
  (`^ENEMY\|(.+)$`, comando `updateEnemyName`).
- [ ] Frammento prompt `outputFormatText`: aggiungere la riga ENEMY
  al formato di output quando la scena ha un blocco combat.
- [ ] `scenes.sample.json`: la scena 4 (battle) adotta il blocco
  combat di 1.5.
- [ ] Validatore scene: se `combat` presente, `winSceneId`
  obbligatorio e le destinazioni devono esistere nel grafo.

---

## Blocco 2 — Regole globali di esito (APERTO)
Resistenza ≤ 0 → deathSceneId; generalizzazione su flag/variabili →
victorySceneId; valutazione a ogni transizione. Da dettagliare.

## Blocco 3 — Gradi Kai (APERTO)
Soglie da v1 (0-4 Novizio ... 10 Gran Maestro); enum in engine, nomi
in strings.xml. Da decidere: effetti meccanici del grado, se esistono.

## Blocco 4 — Discipline e oggetti in combattimento (APERTO)
MINDBLAST +2 CS se nemico non immune (canonico); flag "usabile in
combat" sugli oggetti; HEALING fuori combattimento. Da dettagliare.

## Blocco 5 — Comandi TO_IMPLEMENT (APERTO)
removeItem, checkItemAndJump, rollOnItemTable: semantica esatta,
casi limite (oggetto assente, quantità insufficiente).

## Blocco 6 — Dado del Destino fuori dal combattimento (APERTO)
skill_check_tag / random_choice_table: quando il giocatore tira
manualmente vs quando tira il motore.
