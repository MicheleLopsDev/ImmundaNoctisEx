# Diario di progetto

## 14/07/2026

### Sessione serale

**Fatto:**
- Analisi di `LlamaCppEngine.kt` di v1: architettura chat conversazionale,
  non adatta al nuovo modello di narrazione. Si riusa solo il token tracking
  a soglie e l'idea di interfaccia `InferenceEngine`.
- Analisi di `config.json` di v1: sistema `promptDescription` (tag dichiarativi
  con prompt come dati), riuso integrale deciso.
- README allineato alle decisioni (commit `6a702a0`).
- Creata `content/`: `config.json` spostato lì; `scenes.json` (Project Aon)
  reso locale, non versionato via `.gitignore`.
- Creato `content/scenes.sample.json`: riferimento strutturale pubblico dello
  schema E futuro libro incluso nell'APK — da espandere a mini-avventura
  completa (~10-20 scene) con nomenclatura originale prima del rilascio.

**Decisioni** (vedi changelog README §15 per il dettaglio):
Prosa finita mantenuta nei pacchetti al posto dei canovacci; Gemma arricchisce
e raccorda scena precedente/attuale/continuazioni con prompt stateless senza
chat history; riuso integrale del `config.json` di v1 (sistema
`promptDescription`), con i tag D&D da sostituire con le Discipline Kai.

**Fatto (seconda sessione serale):**
- Struttura scena definitiva chiusa in `content/scenes.sample.json`: eliminata
  la doppia lingua (`narrativeText`/`choiceText` ora stringhe inglesi
  semplici), aggiunto manifest in radice (id, version, title, description,
  language, genre, toneHints di libro), `toneHints` per-scena, campo
  `backgroundImage`, campi predisposti per-scelta (`minRoll`, `maxRoll`,
  `requiredItem`, `requiredFlag`) e `gameMechanics` per-scena.
- Discipline ricondotte alle sole 10 canoniche UPPER_SNAKE (WEAPONSKILL,
  CAMOUFLAGE, HUNTING, SIXTH_SENSE, TRACKING, HEALING, MINDSHIELD,
  MINDBLAST, ANIMAL_KINSHIP, MIND_OVER_MATTER); `SHADOWSTEP` rimosso.
- Grafo di esempio riscritto: 7 scene chiuse ("The Warehouse Letter"),
  START -> ... -> due ENDING (vittoria/morte), con un bivio a disciplina
  (SIXTH_SENSE/CAMOUFLAGE) che evita il combattimento e un ramo di
  combattimento (WIN/LOSE) sulla struttura combat già presente nel file.

**Prossimi task** (sostituiscono i precedenti):
1. [MICHELE] Bozza dell'estensione di `start_adventure_prompt` in
   `config.json`: i nuovi frammenti (`previousSceneText`, `continuationsText`,
   `constraintText`) — testo dei prompt, niente codice
2. Copia di `config.json` da v1 a Ex + sostituzione tag D&D con Discipline Kai
   (delegabile a Claude Code dopo il task 1)
3. Script ottimizzazione immagini v1 (invariato, delegabile quando si vuole)
4. Creare in `content/` una serie di file di test per meccanica (es.
   `test_choices.json`, `test_skillcheck.json`, `test_combat.json`,
   `test_disciplines.json`, `test_mechanics.json`), scene minime 2-3 l'uno,
   sul modello dei `test_*.json` di v1 — ora possibile: la struttura scena
   definitiva è chiusa (`content/scenes.sample.json`), nascono già nel
   formato giusto