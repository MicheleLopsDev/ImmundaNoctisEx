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
- Il protagonista è "tu" (seconda persona singolare).
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
9. Gli "outcome" dei finali sono quelli dichiarati nelle tappe
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
