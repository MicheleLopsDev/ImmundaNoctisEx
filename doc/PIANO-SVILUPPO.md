# Piano di Sviluppo — ImmundaNoctisEx

Ultimo atto del design (17/07/2026). Questo documento ORCHESTRA le
sessioni di sviluppo: dice cosa leggere, cosa costruire, in che ordine,
e chi fa cosa. Una sessione nuova parte da qui.

---

## Come usare questo documento (per la sessione che legge)

1. Leggi `CLAUDE.md` (regole operative) e questo file PER INTERO.
2. NON leggere subito tutte le specifiche: usa la Mappa dei documenti
   qui sotto per aprire SOLO quelle che servono al task corrente.
3. Individua la fase corrente dal `doc/DIARIO.md` (ultima voce) e
   lavora SOLO su task di quella fase. Non anticipare fasi.
4. Ogni task: codice + test + commit atomico + voce a diario.
5. Se una specifica è ambigua o due specifiche sembrano in conflitto:
   FERMATI e chiedi a Michele. Non inventare, non "interpretare".
6. I task marcati **[MICHELE]** sono riservati a Michele: non
   implementarli; se bloccano un tuo task, prepara l'impalcatura
   (interfaccia/segnaposto) e segnala a diario che aspetti.

## Contesto in dieci righe

ImmundaNoctisEx è un motore GDR/libro-game Android nativo con IA
locale: libri a scelte strutturate (niente testo libero) dove Gemma 3
4B (LiteRT-LM, on-device) ARRICCHISCE E TRADUCE la prosa già scritta
del libro — non genera da zero. Ambientazione Lupo Solitario (regole
canoniche: CRT, discipline Kai, Resistenza/Combattività). Device
target: Motorola Razr 70 Ultra. Il progetto è il successore di v1
(ImmundaNoctis-master): v1 si è arenato su separazione UI/logica e
conversione libri — l'intero design di Ex esiste per non ripetere
quei due blocchi. Sorgenti v1 = miniera di riuso, MAI dipendenza.

## Mappa dei documenti (cosa leggere per cosa)

| Domanda | Documento |
|---|---|
| Regole operative sessioni, commit, stile | `CLAUDE.md` |
| Moduli, interfacce, dove vive ogni cosa | `doc/ARCHITETTURA.md` |
| Combat, discipline, comandi, globalRules, dado | `doc/REGOLE.md` |
| SessionData, checkpoint, diario-grafo, inventario | `doc/STATO.md` |
| Schermate, scena teatrale, convenzioni UI | `doc/UI.md` |
| Tool di conversione, pipeline, fonti, legale | `doc/ETL.md` |
| Vincoli di piattaforma, misure, soglie | `doc/CRITICITA.md` |
| Formato scene/manifest | `content/scenes.sample.json` |
| Tag meccaniche e frammenti prompt | `content/config.json` |
| Cosa è riusabile da v1 | `doc/MATERIALE-REGOLE-V1.md`, `doc/ANALISI-FLUSSO-PROMPT-V1.md`, `doc/INVENTARIO-ASSET.md` |
| Cosa è riusabile dalla UI v1, convenzione @Preview | `doc/ANALISI-UI-V1.md` |
| Storia delle decisioni | `doc/DIARIO.md` |

## Vincoli non negoziabili (valgono per ogni riga di codice)

1. `:core:engine` e `:core:data`: **zero dipendenze Android**,
   testabili da terminale.
2. File ~200 righe = soglia d'allarme; una classe = una responsabilità.
3. **Il gioco non si blocca mai**: ogni fallimento (parsing, inferenza,
   config) degrada sul testo/comportamento originale del pacchetto.
4. **Si serializzano i fatti, i bonus si calcolano** (mai persistere
   valori derivati).
5. **Inferenza senza memoria**: una sessione Gemma nuova per scena;
   il diario non entra MAI nel prompt.
6. **Scrittura atomica** di auto-save e checkpoint (temp + rename).
7. Interfacce SOLO le quattro motivate: RulesEngine, InferenceEngine,
   DiceRoller, PackageSource (+ le porte di persistenza analoghe).
   Il resto: classi concrete.
8. ID canonici nei dati (discipline UPPER_SNAKE, role enum); nomi
   localizzati SOLO in strings.xml.

## Principio v0.1: PRIMA FUNZIONA, POI È BELLO

Volontà esplicita di Michele: primo passaggio funzionale, abbellimento
grafico dopo. In pratica:

- Fino alla Fase 6 la UI usa componenti Material di default, colori
  piatti, ritratti/sfondi segnaposto, dado senza animazione (un
  bottone che mostra il numero). Nessun tempo speso in estetica.
- La Fase 7 (Abbellimento) è DEDICATA alla grafica: lì si applica
  l'estetica di `doc/UI.md` (tema scuro, banner, cerchio d'oro, dado
  animato). Non si anticipa.
- Eccezione: la STRUTTURA delle schermate (fasce, zone, navigazione)
  si costruisce subito giusta — è architettura, non estetica.

## Struttura moduli Gradle (Kotlin Multiplatform)

```
:core:data     KMP — modelli, schema, caricamento pacchetti, validatori
:core:engine   KMP — GameState, regole, combat, comandi, stat calcolate
:app           Android — UI Compose, inference LiteRT-LM, TTS, storage
:tool          Compose Desktop — ETL (Fase 6)
```

---

## Fasi e milestone (ordine vincolante)

### Fase 0 — Fondamenta
Progetto Gradle KMP con i 4 moduli, kotlinx.serialization, struttura
test. **Milestone: `./gradlew test` verde su moduli vuoti; l'app
scheletro parte sul Razr.**

### Fase 1 — `:core:data`
Modelli (Scene, Manifest, GameItem, Character, SessionData,
JourneyEntry...), caricamento pacchetto via PackageSource, VALIDATORI
(grafo chiuso, discipline canoniche, combat con winSceneId, intervalli
rollOnItemTable, warning globalRules non-ENDING).
**Milestone: il sample viene caricato e validato da un test JVM;
un pacchetto rotto viene bocciato con messaggi chiari.**
[MICHELE] enum WeaponType e KaiRank (soglie da REGOLE.md §Blocco 3).

### Fase 2 — `:core:engine`
GameState, funzione unica stat effettive, inventario con limiti,
i 18 comandi (15 di v1 + removeItem, checkItemAndJump,
rollOnItemTable), globalRules + morte built-in, HEALING passiva,
HUNTING/EAT_MEAL, CombatManager (round puro, rapido come loop,
macchina a stati del completo, evasione con costo), DiceRoller
iniettato.
**Milestone: una partita completa del sample SIMULATA DA TERMINALE
(test JVM che percorre il grafo, combatte, muore, vince). Fixture
CRT con il caso off-by-one del tiro 0.**
[MICHELE] trascrizione della tabella CRT da LoneWolfRules di v1
(copia meccanica, occhio umano prezioso) + fixture JSON di test
scritte a mano (test_combat, test_skillcheck...).

### Fase 3 — MILESTONE REGINA: il libro gira senza Gemma
App Android minima: Home essenziale → caricamento sample → si gioca
TUTTO (scelte, discipline, combat a due modalità, inventario
operativo, morte, checkpoint, auto-save atomico, diario) con i TESTI
ORIGINALI del pacchetto, UI brutta ma completa nelle zone.
**Milestone: partita completa sul Razr, chiusura app a metà e
ripresa, morte in IRON che cancella. SENZA alcun modello IA.**
Da qui in poi l'IA è un arricchimento su fondamenta solide.
[MICHELE] strings.xml italiano (testi UI, feedback combat, gradi Kai).

### Fase 4 — `inference`
LiteRT-LM in :app dietro InferenceEngine, PromptBuilder (frammenti da
config con default hardcoded), sessione-per-scena, ResponseParser
(pipe CHOICE/DISCIPLINE/ENEMY + fallback per conteggio), TagParser,
streaming bufferizzato ~80-100ms fino a `--- TAGS ---`.
**Milestone: la scena arriva arricchita e tradotta in streaming;
LE MISURE di CRITICITA.md (primo token, token/s, prompt token,
termico) eseguite e annotate a diario. Ogni output reale di Gemma
salvato come fixture.**

### Fase 5 — UI funzionale completa
Le 7 schermate di UI.md in forma funzionale: creazione (lupo/lupa,
dado, discipline, specializzazione), scena teatrale con zone e
trasformazione combat, scheda a due tab, diario a due viste +
export, opzioni con TTS per genere, gestione modelli.
**Milestone: l'intero flusso utente esiste; l'estetica no.**

### Fase 6 — `:tool` ETL
Wizard: downloader user-initiated → parser XML Aon → convertitore
mechanics KC → rifinitura LLM opzionale → validatori condivisi →
simulazione col motore → review human-in-the-loop.
**Milestone: il LIBRO 1 convertito, validato, simulato e GIOCATO
sul Razr. Poi 2 e 3.**
[MICHELE] revisione human-in-the-loop del libro 1 (è SUO per design).

### Fase 7 — Abbellimento
L'estetica di UI.md: tema, banner con ritratti e cerchio d'oro, dado
animato, transizioni, zaino disegnato, asset da v1 (branch develop)
ottimizzati WebP. **Milestone: l'app è bella quanto era nel
screenshot di v1 — ma sotto c'è Ex.**

---

## I task di Michele — regole

Michele scrive codice su task PICCOLI e BEN DELIMITATI, marcati
[MICHELE] per fase (enum canonici, tabella CRT, fixture di test,
strings.xml, revisione ETL). Claude Code: non implementarli; se
bloccano, impalca e segnala. Nuovi task adatti emergeranno: il
criterio è "delimitato, senza dipendenze intrecciate, con feedback
visibile" — proporli a diario marcandoli [MICHELE-PROPOSTO].

## Prima sessione di sviluppo — checklist

1. Leggi CLAUDE.md + questo file. 2. Verifica ambiente (JDK, Android
Studio con plugin KMP — già installato). 3. Fase 0: struttura Gradle
KMP 4 moduli. 4. Commit "build: struttura moduli KMP". 5. App
scheletro sul Razr. 6. Diario: fase 0 chiusa, prossimo task Fase 1.
7. AGGIORNA CLAUDE.md aggiungendo: riferimento a questo piano, fase
corrente, vincoli non negoziabili in forma breve.
