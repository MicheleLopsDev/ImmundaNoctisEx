# Da romanzo a libro-game: il metodo uomo-modello

Michele, 06/08/2026: *"lo scrittore che conosco non vuole scrivere
libri game, lui crea romanzi… lui può riscrivere quante scene vogliamo
ma dobbiamo dirgli quali scrivere e cosa. Il senso di usare un modello è
proprio quello: generare una modalità semplificata uomo-modello per
passare da una specie di romanzo a un grafo"*.

Questo documento è quel metodo. `PROMPT-GENERAZIONE-LIBRI.md` contiene i
prompt; qui c'è **come si lavora**, e soprattutto *chi fa cosa*.

## Il problema, detto in una riga

Un romanzo ha **un cammino**; un libro-game ne ha decine. Il romanzo
esiste già ed è buono. Manca tutto ciò che il lettore avrebbe potuto
fare *invece*.

E quel materiale mancante lo può scrivere solo lo scrittore — un modello
che imita la sua voce si riconosce alla seconda riga. Ma lo scrittore non
sa **quali** scene mancano, perché per saperlo bisogna ragionare sul
grafo, che non è il suo mestiere e non deve diventarlo.

## I tre ruoli

| Chi | Cosa fa | Cosa NON vede mai |
|---|---|---|
| **Lo scrittore** | Scrive le scene che gli vengono commissionate, in prosa, come scriverebbe un romanzo | Numeri di tappa, grafi, JSON, la parola "riconvergenza" |
| **Il modello** | Legge il romanzo, propone dove ramificare, **scrive le commissioni**, e a consegna avvenuta ricuce il tutto in tappe | — |
| **Il curatore** (Michele) | Approva le commissioni prima che partano e i testi quando tornano; taglia quelle che non convincono | — |

Il modello non scrive il libro. **Scrive la lista della spesa.**

## Il ciclo

```
  romanzo  ──Giro A──►  struttura  ──►  COMMISSIONI  ──►  [curatore]
                                                              │
                                                         approvate
                                                              ▼
  JSON ◄──Fase 2──  tappe  ◄──Giro B──  scene nuove  ◄── [scrittore]
   │
   └──►  validate + forma  ──►  editor  ──►  si gioca
```

Il passaggio nuovo, rispetto ai prompt già scritti, è **COMMISSIONI**:
la traduzione da "qui il grafo ha bisogno di un ramo" a "scrivimi questa
scena".

## Il romanzo è la spina dorsale, non l'unica strada giusta

Vale la pena dirlo prima delle commissioni, perché cambia *cosa* si
chiede allo scrittore.

Verrebbe naturale pensare che il romanzo sia il percorso **corretto**, e
tutto ciò che se ne discosta un errore. I dati dicono di no: nei 37
librogame misurati il **95% delle scene può ancora portare alla
vittoria**, e il cammino vincente più lungo è il doppio del più corto
(83 tappe contro 163). Non esiste "la" strada giusta — ne esistono
moltissime, e il libro premia chi esplora, non chi indovina
(`FORMA-DEI-GRAFI.md`).

Quindi:

> Il romanzo dà **un** cammino vincente, completo e già scritto. Le
> deviazioni non sono trappole: sono **altri modi di arrivare in
> fondo**, che costano qualcosa o fanno vedere altro.

Per lo scrittore è una notizia ottima. Non deve inventare venti modi di
morire — quello è game design, e non è il suo mestiere. Deve rispondere
venti volte alla domanda *«e se invece Ariel avesse preso il vicolo?»*,
che è narrativa pura.

Le sconfitte restano, ma sono **poche e nette**: tre-cinque in tutto il
libro, ognuna conseguenza diretta e riconoscibile di una scelta. Mai una
condanna che si sconta venti tappe dopo.

## Il formato di una commissione

È la parte che conta. Una commissione è scritta **nella lingua dello
scrittore**: dove siamo, cosa cambia, come va a finire. Il rientro non si
esprime con un numero ma **citando una frase del romanzo**.

```
### R-07 — "Il vicolo sbagliato"

**Quando**: Capitolo 2, subito dopo che Ariel esce dalla taverna con
Tobias.

**Cosa cambia**: invece di prendere la strada larga verso il porto,
Ariel taglia per i vicoli del quartiere vecchio.

**Chi c'e'**: Ariel, Tobias. I cultisti osservano ma non si mostrano.

**Come finisce**: Ariel sbuca sul molo in ritardo, e Tobias ha un
taglio all'avambraccio che non sa spiegare.

**Poi la storia riprende** da dove il capitolo dice: «Il vascello li
attendeva, la chiglia scura contro il molo».

**Lunghezza**: 250-350 parole.

**Continuita'**: Ariel non deve ancora sospettare dei cultisti — la
rivelazione e' nel Capitolo 4.
```

Sette voci, nessun tecnicismo. Le regole che le governano:

- **"Poi la storia riprende da"** è il punto di rientro, espresso come
  una citazione. Se una commissione non ce l'ha, è un ramo che non
  rientra — cioè un finale, e va detto: *"Come finisce: qui Ariel muore.
  Dopo non c'è nulla."*
- **"Cosa cambia"** deve produrre una differenza vera per il lettore: un
  oggetto, un'informazione, una ferita, un alleato in meno. Se al rientro
  non è cambiato niente, la commissione non va mandata — è lavoro chiesto
  a vuoto.
- **"Continuità"** protegge il romanzo: dice allo scrittore cosa il
  protagonista non può ancora sapere. È la voce che evita di rompere una
  rivelazione posata tre capitoli dopo.
- **Le sconfitte hanno bisogno di più cura delle deviazioni.** In un
  romanzo il protagonista non muore mai, quindi lo scrittore non le ha
  mai scritte. Vanno commissionate esplicitamente, con la scelta
  sbagliata che ci porta.

## Quanto lavoro è, davvero

Su un libro da 50 tappe, il romanzo ne copre circa la metà: le altre
sono rami e finali. Sono **20-25 scene nuove da 250-350 parole**, cioè
6.000-8.000 parole in tutto — un paio di capitoli come mole, ma spezzati
in pezzi corti e indipendenti, ognuno col suo contesto già scritto
dentro la commissione.

Non è un dettaglio: è la ragione per cui il metodo può funzionare con
uno scrittore vero. Nessuna delle scene richiede di tenere in testa
l'intera struttura.

## Il prompt che genera le commissioni

Da lanciare **dopo** il Giro A, allegando la struttura approvata e i
capitoli.

```
Questa e' la struttura di un libro-game ricavata da un romanzo, gia'
approvata:
[qui incolli la risposta del Giro A]

E questo e' il romanzo:
[qui incolli i capitoli]

Le scene alternative NON le scrivi tu: le scrivera' l'autore del
romanzo, che ha la sua voce e va rispettata. Il tuo compito e'
COMMISSIONARGLIELE.

PRINCIPIO DA NON SBAGLIARE
Il romanzo e' UN cammino che arriva alla fine, non l'unico giusto. Le
deviazioni che commissioni sono ALTRI MODI DI ARRIVARE IN FONDO, non
errori da punire: chi le percorre paga un prezzo o vede altro, ma la
storia continua. Le sconfitte sono poche (3-5 in tutto il libro) e
sempre la conseguenza diretta e riconoscibile di una scelta — mai una
condanna che il lettore sconta venti tappe dopo senza saperlo.

Per ogni deviazione e per ogni sconfitta della struttura, scrivi una
scheda cosi':

### <codice> - "<titolo>"
**Quando**: <in quale capitolo e dopo quale momento preciso del
romanzo si stacca>
**Cosa cambia**: <cosa fa il protagonista di diverso>
**Chi c'e'**: <personaggi presenti, e chi resta fuori scena>
**Come finisce**: <lo stato in cui il lettore arriva alla fine>
**Poi la storia riprende** da dove il capitolo dice: «<una frase
letterale del romanzo, copiata esatta>»
**Lunghezza**: <parole, fra 250 e 400>
**Continuita'**: <cosa il protagonista NON deve ancora sapere qui>

REGOLE
- Scrivi nella lingua di un romanziere. Non nominare mai tappe,
  numeri, nodi, grafi, JSON: l'autore non deve sapere che esistono.
- La frase di rientro va COPIATA dal romanzo, parola per parola, non
  parafrasata: e' li' che il testo nuovo si ricuce a quello vecchio.
- Per una sconfitta, al posto del rientro scrivi: "Come finisce: qui
  <protagonista> muore / fallisce. Dopo non c'e' nulla." e aggiungi
  **Perche' ci si arriva**: <la scelta sbagliata che porta qui>.
- Se una deviazione non lascia al lettore niente di diverso (un
  oggetto, una ferita, un'informazione, un alleato in meno), NON
  scrivere la scheda: elencala invece in fondo, sotto "SCARTATE", con
  una riga di motivo.
- Ordina le schede per capitolo.

Chiudi con un CONTEGGIO: quante schede, quante parole in totale.
```

## Cosa torna indietro, e come rientra

Lo scrittore consegna le scene in un file, una per codice (`R-07`, …).
Da lì:

1. Si aggiungono al Giro B come materiale del capitolo, e il modello le
   taglia in tappe insieme alla prosa originale.
2. Fase 2 per il JSON.
3. `validate` (il grafo regge?) e `forma` (ha la forma di un
   libro-game?). Vedi `FORMA-DEI-GRAFI.md` per il significato dei
   numeri.

Due misure di `forma` guidano direttamente il lavoro:

- **cammino obbligato** troppo alto → servono più deviazioni: è la
  misura che dice *quante* scene chiedere ancora;
- **"può ancora vincere"** sotto l'80% → ci sono troppe strade
  condannate. Non servono altre scene: serve **collegare** quelle che ci
  sono, facendole rientrare invece di finire in una morte.

## Perché questo giro può reggere

Ogni pezzo è dato a chi lo sa fare:

- lo **scrittore** scrive prosa, l'unica cosa che un modello non sa
  imitare senza che si veda;
- il **modello** fa il lavoro noioso e combinatorio — leggere 60 KB,
  trovare i punti di frattura, formulare richieste;
- il **curatore** decide, che è l'unica cosa che nessuno dei due può
  fare al posto suo;
- le **misure** (`forma`) dicono quando basta, invece di lasciarlo al
  colpo d'occhio.

Nessuno dei tre fa il mestiere di un altro. È tutto qui.
