# Diario di progetto

> **SE STAI APRENDO UNA SESSIONE NUOVA, LEGGI SOLO IL BLOCCO QUI SOTTO.**
> Il resto del file è la storia cronologica (dal più recente al più
> vecchio): serve per capire *perché* una decisione è stata presa, non
> per sapere cosa fare adesso.

---

## STATO CORRENTE — aggiornato 19/07/2026

**Fase**: 4 (`inference`). Fase 3 chiusa: il libro gira per intero sul
Razr senza IA (Home, creazione, scena, combat a due modalità, scheda,
diario, checkpoint, auto-save atomico).

**Cosa gira già in Fase 4** (tutto committato, suite verde):
- `ResponseParser` + `PromptBuilder` con 22 unit test JVM (girano da
  terminale, senza device né modello).
- Schermata **Modelli LLM**: catalogo, download riprendibile in
  background, token Hugging Face, impostazioni avanzate (maxTokens,
  temperatura, topK, topP). Provata sul device: funziona.
- **`LiteRtLmEngine`**: motore reale su `com.google.ai.edge.litertlm`
  0.14.0, backend GPU con ripiego su CPU. **Compila ma non è ancora
  stato eseguito**: non è mai stato caricato un modello vero.
- **`SceneNarrator`**: il giro completo di una scena (prompt -> sessione
  nuova -> streaming ripulito dei tag -> parsing), con 6 test su
  `FakeEngine`. La degradazione è garantita PER TEST: motore assente o
  caduto a metà -> testo originale del pacchetto, nessuna eccezione.

- **Narratore CABLATO nella scena**: la UI mostra il testo generato in
  streaming (buffer 90ms), le scelte tradotte, il nome del nemico
  tradotto e il semaforo nell'header. Nel diario-grafo finisce il testo
  che il giocatore HA LETTO. Il modello si carica alla prima scena
  (`AppContainer.ensureModelLoaded()`); se manca, il gioco prosegue col
  testo del pacchetto senza dire nulla.

- **GEMMA GENERA DAVVERO SUL RAZR** (19/07, provato da Michele): la
  scena arriva arricchita e tradotta in streaming, backend **GPU**,
  velocità giudicata "molto buona". È il cuore della milestone di
  Fase 4. Restano da raccogliere i NUMERI (sotto).

- **Con LLM attivo il testo originale NON si mostra più**: si vede "Il
  narratore scrive…" e poi solo lo streaming della traduzione. Anche
  scelte e nemico restano nascosti finché la generazione non finisce
  (UI.md: prima lo streaming, POI i pulsanti), altrimenti cambiavano
  sotto gli occhi. Senza motore tutto resta come in Fase 3.
  **Corretto al secondo tentativo**: il primo fix svuotava il testo solo
  quando *partiva la generazione*, ma il CARICAMENTO del modello dura
  secondi e in quel tempo l'inglese restava a schermo. Ora
  `AdventureState` sa fin dalla costruzione se il modello è sul telefono
  (`expectsNarration`: basta l'esistenza del file, non serve
  aspettare il load) e parte già in attesa. Con via d'uscita
  (`narrationUnavailable()`) se il motore non parte: si torna al testo
  del pacchetto invece di restare in attesa per sempre.

- **Banner della scena** (v1): sfondo mappa + ritratti circolari di
  narratore ed eroe (per genere), con **cerchio d'oro su chi parla** —
  narratore mentre scrive, eroe quando tocca a lui. Corretto anche un
  difetto della card di stato (nome e valori si attaccavano: "Lupo
  SolitarioCS 18RES 22/223 Corone").

**PROSSIMA SESSIONE — già deciso con Michele**:
1. **ANIMAZIONE del narratore che pensa** al posto della scritta "Il
   narratore scrive…" (idea di Michele, gli piace molto). Registrata in
   `UI.md §Flusso centrale`. Copre sia il caricamento del modello (la
   prima volta, più lungo) sia la generazione di ogni scena.
2. La **grafica** in generale, che Michele ha rinviato consapevolmente.
   Il banner c'è ma è v0.1: manca il `backgroundImage` PER SCENA (il
   sample dichiara inn/city/alley/battle/warehouse, gli asset non
   esistono — vanno prodotti, Fase 7) e manca il compagno di viaggio.
3. **Raccogliere le misure** — il motore
ora le logga da solo a ogni scena giocata: `adb logcat -s
LiteRtLmEngine` stampa una riga `MISURA backend=… primoToken=… 
tokenPrompt~… tokenGenerati~… velocita~… token/s`. Da giocare qualche
scena e riportare i valori qui, più il comportamento termico su 30-45'.
Poi salvare qualche output reale di Gemma come fixture di test.

**Fatti tecnici da non riscoprire** (verificati, non ipotizzati):
- Il motore è **LiteRT-LM**, non MediaPipe di v1: i Gemma 4 escono solo
  in `.litertlm` e MediaPipe legge `.task`. Formati non intercambiabili.
- **Il backend GPU è un requisito, non un'ottimizzazione**: benchmark
  ufficiali danno primo token 0,8 s su GPU contro 5,3 s su CPU, e
  CRITICITA.md fissa la soglia a 3 s.
- Il progetto è su **Kotlin 2.3.21** perché l'AAR di LiteRT-LM lo
  impone (metadata 2.3). Usa il DSL `compilerOptions`, non
  `kotlinOptions`.
- Device di riferimento: Razr 60 Ultra, **SM8750** (Snapdragon 8
  Elite), 15,5 GB RAM. Esiste una build NPU per questo chip esatto.
- Il modello di v1 (`google/gemma-3n-E4B`) è su **repo gated**: senza
  token restituisce 401 e si salverebbe una pagina d'errore al posto
  del modello. I `litert-community/gemma-4-*` sono aperti.

**Debiti dichiarati** (non sorprese, scelte consapevoli):
- Il conteggio token del motore è una **STIMA** (~4 caratteri/token):
  la libreria non espone un tokenizer pubblico. Serve solo al semaforo.
- `strings.xml` è **impalcato** da Claude: la rifinitura dei testi è di
  Michele.
- Le 3 icone armi mancanti (dagger/short_sword/warhammer) usano un
  segnaposto; `ic_axe` e `ic_map_icon` pesano 3-4 MB (WebP in Fase 7).

**Decisioni in attesa di Michele**:
1. Il `TagParser` previsto in Fase 4 si salta? (in Ex non avrebbe nulla
   da parsare: le meccaniche arrivano già strutturate dal JSON)
2. Le rifiniture UI di Fase 3 che voleva elencare
3. Bonus dello scudo, se lo si vuole come oggetto iniziale
4. Download del modello: ora vincolato al solo Wi-Fi

**Idee rinviate**: stanno in `doc/UPGRADE.md` (audio narrativo, ecc.).
NON sono schedulate: non implementarle senza una decisione esplicita.

---

## 19/07/2026

### Sessione — download del modello e catalogo Hugging Face

**Scoperta che ha cambiato il default** (verificata con richieste HEAD,
non ipotizzata): l'URL di v1
(`google/gemma-3n-E4B-it-litert-preview`) risponde **401 GatedRepo** —
senza token si scaricherebbero 145 byte di pagina d'errore *salvati come
se fossero il modello*. Michele ha giustamente ricordato che v1 IL TOKEN
LO GESTIVA (mia ricerca del giorno prima troncata da `head`, non aveva
visto `ThemePreferences.getToken` + `Authorization: Bearer` nel worker).

Catalogo scelto, con dimensioni e gating VERIFICATI il 19/07:
- `litert-community/gemma-4-E4B-it.litertlm` — 3,66 GB, aperto → **default**
- `litert-community/gemma-4-E2B-it.litertlm` — 2,59 GB, aperto (ripiego
  se le misure diranno che il Razr scotta)
- `google/gemma-3n-E4B` di v1 — gated, resta raggiungibile col token
Così l'app funziona anche a chi non ha un account Hugging Face.

**`ModelDownloadWorker`**: pattern di v1 con i suoi difetti corretti —
(a) il token era OBBLIGATORIO (`?: return failure()`), quindi i modelli
aperti non si sarebbero scaricati; ora è opzionale; (b) v1 cancellava il
parziale a ogni errore: perdere 3,6 GB per una connessione caduta al 90%
è inaccettabile, ora il `.part` sopravvive e si RIPRENDE con richiesta
Range; (c) un solo flusso invece di 8 connessioni parallele (su mobile
la ripresa vale più della velocità di picco); (d) controllo dello spazio
disco prima di iniziare; (e) **verifica della dimensione finale prima di
promuovere il file** — un 401 o un troncamento non devono mai passare per
un modello valido; (f) stream chiusi davvero (v1 lasciava aperto un
`RandomAccessFile`). Stessa disciplina dei salvataggi: si scrive su
`.part` e si rinomina solo a verifica passata.

**`ModelPreferences`**: il token esce da `ThemePreferences` (in v1 un
segreto viveva in casa delle preferenze del tema) e ha il suo posto; mai
nei log; campo mascherato nella UI. `isDownloaded` non si fida
dell'esistenza del file: controlla anche la dimensione attesa.

**Schermata Modelli LLM** (era un segnaposto): catalogo con selezione,
progresso, annulla-che-riprende, elimina, campo token. Download solo su
rete non a consumo (`NetworkType.UNMETERED`) e `ExistingWorkPolicy.
REPLACE` per non accodare due download sullo stesso file. Manifest:
dichiarato il `SystemForegroundService` con `dataSync`.

**Provato sul device da Michele (debug wifi): download OK.**

### Sessione — opzioni importate da ModelActivity di v1

Analisi sezione per sezione di `ModelActivity` (824 righe), tabella dei
verdetti in `doc/ANALISI-UI-V1.md`. Importato:

- **Impostazioni avanzate Gemma**: `maxTokens`, `temperature` (slider),
  `topP` (slider), `topK`. I valori di v1 sono già tarati (0.7 / 40 /
  0.9); `maxTokens` parte da **10240** come da CRITICITA.md, non dai
  4096 di v1. Conservate tali e quali le **descrizioni oneste con
  l'impatto dichiarato su CPU/memoria** — il pezzo migliore di quella
  schermata. Migliorìa: in v1 c'era scritto "richiede il riavvio della
  partita", in Ex no (sessione nuova a ogni scena, vale dalla prossima).
  Aggiunto "Ripristina i valori consigliati" per tornare ai default dopo
  una sessione di misure andata storta.
- **Spazio occupato dai modelli**: assente in v1, ma con file da 3,66 GB
  è informazione dovuta.
- **`InferenceEngine`** (una delle quattro interfacce motivate), erede
  di quella di v1 con due differenze volute: niente
  `chatbotPersonality` nel load (era-chatbot) e `newSession()` invece di
  `resetSession(systemPrompt)` — in Ex la sessione nuova per scena è la
  NORMA, non il rimedio a un contesto pieno. Con `TokenInfo`/
  `TokenStatus` (soglie di v1) per il semaforo dell'header, e
  `InferenceConfig` che le impostazioni avanzate riempiono davvero: le
  manopole sono cablate, non finte.

NON importato, con motivo: reset sessione chatbot (in Ex l'inferenza è
senza memoria per design), modalità dual-engine (solo Gemma),
impostazioni GGUF (motore morto), doppio slot modello.
`SceneJsonPicker` resta in Fase 5 come da piano.

### Sessione — LiteRT-LM: indagine, scelta e motore

**Problema intercettato prima di scrivere codice**: il catalogo che avevo
messo usa file `.litertlm`, ma v1 usa **MediaPipe**, che legge `.task`.
Runtime diversi, formati NON intercambiabili: senza accorgersene si
scaricavano 3,66 GB inutilizzabili.

Indagine (tutto verificato con richieste reali, niente memoria):
- `com.google.ai.edge.litertlm:litertlm-android` **esiste** su Google
  Maven, stabile **0.14.0**, API Kotlin `Engine`/`EngineConfig`.
  (`litert-lm` sotto `com.google.ai.edge.litert` NON esiste: 404.)
- **L'ecosistema si è spostato su LiteRT-LM**: i Gemma 4 escono solo in
  `.litertlm`; i `.task` rimasti per i modelli grandi sono varianti
  `-web`. MediaPipe `tasks-genai` esiste ancora (0.10.35, v1 usava
  0.10.24) ma è la strada che si chiude.
- Device confermato via adb: **SM8750** (Snapdragon 8 Elite), 15,5 GB
  RAM. Esiste `gemma-4-E2B-it_qualcomm_sm8750.litertlm`: build NPU per
  esattamente questo chip.
- **Benchmark ufficiali del model card (Gemma 4 E4B, Android)**:
  GPU primo token **0,8 s**, decode 22,1 tok/s, memoria 710 MB —
  CPU primo token **5,3 s**, decode 17,7 tok/s, memoria 3283 MB.
  CRITICITA.md fissa la soglia a **3 s**: su CPU l'obiettivo NON si
  raggiunge, su GPU si passa largamente. **Il backend GPU non è
  un'ottimizzazione, è un requisito.** Il decode (17-22 tok/s) è
  comunque più veloce della lettura umana: l'altra criticità regge.

**Decisione di Michele: LiteRT-LM** (`com.google.ai.edge.litertlm`).
Conseguenza accettata: l'esperienza di v1 sul motore non si riusa, si
riusa il *pattern* (Flow di token, token tracking, semaforo).

**Aggiornamento imposto**: l'AAR è compilato con metadata Kotlin **2.3**,
il progetto era su 2.0.21 — incompatibilità dura, non aggirabile.
Portato tutto il progetto a **Kotlin 2.3.21** (un solo numero: tutti i
plugin puntano a `version.ref = kotlin`) e migrato `kotlinOptions` ->
DSL `compilerOptions`, che in 2.3 è un errore. **Suite verde su tutti i
moduli dopo l'aggiornamento**: nessuna regressione.

**`LiteRtLmEngine`**: prova la **GPU** e ripiega su **CPU** se OpenCL non
è utilizzabile (degrada invece di lasciare il gioco senza narratore);
espone `activeBackend` perché *un numero di misura senza sapere su quale
backend girava non dice nulla*; `newSession()` crea una conversazione
nuova per scena (inferenza senza memoria: è la norma, non un rimedio);
`SamplerConfig` riceve davvero temperatura/topK/topP dalle impostazioni
avanzate. Manifest: dichiarate `libOpenCL.so` e `libvndksupport.so` come
librerie native opzionali.

**Debito dichiarato**: il conteggio token è una **STIMA** (~4 caratteri
per token) perché la libreria non espone un tokenizer pubblico. Serve
solo al semaforo, che è un'indicazione di massima. Da sostituire se
l'API esporrà il conteggio vero.

### Sessione — SceneNarrator e diario preparato per la ripartenza

**Diario riorganizzato** su richiesta di Michele (la sessione potrebbe
saturarsi): cronologia rimessa dal più recente (era 17->19->16) e
soprattutto aggiunto in testa il blocco **STATO CORRENTE**, ~65 righe
che bastano a una sessione nuova per sapere dove siamo, cosa fare e
quali fatti NON riscoprire (LiteRT-LM vs MediaPipe, GPU come requisito,
Kotlin 2.3, repo gated). Il resto del file resta come storia del
*perché*, non del *cosa fare*.

**`SceneNarrator`**: orchestra il giro di una scena — compone il prompt
(con le continuazioni prese dalle scene raggiungibili, mai rivelate al
giocatore), apre una sessione nuova, streamma, e consegna il parsato.
È il punto in cui "il resto dell'app non sa che esiste Gemma".
6 test su un `FakeEngine` che verificano quello che conta:
- motore non caricato -> testo originale del pacchetto;
- motore che cade a metà generazione -> degradazione, nessuna eccezione
  propagata;
- **le righe dei tag non compaiono MAI nello streaming** (si mostra solo
  ciò che precede `--- TAGS ---`);
- una sessione nuova per ogni scena;
- il prompt porta continuazioni e coda precedente, ma non il diario.

Il `FakeEngine` non è solo un attrezzo di test: permette di sviluppare
la UI senza caricare 3,66 GB a ogni avvio.

### Sessione — il narratore entra nella scena

Cablaggio completo di `SceneNarrator` in `AdventureState`/
`AdventureScreen`:
- **Il testo parte dall'originale del pacchetto** e viene sostituito da
  quello arricchito man mano che arriva: la scena è leggibile fin dal
  primo istante, anche mentre il modello pensa. Nessuna schermata vuota
  in attesa.
- **Streaming bufferizzato a 90ms** (CRITICITA.md chiede ~80-100): senza
  buffer ogni token farebbe ricomporre l'intera schermata.
- Scelte e nome nemico mostrati tradotti quando arrivano, originali
  altrimenti (`choiceText()`/`disciplineChoiceText()`/`enemyName`).
- **Nel diario-grafo finisce il testo che il giocatore HA LETTO**, non
  quello del pacchetto: `moveTo` salva `narrative` (STATO.md Blocco 3 —
  si salva e non si rigenera mai). La coda di quel testo diventa il
  `previous_scene_text` della scena dopo: contesto senza memoria.
- **Semaforo nell'header** (UI.md): tertiary mentre genera, rosso oltre
  il 60% di contesto, primary a riposo.
- `AppContainer.ensureModelLoaded()` carica il modello alla prima scena
  e lo tiene (istanza unica: costa GB e secondi). **Se il modello non
  c'è il gioco prosegue col testo del pacchetto**, senza errori a
  schermo — la Fase 3 continua a funzionare esattamente com'era.
- I frammenti del prompt si leggono da `config.json` negli asset, con
  fallback ai default hardcoded.

Build e suite verdi, APK installato sul Razr. **Ma la generazione vera
non è ancora stata osservata**: finché non si carica un modello sul
device, tutto questo è codice che compila e passa i test col motore
finto.

### Sessione — PRIMA GENERAZIONE VERA: crash risolto, Gemma narra sul Razr

Prima prova sul device: **crash**. Log analizzato (85.437 righe, formato
JSON di Android Studio) — la causa NON era il nostro codice né il
modello:

```
java.lang.NoSuchMethodError: No static method close$default(SendChannel;…)
  at com.google.ai.edge.litertlm.Conversation.onDone(Conversation.kt:264)
```

**Diagnosi**: LiteRT-LM 0.14.0 *dichiara* `kotlinx-coroutines 1.9.0` nel
suo POM — ed era esattamente quella risolta — ma la libreria è
**compilata con Kotlin 2.3**, che genera i metodi con argomenti di
default come statici nell'interfaccia; coroutines 1.9.0, costruito con
un Kotlin più vecchio, li espone in un'altra forma. Il metodo esiste in
compilazione e non a runtime. È un difetto di packaging della libreria.
**Fix**: forzato `kotlinx-coroutines-android 1.11.0` (costruito con
Kotlin recente).

Dentro quel log c'erano già le notizie buone: `Modello caricato su GPU`,
`backend: GPU`, `max_tokens: 10240`, `Creating Gemma4DataProcessor` — e
il crash arrivava in `onDone`, cioè a generazione GIÀ FATTA. Mancava
solo la consegna.

**Dopo il fix, provato da Michele: FUNZIONA.** Gemma arricchisce e
traduce la scena in streaming sul Razr, con velocità giudicata "molto
buona". Il pilastro della Fase 4 regge.

**Osservazione dal log da non perdere**: durante il caricamento il
sistema era sotto forte pressione di memoria (`kswapd is busy`, `PSI
critical`, Android che uccideva altre app). Con 15,5 GB regge, ma se
emergessero lentezze o chiusure improvvise si sa dove guardare — e il
modello E2B da 2,59 GB è già in catalogo come alternativa.

**Strumentazione delle misure**: il motore ora logga da sé, a ogni scena
generata, una riga `MISURA` con backend, tempo al primo token, tempo
totale, token di prompt/generati e token/s. Così i numeri della
milestone vengono dall'uso reale invece che da una prova artificiale.
Nota: i conteggi di token sono STIME (la libreria non espone un
tokenizer), i TEMPI sono reali.

### Chiusura sessione — stato e prossimi passi

Stato: Fase 3 CHIUSA (provata sul Razr), Fase 4 aperta con le sue
fondamenta testabili già in piedi (ResponseParser e PromptBuilder, 22
test JVM verdi che girano da terminale senza device né modello). Suite
verde su tutti i moduli, APK che compila, working tree pulita.

**PROSSIMA SESSIONE — il motore vero**: LiteRT-LM dietro
`InferenceEngine` (load, generate come Flow, reset, token tracking),
sessione-per-scena (inferenza SENZA memoria) e streaming bufferizzato
~80-100ms troncato a `--- TAGS ---`. Poi la milestone di fase: **le
misure di CRITICITA.md sul Razr** (primo token, token/s, prompt token,
termico su 30-45') annotate qui, e ogni output reale di Gemma salvato
come fixture.

**Serve da Michele per quel passo**: il file del modello Gemma (quello
usato in v1 va bene) e sapere se è già sul Razr o va scaricato
dall'app (nel secondo caso il DownloadWorker di v1 è già censito come
riusabile, e i permessi sono già nel manifest).

**Decisioni in attesa**: (a) il TagParser si salta in Fase 4? (vedi
osservazione sopra); (b) rifiniture UI della Fase 3 che Michele deve
ancora elencare; (c) rifinitura `strings.xml` [MICHELE]; (d) bonus
dello scudo, se lo si vuole.

- **Manifest fuso con v1** (richiesta Michele): icona launcher
  ORIGINALE completa (mipmap tutte le densità + adaptive + playstore
  png spostato da Michele), tema XML `Theme.ImmundaNoctis` (solo per
  la finestra pre-Compose), permessi INTERNET/FOREGROUND_SERVICE/
  DATA_SYNC/POST_NOTIFICATIONS (pronti per il download modello di
  Fase 4), backup/data-extraction rules. NON portati: le Activity
  multiple (single-activity), i service stdf (morti),
  network_security_config (era il cleartext per il backend locale
  stdf); il service WorkManager foreground si aggiunge col
  DownloadWorker in Fase 4.

**Chiusura**: `effectiveEndurance` completata (clamp 0..maxEndurance,
test su base/sforo alto/sforo basso/modificatori misti), `./gradlew
test` verde su tutti i moduli. Le modifiche Gradle risultavano già
committate (commit `6c8bf64`, `4837dd6`, `ad87c0d` — nota: portano un
messaggio errato "Create doc/ETL.md...", copiato per sbaglio; già
pushati, si lasciano così). Per i build da terminale il daemon JVM
(JDK 21 JetBrains) è risolto via `org.gradle.java.installations.paths`
nel `gradle.properties` UTENTE (`~/.gradle/`), fuori dal repo.

## 17/07/2026

### Sessione — chiusura specifica 4 (UI)

**Specifica 4 CHIUSA** (`doc/UI.md`). Decisioni chiave:

- **Estetica di v1 CONSERVATA** (riferimento: screenshot "L'Ultimo dei
  Kai" — tema scuro, banner con ritratti sovrapposti, card personaggio
  con grado dorato e icone stats). Corretti solo i tre elementi
  dell'era-chatbot: via la barra di testo libero "Cosa fai?" (sostituita
  dalla zona scelte), via le bolle chat (il testo scorre come pagina di
  libro), il DM da personaggio salvato a **presenza visiva pura**
  (cerchio d'oro su chi parla, nessun `Character` nei dati).
- **7 schermate**: Home (continua/nuova/carica libro/modelli/opzioni),
  Setup avventura (scelta difficoltà), Creazione personaggio (lupo/lupa,
  tiro stat col Dado del Destino, 5 discipline, specializzazione
  WEAPONSKILL), Avventura (scena teatrale), Scheda personaggio a due tab
  (Stats e Discipline / Equipaggiamento e Zaino), Diario del viaggio
  (Racconto + Mappa logica, esportazione Markdown), Opzioni (TTS con
  voce per genere da `TtsPreferences` di v1, gestione modelli, lingua).
- **Scena teatrale**: header con titolo/scena/semaforo token sul
  pallino (verde/giallo/rosso, assorbe il contatore 0/10240 di v1);
  banner `backgroundImage` con ritratti circolari sovrapposti
  (narratore + eroe + compagno); flusso centrale come pagina di libro
  (serif, continuo) con fumetto narratore a tre icone (copia,
  originale/tradotto — toggle ex-icona "traduci", TTS) e le decisioni
  del giocatore incorporate nel flusso come vista live del diario-grafo;
  card di stato che apre la Scheda; zona scelte con pulsanti disciplina
  visivamente distinti.
- **Dado del Destino**: overlay modale animato, oggetto di scena
  rituale — appare SOLO per i tiri del giocatore (tiro stat, skillCheck,
  randomChoiceTable, ogni round del combattimento completo, il round di
  danno dell'evasione); i tiri del motore (randomQuantity,
  rollOnItemTable, combattimento rapido) restano in silenzio nel testo.
  **Combattimento dentro la scena**, nessuna schermata separata: la zona
  scelte si trasforma (scelta modalità rapido/completo, barre Resistenza
  e Rapporto di Forza, menu tattico continua/oggetto/disciplina/fuga).
- **Inventario OPERATIVO** nella Scheda: equipaggia/disequipaggia armi
  con effetto immediato sulle stat mostrate, consuma pasti/oggetti con
  effetto dichiarato (stesso gesto per EAT_MEAL, HUNTING auto-esente),
  zaino con gli 8 posti disegnati anche vuoti.
- **`gender`** con tre clienti: ritratto (lupo/lupa), voce TTS per
  genere, placeholder `{player_gender}` nel prompt (accordi grammaticali
  in italiano).

**Code generate**: elencate nella sezione finale di `doc/UI.md` (asset
mancanti — narratore, lupo/lupa, dado animato, icone discipline; nota:
molte icone vivono sul branch `develop` di v1, non su `master`@8b705b8
scansionato nell'inventario asset — da recuperare da lì; nuovo campo
opzionale `locationName` sulla scena, ereditato/appiccicoso, per la
Mappa logica del diario).

**Prossima specifica: 5 (ETL — conversione libri in pacchetti).**

### Aggiunta post-chiusura specifica 4

- Campo opzionale `locationName` sulla scena: **appiccicoso** (eredita
  dalla scena precedente se assente, l'autore lo scrive solo quando il
  luogo cambia).
- Diario del viaggio a due viste: **Racconto** (rilettura voce per
  voce) e **Mappa logica** dei luoghi visitati (nodi col nome,
  collegati nell'ordine del viaggio) — derivata dal diario-grafo
  raggruppando le voci per `locationName`. v0.1: solo il nome del
  luogo; predisposizione per annotare in futuro combattimenti (già
  derivabili dalle Transition WIN/LOSE), NPC importanti e oggetti
  trovati.
- `JourneyEntry` porta con sé il `locationName` già risolto (ereditato)
  al momento della visita: la Mappa logica non dipende dal ricalcolo
  dell'ereditarietà a lettura.

### Sessione — chiusura specifica 5 (ETL)

**Specifica 5 CHIUSA** (`doc/ETL.md`). Decisioni:

- **Scoperta chiave**: Kai Chronicles (GPL v3) ha le meccaniche dei
  libri 1-13 codificate a mano in `mechanics-X.xml` + `objects.xml`
  bilingue — il lavoro che v1 chiedeva all'LLM esiste già; la
  classificazione fuzzy diventa traduzione deterministica tra formati.
- **Tool desktop Kotlin Multiplatform** nello stesso repo: `:core:data`
  e `:core:engine` condivisi (validatori identici tra app e tool,
  simulazione col motore vero), `:tool` Compose Desktop.
- **Pipeline a 6 stadi**: struttura da XML Aon (deterministica) ->
  meccaniche da KC (deterministica) -> LLM solo rifinitore opzionale
  (`locationName`, `toneHints`; chiave dell'utente; il tool funziona
  anche senza) -> validatori condivisi -> simulazione -> report
  human-in-the-loop (tag + frase sorgente).
- **Modello legale**: si distribuisce lo strumento, mai il contenuto;
  downloader integrato nel tool come primo passo del wizard, AVVIATO
  DALL'UTENTE (precedente: Kai Chronicles scarica da Project Aon via
  HTTP) — solo i file dei libri 1-3, una richiesta alla volta, cache
  locale, fallback manuale; niente script esterni per OS; pacchetti
  solo uso personale.
- **Perimetro v0.1**: libro 1 pilota, poi 2-3 (esperienza diretta del
  primo tentativo: oltre il 3 le meccaniche divergono; dal 6 ciclo
  Magnakai — lavoro futuro).
- **Diagnosi del blocco storico di v1**: modello debole × fonte HTML ×
  zero validazione — tutti e tre i fattori ribaltati.

**Prossima e ULTIMA specifica: 6 (analisi criticità)** — poi
`PIANO-SVILUPPO.md` e si scrive codice.

### Sessione — chiusura specifica 6 (criticità) — DESIGN CONCLUSO

**Specifica 6 CHIUSA** (`doc/CRITICITA.md`) — **DESIGN CONCLUSO (6/6)**.
Decisioni:

- **Modello**: Gemma 3 4B via LiteRT-LM (lo stesso di v1, provato);
  contesto di riferimento 10240 token.
- **INFERENZA SENZA MEMORIA**: sessione nuova per ogni scena; contesto
  = frammenti fissi + coda scena precedente + scena + scelte; il
  diario non entra MAI nel prompt. È la taratura del motore di
  inferenza di Ex.
- **Criticità madre**: velocità inferenza su Razr (misure: primo token
  <3s, token/s vs velocità di lettura, termico su sessioni 30-45').
- **Obblighi di piano**: scrittura ATOMICA dei salvataggi
  (temp+rename), streaming Compose bufferizzato ~80-100ms, fixture con
  output Gemma reali.
- **Non-criticità liquidate con misura di conferma**: pacchetto 350
  scene, diario, auto-save, toggle, animazione dado.

**PROSSIMO E ULTIMO PASSO PRIMA DEL CODICE: `doc/PIANO-SVILUPPO.md`.**

### Sessione — piano di sviluppo: DESIGN CONCLUSO (6/6 + piano)

**`doc/PIANO-SVILUPPO.md` generato.** 8 fasi con milestone testabili
(Fondamenta → `:core:data` → `:core:engine` → **MILESTONE REGINA**:
il libro gira per intero senza Gemma → inference → UI funzionale
completa → `:tool` ETL → Abbellimento). Principio "**prima funziona,
poi è bello**": UI Material di default fino alla Fase 6, l'estetica di
`doc/UI.md` è tutta nella Fase 7 dedicata. Task **[MICHELE]** riservati
per fase (enum canonici, tabella CRT, fixture di test, strings.xml,
revisione ETL) — Claude Code non li implementa, al più impalca e
segnala se bloccano.

**Prossima sessione: SVILUPPO, Fase 0** (checklist in fondo al piano).

### Sessione — SVILUPPO: Fase 0 (Fondamenta) CHIUSA

Prima sessione di codice del progetto. 4 commit atomici + questo:

- **Progetto Gradle KMP**: 4 moduli (`:core:data`, `:core:engine` KMP
  puro con target `jvm()`+`androidTarget()`, zero codice Android nel
  common; `:app` Android/Compose; `:tool` placeholder Kotlin/JVM,
  Compose Desktop vero arriva in Fase 6). Versioni Kotlin 2.0.21 / AGP
  8.10.1 / Gradle 8.11.1, le stesse già in uso in v1 su questa
  macchina. Aggiunto `.gitattributes` (LF forzato su `gradlew`, jar
  wrapper come binario — altrimenti `core.autocrlf=true` lo rompe al
  checkout).
- **`kotlinx.serialization`** (1.7.3) aggiunta come dipendenza in
  entrambi i moduli core, non ancora usata (arriva con i modelli in
  Fase 1).
- **Test JVM segnaposto** in `commonTest` di entrambi i moduli core
  (kotlin.test): verificano sia `jvmTest` (puro JVM, zero SDK Android)
  sia `testDebugUnitTest`/`testReleaseUnitTest` (target Android) —
  doppia conferma che "zero dipendenze Android" nel codice comune è
  rispettato meccanicamente, non solo per convenzione.
- **`:app` scheletro**: single-activity (`MainActivity`) + Compose,
  un solo `Text` segnaposto, tema Android di sistema (niente
  `themes.xml`, coerente col principio "prima funziona poi è bello").
  Namespace/applicationId scelti: `io.github.luposolitario.immundanoctisex`
  (stesso prefisso di v1 + suffisso "ex") — da confermare con Michele,
  facile da cambiare ora.
- **Blocco ambiente (risolto)**: alla prima configurazione Gradle non
  risolveva NESSUN plugin (Google/MavenCentral/Gradle Plugin Portal):
  l'handshake TLS cadeva subito. Causa: due CA di intercettazione
  HTTPS installate nello store di Windows (`AVG Web/Mail Shield Root`,
  scansione SSL/TLS dell'antivirus, e `Cato Networks Root CA`) fidate
  da Windows/.NET (PowerShell funzionava) ma non dalla JVM (Gradle e
  curl fallivano). Sessione fermata e segnalata a Michele com da
  regola del piano; sbloccata disattivando la scansione HTTPS di AVG.
  Nota per sessioni future su questa macchina: se Gradle non risolve
  dipendenze con errori di rete "istantanei" (handshake che si
  interrompe in pochi ms), è questo — non un problema del progetto.
- **Milestone verificata**: `./gradlew test` verde su tutti e 4 i
  moduli; APK installato e avviato sull'AVD `Small_Desktop`
  (android-34), activity in foreground, nessun crash in logcat
  (verifica indiretta: `dumpsys window`/`ps`/`logcat`, lo screenshot
  non è disponibile su questo AVD). Non testato sul Razr fisico
  (non disponibile in questa sessione) — da fare alla prima occasione
  con l'hardware reale.

**Prossimo task: Fase 1 — `:core:data`** (modelli, `PackageSource`,
validatori — vedi `doc/ARCHITETTURA.md` e `doc/PIANO-SVILUPPO.md`).
Task [MICHELE] in coda per questa fase: enum `WeaponType` e `KaiRank`
(soglie da `doc/REGOLE.md` §Blocco 3).

### Sessione — SVILUPPO: Fase 1 (`:core:data`) CHIUSA

6 commit atomici (modelli pacchetto, modelli stato/sessione,
`PackageSource`+`PackageRepository`, validatori, aggiornamento sample,
test JVM).

- **Modelli pacchetto**: `Manifest`, `Scene`/`SceneType`, `Choice`,
  `DisciplineChoice`, `DisciplineDescriptor`, `Combat` (REGOLE.md
  §1.5), `GlobalRule`/`GlobalRuleType`/`ComparisonOperator` (REGOLE.md
  Blocco 2, operatori serializzati con `@SerialName` sui simboli
  `==`/`!=`/ecc.), `Discipline` (10 canoniche).
- **Modelli stato/sessione**: `Character`/`CharacterRole`, `GameItem`/
  `ItemType`, `StatModifier`/`StatType`, `Difficulty`, `SessionData`,
  `JourneyEntry` con `Transition` sealed (ChoiceTaken/DisciplineUsed/
  CombatResolved/AutoJump) — fedeli a STATO.md, nessun calcolo di
  bonus qui (si serializzano i fatti, si calcola in Fase 2).
- **`gameMechanics` generico**: `GameMechanic(command, params:
  JsonObject)` invece di una gerarchia tipizzata per i 18 comandi —
  quei tipi sono comportamento (:core:engine, Fase 2), qui serve solo
  a caricare/validare. I validatori che devono leggere i parametri
  (rollOnItemTable) lo fanno leggendo "params" direttamente.
- **`PackageSource` + `PackageRepository`**: quarta interfaccia
  motivata implementata; il repository carica da `InputStream`,
  valida, espone `getSceneById`/`startScene`/manifest; un pacchetto
  rotto (JSON malformato o che fallisce i validatori) non lascia mai
  l'istanza in stato parziale.
- **5 validatori** (uno per file, orchestrati da `PackageValidator`):
  grafo chiuso, discipline canoniche, `combat.winSceneId` obbligatorio,
  intervalli `rollOnItemTable` completi/disgiunti (0-9), warning
  globalRules verso scene non-ENDING. Aggiunti due controlli minimi
  non esplicitamente elencati nel piano ma necessari per "messaggi
  chiari": id di scena duplicati, assenza di una scena START.
- **`content/scenes.sample.json` disallineato, ora corretto**: era
  fermo al 14/07 (prima delle specifiche 2-4) — scena 4 usava ancora
  `combatChoices[]` legacy invece del blocco `combat` di REGOLE.md
  §1.5. Sostituito; aggiunto `deathSceneId` al manifest e
  `locationName` dove il luogo cambia davvero (scene 1, 2, 3, 6 — le
  altre ereditano, dimostra il comportamento "appiccicoso" della
  specifica 4).
- **Non implementato per scelta**: `WeaponType` è task [MICHELE].
  `GameItem.weaponType` e `Character.weaponSkillType` restano `String`
  segnaposto (impalcatura pronta per lo swap all'enum quando Michele
  lo scrive).
- **Milestone verificata**: 12 test JVM verdi (`PackageRepositoryTest`
  carica il sample reale da risorsa e naviga il grafo;
  `PackageValidatorTest`, 9 casi, isola una violazione per test —
  destinazione inesistente, id duplicato, scena START assente,
  disciplina non canonica, `winSceneId` vuoto, buco/sovrapposizione in
  `rollOnItemTable`, globalRule non-ENDING come warning non errore).
  `./gradlew test` verde su tutti i moduli.

**Prossimo task: Fase 2 — `:core:engine`** (`GameState`, funzione
unica stat effettive, i 18 comandi, `CombatManager`, `DiceRoller` —
vedi `doc/REGOLE.md` e `doc/PIANO-SVILUPPO.md`). Task [MICHELE] in
coda: trascrizione tabella CRT da `LoneWolfRules` di v1 + fixture di
test scritte a mano.

### Attività — DiceRoller e funzione stat effettive (Fase 2, in corso)

Primo pezzo di `:core:engine`: interfaccia `DiceRoller` (tiro 0-9,
iniettata, mai `Random` inline) e `effectiveCombatSkill(Character)`
(base + somma modificatori attivi su COMBATTIVITA, REGOLE.md §1.2).
**TODO**: il bonus WEAPONSKILL nella funzione di stat resta da fare,
in attesa dell'enum `WeaponType` [MICHELE] (STATO.md §4.3) — senza un
tipo canonico non si può confrontare `weaponSkillType` con l'arma
impugnata. Fase 2 resta APERTA: `GameState`, `CombatManager`, i 18
comandi non ancora iniziati.

### Controllo pre-push — DiceRoller e funzione stat effettive

Verifica da terminale: `./gradlew test` verde su tutti i moduli, commit
`35fa280` corretto ma **incompleto** rispetto al task richiesto —
implementata solo `effectiveCombatSkill`; manca `effectiveEndurance`
(Resistenza effettiva, clamp tra 0 e `maxEndurance`). Da completare
nella PROSSIMA sessione operativa, insieme a un commit separato per le
modifiche Gradle generate dalla sync di Android Studio e non ancora
committate: `settings.gradle.kts` (plugin `foojay-resolver-convention`
per il toolchain) e il nuovo file `gradle/gradle-daemon-jvm.properties`
(JDK 21 per il daemon).

**Prossimo task: completare `effectiveEndurance` in `:core:engine`,
commit Gradle a parte, poi proseguire Fase 2** (`GameState`, i 18
comandi, `CombatManager` — questi ultimi bloccati dal task [MICHELE]
sulla tabella CRT).

### Sessione — decisioni UI (Home, Opzioni, salvataggio narrazione) + riuso SetupActivity v1

Richieste di Michele (screenshot v1 alla mano), recepite nei documenti:

- **Home a riquadri come v1** (UI.md §schermata 1): tre tile —
  Avventura (continua/scegli salvataggio/nuova), Modelli LLM (download
  Gemma + configurazione inferenza), Impostazioni. NIENTE tile STDF
  (Genera Immagini e Modelli STDF: feature morta di v1).
- **Opzioni** (UI.md §schermata 7): tema chiaro/scuro (pattern
  `ThemePreferences` v1), abilitazione TTS, **salvataggio narrazione
  automatico/manuale**.
- **Salvataggio narrazione** — RETTIFICA nella stessa giornata:
  l'opzione automatico/manuale con icona salva per-blocco, prima
  recepita, è stata SCARTATA da Michele dopo verifica di
  `ChatComponents.kt` v1. Decisione finale: **si salva sempre tutto
  automaticamente**, nessuna icona salva (STATO.md §Blocco 3
  aggiornato). Nei blocchi del narratore restano TRE icone: copia,
  originale/tradotto, leggi (TTS) — quest'ultima grigia/disattivata
  quando l'auto-lettura è accesa in Opzioni.
- **Verifica su v1**: in `ChatComponents.kt` (master) le icone bubble
  sono copia (sempre), traduci (solo narratore, spinner mentre
  traduce), leggi (sempre); NESSUNA icona save esiste in master —
  l'eventuale icona save ricordata vive forse sul branch develop di
  v1, non presente su disco (la copia locale non è un repo git).
  Regola dello spinner ereditata sul toggle originale/tradotto.
- **Stato del narratore unificato** (richiesta Michele, UI.md §Banner):
  IDLE/GENERATING/SPEAKING — cerchio d'oro acceso mentre Gemma streama
  E mentre il TTS legge. Verificato in v1: la parte "LLM sta
  scrivendo" esiste (`streamingText`+`isGenerating`+
  `respondingCharacterId` nel MainViewModel, stream mostrato troncato
  a `--- TAGS ---` in AdventureActivity, bordo sul ritratto in
  AdventureHeader) ed è pattern riusabile; la parte "TTS sta
  parlando" in v1 NON esiste (`TtsService` senza
  `UtteranceProgressListener`) — in Ex si aggiunge il listener e
  entrambe le sorgenti alimentano lo stesso stato osservabile.

**Analisi riuso `SetupActivity.kt` v1** (451 righe, letta integralmente):

- **RIUSABILI come pattern/struttura** (riscrivere in Ex, non copiare):
  `RandomStatsCard` (tiro stat), `EquipmentChoiceCard` (arma
  obbligatoria + UN oggetto speciale, righe con icona/nome/descrizione),
  `DisciplineGridCard` (griglia adattiva 5/10 con contatore, selezione
  che disabilita le altre a quota raggiunta, alpha 0.5), gating
  `canProceed` (stat tirate + 5 discipline + arma + oggetto + spec
  WEAPONSKILL se scelta), `WeaponSkillSelectionDialog` (flusso spec
  WEAPONSKILL), `ExistingSessionScreen` ("Bentornato!", continua/nuova).
- **DA NON RIPORTARE**: `GameStateManager.getInstance` (singleton con
  Context — in Ex porta iniettata), `GameCharacter` come uiState del
  ViewModel (in Ex i modelli sono in :core:data), discipline come nomi
  display ("Weaponskill" — in Ex ID canonici UPPER_SNAKE),
  `portraitResId`/`characterClass` (concetti v1), doppio avvio
  Activity (in Ex single-activity + routing).
- **Mancano in v1 e vanno aggiunti in Ex** (già in UI.md): scelta
  lupo/lupa, Dado del Destino teatrale per il tiro stat (in v1 è un
  bottone), scelta difficoltà (vive nel Setup avventura, schermata 2).

### Sessione — Fase 2: GameState, inventario, 16 comandi, pipeline di transizione

Tre commit atomici, `./gradlew test` verde su tutti i moduli dopo ognuno:

- **GameState** (`engine/state`): unica fonte di verità, SessionData
  immutabile evoluta per copia, `snapshot()` per la persistenza; accessor
  tipizzati per flag/variabili/eroe. **Inventario** (`engine/inventory`):
  funzioni pure con i limiti canonici (WEAPON 2, zaino 8 posti a unità
  di quantità, GOLD 50 con aggiunta parziale fino al tetto, SPECIAL
  illimitati e impilabili); rimozione tollerante; equip solo di armi
  possedute; rimozione dell'arma impugnata azzera `equippedWeapon`.
- **MechanicsExecutor** (`engine/mechanics`, + ItemMechanics /
  StatMechanics / Params per la soglia ~200 righe): i 16 comandi di
  scena non-combat con semantica verificata sul MainViewModel di v1.
  Decisioni: variazioni ENDURANCE = fatti diretti su currentEndurance
  (clamp 0..max), variazioni COMBAT_SKILL = StatModifier NARRATIVE;
  ID stat canonici (ENDURANCE/COMBAT_SKILL, niente alias italiani);
  operatori sia simboli sia parole v1; outcomes a intervalli espliciti
  minRoll/maxRoll (formato del validatore, non il "range":"0-4" di v1);
  primo salto interrompe i comandi successivi; comando/parametro
  sconosciuto = nessun effetto, mai errore. `handleConditionalAction`
  esegue un comando annidato STRUTTURATO ({command, params}), non una
  stringa-tag come v1. `requireAction EAT_MEAL`: HUNTING esente a costo
  zero, poi consumo "Meal", poi penalità dichiarata. `rollForQuantity`
  default GOLD (come v1), override con `itemType`.
- **TransitionEngine** (`engine/transition`): arrivo -> HEALING passiva
  (+1 verso scene senza combat) -> gameMechanics -> morte built-in
  (PRIMA delle globalRules: morire batte vincere, testato) ->
  globalRules in ordine di scrittura -> giro ripetuto sulla nuova scena;
  `maxHops=20` come rete di sicurezza sui cicli scritti male; scena
  inesistente = si resta dov'eravamo. Restituisce gli `AutoJumpHop` per
  le voci AutoJump del diario-grafo.
- **Modello esteso** (`:core:data`): `AutoJumpReason` += SKILL_CHECK,
  RANDOM_CHOICE, BUILT_IN_DEATH (anche questi salti sono porte del
  diario-grafo).

**Fase 2 ancora APERTA**: mancano CombatManager (bloccato dal task
[MICHELE] tabella CRT), il bonus WEAPONSKILL nelle stat effettive
(bloccato da enum WeaponType [MICHELE]) e il test di milestone "partita
completa del sample da terminale" (ha senso a combat pronto).

### Sessione — ex task [MICHELE] delegati: WeaponType, KaiRank, CRT, bonus WEAPONSKILL

Michele ha delegato i task di copia ("falli tu, io al massimo
controllo"). Quattro commit atomici, test verdi:

- **`WeaponType`** (`:core:data`): le 9 armi canoniche del libro 1 +
  UNARMED (specializzazione, mai arma di un GameItem);
  `Character.weaponSkillType` e `GameItem.weaponType` da String? a
  WeaponType?.
- **`KaiRank`** (`:core:engine`): soglie di REGOLE.md Blocco 3, ID
  canonici inglesi, nomi display rimandati a strings.xml.
- **`CombatResultsTable`** (`:core:engine`): **la CRT di v1 è stata
  CONFRONTATA CON L'UFFICIALE E SCARTATA** — valori difformi (rapporto
  0/tiro 0: v1 dava 16, l'ufficiale 12), nessuna morte istantanea del
  giocatore ai rapporti molto negativi (l'ufficiale ce l'ha da -9/-10
  col tiro 1), bande a coppie non rispettate (in v1 -1 e -2 differivano).
  Fonte adottata: `combatTable.ts` di Kai Chronicles (GPL v3), che
  trascrive la tabella di Project Aon — la stessa fonte della pipeline
  ETL. Supera anche il coerceIn [-10,+10] di REGOLE.md §1.2: le bande
  ufficiali arrivano a ±11-o-più e la colonna estrema assorbe qualsiasi
  rapporto. Il tiro è la chiave reale della riga: la trappola
  off-by-one del tiro 0 di v1 è strutturalmente impossibile; fixture
  comunque presenti (tiro 0 = colpo migliore, bande a coppie, morti
  istantanee, campioni puntuali). **[MICHELE] da verificare**: un
  controllo a campione della tabella contro il libro cartaceo/Project
  Aon resta gradito.
- **Bonus WEAPONSKILL** nelle stat effettive: +2 se arma impugnata del
  tipo della specializzazione; UNARMED: +2 solo senza arma; la
  specializzazione senza la disciplina non vale nulla.

**Restano per chiudere la Fase 2**: CombatManager (ora sbloccato) e il
test di milestone della partita completa del sample da terminale.

### Sessione — FASE 2 CHIUSA: CombatSession e milestone della partita simulata

- **`CombatSession`** (`engine/combat`): nemico idratato in Character
  unico (role ENEMY) dal blocco combat; round sulla CRT ufficiale con
  rapporto da stat effettive simmetriche; sentinella KILL -> Resistenza
  0; la morte del giocatore batte quella del nemico nello stesso round;
  MINDBLAST (+2 una volta, negato da immunità/disciplina mancante,
  decade in `playerAfterCombat`); oggetti solo `combatUsable` con
  HEAL:n, consumati; evasione col costo canonico (round di soli danni
  al giocatore, può uccidere -> LOSE) sbloccata da `evadeAfterRound`;
  rapido = `quickResolve()`, loop dello STESSO round (nessuna logica
  duplicata). `destinationSceneId` con loseSceneId nullable: il
  chiamante degrada su `deathSceneId` (specifico batte globale).
  Il combattimento resta atomico: la sessione vive in memoria, mai
  salvata a metà.
- **MILESTONE FASE 2 VERDE** (`GiocataCompletaDelSampleTest`): il
  sample vero (content/ montato come resources di test, niente copie)
  giocato da terminale su quattro percorsi — vittoria attraverso il
  combattimento (2 round, tiri 0), morte in combattimento (rapporto
  -11, KILL istantaneo, loseSceneId=deathSceneId), percorso furtivo
  con SIXTH_SENSE + HEALING passiva (+1 per ognuna delle 4 transizioni
  senza combat), morte built-in fuori combattimento. Diario-grafo
  registrato dal test come farà la UI.
- CLAUDE.md aggiornato: **fase corrente -> Fase 3** (il libro gira
  senza Gemma).

### Sessione — analisi `ui/` di v1 e convenzione @Preview (annotazione urgente Michele)

Colmata la lacuna segnalata da Michele: la cartella `ui/` di v1 non
era mai stata analizzata. Nuovo documento **`doc/ANALISI-UI-V1.md`**
(11 file, 1.618 righe scansionate; lo zip fornito da Michele è
identico alla copia su disco). Sintesi: `theme/` riuso quasi
integrale; `ChoiceComponents` riuso quasi diretto (zona scelte);
`AdventureHeader` a pezzi (CharacterPortrait, TokenSemaphoreIndicator
= il semaforo di UI.md già fatto); `PlayerActionBar` pattern per la
card di stato + convenzione bordo oro/argento sul dado da conservare;
`AdventureUtils` con bug da correggere (icone mappate sui nomi display
invece che sugli ID); `AdventureDialogs` morto, non riusare;
`configuration/ModelSlot` base della schermata Modelli LLM E modello
della convenzione preview. Nota mapping WeaponType v1→Ex
(STAFF→QUARTERSTAFF, FISTS→UNARMED, GENERIC degrada).

**CONVENZIONE @PREVIEW (requisito Michele)**: ogni composable di Ex ha
la sua @Preview (chiaro+scuro), componenti stateless per costruzione
(mai ViewModel/Context dentro), dati finti in PreviewData.kt per
package, variante *Preview quando servono dipendenze runtime (pattern
ModelSlot v1). Registrata in UI.md §Convenzioni e in ANALISI-UI-V1.md;
mappa documenti del piano aggiornata. In v1, contrariamente al
ricordo, l'unica @Preview reale era in ModelSlot.kt — la volontà "ogni
cosa in preview" diventa realtà in Ex.

**Prossima sessione (richiesta Michele)**: estendere l'analisi UI **a
ritroso** — dalle componenti di `ui/` alle Activity di v1 che le
richiamano (`AdventureActivity` 683 righe, `CharacterSheetActivity`
569, `SetupActivity` 451 già fatta, `MainActivity`, `ModelActivity`
824, `ConfigurationActivity`, `DeathActivity`) — per censire le
funzioni COMPATIBILI tra v1 ed Ex che non costano grandi riscritture.
Poi Fase 3 (app Android minima che gioca il sample sul Razr).

### Sessione — seconda passata analisi UI: dalle Activity alle componenti

Fatta la prima passata a ritroso richiesta (seconda sezione di
`doc/ANALISI-UI-V1.md`): 8 Activity (3.505 righe) + ViewModel (2.561)
+ util/service (1.196). Sintesi dei verdetti:

- **MainActivity riuso quasi diretto**: `MenuIcon`+`MainMenuScreen`
  SONO la Home a riquadri decisa per Ex (meno le tile STDF).
- **AdventureActivity**: scheletro buono della scena teatrale (top bar
  con semaforo + "Paragrafo: N", streaming già troncato a
  `--- TAGS ---`, zone nell'ordine giusto); da buttare MessageInput,
  selezione personaggio chat e i dialoghi combat commentati.
  `LoadingScreen`/`ErrorScreen` generiche. `InventoryFullDialog` =
  upgrade futuro dell'inventario pieno. SCOPERTA: l'opzione "Salva
  Chat Manualmente" viveva nel menu a tendina (con
  `SavePreferences.isAutoSaveEnabled`) — origine del ricordo di
  Michele; la decisione Ex (sempre automatico) resta.
- **CharacterSheetActivity riuso forte**: gli 8 slot zaino DISEGNATI
  ANCHE VUOTI e i 2 slot arma con bordo oro sono già implementati
  (WeaponsCard/WeaponSlot/CommonItemsCard/CommonItemSlot + Stats/
  Discipline/SpecialItems card). Il ViewModel invece si butta (la
  logica ora è nell'engine).
- **ConfigurationActivity = schermata Opzioni**: switch auto-lettura,
  slider rate/pitch, voce per genere MALE/FEMALE già fatti; via gli
  switch auto-save e chat; dropdown "tono narrativo" utente = feature
  fuori design, marcata [MICHELE-PROPOSTO].
- **ModelActivity**: si salvano `SceneJsonPicker` (side-load) e il
  pattern ModelSlot; il dual-engine Gemma+Llama si ridimensiona.
- **DeathActivity**: pattern per il rendering delle ENDING.
- **Censimento finale a basso costo**: ~25 composable/classi pronte
  quasi com'è (elenco in ANALISI-UI-V1.md §Censimento finale);
  riscrittura vera solo per AdventureChatScreen->scena teatrale,
  MainEngineScreen->Opzioni, MessageBubble->blocco narratore,
  gestione modelli solo-Gemma. Tutte le migrazioni nascono con
  @Preview.

**Prossimo: Fase 3** — app Android minima che gioca il sample sul
Razr, partendo da Home (MenuIcon/MainMenuScreen) e scena teatrale.

### Sessione — terza passata: interazioni particolari nei ViewModel di v1

Censite in `doc/ANALISI-UI-V1.md` §Terza passata le interazioni
UI<->logica non ovvie:

- **Tiro a due fasi** (arma->tira->risolvi con bordo oro sul dado):
  antenato dell'overlay Dado del Destino, SI CONSERVA; da correggere
  il trigger (v1 SNIFFA il testo italiano cercando "Tabella dei Numeri
  Casuali" — in Ex trigger strutturale) e il Random inline (->
  DiceRoller).
- **SCOPERTA — WEAPONSKILL: v1 TIRA la specializzazione a caso** (con
  dialog di conferma), che è il canone dei libri; UI.md dice invece
  "scelta". Marcato **[MICHELE]**: decidere tiro canonico (teatrale,
  flusso v1 pronto) vs scelta libera.
- Gating scelte (requiredFlag/Item + canUseDiscipline): identico al
  design Ex, riuso diretto. Decisioni nel flusso come messaggi
  "*Sceglie di...*": embrione della vista live del diario-grafo.
- Canali evento da riusare snelliti: `uiFeedbackEvent` (toast esiti
  mechanics), `engineLoadingState` (Loading/Ready/Error),
  `inventoryFullState` (dialog futuro); da NON riusare `isHeroDead`
  (in Ex la morte è una transizione) e il `flatMapLatest` dual-engine
  del token info (un solo motore).
- `navigateToScene`: reset scelte/tiro identico in Ex; stranezza da
  non ripetere: salvava solo alla PRIMA visita (`usedScenes` come
  gate) — in Ex auto-save a ogni transizione.
- CharacterSheetViewModel: confermata la TERZA copia del calcolo stat
  effettive (il difetto noto) — in Ex la scheda legge solo le funzioni
  dell'engine.
- Combattimento: tutti i canali combat sono commentati, la UI combat
  di Ex nasce da zero su CombatSession (nessun debito).

### Decisioni Michele + asset v1 adottati

- **WEAPONSKILL: SCELTA del giocatore** (non il tiro obbligatorio
  canonico/v1); al massimo un bottone "scegli a caso" in aggiunta.
  UI.md §Creazione e ANALISI-UI-V1.md §Terza passata aggiornati.
- **Nuova opzione: scelta del FONT** del testo di lettura (rosa di
  font, serif di default della pagina di libro) — aggiunta a UI.md
  §Opzioni.
- **Asset v1 adottati**: Michele ha portato nel repo la cartella
  `origina_res/` (44 MB, set del branch develop di v1 — quello RICCO
  che l'inventario asset su master non aveva): icone armi complete
  (axe/sword/mace/staff/spear/broadsword/fists), icone oggetti
  (backpack/gold/meal/potion/armor/helmet), ritratti (eroe m/f, dm,
  elara, mage, classi), lupo_solitario.png, mappa, launcher icon
  Android completa (mipmap + playstore), values (colors/strings/
  themes). Committata così com'è come SORGENTE asset; il cablaggio in
  `app/src/main/res` e l'ottimizzazione WebP (~79% stimato, molti
  PNG da 3-4 MB) restano per le Fasi 3/7. Piace anche il tema di v1
  (`ui/theme` + `values/themes.xml`): si riusa come da analisi.
  Mapping icone->WeaponType Ex: ic_staff->QUARTERSTAFF,
  ic_fists->UNARMED; mancano dagger/short_sword/warhammer (da
  produrre o fallback ic_unknown_item).

### Censimento di completezza: 56/56 file .kt di v1 analizzati

Controllo richiesto da Michele: incrociato l'elenco completo dei 56
file `.kt` di v1 con tutte le analisi fatte — 47 già coperti, i 9
mancanti chiusi nella **Quarta passata** di `doc/ANALISI-UI-V1.md`
(data/ChatMessage e GameData; util/Downloadable, Gemma/Model
Preferences e worker/DownloadWorker riusabili in Fase 4 — i parametri
Gemma di v1 sono un default già tarato; Engine/Llama/ImageGeneration
Preferences morte). Il censimento v1 è COMPLETO: ogni file ha un
verdetto riusa / pattern / sostituito / morto.

### Sessione — FASE 3 APERTA: persistenza e scheletro app

- **3.1 `SessionStore`/`FileSessionStore`** (`:core:data/session`,
  porta iniettabile come PackageSource): auto-save ATOMICO
  (temp+`Files.move` ATOMIC_MOVE), salvataggio corrotto degrada a
  nessun-salvataggio, `listSessions` per la Home, checkpoint scritti
  UNA volta e mai sovrascrivibili (ritorna false, il contenuto resta
  quello del primo piazzamento), ricarica illimitata,
  `deleteAdventure` (sessione+checkpoint: morte IRON e nuova
  avventura). 8 test JVM su directory temporanee.
- **3.2 scheletro `:app`**: `AppContainer` (DI leggera: store su
  filesDir/saves, `RandomDiceRoller` nuovo in engine,
  PackageRepository su `AssetPackageSource`); `content/` montato come
  cartella ASSET dell'APK (niente copie del sample); tema v1 portato
  (Color/Theme, dark+light, niente dynamic color) con
  `ThemePreferences` a tre stati (sistema/chiaro/scuro); routing
  single-activity a stati (enum Route + back stack, solo routing);
  **Home a riquadri** funzionante (3 tile, toggle tema in top bar,
  @Preview chiaro+scuro come da convenzione). Aggiunta dipendenza
  `material-icons-extended`. Build e suite verdi; APK compilato — la
  PROVA SUL RAZR resta da fare (telefono non collegato al momento).

**Prossimi task Fase 3**: 3.3 Setup avventura (difficoltà) + Creazione
personaggio; 3.4 scena teatrale minima con transizioni+auto-save; poi
combat UI, scheda, diario, checkpoint/morte.

### Sessione — Fase 3.3: Setup avventura e Creazione personaggio

- **Modello**: aggiunto `Gender` (MALE/FEMALE) e `Character.gender` —
  lacuna scoperta ora, UI.md lo richiedeva con tre clienti (ritratto,
  TTS, prompt).
- **`strings.xml` IMPALCATO** (il task [MICHELE] resta suo per la
  rifinitura): nomi/descrizioni delle 10 discipline, gradi Kai, tipi
  arma, difficoltà con spiegazione onesta di IRON, testi di setup e
  creazione. Regola rispettata: ID canonici nei dati, nomi mostrati
  SOLO qui.
- **`AdventureSetupScreen`**: lista salvataggi da `listSessions` con
  card "continua" (data ultimo salvataggio, difficoltà, scena) +
  nuova avventura con le tre card difficoltà (IRON evidenziata in
  rosso con la spiegazione della cancellazione). Stateless + 2
  @Preview.
- **`CharacterCreationScreen` + `CreationState`**: lupo/lupa
  (segmented), tiro stat canonico (CS 10+tiro, RES 20+tiro, Corone =
  tiro) via `DiceRoller` del container (MAI Random inline), griglia
  discipline 5/10 con contatore e disabilitazione a quota,
  specializzazione WEAPONSKILL A SCELTA con bottone "scegli a caso"
  (decisione Michele), arma iniziale (4 armi canoniche), gating
  canProceed ereditato da v1. Lo stato è una classe semplice (niente
  androidx ViewModel: previewabile e testabile). `buildSession` crea
  la fotografia iniziale: eroe con arma impugnata, Corone, scena START.
- **Wiring**: `SetupRoute`/`CreationRoute` raccordano container e
  schermate stateless (il file di navigazione resta solo routing);
  primo auto-save alla creazione; "continua"/creazione portano al
  segnaposto ADVENTURE (prossimo task 3.4).

**Prossimo: 3.4 scena teatrale minima** (testo originale, scelte
filtrate, transizioni con TransitionEngine, auto-save, diario-grafo).

### Sessione — Fase 3.4: la scena teatrale minima GIRA

**`AdventureState`** cabla l'engine alla UI: GameState dalla sessione
(nuova o ripresa dall'auto-save), TransitionEngine per ogni porta,
voce del diario-grafo AD OGNI passo (incluse le AutoJump dei salti
d'ufficio), auto-save atomico a ogni transizione, morte in IRON che
CANCELLA la sessione (deleteAdventure) mostrando comunque la scena di
morte. Combat v0.1: solo modalità RAPIDA su CombatSession (riepilogo
esito/round/Resistenze, "Continua" verso win/lose con fallback
deathSceneId); il menu tattico completo è il prossimo task.

**`AdventureScreen`** (v0.1 nelle zone giuste): header
titolo-dal-manifest + "Scena N", testo ORIGINALE come pagina di libro,
card di stato con CS/RES EFFETTIVE dall'engine + Corone, zona scelte
(bottoni pieni; scelte-disciplina distinte con icona, incluse le fughe
gratis pre-combattimento), zone combat/ending. Gating scelte:
requiredFlag/requiredItem rispettati; scelte con minRoll/maxRoll
nascoste (il flusso del Dado a due fasi arriva con la Fase 5; il
sample non ne usa). `AdventureRoute` + wiring navigazione (continua
salvataggio -> riprende dalla scena salvata; exit -> Home con stack
pulito).

Build+test verdi. Da provare sul Razr (device non collegato): il
flusso Home -> Avventura -> creazione -> partita completa -> ENDING.

**Restano per chiudere la Fase 3**: menu tattico combat completo,
Scheda personaggio operativa, Diario del viaggio, checkpoint UI,
side-load libro, prova di milestone SUL DEVICE (partita completa,
ripresa a metà, IRON).

### Sessione — Fase 3.5 + 3.6: combat completo e Scheda personaggio

- **3.5 Combat completo** (`CombatZone.kt`): scelta modalità
  Rapido/Completo dopo le fughe-disciplina; nel completo: barre
  Resistenza di entrambi, Rapporto di Forza, esito dell'ultimo tiro
  (con "MORTE" per la sentinella KILL), menu tattico — round col dado,
  MINDBLAST (disabilitato COL MOTIVO se immune o già attivo), oggetti
  combatUsable, fuga (disabilitata finché `evadeAfterRound` non
  passa, col round di sblocco mostrato). La UI osserva la
  CombatSession dell'engine tramite un contatore (`combatTick`):
  l'engine resta puro, niente Compose in core.
- **3.6 Scheda personaggio** (`ui/sheet`): due tab; Stats con grado
  Kai da strings.xml e stat EFFETTIVE dell'engine (mai ricalcolate:
  il difetto di v1 non si ripete); Equipaggiamento con 2 slot armi
  (tocco = impugna, evidenza sull'impugnata, effetto immediato sulla
  CS mostrata), zaino con gli 8 POSTI DISEGNATI anche vuoti, consumo
  a tocco degli oggetti HEAL:n, oggetti speciali e Corone/50. Overlay
  dentro la route Avventura (stato condiviso; destinazione propria in
  Fase 5). Equip e consumi passano dall'engine e auto-salvano.

**Restano**: Diario del viaggio, checkpoint UI, side-load, prova di
milestone sul device.

### Sessione — Fase 3.7 + 3.8: Diario del viaggio e Checkpoint

- **Modello**: `JourneyEntry.locationName` aggiunto (il luogo GIÀ
  RISOLTO alla visita, come da coda di UI.md); AdventureState risolve
  l'appiccicosità (scena senza luogo eredita il precedente; alla
  ripresa riparte dall'ultima voce del diario).
- **3.7 `JournalScreen`** (`ui/journal`): vista **Racconto** (card per
  voce: scena, luogo, testo, transizione in corsivo — scelta fatta,
  disciplina usata, esito combat, salto del destino) e **Mappa
  logica** v0.1 (i luoghi consecutivi in ordine di viaggio, derivati
  dal diario mai salvati); **export Markdown** con lo share sheet di
  Android (`journeyToMarkdown`: il diario è già un generatore di
  racconto). Accesso dal bottone "Diario" nell'header.
- **3.8 Checkpoint**: bottone di piazzamento sotto le scelte col
  budget visibile (NORMAL 2 / HARD 1 / IRON 0), sparisce a budget
  esaurito; `GameState.incrementCheckpointsUsed` per la contabilità;
  slot immutabili (il FileSessionStore rifiuta le riscritture). Alla
  MORTE fuori da IRON la scena di morte offre "Ricarica il checkpoint
  N": ripristina la fotografia (diario troncato per costruzione),
  la salva come sessione corrente e ricrea lo stato di gioco.

**Per chiudere la Fase 3 resta SOLO la prova di milestone sul Razr**:
partita completa, chiusura a metà e ripresa, morte in IRON che
cancella. Il side-load del libro si sposta in Fase 5 (la milestone
richiede solo il sample incluso). [MICHELE] rifinitura strings.xml.

### Sessione — feedback dal primo test sul device + manifest fuso con v1

L'app GIRA SUL RAZR (primi test di Michele: "inizia a piacermi").
Feedback applicati:

- **Creazione**: lupo/lupa si sceglie TOCCANDO i ritratti
  `class_warrior_male/female` di v1 (circolari, bordo ORO sul
  selezionato); armi iniziali portate a TUTTE e 9 le canoniche; nuova
  opzione "Arti marziali — nessuna arma" (esclusiva con l'arma,
  equippedWeapon null: con WEAPONSKILL+UNARMED vale il +2 a mani nude
  già gestito dall'engine).
- **Equipaggiamento iniziale completo** (richiesta Michele: in v1
  c'erano elmo/cotta/mappa e i pasti): scelta di UN oggetto speciale
  come v1/canone (Mappa, Elmo +2 RES, Gilet di maglia +4 RES — lo
  scudo in v1 era solo un tipo enum senza oggetto) + comuni automatici
  (Pozione Curativa HEAL:4 non-combat come il canone Laumspur, DUE
  Pasti) + Corone dal tiro. NOVITÀ ENGINE: effetto dichiarativo
  `ENDURANCE:n` — il bonus Resistenza degli oggetti posseduti è
  CALCOLATO (`effectiveMaxEndurance`), applicato alla corrente
  all'acquisizione e riclampato alla perdita; tutti i clamp di
  cura/healing e i display usano il massimo effettivo (in v1 il bonus
  era sommato a mano nel ViewModel). La creazione costruisce l'eroe
  facendo passare gli oggetti da `Inventory.addItem`: la stessa strada
  di ogni addItem futuro del libro. Icone v1 per elmo/armatura/mappa
  (ic_map_icon 4,1 MB: candidata WebP con ic_axe).
- **Rifiniture da feedback**: griglia armi con le icone di v1 (mancano
  dagger/short_sword/warhammer -> segnaposto, coda Fase 7; ic_axe e
  ic_map_icon da 3-4 MB candidate WebP); specializzazione Scherma a
  MENU A TENDINA, card SEMPRE visibile (disabilitata con spiegazione
  senza la disciplina — prima appariva solo con Scherma scelta e
  sembrava un bug); etichetta "Arti marziali".

**PROSSIMA SESSIONE**: prova di milestone Fase 3 sul Razr (partita
completa, chiusura a metà e ripresa, morte IRON che cancella,
checkpoint piazzato e ricaricato) -> se passa, FASE 3 CHIUSA a diario
e CLAUDE.md -> Fase 4 (inference). In coda anche: [MICHELE] rifinitura
strings.xml; scudo come oggetto se Michele decide il bonus.

### Sessione — chiusura buchi Fase 3: ciclo di vita, dado, regole fuori da :app

Check di stato: push allineato, suite verde, device non collegato (la
prova di milestone a mano resta in attesa). Lavoro fatto nel frattempo:

- **`CicloVitaPartitaTest`** (`:core:engine`): la milestone Fase 3
  AUTOMATIZZATA per la parte che non richiede occhi — gioca con
  auto-save sullo store vero, "chiude e riapre" ricaricando SOLO da
  disco (verifica scena e diario ripresi), piazza un checkpoint e ne
  verifica l'immutabilità, cancella l'avventura come farebbe la morte
  in IRON, e controlla che non restino file `.tmp` (atomicità). De-
  rischia la prova manuale sul Razr, che resta comunque da fare.
- **BUCO FUNZIONALE CHIUSO — il Dado fuori dal combattimento**: le
  scelte con `minRoll`/`maxRoll` (tabelle dei numeri casuali dei libri)
  erano SILENZIOSAMENTE NASCOSTE — il sample non ne usa, ma un libro
  vero sarebbe stato ingiocabile. Ora la zona scelte diventa la zona
  del Dado: "il destino decide" -> tira -> mostra il numero -> continua
  verso la porta del suo intervallo. Flusso a due fasi ereditato da v1
  ma col trigger STRUTTURALE (v1 fiutava la stringa italiana "Tabella
  dei Numeri Casuali" nel testo!) e il DiceRoller iniettato. v0.1 è un
  bottone: l'overlay animato resta Fase 7.
- **`ChoiceAvailability`** (`:core:engine/choice`): il gating delle
  scelte (requiredItem/requiredFlag), le scelte-disciplina possedute e
  la tabella dei tiri erano REGOLE scritte dentro `AdventureState`,
  cioè in `:app`, dove non sono testabili — contro il vincolo
  "l'engine è testabile da terminale". Spostate nell'engine con 7 test;
  `AdventureState` ora delega e resta orchestrazione pura.
- **Tre nei corretti in `AdventureState`**: `isEnding` confrontava
  `sceneType.name` con la stringa "ENDING" invece dell'enum;
  `requiredFlag` considerava soddisfatto anche un flag posto
  esplicitamente a "false" (ora negato, con test); un tipo scritto col
  nome pienamente qualificato invece che importato.

### FASE 3 CHIUSA — il libro gira sul Razr senza Gemma

**Prova sul device fatta da Michele**: il flusso gira ("per adesso
sembra andare"). Milestone Fase 3 considerata PASSATA, con riserva
dichiarata: **restano rifiniture da elencare** — Michele le raccoglierà
e le affronteremo come giro di feedback, senza tenere ferma la fase.
Note già in coda da sessioni precedenti: [MICHELE] rifinitura
`strings.xml`; scudo come oggetto (serve il bonus deciso da lui); 3
icone armi mancanti (dagger/short_sword/warhammer) e conversione WebP
di `ic_axe`/`ic_map_icon` (3-4 MB l'uno) in Fase 7; side-load libro e
schermata Opzioni in Fase 5.

CLAUDE.md aggiornato: **fase corrente -> Fase 4 (inference)**.

### Sessione — FASE 4 APERTA: le fondamenta testabili dell'inferenza

Decisione di collocazione (conflitto apparente risolto, non
interpretato): `PIANO-SVILUPPO` fissa 4 moduli e mette l'inference in
`:app`; `ARCHITETTURA` §engine vieta all'engine ogni riferimento
all'inference; il vincolo di progetto chiede testabilità da terminale.
Le tre cose stanno insieme così: il package `inference` vive in
`:app` MA le sue classi pure (PromptBuilder, ResponseParser) non hanno
un solo import Android e sono coperte da **unit test JVM** in
`app/src/test` — che girano da terminale senza device né modello. Solo
il motore LiteRT-LM vero avrà dipendenze Android. `content/` montato
anche come test-resources di `:app`: i parser si verificano contro i
file VERI, non contro copie da tenere allineate.

- **`ResponseParser`** (11 test): separa la prosa dal blocco tag,
  legge `CHOICE|sceneId|progressivo|testo` (split A LIMITE: un pipe
  dentro il testo non rompe niente — il testo è sempre l'ultimo campo),
  `DISCIPLINE|id|testo`, `ENEMY|nome`. Aggancio alle scelte vere prima
  per destinazione, poi **fallback per conteggio**; ogni scelta senza
  riga tiene il testo originale del pacchetto. Coperti i casi brutti:
  risposta vuota, separatore mai scritto, righe monche, sceneId
  inventati, due scelte verso la stessa scena (non ricevono la stessa
  riga). Il gioco non si blocca mai, per test.
- **`PromptBuilder` + `PromptFragments`** (11 test): frammenti letti da
  `content/config.json` con **default hardcoded** per ogni frammento
  (config assente/rotta/monca -> default, nessuna eccezione). Le
  sezioni vuote non si scrivono (un "[THE STORY SO FAR]" seguito dal
  nulla confonde il modello e spreca contesto). Le scelte si consegnano
  NELLA STESSA FORMA che il modello deve restituire, così tradurre è
  meccanico e sceneId/progressivo tornano indietro giusti. Un test
  verifica che nel prompt NON entri il diario (inferenza senza memoria).
- **Due code documentate chiuse in `content/config.json`** (modifica
  chirurgica, 5 righe di diff): frammento `enemyFormatText`
  (`ENEMY|translated enemy name`, aggiunto al prompt SOLO se la scena
  ha un combattimento) e placeholder `{player_gender}` in coda a
  `constraintText` per gli accordi grammaticali italiani.

### Sessione — idea audio narrativo: progettata e RINVIATA, nasce `doc/UPGRADE.md`

Michele propone tag narrativi per suoni/effetti (brusio di taverna,
tuono, una porta che cigola) generati da Gemma. Discussione utile, con
una correzione reciproca:

- Prima analisi (mia): farlo in ETL, costo runtime zero. **Sbagliata a
  metà**: valeva per l'ambiente di scena, non per gli effetti puntuali.
- Contro-argomento di Michele (giusto): il valore dell'LLM è legare
  l'effetto al TONO e al momento — stessa porta, tono horror = cigolio,
  tono comico = risate. E l'inferenza la paghi comunque in latenza.
- Sintesi: sono **due feature distinte**. L'ambience di scena è una
  proprietà della scena -> ETL. Gli effetti inline **solo Gemma può
  farli**, perché la prosa su cui ancorarli non esiste finché non la
  scrive lei.

Decisioni di formato prese: delimitatore `[[sound:id]]` DOPPIO (le
parentesi singole sono già i marcatori di sezione del prompt: `[THE
STORY SO FAR]`); mai il carattere `|` (regola 4 protegge il parser);
vocabolario CHIUSO con silenzio come fallback; nel **diario si salva il
testo già ripulito** (altrimenti i marcatori spuntano nel Racconto e
nell'export Markdown — trovato tracciando il flusso fino a
`JourneyEntry.enrichedText`).

**RINVIATA da Michele** con motivazione sua e corretta: istruire Gemma
su tutto il vocabolario compete con il compito principale e non si può
valutare prima di aver MISURATO il modello sul device (che è appunto la
milestone di questa fase).

Nasce **`doc/UPGRADE.md`**: il posto per le idee rinviate, scritte per
bene invece che perse o infilate di soppiatto in una fase in corso.
Dentro anche le altre voci accumulate, separando ciò che il design
chiuso già PREDISPONE (effetti oggetto oltre HEAL, requiredRank,
MINDSHIELD, slot multipli, compagni, altri regolamenti) dalle FEATURE
NUOVE (scambio a inventario pieno, tono narrativo scelto dall'utente,
mappa logica più ricca, scudo). Aggiunto alla mappa documenti del piano
con la nota "NON schedulate, non implementare".

**Osservazione da chiarire con Michele**: il piano elenca in Fase 4 un
**`TagParser`** (erede di `StringTagParser` v1, regex -> EngineCommand).
In v1 serviva perché le meccaniche arrivavano come TAG DI TESTO dentro
la narrazione. In Ex i `gameMechanics` arrivano già STRUTTURATI dal
JSON del pacchetto (`{command, params}`) e Gemma non genera mai tag: a
runtime quel parser non ha un lavoro. I tag-regex di `config.json`
sembrano semmai materiale per l'ETL (Fase 6), che dovrà convertire i
tag testuali di Kai Chronicles/Aon in comandi strutturati. Da
confermare prima di scrivere codice che nessuno chiama.

## 16/07/2026

### Sessione notturna — chiusura specifica 3 (stato e salvataggio)

**Specifica 3 CHIUSA** (`doc/STATO.md`). Decisioni:

- **`SessionData`** con `currentSceneId` esplicito (in v1 era assente,
  ricostruito per vie traverse dentro `GameCharacter`); `flags`/
  `variables` tipizzati e unificati a livello di sessione — corregge tre
  difetti di v1 insieme: il DM salvato come personaggio (retaggio
  chatbot, in Ex non esiste nella sessione), lo stato spezzato tra
  `HeroDetails` e `SessionData`, e la trappola `Map<String, Any>` di Gson
  (i numeri tornano `Double`). Formato **kotlinx.serialization**, un file
  JSON per pacchetto (`session_<packageId>.json`), dietro porta
  iniettabile nel modulo `data` (stesso pattern di `PackageSource`, test
  su file temporanei).
- **Auto-save a ogni transizione di scena**, dopo `gameMechanics` +
  `globalRules` (stato consistente per costruzione); vale per tutte le
  difficoltà. **Il combattimento è atomico**: non si salva a metà, un
  crash a metà combat riprende dall'ingresso della scena.
- **Difficoltà come meta-regola sul salvataggio** (non inflazione di
  statistiche), scelta a inizio avventura e immutabile: NORMALE 2
  checkpoint, DIFFICILE 1, IRON 0. Checkpoint piazzati dal giocatore dal
  menu, fotografia completa della `SessionData` su file separato,
  **scritti una volta e mai spostabili/sovrascrivibili**, ricaricabili
  illimitatamente (la durezza sta nell'irrevocabilità del piazzamento,
  non nel numero di ricarichi); il ricaricamento tronca il diario al
  punto del checkpoint. Morte in IRON = sessione cancellata, libro da
  capo.
- **Diario-grafo**: ogni voce `{sceneId, enrichedText, transition}` — il
  testo generato da Gemma si salva e non si rigenera mai (costo
  inferenza, non-determinismo, coerenza con `previous_scene_text`); la
  sequenza delle voci è il percorso completo nel grafo (non solo dove sei
  stato, anche per quale porta sei uscito); `visitedScenes` non esiste
  come lista salvata, è derivato dal diario.
- **Inventario canonico**: WEAPON max 2, BACKPACK_ITEM 8 posti,
  SPECIAL_ITEM illimitati, GOLD 50 Corone; oggetto
  `{name, type, quantity, combatUsable, effect}` con solo `HEAL:n`
  implementato in v0.1 (formato dichiarativo estensibile senza cambiare
  schema); limiti fatti rispettare dal motore, oltre soglia l'oggetto non
  entra senza errore. `UNARMED` resta solo specializzazione WEAPONSKILL,
  mai arma vera. `HUNTING` auto-soddisfa `requireAction action="EAT_MEAL"`
  a costo zero (una riga di logica per l'effetto canonico della
  disciplina).
- **Nota di estensibilità**: il sistema è pensato per altri regolamenti
  futuri (CRT dentro `LoneWolfRules` non nel motore, effetti oggetto
  dichiarativi, globalRules generiche, difficoltà esterna alle regole) —
  adattarlo tocca le implementazioni, non i contratti.

**Prossima specifica: 4 (UI)** — eredita code precise già tracciate nelle
sezioni finali di `doc/REGOLE.md` (scelta specializzazione WEAPONSKILL
alla creazione, menu tattico, opzione MINDBLAST disabilitata se nemico
immune) e di `doc/STATO.md` (scelta difficoltà in setup con spiegazione
onesta di IRON, menu checkpoint con budget visibile, schermata
inventario con equip/unequip, schermata diario/rilettura del viaggio).

### Sessione serale — chiusura specifica 2 (regole di gioco)

**Specifica 2 CHIUSA** (`doc/REGOLE.md`, sostituisce la versione con solo
il blocco 1). Blocchi 2-6:

- **`globalRules` nel manifest**: lista di regole condizione ->
  destinazione (`type`: FLAG | VAR, operatori ==/!=/>=/<=/>/<), valutate
  a ogni transizione DOPO l'esecuzione dei `gameMechanics` della scena di
  arrivo. Ordine di valutazione: morte built-in (Resistenza ≤ 0 ->
  `deathSceneId`) prima di tutto, poi le `globalRules` in ordine di
  scrittura, prima regola che matcha vince. `victorySceneId` come campo
  dedicato eliminato: la vittoria è una `globalRule` come le altre, non
  un caso speciale.
- **Gradi Kai puramente cosmetici**: enum con soglie nell'engine, nomi in
  `strings.xml`, nessun effetto meccanico (predisposizione concettuale
  per un futuro `requiredRank`).
- **MINDBLAST**: +2 Combattività per tutto il combattimento, attivabile
  una volta dal menu tattico; il nemico può essere
  `immuneToMindblast`. **WEAPONSKILL**: specializzazione scelta alla
  creazione del personaggio, un tipo d'arma oppure `UNARMED` (bonus a
  mani nude); il check "arma impugnata" arriva con la specifica 3.
  **HEALING**: passiva, +1 Resistenza a ogni transizione verso una scena
  senza combattimento, fino al massimo del personaggio. **Oggetti**:
  flag `combatUsable` + effetto dichiarato, visibili solo nel menu
  tattico in modalità completa.
- **Comandi TO_IMPLEMENT chiusi**: `removeItem` tollerante (rimuove
  quel che c'è, mai errore se manca quantità); `checkItemAndJump`
  valutato come `ifStat` nell'ordine dei `gameMechanics`;
  `rollOnItemTable` a intervalli espliciti di tiro 0-9 (validati per
  copertura completa e assenza di sovrapposizioni).
- **Dado del Destino fuori combattimento**: criterio narrativo — se il
  tiro decide il destino tira il giocatore (`skillCheck`,
  `randomChoiceTable`, teatro visibile); se decide una quantità tira il
  motore in silenzio (`randomQuantity`, `rollOnItemTable`).

**Emendamento §1.5** (nemico nella scena): l'authoring resta minimale
(nome + due statistiche + destinazioni nel JSON), ma a RUNTIME il motore
idrata un **`Character` unico** per tutti i ruoli (`role: HERO |
COMPANION | ENEMY | NPC`, enum al posto dell'id magico `"hero"` di v1),
erede di `GameCharacter`+`CharacterType` di v1 ripulito dai campi
Android/presentazione e dalle feature morte. Premio della simmetria: la
funzione di round diventa `resolveRound(a: Character, b: Character)` —
nemici con MINDBLAST proprio o duelli eroe-contro-eroe non costano
codice aggiuntivo al motore.

**Input già decisi per la specifica 3** (stato/salvataggio): si
serializzano i FATTI (stats base, inventario, `equippedWeapon`,
`weaponSkillType`, `StatModifier` narrativi con sourceType/duration), i
bonus (CS effettiva) si CALCOLANO con una sola funzione dell'engine, mai
persistiti. Base di riuso: `HeroDetails`/`ComputedStats` di v1; difetto
da non ripetere: in v1 `LoneWolfRules` sommava i modificatori per conto
proprio invece di passare da `ComputedStats`, calcolo duplicato.

**Nota per la specifica 5 (ETL)**: il secondo blocco storico del
progetto v1 fu l'estrazione dei libri con un modello poco capace.
Piano nuovo: parsing deterministico dell'XML Project Aon per la
struttura (scene, collegamenti, sezioni) + LLM usato solo per
classificare le meccaniche nei tag del config, con i validatori come
rete di sicurezza finale (non come sostituto del parsing strutturale).

### Sessione serale — apertura specifica 2 (regole di gioco)

**Aperta specifica 2** (`doc/REGOLE.md`). **Blocco combattimento CHIUSO:**
- Due modalità a scelta del giocatore a inizio combattimento: rapido (il
  motore itera i round fino all'esito, un solo riepilogo) e completo
  (round per round, menu tattico continua/oggetto/disciplina/fuga).
  Evasione, oggetti e discipline esistono solo nel completo.
- Evasione con costo canonico Lupo Solitario (un ultimo round in cui solo
  il giocatore subisce danni) e sblocco `evadeAfterRound` (fuga disponibile
  solo dopo N round, default 0 = subito); fuga via disciplina (es.
  CAMOUFLAGE) gratuita, offerta come scelta di scena prima del combattimento.
- Priorità degli esiti: `loseSceneId` di scena batte `deathSceneId`
  globale (il globale è il fallback, non il default); `winSceneId`
  obbligatorio.
- Nemico minimale nel JSON di scena: nome + `enemyCombatSkill` +
  `enemyEndurance` + destinazioni (`winSceneId`/`loseSceneId`/
  `evadeSceneId`/`evadeAfterRound`) — niente `GameCharacter` completo
  come in v1.
- `enemyName` tradotto da Gemma via nuova riga pipe `ENEMY|testo`, stessa
  filosofia di fallback di CHOICE/DISCIPLINE (parsing fallito non blocca
  mai il gioco).

**Scoperta da v1** (`doc/MATERIALE-REGOLE-V1.md`): l'intera orchestrazione
del combattimento in `MainViewModel` (loop round, `CombatState`, testi
vittoria/sconfitta) è COMMENTATA, mai attiva in v1 — il combattimento non
ha mai girato in produzione. Riuso reale limitato a due pezzi vivi: la
tabella CRT (`LoneWolfRules.COMBAT_RESULTS_CHART`, con la trappola
off-by-one sul tiro 0) e l'interfaccia `GameRulesEngine`
(`canUseDiscipline`, `getKaiRank`). Il `CombatManager` di Ex nasce da
zero, senza debito di compatibilità con un'orchestrazione mai testata.

**Codice da generare** (tracciato in `doc/REGOLE.md` §1.6, non ancora
fatto): tag `enemy_line` nel config (`^ENEMY\|(.+)$` ->
`updateEnemyName`), riga `ENEMY` aggiunta a `outputFormatText`, blocco
`combat` nella scena 4 (battle) di `scenes.sample.json`, regola
validatore che rende `winSceneId` obbligatorio quando `combat` è presente.

### Sessione lampo — inventario asset v1

**Inventario asset v1 completato** (`doc/INVENTARIO-ASSET.md`): scansionato
`app/src/main/res/drawable*` + `app/src/main/assets/` del repo
`ImmundaNoctis-master` al commit `8b705b8` (branch `master`). Numeri
chiave:
- 18 file, 20,47 MB totali (19,84 MB in `res/drawable/`, 0,63 MB in
  `assets/`).
- Nessuna icona di gioco, nessuno sfondo ambiente, nessun `portrait_elara`/
  `lupo_solitario.png`: questo commit è molto più magro del branch
  `develop` fotografato in `ANALISI-RIUSO-V1.md` il 14/07 (43 MB in
  drawable) — quegli asset sono stati aggiunti su `develop` dopo il merge
  in `master` del 30/06/2025. Da tenere presente per un secondo giro di
  inventario su `develop` se servirà.
- Classificazione: 3 file RIUSABILI (ritratti eroe m/f, ritratto DM,
  boilerplate launcher icon), 8 file LEGATI A FEATURE MORTA (ritratti
  classi D&D sage/thief/warrior/witch — in Lupo Solitario non c'è
  selezione di classe, sostituita dalle Discipline Kai), 4 DA RIFARE
  (cleric/mage generici, map_dungeon non pertinente, scenes.json/
  config.json vecchio schema già superati).
- Nessun candidato v1 per i `backgroundImage` richiesti dal sample
  (inn/city/alley/battle/warehouse): da produrre ex novo.
- Stima conversione WebP: ~79% di risparmio sui 14 JPEG (da 19,84 MB a
  ~4,2 MB), in linea con la stima già fatta il 14/07 sul set più ampio.

### Sessione (secondo task)

**Fatto — audit e revisione `content/config.json`:**
- Trovato che il commit `39030c8` (base di partenza fornita per l'audit)
  aveva sovrascritto l'intero file e perso la sezione `start_adventure_prompt`
  scritta e chiusa il 15/07 (commit `2d3f830`); recuperata da lì e reinserita.
- 2 tag duplicati rimossi (`victory_text_translation`, `defeat_text_translation`
  comparivano due volte identici).
- `victory_text_translation`/`defeat_text_translation` eliminati del tutto
  (anche la prima occorrenza): in Ex gli esiti sono scene ENDING con prosa
  propria; l'esito globale è regola motore + `deathSceneId`/`victorySceneId`
  nel manifest, non tag di traduzione.
- `narrative_choice_translation` -> `choice_line`, regex convertita al
  formato pipe (`^CHOICE\|([^|]+)\|([^|]+)\|(.+)$`), parametro rinominato
  `translatedText` (il formato pipe è neutro rispetto alla lingua).
- `discipline_choice_translation` -> `discipline_line`, stesso trattamento
  (`^DISCIPLINE\|([^|]+)\|(.+)$`); risolve anche il bug di case della
  vecchia regex XML (`<DISCIPLINE_IT ... </discipline_it>`).
- `end_guff_tag`: `replacement` da oggetto `{italian, english}` a stringa
  singola `""` (decisione lingua singola).
- 3 tag orfani (comando assente in v1, meccanica voluta) mantenuti e
  marcati `"status": "TO_IMPLEMENT"`: `remove_item_tag` (removeItem),
  `if_item_choice_tag` (checkItemAndJump), `random_item_table_tag`
  (rollOnItemTable).
- Confermati invariati i comandi vivi in v1: `add_item_tag`,
  `require_action_tag`, `remove_all_tag`, `heal_tag`, `set_flag_tag`,
  `random_quantity_tag`, `stat_mod_tag`, `if_stat_tag`,
  `random_choice_table_tag`, `skill_check_tag`, `conditional_action_tag`,
  `set_global_var_tag`, `update_global_var_tag`.

**Conferma di design:** i tag `gameMechanic` sono scelte dell'autore del
libro, parsati SOLO dai dati del pacchetto (mai generati da Gemma);
dall'output di Gemma si estraggono solo le righe `CHOICE|`/`DISCIPLINE|`.

**Input per la specifica 2 (regole):** comandi da implementare in Ex:
`removeItem`, `checkItemAndJump`, `rollOnItemTable`, regola globale
Resistenza<=0 -> `deathSceneId`.

**Nota schema manifest:** aggiungere `deathSceneId` e `victorySceneId`
(opzionali) — da riportare in `doc/SCHEMA-PACCHETTO.md` quando si farà.

### Sessione (primo task)

**Fatto:**
- Esame `GameRulesEngine` + `LoneWolfRules` + `GameLogicManager` di v1.
- SPECIFICA 1 CHIUSA: `doc/ARCHITETTURA.md` (moduli data/engine/inference/ui).

**Decisioni:**
- Tiro del dado iniettato nell'engine (interfaccia `DiceRoller`): il Dado
  del Destino UI e i test deterministici usano la stessa porta.
- `GameState` nel modulo engine come unica fonte di verità; il salvataggio
  è una fotografia dello stato (mai stato sparso tra manager come in v1).
- Single-activity + DI leggera (`AppContainer`) al posto dei singleton.
  Verificato sui numeri: il multi-activity di v1 non ha prodotto file
  piccoli (`ModelActivity` 824 righe, `AdventureActivity` 683,
  `CharacterSheetActivity` 569) e ha costretto a introdurre il singleton.
  La leggibilità viene dal package per schermata, non dal numero di
  Activity.
- Guardrail UI: file di navigazione solo routing (~100 righe max), un
  package per schermata senza import incrociati, nessun ViewModel
  condiviso tra schermate.
- Interfacce solo dove esiste più di un'implementazione reale: le
  quattro motivate sono `RulesEngine`, `InferenceEngine`, `DiceRoller`,
  `PackageSource`. Tutto il resto classi concrete.
- Gradi Kai come enum/id nell'engine, nomi localizzati in UI.
- Risultato del round di combattimento = dati puri; il testo lo compone
  chi lo mostra.
- La Tabella dei Risultati di Combattimento di `LoneWolfRules` si riusa
  integralmente.
- `GameLogicManager` di v1 riclassificato: è un repository di scene, in
  Ex diventa `PackageRepository` nel modulo data con `PackageSource`
  iniettato (niente Context).

**Nota aggiuntiva:**
- Verificato nel codice: `victory_text`/`defeat_text` di v1 erano campi di
  `CombatState` (esito del singolo combattimento), non condizioni globali.
  La condizione di esito globale (vittoria/sconfitta dell'avventura da
  qualsiasi scena, su stat/flag/variabili) è un concetto NUOVO di Ex, da
  progettare nella specifica 2 con `deathSceneId`/`victorySceneId` nel
  manifest e regole globali valutate dall'engine a ogni transizione.

**Prossime sessioni (piano specifiche, in ordine):**
- Specifica 2: regole di gioco (combat, Dado del Destino, gradi Kai,
  Resistenza/Combattività, modificatori, esiti globali
  vittoria/sconfitta) — dentro il modulo engine.
- Poi: stato e salvataggio (3), UI (4), ETL (5), criticità (6).

## 14/07/2026

### Sessione serale

**Fatto:**
- Analisi di `LlamaCppEngine.kt` di v1: architettura chat conversazionale,
  non adatta al nuovo modello di narrazione. Si riusa solo il token tracking
  a soglie e l'idea di interfaccia `InferenceEngine`.
- Analisi di `config.json` di v1: sistema `promptDescription` (tag dichiarativi
  con prompt come dati), riuso integrale deciso.
- README allineato alle decisioni (commit `6a702a0`).
- Creata `content/`: `config.json` spostato lì; `scenes.json` (Project Aon)
  reso locale, non versionato via `.gitignore`.
- Creato `content/scenes.sample.json`: riferimento strutturale pubblico dello
  schema E futuro libro incluso nell'APK — da espandere a mini-avventura
  completa (~10-20 scene) con nomenclatura originale prima del rilascio.

**Decisioni** (vedi changelog README §15 per il dettaglio):
Prosa finita mantenuta nei pacchetti al posto dei canovacci; Gemma arricchisce
e raccorda scena precedente/attuale/continuazioni con prompt stateless senza
chat history; riuso integrale del `config.json` di v1 (sistema
`promptDescription`), con i tag D&D da sostituire con le Discipline Kai.

**Fatto (seconda sessione serale):**
- Struttura scena definitiva chiusa in `content/scenes.sample.json`: eliminata
  la doppia lingua (`narrativeText`/`choiceText` ora stringhe inglesi
  semplici), aggiunto manifest in radice (id, version, title, description,
  language, genre, toneHints di libro), `toneHints` per-scena, campo
  `backgroundImage`, campi predisposti per-scelta (`minRoll`, `maxRoll`,
  `requiredItem`, `requiredFlag`) e `gameMechanics` per-scena.
- Discipline ricondotte alle sole 10 canoniche UPPER_SNAKE (WEAPONSKILL,
  CAMOUFLAGE, HUNTING, SIXTH_SENSE, TRACKING, HEALING, MINDSHIELD,
  MINDBLAST, ANIMAL_KINSHIP, MIND_OVER_MATTER); `SHADOWSTEP` rimosso.
- Grafo di esempio riscritto: 7 scene chiuse ("The Warehouse Letter"),
  START -> ... -> due ENDING (vittoria/morte), con un bivio a disciplina
  (SIXTH_SENSE/CAMOUFLAGE) che evita il combattimento e un ramo di
  combattimento (WIN/LOSE) sulla struttura combat già presente nel file.

**Decisioni chiuse (sessione serale):**
- `narrativeText` e `choiceText`: stringhe semplici, lingua singola (inglese).
- `language` nel manifest del pacchetto; `userLanguage` a runtime (da Android)
  passato a Gemma per la lingua di output.
- Campi predisposti (`minRoll`, `maxRoll`, `requiredItem`, `requiredFlag`)
  restano nello schema anche se vuoti.
- `toneHints` obbligatorio per scena, con un default a livello di manifest.
- `backgroundImage`: nuovo campo per asset statici per ambiente (inn/city/
  alley/battle/warehouse), fallback su placeholder; distinto da `sceneType`,
  che resta START/TRANSITION/ENDING.
- ID scena numerici (corrispondenza con le pagine dei libri-game).
- ID disciplina in UPPER_SNAKE, solo le 10 canoniche di `GameData.kt`;
  `SHADOWSTEP` rimosso.
- Grafo sample: 1→2→3→(4|5)→6, 7=morte.

**Altro (sessione serale):**
- Cancellato `content/scenes.json` locale (schema vecchio, disallineato dal
  sample): verrà rigenerato dal task ETL libro 1 con lo schema nuovo. La riga
  in `.gitignore` resta invariata.
- Decisione UI registrata: il libro completo si carica da file
  nell'applicazione (side-load con picker); l'APK include solo
  `scenes.sample.json`. Da dettagliare in fase UI.

**Prossimi task** (sostituiscono i precedenti):
1. [MICHELE] Bozza dell'estensione di `start_adventure_prompt` in
   `config.json`: i nuovi frammenti (`previousSceneText`, `continuationsText`,
   `constraintText`) — testo dei prompt, niente codice
2. Copia di `config.json` da v1 a Ex + sostituzione tag D&D con Discipline Kai
   (delegabile a Claude Code dopo il task 1)
3. Script ottimizzazione immagini v1 (invariato, delegabile quando si vuole)
4. Creare in `content/` una serie di file di test per meccanica (es.
   `test_choices.json`, `test_skillcheck.json`, `test_combat.json`,
   `test_disciplines.json`, `test_mechanics.json`), scene minime 2-3 l'uno,
   sul modello dei `test_*.json` di v1 — ora possibile: la struttura scena
   definitiva è chiusa (`content/scenes.sample.json`), nascono già nel
   formato giusto
5. Analisi codice morto v1: scansione di tutti i file `.kt` del repo v1
   (`ImmundaNoctis-master`), mappa dei riferimenti incrociati (chi importa
   cosa, chi usa quali simboli), output in `doc/ANALISI-CODICE-MORTO.md`
   con tre categorie: morto certo (zero riferimenti), sospetto
   (referenziato solo da codice morto o solo da test), vivo ma legato a
   feature abbandonate (es. generazione immagini dinamica, doppia lingua).
   Nota: `SkillData.kt` già identificato come morto da Michele. Da fare
   nella stessa sessione di delega dell'inventario asset v1.

### Sessione mattutina 15/07

**Fatto:**
- Esame classi v1 completato: flusso del prompt ricostruito in 4 tappe
  (dettagli in `doc/ANALISI-FLUSSO-PROMPT-V1.md`).
- Censimento 56 file `.kt`: 4 morti certi (`SkillData`, `AdventureDialogs`,
  `tools/Merger`, `tools/ValidationScript`), ~13 legati a feature abbandonate
  (`stdf/*`, `LlamaCppEngine`/chat libera, `TranslationEngine`, doppia
  lingua), nucleo riusabile identificato (`StringTagParser` quasi
  integrale, `InferenceEngine`, pattern `GemmaEngine`).
- Bug trovato in v1: parsing di `tagsPart` duplicato in `MainViewModel`
  (~righe 1426-1444), comandi eseguiti due volte.
- TASK 2 CHIUSO: frammenti prompt scritti e inseriti in `config.json`.

**Decisioni:**
- `buildGemmaPromptForScene` di v1 (testato con Gemma 3) è il riferimento;
  rivisto per Ex con: lingue parametriche (`source_language` dal manifest,
  `user_language` da Android), tono da `toneHints` scena + default
  manifest, contesto = `narrativeText` scena precedente, continuazioni =
  `narrativeText` scene successive, arricchimento non generazione.
- Chiamata singola confermata (narrativa + scelte + discipline in una
  inferenza, velocità sul Razr).
- Formato output a righe con pipe (`CHOICE|...|...|testo`) al posto dei tag
  XML: in v1 Gemma sbagliava i tag e il parser si inceppava. Il testo è
  sempre l'ultimo campo: il motore splitta con limit, un pipe nel testo
  non rompe il parsing.
- Fallback per conteggio: se dal parsing tornano meno scelte di quelle
  inviate, per le mancanti si usa il `choiceText` originale del pacchetto.
  Il parsing che fallisce non blocca MAI il gioco.
- Prompt in inglese (l'italiano era comunque testato e funzionante su
  Gemma 3).
- Discorsi dei personaggi tra apici singoli, mai doppi apici.
- `challengeLevel` eliminato dallo schema Ex: verificato nel codice,
  nessuna logica di motore dietro (una sola riga di colore nel prompt);
  il ruolo lo coprono i `toneHints`.
- Output reali di Gemma da salvare come fixture di test durante lo
  sviluppo.

**Prossimi task** (aggiunti ai delegabili):
- Validatore config parser (`config.json`): regex compilano, placeholder
  coerenti coi gruppi, tag id univoci, actor validi — in v1 le regex rotte
  fallivano silenziosamente.
- Suite test per parser e validatori con fixture di output Gemma reali.
- (già a diario) validatore pacchetti scene, inventario asset v1,
  analisi codice morto v1 — quest'ultima ora ha la base in
  `doc/ANALISI-FLUSSO-PROMPT-V1.md`.

**Decisione di metodo:**
Lo sviluppo è l'ULTIMA fase. Prima si definiscono tutte le specifiche e si
analizzano le criticità; solo a specifiche complete si passa al codice.
Principi architetturali vincolanti per Ex:
- separazione netta tra responsabilità di UI e di logica;
- file più piccoli possibile, moduli con responsabilità singola;
- anti-modello dichiarato: il `MainViewModel` di v1 (1.634 righe, fa tutto:
  motori, prompt, parsing, comandi, combat, salvataggi, traduzione). Il bug
  del parsing duplicato è conseguenza diretta di quella dimensione.

**Piano specifiche** (sostituisce i punti di sviluppo della roadmap, che
slittano a valle; la specifica narrazione è GIÀ CHIUSA con struttura scena
+ frammenti prompt + formato output + fallback):
1. Architettura moduli (`doc/ARCHITETTURA.md`) — strati: data (modelli +
   caricamento pacchetti, zero Android), engine (regole, stato, combat —
   zero Android e zero UI, testabile da terminale), inference (dietro
   interfaccia `InferenceEngine`: prompt builder, parser, fallback), ui
   (Compose, solo presentazione), ViewModel piccoli, uno per schermata,
   soli punti di raccordo. Definire i contratti tra moduli.
2. Specifica regole di gioco — combat, Dado del Destino, gradi Kai,
   Resistenza/Combattività, modificatori. Base: esame di
   `GameRulesEngine` + `LoneWolfRules` + `GameLogicManager` di v1.
3. Specifica stato e salvataggio — contenuto sessione, quando si salva,
   formato. Base: `GameStateManager`/`SessionData` di v1.
4. Specifica UI — schermate, scena teatrale con `backgroundImage`,
   pulsanti scelta, semaforo token. Solo il cosa, non il come.
5. Specifica ETL — conversione libro -> formato pacchetto.
6. Analisi criticità (trasversale) — prestazioni inferenza su Razr 70
   Ultra, limiti contesto Gemma, memoria, tempi caricamento modello,
   assenza modello, degradazioni.

**Prossima sessione di design:**
Architettura moduli (punto 1), partendo dall'esame di `GameRulesEngine`,
`LoneWolfRules` e `GameLogicManager` di v1 per mappare come v1 mischiava le
responsabilità. Output atteso: `doc/ARCHITETTURA.md`.