# Inventario asset v1

Sessione lampo 16/07/2026. Scansione di `app/src/main/res/drawable*` e
`app/src/main/assets/` nel repo `MicheleLopsDev/ImmundaNoctis-master`,
commit `8b705b8` (branch `master`, merge del 30/06/2025 — punto in cui
`master` si è allineato per l'ultima volta a `develop`).

## Nota importante: questo commit è più magro del `develop` analizzato il 14/07

`doc/ANALISI-RIUSO-V1.md` (14/07/2026) fotografa il branch **`develop`**
(637 MB totali, 43 MB in `res/drawable`) e cita asset che **non esistono**
in questo commit: le icone di gioco (`ic_sword`, `ic_axe`, `ic_map`,
`ic_backpack`, `ic_potion`, `ic_gold`, ecc.), `portrait_elara.jpeg`,
`lupo_solitario.png`, `ic_hero_portrait_placeholder.jpeg`. `master`@`8b705b8`
contiene solo i 16 file elencati sotto (20,47 MB), quasi tutti ritratti/arte
di classe a piena risoluzione — niente icone, niente sfondi ambiente. Le
icone e i ritratti aggiuntivi sono stati aggiunti su `develop` **dopo**
questo merge: se servono per l'inventario completo, vanno scansionati da
`develop` a parte (fuori scope di questa sessione, che ha scansionato
esattamente il commit richiesto).

## `app/src/main/assets/`

| File | Tipo | Dimensione | Risoluzione |
|---|---|---|---|
| `config.json` | JSON | 12,0 KB | — |
| `scenes.json` | JSON | 634,2 KB | — |

**Totale cartella: 646,2 KB (0,63 MB)**

## `app/src/main/res/drawable/`

(unica cartella `drawable*` presente in questo commit — nessuna variante
`-hdpi`/`-xxhdpi`/`-nodpi`)

| File | Tipo | Dimensione | Risoluzione |
|---|---|---|---|
| `class_warrior_female.jpeg` | JPEG | 2,05 MB | 2048×2048 |
| `portrait_hero_male.jpeg` | JPEG | 1,77 MB | 2048×2048 |
| `class_witch_male.jpeg` | JPEG | 1,72 MB | 2048×2048 |
| `class_warrior_male.jpeg` | JPEG | 1,47 MB | 2048×2048 |
| `class_sage_female.jpeg` | JPEG | 1,40 MB | 2048×2048 |
| `portrait_hero_female.jpeg` | JPEG | 1,39 MB | 2048×2048 |
| `class_witch_female.jpeg` | JPEG | 1,39 MB | 2048×2048 |
| `portrait_dm.jpeg` | JPEG | 1,28 MB | 2048×2048 |
| `class_thief_male.jpeg` | JPEG | 1,29 MB | 2048×2048 |
| `map_dungeon.jpeg` | JPEG | 1,27 MB | 2048×2048 |
| `class_thief_female.jpeg` | JPEG | 1,24 MB | 2048×2048 |
| `portrait_cleric.jpeg` | JPEG | 1,17 MB | 2048×2048 |
| `portrait_mage.jpeg` | JPEG | 1,14 MB | 2048×2048 |
| `class_sage_male.jpeg` | JPEG | 1,25 MB | 2048×2048 |
| `ic_launcher_background.xml` | XML (vector) | 4,8 KB | — |
| `ic_launcher_foreground.xml` | XML (vector) | 1,7 KB | — |

**Totale cartella: 19,84 MB**

## Totali

| Categoria | Peso |
|---|---|
| `assets/` | 0,63 MB |
| `res/drawable/` | 19,84 MB |
| **Totale complessivo** | **20,47 MB** (18 file) |

## Classificazione

### RIUSABILE IN EX
- `portrait_hero_male.jpeg`, `portrait_hero_female.jpeg` — ritratto
  dell'eroe per la scena teatrale (anello di parola); la selezione di
  genere sopravvive come scelta minima in creazione personaggio.
- `portrait_dm.jpeg` — ritratto del Narratore/DM, serve per lo stesso
  meccanismo di anello di parola.
- `ic_launcher_background.xml`, `ic_launcher_foreground.xml` — icona
  launcher adattiva, boilerplate Android standard, nessun contenuto
  narrativo da rifare.

### LEGATO A FEATURE MORTA
- `class_sage_female/male.jpeg`, `class_thief_female/male.jpeg`,
  `class_warrior_female/male.jpeg`, `class_witch_female/male.jpeg`
  (8 file, ~11,8 MB) — ritratti del sistema di **classi D&D** (Sage,
  Thief, Warrior, Witch) per la creazione personaggio. In Lupo Solitario
  non esiste selezione di classe: il protagonista è sempre un Iniziato
  Kai: le Discipline sostituiscono le classi (vedi `doc/DIARIO.md`,
  15/07). Feature morta con il pivot di ambientazione, non con stdf/chat
  libera, ma stesso destino: non si porta in Ex.

### DA RIFARE
- `portrait_cleric.jpeg`, `portrait_mage.jpeg` — ritratti generici
  legati a classi D&D (chierico, mago), non a personaggi specifici:
  l'arte potrebbe riservarsi per un futuro Compagno Kai, ma nome e
  contesto vanno rifatti (nessun equivalente di `portrait_elara.jpeg`,
  la candidata Compagna citata in `ANALISI-RIUSO-V1.md`, presente in
  questo commit).
- `map_dungeon.jpeg` — mappa di dungeon generica; non corrisponde a
  nessuno dei 5 ambienti richiesti dal sample (vedi sotto); riusabile
  solo come eventuale sfondo segnaposto per scene sotterranee, non come
  base per `inn`/`city`/`alley`/`battle`/`warehouse`.
- `app/src/main/assets/scenes.json` — schema scena vecchio (350 scene di
  *Flight from the Dark*): decisione già chiusa il 14/07 di non
  riusarlo, verrà rigenerato dall'ETL sul libro 1 nello schema nuovo.
- `app/src/main/assets/config.json` — versione precedente già superata:
  l'audit e la revisione per Ex sono stati fatti in questa sessione sul
  file vivo in `content/config.json` (vedi diario, sessione precedente).

## Candidati per i `backgroundImage` del sample (inn/city/alley/battle/warehouse)

**Nessuno.** In questo commit non esiste alcuno sfondo ambiente: i 16 file
sono tutti ritratti a piena figura (formato 2048×2048, verticale/quadrato,
pensati per riquadri personaggio) o la mappa dungeon sopra citata, che non
copre nessuno dei 5 ambienti. Tutti e 5 gli sfondi (`inn`, `city`, `alley`,
`battle`, `warehouse`) sono da produrre ex novo o reperire altrove — non
c'è materiale v1 da recuperare per questo campo, nemmeno come segnaposto
diverso da un placeholder generico.

## Nota sui formati e stima risparmio

Tutti i 14 file raster sono JPEG a 2048×2048 (risoluzione da poster),
molto oltre la dimensione reale di visualizzazione (ritratto in barra
giocatore o riquadro scena, tipicamente poche centinaia di pixel di lato).
Confermano la stima già fatta in `ANALISI-RIUSO-V1.md` (14/07) sul set
allargato di `develop`: ridimensionare a ~1024 px sul lato lungo e
convertire in **WebP qualità ~80** porta ogni ritratto da 1,2–2,1 MB a
circa 200–400 KB.

Su questo sottoinsieme di 18 file (20,47 MB):
- i 14 JPEG (19,84 MB) sono tutti candidati alla conversione;
- stima: 14 file × ~300 KB medi dopo resize+WebP ≈ 4,2 MB, contro i
  19,84 MB attuali → **risparmio stimato ~79% (~15,6 MB)**;
- i 2 XML vettoriali (6,5 KB totali) e i 2 JSON (646 KB totali) restano
  invariati: non sono immagini raster.

Nessuno stdf/immagine generata dinamicamente è presente in questo commit
(coerente con la decisione già nota: la generazione Stable Diffusion vive
solo in codice/moduli nativi su `develop`, non produce drawable statiche
versionate).