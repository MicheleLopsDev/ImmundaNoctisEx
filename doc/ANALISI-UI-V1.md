# Analisi riuso — cartella `ui/` di v1

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
