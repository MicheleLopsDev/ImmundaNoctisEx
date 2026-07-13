# Immunda Noctis Ex — Documento di Progetto

> Motore GDR/libro-game per Android, narrato da IA locale, interamente offline.
> Progetto nuovo (repo separato), nato dall'esperienza di Immunda Noctis v1.

Ultimo aggiornamento: 13 luglio 2026

---

## 1. Visione

Un motore di avventure testuali in stile libro-game/GDR in solitaria. Il giocatore
sceglie genere e stile narrativo, il motore Kotlin gestisce personaggio, regole e
stato di gioco, un modello IA locale (Gemma 4) si occupa esclusivamente della
narrazione e dei dialoghi degli NPC. Tutto gira sul dispositivo, senza connessione
di rete richiesta durante il gioco.

## 2. Continuità con Immunda Noctis v1

Questo non è un fork: è un progetto nuovo per non "sporcare" quanto già fatto. Alcuni
concetti di v1 vengono ripresi, altri abbandonati deliberatamente per ridurre la
complessità che aveva bloccato lo sviluppo la volta scorsa.

### Riusato (concettualmente, non necessariamente come codice)
- Modello dati `Scene` / `GameCharacter` / `CharacterStats`, adattato alle 5
  caratteristiche nuove.
- Il sistema di tag in `config.json` che fa da ponte tra output dell'LLM e comandi
  del motore — da far evolvere verso il **function calling nativo** di Gemma 4.
- `TtsService.kt` — TTS di sistema Android, selezione voce per genere/lingua
  personaggio. Riusabile quasi invariato.

### Abbandonato
- Architettura a doppio motore (MediaPipe + llama.cpp/GGUF con bridge C++
  compilato a mano). Causa principale della complessità ingestibile in v1.
- `TranslationEngine.kt` (ML Kit). Sostituito dalla generazione multilingue nativa
  di Gemma 4: si passa la lingua desiderata nel prompt invece di tradurre a
  posteriori. Le stringhe fisse dell'interfaccia restano su `strings.xml` standard,
  senza passare per l'IA.
- `scenes.json` come narrativa fissa pre-tradotta (l'adattamento di *Flight from the
  Dark*). Sostituito da canovacci generici randomizzabili, indipendenti dal genere.

## 3. Hardware di riferimento

Dispositivo di test: **Motorola Edge 70** — Snapdragon 7 Gen 4, 12 GB RAM, GPU Adreno
722. Fascia medio-alta, non flagship: le aspettative di prestazioni vanno calibrate
su questo device, non sui benchmark ufficiali Google (che usano hardware flagship).

## 4. Stack tecnico

| Area | Scelta |
|---|---|
| Linguaggio | Kotlin |
| Motore IA | Gemma 4 E2B (valutare E4B dopo test prestazionali) |
| Runtime IA | **LiteRT-LM** (non MediaPipe LLM Inference API, ora in maintenance-only) |
| Ponte narrazione↔motore | Function calling nativo di Gemma 4 / LiteRT-LM |
| UI | Da decidere — Compose vs XML (vedi §7) |
| TTS | `android.speech.tts` nativo di sistema |
| Traduzione | Nessuna libreria dedicata — multilingua gestito da Gemma 4 nel prompt |

## 5. Architettura logica

Separazione netta di responsabilità:

- **Motore di gioco (Kotlin)** — autorità assoluta. Gestisce stato, personaggio,
  inventario, regole, tiri di dado, esito di sfide e combattimenti.
- **Narratore (Gemma 4)** — riceve dal motore un contesto strutturato (evento,
  genere, stile, lingua, storico compresso) e restituisce narrazione e dialoghi
  NPC. Può invocare funzioni esposte dal motore invece di scrivere tag testuali.

Il motore comanda, l'LLM racconta. L'LLM non decide mai l'esito di una regola.

## 6. Genere e stile narrativo

Variabile scelta dal giocatore a inizio partita (fantasy, horror, fantascienza,
ecc.), passata come parte del system prompt. Influenza il tono della narrazione di
Gemma e, potenzialmente, quali regole/eventi sono attivi.

## 7. Personaggio (bozza — da formalizzare)

- 5 caratteristiche base: Forza, Destrezza, Astuzia, Saggezza, Intelligenza —
  **da confermare i nomi esatti**, in particolare se "Astuzia" o un'altra
  caratteristica tipo Carisma.
- Dado: d20.
- Livelli di caratteristica → bonus: **formula ancora da definire**.
- Possibile compagno/alleato incontrabile durante l'avventura.

## 8. Combattimento

**Non ancora formalizzato.** Era il punto di blocco principale di v1. Va risolto
nel documento delle regole prima di scrivere qualsiasi codice legato al combattimento.

## 9. Scene

- JSON strutturato, scene generiche e modulari (non narrativa già scritta).
- Randomizzazione dell'ordine, mantenendo fisse solo scena iniziale e finale.
- Canovaccio generico + stile scelto → Gemma genera la narrativa specifica
  (es. "il personaggio affronta un ostacolo" → spettro in horror, magia oscura in
  fantasy).

## 10. Roadmap

1. Documento delle regole di gioco (caratteristiche, dado, risoluzione sfide,
   combattimento) — **in corso**
2. Schema `scenes.json` v2 (canovacci generici)
3. Scheletro progetto Android (dipendenze LiteRT-LM, struttura moduli)
4. Motore di gioco Kotlin (stato, regole) — senza IA, testabile da solo
5. Integrazione Gemma 4 via LiteRT-LM
6. Function calling / ponte narrazione↔motore
7. UI (Compose o XML — decisione pendente)
8. TTS
9. Test prestazionali reali su Motorola Edge 70

## 11. Decisioni aperte

- [ ] Compose vs XML per la UI
- [ ] E2B vs E4B, dopo test prestazionali reali sul device
- [ ] Nomi definitivi delle 5 caratteristiche
- [ ] Formula del bonus da livello caratteristica
- [ ] Meccanica di risoluzione del combattimento

## 12. Changelog

- **13/07/2026** — Creazione documento. Prima bozza basata sull'analisi di
  fattibilità e sulla revisione del codice di Immunda Noctis v1.
