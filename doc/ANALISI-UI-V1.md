# Analisi riuso — UI di v1 (componenti e Activity)

Richiesta da Michele il 17/07/2026 (annotazione urgente): la cartella
`app/src/main/java/io/github/luposolitario/immundanoctis/ui` di v1 non
era stata analizzata. Scansione completa: 11 file, 1.618 righe
(`theme/` 151, `adventure/` 994, `configuration/` 473). Verificato che
lo zip fornito da Michele è identico alla copia `ImmundaNoctis-master`
su disco.

## Verdetti per file

### `theme/` — RIUSABILE QUASI INTEGRALE
`Theme.kt` (dark/light ColorScheme Material3 completi), `Color.kt`,
`Type.kt`. La STRUTTURA (tema con toggle esplicito via
`ThemePreferences(isSystemInDarkTheme())`) serve già in Fase 3 per
l'opzione tema chiaro/scuro; i VALORI estetici si rivedono in Fase 7.

### `adventure/ChoiceComponents.kt` — RIUSO QUASI DIRETTO
`ChoicesContainer` + `ActionChoiceCard`: scelte normali (card neutra)
vs scelte-disciplina (card dorata/tertiary con bordo e icona). È
esattamente la zona scelte di UI.md. Da cambiare: modelli Ex al posto
di `NarrativeChoice`/`LocalizedText`, testo = scelta tradotta con
fallback all'originale.

### `adventure/AdventureHeader.kt` — RIUSO A PEZZI
- `CharacterPortrait` (ritratto circolare, bordo colorato su chi è
  selezionato) + `PlaceholderPortrait`: base del banner con cerchio
  d'oro di UI.md.
- `TokenSemaphoreIndicator` (pallino verde/giallo/rosso, tocco = dialog
  dettaglio token, stato CRITICAL con reset sessione): è il semaforo
  di UI.md già fatto; `TokenInfo`/`TokenStatus` arriveranno col modulo
  inference (Fase 4).
- `AdventureHeader` in sé NO: ordina i personaggi con id magici
  ("dm"/"hero"), logica chat abilitata/disabilitata (era-chatbot),
  sfondo `map_dungeon` fisso al posto del `backgroundImage` di scena.

### `adventure/ChatComponents.kt` — RIUSO PARZIALE (già analizzato)
`MessageBubble` (icone copia/traduci/leggi — decisioni già a diario:
tre icone, TTS grigia con auto-lettura, niente save);
`GeneratingIndicator` ("X sta pensando..." + bottone Ferma) →
"il narratore scrive…" di UI.md. `MessageInput` ("Cosa fai?", contatore
0/10240): MORTO PER DESIGN in Ex (via la barra di testo libero; il
contatore è assorbito dal semaforo).

### `adventure/PlayerActionBar.kt` — RIUSO COME PATTERN
Card di stato del giocatore: ritratto-dado cliccabile con **bordo ORO
quando il tiro è abilitato, argento altrimenti** (convenzione da
conservare per il Dado del Destino), nome, grado Kai con icona dorata,
conteggio Pasti, stats. Antenato della card di stato di UI.md. Da
correggere: item "Pasto" cercato per nome italiano hardcoded (in Ex ID
canonico "Meal"), colori inline (marrone legno) → tema.

### `adventure/AdventureUtils.kt` — RIUSO CON CORREZIONI
- `getIconForDiscipline`: mappa disciplina→icona Material. BUG di v1:
  chiavi sui NOMI DISPLAY ("Sixth Sense"); in Ex si mappa sugli ID
  canonici UPPER_SNAKE.
- `RobustImage` (immagine con placeholder su errore): pattern utile.
- `WeaponSkillSelectionDialog`: struttura del dialog di
  specializzazione ok; stringhe italiane hardcoded e mappa
  `WEAPON_TYPE_NAMES` → `strings.xml`.

### `adventure/AdventureDialogs.kt` — NON RIUSARE
Morto certo (censimento codice morto del 15/07: zero riferimenti, usa
`data.Skill` di una feature abbandonata).

### `configuration/` — RIUSO COME PATTERN + MODELLO PREVIEW
`ConfigurationComponents.kt` e `ModelSlot.kt`: slot di gestione modello
(titolo, sottotitolo, nome file, stato scaricato, progress) — base
della schermata Modelli LLM. `ModelSlot.kt` contiene **l'unica
`@Preview` di v1**, fatta bene: una variante `ModelSlotViewPreview`
STATELESS con dati finti, avvolta in `ImmundaNoctisTheme`. È il modello
della convenzione che Ex adotta ovunque (sotto).

## Nota: WeaponType v1 ≠ WeaponType Ex
v1: AXE, SWORD, MACE, STAFF, SPEAR, BROADSWORD, FISTS, GENERIC.
Ex (canonico, libro 1): DAGGER, SPEAR, MACE, SHORT_SWORD, WARHAMMER,
SWORD, AXE, QUARTERSTAFF, BROADSWORD + UNARMED.
Mapping per il riuso di codice/asset v1: STAFF→QUARTERSTAFF,
FISTS→UNARMED, GENERIC→nessun tipo (degrada); DAGGER/SHORT_SWORD/
WARHAMMER in v1 non esistevano.

## Convenzione Compose Preview (REQUISITO, deciso 17/07/2026)

Richiesta esplicita di Michele: ogni cosa visibile in Anteprima in
Android Studio, come (nelle intenzioni) in v1 — con XML la preview non
era mantenibile, con Compose sì, ed è uno dei motivi della scelta.

Regole per TUTTO il codice UI di Ex, da Fase 3 in poi:

1. **Ogni composable di schermata o componente ha almeno una
   `@Preview`** (idealmente due: tema chiaro e scuro), avvolta nel
   tema dell'app.
2. **Composable stateless per costruzione**: dati puri in ingresso,
   eventi in uscita. Niente ViewModel, Context, singleton o servizi
   dentro i componenti: lo stato vive nel ViewModel della schermata.
   La preview è anche il GUARDRAIL architetturale: se un componente
   non si riesce a mettere in preview, è scritto male.
3. **Dati finti per le preview** in un file `PreviewData.kt` per
   package-schermata (eroi, scene, inventari di esempio), o
   `PreviewParameterProvider` dove serve variare.
4. Quando il componente reale ha dipendenze runtime inevitabili, si fa
   la variante `*Preview` stateless (pattern `ModelSlot` di v1).
5. Dipendenze già pronte in `app/build.gradle.kts`:
   `androidx.ui.tooling.preview` (implementation) + `androidx.ui.tooling`
   (debugImplementation). Nessuna aggiunta necessaria.

---

# Seconda passata (17/07/2026, sessione successiva) — dalle Activity giù alle componenti

Analisi a ritroso richiesta da Michele: le 8 Activity di v1 (3.505
righe totali) + ViewModel (2.561) + util/service (1.196), per censire
le funzioni COMPATIBILI con Ex che non costano grandi riscritture.

## Activity per Activity

### `MainActivity` (208) — RIUSO QUASI DIRETTO
`MenuIcon` (tile 120dp con tooltip, icona+etichetta) e
`MainMenuScreen` (Scaffold, righe di tile, toggle tema nella top bar)
sono ESATTAMENTE la Home a riquadri decisa per Ex: si tolgono le due
tile STDF, restano Avventura / Modelli LLM / Impostazioni. Nota: in v1
il toggle tema è nella top bar della Home; in Ex la scelta tema sta in
Opzioni (deciso 17/07) — tenere anche il toggle rapido in Home costa
una riga, decidere in Fase 5.

### `AdventureActivity` (683) — SCHELETRO BUONO, CONTENUTO DA EX
`AdventureChatScreen` è il prototipo della scena teatrale: top bar con
`TokenSemaphoreIndicator` + titolo sessione + "Paragrafo: N" (l'ID
scena nell'header di UI.md), corpo con header ritratti, lista messaggi
con **streaming già troncato a `--- TAGS ---`** (la riga di codice è
identica a quella progettata per Ex), `ChoicesContainer`,
`PlayerActionsBar`, `GeneratingIndicator`. Da buttare: `MessageInput`
(era-chatbot), la selezione personaggio per la chat, i dialoghi combat
COMMENTATI (mai vissuti — il combat di Ex nasce da CombatSession).
- `LoadingScreen` / `ErrorScreen`: generiche, riuso com'è.
- `InventoryFullDialog` (scambia oggetto quando l'inventario è pieno):
  in Ex v0.1 l'oggetto non entra in silenzio; questo dialog è
  l'upgrade UI futuro già pronto ("scarta qualcosa", STATO.md §4.1).
- **Scoperta**: il menu a tendina ha "Salva Chat Manualmente" visibile
  solo con auto-save spento (`SavePreferences.isAutoSaveEnabled`) — è
  QUESTA l'origine del ricordo di Michele sull'opzione salvataggio;
  la decisione Ex (sempre automatico) resta.

### `CharacterSheetActivity` (569) — RIUSO FORTE (tab Equipaggiamento)
Mappa 1:1 sulla Scheda personaggio di UI.md:
- `WeaponsCard` + `WeaponSlot`: 2 slot arma, bordo ORO sull'arma
  impugnata, icona per `weaponType`, long-press per scartare.
- `CommonItemsCard` + `CommonItemSlot`: griglia 4x2 che PADDA A 8
  SLOT DISEGNATI ANCHE VUOTI — la regola di UI.md è già implementata.
- `StatsAndMealsCard`, `KaiDisciplinesCard`, `SpecialItemsTableCard`.
Adattamenti: `GameItem` di Ex non ha `iconResId`/`combatSkillBonus`/
`isDiscardable` (icona risolta dalla UI per tipo; il bonus è
WEAPONSKILL calcolato dall'engine; scartabilità da derivare); stats
da `effectiveCombatSkill`/`effectiveEndurance`.
`CharacterSheetViewModel` (540): NON riusare — la logica equip/consumo
ora vive nell'engine (`Inventory`).

### `DeathActivity` (76) — PATTERN MINORE
`DeathScreen` (titolo rosso, motivo, RICOMINCIA/ESCI): in Ex la morte
è una scena ENDING (`deathSceneId`), non una Activity; il layout serve
da spunto per il rendering delle ENDING + azioni di fine partita
(in IRON: cancellazione sessione).

### `ConfigurationActivity` (357) — È LA SCHERMATA OPZIONI
`MainEngineScreen`: switch auto-lettura TTS, slider velocità/pitch,
**dropdown voce MALE/FEMALE dalle voci di sistema** (il "voce per
genere" di UI.md già implementato su `TtsPreferences`), cancella
sessione con dialog di conferma. Da togliere: switch auto-save
(deciso: sempre automatico) e switch chat (morta). Da aggiungere:
toggle tema (pattern `ThemePreferences` già usato dall'Activity).
Nota: c'è un dropdown "tono narrativo" (originale/horror/epico/...)
iniettato nel prompt — feature NON nel design Ex (i toni sono
dell'autore via `toneHints`); da valutare come opzione utente futura
**[MICHELE-PROPOSTO]**.

### `ModelActivity` (824) — SOLO DUE PEZZI
`SceneJsonPicker` (GetContent per application/json): è il side-load
del libro di UI.md, riuso diretto. `ModelSlot`/pattern download: base
della schermata Modelli LLM. Il resto è configurazione dual-engine
(Gemma+Llama) di v1: Ex ha solo Gemma, si ridimensiona.

### `SetupActivity` (451) — già analizzata il 17/07 (vedi diario):
RandomStatsCard, EquipmentChoiceCard, DisciplineGridCard,
WeaponSkillSelectionDialog, ExistingSessionScreen.

### `StdfGenerationActivity` + `StdfModelActivity` (337) — MORTE
Feature immagini abbandonata: non riusare.

## ViewModel e util

- `MainViewModel` (1.634): anti-modello dichiarato; si salvano solo i
  PATTERN di stato osservabile già censiti (streamingText,
  isGenerating, respondingCharacterId, TokenInfo) — in Ex spalmati su
  ViewModel piccoli per schermata.
- `SetupViewModel` (309): gating `canProceed` e tiro stat — logica da
  rifare sottile sopra l'engine (il tiro passa da DiceRoller).
- `util/ThemePreferences` (76), `TtsPreferences` (62): riuso del
  pattern quasi integrale (SharedPreferences sottili).
- `util/SavePreferences` (57): decade (auto-save sempre attivo);
  `GameStateManager` (234): sostituito dalla porta di persistenza Ex;
  `StringTagParser` (121): destinato alla Fase 4 (già a piano);
  `FileHelper` (59): utile per la copia del side-load.
- `service/TtsService` (124): riuso con l'aggiunta
  dell'`UtteranceProgressListener` (deciso 17/07, stato SPEAKING).

## Censimento finale: compatibili a basso costo

Pronte quasi com'è (adattare solo modelli/tema):
`MenuIcon`, `MainMenuScreen`, `LoadingScreen`, `ErrorScreen`,
`TokenSemaphoreIndicator`, `CharacterPortrait`, `PlaceholderPortrait`,
`ChoicesContainer`+`ActionChoiceCard`, `GeneratingIndicator`,
`WeaponsCard`+`WeaponSlot`, `CommonItemsCard`+`CommonItemSlot`,
`StatsAndMealsCard`, `KaiDisciplinesCard`, `SpecialItemsTableCard`,
`RandomStatsCard`, `EquipmentChoiceCard`, `DisciplineGridCard`,
`WeaponSkillSelectionDialog`, `ExistingSessionScreen`,
`SceneJsonPicker`, `DeathScreen` (come pattern), `theme/` intero,
`ThemePreferences`, `TtsPreferences`, `TtsService` (+listener).

Riscrittura vera (ma con scheletro v1 da seguire):
`AdventureChatScreen` → scena teatrale (via chat, dentro zone di
UI.md), `MainEngineScreen` → Opzioni (meno switch, più tema),
`MessageBubble` → blocco narratore (3 icone decise), gestione modelli
(solo Gemma). Tutte le migrazioni nascono con @Preview (convenzione
sopra).
