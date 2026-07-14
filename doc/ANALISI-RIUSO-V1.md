# Analisi di riuso — Immunda Noctis v1 (ramo develop) → ImmundaNoctisEx

> Analisi file per file del repository `ImmundaNoctis-master`, ramo `develop`
> (10.280 file, 637 MB), per decidere cosa copiare, cosa adattare, cosa tenere
> come riferimento e cosa abbandonare.
>
> Data: 14 luglio 2026

---

## Fotografia del repository

| Area | Peso | Contenuto |
|---|---|---|
| `stdf/` | **420 MB** | Modulo generazione immagini: 334 MB di C++ (MNN e terze parti), 86 MB di librerie native `jniLibs` |
| `app/` | 45 MB | L'app vera: 60 file Kotlin, assets, risorse (di cui **43 MB** in `res/drawable`) |
| `DOC/` | 2,3 MB | Documenti di design, scene di test, i 5 libri originali di Lupo Solitario in HTML |
| `llama/` | 120 KB | Modulo bridge llama.cpp |
| Radice | — | ANALISI.MD, TAGS.MD, ROADMAP, PROMPT.txt (55 KB), build files |

Il 66% del peso del repository è il modulo STDF già deciso da abbandonare.

---

## 1. COPIARE (quasi pari pari)

File sani, isolati, coerenti con il design di Ex. Si copiano nel nuovo
progetto cambiando solo il package (`immundanoctis` → `immundanoctisex` o
simile) e i riferimenti alle data class dove indicato.

| File | Note |
|---|---|
| `engine/rules/LoneWolfRules.kt` | **Il gioiello.** 108 righe, Tabella dei Risultati di Combattimento completa (rapporti −10…+10, tiro 0–9, KILL_DAMAGE), rapporto di forza, gradi Kai. Unica dipendenza da rivedere: `LocalizedText` nel `CombatRoundResult` (in Ex il testo localizzato sparisce, il log diventa dato strutturato per Gemma) |
| `engine/GameRulesEngine.kt` | L'interfaccia del contratto regole (`resolveCombatRound`, `canUseDiscipline`, `getKaiRank`). Ben commentata, piccola, giusta. Stessa nota su `LocalizedText` |
| `service/TtsService.kt` | 4,7 KB, TTS di sistema con selezione voce per genere/lingua. Già previsto dal design: riuso quasi invariato |
| `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` | Il tema scuro di v1 (da tenere per decisione di design). **Nota importante: sono tema Compose** — vedi §6, Scoperte |
| `res/values/colors.xml`, `themes.xml` | Piccoli e standard |
| `res/xml/backup_rules.xml`, `data_extraction_rules.xml` | Boilerplate Android standard |
| `util/ThemePreferences.kt`, `TtsPreferences.kt`, `SavePreferences.kt` | Preferences piccole e autonome; SavePreferences va sfoltita dei percorsi legati al vecchio formato scene |

## 2. COPIARE CON POTATURA (il file serve, ma dentro c'è da tagliare)

| File | Cosa tenere / cosa tagliare |
|---|---|
| `data/GameData.kt` | È il cuore dei modelli dati (34 tra data class ed enum). **Tenere:** `StatModifier` + `ModifierSourceType` + `ModifierDuration` + `ComputedStats` (la centralizzazione dei modificatori, decisione chiusa di design), `LoneWolfStats`, `GameItem`/`ItemType`/`WeaponType`, `GameCharacter`, `CombatState`, `Scene`/`NarrativeChoice`/`ChoiceCondition`/`RequiredFlag`/`DisciplineChoice`/`ItemChoices` (base di partenza per lo schema canovacci), `KaiDisciplineInfo`, `SessionData`, `CharacterID`. **Tagliare:** `LocalizedText` (multilingua ora via prompt), `Genre` (decaduto con l'ambientazione fissa), `TagConfig`/`TagsConfigWrapper`/`TagParameter`/`EngineCommand` (il DSL regex muore, arriva il function calling), `SceneImage` (niente generazione immagini) |
| `util/GameStateManager.kt` | Il salvataggio/caricamento sessione. Concetto e struttura buoni; da ripulire dai riferimenti al vecchio formato e ai campi decaduti |
| `view/CharacterSheetViewModel.kt` + `CharacterSheetActivity.kt` | La scheda personaggio è da riportare "con lo stesso design" (decisione chiusa). Il layout e la logica di presentazione si salvano in buona parte; da potare i riferimenti al MainViewModel e al multilingua |
| `worker/DownloadWorker.kt` + `util/Downloadable.kt` + `util/FileHelper.kt` | Il download del modello serve ancora (Gemma va scaricata al primo avvio). Da adattare a un solo modello e a LiteRT-LM |
| `util/GemmaPreferences.kt` | Base per le preferenze del motore IA unico |

## 3. SOLO RIFERIMENTO (non si copia il codice, si tiene sott'occhio il design)

Da consultare durante la riscrittura — il valore è nel *come* risolvono il
problema, non nel codice così com'è.

| Fonte | Perché guardarla |
|---|---|
| `ui/adventure/AdventureHeader.kt` | La scena teatrale: ritratti + anello di parola (`speakingCharacterId`). L'implementazione Compose c'è già e funzionava — se la decisione UI cade su Compose, si promuove a "copiare con potatura" |
| `ui/adventure/PlayerActionBar.kt` | Il Dado del Destino: bordo oro/argento sul ritratto, tiro contestuale. Stesso discorso |
| `ui/adventure/ChatComponents.kt`, `ChoiceComponents.kt`, `AdventureDialogs.kt`, `AdventureUtils.kt` | Chat attribuita, pulsanti scelte filtrate, dialoghi. Buona base visiva, dentro c'è però logica legata al MainViewModel da non trascinare |
| `SetupActivity.kt` + `SetupViewModel.kt` | Flusso di creazione personaggio (tiri, discipline, arma). Il flusso resta, il codice va snellito (via genere/lingua doppia) |
| `MainActivity.kt` | Menu principale: da 5 voci a 3. Più semplice riscriverla guardandola |
| `DeathActivity.kt` | Piccola e carina, la schermata di morte. Riscrivibile in mezz'ora, ma il design c'è |
| `engine/GameLogicManager.kt` | Caricamento scene da JSON con cache e `usedScenesInSession` (il seme dell'anti-ripetizione!). In Ex diventa il caricatore di *pacchetti libro*: struttura diversa, idee buone |
| `util/StringTagParser.kt` + `assets/config.json` + `TAGS.MD` + `DOC/nuovi tag.md` | **La mappa del DSL dei tag.** Non si copia nulla (il parsing regex muore), ma è la specifica di partenza per definire le funzioni del function calling di Gemma: ogni tag = una funzione. Documento di lavoro per la roadmap punto 2 |
| `DOC/check implementazioni meccaniche di gioco.md` | Il design del combattimento (CombatState, loop round, effetti speciali): è la specifica del futuro `CombatManager` |
| `DOC/scene/test_combat.json`, `test_combat_evasion.json` | Le scene di test che definiscono la struttura dati del combattimento: base per lo schema canovacci |
| `PROMPT.txt` (55 KB, radice) | I prompt ETL di estrazione dai libri: riferimento prezioso per il flusso "master" di creazione libri con Claude Code |
| `DOC/LIBRI/*.htm` (5 libri) | I libri originali di Lupo Solitario: materiale di consultazione per scrivere i canovacci del primo libro di test (nel flusso master, non nell'app) |
| `test/StringTagParserTest.kt` | Il parser muore, ma è il modello di *come* testare: le regole pure di Ex meritano la stessa cura |
| `app/build.gradle.kts` | Riferimento per versioni e dipendenze da cui ripartire (togliendo MediaPipe, ML Kit, moduli nativi) |

## 4. ABBANDONARE (non copiare, non guardare)

| Cosa | Perché |
|---|---|
| `stdf/` intero (420 MB) | Generazione immagini: decisione chiusa. Con esso: `StdfGenerationActivity`, `StdfModelActivity`, `StdfViewModel`, tutto `stdf/*` in Kotlin, `ImageGenerationPreferences.kt` |
| `llama/` + `engine/LlamaCppEngine.kt` + `util/LlamaPreferences.kt` | Doppio motore: decisione chiusa |
| `engine/InferenceEngine.kt` | L'astrazione multi-motore non serve più con un motore solo (LiteRT-LM avrà la sua interfaccia minima) |
| `engine/TranslationEngine.kt` | ML Kit: decisione chiusa |
| `engine/GemmaEngine.kt` | Attenzione: sembra riusabile ma è scritto su **MediaPipe LLM Inference API** (maintenance-only). Con LiteRT-LM l'API cambia: si riscrive. Al più, riferimento per il ciclo di streaming/token |
| `view/MainViewModel.kt` (77 KB!) | Il monolite del post-mortem: 1400 righe di cui gran parte commentate. **Non aprirlo nemmeno per "salvare qualcosa"**: tutto ciò che vale è già stato estratto nelle voci sopra |
| `ModelActivity.kt` (46 KB), `ConfigurationActivity.kt`, `ui/configuration/*`, `util/ModelPreferences.kt`, `EnginePreferences.kt` | Gestione multi-modello/multi-motore: con un solo modello si riduce a una schermata semplice da riscrivere |
| `assets/scenes.json` (410 KB, 350 scene) | La conversione uno-a-uno di *Flight from the Dark*: decisione chiusa. (Gli originali HTML in DOC/LIBRI bastano come riferimento) |
| `tools/Merger.kt`, `tools/ValidationScript.kt` | Pipeline di fusione/validazione del vecchio formato scene con data class duplicate. L'idea della validazione rinasce nel punto 2 della roadmap, ma su schema nuovo |
| `ANALISI.MD`, `ROADMAP.MD`, `ROADMAP_IT.MD` | Documenti-fotografia di v1: superati dal DESIGN vivo di Ex |

---

## 5. Risorse grafiche (`res/`) — verdetto e avvertenza

**Da copiare** (sono gli "asset statici curati" previsti dal design):

- **Ritratti**: `portrait_hero_male.jpeg`, `portrait_hero_female.jpeg`,
  `portrait_dm.jpeg`, `portrait_elara.jpeg` (candidata Compagna!),
  `portrait_mage.jpeg`, i ritratti `class_*` (8 file) se la creazione
  personaggio mostrerà le classi
- **Icone di gioco**: armi (`ic_sword`, `ic_axe`, `ic_mace`, `ic_broadsword`,
  `ic_staff`, `ic_fists`), `ic_armor`, `ic_helmet`, `ic_potion`, `ic_gold`,
  `ic_meal`, `ic_backpack`, `ic_map`, `ic_enemy_placeholder`, `ic_unknow`,
  `ic_info`, più i piccoli XML (`ic_add`, `ic_remove`, `ic_unknown_item`)
- `lupo_solitario.png`, `map_dungeon.jpg`
- `mipmap-*` (icone launcher) se si vuole conservare l'identità visiva

**⚠️ Avvertenza pesi — ottimizzare PRIMA di copiare.** La cartella drawable
pesa 43 MB per ~40 file. I casi peggiori:

| File | Peso attuale | Peso ragionevole |
|---|---|---|
| `ic_map.png` + `ic_map_icon.png` (duplicati!) | 4,3 MB × 2 | un solo file, ~100–200 KB |
| `ic_backpack.png` | 4,2 MB | ~100 KB |
| `ic_axe.png`, `ic_gold.png`, `ic_meal.png` | 2,5–3,3 MB | ~50–100 KB |
| Ritratti jpeg | 1,2–2,1 MB l'uno | 200–400 KB a 1024px |

Sono icone da barra giocatore salvate a risoluzione da poster: ridimensionarle
(WebP, dimensioni reali d'uso) porta i 43 MB sotto i 4–5 MB. Da fare come
passaggio unico prima della copia — è anche il primo compito perfetto da
delegare a Claude Code con uno script.

**Non copiare**: `ic_hero_portrait_placeholder.jpeg` (duplicato esatto di
`portrait_hero_male.jpeg`, 1,8 MB), uno dei due `ic_map*.png`.

`res/values/strings.xml` di v1 ha solo 17 righe: si riparte da zero senza
perdere nulla.

---

## 6. Scoperte notevoli emerse dall'analisi

1. **v1 era interamente Jetpack Compose.** Tema Compose, AdventureHeader,
   PlayerActionBar, ChatComponents: tutta l'esperienza UI accumulata (inclusi
   anello di parola e Dado del Destino già implementati) è in Compose. La
   decisione aperta "Compose vs XML" (§13 del DESIGN) ha ora un argomento
   forte: **scegliendo Compose si promuovono a riusabili interi componenti
   della scena teatrale**; scegliendo XML si riparte da zero sulla UI.
2. **`GameLogicManager` conteneva già `usedScenesInSession`**: l'idea
   dell'anti-ripetizione delle scene esisteva in embrione. Conferma che il
   randomizzatore di Ex ha radici solide.
3. **`portrait_elara.jpeg` esiste già**: un ritratto femminile pronto per il
   primo Compagno.
4. **PROMPT.txt** contiene la pipeline ETL di estrazione dai libri, matura e
   dettagliata: mezza specifica del flusso master è già scritta.
5. **I due file `ic_map.png` e `ic_map_icon.png` sono identici** (stesso
   byte count): piccolo esempio del disordine da cui Ex vuole scappare.

---

## 7. Piano di copia pratico (ordine suggerito)

1. **Script di ottimizzazione immagini** (Claude Code): ridimensiona e
   converte le drawable selezionate → cartella `assets-ottimizzati`
2. Copia nel nuovo progetto, appena esiste lo scheletro Android (roadmap
   punto 4): `LoneWolfRules.kt`, `GameRulesEngine.kt`, `TtsService.kt`,
   tema, preferences superstiti — cambiando package
3. `GameData.kt` **potato** secondo §2 (farlo subito dopo, perché tutto
   dipende dai modelli dati)
4. Risorse ottimizzate in `res/drawable`
5. In una cartella `docs/riferimenti-v1/` del nuovo repo (o fuori repo):
   TAGS.MD, `nuovi tag.md`, `check implementazioni meccaniche di gioco.md`,
   `test_combat*.json`, PROMPT.txt — il materiale di consultazione per i
   punti 2 e 3 della roadmap
6. Tutto il resto di v1 resta dov'è: il repo vecchio non si tocca e non si
   importa

---

## Conto finale

| Verdetto | File Kotlin | Note |
|---|---|---|
| Copiare quasi pari pari | 9 | + risorse ottimizzate |
| Copiare con potatura | 7 | |
| Solo riferimento | ~15 | + DOC e PROMPT.txt |
| Abbandonare | ~29 | + interi moduli stdf e llama (420 MB) |

Dei 637 MB di v1, a Ex servono: **~30 KB di Kotlin d'oro** (regole, interfaccia,
TTS, modelli dati potati), **4–5 MB di immagini dopo ottimizzazione**, e
**~2 MB di documentazione di riferimento**. Tutto il resto era il peso che ha
fatto collassare v1 — e resta lì.
