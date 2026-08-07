# Guida per lo scrittore

Poche regole, con esempi. Servono a far sì che il tuo testo diventi un
libro-game giocabile **senza che nessuno lo riscriva**.

Non ti serve sapere come funziona il gioco: ti serve sapere queste sei
cose.

---

## 1. Scrivi in seconda persona

Il lettore **è** il protagonista. Non lo guarda: lo fa.

| | |
|---|---|
| ❌ | *Ariel scartò di lato per schivare il fendente.* |
| ✅ | *Scarti di lato per schivare il fendente.* |

È l'unica regola che cambia il modo in cui scrivi, e c'è un motivo
pratico: in un libro-game il lettore prende decisioni. «Ariel decise di
entrare» gli toglie la scelta che invece deve essere sua.

Il nome del protagonista puoi usarlo quando parlano gli altri
personaggi — *«Ariel! Aspetta!» esclamò la principessa* — e nelle
descrizioni in cui serve.

---

## 2. Segna gli scontri

Quando il protagonista combatte, il gioco deve saperlo: si ferma, tira i
dadi, conta i colpi. Perciò delimita la scena così:

```
*[Inizio Scontro]*

I colpi si susseguono con precisa disciplina. Scarti di lato per
schivare il fendente del manichino d'addestramento…

*[Fine Scontro]*
```

**Dentro i marcatori** metti solo il combattimento. Quello che succede
prima e dopo sta fuori.

Se sai chi è l'avversario, scrivilo in chiaro nella prima riga: *"Ti si
para davanti un Giak"*. I suoi valori (quanto è forte, quanta vita ha)
li mettiamo noi: non sono affar tuo.

---

## 3. Segna le abilità

Il protagonista ha **dieci doti**, ma il lettore ne sceglie solo
**cinque** all'inizio della partita. Quindi ogni volta che ne usi una,
metà dei lettori non ce l'ha — e a loro dobbiamo raccontare qualcosa di
diverso.

Per questo ogni uso va segnato:

```
Tracci un rapido segno con la mano sinistra e canalizzi un impulso di
forza cinetica [Disciplina: Dardo di Forza]. Un dardo di luce azzurrina
scaturisce dai polpastrelli…
```

Il marcatore va **dopo** la frase che descrive l'uso, non al posto suo:
il testo deve leggersi bene anche ignorandolo.

### Le dieci doti

| dote | quando si usa |
|---|---|
| **Percezione Arcana** | percepire anomalie, trappole, presenze nascoste, non-morti |
| **Scherma dell'Astra** | incanalare potere nei colpi di spada |
| **Dardo di Forza** | colpire a distanza con un'ondata cinetica |
| **Scudo Mentale** | difendersi da necromanzia, attacchi mentali, sussurri |
| **Velo d'Ombra** | nascondersi, infiltrarsi, non farsi vedere |
| **Telecinesi Arcana** | muovere oggetti a distanza, azionare leve, respingere |
| **Sostentamento Arcano** | resistere a fame, freddo, fatica |
| **Tracciamento Arcano** | seguire scie, rune residue, rotte magiche |
| **Rigenerazione Vitale** | rimarginare ferite col riposo |
| **Empatia Bestiale** | parlare con gli animali, calmarli, guidarli |

Usa **esattamente questi nomi**: sono quelli che il gioco riconosce.

---

## 4. Segna cosa il protagonista guadagna o perde

È la cosa più importante dopo la seconda persona, e la più facile da
dimenticare.

Ogni volta che il protagonista **ottiene** qualcosa che gli servirà più
avanti — un oggetto, un alleato, un'informazione — segnalo:

```
«Queste due sono Pozioni di Cura», spiega Lyra infilandoti la custodia
tra le mani. «E questa… questa è la Pozione Mangiaferro.»
[Ottieni: 2 Pozioni di Cura]
[Ottieni: Pozione Mangiaferro]
```

E quando lo **perde**:

```
La lama ti sfugge di mano e sparisce nell'acqua nera.
[Perdi: Spada]
```

**Perché conta tanto:** il lettore può arrivare in quel punto per una
strada diversa, senza quell'oggetto. Se sappiamo che lì si ottiene la
Pozione Mangiaferro, sappiamo anche che al Capitolo 4 la porta chiusa
può restare chiusa — ed è quello che rende le scelte importanti.

---

## 5. Come rispondere quando ti chiediamo una scena

Ti arriverà una scheda così:

```
### D-07 — Cap. 3, lo scontro coi banditi nella notte

Dove siamo: la strada per Basara, dopo il guado. Hai ancora l'Astro di
  Giada e le due Pozioni di Cura; non hai ancora incontrato Agata.
La domanda: nel romanzo vinci lo scontro. Se lo perdi, cosa ti succede?
Le strade possibili: (a) sopravvivi ma perdi qualcosa, e la storia
  riprende da dove il capitolo dice «All'alba il profilo di Basara si
  stagliò contro il cielo lattiginoso»; (b) muori, e qui finisce.
Quanto: 250-350 parole.
Attenzione a: Tobias deve restare vivo, serve al Capitolo 5.
```

Tu rispondi **scrivendo la scena**, come scriveresti un pezzo di
romanzo. Con le regole 1-4 di sopra, e due accortezze:

- se scegli una strada che **rientra**, chiudi la scena portando il
  protagonista nel punto indicato — la frase citata è dove il tuo testo
  si ricongiunge al capitolo;
- se scegli che il protagonista **muore o fallisce**, scrivilo per
  intero: non è un "hai perso", è la fine di una storia e merita le sue
  righe.

Se una domanda non ha senso — l'abilità non c'entra niente in quel
punto, o la scena non regge — **dillo**: scrivi «non ha senso» e il
perché. È una risposta utile quanto le altre.

---

## 6. Cosa NON devi fare

- **Non pensare ai bivi.** Non scrivere «se scegli di entrare vai al
  paragrafo 12»: i collegamenti li mettiamo noi. Tu scrivi la scena.
- **Non numerare le scene.** Se ne serve una nuova, ha il codice della
  domanda (`D-07`) e basta.
- **Non tagliare i capitoli in pezzi.** Scrivili interi, come faresti
  sempre: a spezzarli in tappe pensiamo noi.
- **Non spiegare le regole del gioco dentro il racconto.** Niente
  «tira un dado» o «se hai 12 punti Resistenza».

---

## In breve

| | |
|---|---|
| Persona | **seconda** — «Scarti di lato» |
| Combattimento | `*[Inizio Scontro]*` … `*[Fine Scontro]*` |
| Uso di una dote | `[Disciplina: Nome]` dopo la frase |
| Oggetto ottenuto | `[Ottieni: cosa]` |
| Oggetto perso | `[Perdi: cosa]` |
| Formato del file | testo o Markdown, UTF-8 |
| Nome del file | il codice della domanda: `D-07.md` |

Tutto il resto — bivi, numeri, dadi, statistiche — è nostro.
