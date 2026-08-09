# Da romanzo a libro-game: il metodo uomo-modello

Michele, 06/08/2026: *"lo scrittore che conosco non vuole scrivere
libri game, lui crea romanzi… lui può riscrivere quante scene vogliamo
ma dobbiamo dirgli quali scrivere e cosa"*.

Questo documento è il metodo. `PROMPT-GENERAZIONE-LIBRI.md` contiene i
prompt, `FORMA-DEI-GRAFI.md` i numeri che lo governano.

## La regola che viene prima di tutte

> **Il libro resta come l'autore l'ha deciso. Ogni tocco del modello è
> minimo e dichiarato.**

Michele, 07/08/2026: *"il libro deve rimanere per lo più come lo
scrittore ha deciso; noi possiamo tagliare, cambiare minimamente per
chiudere una scena — questo è accettabile come cambio, ma deve essere
anche questo segnalato — e segnalare la terza persona"*.

Cioè: non un divieto, ma la **tracciabilità**. Tre livelli.

| il modello | |
|---|---|
| **taglia** in tappe | sempre, e non conta come modifica: il testo resta intatto |
| **aggiunge una frase** per chiudere una tappa | ammesso, ma va **dichiarato** su quella tappa |
| **riscrive, converte la persona, accorcia, corregge refusi** | mai — si segnala e decide l'autore |

Perché la distinzione conta. Nasce da una proposta sbagliata: avevo
suggerito di far convertire al modello i sei capitoli dalla terza alla
seconda persona. Una conversione riga per riga è **mille
micro-decisioni**, e a ognuna il modello può aggiungere un gesto che «fa
scorrere meglio»: dopo mille frasi il testo non è più quello
dell'autore, e nessuno se n'è accorto perché nessun singolo cambiamento
sembrava sbagliato.

Una frase di raccordo per chiudere una tappa è un'altra cosa: è **una**,
serve al taglio, e — se dichiarata — l'autore la vede e la accetta o la
rifiuta. Il pericolo non era il cambiamento: era il cambiamento
invisibile.

### Come si dichiara

**Solo** le tappe che il modello ha toccato dicono qualcosa:

```
- **Testo**: aggiunta in coda «Ti volti verso la porta.», per chiudere
             la tappa
```

Le altre non scrivono niente. È una regola imparata sbagliando: la
prima versione faceva dichiarare a ogni tappa di essere intatta, e su
58 tappe uscivano 56 righe identiche in mezzo a cui le 2 vere non si
vedevano più. Una dichiarazione che c'è sempre non dichiara niente —
vale qui come vale per la forma narrativa, che infatti si dichiara una
volta per capitolo.

Il conteggio finale di ogni capitolo riporta **quante tappe hanno il
testo modificato**: è quello il numero che l'autore controlla, non le
cinquanta tappe una per una.

**Ciò che il modello non può fare, lo segnala.** Il controllo della
persona in Fase 0 nasce così: se il romanzo è in terza persona, il
modello lo dice tappa per tappa e ne conta quante sono — poi o riscrive
l'autore, o si tiene così. Non converte.

Per questo la [`GUIDA-PER-LO-SCRITTORE.md`](GUIDA-PER-LO-SCRITTORE.md)
esiste: le convenzioni vanno date **prima** che scriva, non applicate
dopo su ciò che ha scritto.

## Il problema, detto in una riga

Un romanzo ha **un cammino**; un libro-game ne ha decine. Il romanzo
esiste già ed è buono. Manca tutto ciò che il lettore avrebbe potuto
fare *invece*.

Quel materiale lo può scrivere solo lo scrittore — un modello che imita
la sua voce si riconosce alla seconda riga. Ma lo scrittore non sa
**quali** scene mancano, perché per saperlo bisogna ragionare sul grafo,
che non è il suo mestiere e non deve diventarlo.

## I tre ruoli

| Chi | Cosa fa | Cosa NON vede mai |
|---|---|---|
| **Lo scrittore** | Consegna il romanzo, poi risponde alle domande scrivendo scene | Numeri di tappa, grafi, JSON, la parola "riconvergenza" |
| **Il modello** | Legge, taglia in scene, **fa le domande**, integra le risposte, e **sorveglia i numeri** | — |
| **Il curatore** (Michele) | Approva il sunto e le domande prima che partano | — |

Il modello non scrive il libro. **Fa le domande e tiene il conto.**

## Da dove nascono le domande

Non da una quota da riempire: **dal testo**. Il romanzo dichiara già
dove si combatte e dove si usa un'abilità, e ognuno di quei punti è una
casella vuota del nostro JSON.

| Nel romanzo c'è | Il JSON ha bisogno di | Quindi si chiede |
|---|---|---|
| uno scontro | `combat.loseSceneId` | *"se perde, cosa gli succede?"* |
| un uso di abilità | il ramo di chi non ce l'ha | *"cosa fa chi non ha quell'abilità?"* |
| una decisione | l'altra `choice` | *"e se avesse scelto il contrario?"* |

**Il dato che rende tutto necessario**: il giocatore sceglie **5
discipline su 10** alla creazione. Il Libro I ne usa **37 volte**.
In 37 punti c'è circa una probabilità su due che il lettore *non abbia*
quello che il protagonista usa — e senza una via alternativa, lì il
libro non è giocabile. I rami non sono una scelta di design: sono una
necessità meccanica, e il testo dice esattamente dove.

**Non si chiede per tutte e dieci le abilità.** Su 28 scene farebbe 280
domande, e nei libri di Dever il 91% delle scene non ha nessuna
disciplina usabile: sarebbero centinaia di "non supportata" e lo
scrittore si stanca prima del Capitolo 2. Si chiede solo dove il testo
già usa qualcosa. In più, separatamente, il modello può *proporre*
cinque o sei scene dove un'altra abilità avrebbe senso — come
suggerimento da accettare o scartare, non come questionario.

## Il ciclo

```
   romanzo
      ↓
  [1] il modello taglia in scene  →  SUNTO  →  [curatore approva]
      ↓
  [2] il modello fa le DOMANDE (una per casella vuota)
      ↓
  [3] lo scrittore risponde scrivendo le scene
      ↓
  [4] il modello INTEGRA — spezza le risposte lunghe in più scene
      ↓
  [5] il modello CONTA e avvisa ────┐
      ↓                             │ restano caselle vuote?
   niente più caselle vuote?        └──────► torna a [2]
      ↓
  validate + forma + morti su misura  →  il libro si gioca
```

Il ciclo **termina da solo**: si chiude quando nessun punto di gioco è
rimasto senza risposta. Non serve decidere a occhio quando basta.

### Il freno alla ricorsione

I rami nuovi possono contenere altri scontri e altre abilità, e
generare altre domande. Perché il giro finisca:

> **Dal secondo livello in poi, i rami rientrano.** Possono avere uno
> scontro, ma quello scontro non apre altri mondi: le sue uscite
> tornano su scene che esistono già.

**«Tutta la storia fino al finale» resta un'eccezione**: va bene per
uno o due rami maggiori, dichiarati in partenza, che meritano un finale
alternativo vero. Se lo si concede a tutti, il libro raddoppia a ogni
giro.

## Cosa sorveglia il modello, giro per giro

Michele: *"se vede che lo scrittore sta andando troppo in là, gli dice
attenzione, non aggiungete più nuove sottotrame, oppure attenzione il
numero delle scelte è ormai compiuto"*.

Le misure non servono solo alla fine come collaudo: servono **a ogni
giro come termostato**. A fine integrazione il modello confronta lo
stato con i bersagli e dichiara se si è sotto, in linea o oltre.

| Cosa | Bersaglio | Se si è sotto | Se si è oltre |
|---|---|---|---|
| scene totali | 40-60 | c'è spazio | **stop: non aggiungere scene, collega quelle che ci sono** |
| uscite per scena | 1,65 | servono altre scelte | **le scelte bastano** |
| scene con più vie | 32% | i rami non rientrano | in linea |
| rientro dei rami | 3 tappe, mai oltre 5 | — | **rami troppo lunghi: accorciali** |
| cammino obbligato | 42-46% | manca la spina dorsale | servono scorciatoie |
| può ancora vincere | ≥ 86% | **troppe strade condannate: falle rientrare** | — |
| finali | 1 vittoria, 3-5 sconfitte | ne mancano | bastano |
| rami che non rientrano | 1-2 in tutto | — | **stop alle sottotrame** |

Da quando esiste un JSON parziale — cioè dal primo giro di
integrazione in poi — questa tabella non si compila a mano:

```bash
./gradlew :tool:cli --args="forma ../percorso/del/libro.json"
```

Il comando misura e **dice quanto manca**, non solo cosa non va:
*«aggiungi ~18 scelte»*, *«fai rientrare ~8 rami»*, *«servono ~7
scorciatoie»*. È quello che il modello gira allo scrittore al giro
dopo, tradotto in domande.

## Il formato di una domanda

Scritto **nella lingua dello scrittore**. Il rientro non è un numero di
nodo: è una frase del suo romanzo, copiata esatta.

```
### D-07 — Cap. 3, lo scontro coi banditi nella notte

**Dove siamo**: Ariel e il capitano Tobias, la strada per Basara,
dopo il guado. Ariel ha ancora l'Astro di Giada e le due Pozioni di
Cura; non ha ancora incontrato Agata.

**La domanda**: nel romanzo Ariel vince lo scontro. **Se lo perde,
cosa gli succede?**

**Le strade possibili** (scegline una e scrivila):
  a) sopravvive ma perde qualcosa — e allora la storia riprende da
     dove il capitolo dice: «All'alba il profilo di Basara si stagliò
     contro il cielo lattiginoso»;
  b) muore, e questo è un finale: dopo non c'è nulla.

**Quanto**: 250-350 parole.

**Attenzione a**: Tobias deve restare vivo, serve al Capitolo 5.
```

Le regole che governano le schede:

- **«la storia riprende da»** è il punto di rientro come citazione. Se
  manca, è un finale, e va detto.
- **«Attenzione a»** protegge il romanzo: dice cosa non si può rompere
  perché serve più avanti.
- **«Dove siamo»** elenca cosa il protagonista ha in mano in quel
  punto: è ciò che permette allo scrittore di scrivere senza rileggere
  tutto, e a noi di sapere quali oggetti il ramo può usare.

### Niente righe che non dicono niente

Solo tre righe sono obbligatorie: **Dove siamo**, **La domanda**,
**Quanto**. Le altre compaiono **solo se hanno qualcosa da dire**
(Michele, 08/08/2026: *"se una domanda non aggiunge nulla al contesto
per lo scrittore… è inutile presentarla nel documento"*).

Concretamente, non si scrive mai:

```
**Attenzione a**: niente in particolare
**Le strade possibili**: quelle che preferisci
**Bivio forzato**: no
```

Quella è la scheda che dà a chi legge il lavoro di scartare le righe
vuote. Peggio: quando ogni scheda ha un «Attenzione a», lo scrittore
smette di leggerlo — e quello vero, sulla scheda dove Tobias deve
restare vivo, passa inosservato in mezzo a venti «niente in
particolare».

Stessa regola un piano più su: **una tappa senza niente da chiedere non
diventa una scheda.** Ventidue domande che valgono sono un lavoro di
tre settimane; cinquanta di cui trenta ovvie sono un lavoro che
l'autore abbandona alla quindicesima.

È lo stesso principio della forma narrativa e delle note di
lavorazione: una dichiarazione che c'è sempre non dichiara niente.

## Quanto lavoro è, sul Libro I

| | |
|---|---|
| romanzo | 7.581 parole, 6 capitoli |
| scene stimate | ~28 |
| scontri marcati | 6 → 6 domande |
| usi di disciplina | 37 → 37 domande |
| **primo giro** | **~43 domande** |

Molte hanno risposte corte (*"perde la spada e prosegue zoppicando"*).
Le altre valgono 250-350 parole. I giri successivi calano in fretta:
i rami di secondo livello rientrano e basta.

## Cosa arriva già fatto, e non è poco

Siccome le domande nascono dai punti di gioco, le risposte arrivano
**già mappate sui campi del JSON**: *"se perde"* → `loseSceneId`,
*"chi non ha Scudo Mentale"* → il ramo alternativo della
`disciplineChoice`. Le meccaniche non sono un lavoro separato da fare
dopo nell'editor: nascono insieme al testo.

Restano da mettere a mano solo le cose che il romanzo non può sapere:
i valori di Combattività e Resistenza dei nemici, i tiri della tabella,
i bonus. Quelle sono decisioni di gioco.

## Alla fine

1. **`validate`** — il grafo regge?
2. **`forma`** — ha la forma di un libro-game? (`FORMA-DEI-GRAFI.md`)
3. **Morti su misura** — le sconfitte generiche diventano scene scritte.
   Nei librogame veri sedici finali su diciassette sono morti con la
   loro prosa: una morte anonima è tempo del lettore buttato.
4. Numeri dei nemici, tiri, oggetti: nell'editor.

## Perché questo giro può reggere

Ogni pezzo è dato a chi lo sa fare: lo **scrittore** scrive prosa,
l'unica cosa che un modello non sa imitare senza che si veda; il
**modello** legge, taglia, chiede e tiene il conto; il **curatore**
decide, che è l'unica cosa che nessuno dei due può fare al posto suo.

E il criterio di fine non è un'opinione: **le caselle vuote sono
finite, e i numeri lo confermano**.
