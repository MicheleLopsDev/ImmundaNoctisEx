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

## Quarto giro: non esiste "la" strada giusta

Michele, 06/08/2026: *"e se il libro fosse il percorso lineare, quello
più semplice e corretto per arrivare alla fine dell'avventura?"* —
l'idea che il romanzo di partenza sia **il** cammino corretto, e tutto
il resto un errore. Misurabile, e misurata.

Per ogni libro: da quante scene la vittoria è **ancora raggiungibile**?
Si risale il grafo all'indietro dal finale vittorioso.

| | media su 37 opere | min | max |
|---|---|---|---|
| scene da cui si può ancora vincere | **95%** | 86% | 100% |
| cammino più **corto** alla vittoria | 83 tappe | | |
| cammino più **lungo** alla vittoria | **163 tappe** | | |

**Due numeri che ribaltano il corollario dell'ipotesi.**

Il 95% delle scene può ancora portare alla vittoria: perdere è
l'eccezione, non la norma. E il cammino vincente più lungo è **il
doppio** del più corto — due lettori possono percorrere 83 e 163 tappe e
vincere entrambi.

Quindi nei librogame veri **non esiste "il" percorso corretto**: ne
esistono moltissimi, di lunghezza molto diversa. Il libro premia
l'esplorazione, non la precisione. Un libro in cui una scelta sbagliata
ti condanna venti tappe prima che tu lo scopra è il difetto peggiore del
genere — quello che i lettori chiamano "indovina la strada giusta", e i
libri buoni non lo fanno.

**Il nucleo dell'intuizione resta però giusto e utile**: il romanzo è
*un* cammino vincente, completo e sensato. È la spina dorsale da cui
partire. Solo che le strade che si staccano non sono errori: sono
**altri modi di arrivare in fondo**. Vedi `DA-ROMANZO-A-LIBROGAME.md`,
dove questo cambia direttamente cosa si chiede allo scrittore.

Implementato in `forma` come `quotaViva`, soglia 80%.

## Quinto giro: quanto costa scegliere male

Michele, 08/08/2026: *"mi sembra che in Lupo Solitario, quando si
sceglieva una cosa diversa da quella che l'autore decideva, era per lo
più morte — io farei un controllo sui primi romanzi per capire come la
gestiva il buon Joe"*.

Misurato sui **cinque libri** con la struttura completa (1004 bivi,
2303 uscite di bivio):

| | totale | 01fftd | 02fotw | 03tcok | 04tcod | 05sots |
|---|---|---|---|---|---|---|
| uscite → morte immediata | **4,8%** | 5,0 | 4,5 | 7,7 | 3,7 | 3,0 |
| uscite che **chiudono la vittoria** | **6,9%** | 5,7 | 4,9 | 15,9 | 3,7 | 3,6 |
| bivi con almeno un'uscita mortale | **10,7%** | 11,2 | 10,0 | 16,8 | 8,5 | 6,9 |
| distanza mediana dalla morte più vicina | **6 tappe** | 6 | 5 | 5 | 6 | 6 |

**Risposta: no.** Nove bivi su dieci non hanno nessuna uscita mortale,
e il 93% delle uscite lascia la vittoria raggiungibile. Da una scena
qualunque la morte è a **sei tappe** di distanza, non a una: Dever non
puniva la scelta diversa, la faceva costare.

Due precisazioni che il numero da solo non dà.

**Perché l'impressione è diversa.** Venti morti secche per libro sono
il 4,8% delle uscite e il 100% di ciò che il lettore ricorda: chi le ha
prese tutte ha ricominciato venti volte. In più il **9,6%** delle
uscite porta dritto a un combattimento, dove si muore per tiro e non
per scelta — mortalità reale che questa tabella non conta.

**Un libro è fuori scala.** *Caverns of Kalte* (03tcok) chiude la
vittoria nel **15,9%** delle uscite, il quadruplo degli altri quattro,
ed è il libro con fama di essere il più punitivo della prima serie. La
misura se ne accorge da sola: è la prova che serve a qualcosa.

Implementato in `forma` come `usciteMortali` e `usciteSenzaRitorno`,
bersaglio **3-8%** per entrambe — l'intervallo tiene fuori Kalte
apposta.

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

## Terzo giro: il limite si restringe (37 opere, 2 autori, 2 generi)

Il limite qui sopra è durato poco. Michele: *"la saga di Lupo Solitario
è la più importante, la stragrande maggioranza degli autori ci si è
ispirata… inoltre i libri sono molto famosi per la loro giocabilità e
il fatto che erano progettati molto bene"*. Obiezione fondata, e
verificabile: Project Aon pubblica i grafi di **altre due serie**.

- **Grey Star** (`gs/`, 4 libri) — *The World of Lone Wolf*, scritti da
  **Ian Page**: autore diverso, stesso mondo, stesso periodo.
- **Freeway Warrior** (`fw/`, 4 libri) — Dever, ma **post-apocalittico**:
  stesso autore, genere e ambientazione completamente diversi.

Un controllo quasi sperimentale: la prima serie isola l'autore, la
seconda isola il genere.

| | Lone Wolf (29) | Grey Star (4)<br>*altro autore* | Freeway W. (4)<br>*altro genere* |
|---|---|---|---|
| uscite per scena | 1,57 | 1,66 | 1,48 |
| scene con più ingressi | 32% | 35% | 26% |
| quota obbligata | 42% | 43% | 50% |
| **rientro (mediana)** | **3** | **3** | **3** |
| finali | 12,6 | **27** | **24** |

**Gli invarianti reggono.** Il grado resta fra 1,46 e 2,00 su tutte e
37 le opere (un solo outlier, `03btng` a 2,00, il grafo più largo mai
misurato). La quota obbligata sta fra 42% e 50%. E il rientro dei rami
è **mediana 3 in tutte e trentasette**, senza una sola eccezione:
cambia l'autore, cambia il genere, cambia il decennio, e i rami
continuano a ricongiungersi dopo tre tappe.

**Una cosa invece non regge, ed è istruttiva:** il numero di finali.
Lone Wolf ne ha 12,6 di media, Grey Star 27 e Freeway Warrior 24 — il
doppio. Quanto sei disposto a uccidere il lettore è **stile**, non
mestiere: coerente con la deriva interna a Lone Wolf, che passa da 17 a
8 nel corso della serie. Nel prompt va quindi trattato come una scelta
dell'autore, non come un vincolo di forma.

**Ha ragione anche sul secondo argomento, e conta di più del primo.**
Lupo Solitario è ricordato per essere *progettato bene* come gioco. Se
è così, i suoi numeri non descrivono "come scriveva Dever": descrivono
**come si fa un librogame che funziona**. Per il nostro scopo — dare a
un modello una forma da rispettare — è la fonte migliore possibile, e
il fatto che due serie diverse per autore e genere ricadano sugli
stessi valori dice che quei numeri non sono un'idiosincrasia.

**Limite che resta**: tutte e tre le serie sono pubblicate dallo stesso
editore e curate dalla stessa comunità. Non abbiamo misurato Fighting
Fantasy o altre collane, semplicemente perché i loro grafi non
esistono in questa forma.

## Ricadute pratiche

- Il prompt di Fase 1 (`PROMPT-GENERAZIONE-LIBRI.md`) ora chiede la
  forma misurata invece che una inventata.
- **`EndingsValidator` (fatto)**: avvisa quando un libro dichiara dei
  finali ma nessuno è una sconfitta, o nessuno è una vittoria. Viene
  dal punto 5 — un libro senza morti non è benevolo, è un libro dove
  nessuna scelta costa niente, che è il difetto tipico di un testo
  generato da un modello. Al primo giro ha trovato tre difetti veri nel
  nostro stesso materiale (vedi DIARIO 06/08).
- **`./gradlew :tool:cli --args="forma <libro.json>"` (fatto)**:
  `FormaDelGrafo` misura un libro e lo confronta con questi numeri.
  Errori (scene orfane, vicoli ciechi, nessuna vittoria) e avvisi
  (grado fuori misura, rami che non rientrano, cammino obbligato
  troppo basso o troppo alto). Due scelte tarate sui dati e non a
  occhio:
  - il grado **esclude i finali dal denominatore**, perché un finale
    non può avere uscite: su 350 scene sposta il 3%, su un libro di 6
    scene il 33% — abbastanza da far gridare "racconto lineare" a un
    libro sano. Con questa formula le 37 opere danno 1,65 di media, da
    1,45 a 2,18;
  - i rientri lunghi si contano **in proporzione** (soglia 15%), non a
    uno a uno: i cinque libri Project Aon ne hanno fra il 5% e il 12%,
    e segnalarli tutti avrebbe reso l'avviso rumore.

  Provato sui cinque libri veri: un solo avviso, `01fftd` al 6% di
  cammino obbligato — che è **corretto**, quel libro ha davvero due
  sole tappe obbligatorie su 33 di profondità.

## Nota sulle edizioni HTML di Project Aon

Verificato il 06/08/2026 (Michele aveva segnalato `xhtml-less-simple`):
quella versione ha una pagina per sezione con markup Bootstrap (navbar,
searchbox, colonne) ma **non** le classi semantiche. `xhtml-simple`,
che il convertitore già usa, ha il libro intero in un file con
`class="choice"`, `class="combat"`, `class="puzzle"`: molto meglio da
leggere a macchina. Il nome inganna — "less simple" si riferisce alla
presentazione, non ai dati.
