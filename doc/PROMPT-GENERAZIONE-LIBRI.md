# Generare un libro da un canovaccio, con un'IA remota

Due prompt da dare a un **modello grosso** (Gemini, Claude, GPT — non
Gemma in locale: qui serve tenere in testa un'intera avventura), con in
mezzo un passaggio che rileggi e correggi tu.

> **Perché due e non uno.** Inventare una storia e produrre JSON valido
> sono mestieri diversi che si danneggiano a vicenda: chiesti insieme, la
> prosa si impoverisce mentre il modello conta le graffe, e il JSON si
> sfilaccia mentre inventa. Separati, ognuno viene meglio — e soprattutto
> **in mezzo ci sei tu**: la storia la correggi come testo, non dentro un
> JSON. Se poi il JSON esce storto, rilanci solo il secondo prompt senza
> perdere le scelte narrative già approvate.

**Cosa NON chiediamo al modello**: combattimenti, discipline, tiri di
dado, oggetti, modificatori. Quelli si aggiungono dopo, nell'editor, e
sono decisioni di gioco — non di racconto. Meno campi si chiedono, meno
il modello sbaglia.

> **Canovaccio o romanzo?** I prompt qui sotto partono da una storia
> *abbozzata*, da arricchire. Se invece hai già la **prosa finita** —
> capitoli veri, con dialoghi — salta alla [variante per un romanzo già
> scritto](#variante-partire-da-un-romanzo-gia-scritto): il compito è
> diverso e chiedere "arricchisci" a un testo finito lo peggiora.

---

## Se il canovaccio è lungo: due giri, non uno

Con un canovaccio di più capitoli, la Fase 1 si spezza in due.

**Il limite non è l'input.** Sei capitoli sono ~10.000 token: un modello
grosso ne regge venti volte tanti. **È l'output** a essere stretto — e i
modelli non troncano di netto, *impoveriscono le ultime parti*: le prime
cinque tappe vengono ricche, le ultime diventano una riga. Cioè il
finale del tuo libro.

Ma lavorare solo capitolo per capitolo costa altrettanto: il modello non
vede l'arco narrativo (non semina indizi per una rivelazione che non
sa), non può far rientrare i rami (non sa cosa viene dopo) e ogni giro
riparte da "Tappa 1".

Quindi:

- **Giro A** — tutti i capitoli insieme, ma si chiede solo la
  **struttura**: quante tappe per capitolo, dove stanno i bivi, quali
  finali. Risposta corta, nessun impoverimento.
- **Giro B** — un capitolo per volta, dando ogni volta il testo **più la
  struttura del giro A**. Ogni capitolo viene dettagliato per bene, ma
  sa da dove viene e dove va.

La numerazione si fissa in partenza a blocchi — capitolo 1 → tappe 1-9,
capitolo 2 → 10-19 — così non serve ricucire niente.

### Giro A — la struttura d'insieme

```
Sei un autore di librogame. Ti do il CANOVACCIO completo di un romanzo,
diviso in capitoli, più il documento di worldbuilding.

NON scrivere ancora le tappe. Voglio solo la STRUTTURA.

Rispondi con:

1. ARCO NARRATIVO (5-8 righe): come la storia sale, dove sta il punto di
   svolta, come si chiude.

2. Per ogni capitolo, una riga così:
   Capitolo <n> — <titolo> — <quante tappe servono> — bivio principale:
   <la decisione vera che il giocatore prende in questo capitolo>

3. FINALI (3-5): per ognuno, da quale capitolo si stacca e se è
   vittoria, sconfitta o neutro. Una sola vittoria, il resto sconfitte.

4. FILI DA SEMINARE: elementi che vanno anticipati nei capitoli
   precedenti perché una rivelazione successiva funzioni.

5. PUNTI DI RIENTRO: per ogni capitolo, la tappa in cui le strade
   alternative si ricongiungono. Circa un terzo delle tappe deve essere
   raggiungibile da più di un percorso (misura reale sui librogame
   pubblicati, vedi FORMA-DEI-GRAFI.md): senza rientri il libro
   raddoppia a ogni bivio e non lo finisci più.

Tieni conto che il totale deve stare fra 40 e 60 tappe.

CANOVACCIO E WORLDBUILDING:
[qui incolli tutti i capitoli e il documento di worldbuilding]
```

### Giro B — un capitolo per volta

Stesso prompt della Fase 1 qui sotto, con due righe in più in testa:

```
Questa è la struttura d'insieme del libro, già approvata:
[qui incolli la risposta del Giro A]

Ora lavora SOLO sul capitolo <n>. Numera le sue tappe da <inizio> a
<fine>. Le tappe degli altri capitoli esistono già: puoi puntarci
scrivendo il loro numero, non riscriverle.
```

---

## Fase 1 — dal canovaccio alle tappe

Incolla questo prompt, e sotto il tuo canovaccio.

```
Sei un autore di librogame. Ti do un CANOVACCIO: una storia abbozzata,
non ancora un libro.

Il tuo compito è arricchirla e trasformarla in una scaletta di TAPPE
pronte a diventare un librogame giocabile.

COSA DEVI FARE
1. Arricchisci la storia: aggiungi luoghi, personaggi secondari,
   oggetti significativi, momenti di tensione e almeno due colpi di
   scena. Resta dentro il tono e l'ambientazione del canovaccio: non
   cambiare genere, non introdurre elementi che lo contraddicono.
2. Costruisci una SPINA DORSALE LINEARE di 15-25 tappe: l'ordine in cui
   la storia procede se il giocatore fa le scelte "principali".
3. Su questa spina, aggiungi DEVIAZIONI: percorsi alternativi che si
   staccano da una tappa e RIENTRANO nella spina dopo 2-3 tappe — mai
   oltre 5. Non creare rami che non rientrano mai, tranne i finali.
4. Prevedi 3-5 FINALI: una vittoria, il resto sconfitte.

LA FORMA DEL LIBRO (numeri misurati su 37 librogame pubblicati, di tre
serie diverse per autore e per genere; rispettali: sono il punto di
equilibrio fra "il lettore sceglie" e "il libro si può scrivere")
- In media **1,5-1,7 uscite per tappa**. Concretamente: circa metà delle
  tappe hanno UNA sola uscita (si prosegue), circa metà sono bivi a due,
  e solo una su dieci offre tre o più strade. Un libro in cui ogni tappa
  è un bivio a tre non è più ricco: è ingestibile e non lo finisci.
- Circa **una tappa su tre deve essere raggiungibile da più di un
  percorso**. È questo che tiene basso il numero totale di tappe: se i
  rami non rientrano mai, il libro raddoppia a ogni bivio.
- **Quattro tappe su dieci, fra quelle che il lettore attraversa,
  devono essere OBBLIGATORIE**: punti in cui tutte le strade
  convergono, qualunque scelta abbia fatto prima. Sono i momenti che
  devono succedere perché la storia sia quella storia. Le altre sei
  dipendono da lui. Un libro dove tutto è evitabile non ha trama; uno
  dove niente lo è non ha scelte.
- **Nessuna tappa irraggiungibile**: ogni numero che scrivi deve essere
  citato da almeno un'altra tappa. Controlla prima di rispondere.
- Quante sconfitte mettere è una **tua** scelta, non un vincolo di
  forma: le serie misurate vanno dalle 12 alle 27 per libro, cioè
  qualcuno uccide il lettore il doppio di qualcun altro. Ma almeno una
  ci vuole, o nessuna scelta costa niente.
- Le sconfitte sono **scene vere, con il loro racconto** (come muori,
  cosa vedi per ultimo), non un "hai perso". Una morte anonima è tempo
  del lettore buttato.

REGOLE
- Ogni tappa deve chiudersi con una decisione vera del protagonista, non
  con "prosegue".
- Le scelte devono avere conseguenze diverse: se due scelte portano allo
  stesso posto senza differenza, accorpale.
- Scegli una forma e tienila per tutto il libro: **LIBROGAME**
  (seconda persona, «Apri la porta») oppure **DIARIO** (prima
  persona, «Apro la porta»). Nel dubbio, LIBROGAME.
- Non scrivere ancora la prosa delle scene: solo cosa succede — tranne
  che per i finali, dove basta una riga su come va a finire.

FORMATO DELLA RISPOSTA
Prima un paragrafo con la storia arricchita (10-15 righe).
Poi l'elenco delle tappe, ognuna esattamente così:

## Tappa <numero> — <titolo breve>
- **Dove**: <luogo>
- **Cosa succede**: <2-3 righe>
- **Scelte**:
  - <cosa può fare il giocatore> -> Tappa <numero>
  - <cosa può fare il giocatore> -> Tappa <numero>

Per i finali, al posto delle scelte scrivi:
- **Finale**: vittoria | sconfitta | neutro

Numera le tappe da 1. La Tappa 1 è l'inizio.

CANOVACCIO:
[qui incolli il tuo canovaccio]
```

**Cosa fare del risultato**: leggilo e correggilo. È il momento in cui il
libro diventa tuo — cambia i nomi che non ti piacciono, togli le tappe
deboli, aggiungine di tue. Il formato va rispettato (serve al secondo
prompt), ma il contenuto è tuo.

---

## Fase 2 — dalle tappe al JSON

Incolla questo prompt, e sotto le tappe **corrette da te**.

```
Trasforma questa scaletta di tappe in un file JSON per un motore di
librogame. Rispondi SOLO col JSON, senza commenti né testo attorno.

SCHEMA (usa esattamente questi campi, nessun altro)

{
  "id": "<identificativo-breve-senza-spazi>",
  "version": "1.0.0",
  "title": "<titolo del libro>",
  "description": "<una riga>",
  "language": "it",
  "genre": "FANTASY",
  "scenes": [
    {
      "id": "1",
      "sceneType": "START",
      "genre": "FANTASY",
      "locationName": "<nome del luogo>",
      "narrativeText": "<il testo che il giocatore legge>",
      "choices": [
        { "id": "c1", "choiceText": "<cosa fa il giocatore>", "nextSceneId": "2" }
      ]
    }
  ]
}

REGOLE OBBLIGATORIE — il file viene rifiutato se non le rispetti
1. Esiste ESATTAMENTE UNA scena con "sceneType": "START", ed è la prima.
2. Ogni "nextSceneId" deve corrispondere all'"id" di una scena che
   esiste davvero nel file. Questo è l'errore più comune: controlla uno
   per uno prima di rispondere.
3. Gli "id" delle scene sono numeri come stringhe ("1", "2", "3"), tutti
   diversi fra loro.
4. Le scene finali hanno "sceneType": "ENDING", nessun "choices", e in
   più il campo "outcome" con valore "VICTORY", "DEFEAT" o "NEUTRAL".
5. Tutte le altre scene hanno "sceneType": "TRANSITION" e almeno una
   scelta.
6. "genre" va ripetuto identico su ogni scena.

NON RIPROGETTARE IL LIBRO
La struttura è già stata decisa e approvata: tu la trascrivi, non la
migliori. In particolare:
7. Una scena per tappa, tutte quante. Se ricevi 47 tappe, il JSON ha 47
   scene. Non accorpare due tappe "simili", non saltare quelle che ti
   sembrano deboli, non aggiungerne di tue.
8. I collegamenti sono quelli scritti nelle tappe. Non aggiungere
   scorciatoie, non togliere rami che ti sembrano ridondanti, non
   "sistemare" un ramo che ti pare strano: se una tappa manda a 23, la
   scena manda a 23.
9. Riporta nella scena le NOTE DI LAVORAZIONE che la tappa dichiara —
   la frase aggiunta per chiudere il taglio, lo scostamento dalla forma
   narrativa — cosi':

   "noteDiLavorazione": [
     {"tipo": "TESTO_RITOCCATO", "testo": "aggiunta in coda «...»"},
     {"tipo": "FORMA", "testo": "due paragrafi in terza persona"}
   ]

   I tipi ammessi sono TESTO_RITOCCATO, FORMA e DA_CHIARIRE. Servono a
   chi rivede il libro nell'editor, che le vede segnalate sulla scena;
   il gioco non le legge mai. Se la tappa non dichiara niente, ometti
   il campo.

10. Gli "outcome" dei finali sono quelli dichiarati nelle tappe
   (vittoria -> VICTORY, sconfitta -> DEFEAT, neutro -> NEUTRAL).

CONTROLLO PRIMA DI RISPONDERE
Fai questi conti sul JSON che hai scritto, e se uno non torna
correggilo PRIMA di rispondere. Non scrivere i conti nella risposta.

a) Numero di scene = numero di tappe ricevute.
b) Ogni "nextSceneId" compare come "id" di una scena. Zero eccezioni:
   è l'errore che il modello commette più spesso.
c) Ogni scena, tranne la START, è citata da almeno un "nextSceneId".
   Una scena che nessuno raggiunge è testo sprecato.
d) Collegamenti totali diviso numero di scene: deve stare fra 1,4 e
   1,9. Se è sotto, hai perso dei rami per strada; se è sopra, ne hai
   inventati.
e) Almeno una scena ENDING con "outcome": "DEFEAT" e almeno una con
   "VICTORY".

COME SCRIVERE narrativeText
- Seconda persona singolare ("Apri la porta e...").
- 80-150 parole: abbastanza da immergere, non tanto da annoiare.
- Chiudi sempre sulla soglia della decisione, senza anticiparla.
- NON scrivere dentro il testo "vai al paragrafo 12" o simili: le scelte
  stanno solo in "choices".

COME SCRIVERE choiceText
- Una frase breve, all'infinito o alla seconda persona.
- Deve dire cosa si fa, non cosa succederà dopo.

TAPPE:
[qui incolli le tappe corrette]
```

---

## Fase 3 — verifica, prima ancora di aprirlo

Salva il JSON e passalo al validatore: dice in due secondi se il grafo
regge.

```bash
./gradlew :tool:cli --args="validate ../percorso/del/libro.json"
```

Cosa trova, in ordine di frequenza:

| errore | cosa è successo |
|---|---|
| `punta a 'N', che non esiste` | il modello ha inventato una destinazione — è **l'errore tipico** |
| `Nessuna scena START trovata` | manca `"sceneType": "START"` |
| `Scena 'N' duplicata` | due scene con lo stesso id |

Fra gli **avvisi** (non bloccano) il più utile qui è
`Nessun finale di sconfitta` / `Nessun finale di vittoria`: è il segnale
che il modello ha prodotto una storia dove nessuna scelta costa niente,
il suo difetto tipico. Vedi `FORMA-DEI-GRAFI.md`.

### E poi la forma

`validate` dice se il grafo **regge**; questo dice se ha la **forma** di
un librogame. Sono domande diverse: un racconto lineare con un bivio
ogni tanto passa il validatore benissimo.

```bash
./gradlew :tool:cli --args="forma ../percorso/del/libro.json"
```

```
libro.json: 47 scene, 61 collegamenti, 4 finali
  uscite per scena     1,45      (libri veri 1,45-2,18)
  scene con piu' vie   28%       (libri veri 23-44%)
  cammino obbligato    44%       (libri veri 42-50%)
  rientro dei rami     3 tappe   (libri veri: mediana 3)
```

Gli **errori** vanno corretti (scene che nessuno raggiunge, scene senza
uscita che non sono finali, nessuna vittoria). Gli **avvisi**
descrivono un libro che si gioca ma non ha la forma giusta: grado
troppo basso (racconto lineare), rami che non rientrano, cammino
obbligato fuori misura.

Sotto le 20 scene i controlli statistici non si applicano: su un libro
di sei scene "il 17% riconverge" vuol dire "una scena".

Se validatore e forma passano, il libro si apre nell'editor e si gioca.
Da lì in poi si aggiungono combattimenti, discipline, tiri e oggetti —
a mano, perché sono decisioni di gioco.

---

> Lo schema qui sopra **è stato provato** (05/08/2026): un libro di
> quattro scene scritto esattamente così passa il validatore al primo
> colpo, e cambiando un `nextSceneId` in uno inesistente il validatore
> risponde `Scena '2': choice 'c1'.nextSceneId punta a '99', che non
> esiste`. Non è uno schema dedotto dal codice: è verificato.

## Variante: partire da un romanzo già scritto

Il caso di `libri/erede-dell-astra/` (Michele, 06/08/2026): sei capitoli di **prosa
finita**, con dialoghi, più un documento di worldbuilding.

**Il compito è l'opposto di quello sopra.** Lì si chiedeva di
arricchire un abbozzo; qui non c'è niente da arricchire — c'è da
*ramificare*. Un romanzo ha **un solo cammino**: il 100% delle sue
tappe è obbligatorio, e in un librogame quella quota deve scendere al
42% (`FORMA-DEI-GRAFI.md`). Le deviazioni non ci sono e vanno inventate;
le sconfitte nemmeno, perché nei romanzi il protagonista non muore a
metà.

Tre regole che valgono solo in questa variante:

1. **La prosa esistente non si riscrive.** Si taglia in tappe e si
   ricuce. Un modello a cui chiedi di "migliorare" un capitolo finito
   lo riscrive con la propria voce, ed è esattamente quello che non
   vuoi.
2. **Il nuovo testo è solo quello dei rami** — le strade che il romanzo
   non percorre. Lì il modello scrive davvero, ma su un binario stretto:
   deve riportare il lettore dove la storia riprende.
3. **I marcatori del testo si conservano.** `*[Inizio Scontro]*` e
   `[Disciplina: Nome]` diventeranno `combat` e `disciplineChoices`
   nell'editor: buttarli via significa rifare a mano un lavoro già
   fatto.

### Fase 0 — dal romanzo alla catena di tappe

Il primo passaggio, prima ancora di pensare ai rami: **il romanzo
diventa una catena lineare di tappe**, e si raccoglie quello che il
testo già dichiara — scontri, abilità, prove. Non si inventa niente
qui: si legge e si mette in ordine.

```
Ti do un ROMANZO in capitoli. Trasformalo in una CATENA DI TAPPE: il
percorso canonico di un librogame, quello piu' ricco, dove il lettore
vede tutto e raccoglie tutto.

Non inventare niente e non riscrivere niente. Dividi e cataloga.

1. TAGLIA IN TAPPE
   Una tappa finisce dove il protagonista DECIDE qualcosa, o dove
   cambia luogo o interlocutore — mai a un tot di parole fisso. Ogni
   tappa e' un pezzo continuo del testo originale.
   Per ognuna: numero, titolo breve, capitolo, la prima e l'ultima
   frase del testo che contiene (copiate esatte, servono a ritagliare).

2. MARCA I PUNTI DI GIOCO gia' presenti nel testo:
   - SCONTRO: dove il testo descrive un combattimento (nel materiale
     e' delimitato da *[Inizio Scontro]* e *[Fine Scontro]*). Segna
     chi combatte.
   - ABILITA': dove il protagonista usa una dote (nel materiale e'
     scritto come [Disciplina: Nome]). Segna quale.
   - PROVA: dove il testo dice che qualcosa poteva andare storto —
     una serratura, un salto, una menzogna, un inseguimento. Anche
     senza marcatore: si riconosce perche' l'esito e' incerto.
   - ACQUISIZIONE: dove il protagonista ottiene un oggetto, un
     alleato, un'informazione o un'abilita' che gli servira' dopo.
     Segna cosa ottiene e, se lo vedi nel testo, dove torna utile.

3. SEGNALA I BIVI FORZATI: i punti in cui il romanzo fa scegliere al
   protagonista e racconta un solo esito. Sono i candidati naturali a
   diventare bivi veri. Per ognuno, la scelta alternativa che il testo
   nomina o lascia intendere.

4. CONTROLLA LA FORMA NARRATIVA. Un libro-game si scrive in due modi, e
   l'autore ne sceglie UNO per tutto il libro:
   - **LIBROGAME**: seconda persona, "Apri la porta", "Scarti di lato";
   - **DIARIO**: prima persona, "Apro la porta", "Scarto di lato".
   Tutto il resto e' TERZA persona ("Ariel apri' la porta"), che guarda
   il protagonista da fuori.

   Dichiara la forma UNA VOLTA, in testa al capitolo. NON ripeterla su
   ogni tappa: se il capitolo e' tutto LIBROGAME, cinquantatre righe che
   dicono "LIBROGAME" nascondono le cinque che dicono altro. Annota la
   forma solo sulle tappe che se ne DISCOSTANO.

   NON correggere niente: e' un AVVISO. Una scena puramente descrittiva
   in terza persona puo' restare tale — succede anche nei libri
   pubblicati — mentre una in cui il protagonista agisce di solito va
   convertita, ma lo decide l'autore.
   Per le tappe in terza di' anche se ti sembra DESCRITTIVA (il
   protagonista non agisce: un luogo, un antefatto, una scena vista da
   lontano) o AZIONE (il protagonista fa qualcosa).

5. DICHIARA OGNI TOCCO AL TESTO. Il libro resta come l'autore l'ha
   deciso: tu puoi tagliarlo, e puoi aggiungere UNA frase per chiudere
   una tappa che il taglio lascerebbe monca. Nient'altro.
   Ogni tappa dice se il testo viene dal romanzo senza aggiunte oppure
   se ne ha una: in quel caso scrivi la frase esatta, cosi' l'autore la
   vede e decide se tenerla.

6. CHIUDI CON UN CONTEGGIO: quante tappe, quanti scontri, quante
   abilita' usate, quante prove, quante acquisizioni, quanti bivi
   forzati, **la forma prevalente del capitolo** (LIBROGAME o DIARIO) con
   quante tappe se ne discostano, quante tappe in terza persona (di cui
   quante descrittive) e **quante tappe hanno il testo modificato**.

FORMATO
## Tappa <n> — <titolo> (Cap. <c>)
- **Dal testo**: «<prima frase>» … «<ultima frase>»
- **Testo**: <dal romanzo, nessuna aggiunta | dal romanzo + aggiunta
  «<la frase esatta>» in coda, per chiudere la tappa>
- **Punti di gioco**: <SCONTRO: … | ABILITA': … | PROVA: … |
  ACQUISIZIONE: … | nessuno>
- **Bivio forzato**: <la scelta e l'alternativa non percorsa | nessuno>
- **Nota sulla forma**: <solo se la tappa si discosta dalla forma del
  capitolo: TERZA (descrittiva) | TERZA (azione) | l'altra forma>

ROMANZO:
[qui incolli i capitoli]
```

Il risultato è il **canonico**: una catena senza rami, che è
esattamente il 100% da cui si parte. Le tappe con acquisizione e i
bivi forzati sono la materia prima del Giro A.

### Giro A — la struttura, da prosa a grafo

Da dare **una volta sola**, con tutti i capitoli e il worldbuilding.

```
Sei un progettista di librogame. Ti do un ROMANZO FANTASY COMPLETO in
capitoli, piu' un documento di worldbuilding.

Il romanzo e' finito e scritto bene. NON devi riscriverlo, ne'
migliorarlo, ne' riassumerlo. Devi trasformarlo in un LIBROGAME, e per
farlo serve una cosa che il romanzo non ha: le strade alternative.

In un romanzo il lettore percorre un solo cammino. In un librogame ne
esistono decine, e circa il 40% delle tappe che attraversa deve essere
obbligatorio, il resto sceglibile. Il tuo lavoro e' trovare DOVE la
storia puo' biforcarsi senza tradirsi.

NON scrivere ancora le tappe. Voglio solo la STRUTTURA.

Rispondi con:

1. ARCO NARRATIVO (5-8 righe): come sale la storia, dove sta il punto
   di svolta, come si chiude.

2. Per ogni capitolo, una riga cosi':
   Capitolo <n> — <titolo> — <quante tappe> — DECISIONE: <la scelta
   vera che il lettore compie in questo capitolo>

   La decisione dev'essere gia' implicita nel testo: un momento in cui
   il protagonista sceglie e il romanzo racconta un solo esito. Non
   inventare svolte che contraddicono il seguito.

3. DEVIAZIONI (2 o 3 per capitolo): per ognuna
   - da quale tappa si stacca e a quale rientra (dopo 2-3 tappe, mai
     oltre 5);
   - cosa succede di diverso;
   - cosa cambia al lettore che la percorre (un oggetto, un'informazione,
     una ferita) — se non cambia niente, non e' una deviazione, e'
     una perdita di tempo.

4. SCONFITTE (3-5 in tutto): dove il lettore puo' morire o fallire, e
   per quale scelta sbagliata. Il romanzo non ne ha nessuna: le
   inventi tu, e devono essere credibili in quel punto della storia.
   Almeno una nella prima meta' del libro, o il lettore non crede al
   pericolo.

5. PUNTI DI RIENTRO: per ogni capitolo, la tappa in cui tutte le strade
   si ricongiungono. Una tappa su tre deve essere raggiungibile da piu'
   di un percorso.

6. COSA SI PERDE: se per ramificare hai dovuto spostare o tagliare
   qualcosa del romanzo, dillo qui in chiaro. Non farlo di nascosto.

Il totale deve stare fra 40 e 60 tappe.

ROMANZO E WORLDBUILDING:
[qui incolli i capitoli e il documento di worldbuilding]
```

### Giro B — un capitolo per volta

Da ripetere per ogni capitolo, allegando **la risposta del Giro A** e il
**solo capitolo** in lavorazione.

```
Questa e' la struttura del libro, gia' approvata:
[qui incolli la risposta del Giro A]

Ora lavora SOLO sul capitolo <n>. Numera le sue tappe da <inizio> a
<fine>; le tappe degli altri capitoli esistono gia' e puoi puntarci
scrivendo il loro numero.

COME TRATTARE IL TESTO
Il libro resta come l'autore l'ha deciso. Puoi fare tre cose, e solo
quelle:

- TAGLIARE in tappe: sempre, e non e' una modifica. Dove tagli e' una
  tua scelta, cosa c'e' scritto no.
- AGGIUNGERE UNA FRASE per chiudere una tappa, quando il taglio la
  lascerebbe monca. Una frase, non un paragrafo — e va DICHIARATA (vedi
  il campo "Testo" nel formato).
- SEGNALARE tutto il resto senza toccarlo: persona verbale sbagliata,
  refusi, pezzi che non funzionano come tappa. Decide l'autore.

Non riscrivere, non accorciare le descrizioni, non convertire la
persona o il tempo, non correggere errori. Se ti viene la tentazione di
"far scorrere meglio" una frase dell'autore, non farlo: e' esattamente
il modo in cui un testo smette di essere suo senza che nessuno se ne
accorga.

Scrivi di tuo SOLO il testo delle deviazioni e delle sconfitte, che nel
romanzo non esistono — e solo quando ti viene chiesto esplicitamente.
Li' imita il tono del capitolo.
- Conserva i marcatori del testo dove li trovi: *[Inizio Scontro]*,
  *[Fine Scontro]*, [Disciplina: Nome]. Servono al motore di gioco.
- Ogni tappa chiude sulla soglia di una decisione, mai su "prosegue".

FORMATO
## Tappa <numero> — <titolo breve>
- **Dove**: <luogo>
- **Testo**: <la prosa della tappa, presa dal capitolo o scritta da te
  se e' un ramo nuovo>
- **Scelte**:
  - <cosa fa il lettore> -> Tappa <numero>
  - <cosa fa il lettore> -> Tappa <numero>

Per i finali, al posto delle scelte:
- **Finale**: vittoria | sconfitta | neutro

CAPITOLO <n>:
[qui incolli il solo capitolo <n>]
```

Poi si prosegue con la **Fase 2** qui sopra, invariata, e si verifica
con `validate` e `forma`.

> **Se il romanzo ha un autore vivo e disponibile**, questa variante non
> basta: le scene alternative le deve scrivere lui, non il modello — una
> voce imitata si riconosce alla seconda riga. Il metodo per farlo (come
> si commissionano le scene a uno scrittore che non deve mai vedere un
> grafo) sta in **`DA-ROMANZO-A-LIBROGAME.md`**.

## Note

- **Il modello locale non c'entra.** Questi due prompt sono per un'IA
  remota usata in fase di scrittura. Gemma sul telefono fa un altro
  mestiere: arricchisce o traduce una scena per volta, a partita in
  corso.
- **Un libro così è distribuibile**: è materiale originale, non deriva
  da Project Aon (vedi README §15).
- Origine: [issue #1](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/1),
  aperta il 21/07/2026 da un'idea di un familiare di Michele, ripresa il
  05/08/2026.
