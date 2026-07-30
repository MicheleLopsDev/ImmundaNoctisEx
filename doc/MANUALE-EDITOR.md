# Manuale dell'editor — come si usa

Questa è la guida pratica per usare l'editor grafico di ImmundaNoctisEx
(il programma che apre una finestra con la mappa del libro): spiega
**cosa vedi e cosa clicchi**, non come è fatto dentro. Non serve
conoscere JSON, programmazione o come è scritto il motore di gioco —
serve solo saper scrivere una storia e seguire questa guida.

Per la documentazione tecnica (per chi sviluppa l'editor, non per chi
scrive libri) vedi `doc/EDITOR.md`.

---

## 1. Avviare l'editor

Se hai ricevuto una cartella con dentro `ImmundaNoctisEx-Editor.exe`,
basta fare doppio click su quel file — non serve installare nulla
(la cartella contiene già tutto il necessario). Se invece stai
sviluppando dentro Android Studio, si avvia con:

```bash
./gradlew :tool:run
```

Si apre una finestra ridimensionabile, con dentro la **schermata
iniziale**.

## 2. La schermata iniziale

Tre pulsanti:
- **"Carica libro esistente"** — apre un libro già scritto (un file
  `.json`).
- **"Crea libro nuovo"** — comincia un libro da zero.
- **"⚙ Impostazioni"** — tema, carattere, grandezza del testo (§8).

In basso a destra, su **ogni schermata dell'editor**, un pulsante
alterna il tema chiaro/scuro (**"🌙 Scuro"** quando sei in chiaro,
**"☀ Chiaro"** quando sei in scuro).

## 3. Aprire un libro già esistente

Click su **"Carica libro esistente"**: si apre la finestra di sistema
per scegliere un file (filtro automatico sui `.json`). Due esiti:

- **Il libro è a posto** → si apre direttamente sulla **mappa**
  (§5).
- **Il libro ha problemi** → schermata **"Libro NON VALIDO"** (in
  rosso), con l'elenco degli errori trovati e un pulsante **"Torna
  all'avvio"**. Nessuna modifica è possibile finché il file non viene
  corretto (a mano, o da chi sa leggere l'errore mostrato).

## 4. Creare un libro nuovo

Click su **"Crea libro nuovo"**:

1. **"Titolo del libro"** — obbligatorio.
2. **"Tono narrativo (puoi sceglierne più di uno)"** — checkbox su una
   lista di toni già pronti (es. cupo, eroico, misterioso...): scegli
   quelli che vuoi, influenzano lo stile con cui l'IA racconterà le
   scene. Non puoi scriverne uno tuo qui: solo quelli della lista, per
   evitare refusi che l'IA non capirebbe.
3. **"Da dove iniziare"** — tre modelli di partenza già pronti:
   - **Base** — solo inizio e fine, scrivi tu tutto il resto.
   - **Lineare** — una sequenza già pronta di 5 scene, da riempire.
   - **Ramificato** — un bivio con due percorsi ("Percorso A"/
     "Percorso B") che si ricongiungono in un finale comune.

   Tutti e tre includono già una scena "morte" di riserva (vedi §5,
   "rete di sicurezza per le nuove scene") — è normale vederla isolata
   sulla mappa, non è un errore.
4. Click su **"Crea"**: si apre subito la finestra per scegliere dove
   salvare il file (un libro appena nato non ha ancora un nome file).
   Se annulli questa finestra, il libro NON viene creato. Se scegli un
   percorso, il libro è salvato e ti ritrovi direttamente sulla mappa.

## 5. La mappa: la vista d'insieme del libro

Ogni **riquadro (nodo)** è una scena; ogni **freccia** è un
collegamento (una scelta del giocatore verso un'altra scena).

### Colori, sempre spiegati in una legenda in alto sulla schermata

- 🟩 collegamento valido
- 🟥 collegamento che punta a una scena che non esiste (più) — non
  blocca nulla, ma va sistemato prima o poi
- 🟪 il percorso dall'inizio (START) fino alla scena sotto il mouse
  (appare passando il mouse su un nodo)
- grigio = tutto il resto del grafo, quando stai osservando un
  percorso
- 🩷 scena di **inizio** (START)
- 💛 scena di **finale** (ENDING)
- verde/rosso di sfondo sul riquadro (quando non c'è un'immagine di
  copertina) = "salute" del collegamento in ingresso, un modo rapido
  per notare colpi d'occhio problemi
- contorno **blu** = la scena che hai selezionato con un click
- contorno **arancione** = la scena trovata con la ricerca (§5.3)

Se una scena ha un'immagine assegnata, quella riempie il riquadro al
posto del colore verde/rosso (resta solo un piccolo pallino colorato
in alto a destra per non perdere l'informazione).

### 5.1 Muoversi sulla mappa

- **Rotella del mouse** — zoom avanti/indietro (anche coi pulsanti
  **"−"**/**"+"** in alto, stessa percentuale mostrata in mezzo).
- **Trascina con il tasto sinistro sullo sfondo** (fuori da un
  riquadro) — sposta la visuale (pan).
- **Click sinistro su un riquadro** — lo seleziona (contorno blu).
- **Click sinistro sullo sfondo** (fuori da ogni riquadro) —
  deseleziona.
- **Doppio click su un riquadro** — apre quella scena per modificarla
  (§6).
- **Trascina un riquadro** — lo sposta a mano sulla mappa (solo per
  vederci meglio: questa posizione non si salva nel libro, si perde
  ricaricandolo o premendo "⟳ Riordina").
- **Tasto centrale del mouse** (sullo sfondo) oppure pulsante **"⟳
  Riordina"** — riporta tutto in ordine automatico: zoom al 100%,
  vista centrata, e annulla tutti gli spostamenti manuali fatti a
  mano.
- **"↔ Orizzontale"** / **"↕ Verticale"** — cambia il verso in cui la
  mappa si dispone da sola (in colonne o in righe).
- Passando il mouse sopra un riquadro **senza cliccare**, l'editor
  evidenzia in viola il percorso dall'inizio del libro fino a lì, e
  attenua tutto il resto: utile per capire "come ci si arriva" a una
  scena specifica.

### 5.2 Aggiungere ed eliminare scene

- **"+ Nuova scena"** — crea subito una scena vuota e apre il suo
  editor. Se, quando la salvi, non hai ancora collegato nessuna
  scelta/disciplina/combattimento in uscita, l'editor aggiunge da
  solo un collegamento di riserva verso la scena di morte del libro
  ("rete di sicurezza") — così non lasci mai per sbaglio un vicolo
  cieco che blocca il gioco.
- **"🗑 Elimina scena"** — attivo solo se hai selezionato un riquadro
  (contorno blu). Chiede conferma: **"Eliminare la scena ID?"**,
  avvisando che i collegamenti di altre scene verso quella eliminata
  resteranno (si vedranno rossi sulla mappa, l'editor non li corregge
  da solo).

### 5.3 Cercare una scena

Il campo **"Cerca scena per ID o testo..."** + pulsante **"🔍 Trova"**
cerca per numero/ID della scena o dentro il testo narrato. Funziona
come la ricerca di un browser: premendo di nuovo "Trova" con la stessa
ricerca passi al risultato successivo, ricominciando dal primo dopo
l'ultimo. Se non trova nulla, scrive **"Nessuna corrispondenza"**.

### 5.4 Salvare il libro

- **"💾 Salva libro"** — sovrascrive il file. La PRIMA volta che lo fai
  in una sessione di lavoro, chiede conferma spiegando che verrà
  creata una copia di sicurezza automatica (§9); le volte successive
  non chiede più (fino a quando non chiudi l'editor). Dopo il
  salvataggio compare per un momento la scritta **"✓ Salvato (backup
  in NOME.bak1)"**.
- **"Salva con nome…"** — salva su un nuovo file, scelto dalla
  finestra di sistema.
- **"🔍 Valida libro"** — controlla tutto il libro e apre un popup:
  **"Libro VALIDO (N avvisi)"** oppure **"Libro NON VALIDO"**, con
  l'elenco di errori e avvisi. Da fare ogni tanto, specialmente prima
  di chiudere una sessione di lavoro lunga.

### 5.5 Le immagini e i suoni del libro

Pulsante **"🔗 Risorse url:"** — apre un pannello con tre elenchi:

1. **Immagini in uso nelle scene** — tutte le immagini che compaiono
   già in qualche scena. Per ognuna puoi: **"+ Registra"** (darle un
   nome comodo da riusare altrove), **"✎"** (cambiarne il link — la
   modifica si applica ovunque quell'immagine compaia, in tutte le
   scene), **"🗑"** (rimuoverla ovunque compaia).
2. **Altre immagini registrate (non ancora usate in una scena)** —
   immagini che hai salvato con un nome ma non hai ancora messo in
   nessuna scena; stesso **"✎"**/**"✕"**, più un modulo per
   aggiungerne di nuove (campi **"ID"** e **"https://..."**, pulsante
   **"+ Aggiungi"**).
3. **Suoni personalizzati** — stessa cosa, per l'audio.

Quando scrivi un'immagine/suono nella scheda di una scena, puoi
scegliere tra due modi: un **ID già pronto** del catalogo dell'app
(scritto semplicemente come il nome, es. `loc_alley`), oppure un
**link tuo** che inizia per `url:` (es. `url:https://...`) — l'editor
ti suggerisce entrambe le opzioni mentre digiti, in una lista sotto il
campo, e mostra un'anteprima appena scrivi qualcosa.

## 6. Modificare una scena

Doppio click su un riquadro della mappa (o subito dopo "+ Nuova
scena"). In alto, due pulsanti alternano la vista: **"Maschera"** (i
campi spiegati sotto, il modo normale di lavorare) e **"JSON"** (il
testo grezzo, solo se sai già cosa stai facendo — passare da una vista
all'altra non perde le modifiche fatte).

### Campi della scena

- **"Testo narrato"** — il testo della scena. Obbligatorio.
- **"Sfondo (static:id o url:...)"** — l'immagine di ambientazione.
  Se è un'immagine del catalogo con un suono ambientale collegato,
  compare un pulsante **"▶ Ascolta"** per sentirlo in anteprima (si
  disabilita mentre suona, per evitare che due suoni si sovrappongano
  cliccando più volte; si interrompe da solo se chiudi la scena).
- **"Ritratto NPC (static:id o url:...)"** — l'immagine di un
  personaggio, se la scena ne ha uno.
- **Combattimento** — pulsante **"+ Aggiungi combattimento"** (diventa
  **"✕ Rimuovi combattimento"**): se attivo, compaiono i campi nome
  nemico, immagine, Combattività, Resistenza, scena se vinci, scena se
  perdi (opzionale), scena se fuggi (opzionale), dopo quanti round si
  può fuggire, e una casella "Immune a MINDBLAST".
- **Scelte** — una riga per ogni scelta del giocatore: il testo che
  legge, e la scena a cui porta (con suggerimenti mentre digiti).
  Pulsante **"+ Aggiungi scelta"** per aggiungerne altre, **"✕"** per
  toglierle.
- **Scelte legate a una disciplina** — come sopra, ma disponibili solo
  se il personaggio ha una certa disciplina (Kai) scelta da un menu a
  tendina.

Una scelta che punta a una scena che ancora non esiste **non ti
impedisce di salvare** — è normale mentre scrivi in ordine sparso, si
vedrà solo rossa sulla mappa finché non scrivi anche quella scena.

### Salvare la scena: il colore ti dice cos'è successo

Pulsante **"Salva scena"** in fondo:

- **Sfondo rosso** — mancava qualcosa di obbligatorio (es. testo
  narrato vuoto, una scelta senza testo o senza destinazione). Il
  pannello **resta aperto** e compare un popup **"Impossibile
  salvare"** con scritto esattamente cosa manca; chiudi il popup e
  correggi.
- **Sfondo giallo, poi si chiude da solo** — salvata, ma con qualche
  riferimento "in avanti" a una scena non ancora scritta (normale,
  vedi sopra).
- **Sfondo verde, poi si chiude da solo** — salvata senza problemi.

Pulsante **"Ritorna"**: se la scena era appena stata creata da "+
Nuova scena" e non l'hai ancora salvata, annulla la creazione (sparisce
del tutto, niente residui vuoti); se la scena esisteva già, torna
semplicemente alla mappa senza applicare modifiche non salvate.

## 7. Impostazioni

Dalla schermata iniziale, **"⚙ Impostazioni"**:

- **Tema** — Chiaro/Scuro.
- **Font** — 4 caratteri a scelta (Almendra, Cinzel Decorative,
  MedievalSharp, Uncial Antiqua), applicati a tutta l'interfaccia.
- **Grandezza testo** — tre passi (**"A-"**/**"A"**/**"A+"**).

Tutte e tre le scelte restano memorizzate anche dopo aver chiuso e
riaperto l'editor.

## 8. Le copie di sicurezza automatiche

Ogni volta che salvi un libro (sovrascrivendo il file), l'editor crea
prima una copia del file **così com'era prima** — si accumulano fino
a 5 copie via via più vecchie (`NOME.json.bak1` la più recente,
`.bak5` la più vecchia), nella stessa cartella del libro. Se ti accorgi
di un errore grosso appena fatto, puoi recuperare rinominando la copia
`.bak1` sopra al file principale.

Non è una cronologia completa (solo le ultime 5 versioni, non "torna a
ieri"): per una sicurezza più solida su un libro importante, tieni tu
stesso delle copie separate ogni tanto (es. su una chiavetta o un
cloud), specialmente prima di sessioni di lavoro lunghe.

## 9. Un limite da conoscere

L'editor è pensato per libri fino a circa **350 scene**. Oltre questa
soglia continua a funzionare (avvisa, non blocca), ma non è ancora
stato verificato a fondo che resti comodo da usare — i libri più
lunghi già provati arrivano fino a circa 400 scene.
