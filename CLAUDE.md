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

`content/` contiene i contenuti di gioco: `config.json` (registro tag),
`scenes.sample.json` (libro di esempio, versionato). `scenes.json` (libro
Project Aon, uso locale) non è versionato.

In `doc/ANALISI-RIUSO-V1.md` si trova l'analisi di riuso dal vecchio progetto v1:
consultala prima di copiare o riscrivere qualunque componente ereditato.

## Sviluppo
Design concluso (6/6 specifiche). Lo sviluppo è orchestrato da
`doc/PIANO-SVILUPPO.md`: leggilo per intero a inizio sessione, individua
la fase corrente da `doc/DIARIO.md` e lavora solo su quella (non
anticipare fasi). Apri le altre specifiche solo quando il piano lo
richiede per il task in corso.

Fase corrente: **Fase 3 — MILESTONE REGINA: il libro gira senza Gemma**
(Fase 2, `:core:engine` completo — stat effettive, inventario, 16
comandi, transizioni, CombatSession su CRT ufficiale, milestone della
partita simulata — chiusa il 17/07/2026).

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