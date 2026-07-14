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

**Decisioni** (vedi changelog README §15 per il dettaglio):
Prosa finita mantenuta nei pacchetti al posto dei canovacci; Gemma arricchisce
e raccorda scena precedente/attuale/continuazioni con prompt stateless senza
chat history; riuso integrale del `config.json` di v1 (sistema
`promptDescription`), con i tag D&D da sostituire con le Discipline Kai.

**Prossimi task** (sostituiscono i precedenti):
1. [MICHELE] Definire la struttura scena definitiva partendo dai JSON di v1:
   quali campi restano, quali si tolgono (doppia lingua), quali si aggiungono
   (`toneHints` se manca) — su `doc/SCHEMA-PACCHETTO.md`
2. [MICHELE] Bozza dell'estensione di `start_adventure_prompt` in
   `config.json`: i nuovi frammenti (`previousSceneText`, `continuationsText`,
   `constraintText`) — testo dei prompt, niente codice
3. Copia di `config.json` da v1 a Ex + sostituzione tag D&D con Discipline Kai
   (delegabile a Claude Code dopo il task 2)
4. Script ottimizzazione immagini v1 (invariato, delegabile quando si vuole)