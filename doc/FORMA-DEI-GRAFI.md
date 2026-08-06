# La forma dei grafi: 29 librogame misurati

Michele, 06/08/2026: *"abbiamo i grafi per più avventure, ognuno è
diverso però alla fine immagino che di modelli non ce ne siano così
tanti, sono variazioni statistiche rispetto a un numero limitato da cui
il resto deriva — e questo penso sia nella maggioranza dei libri game"*.

L'ipotesi era giusta, e più forte di così: **di modello ce n'è
essenzialmente uno**, con una variante tarda. Questo documento riporta
le misure, perché i numeri di forma del prompt di generazione
(`PROMPT-GENERAZIONE-LIBRI.md`) smettessero di essere scelti a naso.

## Come sono state prese

Project Aon pubblica, per ogni libro, il grafo Graphviz di tutti i
percorsi: <https://www.projectaon.org/en/svg/lw/> — 30 file `.svgz`,
uno per libro (29 della serie di Lupo Solitario, più `manual`).

Gli archi si leggono direttamente dai `<title>N-&gt;M</title>` dell'SVG.
Sono la **fonte ufficiale**: non derivano dalla nostra conversione, e
infatti hanno già trovato archi che nessun altro controllo vedeva (vedi
`verificaGrafo`, DIARIO 04/08).

## I numeri

| libro | grado | corridoi | bivi | 3+ uscite | riconv. | finali | profondità |
|---|---|---|---|---|---|---|---|
| 01fftd | 1,59 | 45% | 50% | 12% | 37% | 17 | 33 |
| 02fotw | 1,65 | 44% | 50% | 16% | 32% | 19 | 70 |
| 03tcok | 1,72 | 34% | 60% | 16% | 35% | 21 | 37 |
| 04tcod | 1,62 | 43% | 52% | 13% | 34% | 15 | 54 |
| 05sots | 1,71 | 39% | 58% | 13% | 37% | 13 | 63 |
| 06tkot | 1,68 | 39% | 55% | 17% | 34% | 20 | 74 |
| 07cd | 1,83 | 30% | 64% | 21% | 38% | 20 | 38 |
| 08tjoh | 1,61 | 41% | 53% | 13% | 35% | 19 | 43 |
| 09tcof | 1,63 | 47% | 50% | 14% | 33% | 11 | 52 |
| 10tdot | 1,52 | 50% | 45% | 11% | 34% | 16 | 53 |
| 11tpot | 1,51 | 53% | 43% | 11% | 30% | 13 | 102 |
| 12tmod | 1,47 | 49% | 45% | 7% | 30% | 21 | 97 |
| 13tplor | 1,51 | 51% | 46% | 7% | 31% | 10 | 73 |
| 14tcok | 1,55 | 45% | 51% | 7% | 27% | 14 | 72 |
| 15tdc | 1,42 | 59% | 39% | 4% | 23% | 7 | 107 |
| 16tlov | 1,49 | 54% | 44% | 6% | 28% | 8 | 101 |
| 17tdoi | 1,53 | 47% | 48% | 10% | 31% | 17 | 101 |
| 18dotd | 1,42 | 59% | 39% | 5% | 28% | 10 | 110 |
| 19wb | 1,52 | 54% | 43% | 8% | 28% | 10 | 89 |
| 20tcon | 1,62 | 41% | 53% | 12% | 32% | 19 | 103 |
| 21votm | 1,53 | 54% | 44% | 8% | 30% | 5 | 103 |
| 22tbos | 1,52 | 52% | 45% | 7% | 31% | 10 | 114 |
| 23mh | 1,50 | 56% | 42% | 9% | 27% | 8 | 109 |
| 24rw | 1,56 | 54% | 44% | 10% | 30% | 7 | 90 |
| 25totw | 1,62 | 48% | 50% | 11% | 32% | 8 | 75 |
| 26tfobm | 1,66 | 47% | 52% | 12% | 32% | 6 | 86 |
| 27v | 1,57 | 51% | 45% | 11% | 34% | 14 | 85 |
| 28thos | 1,49 | 62% | 37% | 11% | 33% | 5 | 103 |
| 29tsoc | 1,56 | 53% | 46% | 9% | 33% | 2 | 125 |

- **grado** = archi ÷ scene, cioè quante uscite ha in media una scena
- **corridoi** = scene con una sola uscita; **bivi** = due o più
- **riconv.** = scene raggiunte da più di un percorso
- **profondità** = lunghezza del cammino più lungo dall'inizio

Sintesi sui 29 libri:

| | media | min | max | dev.st |
|---|---|---|---|---|
| uscite per scena | **1,57** | 1,42 | 1,83 | 0,09 |
| scene con più ingressi | **32%** | 23% | 38% | 3,3 |
| finali | 12,6 | 2 | 21 | 5,5 |
| scene irraggiungibili | **0,24** | 0 | 2 | 0,6 |

Il numero di scene non è un dato emergente: 350 in 27 libri su 29 (400
in `05sots`, 300 in `28thos`). È il formato della collana.

## Cosa dicono davvero

**1. La ramificazione è una costante, non una scelta.** 1,57 uscite per
scena, deviazione standard 0,09 — sei per cento. Ventinove libri scritti
nell'arco di vent'anni, autori diversi dopo il decimo, e la larghezza
del grafo non si muove. Non è uno stile: è il punto di equilibrio fra
"il lettore deve poter scegliere" e "l'autore deve poter scrivere". Sopra
le due uscite medie il numero di scene esplode e nessuno finisce il libro.

**2. Un terzo delle scene si raggiunge da più di una strada.** Anche
questo stabile (23-38%). È la manutenzione che tiene in piedi il punto
1: se i rami non rientrassero, con 1,57 uscite medie servirebbero
migliaia di sezioni invece di 350.

**3. Zero scene irraggiungibili.** Media 0,24 su 350, e sono refusi
tipografici, non scelte. Un librogame pubblicato non ha scene morte —
il che rende il nostro controllo di raggiungibilità un criterio
*giusto*, non pedanteria.

**4. C'è una deriva, ed è netta.**

| | primi 9 (01-09) | mezzo (10-19) | ultimi 10 (20-29) |
|---|---|---|---|
| uscite per scena | 1,67 | 1,49 | 1,56 |
| scene con più ingressi | 35% | 29% | 31% |
| scene con 3+ uscite | 15% | 8% | 10% |
| finali | 17,2 | 12,6 | 8,4 |
| profondità | 52 | 91 | 99 |

A parità di 350 sezioni, la serie passa da **larga e letale** a
**stretta e lunga**: il cammino più lungo raddoppia (52 → 99) mentre i
finali dimezzano (17 → 8). Lo stesso budget di sezioni speso in
profondità invece che in ampiezza. È l'autore che impara che un
corridoio lungo si scrive meglio di un albero largo — e che uccidere il
lettore diciassette volte per libro, alla lunga, stanca.

**5. I finali sono quasi tutti morti, e sono scritte.** Nel libro 1,
sedici dei diciassette finali sono morti narrative con la loro prosa
(cadute, agguati, veleno), e la 350 è l'unica vittoria. Non esiste una
schermata generica di sconfitta: ogni modo di morire è una scena. Da
noi il `deathSceneId` globale copre le morti *meccaniche*
(combattimento, statistiche); le morti da scelta sbagliata restano
scene `ENDING` vere, e devono avere la loro prosa.

## Secondo giro: i modelli, non le medie

Le misure sopra sono aggregate. La domanda di Michele però parlava di
*modelli* — strutture ricorrenti. Tre misure locali, sugli stessi 29
grafi.

### Quanto della storia è obbligatorio

Un nodo è **obbligato** se ogni singolo percorso dall'inizio alla
vittoria ci passa (in gergo: lo *domina*). È lo scheletro vero del
libro, quello che il lettore non può evitare comunque scelga.

In numero assoluto varia moltissimo (da 2 nel libro 1 a 76 nel 29), e
deriva lungo la serie come tutto il resto. Ma **in rapporto al cammino
percorso è stabile**:

> **il 42% del cammino principale è obbligato** (media su 29, dev.st
> 10,8; escluso il libro 1 che è un outlier a 6%).

Quattro tappe su dieci fra quelle che attraversi *dovevano* succedere;
le altre sei dipendono da te. È il numero più utile per noi, perché è
un **rapporto**: vale su 350 sezioni come su 50.

### Dopo quanto rientra un ramo

Su 4898 bivi di tutta la serie:

| rientra dopo | quota | cumulato |
|---|---|---|
| 2 tappe | 39% | 39% |
| 3 tappe | 37% | **76%** |
| 4 tappe | 14% | 90% |
| 5 tappe | 5% | **95%** |
| 6+ tappe | 5% | 100% |

Mediana **3**. Il 9% dei bivi non rientra mai: sono le deviazioni
definitive, cioè i finali. Il "entro 2-3 tappe" che il prompt diceva a
naso era **giusto** — ora si può anche dire il limite: oltre 5 non si
va quasi mai.

### Le misure che derivano e quelle che no

Correlazione di ogni misura col numero del libro (cioè: quanto cambia
lungo la serie):

| misura | r |
|---|---|
| profondità | **+0,74** |
| finali | **−0,73** |
| tappe obbligatorie | +0,59 |
| scene con 3+ uscite | −0,50 |
| uscite per scena | −0,42 |
| scene con più ingressi | −0,38 |
| **quota di cammino obbligata** | **+0,28** |

Si leggono due famiglie. Profondità e finali si muovono *molto*: sono
le scelte di stile di un autore che cambia idea nel tempo. Grado,
riconvergenza e quota obbligata si muovono *poco*: sono il mestiere.

Questo è precisamente l'assetto che Michele ipotizzava — un parametro
latente ("quanto largo scrivo") che si sposta e trascina con sé mezza
dozzina di metriche, sopra un pavimento di invarianti che non si muove
mai.

## La domanda diretta: ci sono categorie?

Michele, 06/08/2026: *"le mie ipotesi erano giuste o no, ci sono degli
schemi ricorrenti e si possono classificare i grafi in poche categorie
sì o no?"*. Sono due domande e hanno due risposte diverse.

**Schemi ricorrenti: sì.** Grado 1,57 con deviazione standard 0,09,
riconvergenza 32 ± 3, rientro dei rami con mediana 3 su 4898 bivi,
quota obbligata 42%. Sono invarianti solidi.

**Categorie: no.** Verificato con k-means sulle sei metriche
standardizzate, misurando la silhouette contro una nuvola casuale di
controllo (stesso numero di libri, stesse dimensioni, nessuna
struttura):

| k | silhouette | su dati casuali |
|---|---|---|
| 2 | 0,345 | 0,188 |
| 3 | 0,300 | 0,168 |
| 4 | 0,309 | 0,187 |
| 5 | 0,257 | 0,174 |

C'è **più struttura del caso**, ma nessun k arriva a una separazione
netta (servirebbe > 0,5; sotto 0,25 è nulla). E i due gruppi che
k-means trova con k=2 sono i primi dieci libri più quattro tardi contro
tutti gli altri: è di nuovo **la deriva temporale**, non due specie
diverse di grafo.

I libri stanno su un **continuum**, non in scatole. L'ipotesi completa
di Michele era *"variazioni statistiche rispetto a un numero limitato
da cui il resto deriva"* — e quella regge: il "numero limitato" esiste,
solo che è **un asse continuo** (quanto largo / quanto lungo) e non un
insieme di categorie. Un cursore, non un menu a tendina. Il che è più
utile: un cursore si può impostare per generare, una categoria sarebbe
stata solo un'etichetta.

**Limite dichiarato:** sono 29 libri di *una sola serie*, con un autore
principale. Vale per Lupo Solitario. Che valga per il genere è
plausibile — i vincoli sono di mestiere, non di stile — ma non è stato
misurato e questo documento non lo dimostra.

## Ricadute pratiche

- Il prompt di Fase 1 (`PROMPT-GENERAZIONE-LIBRI.md`) ora chiede la
  forma misurata invece che una inventata.
- **`EndingsValidator` (fatto)**: avvisa quando un libro dichiara dei
  finali ma nessuno è una sconfitta, o nessuno è una vittoria. Viene
  dal punto 5 — un libro senza morti non è benevolo, è un libro dove
  nessuna scelta costa niente, che è il difetto tipico di un testo
  generato da un modello. Al primo giro ha trovato tre difetti veri nel
  nostro stesso materiale (vedi DIARIO 06/08).
- Un controllo di **forma strutturale** completo resta da fare: grado
  fuori da 1,3-1,9, riconvergenza sotto il 20%, o una sola strada per
  ogni scena, sono segnali che il modello ha prodotto un racconto
  lineare travestito da librogame. Il posto naturale è accanto a
  `ConfrontoGrafo` nel `:tool`.

## Nota sulle edizioni HTML di Project Aon

Verificato il 06/08/2026 (Michele aveva segnalato `xhtml-less-simple`):
quella versione ha una pagina per sezione con markup Bootstrap (navbar,
searchbox, colonne) ma **non** le classi semantiche. `xhtml-simple`,
che il convertitore già usa, ha il libro intero in un file con
`class="choice"`, `class="combat"`, `class="puzzle"`: molto meglio da
leggere a macchina. Il nome inganna — "less simple" si riferisce alla
presentazione, non ai dati.
