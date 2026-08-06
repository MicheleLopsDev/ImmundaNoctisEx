# CLAUDE.md

## Lingua
Rispondi sempre in italiano. I commenti nel codice devono essere scritti in italiano.

## Progetto
ImmundaNoctisEx è un motore GDR/libro-game per Android nativo, sviluppato in Kotlin,
con integrazione di un modello IA locale (Gemma 4 via LiteRT-LM). L'ambientazione è
quella di Lupo Solitario (Lone Wolf).

## Documentazione di riferimento
Il documento di riferimento per le decisioni di design è `README.md`. Consultalo
quando serve contesto sulle scelte architetturali o di design del progetto.

## Documentazione
Tutti i documenti di progetto vivono in `doc/`, ad eccezione di `README.md`
(visione e design, resta in radice) e di questo stesso `CLAUDE.md`.

Per orientarsi nel codice ci sono due mappe, scritte per chi arriva da
fuori: `doc/CLASSI-CLIENT.md` (gioco: `:core:data`, `:core:engine`,
`:app`) e `doc/CLASSI-EDITOR.md` (editor e CLI: `:tool`). Dicono cosa
fa ogni classe importante e perché è fatta così; le decisioni di design
restano in `doc/ARCHITETTURA.md`.

`content/test-books/` contiene i libri di gioco versionati: uno per
feature da verificare, più `scenes.sample.json` (il libro incluso
nell'APK e caricato di default — `AppContainer` lo apre come asset
`test-books/scenes.sample.json`, cambiarne il percorso rompe l'avvio).
Elenco commentato in `doc/LIBRI-DI-PROVA.md`, si portano sul telefono
con `./gradlew pushTestBooks`. `scenes.json` (libro Project
Aon, uso locale) non è versionato.

`doc/LIBRI/` contiene i cinque libri Lupo Solitario **senza la prosa**:
solo le meccaniche, perché la licenza Project Aon vieta di
redistribuire i testi (README §15). Si riempiono in locale con
`./gradlew :tool:riempiTesti` e si verificano contro il grafo ufficiale
dei percorsi con `./gradlew :tool:verificaGrafo`.

Per creare un libro NUOVO da un canovaccio con l'aiuto di un'IA remota
c'è `doc/PROMPT-GENERAZIONE-LIBRI.md`: due prompt pronti e lo schema
JSON minimo, verificato contro il validatore. I vincoli di forma che
quei prompt impongono (uscite per scena, riconvergenza, finali) non
sono inventati: vengono da `doc/FORMA-DEI-GRAFI.md`, la misura dei 29
grafi ufficiali della serie.

In `doc/ANALISI-RIUSO-V1.md` si trova l'analisi di riuso dal vecchio progetto v1:
consultala prima di copiare o riscrivere qualunque componente ereditato.

## Sviluppo
Design concluso (6/6 specifiche). Lo sviluppo è orchestrato da
`doc/PIANO-SVILUPPO.md`: leggilo per intero a inizio sessione, individua
la fase corrente da `doc/DIARIO.md` e lavora solo su quella (non
anticipare fasi). Apri le altre specifiche solo quando il piano lo
richiede per il task in corso.

Fase corrente: **client sostanzialmente completo** (aggiornato
27/07/2026). Fase 3 chiusa il 17/07 (il libro gira sul Razr senza
Gemma) e Fase 4 chiusa nei fatti (Gemma genera davvero sul device,
misure di CRITICITA.md tutte raccolte). Buona parte della Fase 5 (UI
funzionale) e della Fase 7 (abbellimento: sfondi, icone, animazioni,
colori) sono state completate fuori dall'ordine rigido del piano,
guidate sessione per sessione da Michele — nessun bug bloccante noto,
239 test verdi su tutti i moduli. Restano aperti, non bloccanti: un
leak di ~140MB/partita di memoria nativa (rinviato consapevolmente,
non si sente su 15,5GB di RAM anche su partite ripetute), la verifica
di un motore GGUF alternativo a LiteRT-LM (vedi `doc/UPGRADE.md` §3),
e l'audit delle ~100 stringhe UI ancora scritte a mano nel codice
invece che in `strings.xml` (solo 3 file su tutta la UI usano
`stringResource`, contro 107 voci già pronte in `strings.xml`).
Prossimo grande capitolo: il tool di conversione/authoring libri
(`doc/ETL.md`, Fase 6) — piano dettagliato ancora da ricevere da
Michele, non ancora iniziato (modulo `:tool` esiste solo come
scheletro Gradle vuoto).

Vincoli non negoziabili (dettaglio in `doc/PIANO-SVILUPPO.md`):
`:core:engine`/`:core:data` senza dipendenze Android; file ~200 righe
= soglia d'allarme; il gioco non si blocca mai (ogni fallimento degrada
sul contenuto originale); si serializzano i fatti, i bonus si
calcolano; inferenza senza memoria (sessione Gemma nuova per scena, il
diario non entra mai nel prompt); scrittura atomica di auto-save e
checkpoint; interfacce solo le quattro motivate (RulesEngine,
InferenceEngine, DiceRoller, PackageSource); ID canonici nei dati, nomi
localizzati solo in `strings.xml`.

I task marcati **[MICHELE]** nel piano sono riservati a Michele: non
implementarli. Se bloccano un task in corso, prepara l'impalcatura
(interfaccia/segnaposto) e segnalalo a diario.