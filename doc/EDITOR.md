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
- **Pan e zoom** liberi sul canvas, con un controllo esplicito in
  barra strumenti oltre al gesto sul canvas (rotellina/pinch): due
  pulsanti `−`/`+` (scelta di partenza, più semplice da implementare di
  uno slider e coerente con "il più semplice possibile") — uno slider
  resta un'alternativa se in prova si rivela più comodo per un
  controllo fine.
- **Pulsante "Riordina automaticamente"**: ritraccia da capo
  l'auto-layout gerarchico su richiesta — utile dopo aver spostato dei
  nodi a mano o dopo aver aggiunto/rimosso scene, per tornare alla
  disposizione pulita senza dover ricaricare il libro.
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

## 15. Seconda fase: risorse, interazione e qualità di scrittura (30/07/2026)

Specifica aggiunta dopo la v1 (§1-§14, implementata e in uso — vedi
`doc/DIARIO.md` per la cronologia dei giri di correzione). Discussa a
lungo per conversazione con Michele lo stesso giorno, **da fare in più
sezioni separate** (non tutto in una sessione): questo capitolo fissa
cosa si è deciso, in modo da poter riprendere da qui in ciascuna.
Sottosezioni pensate come unità di lavoro indipendenti, nell'ordine
proposto §15.1 → §15.8 (§15.1 già fatta).

### 15.1 Registro delle risorse statiche — FATTO

`content/static-resources.json`: istantanea di `SceneImageCatalog`/
`NpcImageCatalog`/`EnemyImageCatalog` (`:app`), letta solo dall'editor
(`StaticResourceCatalog.kt`, `:tool/editor`). Il client resta
invariato: la fonte di verità per il gioco vero restano i cataloghi
Kotlin in `:app`, questo file è una copia. Copiato anche in
`tool/src/main/resources/` insieme alle immagini JPG, così l'editor
mostra anteprime vere anche impacchettato come `.exe` standalone, non
solo lanciato con `:tool:run` da dentro il repository.

Quando (non se) si deciderà il passaggio del CLIENT a un registro
risorse dinamico invece dei cataloghi Kotlin compilati — l'idea più
grande discussa lo stesso giorno, che tocca `:app` per davvero — serve
una specifica scritta dedicata a parte, non un paragrafo qui: comporta
spostare le immagini da `res/drawable-nodpi/` (risorse Android
compilate, non leggibili per nome a runtime) ad `assets/` (già la sede
degli mp3, §15.6), e cambiare come `SceneImages.kt`/`NpcImages.kt`/
`EnemyImages.kt` risolvono un ID — decisione rimandata di proposito,
non ancora presa.

### 15.2 Vocabolario chiuso anche per i toni

`Manifest.toneHints`/campo tono nel form di creazione libro (§9.1):
oggi testo libero separato da virgole. Va vincolato allo stesso
principio già in vigore per le immagini (menu a tendina, non testo
libero) — vocabolario preso da `NarrativeTone` (`:app`,
`util/NarrativeTonePreferences.kt`): Cupo, Avventuroso, Misterioso,
Eroico, Leggero, Duro e crudo, Erotico, Brutale, ciascuno con i propri
`hints` (es. Cupo -> dark, grim). L'elenco va aggiunto al registro di
§15.1 (stessa istantanea, stesso principio: il client non cambia,
l'editor legge una copia) invece di duplicarlo a mano una seconda
volta.

### 15.3 Immagini: anteprima nella scheda, sfondo nel grafo

Due usi distinti, non lo stesso:

- **Nella scheda di editing di una scena (§7.1)**: ogni immagine
  presente (`backgroundImage`/`npcImage`/`combat.enemyImage`) va
  **caricata e mostrata graficamente**, non solo come testo dell'ID —
  un'anteprima per ciascuna, **ognuna nel proprio posto** (tre slot
  distinti: location, NPC, nemico), nessuna priorità qui perché
  possono coesistere tutte e tre nella stessa scena.
- **Sul nodo della mappa (§6.2)**: un nodo mostra **una sola**
  immagine come proprio sfondo (al posto del verde/rosso pieno di
  oggi, o sovrapposta ad esso — da decidere in implementazione se la
  salute resta leggibile come bordo colorato invece che come
  riempimento). Quando una scena ne ha più di una, ordine di priorità
  deciso: **NPC** (`npcImage`) prima, poi **nemico** (`combat.
  enemyImage` — copre sia "nemico" che "bestia", stesso campo unico
  nello schema, vedi `NpcImageCatalog.kt`/`EnemyImageCatalog.kt`: le
  `beast_*` sono le stesse in entrambi i cataloghi, è l'autore a
  scegliere il campo), infine **location** (`backgroundImage`) come
  ultima scelta.

### 15.4 Mouse: rotella e tasto centrale

- **Rotella del mouse** sopra la mappa: zoom (stesso effetto dei
  pulsanti `−`/`+` di §6.1, non li sostituisce).
- **Pressione del tasto centrale**: stesso effetto del pulsante
  "⟳ Riordina" già esistente (§6.1) — una scorciatoia, non un
  comportamento nuovo.

### 15.5 Aggiungere ed eliminare scene dalla mappa

Due azioni nuove in barra strumenti, che usano la selezione con click
singolo già costruita (contorno blu):
- **Nuova scena**: crea una scena vuota (nessun collegamento ancora) e
  apre subito il suo pannello di editing.
- **Elimina scena selezionata**: rimuove la scena marcata dal contorno
  blu. I collegamenti di altre scene che puntavano ad essa restano
  come riferimenti a una scena non più esistente — la validazione li
  segnala (rosso, §6.2), non blocca né corregge da sola, stessa
  filosofia di "mai bloccare" già in vigore.

**Rete di sicurezza per le scene nuove**: se una scena appena creata
non ha ANCORA nessun collegamento in uscita nel momento in cui la si
salva/chiude per la prima volta, l'editor aggiunge da solo una scelta
di default verso `manifest.deathSceneId` (la scena di sconfitta già
garantita da ogni libro, §9.3) — evita il caso, già capitato, di una
scena senza uscita che fa fallire la validazione per una semplice
dimenticanza. Vale **solo alla creazione**, non è un correttore
retroattivo su scene già esistenti senza uscita (quelle restano un
avviso di validazione da guardare, non un'azione automatica silente
su dati già scritti).

### 15.6 Audio delle risorse

`app/src/main/assets/sfx/images/loc_*.mp3` (23 file, un suono
ambientale per location, stessa convenzione di nome degli ID —
`loc_alley.mp3` per `loc_alley`): stesso trattamento delle immagini di
§15.1, copiati nelle risorse di `:tool` e **ascoltabili dall'editor**
(pulsante di riproduzione accanto alla scelta della location). Restano
FUORI da questo giro (categoria concettualmente diversa, non "una
risorsa per ID"): `assets/music/` (tracce per umore, non per scena) e
`assets/sfx/`/`assets/sfx/endings/` (effetti generici — dado, passi,
finali). Riproduzione via **JLayer** (libreria Java pura, gratuita,
LGPL — `javax.sound` di base non decodifica MP3).

### 15.7 Risorse `url:` fornite da chi scrive il libro

Nuovo campo a livello di **Manifest** (non nel registro condiviso di
§15.1, che descrive cosa porta con sé l'APP — questo è dati DI QUEL
libro, stesso posto di `toneHints`/`disciplineChoices`):

```json
"customResources": {
  "images": [ { "id": "mio_villain", "url": "https://..." } ],
  "sounds": [ { "id": "mio_tema", "url": "https://..." } ]
}
```

Serve **solo come comodità per l'autore nell'editor** (una lista da
cui scegliere invece di riscrivere lo stesso URL più volte) — la scena
continua a salvare direttamente `"url:https://..."` come già fa oggi,
il client non deve sapere che `customResources` esiste: zero impatto
sul motore di gioco, solo un nuovo pannello nell'editor per
aggiungere/gestire queste voci. Richiede un aggiornamento di
`doc/SCHEMA-JSON.md` (nuovo campo opzionale di Manifest) e del
validatore (un ID duplicato tra voci di `customResources` è un
avviso, non un errore bloccante).

### 15.8 Salvataggio di una scena con esito a colori

Al salvataggio di una scena (§7.3), tre esiti invece del semplice
blocco/non blocco di oggi:
- **Valida, nessun avviso**: lo sfondo del pannello lampeggia di un
  **verde chiarissimo**, poi il pannello si chiude (torna alla mappa)
  — comportamento di oggi, solo con il colore in più.
- **Valida con avvisi** (es. un riferimento "in avanti" non ancora
  creato, §7.3): lampeggio **giallo chiarissimo**, poi si chiude
  comunque — gli avvisi non bloccano, come oggi.
- **Non valida** (errore vero, es. campo obbligatorio mancante):
  lampeggio **rosso molto chiaro**, il pannello **non si chiude**;
  compare invece un popup che spiega cosa manca. Chiudendo il popup
  (OK) il pannello **resta aperto** per permettere la correzione — il
  popup blocca solo se stesso, non "sblocca" la chiusura del pannello.

## 16. Impostazioni dell'editor (30/07/2026)

Michele: "importi dal client le font disponibili, il tema selezionato
che deve essere persistente e si deve poter aumentare e diminuire la
grandezza dei caratteri — un pulsante sulla prima maschera". Una
schermata `Impostazioni` raggiungibile solo dall'Avvio (§5), con
"Ritorna" per tornare indietro.

### 16.1 Tema, font e grandezza del testo

- **Tema**: chiaro/scuro, scelta esclusiva (radio button). Prima
  d'ora si perdeva a ogni riavvio (semplice `remember`); ora è
  persistente (vedi §16.2) — il pulsante rapido 🌙/☀ in basso a destra
  su ogni schermata resta, ma ora scrive nella stessa preferenza.
- **Font**: le stesse 4 famiglie già bundlate nel client
  (`FontPreferences.kt`, `:app` — Almendra, Cinzel Decorative,
  MedievalSharp, Uncial Antiqua; Google Fonts, licenza OFL), copiate
  in `tool/src/main/resources/fonts/` così l'editor non dipende dai
  font installati sul PC. Si applicano a *tutta* l'interfaccia
  costruendo una `Typography` con ogni stile sovrascritto (un
  `MaterialTheme(typography = ...)` da solo non basta: ogni
  `Text(style = MaterialTheme.typography.X)` porta già un
  `fontFamily` non-null nel proprio `TextStyle`, che vince
  sull'ambiente).
- **Grandezza testo**: tre passi fissi (A-/A/A+, moltiplicatori
  1.0/1.15/1.35), non uno slider continuo — bastano e restano
  leggibili nell'etichetta. Si applicano scalando `LocalDensity.
  fontScale` con un `CompositionLocalProvider` attorno a tutta la
  finestra: stessa tecnica dell'impostazione di accessibilità di
  Android, scala ogni `Text()` esistente senza doverli toccare uno a
  uno.

### 16.2 Persistenza

`java.util.prefs.Preferences` (`EditorPreferences.kt`) — già nella
JDK, nessuna dipendenza in più (su Windows usa il registro utente
corrente). Stesso principio delle `SharedPreferences` che il client
usa per le proprie impostazioni equivalenti
(`NarrativeTonePreferences`/`FontPreferences`), senza però un
`Context` Android a disposizione.

## 17. Riferimenti

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
- `content/static-resources.json` — registro delle risorse statiche
  letto dall'editor (§15.1/§15.2), copia dei cataloghi Kotlin di `:app`.
- `doc/DIARIO.md` — cronologia dei giri di correzione della v1
  (30/07/2026) da cui è nata questa seconda fase.
