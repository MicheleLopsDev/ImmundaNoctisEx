# Materiale grezzo per la Specifica 2 — Regole di gioco (da v1)

Sessione lampo 16/07/2026. Estratto da `engine/GameRulesEngine.kt` (48 righe),
`engine/rules/LoneWolfRules.kt` (107 righe) e dai punti di chiamata in
`view/MainViewModel.kt` di v1 (`master`@8b705b8).

## Scoperta principale: il combattimento in v1 non è mai stato attivo

Tutta l'orchestrazione del combattimento nel MainViewModel è COMMENTATA:

| Cosa | Riga (MainViewModel.kt) | Stato |
|---|---|---|
| `_combatState` / `combatState` StateFlow | 129-130 | commentato |
| `resolveAutomaticCombat()` (loop round, resistenze) | 570-623 | commentato |
| `generateCombatOutcomeTexts()` | 625+ | commentato |
| Creazione `CombatState(enemy, canEvade, evadeSceneId)` | 1219 | commentato |
| Comandi `setVictoryText`/`setDefeatText` | 1172-1181 | commentati |

Uniche chiamate VIVE a `gameRules`:
- `canUseDiscipline(hero, discipline, scene)` — riga 1528 (filtro pulsanti discipline)
- `getKaiRank(kaiDisciplines.size)` — riga 1600 (etichetta grado)

**Conseguenza per Ex**: si riusa la tabella CRT e il contratto; il
CombatManager si progetta da zero, senza debito di compatibilità.

## Pezzi riusabili così come sono

### 1. Tabella CRT (`LoneWolfRules.COMBAT_RESULTS_CHART`)
- `Map<Int, List<Pair<Int,Int>>>`: chiave = Rapporto di Forza (-10..+10),
  lista indicizzata dal tiro, Pair = (danno giocatore, danno nemico)
- `KILL_DAMAGE = 999` come sentinella di uccisione istantanea
- Rapporto fuori scala gestito con `coerceIn` sui limiti della mappa
- ⚠️ Trappola off-by-one documentata nel codice: tiro casuale 0-9, ma il
  tiro '0' della tabella ufficiale è l'ULTIMO indice della lista
  (`tableRollIndex = if (roll == 0) 9 else roll - 1`). Nei test di Ex
  serve una fixture che copra proprio questo mapping.

### 2. Algoritmo del round (`resolveCombatRound`)
1. Combattività Effettiva = base + somma modificatori attivi con
   `statName == "COMBATTIVITA"` (pattern StatModifier già presente!)
2. Rapporto di Forza = CS giocatore − CS nemico
3. Tiro 0-9
4. Lookup CRT con coerceIn
5. Ritorna danni (giocatore, nemico)

### 3. `canUseDiscipline`: possesso disciplina AND scena la offre nei
   `disciplineChoices`. Logica minima e giusta, si porta in Ex.

### 4. Scala gradi Kai (`getKaiRank`)
0-4 Novizio, 5 Iniziato, 6 Discepolo, 7 Viandante, 8 Guerriero,
9 Maestro, 10 Gran Maestro, >10 Gran Maestro Supremo.
⚠️ In v1 i NOMI italiani sono hardcoded nell'engine — viola la decisione
di architettura (enum/id in engine, nomi localizzati in UI). In Ex:
`enum KaiRank` + soglie in engine, stringhe in `strings.xml`.

## Design dell'evasione già abbozzato in v1 (in `CombatState`, mai cablato)
```kotlin
data class CombatState(
    val enemy: GameCharacter,
    val canEvade: Boolean,
    val evadeSceneId: String?,
    val victoryText: String? = null,   // morto in Ex: esiti = scene
    val defeatText: String? = null,    // morto in Ex: esiti = scene
    val evadeAfterRound: Int? = 0      // idea: evasione solo dopo N round
)
```
`evadeAfterRound` è un'idea da valutare nella specifica 2: nei libri
originali l'evasione è spesso permessa solo in certe condizioni.

## Cose da cambiare rispetto a v1 (decisioni già prese in architettura)
- `Random.nextInt` inline → iniettare `DiceRoller` (test deterministici +
  Dado del Destino UI sulla stessa porta)
- `CombatRoundResult` contiene `LocalizedText` con messaggio pre-composto →
  in Ex risultato = dati puri (danni, tiro, rapporto); il testo lo compone
  la UI. `LocalizedText` sparisce comunque (lingua singola)
- Nomi gradi Kai fuori dall'engine (vedi sopra)

## Input già raccolti per la specifica 2 (dalle sessioni precedenti)
- Comandi da implementare: `removeItem`, `checkItemAndJump`,
  `rollOnItemTable` (marcati TO_IMPLEMENT nel config)
- Regole globali di esito: Resistenza ≤ 0 → `deathSceneId`;
  generalizzazione su flag/variabili globali → `victorySceneId`
  (valutate dall'engine a ogni transizione)
- Esiti combattimento = transizioni: WIN → scena X, LOSE → scena Y,
  evasione via `disciplineChoices` della stessa scena
- Feedback immediato di fine combattimento = testo UI (`strings.xml`),
  non contenuto del libro
