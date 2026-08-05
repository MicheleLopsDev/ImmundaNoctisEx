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
   staccano da una tappa e RIENTRANO nella spina entro 2-3 tappe. Non
   creare rami che non rientrano mai, tranne i finali.
4. Prevedi 2-4 FINALI: almeno una vittoria, almeno una sconfitta.

REGOLE
- Ogni tappa deve chiudersi con una decisione vera del protagonista, non
  con "prosegue".
- Le scelte devono avere conseguenze diverse: se due scelte portano allo
  stesso posto senza differenza, accorpale.
- Il protagonista è "tu" (seconda persona singolare).
- Non scrivere ancora la prosa delle scene: solo cosa succede.

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

Se il validatore passa, il libro si apre nell'editor e si gioca. Da lì in
poi si aggiungono combattimenti, discipline, tiri e oggetti — a mano,
perché sono decisioni di gioco.

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
