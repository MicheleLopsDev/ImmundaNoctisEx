# Erede dell'Astra — cantiere

Il primo libro-game ricavato da un romanzo originale, e il caso di prova
del metodo. Le convenzioni di questa cartella sono in
[`doc/CANTIERE-DI-UN-LIBRO.md`](../../doc/CANTIERE-DI-UN-LIBRO.md); il
metodo in [`doc/DA-ROMANZO-A-LIBROGAME.md`](../../doc/DA-ROMANZO-A-LIBROGAME.md).

## Il materiale di partenza

Sei capitoli di prosa finita (7.581 parole) più quattro documenti di
supporto, scritti da Michele. In `01-romanzo/`, e **non si toccano**: è
l'unica cosa che un modello non può rifare.

Il testo porta già i marcatori che servono al gioco — `*[Inizio Scontro]*`
e `[Disciplina: Nome]` — e le dieci discipline sono già mappate sugli ID
canonici del motore dentro `mondo.md`. È il motivo per cui la Fase 0
legge e cataloga invece di indovinare.

## Una nota sul testo: convertito alla forma LIBROGAME

Il romanzo è nato in terza persona (*«Ariel scartò di lato»*). Il
07/08/2026 Michele ha chiesto di convertirlo alla forma **LIBROGAME**
(seconda persona, presente) per avere un materiale di prova già nella
forma giusta.

**È un'eccezione dichiarata, non il metodo.** La regola resta quella in
[`DA-ROMANZO-A-LIBROGAME.md`](../../doc/DA-ROMANZO-A-LIBROGAME.md): il
modello non riscrive il testo dell'autore. Qui l'ha fatto perché
l'autore l'ha chiesto esplicitamente, per un libro di prova, come lavoro
a sé stante e fuori dal processo — e con la verifica qui sotto.

| capitolo | parole prima | dopo | dialoghi | alterati |
|---|---|---|---|---|
| cap-01 | 1215 | 1210 | 24 | **0** |
| cap-02 | 1420 | 1416 | 13 | **0** |
| cap-03 | 1054 | 1043 | 8 | **0** |
| cap-04 | 1452 | 1446 | 27 | **0** |
| cap-05 | 996 | 980 | 17 | **0** |
| cap-06 | 1444 | 1428 | 20 | **0** |
| **totale** | **7581** | **7523** (−0,8%) | **109** | **0** |

Le 37 occorrenze di `[Disciplina: …]` sono tutte al loro posto; sono
stati **aggiunti** 10 marcatori `[Ottieni: …]` / `[Perdi: …]`, che il
testo non aveva.

**Refusi trovati e NON corretti** (la regola vale anche durante una
conversione): `advancing` e `dei senses` nel cap-02, `awolse` e
`reaching` nel cap-03, `sorpresi gli stessi sovrani` nel cap-06. Restano
lì: li corregge l'autore se vuole.

Il testo originale in terza persona è nella storia di git, al commit
precedente.

## A che punto siamo

| passo | stato |
|---|---|
| 01 · romanzo | ✅ completo |
| 02 · catena (Fase 0) | ✅ **58 tappe**, tutte col testo intatto — vedi `02-catena/` |
| 03 · acquisizioni | ✅ **30 censite**, 7 candidate saltabili — vedi `03-acquisizioni.md` |
| 04 · domande | ✅ **22 schede** pronte — da approvare e inviare, vedi `04-domande.md` |
| 05 · risposte dell'autore | ⏸ **tocca all'autore** — ~6.200 parole in 22 pezzi indipendenti |
| 06 · tappe | ⬜ |
| `libro.json` | ⬜ |

## Il quadro dopo la Fase 0

| | |
|---|---|
| tappe | **58** (1-59, il 29 resta libero) |
| scontri | **6**, uno per capitolo |
| usi di disciplina | **37**, tutti censiti |
| acquisizioni | **31** — 10 marcate nel testo, 21 dedotte |
| bivi forzati | **20** |
| forma | **LIBROGAME** su tutti e sei i capitoli |
| tappe in terza persona | **1** (la 49: Malakor, che il protagonista non vive) |
| **tappe col testo modificato** | **0 su 58** |

Le **115 citazioni** «Dal testo» sono state verificate una per una
contro i capitoli: tutte presenti. I tagli sono ancorati al testo vero,
non a una parafrasi.

Nessuna tappa ha avuto bisogno della frase di raccordo: i punti di
taglio cadono da soli dove il protagonista decide o cambia luogo.

## I numeri di partenza

Misurati sul materiale, servono a sapere cosa aspettarsi:

| | |
|---|---|
| parole | 7.581 |
| scene stimate | ~28 |
| scontri marcati | 6 (uno per capitolo) |
| usi di disciplina | 37, di 8 tipi su 10 |
| **domande attese al primo giro** | **~43** |

Il giocatore sceglie 5 discipline su 10: dove il romanzo ne usa una,
metà dei lettori non ce l'ha e serve una via alternativa. Non è una
scelta di design, è una necessità meccanica — ed è da lì che nascono le
domande.
