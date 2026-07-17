# Specifica 6 — Analisi criticità

Stato: **CHIUSA** (sessione 17/07/2026). Ultima specifica di design.

Formato: per ogni criticità — analisi, mitigazione di design (decisa),
cosa misurare in sviluppo e soglia di allarme. Device di riferimento:
Motorola Razr 70 Ultra (Snapdragon 8 Elite, 16 GB RAM).

Decisioni di piattaforma prese qui:
- **Modello: Gemma 3 4B** (lo stesso di v1, già provato e funzionante
  sul flusso reale), via LiteRT-LM. Contesto di riferimento: 10240
  token (config di v1).
- **Inferenza SENZA MEMORIA, una sessione nuova per ogni scena**: il
  contesto è sempre e solo frammenti fissi + coda scena precedente +
  scena attuale + scelte; a fine generazione la sessione si scarta.
  Gemma non deve ricordare ciò che ha scritto: la continuità la porta
  `previous_scene_text`. Questa è la taratura del motore di inferenza
  di Ex (in v1 la LlmInferenceSession rischiava di accumulare storia).

---

## Criticità maggiori

### C1 — Dimensione del prompt vs contesto
**Analisi**: con l'inferenza per-scena senza memoria il prompt è
limitato per costruzione: frammenti fissi (~centinaia di token) +
UN testo di scena precedente + scena + scelte. Su 10240 token il
margine è ampio.
**Mitigazione**: la regola stessa della sessione per-scena. Il diario
completo NON entra MAI nel prompt (regola anti-regressione). Valvola
di sicurezza silenziosa nel PromptBuilder: se il prompt supera un
budget prudenziale, si passa solo la coda di previous_scene_text
(guardia da ingegnere, non feature).
**Misura**: token del prompt sulle 10 scene più lunghe del libro 1
convertito. Allarme: > 60% del contesto.

### C2 — Velocità di inferenza (la criticità madre)
**Analisi**: latenza primo token e token/s determinano se il gioco
sembra vivo. Gemma 3 4B su Snapdragon 8 Elite via LiteRT-LM è
territorio noto ma va misurato sul flusso reale (prompt ~1-2k token,
output ~300-600 token).
**Mitigazione (già in design)**: streaming in UI (la percezione batte
la velocità); modello caricato una volta e tenuto in memoria tra le
scene; degradazione a testo originale del pacchetto se l'inferenza
fallisce (il gioco non si blocca mai).
**Misura**: primo token, token/s, RAM, tempo di caricamento modello.
Allarme: primo token > 3s; token/s sotto la velocità di lettura.

### C3 — Termico e batteria
**Analisi**: pattern a raffiche (un'inferenza per transizione), non
continuo — ma il Razr è un pieghevole e smaltisce peggio. Rischio:
throttling dopo 30' → inferenze degradanti.
**Mitigazione**: il pattern a raffiche stesso; se necessario, in
futuro: taglia modello inferiore o pausa termica.
**Misura**: sessione di gioco 30-45', temperatura e curva token/s.
Allarme: degradazione > 30% a fine sessione.

## Criticità medie (gestite dal design, da verificare)

### C4 — Streaming in Compose
Ricomposizione a ogni token = jank sui testi lunghi. **Mitigazione**:
buffer con aggiornamento UI ogni ~80-100 ms, non per token.
**Misura**: frame time durante streaming su scena lunga.

### C5 — Robustezza del parser pipe
Output Gemma malformato. **Mitigazione (già in design)**: fallback per
conteggio ai testi originali; il parsing fallito non blocca mai.
**Misura/pratica di sviluppo**: suite di fixture con output REALI di
Gemma raccolti durante lo sviluppo, inclusi i malformati.

### C6 — Salvataggio corrotto
Crash a metà scrittura JSON. **Mitigazione (obbligo di piano)**:
scrittura ATOMICA — file temporaneo + rename — per auto-save e
checkpoint. **Misura**: kill del processo durante il save, il file
precedente deve sopravvivere.

### C7 — Primo avvio e gestione modelli
Il download del modello (GB) è il vero onboarding. **Mitigazione**:
UX di attesa onesta nella sezione modelli della Home (progresso,
dimensione, resume); il gioco senza modello gira in modalità testo
originale. **Misura**: flusso completo da APK pulito.

## Non-criticità (liquidate con una misura di conferma)

- **C8** Pacchetto 350 scene: ~0,5-1 MB JSON, parsing una tantum.
- **C9** Diario su libro lungo: centinaia di KB di testo.
- **C10** Auto-save a transizione: scrittura asincrona, millisecondi.
- **C11** Toggle originale/tradotto: due stringhe in RAM per scena.
- **C12** Animazione dado su Snapdragon 8 Elite: nessun rischio.

Ciascuna: una misura di conferma durante lo sviluppo, poi archiviata.

## Nota di piattaforma per il piano di sviluppo

LiteRT-LM è Android: l'implementazione dell'`InferenceEngine` per
l'app vive nel modulo Android; l'interfaccia resta nel codice
condiviso. Il tool desktop usa la SUA implementazione (API/endpoint
locale per la rifinitura ETL). Stessa porta, due prese — coerente con
l'architettura.
