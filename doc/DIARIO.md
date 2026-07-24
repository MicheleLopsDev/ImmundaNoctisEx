# Diario di progetto

> **SE STAI APRENDO UNA SESSIONE NUOVA, LEGGI SOLO IL BLOCCO QUI SOTTO.**
> Il resto del file è la storia cronologica (dal più recente al più
> vecchio): serve per capire *perché* una decisione è stata presa, non
> per sapere cosa fare adesso.

---

## STATO CORRENTE — aggiornato 21/07/2026

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

- **ANIMAZIONE del narratore che pensa** (20/07, FATTA): al posto della
  scritta ferma, l'alone d'oro attorno al ritratto del narratore PULSA
  nel banner e tre puntini si accendono in sequenza nel blocco testo.
  Distingue a parole i due momenti — "Il narratore apre il libro…"
  mentre CARICA il modello, "Il narratore scrive…" mentre genera
  (nuovo `AdventureState.isLoadingModel`). Zero asset nuovi: si riusa
  `portrait_dm`. **Non ancora vista sul device** (Razr non collegato).

- **ICONE nella card di stato** (20/07, FATTA): ritratto-lupo tondo col
  bordo d'oro, medaglia dorata del grado Kai, spada/cuore/monete e la
  riga delle discipline possedute. La card è uscita da `AdventureScreen`
  in `StatusCard.kt` (lo schermo era oltre le ~200 righe).
  `kaiRankName` è diventata `internal`. Provata sul device: gira.

- **UN'AVVENTURA DICHIARA SEMPRE COM'È ANDATA** (20/07): `Scene.outcome`
  dichiarato dall'autore, scena di finale **garantita** dal motore anche
  se il pacchetto non ce l'ha (`AdventureEnding`, 10 test JVM), tavole a
  china di Michele per vittoria e sconfitta. Chiusi tre modi in cui il
  gioco poteva restare fermo o schiantarsi. Dettaglio sotto.
  **Mai visto girare sul device.**

- **MILESTONE DI FASE 4 RAGGIUNTA** (20/07, misure reali dal Razr):
  **primo token 1,43-1,88 s su GPU, sotto la soglia di 3 s** di
  CRITICITA.md. Caricamento del modello ~9,0 s. Dettaglio e tabella
  nella voce del 20/07.

- **IL BUG DELL'ACCUMULO NON ESISTE** (misurato 20/07 su 15 generazioni,
  3 partite di fila): la velocità reale del Razr è **12,1 token/s**, e i
  18,5 delle prime due generazioni sono il BOOST iniziale del SoC, non
  una velocità che si perde. Dettaglio sotto. **Ipotesi mia smentita dai
  dati.**

- **QUATTRO FIX SERALI SUI SALVATAGGI** (20/07 sera, dettaglio sotto):
  la UI non rifletteva `GameState` (era un `var` non osservabile),
  mancava ogni riscontro/uscita, l'icona Home era invisibile per un
  titolo lungo, i checkpoint di test bloccavano i piazzamenti nuovi
  in silenzio. **"Il bug sembra risolto" — confermato da Michele su
  device solo per l'ultimo dei quattro**; gli altri tre non ancora
  rivisti dopo il fix successivo.

**CONFERMATA da Michele su device, ENTRAMBE LE METÀ**: il testo con i
puntini e l'alone dorato pulsante nel banner. Nonostante il blocco di
2,1s nei log — declassato, non è più un problema prioritario.
L'animazione del narratore è CHIUSA.

**Musica e TTS discussi, ENTRAMBI RINVIATI** (Michele 20/07): siamo
ancora in Fase 4, non si anticipa. Per il TTS il design è già completo
(`UI.md`) e v1 ha i pezzi pronti da riusare quasi invariati
(`TtsService.kt`, `TtsPreferences.kt` — `ANALISI-RIUSO-V1.md`); manca
solo un `UtteranceProgressListener` che v1 non aveva. Per la musica,
Michele stesso aveva posto la condizione "prima misura Gemma sul
device" (`UPGRADE.md §1`): primo token e velocità sono misurati, **manca
ancora termico e batteria** — Michele ha scelto di aspettare quei due
prima di riconsiderarla.

- **IL DIARIO DI COMBATTIMENTO** (20/07, richiesta di Michele con foto
  del registro cartaceo ufficiale): il combattimento COMPLETO ha ora
  un pannello che resta aperto per l'intero scontro — RES/CS di
  entrambi coi modificatori, Rapporto di Forza, dado a 10 facce
  (faccia zero = simbolo del lupo) che gira e si ferma sul tiro vero.
  **Anticipa di proposito un pezzo di Fase 7** (l'overlay animato del
  Dado del Destino), solo per questo caso — scelta esplicita di
  Michele, annotata come tale in `UI.md`. Nuovi file
  `CombatDiaryPanel.kt` e `TenSidedDie.kt` (quest'ultimo pensato
  riusabile per il resto dei tiri quando arriverà Fase 7).
  **Mai visto girare sul device.**

- **LA SCHEDA SPIEGA I NUMERI, NON SOLO IL TOTALE** (20/07, richiesta
  di Michele — mockup mostrato e approvato prima di scrivere codice):
  Combattività e Resistenza nella tab Equipaggiamento ora mostrano
  Base + ogni modificatore riga per riga (es. "+2 WEAPONSKILL
  (Spada)"), non solo il numero finale. `weaponskillBonus` è passata
  da `private` a pubblica in `EffectiveStats.kt` apposta: la UI legge
  la stessa funzione che calcola il numero, non lo ricalcola. Gli slot
  arma (**max 2, invariato**) mostrano "Impugnata · +2" quando la
  specializzazione coincide. Nuovi file `EquipmentTab.kt` e
  `InventoryCards.kt` (Zaino/Oggetti spostati lì, invariati, solo per
  restare sotto la soglia delle 200 righe). **Mai vista sul device.**

- **ESPERIMENTO: GEMMA SCEGLIE LO SFONDO** (20/07, "vediamo se può
  funzionare"): quando la scena non ha un `backgroundImage` VALIDO
  dichiarato, il prompt offre le 21 location del catalogo
  (`SceneImageCatalog`, unica fonte di verità condivisa tra
  prompt/parser/UI) e Gemma può scrivere `IMAGE|nome` — stesso formato
  pipe di CHOICE/DISCIPLINE/ENEMY, NON XML (Michele l'ha chiesto
  esplicitamente: è il motivo per cui il formato pipe esiste, v1 andava
  in stallo sui tag XML sbagliati). Vocabolario CHIUSO: un nome
  inventato è scartato dal parser in silenzio; l'autore vince sempre se
  ha dichiarato uno sfondo che esiste davvero.

  **BUG trovato da Michele lo stesso giorno, giocando**: la condizione
  era solo `!= null`, non "esiste nel catalogo". Il sample dichiara
  `backgroundImage` su TUTTE le 7 scene con i vecchi placeholder mai
  risolti ("inn", "city", "alley"...) — quindi il tag non veniva MAI
  chiesto a Gemma. L'esperimento era morto sul nascere, e nulla nei log
  o nei test lo segnalava: i test di ieri usavano solo nomi validi
  ("loc_market") o `null`, mai un placeholder come quelli VERI del
  sample. Corretto in `PromptBuilder` e `ResponseParser`: si controlla
  `SceneImageCatalog.isValid`, non la sola presenza. 2 test nuovi per
  file, con `"inn"` — il valore reale del sample, non uno inventato.

  **21/07: dizionario descrittivo**, richiesta di Michele — "spiegando
  ogni scena a cosa può corrispondere". Prima Gemma aveva solo i 21 nomi
  nudi: `loc_black_gate` e `loc_helgedad_gate` sono due portali di
  pietra quasi identici, indistinguibili dal solo nome del file.
  `SceneImageCatalog` ora porta una descrizione per voce, **scritta
  guardando le 21 immagini vere** (non a memoria del nome — un
  dizionario sbagliato confonde più di nessun dizionario), e il prompt
  mostra "nome: descrizione" riga per riga. **Costo**: ~740 token
  stimati per il dizionario completo, si somma OGGI a ogni scena del
  sample (nessuna ha ancora un `backgroundImage` valido). 1 test in
  più che verifica esplicitamente la presenza della descrizione, non
  solo del nome.

  **21/07 (stesso giorno): vincolo stringente**, richiesta di Michele —
  "deve essere fatto stringente, non deve inventarne di nuovi". Il
  parser scartava già in silenzio un id inventato, ma un'istruzione
  debole spreca comunque la scelta di Gemma su qualcosa che verrebbe
  buttato via. Il testo ora dice esplicitamente "CLOSED dictionary",
  "MUST NOT invent", "EXACTLY as written — do not modify, abbreviate,
  translate or combine it". Costo del blocco IMAGE: da ~740 a **~830
  token stimati**. 1 test dedicato sul vincolo.

  6 test sul parser, 5 sul prompt builder in totale (oggi solo il
  prompt builder è cambiato: descrizione e vincolo vivono nel
  dizionario/frammento che costruisce il prompt, il parser continua a
  validare solo il nome). **Mai visto girare**: è un esperimento, si
  giudica solo giocando e guardando se Gemma sceglie bene, ignora, o
  prova comunque a inventare nonostante l'istruzione.

  Nota a margine: `AdventureState.kt` è a 450 righe, ben oltre la
  soglia dei ~200 — ma il debito è PREGRESSO (438 già prima di oggi),
  non introdotto da questo lavoro (+12 righe). Andrà spezzato, ma non
  in questa sessione: fuori scope per la richiesta di oggi.

- **SIDE-LOAD DEL LIBRO** (20/07, richiesta URGENTE — "devo poter
  caricare vari file, serve per i test"): `PackageSource` prevedeva
  già tre implementazioni nel suo stesso commento (asset, side-load
  SAF, file di test) — solo l'asset esisteva. Ora l'icona in Home apre
  il picker di sistema, valida SUBITO il file (non aspetta che
  Creazione/Avventura lo scoprano), mostra titolo o primo errore.
  `AppContainer.packageRepository` è passato da `val` a `var` per
  poter cambiare libro senza riavviare l'app.

  **Un rischio trovato e chiuso nello stesso giro**: `SetupRoute`
  mostrava TUTTE le sessioni salvate in "Continua", di qualunque
  libro — innocuo finché esisteva un solo libro possibile, ma con più
  libri avrebbe potuto offrire la sessione di un pacchetto diverso da
  quello appena caricato. Filtrata per `packageId` del libro corrente.

  Nuovi file: `HomeRoute.kt` (il pattern Route/Screen esistente,
  `AppNavigation.kt` doveva restare puro routing), `UriPackageSource`
  in `AppContainer.kt`. **Mai visto girare.**

- **11 NUOVE IMMAGINI CATALOGATE** (21/07, richiesta di Michele — "ho
  aggiunto delle nuove immagini per gli animali... rinomina gli altri
  come ti sembra giusto"): `enemy_wolves` rinominato in `beast_wolves`
  (richiesto esplicitamente — i lupi non sono "nemici" in senso
  stretto, sono bestie). Aggiunte, con nome scelto guardando ogni
  immagine vera (non il nome del file caricato da Michele, spesso
  fuorviante — vedi sotto): `beast_stallion` (richiesto esplicitamente),
  `beast_anaconda`, `beast_cat`, `beast_familiar` (gatto nero con
  collare a mezzaluna, il "famiglio" magico — entità diversa dal
  semplice `beast_cat` pur essendo visivamente lo stesso gatto),
  `beast_rats`, `npc_mage` (mago anziano nel suo studio), `npc_battlemage`
  (mago diverso, in azione su una vetta di notte — nome distinto da
  `npc_mage` apposta, stesso archetipo ma contesto opposto),
  `loc_warehouse` (magazzino/dispensa — combacia esattamente con un
  placeholder morto del sample, vedi nota sotto), `loc_mountain_pass`
  (il file si chiamava "alley" ma il contenuto è un cavaliere con
  soldati su un sentiero di montagna verso un castello — NON un vicolo:
  ho scelto il nome dal contenuto, non dal file), `loc_storm_tower`
  (il file si chiamava "lighting force": una torre runica sotto un
  temporale con fulmini — location, non un effetto).

  **Tre file avevano una banda decorativa runica in basso** (rune
  fantasy + simbolo del sole), assente in tutte le altre immagini del
  catalogo (bordo netto, nessuna cornice): `beast_familiar`,
  `loc_storm_tower`, `beast_rats`. Tagliata via (bordo di separazione
  trovato per riga di pixel, non a occhio) per restare coerenti con lo
  stile "senza cornice" già in uso — stesso principio del testo
  italiano ripulito ieri: un'incoerenza visiva nel catalogo confonde
  quanto un errore. Altri tre file (`beast_anaconda`, `npc_battlemage`,
  `loc_warehouse`) hanno solo un fregio celtico agli angoli, molto più
  discreto: lasciato — segnalato qui, non deciso a tavolino.

  **Un file lasciato FUORI dal vocabolario location**: "combat" (due
  spade incrociate su una battaglia campale) — è una tavola simbolica,
  non un luogo in cui una scena si svolge; inserirla tra le location
  avrebbe rischiato che Gemma la scegliesse come sfondo per qualunque
  scena di combattimento. Catalogata come asset (`misc_battle_clash`,
  prima volta che si usa il prefisso `misc_`) ma NON aggiunta a
  `SceneImageCatalog` — è una scelta di giudizio, da rivedere con
  Michele se serve altrove.

  **Nota per Michele, non agita autonomamente**: `loc_warehouse` e
  `loc_mountain_pass` corrispondono per contenuto a due dei placeholder
  morti dichiarati nel sample ("warehouse", "alley" — vedi la voce
  del 20-21/07 sul dizionario). Aggiornare `scenes.sample.json` per
  usarli tocca il contenuto narrativo del libro, non solo asset: non
  l'ho fatto.

  `SceneImageCatalog` passa da 21 a 24 location (+`loc_mountain_pass`,
  `loc_storm_tower`, `loc_warehouse`, con descrizione scritta guardando
  le immagini vere). `SceneImages.kt` aggiornato con i 3 nuovi case.
  Compilazione e suite `PromptBuilderTest`/`SceneImage*` verdi. **Mai
  visto girare sul device**: come per il resto del catalogo, nessun
  codice assegna ancora questi file a scene o personaggi specifici
  (punto 2 di APERTO, sotto — ora sono 52 immagini in attesa, non 41).

- **SAMPLE BONIFICATO** (21/07, richiesta di Michele — "bonifichiamo il
  file json... così possiamo provare la nuova versione"): i 5 vecchi
  placeholder morti di `content/scenes.sample.json` ("inn", "city",
  "alley"×2, "battle"×2) sostituiti guardando il contesto narrativo di
  ogni scena, non a caso. Due avevano un match reale nel catalogo:
  scena 1 (camera sopra una locanda) -> `loc_tavern`, scena 6 (interno
  del vecchio magazzino) -> `loc_warehouse`. Le altre quattro (due
  vicoli, due scene di combattimento generiche) non hanno nulla di
  adatto nel catalogo attuale: il campo è stato tolto invece di
  forzare un abbinamento debole — restano il caso di test per "Gemma
  sceglie" (o omette, se nessuna delle 24 le convince). La copia
  gemella in `core/data/src/jvmTest/resources/scenes.sample.json`
  NON è stata toccata: è già divergente dall'originale (manca il
  campo `outcome`), sembra un fixture di test isolato, non
  sincronizzato di proposito — fuori scope. Compilazione e suite
  `app` verdi. **Mai visto girare.**

- **5 LIBRI DI TEST per l'esperimento IMAGE** (21/07, richiesta di
  Michele — "facciamo dei file scene personalizzati con poche scene
  per provare queste cose"): nuova cartella `content/test-books/`
  (side-load, NON asset dell'APK — CLAUDE.md elenca solo config.json e
  scenes.sample.json in `content/`, questa è un'aggiunta accanto, da
  tenere a mente se si aggiorna quel documento). Un file per scenario,
  2-3 scene ciascuno, `id` distinto per non mischiarsi nella lista
  "Continua" (già filtrata per `packageId`, vedi side-load del 20/07):
  - `test_image_author_wins.json`: `backgroundImage` valido dichiarato
    (`loc_crypt`) — Gemma NON deve mai essere interpellata.
  - `test_image_gemma_picks.json`: nessun `backgroundImage`, testo che
    punta chiaramente a una location del catalogo (locanda affollata)
    — Gemma dovrebbe scegliere `loc_tavern`.
  - `test_image_no_match.json`: nessun `backgroundImage`, ambientazione
    assente dal catalogo (ponte di nave in mare aperto) — Gemma
    dovrebbe OMETTERE il tag, non forzare un id sbagliato.
  - `test_image_dead_placeholder.json`: `backgroundImage` dichiarato
    ma non nel catalogo (`"tavern_old"`, il bug del 20/07) — deve
    contare come non valido, Gemma va comunque interpellata.
  - `test_image_similar_pair.json`: due scene consecutive con portali
    di pietra quasi identici (`loc_black_gate` vs `loc_helgedad_gate`)
    — verifica se la descrizione nel dizionario basta a distinguerli.

  JSON validati contro lo schema (`Manifest`/`Scene`, tutti i campi
  opzionali omessi si affidano ai default già in uso nel sample). **Mai
  caricati sul device**: da provare col side-load di ieri.

- **PRIMO TEST SUL DEVICE, sfondo assente** (21/07): Michele ha provato
  `test_image_gemma_picks.json` (locanda affollata, nessun
  `backgroundImage` — ci si aspettava `loc_tavern`) e lo sfondo mostrato
  era il default (`map_dungeon`), non la taverna. Il log fornito si
  fermava alla riga `MISURA gen=1...`: nessun log esistente mostra il
  blocco TAGS grezzo o l'esito del parsing IMAGE, quindi da fuori è
  impossibile distinguere "Gemma non ha scritto la riga" da "l'ha
  scritta in un formato che il parser scarta" — stesso sintomo, cause
  opposte. Aggiunto un log (`SceneNarrator`, `Log.i` con
  `runCatching` attorno — `android.util.Log` non è mockato nei test JVM
  del modulo, nessun Robolectric per scelta, un log non deve mai far
  fallire un test) che stampa l'esito di `backgroundImage` e l'intero
  blocco tag ricevuto da Gemma: `adb logcat -s SceneNarrator`. Non
  ancora diagnosticato: serve rilanciare il test con questo log per
  vedere cosa Gemma ha scritto davvero. Compilazione e suite `app`
  verdi (4 test rotti dal primo tentativo senza `runCatching`, poi
  risistemati).

  **DIAGNOSTICATO, stesso giorno**: il blocco tag ricevuto era
  `CHOICE|2|1|Ordinare una bevanda e ascoltare i pettegolezzi` — SOLO
  la riga CHOICE, nessuna riga IMAGE. Non è un bug di parsing (la
  CHOICE è letta correttamente, il parser funziona), non è un bug di
  `PromptBuilder` (riverificato: `imageFormatText` viene iniettato per
  questa scena, la condizione è corretta). **Gemma ha semplicemente
  ignorato l'istruzione OPTIONAL**, con soli 120 token generati in
  tutto (si è fermata da sola, non per limite di `maxTokens`=10240).
  Ipotesi per cui l'istruzione viene ignorata (da verificare, non
  ancora testate): è l'ULTIMA istruzione del prompt, preceduta dal
  dizionario di 24 righe che potrebbe "diluirla"; dice esplicitamente
  "OPTIONAL" e un modello 4B potrebbe leggerlo come "posso sempre
  ometterla"; a differenza di CHOICE/DISCIPLINE (che hanno un formato
  già dimostrato nel blocco scelte da tradurre), IMAGE è solo descritta
  a parole, senza un esempio concreto da imitare. Discusso con Michele,
  non ancora deciso quale intervento provare per primo.

  **PRIMO TENTATIVO, stesso giorno**: Michele ha scelto "aggiungere un
  esempio concreto" tra le tre ipotesi. `imageFormatText` ora include
  una riga di esempio col formato esatto, usando un id VERO del
  dizionario (`loc_tavern`) ma con un chiarimento esplicito — "solo la
  sintassi, scegli quello che combacia con QUESTA scena, non
  necessariamente questo" — per non trasformare l'esempio in un
  suggerimento di scelta involontario. Aggiornati **entrambi** i posti:
  `PromptFragments.kt` (DEFAULTS) e `content/config.json` (sono a
  specchio, il secondo vince se presente — un test già esistente
  confronta i due leggendo `content/config.json` dal classpath, niente
  di nuovo da sincronizzare a mano). Costo: +44 token stimati sul
  blocco IMAGE (da ~123 a ~168). Suite `app` verde. **Mai visto
  girare**: prossimo passo di Michele è riprovare
  `test_image_gemma_picks.json` con questa build.

  **LOG DEL PROMPT COMPLETO** (21/07, richiesta di Michele — "voglio
  provare il prompt su Gemma in locale sul PC"): estratta
  `logChunked(tag, label, text)`, funzione di file (non di classe,
  riusabile). Logga sia il prompt intero (nuovo) sia il blocco tag
  ricevuto (già c'era) spezzandoli in pezzi da 3500 caratteri
  numerati `[i/n]` — **logcat tronca in silenzio oltre ~4000
  caratteri per riga**, e il prompt col dizionario delle 24 location
  li supera abbondantemente (senza lo split Michele avrebbe visto un
  prompt incompleto senza saperlo). `adb logcat -s SceneNarrator`.
  Compilazione e suite `app` verdi.

  **CONFERMATO su LM Studio, stesso giorno**: Michele ha provato il
  prompt (copiato dal log) su `gemma-4-E4B-it` GGUF locale, con
  `imageFormatText` RISCRITTO A MANO in modo imperativo — ha
  funzionato, `IMAGE|loc_tavern` scritto correttamente. Conferma
  l'ipotesi 2 di prima: la parola "OPTIONAL" era il problema, non la
  posizione nel prompt né la mancanza di un esempio (quello già
  c'era dal tentativo precedente). **Non adottata la formulazione
  letterale di Michele**: diceva "se nessuno è coerente scegli
  comunque una location" — funziona, ma contraddice il vincolo del
  21/07 mattina ("se nessuna calza, ometti, non indovinare").
  Riscritto `imageFormatText` prendendo solo la lezione (tono
  imperativo, "decidi ORA" invece di "OPTIONAL") mantenendo la
  possibilità di omettere la riga quando nessuna location è un buon
  match. Aggiornati di nuovo entrambi i posti (`PromptFragments.kt`
  DEFAULTS e `content/config.json`). Suite `app` verde. **Ancora da
  provare sul device con questa terza formulazione** — il test su LM
  Studio ha validato "l'imperativo funziona", non ancora "l'imperativo
  CHE OMETTE SE SERVE funziona altrettanto".

  **QUARTA FORMULAZIONE, stesso giorno**: idea di Michele — "così
  evitiamo che sbagli, per lui è più facile prendere sempre una
  decisione, se poi troviamo xxx lo ignoriamo". "Ometti la riga se
  nessuna calza" chiede al modello un giudizio IN PIÙ sopra quello
  vero (quale location?): quanto sono sicuro di non essere sicuro?
  Tolto quel giudizio: la riga si scrive SEMPRE, e quando nessuna
  location è un buon match si scrive `IMAGE|xxx` invece di ometterla
  o di inventare un id plausibile. Nessuna modifica al parser:
  `SceneImageCatalog.isValid` scarta già in silenzio ogni id fuori
  dal catalogo, `xxx` compreso — stesso esito finale dell'omissione
  (`backgroundImage = null`, verificato leggendo di nuovo
  `ResponseParser.parse`), compito più semplice per il modello.
  Nella riscrittura il vincolo "MUST NOT invent" si era indebolito
  per errore in "do not invent" (minuscolo): un test del 21/07
  mattina lo controlla testualmente
  (`ilVincoloSuiNomiEStringente`) ed è servito da rete — riportato a
  "MUST NOT invent", non indebolito il test. Suite `app` verde (44
  test, il rotto corretto). **Mai vista girare né su device né su LM
  Studio**: prossimo test di Michele.

  **CONFERMATO che il meccanismo funziona** (Michele: "funziona"),
  poi richiesti altri libri di test più uno con "una loc non
  definita". 2 nuovi file in `content/test-books/` e uno aggiornato:
  - `test_image_no_match_desert.json`: secondo campione di "nessuna
    location calza" (deserto, soggetto diverso dalla nave di
    `test_image_no_match.json`) — un solo campione non basta a
    fidarsi che `IMAGE|xxx` sia il comportamento reale e non un
    colpo di fortuna, serve triangolare.
  - `test_image_with_combat.json`: combattimento (`combat`) senza
    `backgroundImage`, ambientato in una caverna (`loc_caves`) — mai
    provato insieme a IMAGE, verifica che il blocco tag contenga
    ENTRAMBI `ENEMY|...` e `IMAGE|...` nella stessa risposta
    (`enemyFormatText` e `imageFormatText` si aggiungono entrambi
    quando c'è combat e nessuno sfondo valido, mai testato insieme).
  - `test_image_no_match.json`: descrizione aggiornata (diceva
    "Gemma dovrebbe OMETTERE il tag", ora si aspetta `IMAGE|xxx` —
    il file narrativo non è cambiato, solo cosa ci si aspetta in
    uscita con la quarta formulazione).

  **CONFERMATO su device anche `test_image_author_wins.json`**
  (21/07): `backgroundImage: "loc_crypt"` dichiarato dall'autore,
  Gemma non interpellata sull'immagine (la condizione in
  `PromptBuilder.outputFormat` non aggiunge `imageFormatText` quando
  lo sfondo è già valido) — lo sfondo mostrato è quello giusto.
  Scenari dell'esperimento IMAGE confermati funzionanti sul device o
  su LM Studio: autore esplicito, Gemma sceglie bene (locanda), la
  quarta formulazione (sempre una riga). Restano da provare:
  `test_image_dead_placeholder`, `test_image_similar_pair`,
  `test_image_no_match`/`_desert` (con `xxx` atteso),
  `test_image_with_combat` (ENEMY+IMAGE insieme).
  JSON validati. **Mai caricati sul device**: da provare col
  side-load.

- **BUG: il Diario di Combattimento non si aggiornava** (21/07,
  Michele: "click su MINDBLAST non succede nulla, click sul dado non
  cambia i valori di RES"). Causa: `CombatSession` (`:core:engine`,
  classe pura senza dipendenze Android per vincolo architetturale) ha
  `var player`/`var enemy` interne che mutano DAVVERO ad ogni round —
  `fightRound()`/`activateMindblast()` funzionano, i numeri sono
  giusti in memoria — ma Compose non può saperlo: il riferimento a
  `session` non cambia mai, solo i suoi campi interni. C'era già un
  contatore pensato apposta (`AdventureState.combatTick`, dal lavoro
  di ieri sul Diario) e già letto in `CombatActiveZone`, ma la sola
  LETTURA non bastava a garantire la ricomposizione del blocco che
  mostra i numeri. Corretto avvolgendo `CombatDiaryPanel` +
  `TacticalMenu` in `key(state.combatTick) { ... }`
  ([CombatZone.kt](app/src/main/kotlin/io/github/luposolitario/immundanoctisex/ui/adventure/CombatZone.kt)):
  forza la ricreazione completa del blocco ad ogni azione, invece di
  affidarsi al gruppo di ricomposizione implicito. Compilazione e
  suite `app` verdi — **nessun test automatico copre questo bug**
  (è un problema di ricomposizione Compose, serve un instrumented
  test per intercettarlo, non uno unitario JVM): da confermare sul
  device.

  **CONFERMATO da Michele su device, stesso giorno**: "sembra
  sistemato il bug". Ha allegato un log di ~21 minuti, 18 generazioni
  consecutive (side-load ripetuto del sample), analizzato per intero
  a caccia di ALTRI problemi: nessun crash, nessuna eccezione. Tag
  IMAGE risolto a un valore valido in tutte le 18 generazioni
  (`loc_warehouse`/`loc_tavern`/`loc_market`, mai `null` né `xxx` —
  nessuna scena di questo giro era un vero "no match", quel caso
  resta da vedere con `test_image_no_match`/`_desert`). Due fenomeni
  presenti ma GIÀ NOTI, non nuovi bug: velocità in calo da ~18-19 a
  ~11-13 tok/s (boost iniziale del SoC che si esaurisce, misurato il
  20/07) e memoria nativa in crescita da 1017 a ~1496 MB in 17 cicli
  (il leak già rinviato consapevolmente da Michele). **Nota minore
  nuova**: micro-blocchi UI sparsi da 30-34 frame (~0,5s), più piccoli
  del blocco noto di caricamento modello (~193-198 frame) — probabile
  decodifica delle immagini di sfondo, non un malfunzionamento, da
  tenere d'occhio se la fluidità peggiora.

**RI-PRIORITIZZATO da Michele (21/07 sera)**: "finiamo prima le
implementazioni, le prestazioni e le ottimizzazioni le portiamo dopo
visto che non sono proprio pessime adesso". Cambio esplicito rispetto
all'ordine del 20/07 sotto — segnalato prima di partire perché
contraddiceva una sua decisione di ieri ("TTS rinviato, siamo ancora
in Fase 4, non si anticipa"): confermato consapevolmente, non un
cambio per inerzia. Drain batteria e termico esteso SCENDONO in
priorità (restano da fare, ma dopo); **Preferences (Opzioni) + TTS
SALGONO**, in anticipo su Fase 5 — scelta esplicita di Michele.

- **OPZIONI (schermata 7) + font, IMPLEMENTATE** (21/07 sera): tema
  (già c'era), font di lettura, lingua della narrazione, TTS —
  Michele ha confermato di voler anticipare anche il font, che
  `Theme.kt` dichiarava esplicitamente "fino alla Fase 7".

  **3 nuove preferenze** (`util/`): `FontPreferences` (enum
  `ReadingFont` — solo famiglie di sistema Serif/SansSerif/Monospace/
  Cursive, zero asset da scaricare), `LanguagePreferences` (enum
  `OutputLanguage`, 5 lingue, con `locale` per il TTS oltre al
  `promptValue` inglese per Gemma), `TtsPreferences` (riuso quasi
  invariato di v1, `Gender` enum di Ex al posto della stringa libera
  di v1).

  **`tts/TtsService.kt`**: riuso di v1 con due differenze — `Gender`
  invece di stringa, e un `UtteranceProgressListener` che v1 NON
  aveva (`onSpeakingStarted`/`onSpeakingFinished`, richiesto da
  `UI.md` §Stato del narratore unificato per lo stato SPEAKING).
  I callback sono predisposti ma **inerti**: nessuno li aggancia
  ancora, arriva con la Tappa 2 (integrazione nel flusso scena).

  **UI** (`ui/options/`): `OptionsRoute` + `OptionsScreen` (tema,
  lingua inline; font e TTS in `FontSection.kt`/`TtsSection.kt`
  separati per restare sotto soglia — il file più lungo è
  `OptionsScreen.kt` a 175 righe). Il tema ha una particolarità: vive
  SIA nella preference SIA nello stato di `MainActivity` (nuovo
  `onThemeOverrideChange: (Boolean?) -> Unit`, accanto al vecchio
  `onThemeToggle` del toggle rapido di Home) — deve applicarsi SUBITO
  senza riavviare l'app, la sola preference salvata non basta.
  Collegata da Home: `onSettingsClick` portava già a `Route.OPTIONS`
  (era nell'enum, cadeva sul `PlaceholderScreen` — nessuna modifica a
  `HomeScreen`/`HomeRoute` necessaria).

  **Font e lingua APPLICATI davvero**, non solo salvati: il font
  scelto arriva a `AdventureScreen` (`readingFont: FontFamily`, letto
  da `AdventureRoute`) e si vede nel testo della scena; la lingua
  scelta sostituisce l'`"Italian"` che prima era fisso nel default di
  `SceneNarrator`. Il TTS invece resta SOLO configurabile per ora
  (nessuno lo richiama per leggere davvero) — è la Tappa 2.

  Compilazione pulita al primo tentativo, suite `app` verde. **Mai
  visto girare sul device**: da provare, in particolare il cambio
  tema/font a caldo e l'elenco delle voci TTS disponibili (dipende
  dal motore TTS installato sul Razr, mai verificato quali lingue
  offre davvero).

  **GRANDEZZA DEL TESTO, stesso giorno** (richiesta di Michele: "un
  pulsante con una lente per cambiare la grandezza del font nella
  parte alta dove c'è l'icona della casa"): nuovo `TextScale` in
  `FontPreferences.kt` (piccolo/medio/grande, moltiplicatore sul
  `bodyLarge` di Material, non una dimensione assoluta). Non una
  schermata a parte: un `IconButton` con la lente
  (`Icons.Default.ZoomIn`, già disponibile — il progetto ha
  `material-icons-extended` in dipendenza) nell'header, subito prima
  di Home, che cicla piccolo→medio→grande→piccolo a ogni tocco.
  Stato tenuto dentro `AdventureScreen` (come `showSheet`/
  `showJournal`, non in `AdventureState`): il pulsante che lo cambia
  vive in questa schermata, non serve altrove. Compilazione e suite
  verdi. **Debito di `AdventureScreen.kt` pregresso, non introdotto
  oggi**: era già a 351 righe prima di questa modifica (soglia
  ~200 superata da tempo, segnalato ma non ancora spezzato), le
  modifiche di oggi ne hanno aggiunte 39. **Mai visto girare sul
  device.**

  **BUG: i quattro font erano tutti identici** (21/07, Michele su
  device): la prima versione usava le famiglie generiche di sistema
  (`FontFamily.Serif`/`SansSerif`/`Monospace`/`Cursive`) per zero
  asset — assunzione sbagliata, quelle famiglie NON garantiscono un
  typeface distinto su ogni produttore Android, sul Razr di Michele
  finivano tutte sullo stesso font. Verificato `v1` (`ui/theme/
  Type.kt`): usava solo `FontFamily.Default`, nessuna scelta di font
  — niente da riusare per questo problema specifico.

  **Corretto con 4 font veri**, scaricati da Google Fonts (repo
  ufficiale `github.com/google/fonts`, licenza OFL) con permesso
  esplicito di Michele: Lora (serif, default), Inter (sans serif),
  Roboto Mono (monospace), Caveat (corsivo) — verificati come
  TrueType validi prima di usarli, ~1,7 MB totali in
  `res/font/`. `ReadingFont` ora costruisce `FontFamily(Font(R.font
  .xxx))` invece delle famiglie generiche. `FontSection.kt` mostra
  anche il nome del font sopra l'anteprima (prima solo la forma, non
  il nome — poco utile con nomi generici tipo "Serif", più utile ora
  che sono nomi propri). Compilazione e suite verdi. **Mai visto
  girare sul device**: da riconfermare che ORA siano davvero
  distinguibili.

  **GRASSETTO, stesso giorno** (richiesta di Michele: "aggiungi che
  si può mettere in grassetto"): nuovo `boldText: Boolean` in
  `FontPreferences.kt`, interruttore in più nella stessa card del
  font (non una scelta a sé) — si applica anche all'anteprima in
  `FontSection.kt`, così l'effetto combinato font+grassetto si vede
  subito. A differenza della grandezza (che ha il pulsante lente
  nell'header, cambiabile dentro la scena), il grassetto si sceglie
  SOLO in Opzioni: `AdventureScreen` lo riceve come semplice
  parametro `boldText`, niente stato locale da ciclare. Compilazione
  e suite verdi. **Mai visto girare sul device.**

  **TONO DELLA NARRAZIONE, stesso giorno** (richiesta di Michele:
  "aggiungi l'opzione per il tono" — chiarito con una domanda diretta,
  dato che "Tono (pitch)" esiste già nella sezione TTS: intendeva il
  tono NARRATIVO, non la voce). Prima di oggi il tono lo decideva SOLO
  l'autore (`Scene.toneHints`, fallback su `Manifest.toneHints`) — il
  giocatore non aveva voce in capitolo. Nuovo `NarrativeTonePreferences
  .kt`: enum `NarrativeTone` con `AUTHOR` (default, `hints = null`,
  comportamento invariato) più sei toni che SOSTITUISCONO quelli
  dell'autore per l'intera sessione — Cupo, Avventuroso, Misterioso,
  Eroico, Leggero, Duro e crudo. Deciso di FAR VINCERE il giocatore
  quando sceglie qualcosa (non sommare ai toni dell'autore): un
  "avventuroso" scelto sopra una scena scritta "grim" deve essere
  inequivocabile, non un mix ambiguo.

  `SceneNarrator` prende un nuovo parametro opzionale `toneOverride:
  List<String>? = null` (stesso pattern di `userLanguage`), usato
  al posto di `scene.toneHints.ifEmpty { manifest.toneHints }` quando
  non è null. Nuova `ui/options/ToneSection.kt` (RadioButton, come
  `LanguageSection`). Compilazione e suite verdi. **Mai visto girare
  sul device**: né la scelta in sé, né l'effetto vero su cosa scrive
  Gemma con un tono forzato.

  Michele, subito dopo: "non trovo la modifica" (riferito al tono).
  Codice riverificato: `ToneSection` è chiamata correttamente in
  `OptionsScreen.kt`, tutto committato su `develop` — sospetto più
  probabile: l'APK sul Razr non era stato ricompilato dopo l'ultimo
  commit. Chiesta conferma, non ancora arrivata risposta.

  **DIMENSIONI DEL FONT AUMENTATE, stesso giorno** ("aumenta le
  dimensioni del font"): i tre valori di `TextScale` (il ciclo del
  pulsante lente) partivano troppo vicini alla taglia normale di
  Material per sentirsi un vero cambiamento — 0.85/1/1.2 diventano
  1/1.25/1.5. Compilazione e suite verdi. **Mai visto girare sul
  device.**

  **COLORE D'ACCENTO SELEZIONABILE, stesso giorno**: Michele ha
  mandato lo screenshot della card di stato e chiesto "una selezione
  dei colori per... cambiarla". Chiarito con una domanda diretta
  (il tema generale dell'app o solo il dorato Kai della card?):
  intendeva il **tema generale**. Nuovo `AccentColorPreferences.kt`:
  enum `AccentColor` con 5 preset (Blu/default, Oro, Verde, Corallo,
  Turchese), ciascuno con la coppia `primary`/`onPrimary`/
  `primaryContainer`/`onPrimaryContainer` già bilanciata per
  contrasto sia in scuro sia in chiaro — non un color picker RGB
  libero, una rosa curata come per il font.

  `Theme.kt`: `DarkColorScheme`/`LightColorScheme` (val fissi) sono
  diventati `darkScheme(accent)`/`lightScheme(accent)` (funzioni):
  solo l'accento cambia, il resto della palette (secondary, tertiary,
  error, superfici) resta uguale per ogni scelta. `ImmundaNoctisTheme`
  prende un nuovo parametro `accentColor` CON DEFAULT (`AccentColor
  .BLUE`, il colore di sempre): tutte le decine di `@Preview` sparse
  nel progetto continuano a compilare senza toccarle.

  Stesso pattern del tema chiaro/scuro: lo stato vive in
  `MainActivity` (nuovo `onAccentColorChange`), non solo nella
  preference, perché deve applicarsi SUBITO senza riavviare l'app.
  Nuova `ui/options/AccentColorSection.kt`: swatch cliccabili col
  colore VERO (non un elenco di nomi) — si sceglie guardando. Il
  colore mostrato negli swatch segue il tema di sistema attivo,
  scuro o chiaro, per restare leggibile sul relativo sfondo.

  Compilazione pulita al primo tentativo, suite verde. **Mai visto
  girare sul device**: in particolare, non ho modo di giudicare da
  qui se i 5 preset hanno davvero un buon contrasto — sono stime
  ragionevoli, non misurate.

  **SFONDO DELLA CARD DI STATO, stesso giorno**: Michele ha chiesto
  "un altro picker per la barra di sotto" — chiarito con una domanda
  diretta (navigation bar di sistema o un elemento dell'app?), poi con
  lo STESSO screenshot della card di stato già mandato prima: intendeva
  proprio quella card, non la barra di sistema. Nuovo
  `StatusCardColorPreferences.kt`: enum `StatusCardColor`, `DEFAULT`
  (background/content entrambi null, la card resta quella di sempre)
  più 5 pastelli chiari (Lavanda, Azzurro, Menta, Ambra, Rosa) con un
  `content` (colore testo/icone) ESPLICITO e scuro abbinato a
  ciascuno — necessario perché con tema scuro attivo il testo di
  default sarebbe chiaro, illeggibile su uno sfondo pastello chiaro.

  `StatusCard.kt` prende un nuovo parametro `cardColor`, applicato via
  `CardDefaults.cardColors(containerColor, contentColor)` solo se
  entrambi i valori del preset non sono null. **Estratta
  `ColorSwatch` in un file condiviso** (`ui/options/ColorSwatch.kt`):
  era identica, copiata di netto, tra `AccentColorSection` e questa
  nuova sezione — stesso pattern chiesto due volte nello stesso
  giorno, ha senso condividerlo invece di tenere due copie.

  Compilazione e suite verdi. `OptionsScreen.kt` è a 197 righe, vicino
  alla soglia dei ~200 — da tenere d'occhio se arriva un altro picker.
  **Mai visto girare sul device.**

- **ZAINO: scritte lunghe e scarto oggetti** (21/07, Michele dallo
  screenshot della scheda: "alcune scritte sono troppo lunghe e manca
  la possibilità di scartare una cosa tenendo premuto"). Due problemi
  distinti nello stesso giro:
  - **Testo lungo**: `BackpackCard`/`WeaponSlot` non avevano
    `maxLines`/`overflow` — un nome come "Laumspur Potion" andava a
    capo spezzando la PAROLA a metà nello slot quadrato. Aggiunto
    `maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign =
    Center` su entrambi.
  - **Scarto con tocco lungo**: l'engine aveva già `Inventory
    .removeItem` (segnalato nel diario da giorni, mancava solo il
    gancio UI). Nuovo `AdventureState.discardItem(itemName)` (stesso
    pattern di `consumeItem`, -1 unità, autosave). `BackpackCard` ora
    usa `Modifier.combinedClickable(onClick, onLongClick)` al posto
    del semplice `onClick` della Card — il tocco lungo apre un
    `AlertDialog` di conferma prima di scartare davvero (un long-press
    accidentale non deve far sparire un oggetto senza che il
    giocatore possa fermarsi). Filo passato per tre file
    (`EquipmentTab` → `CharacterSheetScreen` → `AdventureScreen`, fino
    a `state::discardItem`).

  Nuovo libro di test `content/test-books/test_items_and_weapons.json`
  (richiesta di Michele, per verificare il meccanismo di scelta armi):
  una scena con **3 armi** (solo 2 slot esistono, `Inventory
  .MAX_WEAPONS` — la terza non deve entrare, verifica anche il
  limite), pasti, una pozione curativa, un oggetto speciale col nome
  VOLUTAMENTE lungo ("Tarnished Ring of the Old Kingdom", per
  verificare anche il fix del testo troncato) e delle Corone.

  Compilazione e suite verdi. **Mai visto girare sul device**: né il
  fix del testo, né lo scarto, né se le 3 armi si comportano come
  atteso (2 entrano, 1 no) — tutto da provare col side-load.

  **BUG REALE DEL MOTORE trovato dal test, stesso giorno**: Michele
  ha provato `test_items_and_weapons.json` — "non mi ha aggiunto
  nulla all'inventario". Non era il file di test: **nessun libro,
  mai, poteva dare oggetti (o applicare regole) tramite
  `gameMechanics` sulla propria scena START**. Causa: `TransitionEngine
  .transitionTo` è l'UNICO punto che esegue `gameMechanics`/HEALING
  passivo/morte built-in/globalRules (REGOLE.md §2.3) — ma
  `CreationState.buildSession` costruiva la `SessionData` con
  `currentSceneId = startSceneId` DIRETTAMENTE, senza mai passare da
  lì. La primissima scena della partita nasceva "dentro" se stessa,
  saltando l'unica pipeline che avrebbe eseguito i suoi comandi.

  Corretto in `buildSession`: dopo aver costruito la sessione grezza,
  gira `TransitionEngine(manifest, MechanicsExecutor(dice))
  .transitionTo(gameState, startSceneId)` UNA VOLTA, alla nascita
  della sessione — non in `AdventureState` (che gira anche alla
  ripresa di un checkpoint già esistente, dove ri-eseguire i
  `gameMechanics` darebbe gli oggetti una seconda volta). Se la
  scena START ha una `globalRule` che scatta subito, ora può anche
  saltare altrove alla creazione — comportamento nuovo ma coerente:
  è la stessa pipeline di ogni altra transizione, non doveva essere
  un caso speciale.

  Nessun test esistente copriva `buildSession` (l'assenza stessa ha
  lasciato il bug invisibile finché un libro non ha davvero provato a
  usare la scena START in questo modo). Compilazione e suite di
  `:app` e `:core:engine` verdi. **Mai visto girare sul device**: da
  riprovare con lo stesso `test_items_and_weapons.json`.

- **PICK ESPLICITO DEGLI OGGETTI, stesso giorno** (Michele, dopo aver
  visto lo scarto silenzioso: "il pick deve sempre necessariamente
  essere di una singola cosa per volta, addItem non può funzionare in
  maniera silenziosa"). Nuovo concetto di gameplay, non discusso
  prima in `REGOLE.md`: quando una scena offre più oggetti di quanti
  se ne possano prendere (es. 3 armi, 2 soli slot), la scelta di
  quale prendere dev'essere del giocatore — mai un cap automatico che
  scarta in silenzio in base all'ordine di scrittura nel JSON.

  **Nuovo comando `offerItem`** (accanto ad `addItem`, che resta
  invariato per gli oggetti che l'autore vuole dare senza ambiguità):
  a differenza di `addItem`, `MechanicsExecutor` non lo esegue
  all'arrivo in scena — il comando non è nel suo `when`, cade nel
  ramo di default (nessun effetto), blindato da un test dedicato.
  Resta "sul banco" finché il giocatore non lo sceglie esplicitamente.

  `Inventory.canAdd(character, item): Boolean` (nuovo, `:core:engine`):
  sapere PRIMA se c'è spazio, per disabilitare il pulsante "Prendi"
  con un motivo esplicito ("Hai già 2 armi", "Zaino pieno", "Borsa
  piena") — mai un tocco che silenziosamente non fa nulla. Riusata
  anche da `addCapped` per togliere una piccola duplicazione.

  `ItemOffers.offeredItems(scene): List<GameItem>` (nuovo, pubblico,
  `core.engine.inventory`): estrae gli `offerItem` di una scena senza
  eseguirli — stessa logica di parsing di `ItemMechanics.addItem`,
  factorizzata in `Params.kt` (`itemType`/`weaponType` erano private
  dentro `ItemMechanics`, spostate `internal` top-level per essere
  riusabili nello stesso modulo da un package diverso).

  `AdventureState`: `availableItems` (gli offerti non ancora presi),
  `canPickItem`, `pickItem` — un oggetto alla volta, mai automatico.
  "Già preso" tracciato con un flag di sessione (`picked_item_<scena>
  _<nome>`, sopravvive a checkpoint/autosave) MA lo stato che guida la
  UI è un `Set<String>` Compose-osservabile separato, ricalcolato ad
  ogni cambio scena — i flag di `GameState` da soli non bastano a far
  ricomporre (stesso problema già risolto ieri per `CombatSession`/
  `combatTick`).

  Nuova `ui/adventure/PickupZone.kt`: una card "Puoi prendere" con un
  pulsante per oggetto, mostrata sopra le scelte normali quando la
  scena ne ha. Riscritto `test_items_and_weapons.json` per usare
  `offerItem` su tutti gli oggetti (non solo le armi): il principio
  vale in generale, non solo quando lo spazio è il problema.

  6 test nuovi (`InventoryTest`: `canAdd` nei 4 casi di capacità;
  `ItemOffersTest`, nuovo file: parsing, `addItem` che non conta come
  offerta, le 3 armi che restano tutte disponibili, e il test che
  blinda "`MechanicsExecutor` non deve mai eseguire `offerItem` da
  solo"). Compilazione e suite verdi. **`AdventureState.kt` è salito
  a 497 righe** (era già a 450, debito pregresso segnalato ieri, non
  ancora spezzato — continua a crescere). **Mai visto girare sul
  device.**

  **CONFERMATO da Michele su device (22/07)**: "le modifiche
  all'interfaccia, la scelta delle armi e lo scarto degli oggetti"
  funzionano. Copre: la schermata Opzioni intera (tema, colore
  d'accento, font veri, grassetto, tono narrativo, dimensioni testo,
  sfondo della card di stato), il pick esplicito degli oggetti
  (`offerItem`/`PickupZone`, coi limiti 2 armi/8 zaino/50 corone), lo
  scarto col tocco lungo. **Conferma indiretta anche il fix del bug
  sulla scena START**: senza quello non ci sarebbe stato nulla da
  scegliere o scartare — le 3 armi/oggetti del test non sarebbero mai
  arrivati in inventario. Prima buona giornata di verifiche sul
  device per tutto il lavoro del 21/07.

  **SEGNALATO da Michele per il futuro validatore**: "quando
  implementiamo il software per scrivere/validare il JSON dobbiamo
  tenere presente tutte queste regole". Verificato: i validatori
  esistenti (`:core:data/validation/`, Fase 2 CHIUSA — `GraphValidator`,
  `CombatValidator`, `DisciplineValidator`, `GameMechanicValidator`...)
  **non controllano NESSUNO dei limiti di inventario**.
  `GameMechanicValidator` oggi verifica solo la copertura dei tiri di
  `rollOnItemTable`, nient'altro. Regole da coprire quando si estende
  (qui, o nel `:tool` ETL di Fase 6, che ha già "validatori condivisi"
  nel piano):
  - Più `addItem`/`offerItem` di tipo `WEAPON` sulla stessa scena (o
    sommati a quelle già possedute) oltre `Inventory.MAX_WEAPONS` (2):
    con `addItem` è un probabile errore dell'autore (l'eccedenza si
    scarta in silenzio) — con `offerItem` è voluto, ma solo se ce ne
    sono ALMENO 2 offerte per lasciare una scelta vera.
  - Idem per `BACKPACK_ITEM`/`GOLD` oltre `MAX_BACKPACK_SLOTS` (8) /
    `MAX_GOLD` (50).
  - Nessun limite per `SPECIAL_ITEM` (giusto non validarlo).
  - Un `addItem` che offre PIÙ ARMI di quante ne stiano dovrebbe
    probabilmente essere un WARNING ("forse volevi `offerItem`?"), non
    un errore bloccante — coerente con "il gioco non si blocca mai".

- **BUG SEGNALATO: "il tono narrativo cambiandolo non succede nulla"**
  (21/07, Michele dal device, dopo aver confermato che il resto della
  schermata Opzioni funziona). Causa plausibile individuata in
  `AdventureRoute.kt`: `remember(manifest)` costruiva `SceneNarrator`
  UNA VOLTA sola — `manifest` non cambia mai durante la sessione, quindi
  se le Opzioni cambiavano lingua/tono senza uno smontaggio completo
  della route, il narratore restava quello vecchio. Fix: `userLanguage`
  e `toneOverride` sono ora chiavi esplicite del `remember`. Compila,
  suite verde. **NON CONFERMATO come causa reale** — richiesta a Michele
  una verifica con `adb logcat -s SceneNarrator` per vedere se il tono
  arriva davvero al prompt ora, oppure se Gemma lo riceve e lo ignora
  (stesso pattern già visto col tag IMAGE e la dicitura "OPTIONAL").
  Rimasta in sospeso: Michele ha preferito la nuova funzione sotto.

- **MODELLI PERSONALIZZATI DA LINK HUGGING FACE** (21/07, richiesta di
  Michele — "gli do il link huggingface e lui mi scarica il modello che
  voglio, fermo restando che i preferiti sono quelli che abbiamo
  deciso"): nuova sezione "Aggiungi un modello" nella schermata Modelli,
  per provare altri modelli LiteRT-LM (Qwen, Phi-4-mini, DeepSeek-R1-
  Distill, …) oltre ai tre del catalogo fisso, che restano invariati.
  - `DownloadableModel` è ora `@Serializable` e ha un campo `custom`
    (`false` per il catalogo, `true` per quelli aggiunti da Michele) —
    solo per distinguerli nella UI (pulsante "Rimuovi dalla lista"),
    non cambia il download, che era già generico.
  - `ModelPreferences.customModels`: lista salvata come JSON nelle
    SharedPreferences (`addCustomModel`/`removeCustomModel`, dedup per
    id). Stesso pattern delle altre `*Preferences`, niente di nuovo.
  - Dal link incollato si ricava `fileName` (ultimo pezzo del percorso,
    come fa Hugging Face per il download diretto) e un `id` slug; la
    `sizeBytes` resta 0 (ignota) finché il download non la scopre da
    sé — stesso trattamento già in uso per `GEMMA_3N_E4B_GATED`. Uno
    switch "repository riservato" nel form imposta `requiresToken`
    (usa lo stesso campo Token già presente più sotto nella schermata).
  - "Aggiungi e scarica" fa tutto in un gesto: costruisce il modello,
    lo salva, lo seleziona, avvia subito il download (stesso worker
    riprendibile di sempre). "Rimuovi dalla lista" cancella anche il
    file scaricato, non solo la voce.

  **Estensione, stesso giorno** (Michele: "il modello si può scaricare
  da huggingface oppure caricare da SD, però dobbiamo chiarire che il
  modello deve essere di tipo litert-lm"):
  - **Import da file locale**: pulsante "Scegli file .litertlm" nella
    stessa card, apre il selettore di sistema (`ActivityResultContracts
    .OpenDocument`). `LiteRtLmEngine.load` vuole un `java.io.File` vero
    (`modelFile.absolutePath`), non un `Uri` — verificato nel codice
    prima di scegliere l'approccio: un `content://` non basta, il file
    va COPIATO per intero in `modelsDirectory()`. Copia su
    `Dispatchers.IO` (sono GB, mai sul thread di UI), con lo stato
    "Importazione…" a schermo.
  - **Validazione bloccante sull'estensione**: `.litertlm` è l'unico
    formato che `LiteRtLmEngine` sa caricare (`.task` di MediaPipe,
    ancora nel catalogo per `GEMMA_3N_E4B_GATED`, NON è intercambiabile
    — vedi "Fatti tecnici" più sotto). Sia il link sia il file scelto
    da SD passano da `validateLitertlm()`: se l'estensione non torna,
    errore bloccante a schermo, niente download/copia. Meglio un
    rifiuto immediato e chiaro che un caricamento silenziosamente
    fallito sul device.
  - Compilazione verificata (`:app:compileDebugKotlin`), **mai provato
    sul device**: manca ancora la prova con un modello vero diverso da
    Gemma (link o file, download/copia completa, caricamento reale in
    `LiteRtLmEngine`) e la prova pratica del selettore file su Android
    (permessi, provider di file del Razr).

  **BUG SUL DEVICE: crash aggiungendo un modello** (22/07, dal log di
  Michele): `kotlinx.serialization.SerializationException: Serializer
  for class 'DownloadableModel' is not found`, in
  `ModelPreferences.setCustomModels` (`Json.encodeToString`). Causa:
  `:app/build.gradle.kts` ha solo la libreria runtime
  (`kotlinx.serialization.json`), non il **plugin del compilatore**
  (`alias(libs.plugins.kotlin.serialization)`) che genera il
  `serializer()` per le classi `@Serializable` — `:core:data` ce l'ha,
  `:app` non ne aveva mai avuto bisogno finché `DownloadableModel` non
  è diventato `@Serializable` oggi stesso. La classe compila lo stesso
  (l'annotazione esiste nella libreria runtime), ma a runtime il
  serializer non esiste: crash silenzioso solo al primo uso. Fix: una
  riga nel blocco `plugins {}` di `app/build.gradle.kts`. Ricompilato e
  suite riverificata verde. **Non ancora riprovato sul device da
  Michele.**

  **BUG SUL DEVICE: "dice già scaricato" senza aver scaricato nulla**
  (22/07, dal log di Michele): il worker parte e chiude con SUCCESS in
  ~450ms — impossibile per un file da GB. Causa in due punti, entrambi
  legati a `sizeBytes=0` (dimensione ignota, la norma per OGNI modello
  personalizzato, non l'eccezione com'era per `GEMMA_3N_E4B_GATED`):
  - `ModelDownloadWorker`: la verifica di integrità finale
    (`totalSize > 0L && partFile.length() != totalSize`) non scatta MAI
    con dimensione ignota — un file troncato, una pagina d'errore, o
    (sospetto più probabile: Michele ha incollato il link alla PAGINA
    del repo invece che al file diretto "…resolve/main/…") una
    paginetta HTML da poche decine di KB passavano per "download
    riuscito" senza alcun controllo.
  - `ModelPreferences.isDownloaded()`: con `sizeBytes<=0` considera
    scaricato qualsiasi file esista a quel percorso, qualunque sia la
    sua dimensione reale.
  Fix, tre parti:
  1. Nel worker, controllo del `Content-Type` della risposta: se è
     `text/html`, fallisce subito con messaggio esplicito ("il link
     punta a una pagina web, non al file — serve …resolve/main/…").
  2. Nel worker, rete di sicurezza quando la dimensione resta ignota
     anche a valle: sotto `MIN_PLAUSIBLE_MODEL_BYTES` (10 MB — nessun
     modello LLM reale è più piccolo) il download fallisce invece di
     essere promosso.
  3. In `ModelsRoute`, a download riuscito la `sizeBytes` del modello
     personalizzato viene fissata alla dimensione REALE del file
     scaricato (stesso trattamento già in uso per l'import da file
     locale): i controlli di integrità successivi diventano
     significativi, non sempre veri per costruzione.
  Compilazione e suite riverificate verdi. **CONFERMATO da Michele**:
  la causa era proprio il link — quello alla pagina del repo invece che
  al file diretto. Col link giusto
  (`…/resolve/main/nomefile.litertlm?download=true`) il download del
  modello personalizzato (Gemma 4 E4B abliterato, 3,66 GB) è riuscito.

  **BUG SUBITO DOPO: "scarica il modello personalizzato ma non lo usa"**
  (22/07, Michele): il download funzionava, il modello risultava
  selezionato in UI (card evidenziata), ma la partita continuava a
  usare Gemma 4 E4B ufficiale. Causa in `ModelPreferences.selectedModel`
  ([ModelPreferences.kt](../app/src/main/kotlin/io/github/luposolitario/immundanoctisex/model/ModelPreferences.kt)):
  cercava SOLO in `ModelCatalog.byId()` (il catalogo fisso) — un
  modello personalizzato non ci sta mai dentro, quindi tornava `null` e
  si ricadeva SILENZIOSAMENTE su `ModelCatalog.default`, qualunque cosa
  l'utente avesse scelto. `AppContainer.ensureModelLoaded()` legge
  proprio `selectedModel` per decidere quale file caricare in
  `LiteRtLmEngine` — da lì il modello sbagliato arrivava fino alla
  partita. Fix: cerca prima in `customModels`, poi nel catalogo fisso,
  poi il default. Compilazione e suite riverificate verdi. **Non ancora
  riprovato sul device.**

  **SEGNALATO da Michele (22/07), RINVIATO ESPLICITAMENTE** — "serve un
  tasto per far load/unload del modello se ne scarichi più di uno, ma
  lo facciamo dopo, per adesso scarico solo un modello per volta": oggi
  il cambio modello richiede riavviare l'app (o rientrare in
  un'avventura) perché `LiteRtLmEngine` carica il modello una volta e
  lo tiene; con più modelli scaricati in parallelo servirebbe un modo
  per scaricare quello attivo e caricarne un altro senza uscire. Non
  implementare finché Michele non lo richiede: per ora tiene un solo
  modello scaricato per volta, il flusso attuale gli basta.

- **RITRATTO DEL NEMICO IN COMBATTIMENTO** (22/07, punto 2 della lista
  aperta — "agganciare le 52 immagini"): verificato che le sole
  LOCATION (24 `loc_*`) erano già agganciate (`SceneImageCatalog`, tag
  `IMAGE`, banner scena, esperimento del 21/07) — mancavano le altre 28
  (`enemy_*`, `beast_*`, `npc_*`, `hero_*`, `misc_*`), quelle "di chi/
  cosa c'è in scena" già discusse e rimandate. Due decisioni prese con
  Michele prima di scrivere codice:
  - **L'ID lo dichiara l'autore nel JSON**, non un tag Gemma: stesso
    pattern di `backgroundImage`, zero costo nel prompt. Risponde da
    solo al dubbio già sollevato ("Gemma regge con più vocabolari?") —
    con questo approccio non si pone nemmeno.
  - **Il ritratto va accanto al nome nemico** in `CombatEntryZone`
    (`CombatZone.kt`), il posto dove oggi c'era solo testo.
  Fatto: `Combat.enemyImage: String?` (nuovo campo, null = nessun
  ritratto, comportamento invariato), `EnemyImageCatalog` (14 ID:
  `enemy_*`+`beast_*`, verificato — sono avversari di combattimento
  indipendentemente dal nome proprio o meno) con test, `enemyImageRes()`
  in `ui/adventure/EnemyImages.kt`, ritratto circolare 48dp mostrato in
  `CombatEntryZone` quando dichiarato.
  **Aggancio NPC completato subito dopo** (stesso giorno, Michele:
  "npc o beast se sono amichevoli vanno sotto il testo nella parte di
  storia"): `Scene.npcImage` si mostra ora sotto la card del testo
  narrato in `AdventureScreen.kt`, prima della `StatusCard` — un
  incontro pacifico nella narrazione, non un avversario (quello resta
  in `CombatEntryZone`). `NpcImageCatalog` è salito a 20 ID: alle 14 di
  npc/hero/misc si sono aggiunte le 6 `beast_*`, PRESENTI ANCHE in
  `EnemyImageCatalog` — la stessa immagine (un lupo, un cavallo) può
  essere un nemico in una scena e un incontro pacifico in un'altra: è
  l'autore a scegliere il campo giusto (`Combat.enemyImage` se ostile,
  `Scene.npcImage` se no), non l'immagine a deciderlo da sola.
  Compilazione e suite riverificate verdi su `:core:data`, `:core:engine`,
  `:app`. **Mai visto girare sul device.**

  **BUG SUL DEVICE: le immagini non si vedono** (22/07, Michele con
  screenshot + log): provato con `test_image_enemy_npc.json`, scena 1
  (stallion, `npcImage`) — il banner mostra la location scelta da Gemma
  (`loc_forest_prey`, corretto, comportamento preesistente), ma nessuna
  immagine tra il testo e la `StatusCard`. Verificata l'intera catena
  riga per riga senza trovare il difetto: deserializzazione JSON (test
  JVM diretto, conferma `npcImage=beast_stallion` letto bene),
  `PackageRepository`/`PackageValidator` (non trasformano il manifest),
  `sceneById` (legge diretto da `manifest.scenes`), posizione del
  codice in `AdventureScreen.kt` (corretta, tra la card di testo e
  `StatusCard`), `NpcImageCatalog`/`npcImageRes` (mapping presente e
  corretto per `beast_stallion`). **Nessuna eccezione nel log.**
  Aggiunto un `Log.d("AdventureState", ...)` DIAGNOSTICO TEMPORANEO in
  `AdventureState.kt` (`logSceneImages`, chiamato a ogni cambio scena
  e all'avvio) che stampa `npcImage`/`combat.enemyImage` della scena
  corrente: il prossimo log dirà con certezza se il dato arriva null
  (bug a monte, nei dati — es. file vecchio caricato sul device) o
  arriva valorizzato (bug nel rendering Compose, non ancora trovato).
  Da togliere una volta chiarita la causa. Compilazione e suite
  riverificate verdi.

  **CAUSA REALE: build non aggiornata, non un bug** — Michele aveva
  ricopiato l'ultimo JSON ma non aveva installato l'ultima build con
  tutto il codice del giorno; ricompilando per il log diagnostico ha
  installato la build giusta, e le immagini sono comparse subito.
  Confermato su device: cavallo (scena 1) e viandante (scena 2)
  mostrati sotto il testo, entrambi giudicati "molto belli". Rimosso
  il log diagnostico (`logSceneImages`), non serviva più.

  **RIFINITURA IMMEDIATA** (stesso test, Michele: "nel combat non mi
  piace molto perché l'immagine è troppo piccola e si perde il senso"):
  il ritratto tondo 48dp accanto al nome nemico in `CombatEntryZone`
  non reggeva il confronto con l'illustrazione grande di `npcImage`.
  Uniformato allo stesso trattamento: illustrazione a piena larghezza,
  100dp, angoli arrotondati, sopra il nome invece che di fianco in
  miniatura. Compilazione e suite riverificate verdi.

  **Altezza ritoccata due volte di seguito** (stesso giorno, Michele
  soddisfatto del risultato: "sono molto belle, un 10% in più" e poi
  "un altro 10% e ci siamo"): 100dp -> 110dp -> 120dp, stessa misura
  per `npcImage` (`AdventureScreen.kt`) ed `enemyImage`
  (`CombatEntryZone`). Compilazione riverificata verde a ogni passo.

  **Il ritratto nemico spariva a combattimento iniziato** (stesso
  giorno, Michele: "quando combatte puoi lasciare sotto l'immagine del
  nemico?"): lo mostrava solo `CombatEntryZone` (prima di scegliere
  Rapido/Completo) — appena partiva lo scontro, `CombatActiveZone`
  prendeva il suo posto e l'immagine spariva. Estratta in un
  `EnemyPortrait` condiviso, ora resta visibile per tutta la durata
  dello scontro, in cima al Diario di Combattimento. Compilazione e
  suite riverificate verdi.

- **MUSICA DI SOTTOFONDO, SOLO CONFIGURAZIONE** (22/07, richiesta di
  Michele — "ho creato delle musiche mp3, mettiamo in preference la
  selezione del mp3 e il volume?"): promemoria dato PRIMA di
  implementare — il 20/07 la musica era stata rinviata apposta, in
  attesa delle misure di batteria/termico con Gemma attivo (non ancora
  fatte, `UPGRADE.md §1`), perché il dubbio era il carico COMBINATO
  GPU+audio continuo. La richiesta di oggi è scoped alla sola
  configurazione (selezione file + volume), a costo zero — nessuna
  riproduzione parte davvero. Stesso trattamento già dato al TTS prima
  della Tappa 2: si predispone, si collega dopo.
  - `MusicPreferences` (nuova, `util/`): `musicEnabled`,
    `selectedTrackUri` (Uri content:// persistito con
    `takePersistableUriPermission`, NON copiato in storage — a
    differenza dei modelli LiteRT-LM, `MediaPlayer` legge un Uri
    direttamente, non serve un `File` vero), `selectedTrackName` (per
    l'etichetta in UI, l'Uri non è leggibile), `volume` (default 0.5).
  - `MusicSection.kt` (nuova, `ui/options/`): stesso stampo di
    `TtsSection` — switch attiva/spegni, pulsante "Scegli file MP3"
    (selettore di sistema, `ActivityResultContracts.OpenDocument`,
    filtro `audio/*`), slider volume con commit al rilascio.
  - Wiring in `OptionsRoute`/`OptionsScreen`/`AppContainer`.
  Compilazione e suite riverificate verdi. **Riproduzione vera NON
  collegata**: nessun `MediaPlayer` istanziato, nessun audio parte
  durante la partita — resta un passo separato, da riconsiderare
  insieme al TTS (Tappa 2) quando le misure di batteria arriveranno o
  Michele deciderà di procedere comunque. **Mai vista girare sul
  device.**

  **Traccia di default e volume corretti, stesso giorno** (Michele:
  "gli mp3 li trovi qui, puoi selezionare una di queste, ovviamente
  vanno in loop, per default il volume è molto basso al 15%"): le 4
  tracce composte da Michele (`origina_res/`, temi combattimento/
  esplorazione/mercato/romantico) copiate in `assets/music/` — restano
  tutte disponibili, non solo quella scelta. Selezionata "esplorazione"
  come default: è il momento più frequente di gioco, l'unico sottofondo
  che deve reggere ovunque senza essere legato a un contesto specifico.
  `MusicPreferences.effectiveTrackName` mostra quella inclusa finché
  l'utente non ne sceglie una sua da file; `DEFAULT_VOLUME` sceso da
  0.5 a 0.15. **Il loop non è un'opzione**: è già annotato nel codice
  come requisito per quando arriverà il `MediaPlayer` vero
  (`isLooping = true`), non c'è nulla da configurare oggi. Compilazione
  e suite riverificate verdi.

- **SALVATAGGIO VECCHIO NON CANCELLATO AL SIDE-LOAD** (22/07, Michele:
  "se carico un file json delle scene cancella il salvataggio corrente
  se c'è, non ha senso"): `SetupRoute` filtra i salvataggi per
  `packageId`, non per contenuto — se si ricarica (side-load) un file
  con lo stesso `id` di un giro precedente (tipico nei test: si
  modifica il JSON, si ricarica, l'`id` resta uguale), il salvataggio
  vecchio sopravvive e viene offerto come "Continua", su scene che nel
  file appena caricato possono essere cambiate o non esistere più.
  Stesso `SessionStore.deleteAdventure` già usato da
  `CreationRoute.onCreate` per "nuova avventura", chiamato ora anche in
  `HomeRoute.pickBook` subito dopo un caricamento riuscito. Compilazione
  e suite riverificate verdi.

  **Serviva comunque un modo esplicito** (stesso giorno, Michele:
  "così avevo la possibilità di cancellare la sessione, adesso come
  cancello la sessione, mi ci vuole un tasto?"): prima del fix sopra,
  il side-load era un modo INDIRETTO di liberarsi di un salvataggio —
  tolto quello, non restava più nessun modo di eliminarne uno a
  piacere (es. per ricominciare la stessa avventura da capo senza
  toccare il file). Aggiunto un pulsante "Elimina" esplicito su ogni
  `SavedSessionCard` (`AdventureSetupScreen.kt`), con conferma
  (`AlertDialog`, azione irreversibile — stesso trattamento già dato
  all'uscita dalla scena) prima di cancellare per davvero. Compilazione
  e suite riverificate verdi. **Mai vista girare sul device.**

- **TTS TAPPA 2: agganciato nel flusso della scena** (22/07, Michele:
  "prima del push voglio chiudere tutti gli sviluppi base, manca
  l'implementazione del TTS"): `TtsService`/`TtsPreferences` esistevano
  già (configurazione, voci per genere, `UtteranceProgressListener`),
  ma non erano mai collegati a una lettura vera — restava tutto inerte,
  come i due file stessi dichiaravano in commento ("Tappa 2" non fatta).
  Seguito `UI.md` §Stato del narratore unificato e §Flusso centrale,
  non reinventato:
  - `AdventureState`: nuovo `isSpeaking` (terzo valore dello stato
    unificato IDLE/GENERATING/SPEAKING), `readAloud()` che chiama
    `TtsService.speak(narrative, hero.gender, userLocale)`,
    `onSpeakingStarted`/`onSpeakingFinished` collegati a `isSpeaking`
    in un `init`. Auto-lettura solo a `NarrationEvent.Completed` (testo
    finito, non durante lo streaming — leggere un testo che cambia
    sotto la voce non avrebbe senso) quando `autoReadEnabled`.
    `ttsService.stop()` a ogni `moveTo`: la voce della scena lasciata
    non deve continuare sopra quella nuova.
  - `AdventureRoute`: `TtsService` connesso una volta sola per tutta la
    vita della route (a differenza del narratore non serve ricrearlo
    quando cambiano lingua/tono), smontato con `DisposableEffect`.
  - `AdventureScreen`: il cerchio d'oro del banner resta acceso anche
    in SPEAKING, non solo in GENERATING. Icona "leggi" (`VolumeUp`)
    sopra il testo narrato — grigia/disattivata se l'auto-lettura è
    già accesa in Opzioni, come da UI.md.
  Le altre due icone del "blocco del narratore" previste da UI.md
  (copia, toggle originale/tradotto) restano FUORI da questo giro: non
  erano nella richiesta di oggi. Compilazione e suite riverificate
  verdi. **Mai vista girare sul device**, né sentita: verificare che
  la voce parta davvero, che si fermi al cambio scena e che l'icona si
  disabiliti con l'auto-lettura accesa.

- **MUSICA: COMBO invece del picker, con anteprima** (22/07, dal
  device — Michele: "vorrei una combo con le canzoni che ho fatto, non
  un picker, e quando seleziono una combo questa parte per provarla").
  Il file picker di sistema (SAF) per un file esterno è sparito:
  sostituito da un vero menu a tendina sulle 4 tracce incluse
  (`BundledMusicCatalog`, nuovo — `esplorazione`/`combattimento`/
  `mercato`/`romantico`). `MusicPreferences.selectedTrackUri`/
  `selectedTrackName` (pensati per un file esterno) sostituiti da
  `selectedTrackId` + `effectiveTrack`.
  Selezionare una voce dal menu la fa partire SUBITO in un
  `MediaPlayer` di anteprima locale a `OptionsRoute` (letto da
  `assets/` via `AssetFileDescriptor`, `isLooping = true`, volume
  allineato allo slider anche mentre lo si trascina) — vive e muore con
  la schermata Opzioni (`DisposableEffect`), non è la riproduzione
  vera in partita: quella resta un passo separato, non ancora fatto.
  Compilazione e suite riverificate verdi. **Mai vista girare sul
  device.**

- **NOMI ESTESI, TRE VOLUMI, TEST TTS** (22/07, stesso giro, Michele:
  "fai apparire il nome per esteso dei file mp3", poi "serve una parte
  nel menu opzioni dove metti 3 barre volume [...] aggiungi un test
  per il tts"):
  - **Nomi estesi**: `BundledMusicCatalog` ora mostra categoria +
    titolo originale ("Esplorazione — Where the Statues Kneel" invece
    di solo "Esplorazione").
  - **Tre volumi raccolti in una card sola** (`VolumeSection`, nuova):
    voce/TTS (default 75%), musica (default 15%, invariato), generale
    (default 80%) — quest'ultimo MOLTIPLICA gli altri due, non li
    sostituisce. `TtsPreferences.volume` (nuovo) e `AudioPreferences`
    (nuovo file, `generalVolume`) sono preferenze separate: il volume
    non appartiene né al TTS né alla musica in modo esclusivo.
    `TtsService.speak()` applica `ttsVolume * generalVolume` via
    `KEY_PARAM_VOLUME` sull'utterance — non esiste un `setVolume()`
    sul `TextToSpeech` come per rate/pitch. Lo slider Volume è uscito
    da `MusicSection` (ora solo switch + combo tracce); l'anteprima
    della musica in Opzioni riflette anch'essa `musicVolume *
    generalVolume`, così il generale si sente subito anche lì.
  - **Test TTS**: un tasto "Prova" (icona play) accanto a ciascun
    selettore voce (maschile/femminile) legge una frase fissa —
    "Ciao, sono il TTS di Android." se la lingua di output è italiano,
    altrimenti "Hello, I am Android TTS." per qualunque altra lingua
    configurata (non tradotta lingua per lingua: una sola frase inglese
    di riserva). La locale della lettura segue il TESTO, non la lingua
    di output scelta — leggere inglese con voce/locale tedesca
    suonerebbe male.
  - **Bug di percorso**: nel primo tentativo di aggiungere
    `AudioPreferences` a `AppContainer.kt` un `Edit` con match parziale
    ha troncato l'import di `AccentColorPreferences` in `AccentColor`,
    producendo anche il typo "AudioPreferencesPreferences" corretto
    (nel modo sbagliato) subito dopo. Scoperto solo alla ricompilazione
    finale (`Unresolved reference`), risolto ripristinando l'import
    corretto. Nessun impatto sul codice già committato.
  Compilazione e suite riverificate verdi. **Mai vista/sentita girare
  sul device**: verificare i due tasti "Prova" e che muovere il volume
  generale si senta davvero su entrambi i canali.

- **MUSICA: non partiva accendendo lo switch, si fermava uscendo da
  Opzioni** (22/07, dal device — Michele: "quando abilito non parte a
  meno che non cambio canzone e poi quando esco dalle opzioni smette
  di suonare"). Nel log: `error (-38, 0)`, il classico segnale di un
  metodo chiamato su un `MediaPlayer` gia' rilasciato — confermava la
  causa: il player viveva dentro `OptionsRoute`
  (`DisposableEffect { onDispose { previewPlayer.release() } }`) e
  moriva appena si usciva dalla schermata.
  - **Bug 1**: `onMusicEnabledChange` metteva in pausa quando si
    spegneva lo switch, ma non avviava mai nulla quando si accendeva —
    partiva solo indirettamente se poi si cambiava canzone (che aveva
    la sua chiamata a play separata).
  - **Fix strutturale**: nuovo `MusicPlayer` (`app/.../music/`), un
    `MediaPlayer` gestito a scope **APPLICAZIONE** (`AppContainer`),
    non piu' locale alla Route — sopravvive alla navigazione tra
    schermate. Conseguenza esplicita, non un effetto collaterale
    nascosto: la musica ora continua a suonare ANCHE durante
    l'Avventura, con Gemma attivo — la condizione posta da Michele il
    20/07 ("prima le misure di batteria") torna rilevante, ma e' lui
    stesso ad aver chiesto ora che non si fermi piu' uscendo da una
    schermata. Segnalato esplicitamente in chat, non implementato di
    nascosto.
  - Accendere lo switch avvia subito la traccia gia' selezionata;
    scegliere una traccia dal menu accende anche lo switch da solo se
    era spento (lo stato visibile non deve mentire su cosa sta
    suonando davvero).
  Compilazione e suite riverificate verdi. **Mai vista/sentita girare
  sul device.**

**APERTO — ordine del 20/07, ora aggiornato dalla nota sopra**:
1. ~~Chiudere la milestone di Fase 4: termico su 30-45' e drain
   batteria~~ — rimandato, vedi nota di ri-priorizzazione sopra.
2. ~~Agganciare le 52 immagini del catalogo alle scene~~ — FATTO 22/07:
   location (24) nel banner, nemico (14) in `CombatEntryZone`, NPC/
   incontri pacifici (20, comprese le `beast_*` condivise col nemico)
   sotto il testo narrato.
3. ~~Inventario: pasti senza effetto~~ — FATTO 22/07 (in due passi).
   ~~Scartare oggetti non esiste~~ — FATTO 21/07, tocco lungo con
   conferma. ~~Il pasto OBBLIGATORIO (requireAction EAT_MEAL) non
   curava~~ — FATTO 22/07 (Michele: "se i pasti oltre ad essere
   obbligatori fanno guadagnare 1 heal direi che abbiamo mantenuto il
   bilanciamento cosi che se ne trovo troppe almeno mi curo"): mangiare
   un Pasto quando disponibile ora cura +1 Resistenza oltre a evitare
   la penalita, capped a `effectiveMaxEndurance`. HUNTING non consuma
   un pasto vero (auto-soddisfa a costo zero) e percio' non guadagna la
   cura — verificato con test dedicato.
   ~~Il consumo MANUALE dalla scheda restava in silenzio per un Meal~~
   — FATTO 22/07, stesso turno (Michele: "si anche fuori puoi
   consumarli con questo effetto"): nuovo `MealRules`
   (`core/engine/inventory`, pubblico apposta) con nome canonico "Meal"
   e quantita di cura, condiviso tra `StatMechanics.requireAction` (il
   consumo obbligatorio, gia' esistente) e `AdventureState.consumeItem`
   in `:app` (il tocco sullo zaino, che gia' chiamava sempre
   `onConsumeItem` per qualunque oggetto — non serviva toccare la UI,
   solo la logica che decide se succede qualcosa). Un solo posto da
   cambiare se in futuro cambia quanto cura un pasto.

- **SUONO AL TIRO DEL DADO** (22/07, stesso messaggio di Michele:
  "ho aggiunto dragon-studio-sword-clashhit-393837.mp3, vorrei che
  quando premi il dado del destino nel combat si sente questo suono"):
  nuovo `SoundEffectPlayer` (`app/.../sfx/`, `SoundPool` non
  `MediaPlayer` — un colpo secco deve partire SUBITO al tocco, non
  dopo la latenza di preparazione di un player pensato per file lunghi
  in loop, quello resta a `MusicPlayer`). Volume legato al generale
  (`AudioPreferences`), stesso principio gia' in uso per TTS e musica.
  File copiato in `assets/sfx/dice_roll.mp3`. `TenSidedDie` ha un nuovo
  `onTap` chiamato al tocco (prima dell'animazione di rotazione, non a
  fine giro): il suono deve accompagnare il gesto di lanciare, non il
  risultato. Componente gia' pensato per essere riusato altrove (Fase
  7, Dado del Destino generale) — l'`onTap` e' li' pronto per quando
  servira'.
  Compilazione e suite riverificate verdi. **Mai vista/sentita girare
  sul device.**

  **Confermato subito dopo, stesso giro** (Michele: "ho aggiunto altri
  2 suoni, uno bere freesound_community-quick-pour-86306, mangiare
  nahtt-eat-323883, ovviamente se le associ alle azioni specifiche
  andrebbe bene"): `SoundEffectPlayer` riscritto attorno a un enum
  `SoundEffect` (`DICE_ROLL`/`EAT`/`DRINK`) invece di un metodo per
  suono — un solo punto di caricamento/riproduzione, scalabile per i
  prossimi. File copiati in `assets/sfx/eat.mp3` e `assets/sfx/drink.mp3`.
  `AdventureState.consumeItem` (il consumo MANUALE dalla scheda) fa
  partire `EAT` per il Pasto e `DRINK` per tutto il resto con effetto
  HEAL (pozioni) — distinzione per nome (`MealRules.ITEM_NAME`), non
  per un campo dedicato sull'oggetto.
  **Poi collegato anche il consumo OBBLIGATORIO** (22/07, stesso
  giorno, Michele dopo aver scartato l'idea del tag generato da Gemma:
  "però EAT_MEAL lo possiamo mettere nel JSON" — cioè: `requireAction
  action="EAT_MEAL"` è già scritto dall'autore nel libro, non generato
  a runtime, quindi il fatto "si è mangiato" è affidabile senza
  bisogno di istruire Gemma). `:core:engine` resta senza dipendenze
  Android (vincolo di progetto): non riproduce il suono lui, ma
  restituisce un fatto booleano che risale fino a `:app` dove vive
  `SoundEffectPlayer`. Percorso: `StatMechanics.requireAction` ora
  ritorna `Boolean` (pasto consumato per davvero, non la sola penalità)
  -> `MechanicsOutcome.mealEaten` (accumulato su tutti i gameMechanics
  della scena) -> `TransitionResult.mealEaten` (accumulato su TUTTI gli
  hop di un salto d'ufficio a catena, non solo l'ultimo) ->
  `AdventureState.moveTo` fa partire `SoundEffect.EAT` se il fatto è
  vero. Stesso principio di sempre (REGOLE.md: si serializzano i
  fatti, non si ricalcola nulla a valle). Tre nuovi test JVM
  (`MechanicsExecutorTest`) blindano i tre casi: pasto consumato,
  nessun pasto disponibile, HUNTING (che non consuma un pasto vero e
  quindi non deve far partire il suono).
  Compilazione e suite riverificate verdi. **Mai vista/sentita girare
  sul device.**

  **CONFERMATO sul device, poi corretto due volte, stesso 22/07**:
  primo giro con [test_eat_meal_sound.json](../content/test-books/test_eat_meal_sound.json)
  (nuovo libro campione dedicato, cartella `content/test-books/`, "crea
  sempre file json per fare i test quando aggiungiamo feature" —
  Michele) — i numeri sullo schermo (screenshot: Resistenza 26→21 in
  scena 1, poi 21→22 in scena 2 con i Meal scesi da 3 a 2) hanno
  confermato che consumo, cura e suono FUNZIONANO. Due correzioni
  emerse dal giro reale, non dalla logica di gioco:
  - Il file di test dava un Meal extra in scena 1 senza contare i 2
    Meal di serie che ogni eroe ha già da `INITIAL_COMMON_ITEMS`: 3
    pasti per 2 sole tappe di `requireAction` non arrivavano mai allo
    zaino vuoto, quindi il ramo "niente suono, solo penalità" non si
    vedeva mai. Corretto: niente `addItem` extra, quattro tappe totali
    così i 2 Meal di serie si esauriscono davvero all'ultima.
  - **Il suono partiva nell'istante del tocco sulla scelta, molti
    secondi prima che Gemma finisse di scrivere** (log: suono alle
    21:50:35, narrazione pronta alle 21:50:41 — 6 secondi dopo, prima
    ancora di essere mostrata). Michele: "molto prima che il testo
    venga scritto". Corretto spostando il trigger da `moveTo` (subito)
    a `startNarration`: un `pendingMealSound` messo da `moveTo` e
    consumato al primo pezzo di testo che arriva davvero (primo evento
    `NarrationEvent.Streaming` non vuoto), con reti di sicurezza su
    `NarrationEvent.Completed` e `narrationUnavailable()` per i casi
    degradati (niente streaming, si salta dritti al finale). Stesso
    principio già in uso per scelte/nemico (nascosti finché la
    generazione non finisce): un effetto legato alla narrazione deve
    aspettare che ci sia narrazione da vedere, non scattare sul fatto
    meccanico crudo.
  Compilazione e suite riverificate verdi. **Numeri confermati sul
  device (screenshot); il riallineamento col testo NON ancora
  riprovato sul device.**

  **Terzo giro, stesso 22/07** (Michele, dopo aver riprovato: "ci
  siamo quasi, l'unico problema è che il suono di EAT parte prima che
  il testo venga finito di scrivere, la cosa migliore sarebbe che il
  suono venga riprodotto dopo che finisce lo streaming del testo"):
  agganciare il suono al primo pezzo di `NarrationEvent.Streaming` non
  bastava — quel primo pezzo arriva comunque a metà generazione, non a
  streaming concluso (log: suono alle 22:08:49, generazione finita
  alle 22:08:52). Tolto il trigger dallo `Streaming`, resta solo su
  `NarrationEvent.Completed` (testo tradotto completo, stesso istante
  in cui compaiono scelte e nemico) e su `narrationUnavailable()` per
  il caso degradato.
  Compilazione e suite riverificate verdi. **CONFERMATO sul device
  (Michele 22/07/2026: "tutti i test ok") — chiuso.**

  **Nota a parte, non toccata da questo giro ma trovata durante
  l'indagine**: `consumeItem` (consumo manuale dallo zaino) limitava
  la cura al `maxEndurance` grezzo del personaggio invece che a
  `effectiveMaxEndurance` (che include i bonus da oggetto, es. Elmo/
  Gilet) — incoerente col resto del motore. Corretto per coerenza, non
  era la causa di quanto osservato stavolta (nessun bonus equipaggiato
  nel test).

- **BUG trovato durante lo stesso giro, separatore dei tag a schermo**
  (22/07, Michele: "funziona tutto ma devo vedere per forza --TAGS?"):
  `ResponseParser.narrativeOf` nascondeva il blocco tag solo quando il
  separatore `--- TAGS ---` era scritto PER INTERO nel testo
  accumulato. Lo streaming arriva un token alla volta: per la
  frazione di secondo in cui Gemma lo sta ancora scrivendo (es.
  `--- TAG`), quel pezzo non combacia col separatore completo e
  passava per narrativa vera, comparendo a schermo. Corretto con
  `trimTrailingPartialSeparator`: oltre al separatore completo, si
  taglia anche un suo prefisso ancora incompleto in coda al testo
  mostrato — si auto-corregge da solo al token successivo se il
  confronto smette di combaciare (es. un trattino di punteggiatura
  legittimo sparisce per una frazione di secondo, poi ricompare).
  Due nuovi test JVM (`ResponseParserTest`) sui tre stadi del
  separatore a metà (`--`, `--- TAG`, `--- TAGS -`).
  Compilazione e suite riverificate verdi. **CONFERMATO sul device
  (Michele 22/07/2026: "tutti i test ok") — chiuso.**

4. ~~**Preferences**: le classi ci sono, manca la schermata Opzioni~~ —
   **FATTO**, pulita la riga doppia (refuso di trascrizione). La
   schermata Opzioni (n. 7 di `UI.md`) è stata costruita e confermata
   sul device più volte in questi giorni: tema, colore d'accento, font
   veri, grassetto, tono narrativo, dimensioni testo, sfondo della
   card di stato (22/07), poi TTS con tre volumi e test voce, musica a
   combo con anteprima (22/07).
5. ~~Mancano ancora le **fixture** da output reali di Gemma~~ —
   **FATTO 22/07**: sei nuovi test in `ResponseParserTest`, non testo
   scritto a mano ma blocchi tag copiati dai log del device (E4B e 2B
   abliterated, stesso run di quella sera): un blocco ben formato con
   un refuso grammaticale reale ("sul tuo'arma"), l'allucinazione
   sulla scena finale (segnaposto del formato scritti alla lettera),
   un'IMAGE senza prefisso `loc_` scartata dal 2B, tre scelte
   inventate di sana pianta scartate, e caratteri giapponesi mescolati
   nella prosa (`strarつきate`) che non rompono il parsing. Tutti e sei
   confermano la stessa garanzia: qualunque cosa scriva il modello,
   solo ciò che combacia con la scena vera arriva al giocatore.
6. **RINVIATO CONSAPEVOLMENTE da Michele**: il leak di **140 MB per
   partita** (memoria nativa). Su 15,5 GB non si sente con 3 partite;
   l'ottimizzazione si fa alla fine. Non è una svista: è una scelta.
7. La **grafica** rinviata consapevolmente: il banner è v0.1, manca il
   compagno di viaggio. I PNG da 3-4 MB (`ic_axe`, `ic_map_icon`,
   `ic_gold`) vanno in WebP.
8. ~~Fallback tematico quando manca una location adatta (Gemma pesca
   da un altro catalogo, es. `misc_battle_clash`/`beast_wolves` come
   sfondo)~~ — **SCARTATO 22/07**, vedi punto 9.
9. ~~Tag DEDICATI `ENEMY_IMAGE`/`BEAST_IMAGE`/`NPC_IMAGE`~~ —
   **SCARTATO 22/07** (Michele, dopo essersi chiesto se servisse anche
   un TagParser dedicato per gestirli — risposta: no, sarebbe bastato
   estendere `ResponseParser` esistente: "quello non si fa, Gemma non
   è adatta a creare una logica così complessa, almeno la versione
   4B"). Non un "forse dopo": una decisione presa, motivata dal
   modello in uso oggi — se in futuro si cambia modello (i test con
   quelli alternativi sono già in corso, vedi 22/07 mattina) la
   domanda potrebbe riaprirsi, ma non è schedulata.
10. ~~`EAT_MEAL` scritto da Gemma nella narrazione per sincronizzare il
    suono del mangiare~~ — **SCARTATO 22/07**, stessa motivazione del
    punto 9: è la stessa famiglia (un tag in più = un altro
    vocabolario nel prompt), e Michele l'ha chiusa insieme agli altri
    due, non separatamente.

**Aggiornamento 22/07, stesso giorno**: la regola "solo azioni MANUALI"
sopra riguardava SOLO l'idea del tag generato da Gemma (punto 10,
scartata). Il consumo obbligatorio del pasto resta un caso a parte
perché è dichiarato dall'autore nel JSON, non generato dal modello —
vedi il paragrafo "Poi collegato anche il consumo OBBLIGATORIO" più
sopra: ora fa suono anche lui, con un canale diverso (il fatto
`mealEaten` che risale da `:core:engine`), non con un tag testuale da
riconoscere nell'output di Gemma.

**Misure ancora mancanti**: ~~il **termico** su 30-45'~~ — **FATTA
22/07**, vedi sotto (run di ~33'): degradazione confermata, sopra la
soglia d'allarme di CRITICITA.md C3. ~~il **drain della batteria**~~ —
**FATTA nello stesso run**.

**Batteria e temperatura AGGIUNTE alla riga MISURA** (22/07, Michele:
"per controllare anche il drain cosa dobbiamo fare?"): `LiteRtLmEngine`
ora legge percentuale e temperatura dallo sticky broadcast di sistema
`ACTION_BATTERY_CHANGED` (nessun permesso richiesto) e le scrive in
coda alla stessa riga di sempre (`batteria=NN% temp=NN.N°C`). Un solo
run lungo copre ora insieme velocità/token, memoria, batteria e
temperatura — non serve più incrociare a mano uno screenshot dello
stato con l'orario del log.
Compilazione e suite riverificate verdi. **Mai vista/sentita girare
sul device.**

**RUN DA ~33' — MISURA TERMICA E DRAIN CHIUSE, ALLARME CONFERMATO**
(22/07, Michele: "mezz'ora di gioco più o meno"): 55 generazioni,
22:38:40 → 23:11:22, nessun crash/eccezione/ANR in tutto il log.
- **Batteria**: 96% → 81%, **-15 punti in 33'** (~27,5%/h — una carica
  piena durerebbe ~2h10' di gioco continuo come questo). Primo numero
  vero sul drain, richiesto da Michele il 20/07.
- **Temperatura**: 32,0°C → 37,0°C (+5°C), sale con continuità e si
  stabilizza sui 36-37°C nella seconda metà del run.
- **Velocità: DEGRADAZIONE CONFERMATA, sopra la soglia d'allarme di
  CRITICITA.md C3** ("> 30% a fine sessione"). A parità di dimensione
  del prompt (~549 token, il caso più frequente nel run) si passa da
  18,2 token/s alla prima generazione a un regime stabile di 10-12 dopo
  il riscaldamento iniziale, fino a un pavimento di 7,2-8,0 token/s
  nell'ultimo terzo del run — una caduta del **~55-60%** dal picco. Non
  è un artefatto del prompt più lungo (quello incide sul primoToken,
  non sul token/s a parità di lunghezza): è il primo segnale REALE di
  throttling cumulativo che il run da 12' del giorno prima aveva solo
  fatto sospettare (allora fermo a 9,7 token/s sull'ultima
  generazione).
- **Memoria nativa**: 1082MB → 2152MB nel run, con cali parziali tra
  una partita e l'altra (pattern già noto, coerente col leak da
  ~140MB/partita rimandato consapevolmente — nessuna novità).
  Compilazione e suite riverificate verdi. **CONFERMATO sul device.**

  **DECISIONE DI MICHELE (22/07, stesso giorno)**: allarme
  ACCETTATO così com'è, non è un blocco. "È un GDR cartaceo, la
  velocità non è un problema purché non vada in freeze — e per adesso
  non ne ho avute. Avendo poi tutto abilitato, dal TTS alla musica, un
  compromesso è accettabile." Nessuna mitigazione da implementare ora
  (né taglio del modello né pausa termica): il turno per turno di un
  librogioco tollera 7-19 token/s meglio di un'app in tempo reale, e
  finché non si arriva a un freeze vero non c'è azione da prendere.
  **Aperta però un'esplorazione, non una scelta già presa**: "potremmo
  provare con un modello più piccolo ma la narrativa è buona, non
  vorrei compromettere il testo — si potrebbero provare più scelte"
  — cioè confrontare alternative più leggere SENZA sacrificare la
  qualità della prosa, non sostituire Gemma 3 4B a prescindere. Non
  schedulata: si riprende quando Michele vuole confrontare modelli
  candidati (i test con alternative erano già in corso dal mattino del
  22/07, vedi sopra).

  **PROSSIMO PASSO (Michele, fine sessione 22/07)**: sta per scaricare
  un paio di **Gemma 4 2B abliterated** trovati in giro per provarli al
  posto del modello attuale (Gemma 4B) — un modello più piccolo per
  davvero, coerente con l'esplorazione appena aperta sopra (si ferma
  qui per la sera, si riprende domani notte). La riga MISURA con
  batteria/temperatura appena aggiunta serve esattamente a questo
  confronto: stessi numeri, modello diverso — e resta il vincolo di
  Michele, "non vorrei compromettere il testo": la prova non è solo
  velocità/batteria, è anche leggere se la prosa regge.

- **BOTTONE "ATTIVA" per cambiare motore a caldo** (22/07, Michele,
  subito dopo: "potresti implementare al volo un tasto per rendere
  attivo uno dei motori che scarico, così posso scaricarne più di uno
  e provare?"): **bug reale trovato nel farlo** — selezionare un
  modello nella schermata Modelli (tocco sulla card) cambiava SOLO
  `ModelPreferences.selectedModelId`, mai il motore davvero caricato.
  `AppContainer.ensureModelLoaded()` esce subito se
  `inferenceEngine.isLoaded` è già vero, senza mai controllare se il
  modello caricato è quello selezionato: cambiare selezione con un
  modello già in memoria non avrebbe avuto ALCUN effetto fino al
  riavvio dell'app (processo nuovo, `isLoaded` riparte da falso). Con
  un solo modello scaricato per volta il bug non si vedeva mai — con
  più modelli scaricati (esattamente quello che Michele vuole fare)
  sarebbe sembrato "ho attivato il modello B ma gira ancora l'A".
  Aggiunto `AppContainer.loadedModelId` (quale modello è DAVVERO nel
  motore, distinto dalla sola preferenza) e `AppContainer.activateModel()`
  (chiama `InferenceEngine.load()` col nuovo file — che già fa
  l'unload del precedente da sé, in `LiteRtLmEngine`) — cambio motore
  SICURO anche a partita in corso: ogni scena apre una sessione nuova
  e senza memoria, quindi non c'è contesto da perdere nel cambio. In
  `ModelsScreen`: bottone "Attiva" su ogni modello scaricato
  (disabilitato se già attivo o durante l'attivazione), etichetta
  "In uso ora" sulla card del motore davvero caricato.
  Compilazione e suite riverificate verdi. **Mai vista/sentita girare
  sul device.**

- **BUG: la musica non riparte all'apertura dell'app** (22/07,
  Michele, dopo aver provato i due modelli 2B: "un baco rimane, ad
  esempio sulla musica appena apro l'applicazione anche se la musica è
  selezionata come attiva non suona"). Causa: `musicPlayer.play()`/
  `.pause()` partono SOLO da un tocco dentro `OptionsRoute` (switch o
  scelta traccia) — mai da soli. `MusicPlayer` a scope applicazione
  (fix del 22/07 mattina) sopravvive alla navigazione DENTRO un
  processo già avviato, ma su un processo NUOVO (app appena aperta)
  nasce silenzioso e nessuno leggeva mai `MusicPreferences.musicEnabled`
  per farlo ripartire da sé: la musica restava spenta finché non si
  rientrava in Opzioni e si ritoccava lo switch, anche con la
  preferenza salvata su "attiva". Corretto con un `LaunchedEffect(Unit)`
  in `AppNavigation` (una volta sola per processo, non dentro
  `OptionsRoute` che si monta/smonta a ogni navigazione): se
  `musicEnabled` è vero, fa partire subito `musicPlayer.play()` con la
  traccia e il volume salvati (stessa formula `musicVolume *
  generalVolume` di `OptionsRoute`).
  Compilazione e suite riverificate verdi. **Mai vista/sentita girare
  sul device.**

- **PROVATO un Gemma 4 2B abliterated — più veloce, ma testo peggiore**
  (22/07, Michele, log dedicato dopo che il primo inviato risultava
  ancora sull'E4B — impronta esatta delle sezioni del file, stessa
  identica ai log precedenti: confermato PRIMA di fidarsi del
  "sembra migliorato"). Col 2B vero confermato dall'impronta diversa
  (`nativa` ~620MB invece di ~1080-2150MB, velocità 21-37 token/s
  contro 8-19 dell'E4B — più veloce di un buon 2×) è comparsa la cosa
  che Michele temeva: "non vorrei compromettere il testo".
  - **Caratteri estranei nel testo mostrato al giocatore**: "le lame
    già strarつきate" — caratteri giapponesi mescolati dentro una
    parola italiana, nella prosa VISIBILE (non nel blocco tag
    scartato: si vede a schermo per davvero, salvato nel diario-grafo
    come testo letto). "Caratteri strani" di Michele, confermati.
  - **Grammatica peggiore in generale**: "Lo acciaio strascia
    liberando due brutti che emergono dalle ombre" — italiano rotto,
    frase senza senso; l'E4B sullo stesso punto della storia
    (sample-adventure scena 4) scriveva "L'acciaio risuona liberamente
    mentre due sciacalli escono dall'ombra del magazzino", corretto.
  - **Blocco tag ancora più confuso dell'E4B**: ID inventati invece di
    quelli veri (`CHOICE|scene_01|...`, `DISCIPLINE|discipline_basic|
    ...`, `IMAGE|warehouse` invece di `loc_warehouse` — prefisso perso
    due volte su due), e sulla scena finale (senza scelte vere)
    inventate di sana pianta tre scelte fittizie ripetute anche come
    DISCIPLINE. **Nessun rischio per il giocatore**: `ResponseParser.
    resolveChoices` fa il match sul `nextSceneId` VERO della scena
    (`choices.isEmpty() -> emptyMap()` se la scena non ne ha), quindi
    tutta questa invenzione viene scartata in silenzio e le scelte
    mostrate restano sempre quelle reali del libro (al più non
    tradotte, mai finte) — degradazione garantita, confermata anche
    sul modello peggiore provato finora.
  **Non ancora una decisione**: Michele valuta se il guadagno di
  velocità vale il calo di qualità del testo, coerente con quanto
  detto la sera prima ("non vorrei compromettere il testo — si
  potrebbero provare più scelte"). Nessuna azione di codice presa: è
  un giudizio sul modello, non un bug da correggere.

- **Tre segnalazioni sul motore dei modelli, dopo il giro di prove col
  2B** (22/07, Michele, in un colpo solo):
  1. **Download bloccato anche col 5G**: "non mi fa scaricare anche
     con il 5G, io ho la connessione flat per cui non mi importa".
     `startDownload` vincolava il worker a `NetworkType.UNMETERED`
     (solo Wi-Fi) — una decisione lasciata esplicitamente "in attesa
     di Michele" da giorni (vedi sopra, sessione 20/07). **Presa ora**:
     basta `NetworkType.CONNECTED`, qualunque rete va bene. Questo
     probabilmente spiega anche il punto 3 sotto.
  2. **BUG: cancellare un modello attivo non lo segna più tale**:
     "anche se ho cancellato un modello questo risulta attivo,
     controlla". `onDelete` in `ModelsRoute` cancellava il file ma non
     toccava mai `activeModelId`: la card continuava a mostrare "In
     uso ora" per un modello che non esisteva più sul telefono.
     Corretto: cancellare il modello attivo azzera `activeModelId`.
  3. **`gemma-4-E2B-it_qualcomm_sm8750.litertlm` non parte**: NON è un
     bug nostro, è un limite del motore attuale. Quel file (e i
     fratelli `_intel_LNL`/`_intel_PTL`/`_Google_Tensor_G5`/
     `_qualcomm_qcs8275` sullo stesso repo Hugging Face) sono varianti
     compilate apposta per il delegate NPU del chip indicato nel nome
     — `sm8750` è il nome interno Qualcomm dello Snapdragon 8 Elite del
     Razr di Michele, quindi sulla carta sarebbe la variante giusta.
     Ma `LiteRtLmEngine.load()` prova solo `Backend.GPU()` e
     `Backend.CPU()`, mai NPU, e non imposta mai
     `litert_dispatch_lib_dir` (il warning
     "You should provide the `DispatchLibraryDir` option to use NPU"
     compare in OGNI log finora, sempre ignorato perché finora si è
     sempre usata la variante generica). Un file compilato per
     l'Hexagon NPU probabilmente fallisce o degrada sul percorso
     GPU/CPU generico. Per ora: usare sempre la variante SENZA
     suffisso vendor (es. `gemma-4-E2B-it.litertlm`, quella già usata
     con successo ieri notte).

     **Approfondito e SCARTATO, stessa giornata**: Michele ha fatto
     notare di aver già bundlato le librerie QNN vere (`libQnnHtp.so`
     + varianti V69/V73/V75 con Skel/Stub) in v1 (progetto `stdf`,
     generazione immagini con Stable Diffusion) — quindi ottenere e
     ridistribuire le librerie Qualcomm sul suo device è fattibile,
     dimostrato. Ma quel bridge era C++/JNI scritto a mano, parlava
     con l'SDK QNN direttamente; LiteRT-LM invece vuole uno specifico
     collante di Google (`libLiteRtDispatch_Qualcomm.so`) che l'AAR
     Android non include (issue aperta su google-ai-edge/LiteRT) — si
     otterrebbe solo compilandolo da sorgente con Bazel, un toolchain
     nuovo fuori dal progetto Gradle/Kotlin attuale. Michele: "concordo
     che non voglio reintrodurre C++ in questa versione... non stiamo
     generando immagini ma testo, e se devo aspettare un po' va bene
     così" — **decisione presa, non un rinvio**: niente NPU in Ex, il
     motivo non è la difficoltà tecnica ma una scelta di architettura
     (niente C++/toolchain nativo in questa riscrittura) unita al
     fatto che per il testo (non immagini) la velocità attuale basta.
     Non riaprire senza una ragione nuova ed esplicita.
  Compilazione e suite riverificate verdi (fix 1 e 2). **Mai visti
  girare sul device.**

- **BUG: il numero del dado sparisce dopo il tiro nel combattimento**
  (22/07, Michele: "dopo che premo il pulsante il numero che ha dato
  il dado scompare, sarebbe più bello che restasse così so cosa è
  uscito"). Causa: `TenSidedDie` tiene `face` in memoria LOCALE
  (`remember`), ma vive dentro `CombatActiveZone`'s `key(combatTick)`
  (fix del 21/07 per "RES non cambiano dopo MINDBLAST/il dado" —
  `CombatSession` non è osservata da Compose, serve la ricreazione
  forzata per far leggere di nuovo RES/CS). Ogni round `combatTick`
  cambia -> `key()` ricrea l'INTERO sottoalbero -> `face` torna a
  `null` -> il numero appena uscito sparisce, sostituito da "?".
  Corretto senza toccare `key(combatTick)` (servirebbe di nuovo per
  RES/CS): nuovo parametro `TenSidedDie(initialFace: Int?)`, seminato
  da `AdventureState.lastRound?.roll` — quel valore vive FUORI dal
  sottoalbero ricreato (in `AdventureState`, già impostato PRIMA che
  `combatTick` cambi in `combatFightRound()`), quindi sopravvive alla
  ricreazione: il dado nuovo nasce già mostrando l'ultimo tiro vero,
  invece di azzerarsi. `TenSidedDie` resta un componente autonomo
  (candidato al Dado del Destino generale di Fase 7): il parametro è
  opzionale, default `null`, non lega il componente al combattimento.
  Compilazione e suite riverificate verdi. **Mai vista girare sul
  device.**

- **Prime rifiniture UI dal registro cartaceo di Lupo Solitario**
  (22/07, Michele ha mandato le foto delle 4 pagine del registro
  ufficiale — Diario di Combattimento, Zaino/Borsa/Pasti/Oggetti
  Speciali, Combattività/Resistenza/Armamento, Registro di Guerra —
  chiedendo confronto e proposte). Il reskin completo (pergamena,
  bordi strappati, font gotico, illustrazioni) resta un discorso a
  parte: servono asset veri (font, texture, icone) che Michele
  dovrebbe procurarsi — non le stesse illustrazioni Mongoose/
  originali, per non riprodurre materiale protetto. Fatte le due
  proposte più economiche, che riusano dati già presenti:
  - **`CombatDiaryPanel`**: aggiunto "Paragrafo {scena}" in testa (il
    cartaceo lo mostra sempre, noi avevamo `currentScene.id` ma non
    lo portavamo mai dentro al combattimento) e il Rapporto di Forza
    ora sta dentro un riquadro bordato invece che testo nudo, più
    vicino al box del cartaceo.
  - **Scheda personaggio**: Combattività e Resistenza scomposte in
    **Base + Modificatori**, come nel registro. Non ricalcola nulla
    di nuovo: `weaponskillBonus()` e `itemEnduranceBonus()` erano già
    pubbliche apposta (`EffectiveStats.kt`, commento del 20/07: "la
    Scheda deve poter SPIEGARE il numero... invece di duplicare
    l'if/else lato UI"), `modifierLabel` (già in `CombatDiaryPanel`
    per gli stessi modificatori nel combattimento) è diventata
    `internal` e riusata qui invece di duplicata.
  - **Verificato durante la discussione**: il grado Kai (Novizio →
    Gran Maestro Supremo) esiste GIÀ (`KaiRank`, REGOLE.md Blocco 3) e
    si vede già in scheda e card di stato — non era un pezzo mancante,
    solo non ancora vestito come "Registro di Guerra". Con le 5
    discipline fisse del Libro 1 resta sempre "Iniziato Kai": Michele
    ricordava "3 discipline nel primo libro" ma ha confermato di
    essersi sbagliato, 5 resta la regola.
  Restano da valutare: lista icone per arma stile "Disciplina della
  Scherma" (icone già presenti, `ic_axe` e sorelle) e layout dedicato
  per il grado Kai.
  Compilazione e suite riverificate verdi. **Mai viste girare sul
  device.**

- **Suono dei passi nelle transizioni + dado sostituito** (22/07,
  Michele: "aggiungo un suono per il roll del dado e uno di passi da
  usare nelle transizioni, che dici?"). Discusso prima di scrivere
  codice: le transizioni scattano anche nei salti d'ufficio a catena e
  in contesti dove "camminare" non c'entra (ingresso in combattimento,
  morte, finale) — passi su OGNI cambio scena sarebbero stati
  ripetitivi o stonati. Michele ha confermato lo scope proposto.
  - `assets/sfx/dice_roll.mp3` sostituito (`roll_dice.mp3`, un suono
    di dado vero al posto del provvisorio scontro di spade).
  - Nuovo `SoundEffect.FOOTSTEPS` (`bubu07audio-running-on-sand-
    357373.mp3`). Parte in `AdventureState.moveTo` solo se la scena di
    arrivo è `sceneType == TRANSITION` E non ha combattimento (una
    TRANSITION può essere anche una scena di scontro, es.
    sample-adventure scena "4" — lì i passi stonerebbero). Parte
    SUBITO, non in sospeso come il suono del pasto: accompagna il
    gesto di scegliere una strada, non un fatto da confermare col
    testo di Gemma.
  Compilazione e suite riverificate verdi. **Mai sentiti girare sul
  device.**

- **Suono d'inizio combattimento, riciclando il vecchio dado** (22/07,
  Michele, confermato il nuovo suono del dado: "ok sembra andare il
  vecchio roll prima era un rumore di spade lo possiamo usare per
  l'inizio delle scene di combattimento?"). Nessun asset nuovo: il
  file rimasto libero dal cambio di `dice_roll.mp3`
  (`dragon-studio-sword-clashhit-393837.mp3`) diventa
  `SoundEffect.COMBAT_START`, agganciato sia a `startQuickCombat()`
  che a `startCompleteCombat()` — l'unico punto in cui un
  combattimento comincia per davvero, in entrambe le modalità.
  Compilazione e suite riverificate verdi. **CONFERMATO il dado sul
  device (Michele: "sembra andare"); il suono d'inizio combattimento
  mai sentito girare.**

- **Suoni per le immagini del catalogo e per i finali — struttura
  completa, PRIMA degli asset** (22/07, Michele: "puoi prevederli
  tutti se non ci sono non si suonano... l'importante è che non vada
  in errore... intanto puoi fare il codice per tutte queste cose").
  Scritto tutto senza aspettare i file mp3, che Michele procurerà con
  calma (checklist in `doc/SUONI-IMMAGINI.md`).
  - `SoundEffectPlayer.playNamed(name, folder)`: vocabolario APERTO
    (a differenza dell'enum `SoundEffect` fisso) — carica al bisogno
    invece che tutto all'avvio (troppi nomi, 50+ immagini più i
    finali), cache con `null` per i nomi già provati e assenti (non
    si ritenta ad ogni chiamata — attenzione già presa: `getOrPut` con
    Kotlin NON distingue "chiave assente" da "chiave presente con
    valore null" per una mappa `<String, Int?>`, quindi si usa
    `containsKey` esplicito invece). File mancante = silenzio, mai un
    errore. Il costruttore è diventato `private val context` (prima
    solo parametro, serviva anche fuori dall'`init`).
  - **Immagini** (`AdventureState.syncImageSounds`): un suono per
    `backgroundImage`/`enemyImage`/`npcImage`, ma SOLO quando il
    valore cambia rispetto all'ultima volta (tre variabili
    `lastPlayed*`), non ad ogni ricomposizione. Richiamata sia da
    `moveTo` (copre nemico/NPC, sempre dell'autore, noti subito, e lo
    sfondo quando l'autore ne ha uno valido) sia dal completamento
    della narrazione (copre lo sfondo quando arriva più tardi da
    Gemma). Cartella attesa: `assets/sfx/images/`.
  - **Finali** (`AdventureState.playEndingSoundIfNew`): agganciato a
    `EndingOutcome` (VICTORY/DEFEAT/NEUTRAL) e al genere dell'eroe,
    stessa distinzione già in uso per la voce TTS — 6 nomi attesi
    (`ending_victory_male`/`_female`, `ending_defeat_male`/`_female`,
    `ending_neutral_male`/`_female`). Non è più "che aspetto ha
    l'eroe" (`hero_female`/`hero_male`, scartato) ma "come finisce la
    storia" — aggancio più corretto. Cartella `assets/sfx/endings/`.
  Compilazione e suite riverificate verdi. **Mai sentiti girare sul
  device: nessun asset esiste ancora, solo la struttura.**

- **Quarta barra volume: Effetti sonori** (22/07, Michele: "manca una
  cosa, la barra con il volume dei suoni nelle preferenze deve essere
  una barra a parte"). Fino a qui `SoundEffectPlayer` (dado, passi,
  mangiare/bere, inizio combattimento, e ora immagini/finali) usava
  SOLO il volume generale — a differenza di TTS e musica, che hanno
  già ciascuno il proprio controllo indipendente.
  - Nuova `SoundEffectPreferences` (`util/`, stesso schema esatto di
    `TtsPreferences`/`MusicPreferences`: una sola preferenza `volume`,
    default 0,7).
  - `SoundEffectPlayer` prende ora anche `SoundEffectPreferences` nel
    costruttore (con default `= SoundEffectPreferences(context)` per
    non rompere chi lo istanzia con un solo argomento); il volume
    effettivo è `effettiSuoni × generale`, stessa formula già in uso
    per `effectiveMusicVolume()`.
  - `VolumeSection`/`VolumeUi`: quarta barra "Effetti sonori", stesso
    slider degli altri tre, nessun aggiornamento "live" mentre si
    trascina (a differenza della musica, non c'è uno stream continuo
    da correggere in tempo reale — un tocco per sentirlo basta).
  Compilazione e suite riverificate verdi. **Mai vista girare sul
  device.**

- **Font di lettura sostituiti con quattro font a tema Lupo Solitario**
  (22/07, Michele: "ho trovato queste, puoi scaricarle e sostituirle
  alle font esistenti" — Cinzel Decorative, Almendra Display,
  MedievalSharp, Uncial Antiqua). Stessa fonte e licenza dei quattro
  precedenti (github.com/google/fonts, OFL), verificati scaricabili
  uno per uno prima di sostituire (`ofl/almendra/`, non
  `ofl/almendradisplay/`: Michele stesso ha notato che la Display
  "non è l'ideale per testi leggermente più lunghi" — qui il font
  copre il testo di lettura vero, non solo i titoli, quindi si è
  scelta la variante pensata per quello). `almendra.ttf`
  identificato in modo bizzarro dal comando `file` di sistema ("SIMH
  tape data") ma con tabelle sfnt regolari nell'header
  (DSIG/GDEF/GPOS/GSUB/OS2/cmap/glyf/head/...) e compilazione risorse
  Android passata senza errori — falso allarme dell'euristica di
  `file`, non corruzione vera.
  `ReadingFont` rinominato (ALMENDRA/CINZEL/MEDIEVAL_SHARP/UNCIAL al
  posto di SERIF/SANS_SERIF/MONOSPACE/CURSIVE — sicuro in Fase 4,
  nessun utente vero con preferenze salvate da rompere): default
  spostato su Almendra, la più leggibile delle quattro. Vecchi file
  (`lora.ttf`, `inter.ttf`, `roboto_mono.ttf`, `caveat.ttf`) cancellati
  per davvero, non lasciati come scarto morto in `res/font/`.
  Compilazione e suite riverificate verdi. **Mai visti girare sul
  device.**

- **Primi asset del reskin: 9 icone armi + decorazioni, e un bug di
  esportazione trovato per strada** (22/07, Michele ha mandato tre
  file in `origina_res/` per il piano di reskin — foglio armi,
  decorazioni opzionali, texture pergamena — dicendo "trovi le
  corrispondenze nel doc di UPGRADE").
  - **BUG scoperto**: tutti e tre i file avevano il canale alpha
    pieno (255 ovunque) — la scacchiera "trasparente" che si vedeva
    era disegnata DENTRO i pixel, non trasparenza vera del formato
    (probabile anteprima del tool di generazione salvata al posto
    dell'export con canale alpha). Nell'app sarebbe comparso un
    riquadro grigio a scacchi intorno a ogni icona. Nel frattempo
    avevo già sovrascritto le 6 icone armi esistenti (che ERANO
    trasparenti per davvero) tagliando il foglio a griglia — **tutto
    ripristinato** (`git checkout` sulle 6 icone, cancellati i file
    nuovi rotti, codice tornato pulito) prima di procedere oltre.
  - Michele ha rifatto due dei tre file con **sfondo bianco pieno**
    invece che trasparente (più facile da esportare per lui). Rimosso
    lo sfondo qui con `cv2.floodFill` a 8 semi (i 4 angoli + i 4 punti
    medi dei lati) — **prima tentativo con range "floating" (default):
    ha mangiato quasi tutto il disegno**, non solo lo sfondo: su un
    bordo anti-aliasato la tolleranza confronta ogni pixel col VICINO
    appena colorato, quindi "scivola" un passo alla volta dal bianco
    fino al nero lungo la sfumatura, senza mai un salto abbastanza
    grande da fermarsi (misurato: da 0-1% di pixel opachi rimasti su
    7 icone su 9). **Corretto con `FLOODFILL_FIXED_RANGE`** (confronta
    col colore ORIGINALE del seme, non col vicino): 20-33% di pixel
    opachi su tutte e nove, range sano.
  - **Secondo bug, di slicing non di trasparenza**: il foglio
    decorazioni ha 6 colonne nella prima riga ma solo 3 nella seconda
    (icone più grandi, spaziate diversamente) — tagliarlo con una
    griglia uniforme 6×2 ha preso `wolf_logo` a metà del medaglione
    arcano e `deco_combat_emblem` a metà delle spade incrociate.
    Corretto ritagliando la riga 2 da sola, a griglia 3 colonne.
  - **Risultato**: 9 icone armi coerenti (`ic_dagger`/`ic_spear`/
    `ic_mace`/`ic_short_sword`/`ic_warhammer`/`ic_sword`/`ic_axe`/
    `ic_staff`/`ic_broadsword`, agganciate in
    `CreationCatalog.weaponTypeIcon` — le tre mancanti non usano più
    il segnaposto `ic_unknown_item`), `ic_map_icon` sostituita nello
    stesso stile, e 8 decorazioni (`deco_backpack`/`deco_gold_pouch`/
    `deco_meal`/`deco_travel_gear`/`deco_potion`/`deco_combat_emblem`/
    `deco_arcane_medallion`/`wolf_logo`) pronte in `res/drawable/` ma
    **non ancora agganciate a nessuno schermo** — manca la decisione
    di dove usarle. La texture di pergamena resta in sospeso: quel
    file non è stato riesportato con sfondo bianco, stesso trattamento
    ancora da fare.
  Compilazione e suite riverificate verdi (icone armi, uniche già
  agganciate al codice). **Mai visti girare sul device.**

- **Scoperta a margine**: due commit (`5a31894`, `0a54760`) risultano
  fatti da Michele con "Claude Fable 5" come co-autore — un'altra app/
  sessione Claude (probabilmente sul telefono) che Michele usa per
  salvare in fretta i file grezzi in `origina_res/` mentre continua a
  lavorare qui per l'integrazione vera. Nessun conflitto, solo utile
  saperlo per non stupirsi di file nuovi o piccoli edit a UPGRADE.md
  comparsi senza essere passati da questa sessione.

- **Texture di pergamena: stesso trattamento, un bug in più trovato
  per strada** (22/07, stesso giro): Michele ha riesportato anche
  questo file con sfondo bianco. Sfondo rimosso con lo stesso
  flood-fill a range fisso → `res/drawable/parchment_panel.png`, bordi
  strappati puliti, trasparenza vera confermata.
  **BUG nello script trovato qui, invisibile prima**: `cv2.imwrite` si
  aspetta l'ordine canali BGR(A), non RGB(A) — lo script convertiva in
  RGBA prima di scrivere, quindi `imwrite` ha ri-invertito i canali
  una seconda volta, scambiando rosso e blu. Sulle icone armi/
  decorazioni (bianco e nero, R≈G≈B) non si vedeva nessuna differenza;
  sulla pergamena (colore vero, marrone/beige) il risultato usciva
  bluastro. Verificato che le icone già salvate NON fossero affette
  (scarto massimo tra R e B: 15/255, rumore di compressione, non un
  vero scambio di canale) — corretto lo script per il resto.
  Non ancora agganciato a nessun pannello: la pergamena è chiara,
  l'app gira quasi sempre in tema scuro con testo chiaro — serve
  decidere come restare leggibili sopra un fondo chiaro prima di
  usarla per davvero (vedi UPGRADE.md).
  Compilazione riverificata verde. **Mai vista girare sul device.**

- **Pergamena scura + scelta in Opzioni** (23/07, seguito diretto del
  punto sopra): Michele ha chiesto se creare una variante scura avesse
  senso; risposta data — più semplice e fedele forzare l'inchiostro
  scuro sul testo sopra la pergamena chiara, invece di ridisegnare
  tutta la texture. Michele ha comunque fatto realizzare la variante
  scura E proposto di **far scegliere lo stile in Opzioni** — deciso di
  fare entrambe le cose insieme: 1 texture scura pronta subito,
  struttura a scelta multipla già pronta per quando ce ne saranno
  altre.
  - `origina_res/Texture di sfondo scuro.png` (2048×2048, sfondo
    pieno) processata con lo stesso script ormai corretto (flood-fill
    a range fisso, nessuna conversione di canale prima di
    `imwrite`) → `res/drawable/parchment_panel_dark.png`, verificata
    numericamente (bordi trasparenti, centro marrone scuro intatto).
  - Nuovo `ParchmentStyle` (OFF/LIGHT/DARK, enum con `drawableRes` +
    colore `INK` condiviso) e `ParchmentPreferences` (stesso schema di
    `AccentColorPreferences`/`StatusCardColorPreferences`), nuova
    `ParchmentSection.kt` nelle Opzioni, cablata in `AppContainer` ->
    `OptionsRoute`/`OptionsScreen` -> `AdventureRoute`/`AdventureScreen`
    -> `CombatActiveZone` -> `CombatDiaryPanel` (l'unico pannello che
    già usava una Card dedicata).
  - **Inciampo tecnico**: il primo tentativo (`Box` + `Image(Modifier
    .matchParentSize())`, pattern visto altrove) non compilava —
    `Unresolved reference 'matchParentSize'` non risolveva in questo
    setup Compose. Risolto dipingendo la pergamena direttamente come
    sfondo del `Column` con `Modifier.paint(painter, contentScale =
    Crop, sizeToIntrinsics = false)`: il flag è quello che conta,
    senza il pannello vorrebbe adottare il rapporto d'aspetto
    intrinseco dell'immagine (quasi quadrato) invece di seguire il
    proprio contenuto. Testo forzato all'inchiostro scuro con
    `CompositionLocalProvider(LocalContentColor provides
    ParchmentStyle.INK)` attorno al contenuto, così non serve toccare
    ogni singolo `Text` — quelli con un colore esplicito (es. i
    modificatori `tertiary`) restano quello che erano.
  Compilazione e suite riverificate verdi (`core:engine`, `core:data`,
  `app:compileDebugKotlin`, `app:testDebugUnitTest`). **Mai vista
  girare sul device**: default OFF (comportamento di sempre), da
  provare accendendo la scelta in Opzioni durante un combattimento.

- **Quarto stile AUTO + bug d'inchiostro corretto** (23/07, stesso
  giorno, Michele: "auto mode per lo sfondo sceglie chiaro scuro a
  seconda del tema del sistema, oppure selezioni indipendentemente e
  in quel caso adatti i colori"):
  - `ParchmentStyle` ha ora un quarto valore, **AUTO**: risolve in
    `LIGHT`/`DARK` in base al tema EFFETTIVAMENTE in uso
    (`isDarkTheme`, filo diretto da `MainActivity` — che già lo calcola
    con `ThemePreferences.useDarkTheme`, override incluso — fino a
    `AppNavigation` -> `AdventureRoute` -> `AdventureScreen` ->
    `CombatActiveZone` -> `CombatDiaryPanel`, stesso schema di
    `parchmentStyle`). Non il solo sistema operativo: se Michele ha
    forzato un tema dalle Opzioni, AUTO rispetta quello.
  - **BUG trovato rileggendo il codice per aggiungere AUTO**: il colore
    d'inchiostro (`ParchmentStyle.INK`) era UNICO per entrambe le
    varianti — un residuo del primo giro, quando la pergamena scura
    ancora non esisteva. Sulla texture scura (centro ~83,66,50, un
    marrone scuro) il testo forzato scuro (0x2A1F14) sarebbe stato
    quasi invisibile: lo stesso problema di leggibilità già segnalato
    come "da decidere" nella voce precedente, mai più ripreso finché
    non è saltato fuori scrivendo AUTO. Corretto PRIMA di finire sul
    device: due inchiostri (`INK_ON_LIGHT` invariato, nuovo
    `INK_ON_DARK` color crema 0xE8DCC5), scelti da una nuova
    `ParchmentStyle.inkColor()` in base allo stile GIÀ RISOLTO (mai su
    AUTO, che da solo non ha drawable/inchiostro propri).
  Compilazione e suite riverificate verdi. **Mai vista girare sul
  device**: da provare tutte e tre le scelte (AUTO coi due temi, LIGHT
  e DARK forzati) durante un combattimento vero.

- **Pergamena estesa al pannello di narrazione** (23/07, stesso giorno,
  Michele con uno screenshot: "no claude non ha aggiornato lo sfondo
  guarda" — riferito al box di testo della scena normale, non al
  combattimento, dove infatti non era mai stata agganciata). Chiarito
  lo scope ed esteso su richiesta esplicita: stesso trattamento di
  `CombatDiaryPanel` applicato al `Card` che mostra `state.narrative`
  in `AdventureScreen.kt` — Card Material3 di sempre se OFF, altrimenti
  `Column` dipinta con `Modifier.paint` (`sizeToIntrinsics = false`) e
  inchiostro forzato via `CompositionLocalProvider`, stile risolto con
  lo stesso `ParchmentStyle.resolved(isDarkTheme)`/`inkColor()` di
  poco fa — nessuna nuova astrazione, stesso codice duplicato una
  seconda volta perché i due pannelli hanno strutture diverse (Card
  con `weight(1f)` + scroll qui, Card semplice nel diario di
  combattimento).
  Compilazione e suite riverificate verdi. **Mai vista girare sul
  device.**

- **BUG REALE trovato da Michele sul device: la pergamena non si
  vedeva affatto** (23/07, screenshot + poi un log completo su
  richiesta): niente texture, niente bordi strappati, niente scudi
  negli angoli — solo il colore piatto di sfondo dell'app dietro un
  riquadro trasparente. Il log (`adb logcat`, 853 righe) non mostrava
  nessun crash né eccezione: il motore Gemma generava regolarmente
  (misura 42,1 token/s), la UI restava viva. Non un errore rumoroso,
  un `Modifier.paint()` che semplicemente non disegnava nulla — mai
  verificato sul device prima d'ora (introdotto durante il turno
  precedente proprio per aggirare l'errore di compilazione di
  `matchParentSize`, mai riconsiderato dopo).
  **Causa radice, trovata rileggendo con calma l'errore originale**:
  quell'errore (`Unresolved reference 'matchParentSize'`) non era
  perché l'API non esiste — è un MEMBRO di `BoxScope`, non una
  funzione top-level, quindi non si importa affatto: basta chiamarla
  dentro un `Box { }`. Il vero errore era una riga di troppo,
  `import androidx.compose.foundation.layout.matchParentSize`, che
  non ha mai avuto nulla da risolvere. Invece di cancellare quella
  riga, il giro precedente aveva cambiato completamente approccio
  (`Modifier.paint`) — la correzione sbagliata, mai controllata su
  device, e infatti quella rivelatasi rotta.
  **Corretto tornando al pattern originale** `Box` + `Image(Modifier
  .matchParentSize())`, lo STESSO già usato e confermato funzionante
  in `AdventureBanner` (l'illustrazione visibile in cima a ogni
  scena, in entrambi gli screenshot di Michele) — non una scelta
  nuova, un pattern già in produzione. Applicato sia a
  `CombatDiaryPanel` sia al pannello di narrazione di
  `AdventureScreen`.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device**: la correzione si basa su un pattern già provato altrove
  nel progetto, ma il caso preciso (pergamena + testo sopra) va
  comunque riverificato con gli occhi.

  **EPILOGO, stesso 23/07**: dopo il fix Box+Image, Michele riportava
  ancora "ancora non va" anche dopo un "restart pulito". Aggiunto un
  log temporaneo (`Log.d`, tag `ParchmentDebug`) che stampa
  preferenza/tema/stile risolto/drawable ad ogni composizione, per
  capire se il ramo nuovo veniva anche solo raggiunto — a quel punto
  tre giri di fix diversi avevano prodotto zero cambiamenti visibili,
  serviva un dato oggettivo invece di continuare a indovinare.
  **Il log ha chiuso il caso in un colpo solo**: `preferenza=OFF`. Il
  codice era corretto fin dal giro precedente — il "restart pulito"
  di Michele era quasi certamente una disinstallazione, che azzera le
  `SharedPreferences` (`parchment_preferences` è un file a parte): la
  scelta "Automatica" fatta in una build precedente non sopravvive a
  una disinstallazione, e serve rifarla da Opzioni dopo ogni
  reinstallazione pulita. **Non un bug di rendering, un problema di
  persistenza delle preferenze durante il test** — la lezione per le
  prossime volte: un log con i valori effettivi chiude in un giro
  quello che tre round di fix "al buio" non erano riusciti a
  chiarire. Log di debug rimossi una volta trovata la causa.
  Compilazione riverificata verde. **Ancora da confermare sul
  device**: Michele deve riselezionare lo stile in Opzioni (senza
  disinstallare) e riprovare.

  **CONFERMATA sul device, stesso 23/07**: dopo aver riselezionato lo
  stile in Opzioni la pergamena si vede — foto mandata da Michele,
  bordi strappati e scudi visibili, inchiostro leggibile. La causa
  era davvero solo la preferenza azzerata dalla disinstallazione, il
  codice era già giusto.

  **Difetto reale trovato SUBITO dopo, dalla stessa foto** (Michele:
  "il testo sfora, si potrebbe tagliare in due ed avere la parte
  centrale che si allunga fino alla dimensione del testo?"):
  `ContentScale.Crop` su un'unica immagine quadrata tiene il bordo
  strappato a proporzioni FISSE, ma il testo del narratore può essere
  più lungo del riquadro — risultato, l'ultima riga di prosa finiva
  sforata oltre il bordo inferiore della pergamena. Proposta di
  Michele esattamente giusta: è il principio del nine-patch Android
  (bordi fissi, centro elastico), mai usato in questo progetto
  finora.
  **Un ostacolo in più scoperto tagliando**: i bordi strappati corrono
  su TUTTI e quattro i lati della pergamena, non solo alto/basso — una
  fascia centrale presa a piena larghezza avrebbe portato dentro una
  tacca del bordo laterale, poi stirata in una riga verticale enorme e
  innaturale. Risolto con un inset orizzontale (10% per lato) sul
  ritaglio della sola fascia centrale, che esclude le tacche più
  profonde e lascia solo texture pulita.
  Costruita `ParchmentBackground.kt` (nuovo file, estensione di
  `BoxScope`: `Modifier.matchParentSize()` va richiamata dentro un
  `Box`, quindi la funzione condivisa deve dichiararsi tale, non una
  funzione libera) che impila tre `Image` — alto e basso a dimensione
  naturale (`ContentScale.FillWidth`), il centro steso
  (`ContentScale.FillBounds`) su un `Modifier.weight(1f)` che assorbe
  tutto lo spazio verticale restante. `ParchmentStyle` non espone più
  un singolo `drawableRes`, ma tre risorse (`topRes`/`middleRes`/
  `bottomRes`) per LIGHT e DARK; 6 nuovi file PNG generati con uno
  script Python/Pillow dai due pannelli già pronti (nessun nuovo
  asset chiesto a Michele). Condivisa da `CombatDiaryPanel` e dal
  pannello di narrazione di `AdventureScreen`, un solo posto invece
  di due copie della stessa pila.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device**: risolve il difetto SULLA CARTA (bordi fissi, centro
  elastico), ma la fascia centrale è stata scelta a occhio — va
  vista con un testo vero, lungo, per giudicare se il "cucito" tra le
  tre fasce si vede o no.

  **Vista sul device, BRUTTA** (Michele, foto + "è brutta come è
  venuta"): la fascia centrale, presa da una fettina di soli 100px
  (200 per la variante scura), su un paragrafo lungo si stirava in
  verticale per 5-8 volte — la grana sottile della pergamena
  diventava una striatura verticale che sembrava legno, con un
  cucito visibile dove incontrava la fascia superiore. Il principio
  (bordi fissi, centro elastico) era giusto; la fettina scelta per il
  centro era troppo sottile per lo stiramento reale che doveva
  sopportare.
  **Corretto** allargando la fascia centrale a quasi TUTTO lo spazio
  verticale disponibile tra le due fasce fisse nell'immagine
  originale (576px su 1024 per la chiara, 1150px su 2048 per la
  scura, invece di 100/200px) — con una fonte quasi della stessa
  taglia del bisogno reale, lo stiramento resta vicino a 1:1 anche su
  testi lunghi, niente più smagliatura. Stesso inset orizzontale di
  prima (i denti del bordo laterale restano esclusi).
  Compilazione riverificata verde. **Ancora da confermare sul
  device.**

  **Vista sul device, ANCORA CONFUSA** (stesso 23/07, Michele, con la
  stessa foto: "stesso problema la parte alta è troppo piccola
  rispetto a quella centrale e il testo va fuori"): la vera causa non
  era più la texture, ma la STRUTTURA del pannello. Il pannello di
  narrazione ha sempre avuto un'altezza FISSA (`Modifier.weight(1f)`,
  riempie lo spazio che resta sullo schermo) con lo scroll interno del
  testo — comportamento invariato da prima della pergamena. Con la
  pergamena, però, lo scroll interno mostra un pezzo A CASO del
  paragrafo dentro una cornice che sembra sempre "l'inizio di una
  pagina" (bordo strappato in alto fisso sullo schermo), e il pezzo
  visibile può iniziare a metà frase — da qui la sensazione di testo
  "che va fuori": non uno sforamento di rendering, un disallineamento
  concettuale tra "quanta pergamena si vede" (fissa) e "quale pezzo di
  storia si vede" (scorrevole a caso).
  Michele ha scelto esplicitamente, tra tre alternative proposte (far
  crescere il pannello e scorrere tutta la schermata / tenere il
  riquadro fisso ma con una pergamena continua senza l'aria di inizio-
  pagina / spezzare in pagine): **il pannello cresce, scorre l'intera
  schermata**. Tolto `Modifier.weight(1f)` e lo scroll interno dal
  pannello di narrazione (ora wrap-content, stesso principio di
  `CombatDiaryPanel`): l'INTERA `Column` da banner a scelte/scheda è
  ora dentro una nuova `Column(Modifier.weight(1f).verticalScroll(...))`,
  solo l'header resta fisso in cima. Il testo, ora per intero, sta
  sempre su tutta la pergamena — niente più "alcune parti sì, altre
  no".
  **Corretta nella stessa foto anche l'animazione del narratore
  troppo in alto** (Michele: "scrive sopra il logo e confonde il
  testo"): lo stato "pensa" saltava la riga dell'icona "leggi" che nel
  testo vero riserva ~48dp sopra — aggiunto uno `Spacer(48.dp)`
  identico prima di `NarratorThinking`, stesso margine dal bordo della
  pergamena in entrambi gli stati.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device**: cambia la UX in modo reale (le scelte in fondo ora
  possono richiedere uno scroll su un paragrafo lungo, invece di
  essere sempre visibili appena sotto) — scelta esplicita di Michele,
  non collaterale.

  **Vista sul device, testo ANCORA su nero in due punti** (stesso
  23/07, Michele, con la stessa identica foto: "la parte alta e
  troppo piccola rispetto a quella centrale... il testo finisce sopra
  il logo... lo noti con le parole 'svegli' e 'verso'"): il pannello
  ora cresce correttamente, ma proprio quelle due parole cadevano
  dentro un dente del bordo strappato — misurato con numpy: il dente
  PIÙ profondo nella fascia alta arriva al 30,5% dell'altezza
  originale (chiara) e 36,4% (scura), ben oltre il 90° percentile
  tipico (~6-12%). Non un bug di layout: la texture del bordo
  strappato ha zone trasparenti profonde per disegno, e prima o poi
  una riga di testo ci casca sopra.
  **Primo tentativo, SCARTATO**: "sanare" via script i denti oltre il
  92° percentile (riempiendoli con il colore del bordo naturale a
  quella profondità) ha introdotto un artefatto peggiore — righe
  verticali dorate che sforavano fin sopra il bordo strappato, dove
  il colore campionato era un pixel di bordo/luce anziché pergamena
  piatta. Ripristinati i due master puliti da git (`git checkout --`)
  prima di procedere oltre: nessun danno resta.
  **Fix più semplice e robusto**: un colore PIENO (vicino alla media
  della texture: chiaro `#DEC9AB`, scuro `#554334`, campionati con
  numpy dalla zona centrale sicura) dipinto PRIMA della pila di tre
  immagini in `ParchmentBackground`. Qualunque dente, per quanto
  profondo, mostra questo colore invece del nero del tema — risolve
  il problema per costruzione, indipendentemente da quanto sono
  profondi i denti, senza dover ritoccare pixel per pixel le due
  texture. Nuovo campo `ParchmentStyle.baseColor`.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

  **BOCCIATO DA MICHELE, 24/07**: uno schizzo suo, mandato con un
  disegno vero — "non mi piace nulla, deve essere una cosa così, vorrei
  però che ripartissi": tutta la pila a tre fasce (nine-patch, colore
  di sfondo pieno) buttata via. Il disegno mostra un'idea più semplice
  e diversa: la pergamena grande resta un'illustrazione DECORATIVA
  (bordo strappato + scudi, `ContentScale.Crop` sull'intero riquadro,
  nessun bisogno di inseguire l'altezza del testo pixel per pixel), e
  il testo vive in un riquadro PIÙ PICCOLO e SEPARATO, con margine
  dalla cornice, un bordo vero (marrone cuoio) e uno sfondo = la
  stessa texture piatta del centro già pronta (`middleRes`, senza
  denti strappati — qui non serve nessun trucco). Due domande fatte
  PRIMA di ricominciare (per non sbagliare una quarta volta): il
  riquadro di testo torna ad avere altezza fissa con scroll interno
  (confermato — annulla la scelta "scorre tutta la schermata" di
  poco fa) e il bordo va disegnato per davvero (confermato).
  Nuovo file `NarrationParchmentPanel.kt`: `Box` con l'immagine intera
  (`ParchmentStyle.fullRes`, nuovo campo) a piena dimensione, dentro
  un secondo `Box` più piccolo (padding 28dp, bordo 2dp marrone cuoio
  `#4A3524`, sfondo = `middleRes` con `ContentScale.Crop` — sicuro
  perché il riquadro ha dimensione FISSA, non deve stirarsi). Ripristinato
  lo scroll interno sul `Text` (`verticalScroll`) e `Modifier.weight(1f)`
  sul pannello, tolta la Column-wrapper che faceva scorrere l'intera
  schermata. `ParchmentBackground.kt` (la pila a tre fasce) resta
  SOLO per `CombatDiaryPanel` (contenuto breve, non scrolla, il
  problema del dente profondo è meno probabile ma protetto comunque
  dal `baseColor`).
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device**: quarto giro su questa stessa feature, stavolta partito da
  un disegno esplicito invece che da un'idea mia — file da guardare
  con più attenzione prima di dichiararlo chiuso.

  **Vista sul device, "molto meglio"** (24/07, Michele: "l'idea è
  giusta" — primo riscontro positivo sull'intera feature pergamena):
  ha mandato due foto, la seconda con un rettangolo arancione
  disegnato SOPRA lo screenshot per indicare dove vuole davvero il
  bordo — più stretto di com'era, ben dentro rispetto agli scudi degli
  angoli. Due correzioni precise:
  - **Margine insufficiente**: misurato con Pillow sull'immagine
    intera, gli scudi occupano fino al ~19-22% dell'area dagli angoli
    — il padding fisso di 28dp non bastava, e su schermi diversi da
    quello di prova il rapporto cambia comunque (un dp fisso non
    scala con la dimensione del riquadro). Sostituito con
    `Modifier.fillMaxSize(0.68f)` — una FRAZIONE del riquadro
    esterno, non un valore assoluto: scala automaticamente qualunque
    sia lo schermo, resta ben dentro gli scudi.
  - **Bordo oro/argento secondo il TEMA**, non lo stile pergamena
    (richiesta esplicita: "in notturna può essere oro e in tema
    chiaro argento"): `NarrationParchmentPanel` ora riceve
    `isDarkTheme` solo per questo — chi sceglie "Pergamena chiara" col
    telefono in tema scuro vuole comunque il bordo oro, non quello
    legato allo stile. Il marrone cuoio di prima si confondeva con la
    texture (contrasto troppo basso); oro (`#D4AF37`) e argento
    (`#9A9A9A`) risaltano su qualunque variante.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

  **"Bello" — primo riscontro pienamente positivo sull'intera feature
  pergamena, 24/07**: Michele ha provato anche il tema chiaro senza
  pergamena, gli piace anche l'effetto "zoom" del pannello scoperto
  poco fa (lo trova un effetto speciale piacevole, non un bug — resta
  invariato, nessun codice toccato per quello). Ha chiesto se il
  bordo oro/argento potesse usare una texture: spiegato che una
  texture fotografica su un bordo di pochi dp non renderebbe (Compose
  non segue il tracciato del bordo con un pattern ripetuto, si
  vedrebbe solo schiacciata/irriconoscibile) — un GRADIENTE imita
  meglio un riflesso metallico. Confermato, con due dettagli:
  colori PIENI (non pastello) e bordo un po' più spesso per dargli
  risalto.
  `Modifier.border()` accetta anche un `Brush`, non solo un `Color`:
  sostituito con `Brush.linearGradient` a 5 fermate (chiaro→scuro→
  chiaro→scuro→chiaro, diagonale) per entrambe le varianti, spessore
  da 3dp a 5dp.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

- **Nuovo default per la card di stato** (24/07, richiesta Michele: "il
  default non sarà più uguale a quello di visualizzazione del testo" —
  prima DEFAULT non passava nessun colore, restava la superficie
  standard di Material, la stessa del riquadro di lettura quando la
  pergamena è OFF): ora DEFAULT ha un colore VERO, diverso per tema —
  blu navy (`#1E2A4A`) in tema chiaro, marroncino (`#D8B48C`) in tema
  scuro, con testo abbinato per il contrasto. Gli altri 5 preset
  (Lavanda/Azzurro/Menta/Ambra/Rosa, personalizzabili in Opzioni)
  restano fissi e invariati — solo il default cambia, come richiesto
  esplicitamente. Due nuove funzioni `resolvedBackground(isDarkTheme)`/
  `resolvedContent(isDarkTheme)` al posto di leggere `background`/
  `content` direttamente; `StatusCard` riceve `isDarkTheme` solo per
  questo. **Nota a margine, non corretta apposta**: lo swatch di
  anteprima in Opzioni per DEFAULT mostra ancora il grigio generico
  "come il tema" (non naviga/marroncino) — richiederebbe risolvere il
  tema anche lì (Options non lo fa oggi), fuori scope per la richiesta
  di oggi.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

- **Nome dell'eroe personalizzabile** (24/07, idea di Michele, discussa
  prima di scrivere codice: "che ne dici?" — accettato con una
  precisazione mia: il default va accorciato da "Lupo Solitario"/"Lupa
  Solitaria" (il nome CANONICO del protagonista dei libri, sembra già
  scelto da qualcuno) a "Lupo"/"Lupa" (un segnaposto chiaramente
  anonimo, invita a essere sostituito) — Michele ha confermato).
  Nuovo `CreationState.heroName` (stringa vuota di default), un
  `OutlinedTextField` opzionale nella schermata di creazione (subito
  sotto la scelta del genere) col placeholder che mostra già "Lupo"/
  "Lupa" secondo il genere selezionato — così si vede cosa succede
  lasciandolo vuoto, senza doverlo indovinare. `buildSession` risolve
  `heroName.trim().ifBlank { "Lupo"/"Lupa" }`.
  **Stessa richiesta, seconda parte**: il nome dell'eroe ora compare
  anche nella lista dei salvataggi (schermata Avventura, quando esiste
  già una sessione) — prima si vedeva solo il titolo del libro e la
  scena, non chi la sta giocando. L'eroe è unico per costruzione
  (stesso presupposto già usato in `GameState.hero`): si legge con
  `characters.first { role == HERO }`.
  Aggiornata anche la @Preview della schermata Avventura (prima non
  aveva nessun personaggio nella sessione d'esempio — `.first{}`
  sull'eroe sarebbe andato in crash appena renderizzata).
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

- **Icona dell'eroe personalizzabile, tutti animali** (24/07, idea di
  Michele — prima discussa: gli ho dato una lista di 15 animali con le
  specifiche (2048×2048, stile china bianco/nero come il lupo attuale)
  da passare al grafico; poi confermato "prepara tutto... come default
  se non ci sono dai il lupo così tutto funziona"):
  - Nuovo `HeroIcon` (enum, `:core:data`, zero Android — stesso schema
    di `WeaponType`): WOLF + 14 animali (Falco, Aquila, Orso, Volpe,
    Corvo, Gufo, Leone, Tigre, Pantera, Lince, Cinghiale, Cervo,
    Serpente, Drago). Nuovo campo `Character.icon`, default WOLF —
    `@Serializable` con default, retrocompatibile con le sessioni già
    salvate (JSON vecchio senza il campo → WOLF).
  - Mappa verso i drawable in `:app` (`CreationCatalog.heroIconRes`/
    `heroIconName`, stesso schema di `weaponTypeIcon`): riga per riga,
    non un `else` unico — SOLO WOLF ha un asset vero
    (`lupo_solitario.png`), tutti gli altri 14 puntano anch'essi al
    lupo per ora, così la scelta funziona e si vede subito già oggi;
    quando arriva un'illustrazione nuova si cambia una riga sola.
  - Scelta in creazione (`HeroIconCard`, nuova, riusa `WeaponCell` già
    esistente per le armi — stessa griglia, stessa cella): subito dopo
    la scelta del genere. `CreationState.heroIcon` (default WOLF),
    passato a `Character.icon` in `buildSession`.
  - `StatusCard` non mostra più `R.drawable.lupo_solitario` fisso: usa
    `heroIconRes(hero.icon)`, che oggi degrada sempre sul lupo ma è già
    collegato alla scelta reale.
  Compilazione e suite riverificate verdi (incluse `core:engine`/
  `core:data`, la serializzazione del salvataggio non si è rotta).
  **Ancora da confermare sul device**: con l'asset unico (lupo) per
  tutte le scelte, oggi si vede sempre lo stesso ritratto qualunque
  animale si scelga — atteso, non un bug, finché non arrivano le
  illustrazioni vere.

  **Le 14 illustrazioni sono arrivate lo stesso giorno**: Michele ha
  consegnato `origina_res/icone per personaggi.png`, un foglio unico
  5×3 con tutti e 14 gli animali (più un lupo di riferimento nello
  stesso stile) e un'etichetta di testo sotto ogni icona. Ritagliate
  con uno script Python/OpenCV: confini misurati con un profilo di
  oscurità per riga (il testo sotto ogni icona ha una "gobba" di
  pixel scuri separata, non attaccata al disegno — le tre righe hanno
  margini diversi, la terza riga finisce più in basso delle prime
  due) per tagliare SOLO il disegno, mai la scritta. Sfondo rimosso
  con lo stesso flood-fill a range fisso già collaudato (nessuna
  sorpresa stavolta: sfondo bianco pieno, non a scacchi). 14 nuovi
  file `res/drawable/hero_*.png`, percentuali di opacità 18-35%,
  sane. Agganciati uno per uno in `CreationCatalog.heroIconRes`.
  **Deciso subito dopo da Michele: "uniforma il tutto"** — il lupo
  nuovo (stesso stile "ombreggiato" degli altri 14) ha SOSTITUITO
  `lupo_solitario.png` (stesso nome file, quindi anche `TenSidedDie`
  — la faccia zero del dado — eredita il lupo nuovo senza bisogno di
  toccarlo). `hero_wolf_new.png` cancellato, non più necessario.
  Tutte e 15 le icone sono ora nella stessa famiglia visiva.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device**: primo giro con le illustrazioni vere, mai visto girare.

- **Default scuro della card di stato, invertito** (24/07, stesso
  giorno, Michele sul device con foto: "un colore di default per il
  tema scuro che non si legge chiaramente" — la card col marroncino
  CHIARO spiccava come una toppa fuori posto in mezzo a tutto il resto
  scuro, più uno stacco di contesto che un vero difetto di contrasto
  testo/sfondo). Invertito: `DEFAULT_DARK_BG` ora un marrone cuoio
  SCURO (`#3D2B1F`, prima `#D8B48C` chiaro), `DEFAULT_DARK_CONTENT`
  crema chiaro (`#EFE0C9`, prima marrone scuro) — la card ora si
  comporta da superficie scura in mezzo alle altre invece di stonare,
  il default in tema chiaro (blu navy) resta invariato.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

- **Main theme dell'app, nuovo brano di default** (24/07, richiesta
  Michele: nuovo file consegnato, "vorrei fosse il default... dovrebbe
  essere il main theme"): `menu_Destino_segnato.mp3` copiato in
  `assets/music/`, aggiunto a `BundledMusicCatalog.TRACKS` come "Main
  Theme — Destino Segnato" — messo in TESTA alla lista, così
  `default = TRACKS.first()` lo prende automaticamente senza toccare
  nessun'altra riga. Catalogo ora a 5 tracce (prima 4). Nessun test
  dipende dall'ordine o dal conteggio del catalogo.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

- **Icone animale più grandi + nuovo nome di default** (24/07, stesso
  giorno, Michele: "ingrandisci un po' le icone degli animali... il
  nome di default a questo punto dovrebbe essere Eroe Solitario/Eroina
  Solitaria"):
  - `WeaponCell` (condivisa con la griglia delle armi) ha ora un
    `iconSize` parametrico (default 48dp invariato, le armi non
    cambiano) — `HeroIconCard` passa 72dp, griglia con celle minime
    da 90dp a 120dp e altezza da 360dp a 460dp per farci stare le
    icone più grandi senza schiacciarle.
  - Il nome di default non è più "Lupo"/"Lupa" (aveva senso quando
    l'icona era sempre il lupo, non più ora che si sceglie tra 15
    animali — "Lupo" come nome scegliendo un drago sarebbe strano):
    **"Eroe Solitario"/"Eroina Solitaria"**, generico per qualunque
    icona scelta. Nuove stringhe `creation_name_default_male/female`
    (placeholder nel campo nome); `creation_gender_male/female`
    ("Lupo"/"Lupa", le etichette del toggle genere) restano invariate,
    sono un'altra cosa. Fallback in `CreationState.buildSession`
    aggiornato allo stesso modo.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

- **I due eroi a china, sostituiscono i ritratti fotorealistici** (24/07,
  Michele: "ho aggiunto un nuovo file con i due eroi, genera le due
  icone e vorrei che nel menu ci fosse la figura intera e sostituire
  le vecchie icone con icone realizzate da queste"): consegnato
  `origina_res/hero.png` (1024×1024, i due eroi affiancati, stile a
  china coerente col resto del reskin). Da notare PRIMA di procedere:
  i vecchi `class_warrior_male/female.jpeg` (usati nel ritratto di
  creazione e nel banner) erano fotorealistici — assomigliavano
  parecchio a Geralt di Rivia (The Witcher), stile completamente fuori
  registro rispetto al resto dell'app. Sostituiti, non solo aggiornati.
  - Ritagliati due busti (440×440 maschio, 370×370 femmina) con sfondo
    rimosso (stesso flood-fill collaudato) → `hero_portrait_male.png`/
    `hero_portrait_female.png`, agganciati al posto di
    `class_warrior_male/female` sia nel ritratto di creazione
    personaggio sia nel banner della scena (`AdventureBanner`). File
    vecchi rimossi (`git rm`), nessun altro riferimento rimasto.
  - Illustrazione intera (`hero_banner.png`, stesso sfondo rimosso)
    aggiunta come apertura della schermata Home (`ContentScale.Fit`,
    mai `Crop` — "la figura intera", non ritagliata): la Home prima
    non aveva NESSUNA immagine, solo testo e tile.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

- **Icone animale, secondo giro** (24/07, stesso messaggio di Michele:
  "quasi non distingui gli animali diversi" — 72dp non bastava
  ancora): altro salto, non un ritocco — 100dp (celle minime da 120 a
  140dp, altezza griglia da 460 a 560dp).
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

- **BUG SERIO nel download dei modelli: il progresso si legava alla
  selezione, non al download vero** (24/07, Michele mentre provava i
  download: "sembra che l'app sia bloccata... sono partiti i task per
  il download e non si stoppano... l'interfaccia non aggiorna" — log
  allegato). Il log mostrava la prova diretta: un lavoro WorkManager
  (`6d8450de...`) cancellato dopo soli ~15MB, e un secondo
  (`66b56673...`) partito 7ms dopo, stesso worker unico
  (`ModelDownloadWorker`, `ExistingWorkPolicy.REPLACE`).
  **Causa radice**, trovata in `ModelsScreen.kt`: ogni card mostrava il
  vero `downloadState` SOLO se `model.id == selectedModelId` — per
  TUTTE le altre card restava sempre `Idle`, quindi il loro bottone
  "Scarica" restava SEMPRE attivo, anche mentre un download diverso
  era in corso. Un tocco lì — anche per sbaglio, o perché la card
  giusta sembrava non aggiornarsi — cancellava il download in corso
  (WorkManager REPLACE sullo stesso lavoro unico) e ne accodava uno
  nuovo, potenzialmente all'infinito se il giocatore, confuso, ritocca
  "Scarica" pensando che l'app sia bloccata.
  **Corretto**: il worker ora rimanda l'ID del modello nel progresso
  (`KEY_MODEL_ID`, scritto SUBITO all'avvio, non solo ai passi di
  `PROGRESS_STEP` — l'identità è nota fin dal primo istante, non
  serve aspettare 2MB scaricati) — mai più fidarsi di
  `selectedModelId`, che poteva appartenere a un modello diverso da
  quello che sta scaricando per davvero. Nuovo `runningModelId` in
  `ModelsRoute`, derivato dal progresso vero. Il bottone "Scarica" di
  OGNI card che non è quella in corso viene ora DISABILITATO
  (`downloadBlockedByOther`) finché il download attivo non finisce:
  il tocco che prima cancellava tutto ora è semplicemente impossibile.
  Compilazione e suite riverificate verdi. **CONFERMATO sul device**
  (24/07, Michele, dopo aver rifatto la prova dei tre tocchi in
  sequenza sulla build con il fix: "ok questo fix funziona") — chiuso.
  Un giro di analisi in più prima della conferma: un log successivo
  mostrava un solo download (Gemma 3n, ripreso da 1,37GB, completato
  con successo) senza nessuna cancellazione — utile a escludere che
  `isDownloaded()` avesse un problema strutturale a parte (per Gemma
  3n, dimensione ignota in anticipo: il file finale esiste SOLO se il
  worker l'ha rinominato dopo aver verificato che il download fosse
  completo, mai un troncato spacciato per buono).

- **Musica "zombie" dopo aver chiuso l'app + notifica di download senza
  modo di fermarla** (24/07, Michele: "anche se chiusa continuavo a
  sentire la musica... ho dovuto riavviare il telefono... e anche se
  nella lista dei processi mi dice che l'app non c'è più continua a
  scaricare e sento la musica"). Meccanismo esatto, confermato prima di
  scrivere codice: `MusicPlayer` è un `MediaPlayer` grezzo a scope
  applicazione, SENZA nessun Service né notifica — di suo, chiudendo
  l'app il processo morirebbe e la musica con lui. Ma il download usa
  un Foreground Service (corretto: i download DEVONO sopravvivere in
  background) che tiene in vita l'INTERO processo, musica compresa,
  che invece non aveva alcun motivo di restare accesa — due componenti
  diversi, un solo sintomo. Proposta a Michele e confermata: NON un
  vero player persistente stile radio per la musica (troppo lavoro per
  qualcosa che nel gioco deve restare musica d'ambiente legata all'app
  aperta) — due fix mirati:
  - `MainActivity.onDestroy()` ora ferma `musicPlayer` esplicitamente:
    quando l'utente chiude davvero l'app (swipe dai recenti), Android
    chiama SEMPRE onDestroy sull'Activity, a prescindere dal fatto che
    il processo sopravviva per il download.
  - La notifica del download ora ha un bottone **Annulla**
    (`WorkManager.createCancelPendingIntent`, l'API pensata apposta per
    questo — nessun `BroadcastReceiver` scritto a mano): prima non
    aveva NESSUNA azione, l'unico modo di fermare un download bloccato
    era riaprire l'app o riavviare il telefono.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

- **Bottone "Attiva" rosso quando è già attivo** (24/07, stesso giorno,
  Michele: "invece che grigio quando è attivato il pulsante diventa
  rosso e compare la frase Attivato" — il grigio disabilitato di
  Material passava inosservato, non si capiva a colpo d'occhio quale
  modello fosse in uso): `colorScheme.error` pieno (non lo sbiadito di
  default sui bottoni disabilitati — servono
  `disabledContainerColor`/`disabledContentColor` espliciti, altrimenti
  Material applica comunque la sua trasparenza) + testo "Attivato" al
  posto di "Attiva", resta comunque non cliccabile.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

- **Icone nello zaino, arti marziali/elmo/corazza ancora vecchie**
  (24/07, Michele con due screenshot: scheda personaggio e creazione).
  Due cose distinte:
  - **Zaino senza icone**: Pasti e pozioni mostravano solo testo,
    diversamente dalle celle arma/oggetto speciale altrove (icona
    sopra, nome+modificatore sotto). Agganciate `deco_meal`/
    `deco_potion` (dal foglio decorazioni, già pronte ma mai
    agganciate finora) — nuova `backpackItemIcon()`, null per
    qualunque oggetto non riconosciuto (es. da un `ADD_ITEM` di un
    libro): niente icona, mai un segnaposto rotto.
  - **Arti marziali/Elmo/Corazza, confermate vecchie icone v1**:
    misurate con Pillow — `ic_fists.png` (204×247, palette
    indicizzata, 9KB), `ic_helmet.png`/`ic_armor.png` (386×150,
    palette indicizzata, 2-5KB), tutte MOLTO più piccole e a linea
    sottile rispetto alle 9 icone armi nuove (748×226+, RGBA,
    100-200KB, contorni spessi) — erano rimaste fuori dai due fogli
    già consegnati. Aggiunte a `doc/UPGRADE.md` con le stesse
    specifiche (2048×2048, PNG, sfondo bianco) per il prossimo giro
    dal grafico — nessun codice da scrivere finché non arrivano i
    file.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

- **Arti marziali/Elmo/Corazza: illustrazioni vere, stesso giorno**
  (24/07, Michele: "ho aggiunto il file decorazioni... trovi anche
  altre immagini ma trovi quelle tre nuove"): consegnato
  `Decorazioni.png`, un foglio con 10 icone — 7 già viste in un giro
  precedente (zaino, borsa oro, pasto, corredo da viaggio, pozione,
  emblema lupo+spade, medaglione arcano) e 3 davvero nuove: un gesto
  "karate chop" (arti marziali, sostituisce il pugno chiuso di v1), un
  elmo con muso di lupo, un gilet di maglia. Ritagliate e sfondo
  rimosso con lo stesso flood-fill collaudato, **sovrascritti
  direttamente** `ic_fists.png`/`ic_helmet.png`/`ic_armor.png` (stesso
  nome file di prima): zero righe di codice da toccare, il catalogo
  puntava già lì.
  Compilazione e suite riverificate verdi. **Mai visto girare sul
  device.**

- **Icone ingrandite ovunque, un pelo** (24/07, stesso giorno, Michele:
  "rendi tutte le icone un pelo più grandi, soprattutto quelle delle
  armi in fase di creazione"): `WeaponCell` (condivisa da armi e
  oggetti speciali) da 48dp a 64dp di default — celle della griglia
  armi da 110dp a 130dp, altezza da 420 a 460dp per farcele stare.
  Icona nello zaino (appena agganciata) da 28dp a 34dp. `HeroIconCard`
  (animali, già a 100dp da un giro precedente di ingrandimenti) lasciata
  invariata.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

- **Icone armi, secondo giro** (24/07, stesso giorno, Michele: "aumenta
  ancora le icone delle armi, non le distingui, almeno il 50%"): 64dp
  non bastava ancora — `WeaponCell` (condivisa da armi e oggetti
  speciali) a 96dp, esattamente +50% rispetto a 64dp. Griglia armi:
  celle minime da 130 a 190dp, altezza da 460 a 700dp per farcele
  stare senza schiacciarle.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

- **Griglia armi: era la griglia sbagliata, non (solo) l'icona
  piccola** (24/07, foto: "puoi ingrandirle anche del doppio rispetto
  allo spazio" — con `GridCells.Adaptive(minSize = 190.dp)` lo schermo
  ci faceva stare una SOLA colonna: celle enormi a piena larghezza con
  l'icona (96dp) persa in mezzo al vuoto. Cambiata a `GridCells
  .Fixed(2)`: celle più strette e quadrate, icona a 120dp che le
  riempie per davvero — non più un numero scelto alla cieca ma
  proporzionato alla cella vera. Altezza griglia 700→950dp per le 5
  righe da due colonne.
  **Stessa richiesta, seconda parte**: sfondo VERDE
  (`0xFF2E7D32`, alpha 0.35) sull'arma della specializzazione
  WEAPONSKILL, se scelta — indipendente dal bordo oro dell'arma
  IMPUGNATA ORA: due fatti diversi (specializzazione vs selezione
  corrente), visibili insieme se coincidono. Nuovo parametro
  `isSpecialization` su `WeaponCell`, applicato anche alla cella Arti
  marziali (la specializzazione può essere UNARMED).
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

- **Creazione più veloce: default sensati + statistiche già tirate**
  (24/07, Michele: "per velocizzare la creazione mettiamo alcune
  scelte a default... le discipline non selezionarle ovviamente"):
  - `weaponSkillType` di default `SWORD` (Spada) — resta INERTE finché
    WEAPONSKILL non è tra le discipline scelte (`buildSession` lo
    azzera comunque se non serve), ma se si sblocca il menu a tendina
    mostra già "Spada" invece di "—".
  - `selectedWeapon` di default l'arma Spada (da `INITIAL_WEAPONS`),
    `selectedSpecialItem` di default Mappa (`INITIAL_SPECIAL_ITEMS
    .first()`) — entrambi restano liberamente cambiabili con un tocco.
  - `heroIcon` era già WOLF di default, invariato. Discipline NON
    precompilate, come richiesto esplicitamente — restano l'unica
    scelta obbligatoria manuale (`canProceed` ora dipende quasi solo
    da quella).
  - **Statistiche tirate SUBITO all'apertura** (`init { rollStats() }`
    in `CreationState` — costruita una volta sola per apertura della
    pagina, `remember` in `CreationRoute`): il bottone "Tira le
    Statistiche" NON si disabilita più dopo il primo tiro ("si deve
    divertire ma col giusto livello di aleatorietà" — richiesta
    esplicita di non limitare i ritiri), etichetta che cambia in
    "Ritira le Statistiche" quando non è più il primo tiro.
  Compilazione e suite riverificate verdi. **Ancora da confermare sul
  device.**

**RUN PIÙ LUNGO CON TTS+MUSICA ATTIVI** (22/07, Michele: "finita 3
volte, sfruttati anche i salvataggi, TTS abilitato, anche musica, il
cel scalda un po' ma il mio è un foldable quindi è normale"): 16
generazioni in ~12' (3 partite complete), nessun errore/crash nel log —
conferma che il fix del player musicale a scope applicazione regge
senza side-effect. Due dati:
- **Memoria nativa**: 1084MB -> 1506MB in 12' — coerente col leak da
  ~140MB/partita già noto e rimandato consapevolmente, nessuna novità.
- **Velocità**: alterna ~19 token/s (boost del SoC dopo una pausa) e
  ~12-13 token/s (regime normale), stesso pattern già documentato il
  20/07. Ma l'ULTIMA generazione della sessione segna **9,7 token/s**,
  il valore più basso mai registrato finora — con TTS+musica attivi
  insieme a Gemma per 12' continui (carico CPU aggiuntivo oltre alla
  GPU), potrebbe essere il primo segno di throttling termico
  cumulativo. Non ancora confermato: serve un run ancora più lungo
  (30-45') per dire se è un trend reale o rumore di una singola
  misura.

**Come si raccolgono le misure**: il motore le logga da solo a ogni
scena giocata. `adb logcat -s LiteRtLmEngine` stampa una riga
`MISURA backend=… primoToken=… tokenPrompt~… tokenGenerati~…
velocita~… token/s`. Basta giocare e leggere.

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
  segnaposto; `ic_axe`, `ic_map_icon` e `ic_gold` pesano 3-4 MB l'una
  (conversione WebP in Fase 7).

**Decisioni in attesa di Michele**:
1. Il `TagParser` previsto in Fase 4 si salta? (in Ex non avrebbe nulla
   da parsare: le meccaniche arrivano già strutturate dal JSON)
2. Le rifiniture UI di Fase 3 che voleva elencare
3. Bonus dello scudo, se lo si vuole come oggetto iniziale
4. Download del modello: ora vincolato al solo Wi-Fi

**Idee rinviate**: stanno in `doc/UPGRADE.md` (audio narrativo, ecc.).
NON sono schedulate: non implementarle senza una decisione esplicita.

---

## 20/07/2026 (sera)

### Sessione — quattro bug sotto lo stesso sintomo "non funziona"

Michele: *"lo premo ma sembra che non funzioni esattamente"* riferito ai
salvataggi. Sono usciti QUATTRO problemi distinti, ognuno mascherato dal
precedente — un buon promemoria che "non funziona" non è mai una
diagnosi, è un punto di partenza.

**1. La UI non vedeva `GameState` (`121e112`).** `GameState.session` è
un `var` normale — e DEVE restarlo, `:core:engine` non dipende da
Android — quindi Compose non lo osservava e nessuna mutazione faceva
ridisegnare nulla. Piazzavi un checkpoint: il file si scriveva, il
contatore restava fermo. Bevevi una pozione: la Resistenza non cambiava
finché non si cambiava scena (lì `currentScene`, quello sì osservabile,
forzava il ridisegno). Non una regressione: c'era dalla Fase 3.
`AdventureState` ora tiene una copia osservabile (`hero`,
`checkpointsRemaining`) risincronizzata da `rinfresca()` dentro
`autoSave()`.

**Nella stessa sessione, la regola dei checkpoint è cambiata** (decisione
Michele): prima si ricaricava un checkpoint **illimitatamente** — due
piazzamenti bastavano a rendere l'avventura innocua, si moriva quante
volte si voleva tornando sempre allo stesso punto. Ora **ricaricare
consuma il checkpoint** (nuovo `SessionStore.deleteCheckpoint`), e chi
resta senza vite muore per davvero: sessione cancellata come in IRON,
che diventa il caso limite della stessa regola invece di un'eccezione.
`STATO.md` Blocco 2 aggiornato.

**2. Non esisteva NESSUN riscontro né via d'uscita (`8ba5e75`).**
Piazzare un checkpoint non dava nessun segnale (ora: spunta verde
"Checkpoint salvato" per 1,5s). E non c'era alcun modo di tornare al
menu dalla scena — `onExitToHome` era già un parametro ma usato solo a
fine avventura. Aggiunta icona Home nell'header, con conferma (l'auto-
save è sempre attivo, la conferma è solo contro il tocco accidentale).

**3. L'icona Home era invisibile, non rotta (`c10e68a`).** Michele ha
mandato uno screenshot: l'header finiva con "Scena 1 ·", un puntino
tagliato al bordo destro. Il titolo del libro non aveva limite di
larghezza; `Arrangement.SpaceBetween` non comprime i figli che eccedono
lo spazio, semplicemente trabocca oltre il bordo. Con un titolo
abbastanza lungo ("The Warehouse Letter"), Diario/Scena/Home uscivano
letteralmente dallo schermo — non impremibili, invisibili. Fix: il
gruppo di sinistra ha `weight(1f)` e il titolo tronca con ellipsis; il
gruppo dei controlli mantiene sempre la sua dimensione ed è sempre
intero.

**4. I checkpoint di test bloccavano i piazzamenti nuovi, in silenzio
(`b67b9fb`).** Dopo il fix 1, Michele: *"anche se fai click rimasti
2"* — il contatore non scendeva MAI. Causa: `saveCheckpoint()` rifiuta
di scrivere se lo slot esiste già (per design: scritto una volta, mai
sovrascrivibile). Con decine di partite di test giocate oggi sullo
stesso `packageId`, i file `checkpoint_sample_1.json` e `_2.json` erano
già occupati da sessioni precedenti — nessuno li ripuliva mai quando si
creava una nuova avventura. `SessionStore.deleteAdventure` esisteva
apposta per questo (il suo commento lo dichiara: "i checkpoint di una
partita finita non devono sopravvivere alla successiva"), semplicemente
`CreationRoute` non lo chiamava. **Non sblocca la sessione già in corso**
— "Continua" salta apposta `CreationRoute` — serve iniziare una nuova
avventura.

**Confermato da Michele su device solo il fix 4** ("questo bug sembra
risolto"). I fix 1-3 sono stati scritti e testati (JVM + compilazione)
ma non ancora rivisti sul Razr dopo l'ultimo aggiornamento — la sessione
di test si è chiusa sul quarto problema, non è tornata indietro a
confermare i primi tre.

**Altro toccato nella stessa sessione, su richiesta di Michele:**
- **Testo del finale**: conclusivo ma con "un filo aperto" — non
  promette un seguito, non offre una scelta. Vincolo dedicato perché
  quello normale ordina di "riscrivere la CURRENT SCENE", contraddittorio
  per un finale da inventare (trovato dal test, non dalla lettura).
- **Enfasi soprannaturale delle Discipline Kai** nel prompt
  (`disciplineEmphasisText`), con il limite esplicito di non inventare
  effetti oltre il testo sorgente.
- **Descrizioni delle discipline nella scheda**: mostrava l'ID canonico
  grezzo ("MINDBLAST") mentre nome e descrizione italiani erano in
  `strings.xml` da sempre, mai collegati.
- **Le tavole a china di Michele** per i due finali (scheletri/sole
  nascente), sostituiscono i vettoriali di ripiego. Taglio a metà
  misurato sulla densità di pixel scuri, non a occhio. WebP lossless
  perché il lossy sporcava il tratteggio.
- **41 immagini di locazioni/NPC/nemici** catalogate, rinominate in
  inglese, testo italiano rimosso da 9 (dove stava dentro una targa —
  gli archi con testo inciso sulla pietra non si sono potuti pulire).
  In `drawable-nodpi`, non `drawable`: altrimenti Android le scala fino
  a 4× su schermi xxxhdpi. **Ancora nessun codice le usa.**

**Il Diario di Combattimento.** Michele ha fotografato due pagine del
registro cartaceo ufficiale: la prima (scheda personaggio) era il
riferimento sbagliato, corretto subito dopo con la seconda — il
"Diario di Combattimento" vero, quello che si compila round per round
con RES/CS di entrambi i contendenti e il Rapporto di Forza al centro.
Tre domande chiuse prima di scrivere codice (il pannello sostituisce
tutto o si affianca? il dado gira con animazione o secco? resta aperto
fino alla fine o torna alla vista attuale a un certo punto?) — le
risposte hanno deciso l'architettura: un pannello UNICO dal primo
colpo al riepilogo finale, mai un salto visivo.

Un dettaglio tecnico ha deciso l'ordine delle operazioni:
`CombatSession.fightRound()` è SINCRONO — tira e applica i danni in un
solo colpo. Se il dado avesse chiamato quella funzione al TOCCO,
Resistenza e Combattività sarebbero cambiate mentre il dado stava
ancora girando, rovinando l'effetto. Soluzione: il tiro vero parte
SOLO a fine animazione (`TenSidedDie.onRoll`, chiamato dopo il loop di
rotazione); i numeri mostrati durante il giro sono scenografia pura,
non anticipano nulla.

`combatFightRound()` è stata cambiata da `Unit` a `RoundResult?` (unico
chiamante, nessun rischio) perché il dado ha bisogno del tiro uscito
per fermarsi sulla faccia giusta.

Il dado è finito in un file suo (`TenSidedDie.kt`, non `internal`,
pubblico) invece di restare dentro il pannello: `CombatDiaryPanel.kt`
aveva superato le 200 righe, e il dado è un componente autonomo che
probabilmente tornerà utile quando Fase 7 costruirà l'overlay generale
del Dado del Destino per gli altri tiri (skillCheck, creazione,
`randomChoiceTable`).

---

## 20/07/2026 (pomeriggio)

### Sessione — l'attesa raccontata e la card che si legge a colpo d'occhio

Due lavori di UI, entrambi chiesti da Michele il 19/07. Nessuno dei due
tocca il motore: la suite era e resta verde, e il comportamento con LLM
assente non cambia di una riga.

**1. L'animazione del narratore che pensa.** In `origina_res` non c'era
NULLA di animato — solo immagini statiche, nessun Lottie, nessuna
sequenza di frame. Chiesto a Michele prima di inventare, come da
istruzioni: ha scelto di animare il ritratto che c'è già invece di
aspettare un asset da produrre. Quindi zero file nuovi.

Due metà che si accendono insieme: nel banner l'alone d'oro attorno a
`portrait_dm` **pulsa** (da 1 a 4 dp, ciclo 900ms) invece di restare
fisso; nel blocco testo tre puntini si accendono in sequenza, sfasati di
un terzo di ciclo, così l'onda va da sinistra a destra invece di far
lampeggiare i tre insieme.

**La parte che conta davvero è la distinzione dei due momenti**, che
durano molto diversamente: il caricamento del modello è secondi, una
volta per partita; la generazione della scena è breve. Con la stessa
frase sopra, l'attesa lunga della prima volta sembra un blocco. Ora
`AdventureState.isLoadingModel` li separa e la UI dice "Il narratore
apre il libro…" contro "Il narratore scrive…". Il flag si spegne sia
quando parte la narrazione sia in `narrationUnavailable()`: se il motore
non parte non resta acceso per sempre, stessa disciplina del resto.

L'alone pulsa solo finché `narrative` è vuoto: appena arriva il primo
pezzo di streaming torna fermo, altrimenti pulserebbe per tutta la scena.

**Nota di piano**: `UI.md` collocava questa animazione in Fase 7. È
stata anticipata su richiesta esplicita di Michele, e `UI.md` è stato
aggiornato di conseguenza — non è uno sconfinamento silenzioso.

**2. Le icone nella card di stato.** Era solo testo. Ora ha quello che
v1 mostrava a colpo d'occhio: ritratto-lupo tondo col bordo d'oro,
medaglia dorata del grado Kai, spada per la Combattività, cuore per la
Resistenza, monete per le Corone, più la riga delle discipline.

**Un conflitto di specifica, fermato invece che interpretato**:
`UI.md §Card di stato` diceva testualmente che le icone discipline
vivono nella scheda *e non nella card*, mentre Michele le chiedeva nella
card. Chiesto a lui: le vuole in entrambe (nella card sono solo icone,
a colpo d'occhio mentre si sceglie; nella scheda restano con nome e
descrizione). `UI.md` corretto con la nota del perché.

Nessuna icona disegnata da zero: le discipline riusano
`disciplineIcon()` della creazione — così il giocatore le riconosce da
dove le ha scelte — `ic_sword` era già in uso, `ic_gold` e
`lupo_solitario` arrivano da `origina_res`.

La card è uscita da `AdventureScreen` e ha preso un file suo: lo schermo
era ben oltre la soglia d'allarme delle ~200 righe. `kaiRankName` è
passata da `private` a `internal` per non duplicare il grado localizzato.

### Le PRIME MISURE REALI (log di Michele, 20/07 ore 11:12-11:14)

Michele ha installato e giocato tre scene, poi ha passato il logcat.
**I due lavori di stamattina girano sul device.**

| scena | primo token | totale | prompt | generati | decode |
|-------|-------------|--------|--------|----------|--------|
| 1     | 1,62 s      | 10,68 s| ~552   | ~180     | 19,9 tok/s |
| 2     | 1,43 s      | 16,16 s| ~689   | ~197     | 13,4 tok/s |
| 3     | 1,88 s      | 18,47 s| ~775   | ~204     | 12,3 tok/s |

**Primo token 1,43-1,88 s su GPU: SOTTO la soglia di 3 s di
CRITICITA.md.** È il numero che chiudeva la Fase 4, e passa con margine.
Il backend confermato GPU in tutte e tre (`MainExecutorSettings:
backend: GPU`, delegate LITERT_CL su tutti i 2712 nodi).

**Caricamento del modello: ~9,0 s** (11:13:12,8 -> 11:13:21,8 "Modello
caricato su GPU"). L'animazione fatta stamattina non era un vezzo:
nove secondi con una scritta ferma sembrano un blocco.

### RISOLTO in giornata: il bug dell'accumulo NON ESISTE

Michele ha giocato **3 partite di fila senza chiudere l'app**, 15
generazioni, con la strumentazione nuova. I dati smentiscono l'ipotesi
qui sotto — la lascio scritta perché il ragionamento che portava a
sbagliare è istruttivo.

**1. Le conversazioni si chiudono**: `vive=1` in tutte e 15 le
generazioni, `chiusureFallite=0`. Il `close()` sospettato non c'entra.

**2. Non è un degrado progressivo, è un GRADINO**. Rigiocando le stesse
scene (prompt IDENTICI, quindi confronto pulito):

| prompt | giro 1 | giro 2 | giro 3 |
|--------|--------|--------|--------|
| ~552   | 18,4   | 12,5   | 12,0   |
| ~689   | 18,6   | 12,4   | 12,5   |
| ~867   | 12,2   | 12,4   | 12,4   |
| ~505   | 12,2   | 12,2   | 12,3   |

Dopo le prime due generazioni la velocità si assesta a **12,1 token/s e
ci resta per 13 generazioni**. Il giro 3 va come il giro 2.

**Le anomale sono le prime due, non le altre**: il SoC parte in boost di
frequenza e poi scende al regime sostenibile. Questo spiega anche le
misure di stamattina — quel "19,9 -> 12,3" che leggevamo come degrado
era lo stesso fenomeno, e "riavviare l'app resetta la velocità" voleva
solo dire "rimette il telefono in boost".

**La velocità vera del Razr su Gemma 4 E4B è 12 token/s.** I 18,5 dei
primi secondi non sono una velocità che perdiamo: sono un transitorio.

**3. La strumentazione ha però trovato un problema DIVERSO e reale**: la
memoria nativa cresce di **~140 MB a ogni PARTITA** (1086 -> 1226 ->
1365 MB), non a ogni generazione. Il salto avviene quando si torna alla
Home e si ricomincia. Causa ignota: l'engine non viene ricreato e le
conversazioni si chiudono. **Michele ha deciso di rinviarlo**: su
15,5 GB non si sente, le prestazioni sono decenti e il calore
accettabile. L'ottimizzazione si fa alla fine.

**Lezione di metodo**: l'ipotesi era ragionevole (rallenta solo il
decode, non il prefill -> qualcosa si accumula) ed era sbagliata. A
salvarla non è stato ragionare meglio ma **misurare**: tre righe di
contatori nel log hanno chiuso in una giocata una questione che
sarebbe rimasta aperta per sessioni.

---

### Il sospetto sul decode — SMENTITO, si tiene per memoria del metodo

La velocità di decode **scende del 38% in tre scene** e l'attesa reale
per scena sale da 10,7 a 18,5 s. Michele riferisce **telefono freddo**:
non è throttling.

Il dettaglio che rende poco convincente la spiegazione ovvia: il prompt
cresce (552 -> 775, +40%) ma **il primo token NON peggiora** (1,62 /
1,43 / 1,88 = rumore). Il prefill è la fase che dipende dalla lunghezza
del prompt e sta benissimo; **rallenta solo il decode**. Se fosse
"contesto più lungo" peggiorerebbero entrambi. Punta a qualcosa che si
ACCUMULA tra una generazione e l'altra, a temperatura costante.

Sospetto concreto in `LiteRtLmEngine.newSession()`: fa
`runCatching { conversation?.close() }` e **scarta l'esito**. La
Conversation nuova si crea comunque. Se `close()` fallisce, le
conversazioni e la loro KV cache sulla GPU si accumulano in silenzio e
noi non lo sapremmo: quell'errore non viene loggato da nessuna parte.

**Tre campioni non bastano.** Prossimo passo deciso con Michele: ~10
scene di fila. Se la velocità si stabilizza è il contesto; se continua
a scendere si accumula qualcosa ed è un bug del motore, che viene
prima di tutto il resto.

### Altro emerso dai log (non toccato)

- **Sampler OpenCL assente**: `libLiteRtTopKOpenClSampler.so` non
  trovata, ripiego sull'API C statica. Il modello sta su GPU ma il
  campionamento no — costo plausibile su ogni token generato.
- **187 frame saltati, 2,1 s di UI congelata** (`Davey! duration=2116ms`)
  durante il caricamento del modello: **l'animazione dei puntini si
  inchioda proprio nei secondi in cui deve girare**. Difetto diretto
  del lavoro di stamattina, da sistemare.
- **NPU non configurata**: `DispatchLibraryDir` mancante, l'NPU non si
  registra. Per l'SM8750 esiste una build dedicata: opportunità, non
  problema.
- "Image decoding logging dropped!" a raffica = i PNG da 3-4 MB.

### Un'avventura finisce sempre dichiarando com'è andata

Nata da una frase di Michele dopo la prova: *"si è chiusa, non ho capito
se è andata bene"*. **Non era un crash**: il sample ha 7 scene, la 6 e la
7 sono ENDING, quindi aveva semplicemente finito il libro. Il difetto era
che la schermata di finale **non diceva l'esito** — mostrava solo "Torna
alla Home", tranne che alla morte in IRON.

Scavando sono usciti altri tre modi di lasciare il giocatore senza
finale, tutti contro il vincolo "il gioco non si blocca mai":
- manifest senza `deathSceneId` -> la morte built-in NON era attiva: si
  continuava a giocare con Resistenza <= 0;
- sconfitta in combattimento senza `loseSceneId` né `deathSceneId` ->
  `?: return`, il giocatore restava fermo nella scena persa;
- `sceneById` usava `.first{}` -> un id inesistente (grafo rotto,
  sessione di un libro poi cambiato) chiudeva il gioco con un'eccezione.

Decisioni prese con Michele (le tre alternative gli sono state
sottoposte, non interpretate):
- **`Scene.outcome`** (VICTORY|DEFEAT|NEUTRAL) lo dichiara l'AUTORE. Il
  motore non indovina: un finale amaro raggiunto vivi e una vittoria si
  somigliano troppo. Unica deduzione ammessa: la morte built-in, che
  BATTE la dichiarazione. Assente = NEUTRAL.
- **`AdventureEnding.withGuaranteedEnding`** fabbrica la scena di morte
  quando manca e la aggiunge al grafo, così il resto del motore lavora
  su un manifest dove `deathSceneId` punta sempre a qualcosa di vero.
- **Il finale fabbricato lo scrive Gemma** (scelta di Michele), con il
  testo fisso di `strings.xml` sotto: il vincolo di degradazione resta.

Regola in `:core:engine` con **10 test JVM**. `REGOLE.md` §2.2-bis.

**Il vincolo del prompt l'ha trovato un test, non la lettura**: il
`constraintText` normale ordina di "riscrivere la CURRENT SCENE", che
per un finale da inventare è contraddittorio. È servito un
`syntheticEndingConstraintText` dedicato.

### Le quattro richieste successive di Michele

1. **Testo del finale**: conclusivo ma con "un filo aperto" — possibilità
   di continuo senza promettere un seguito. Genere e tono erano già nel
   prompt; il vincolo ora insiste che suoni come QUESTA avventura.
2. **Immagini dell'esito**: prima due VectorDrawable disegnati a mano,
   poi **sostituiti dalle tavole a china fornite da Michele** (scheletri
   / sole nascente). Taglio a metà misurato sulla densità di pixel scuri
   (banda bianca esatta: colonne 500-529), non a occhio. **WebP
   lossless** perché il lossy sporcava i bordi del tratteggio: 160 e
   149 KB. Fondo bianco e cornice TENUTI: è la tavola stampata nella
   pagina, come nei gamebook.
3. **Descrizione delle discipline** nella scheda. Qui c'era di peggio
   della richiesta: **la scheda mostrava gli ID canonici GREZZI**
   ("MINDBLAST") mentre nome e descrizione italiani erano in
   `strings.xml` da sempre, mai collegati — contro il vincolo "nomi
   localizzati solo in strings.xml".
4. **Enfasi soprannaturale** delle Discipline Kai nel prompt
   (`disciplineEmphasisText`), con il **limite esplicito** di non
   inventare effetti oltre il testo sorgente: l'enfasi sta nel racconto,
   non nelle meccaniche, altrimenti il narratore contraddice le regole.
   Si spende solo se la scena ha davvero una disciplina in gioco.

**Nulla di tutto questo è stato visto girare**: compila, la suite è
verde, le preview mostrano i disegni. Il percorso "muori in un libro
senza deathSceneId" non l'ha ancora eseguito nessuno.

**Cosa NON sappiamo ancora**: il **termico**. Il log copre **1 minuto e
54 secondi**; CRITICITA.md chiede 30-45'. "Telefono freddo" dopo due
minuti è atteso, non è il dato. Mancano ancora le **fixture** da output
reali di Gemma.

**Debito nuovo, piccolo**: `ic_gold.png` pesa 3,2 MB — stessa storia di
`ic_axe` e `ic_map_icon`, tutti da convertire in WebP in Fase 7.

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