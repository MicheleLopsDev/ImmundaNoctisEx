# Il cantiere di un libro: dove stanno i file e come si chiamano

Michele, 07/08/2026: *"vorrei un documento semplice e chiaro sui file da
usare per il modello, formalizzare il nome e il formato… così poi lo
possiamo replicare anche con altro materiale, d'altronde è lo scopo di
questo test"*.

Questo è quel documento. Il **metodo** sta in
[`DA-ROMANZO-A-LIBROGAME.md`](DA-ROMANZO-A-LIBROGAME.md), i **prompt** in
[`PROMPT-GENERAZIONE-LIBRI.md`](PROMPT-GENERAZIONE-LIBRI.md): qui c'è
solo dove mettere le cose e come chiamarle.

> Quello da **consegnare all'autore** è un altro ancora:
> [`GUIDA-PER-LO-SCRITTORE.md`](GUIDA-PER-LO-SCRITTORE.md). Sei regole
> con esempi, nessun tecnicismo — non deve leggere nient'altro.

## Perché serve una convenzione

Trasformare un romanzo in libro-game richiede sei o sette passaggi, tre
mani diverse (autore, modello, curatore) e una decina di file per
capitolo. Senza nomi prevedibili:

- gli script devono indovinare (oggi il worldbuilding si chiama
  *"Documento di Analisi e Worldbuilding.md"*: nessun programma lo
  trova da solo);
- non si sa quale versione di un file sia stata rivista e quale no;
- non c'è un posto ovvio dove mettere le risposte dello scrittore, e
  finiscono sparse;
- il secondo libro si organizza in un altro modo, e il metodo non è più
  ripetibile.

## Dove sta un cantiere

```
libri/<nome-corto>/
```

In **radice**, non in `doc/`: un cantiere non è documentazione di
progetto. Gli altri due posti in cui vivono dei libri restano quello che
sono e non si mescolano:

| cartella | cosa contiene |
|---|---|
| `libri/<nome>/` | **la lavorazione** di un libro nostro |
| `content/test-books/` | libri **finiti**, distribuiti con l'APK |
| `doc/LIBRI/` | le **meccaniche** dei cinque Project Aon (senza prosa) |

Il `<nome-corto>` è minuscolo, con trattini, senza accenti:
`erede-dell-astra`, non `LIBRO I`.

## La struttura, che è anche l'ordine del lavoro

Le cartelle sono numerate come le fasi: leggendo l'elenco si capisce il
processo.

```
libri/erede-dell-astra/
├── 01-romanzo/           ← lo scrive l'AUTORE. Non si tocca mai.
│   ├── cap-01.md              un capitolo per file
│   ├── cap-02.md
│   ├── …
│   ├── mondo.md               personaggi, luoghi, filo della storia
│   ├── discipline.md          le abilità e come si chiamano  (facoltativi:
│   ├── bestiario.md           i nemici                        ci sono se
│   └── magia.md               le regole del sistema           l'autore li ha)
│
├── 02-catena/            ← Fase 0: il modello taglia in tappe
│   ├── cap-01.md
│   └── …
│
├── 03-acquisizioni.md    ← dove il lettore guadagna qualcosa
├── 04-domande.md         ← le schede da girare all'autore
│
├── 05-risposte/          ← le scene nuove, scritte dall'AUTORE
│   ├── D-07.md                un file per domanda, stesso codice
│   └── …
│
├── 06-tappe/             ← Giro B: prosa originale + rami, in tappe
│   ├── cap-01.md
│   └── …
│
├── libro.json            ← il risultato, quello che il gioco apre
│
└── prompt/               ← pacchetti pronti da incollare nel modello
    ├── fase0-cap-01.txt       rigenerabili: si possono cancellare
    └── …
```

**Chi scrive cosa**, che è la regola più importante:

| cartella | mano |
|---|---|
| `01-romanzo/`, `05-risposte/` | **l'autore** |
| `02-catena/`, `03-`, `04-`, `06-tappe/`, `libro.json` | **il modello** |
| tutte, in lettura e correzione | **il curatore** |

`01-romanzo/` e `05-risposte/` sono **sola scrittura dell'autore**: il
modello non ci scrive mai, nemmeno per correggere un refuso o convertire
la persona verbale.

Nelle cartelle a valle il testo dell'autore viene **ricopiato** tagliato
in tappe. Lì il modello può aggiungere una frase per chiudere una tappa
monca, ma deve **dichiararlo** tappa per tappa e contarne il totale: il
principio non è "vietato toccare", è "ogni tocco si vede" — vedi
[`DA-ROMANZO-A-LIBROGAME.md`](DA-ROMANZO-A-LIBROGAME.md) in cima.

## I nomi

- minuscolo, parole separate da trattini: `cap-01.md`, non `Cap 1.md`;
- **due cifre** per i numeri, sempre: `cap-01`, non `cap-1` — altrimenti
  `cap-10` si ordina prima di `cap-2`;
- niente spazi, niente accenti, niente maiuscole: i nomi finiscono in
  righe di comando e in script;
- le risposte dell'autore portano il **codice della domanda**:
  `D-07.md` risponde alla scheda `D-07`. Nessun dubbio su cosa sia.

## I formati

| cosa | formato | perché |
|---|---|---|
| romanzo, catena, domande, risposte, tappe | **Markdown** (`.md`) | si legge come testo, si versiona bene, il modello lo produce nativamente |
| pacchetti da incollare | **testo semplice** (`.txt`) | vanno copiati in una finestra di chat: nessuna formattazione da perdere |
| il libro | **JSON** (`.json`) | è quello che il motore apre — vedi `SCHEMA-JSON.md` |

Tutto in **UTF-8**, a capo `LF`.

## Come si sa se un file è già stato riletto

Lo dice il file stesso, non il nome. Ogni file **generato dal modello**
comincia con una riga così:

```
> Fase 0 · generato il 2026-08-07 · DA RIVEDERE
```

Il curatore, quando l'ha letto e corretto, cambia l'ultima parola:

```
> Fase 0 · generato il 2026-08-07 · rivisto da Michele il 08/08
```

Due parole in cima a un file valgono più di una cartella `bozze/`: si
vedono aprendo il file, e sopravvivono a un rinominamento.

## Cominciare un libro nuovo

```bash
./gradlew :tool:cli --args="nuovoCantiere <nome-corto>"
```

Crea le cartelle vuote e un `01-romanzo/mondo.md` con le voci da
riempire. Poi si mettono i capitoli dell'autore in `01-romanzo/` e si
parte dalla Fase 0.

> Il comando **non esiste ancora**: finché non c'è, si copia la
> struttura a mano. Ha senso scriverlo dopo il primo libro vero, quando
> si sa che la convenzione regge davvero.

## Il ciclo, in una tabella

| # | passo | legge | scrive |
|---|---|---|---|
| 0 | Fase 0 | `01-romanzo/cap-NN.md` | `02-catena/cap-NN.md` |
| 1 | acquisizioni | `02-catena/` | `03-acquisizioni.md` |
| 2 | domande | `02-catena/`, `03-` | `04-domande.md` |
| 3 | *l'autore scrive* | `04-domande.md` | `05-risposte/D-NN.md` |
| 4 | Giro B | `01-romanzo/`, `05-risposte/` | `06-tappe/cap-NN.md` |
| 5 | JSON | `06-tappe/` | `libro.json` |
| 6 | verifica | `libro.json` | — |

Il passo 6 sono i due comandi che esistono già:

```bash
./gradlew :tool:cli --args="validate libri/<nome>/libro.json"
./gradlew :tool:cli --args="forma libri/<nome>/libro.json"
```

Se `forma` dice che manca qualcosa, si torna al passo 2 con le sue
istruzioni (*«servono ~7 scorciatoie»*) e si itera. Vedi
`FORMA-DEI-GRAFI.md` per il significato dei numeri.

## Una regola che vale più di tutte le altre

**Tutto ciò che sta fuori da `01-romanzo/` e `05-risposte/` è
rigenerabile.** Se una fase esce male, si cancella la cartella e si
rifà: non si perde niente di irrecuperabile.

Quelle due cartelle no: contengono l'unica cosa che nessuno può
riprodurre, cioè le parole dell'autore.
