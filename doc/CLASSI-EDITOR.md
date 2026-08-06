# Le classi dell'editor e della CLI — guida per chi arriva

Mappa del codice del modulo **`:tool`**: l'editor grafico da scrivania
e i comandi a riga di comando. Per il gioco c'è il documento gemello,
[`CLASSI-CLIENT.md`](CLASSI-CLIENT.md).

Le decisioni di design stanno in [`EDITOR.md`](EDITOR.md) e
[`ETL.md`](ETL.md); qui si racconta *dove sta cosa* nel codice.

## Che cos'è `:tool`

Un solo modulo per due programmi, per scelta esplicita (*"vorrei
lasciare il tool proprio unico"*):

- **l'editor**, finestra Compose Desktop — `EditorMain.kt`;
- **la CLI**, senza interfaccia — `Main.kt`, avviata da
  `./gradlew :tool:cli --args="…"`.

Dipende da `:core:data` e `:core:engine`, mai da `:app`: apre gli
stessi libri del gioco e li valida con lo stesso validatore, ma non sa
nulla di Android.

> **Java 21**, unico modulo del progetto (gli altri sono su 17):
> `litertlm-jvm` è compilata per la 21 e su una JVM 17 non si carica
> nemmeno. Per generare `.msi`/`.exe` serve un JDK 21 **con jpackage**
> indicato in `local.properties` (`packagingJdk`); una guardia nel
> `build.gradle.kts` blocca il packaging se ne trova uno più vecchio,
> perché un pacchetto costruito con la 17 si installa, si avvia e muore
> alla prima generazione.

---

## Il giro dell'editor

```
EditorMain          la finestra, la navigazione, le preferenze
   ├── MapScreen         la mappa del libro (nodi, collegamenti, menu)
   ├── SceneEditorScreen la scheda di UNA scena
   └── ModelloScreen     scelta, scaricamento e prova del modello
```

### `EditorMain.kt`
Punto d'ingresso e navigazione (`Schermata`: Avvio, Mappa, EditorScena,
Impostazioni, Modello). Qui vivono le cose che devono **sopravvivere
al cambio di schermata**: le preferenze, il motore del modello, lo
stato della mappa (zoom, pan, posizioni trascinate).

Contiene `EDITOR_BUILD_MARKER`: stampato all'avvio e scritto nel titolo
della finestra. **Va aggiornato a ogni modifica del modulo**, prima di
ricompilare — serve a sapere a colpo d'occhio quale versione si sta
guardando, e più di una volta ha smascherato una prova fatta su una
build vecchia.

### `SceneGraph.kt`
Il grafo del libro. `buildSceneGraph(manifest)` calcola nodi, archi e
livelli; `percorsoDaStart(graph, sceneId)` ricostruisce un cammino
dalla scena iniziale a una qualunque — è quello che colora la mappa in
viola **e** quello che alimenta il riassunto del percorso.

### `MapScreen.kt`
La mappa: disegno, selezione (anche a rettangolo), trascinamento di
gruppo, allineamenti, menu col tasto destro. È il file più grande del
modulo perché l'interazione con la mappa è tutta lì.

### `SceneEditorScreen.kt`
La scheda di una scena: testo narrato, immagini, suono, combattimento,
scelte e scelte-disciplina, più la vista JSON grezza. Valida in locale
prima di salvare e colora l'esito (verde / giallo / rosso).

### `BookStorage.kt` e `CronologiaDocumento.kt`
Salvataggio con **backup a livelli** (da 3 a 9, configurabili) e
annulla/ripeti in sessione con Ctrl+Z / Ctrl+Y.

### `EditorPreferences.kt`
Tutto ciò che resta fra un avvio e l'altro, in `java.util.prefs` (su
Windows il registro utente): tema, font, scala del testo, libri
recenti, percorso e cartella dei modelli, lingua, modalità di
generazione, solo-CPU.

### `EditorLog.kt`
Il logcat che l'editor non ha. Scrive **sempre** su console *e* su
`%LOCALAPPDATA%\ImmundaNoctisEx\editor.log` (con rotazione a 5 MB),
perché avviando l'`.exe` col doppio click una console non esiste — ed è
esattamente il momento in cui serve capire cosa è andato storto.

### `StaticResourceCatalog.kt`
Copia delle risorse statiche del client (toni, sfondi, ritratti),
impacchettata dentro il programma insieme alle immagini stesse: l'editor
funziona anche su una macchina che non ha il repository.

---

## Il modello dentro l'editor

Stesso motore del telefono, sul PC: stessa libreria alla stessa
versione, stesso file `.litertlm`, stesso prompt.

### `EditorInferenceEngine.kt`
Il gemello desktop di `LiteRtLmEngine`. `load()`, `newSession()`,
`generate()`, `unload()`, più due cose che sul telefono non servono:

- **`soloCpu`** — di default acceso qui, al contrario del client: su GPU
  integrata Windows può resettare il driver a metà di un testo lungo, e
  per un editor vale più la certezza di arrivare in fondo;
- **`ricarica()`** — dopo un reset del driver l'`Engine` esiste ancora
  ma è morto, e senza ricordare file e configurazione l'unico rimedio
  sarebbe chiudere il programma.

`eDispositivoPerso(errore)` riconosce quel guasto (anche dentro le
cause annidate) ed è ciò che decide se **ritentare ha senso**.

La cartella di cache è **una per backend** e **riservata a un processo**
con un lock: mescolare cache CPU e GPU, o farci scrivere due processi
insieme, faceva morire la JVM intera in codice nativo.

### `SupportoGpuWindows.kt`
Su Windows il backend GPU è WebGPU su Direct3D 12 e pretende
`dxil.dll` e `dxcompiler.dll` del DirectX Shader Compiler, che non
fanno parte del sistema. Qui si **precaricano per percorso assoluto**
prima di costruire l'`Engine`, così la `LoadLibrary` interna trova il
modulo già in memoria. Il commento in testa al file dice anche *quale
versione* funziona: non è l'ultima.

### `ModelDownloader.kt`
Scarica i modelli (gli stessi link del client, da `ModelCatalog`). Tre
attenzioni pagate in anticipo: segue i redirect della CDN, scrive su
`.parziale` e **riprende** con una richiesta `Range` se cade, rifiuta
qualunque cosa pesi meno di 1 MB.

### `TraduttoreScene.kt`
L'anteprima di come il modello renderà una scena. **Non traduce stringa
per stringa**: manda la scena intera con `PromptBuilder` e ne scompone
la risposta con `ResponseParser`, esattamente come fa il gioco — le
scelte tradotte separatamente darebbero un risultato che il giocatore
non vedrà mai. Un `Mutex` serializza le richieste; se la GPU muore,
ricarica e ritenta una volta.

### `RiassuntorePercorso.kt`
Il riassunto del cammino da START a una scena. Lavora **a blocchi**: il
percorso peggiore misurato sui libri veri è di 73 scene per 53.000
caratteri, oltre il contesto del modello. Le scene si raggruppano
finché stanno in un blocco, ogni blocco diventa un riassunto parziale e
i parziali si ricuciono. `Avanzamento` è ciò che la finestra mostra —
un'attesa muta di minuti è indistinguibile da un programma piantato.

### `EsportaRiassunto.kt`
Scrive il riassunto in Markdown: titolo, testo, immagini e il percorso
in chiaro (`1 → 85 → 141`). Le immagini `static:` **non hanno un
percorso su disco** — vivono dentro il programma — quindi si copiano in
una cartella accanto al documento e si linkano in relativo; gli `url:`
restano URL.

### `AnteprimaModello.kt`
I pezzi di interfaccia condivisi: `RigaTradotta` (il riquadro sotto
ogni campo), `BarraAnteprima` (il pulsante e lo stato), `SezioneScheda`
(il box con titolo attorno a un gruppo di campi).

---

## La CLI

`Main.kt` smista verso i sottocomandi:

| Comando | File | Cosa fa |
|---|---|---|
| `validate` | `ValidateMain.kt` | Valida un libro con lo stesso `PackageValidator` del gioco |
| `forma` | `FormaMain.kt` | Misura la **forma** del grafo e la confronta con quella dei librogame pubblicati |
| `convert` | `ConvertMain.kt` | Converte un libro Project Aon da HTML a JSON |
| `illustrazioni` | `IllustrazioniMain.kt` | Elenca le illustrazioni originali di un libro |
| `bonifica` | `BonificaMain.kt` | Ripulisce i riferimenti alle risorse |
| `svuotaTesti` / `riempiTesti` | `TestiMain.kt` | Separano le meccaniche dalla prosa Project Aon (README §15) |
| `verificaGrafo` | `VerificaGrafoMain.kt` | Confronta la conversione col grafo ufficiale dei percorsi |

`validate` e `forma` rispondono a due domande diverse, ed è il motivo
per cui sono comandi separati: il primo dice se il grafo **regge**
(destinazioni esistenti, niente scene duplicate), il secondo se ha la
**forma** di un librogame. Un racconto lineare con un bivio ogni tanto
passa `validate` senza un rilievo. Le soglie di `FormaDelGrafo.kt`
vengono da `doc/FORMA-DEI-GRAFI.md`, la misura di 37 opere pubblicate;
sotto le 20 scene i controlli statistici si spengono, perché una
percentuale su sei elementi non è una misura.

### `ProjectAonHtmlParser.kt`
Il pezzo più delicato della CLI: riconosce nel testo inglese scelte,
combattimenti, tiri della Tabella dei Numeri Casuali e i
`rollModifiers` condizionali. Usa **jsoup**, non espressioni regolari
sull'HTML grezzo.

Due principi guadagnati sul campo:

- ciò che non si riconosce **con certezza** finisce nel report dei casi
  da rivedere a mano, non viene indovinato in silenzio;
- ogni correzione alle espressioni regolari va verificata **scena per
  scena** su tutti i libri: una modifica che alza il conteggio totale
  può contemporaneamente perdere casi che prima funzionavano. È già
  successo.

---

## Convenzioni del modulo

- **Nomi in italiano** per il codice nuovo di `:tool`
  (`RiassuntorePercorso`, `avviaRiassunto`, `percorsoDaStart`), come i
  commenti. I nomi che vengono da `:core` restano quelli.
- **Ordinamento deterministico per ID**: scene e nodi si ordinano
  sempre allo stesso modo, così le viste e le esportazioni non
  "ballano" fra un'apertura e l'altra.
- **Degrado silenzioso**: un'immagine mancante, un suono assente, una
  risorsa sconosciuta non fermano mai l'editor — al massimo non si
  vedono.
- **Niente crash a carico dell'autore**: qualunque errore del modello o
  del disco diventa un messaggio leggibile, e il lavoro in corso non si
  perde.

## Da dove cominciare a leggere

1. `EditorMain.kt` — come si naviga e cosa sopravvive al cambio schermata;
2. `SceneGraph.kt` — come il libro diventa un grafo;
3. `EditorInferenceEngine.kt` — come il modello del telefono gira sul PC;
4. `ProjectAonHtmlParser.kt` — solo se si tocca la conversione, ed
   entrando in punta di piedi.
