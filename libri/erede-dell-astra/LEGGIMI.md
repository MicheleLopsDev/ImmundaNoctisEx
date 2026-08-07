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

## A che punto siamo

| passo | stato |
|---|---|
| 01 · romanzo | ✅ completo |
| 02 · catena (Fase 0) | ⬜ da fare — i pacchetti sono in `prompt/` |
| 03 · acquisizioni | ⬜ |
| 04 · domande | ⬜ |
| 05 · risposte dell'autore | ⬜ |
| 06 · tappe | ⬜ |
| `libro.json` | ⬜ |

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
