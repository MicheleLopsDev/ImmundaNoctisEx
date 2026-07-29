# EDITOR.md — Specifica del tool grafico di authoring (`:tool`)

Documento di specifica per la parte grafica di `:tool`: un editor visuale
che disegna la mappa delle scene di un libro e permette di crearlo o
modificarlo, girando su PC dentro Android Studio. Nasce come seconda fase
dopo l'ETL (conversione Project Aon → JSON, vedi `doc/ETL.md`): quella
parte resta CLI e non cambia; questo documento riguarda solo l'aggiunta
di un'interfaccia grafica allo stesso modulo.

Definito insieme a Michele per conversazione (29/07/2026), stesso metodo
delle altre specifiche (`doc/ARCHITETTURA.md`, `doc/REGOLE.md`,
`doc/STATO.md`, `doc/UI.md`, `doc/ETL.md`).

---

## 1. Scopo

Un libro (`Manifest` + `Scene[]`) oggi si scrive a mano in JSON o si
ottiene dall'ETL. Sopra le ~50 scene diventa scomodo tenere a mente la
struttura del grafo solo leggendo testo. Questo tool deve permettere di:

- **vedere** il grafo delle scene di un libro come una mappa navigabile;
- **modificare** un libro già esistente (uno dei 5 convertiti in
  `doc/LIBRI/`, o uno dei `content/test-books/`);
- **creare un libro nuovo da zero**, partendo da uno scaffold guidato.

È pensato per uso di Michele (rifinire libri convertiti) e di suo figlio
(scrivere libri originali senza conoscere Lupo Solitario, vedi memoria
"direzione_creativa_contenuti_adulti" e "tool_authoring_scope_ampliato") —
**deve essere utilizzabile da chi non conosce lo schema JSON a memoria**.

## 2. Tecnologia

**Compose Multiplatform Desktop**, sullo stesso modulo Kotlin/JVM
(`kotlin("jvm")` + `application`, non serve convertire `:tool` in un
modulo Kotlin Multiplatform vero e proprio): basta aggiungere al
`build.gradle.kts` esistente i plugin `org.jetbrains.compose` e
`org.jetbrains.kotlin.plugin.compose` (lo stesso compilatore Compose già
usato in `:app`) e la dipendenza `compose.desktop.currentOs`. Si lancia
con `./gradlew :tool:run` come i comandi CLI già scritti, e gira/debugga
dentro Android Studio senza uscirne — Android Studio è basato su
IntelliJ ed esegue moduli Gradle-JVM come questo indipendentemente dal
plugin "Kotlin Multiplatform" installato da Michele (utile per
l'ergonomia dell'IDE, non un requisito tecnico).

Motivazione della scelta (Michele): sviluppatore Java con background
Swing/JavaFX ma niente preferenza forte; Compose Desktop vince perché
resta nella stessa famiglia concettuale già in uso in `:app` e perché
permette di restare in Android Studio senza cambiare IDE o creare un
progetto separato.

## 3. Struttura del modulo

**Un solo modulo `:tool`**, nessun progetto/modulo nuovo. La CLI
esistente (`validate`/`convert`/`illustrazioni`, vedi `Main.kt`) e la
GUI convivono: un nuovo comando (es. `editor`) o un secondo entry point
apre la finestra Compose, riusando `core:data`/`core:engine` esattamente
come già fa la CLI (stesso `PackageRepository`/`PackageValidator`,
zero logica duplicata).

## 4. Perimetro (v1)

Dentro:
- Caricare un libro JSON esistente e visualizzarne/modificarne il grafo.
- Creare un libro nuovo da zero con scaffold guidato (§9).
- Editing completo di manifest e scene (campi, scelte, combattimento,
  discipline, immagini `static:`/`url:`).
- Validazione locale (per-scena) e globale (tutto il libro), con
  colorazione della mappa.
- Backup automatico su disco, nessuna integrazione Git (§10).

Fuori (rimandato, §13):
- Editor dei frammenti di prompt (`content/config.json`).

## 5. Flusso di avvio

All'apertura, due scelte:

1. **Carica un libro esistente** — apri un file JSON, il tool ne
   ricostruisce la mappa.
2. **Crea un libro nuovo** — prima un form per i campi globali del
   manifest (`title`, `genre`, `toneHints`, ecc. — vedi
   `doc/SCHEMA-JSON.md` §2), poi la scelta di uno dei tre scaffold
   (§9.2).

## 6. La mappa

### 6.1 Layout e navigazione

- **Auto-layout gerarchico** a partire dalla scena `START` (livelli per
  distanza nel grafo) — nessun posizionamento manuale dei nodi da fare
  a mano.
- **Pan e zoom** liberi sul canvas.
- **Ricerca** per ID scena o per testo nel `narrativeText`, che centra
  la vista sul nodo trovato.
- **Evidenziazione del vicinato**: selezionando un nodo, i suoi
  collegamenti diretti (entranti e uscenti) restano ben visibili, il
  resto della mappa si attenua senza sparire.

### 6.2 Colori: salute dei nodi e dei percorsi

Due segnali visivi distinti, sempre attivi (niente proliferazione di
colori oltre questi):

- **Sfondo del nodo** (salute della singola scena): **verde** se tutti
  i suoi riferimenti in uscita (`choice.nextSceneId`,
  `disciplineChoice.nextSceneId`, `combat.winSceneId`/`loseSceneId`/
  `evadeSceneId`) puntano a scene che esistono davvero nel libro;
  **rosso** se almeno uno punta a una scena non ancora creata (vedi
  §7.3 — riferimento "in avanti", non un errore da correggere subito).
- **Contorno del percorso** (salute del cammino): tracciando un cammino
  da `START` a un finale, il contorno è **verde** solo se OGNI nodo
  lungo il cammino è verde e il cammino arriva davvero a un finale non
  di sconfitta; basta un solo nodo rosso lungo il percorso per rendere
  rosso l'intero contorno (un anello debole sporca tutta la catena).
  Il contorno di un cammino specifico si mostra **a richiesta**,
  selezionando una scena e chiedendo "mostrami un percorso da START
  fin qui" — enumerare e colorare TUTTI i cammini possibili di un
  libro da centinaia di scene non è praticabile (crescita
  combinatoria) né leggibile.
- Riuso diretto della logica già scritta in `GraphValidator`
  (`core/data`), applicata nodo per nodo invece che come report unico
  di fine libro — nessuna nuova logica di validazione dei riferimenti,
  solo un nuovo modo di presentarla.

## 7. Editing di una scena

Selezionando un nodo si apre un pannello con **due viste della stessa
scena**, alternabili con un pulsante (stesso pattern dell'editor
XML/Design di Android Studio):

### 7.1 Vista maschera

Un campo per ciascun attributo dello schema (vedi `doc/SCHEMA-JSON.md`
§4), con i casi a "catalogo chiuso" presentati come menu a tendina
invece che testo libero:
- `backgroundImage`/`npcImage`/`combat.enemyImage`: scelta tra
  `static:<id>` (con l'elenco da `doc/SUONI-IMMAGINI.md`) o `url:` con
  campo libero per il link.
- `disciplineId` di una `DisciplineChoice`: le 10 discipline canoniche.
- Scelte (`choices`): lista con `choiceText`, destinazione (vedi
  sotto), `minRoll`/`maxRoll`/`requiredItem`/`requiredFlag`.
- Blocco `combat`: nome, immagine, statistiche, tre destinazioni.

**Destinazione di una scelta**: il tool propone la lista delle scene
già esistenti nel libro per agganciare subito una reale, ma permette
anche di scrivere un ID nuovo per una scena non ancora creata (pensata
ma non ancora scritta) — vedi §7.3 per cosa succede in quel caso.

### 7.2 Vista JSON

Editor di testo della scena in formato JSON grezzo, con gli stessi
errori di validazione mostrati a fianco — via di fuga per i casi che
la maschera non copre ancora (es. un nuovo `gameMechanic`).

### 7.3 Validazione locale (blocco in uscita)

Chiudere il pannello di una scena passa **sempre** da una validazione
locale, che blocca l'uscita se:
- manca un campo obbligatorio dello schema (es. `choiceText` vuoto,
  `narrativeText` vuoto, `combat.winSceneId` assente);
- il JSON (vista 7.2) non è ben formato.

**Non blocca** l'uscita se una scelta punta a una scena non ancora
creata — è un pattern di scrittura normale (si scrive in avanti e si
torna indietro dopo). In quel caso il nodo resta/diventa rosso (§6.2)
finché la scena mancante non viene creata.

### 7.4 Editing del JSON completo del libro

Oltre alla vista JSON della singola scena (§7.2), un secondo modo,
raggiungibile dalla barra strumenti principale (non dal pannello di
una scena): **modifica del `Manifest` intero come testo JSON**, per chi
preferisce lavorare sul file per intero invece che scena per scena.
Anche qui **nessuna scrittura su disco senza validazione**: applicare
le modifiche richiama la stessa validazione globale del pulsante di
§8 (`PackageValidator` sull'intero manifest) — se il libro risultante
non è valido, le modifiche non vengono accettate e restano a video con
l'elenco degli errori, esattamente come già succede oggi con la CLI
`validate`.

## 8. Validazione globale del libro

Un pulsante "valida tutto il libro" richiama `PackageValidator` così
com'è (stesso codice della CLI `validate`, zero duplicazione) e mostra
l'elenco di errori/avvisi — compresi i riferimenti ancora "in sospeso"
che durante la scrittura sono normali, ma che vanno risolti prima di
considerare il libro finito o di volerlo esportare/distribuire.

## 9. Creazione di un libro nuovo

### 9.1 Campi globali del manifest

Prima di generare lo scaffold, un form per i campi di livello manifest:
titolo, genere, `toneHints` (vedi `doc/SCHEMA-JSON.md` §2) — le
discipline restano le 10 canoniche, non richiedono input.

### 9.2 Scaffold: tre modelli di partenza

Tre opzioni, tutte con la rete di sicurezza di §9.3 già collegata:

1. **Base** — solo una scena `START` e una scena `ENDING`, il resto si
   scrive da zero.
2. **Lineare** — `START` + una catena di 4-5 scene concatenate +
   `ENDING`: esempio minimo di sequenza.
3. **Ramificato** — `START`, una scena centrale che si divide in almeno
   due percorsi (A e B) con un paio di scene ciascuno, poi confluenza
   in un `ENDING` comune: esempio di come funzionano le diramazioni.

Servono da esempio funzionante di come si struttura un libro a scelte
(un libro può essere semplice o complesso), non solo da punto di
partenza vuoto.

### 9.3 Rete di sicurezza (`deathSceneId`)

Ogni scaffold (tutti e tre) crea anche una scena `ENDING`/`DEFEAT` e
collega `manifest.deathSceneId` ad essa fin dall'inizio — un libro
nuovo non è mai sprovvisto della rete di sicurezza per la morte fuori
combattimento che il motore già prevede (`doc/REGOLE.md`).

`deathSceneId` resta un campo di **livello manifest** (non un
collegamento per-scena disegnato sulla mappa: nello schema non lo è, è
un fallback implicito controllato dal motore) — si mostra e si modifica
nel pannello delle proprietà globali del libro, non come arco visibile
tra ogni nodo e la scena di morte (evita di disegnare centinaia di
frecce ridondanti sulla mappa).

## 10. Backup e cronologia

- **Nessuna integrazione con Git**: il tool lavora solo su file JSON su
  disco, non esegue né gestisce comandi Git.
- **Backup automatico su disco** prima di ogni modifica sostanziale a
  una scena (es. `libro.json.bak1`, `libro.json.bak2`, ...,
  rotazione da definire in fase di implementazione) — permette di
  tornare indietro se una modifica va storta.
- Il salvataggio esplicito (azione dell'utente) scrive il file
  principale; i backup sono lo strumento per recuperare da un errore
  di editing, non un sistema di versioning completo.

## 11. Limiti dimensionali

Tetto auto-imposto di **~350 scene per libro** (i 5 libri Project Aon
di prova ne hanno 362-406, già oltre — restano utili per collaudare il
tool proprio perché stressano il limite). Oltre soglia, il tool avvisa
ma non blocca rigidamente: da confermare in fase di implementazione se
serve un margine per le scene sintetiche generate da altre parti della
pipeline (es. combattimenti multi-nemico, vedi `doc/ETL.md`).

## 12. Distribuzione e pacchettizzazione

Obiettivo di Michele: passare l'editor a suo figlio senza dargli
accesso al repository Git/GitHub — un pacchetto autonomo che installa
o si avvia da una cartella, punto. Il plugin `org.jetbrains.compose`
copre già questo caso: include il packaging nativo via **jpackage**
(tool del JDK, niente da installare a parte per costruire il
pacchetto), che imbustano anche la JVM — chi riceve il pacchetto non
deve installare Java a parte.

Due formati, entrambi dallo stesso plugin, nessun cambio di
tecnologia tra l'uno e l'altro:

- **Cartella portatile** (task Gradle `createDistributable`): una
  cartella con dentro un `.exe` e tutto il necessario. Si zippa, si
  manda, si scompatta ovunque e si avvia col doppio clic — nessuna
  installazione, nessun tool aggiuntivo da installare nemmeno sul PC
  che genera il pacchetto. **Opzione di partenza consigliata.**
- **Installer Windows vero** (task `packageMsi`/`packageExe`): stessa
  build, procedura guidata avanti-avanti-fine per chi lo riceve, ma
  richiede il **WiX Toolset** installato sul PC che genera il
  pacchetto (solo lato build, non per chi lo riceve) — passaggio
  successivo se si vuole un'esperienza da "programma installato" vera
  e propria, non una scelta obbligata fin da subito.

## 13. Fuori perimetro (rimandato)

- **Editor dei frammenti di prompt** (`content/config.json`): previsto
  dalla memoria di sessione come parte dello stesso tool, ma **non in
  questa prima versione** — specifica a parte in un secondo momento,
  per non appesantire questo documento con un ambito ancora da
  esplorare.

## 14. Bozze grafiche (mockup)

Wireframe finti, disegnati da Claude solo per dare un'idea della
disposizione — non un design definitivo, nessuno stile/colore qui è
vincolante. Coerenti con quanto chiesto: **il più semplice possibile**,
niente fronzoli, potente nel contenuto non nell'estetica.

File sorgente in `doc/editor-mockup/` — apri direttamente questi `.svg`
se il tuo editor Markdown non renderizza l'anteprima qui sotto.

### 14.1 Schermata di avvio (§5)

![Schermata di avvio: due bottoni, "Carica libro esistente" e "Crea libro nuovo"](editor-mockup/01-avvio.svg)

### 14.2 Finestra principale, la mappa (§6)

![Mappa con nodi verdi collegati in un percorso valido verso ENDING, un nodo rosso che punta a una scena non ancora creata, e il pannello di editing a destra](editor-mockup/02-mappa.svg)

### 14.3 Pannello di editing di una scena — vista Maschera (§7.1)

![Pannello scena in vista Maschera: testo narrato, immagine di sfondo a tendina, elenco scelte con destinazione, pulsante Aggiungi scelta](editor-mockup/03-scena-maschera.svg)

### 14.4 Stessa scena — vista JSON, con un errore (§7.2)

![Stesso pannello in vista JSON: testo grezzo della scena e un banner rosso con l'errore di validazione che blocca il salvataggio](editor-mockup/04-scena-json.svg)

## 15. Riferimenti

- `doc/ETL.md` — pipeline di conversione Project Aon → JSON, la parte
  CLI di `:tool` che questo documento estende con una GUI.
- `doc/SCHEMA-JSON.md` — schema completo di manifest/scene/choice/
  combat/gameMechanics che l'editor deve esporre in forma di maschera.
- `doc/REGOLE.md` — semantica di combattimento, discipline, morte fuori
  combattimento (`deathSceneId`).
- `doc/SUONI-IMMAGINI.md` — elenco completo degli ID `static:` del
  catalogo immagini, da presentare come menu a tendina nella maschera.
- `core/data/.../validation/PackageValidator.kt` (e i validatori che
  compone) — logica di validazione riusata sia per il pulsante "valida
  tutto il libro" sia per la colorazione dei nodi.
