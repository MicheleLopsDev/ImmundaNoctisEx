# Proposte di upgrade dell'app

**Cosa NON è questo documento**: non è un piano e non è un impegno.
Niente di quanto sta qui è schedulato in `doc/PIANO-SVILUPPO.md`; niente
di qui si implementa senza una decisione esplicita di Michele.

**Cosa è**: il posto dove le idee emerse durante lo sviluppo vengono
scritte per bene invece di essere perse o, peggio, infilate di soppiatto
in una fase in corso. Ogni voce dice **cosa**, **perché**, **cosa
costerebbe** e soprattutto **cosa va verificato prima**.

Distinzione utile: alcune voci sono già **predisposte** nel design
chiuso (i contratti reggono, manca l'implementazione), altre sono
**feature nuove** che allargherebbero il perimetro.

---

## 1. Audio narrativo: ambiente di scena ed effetti puntuali

**Origine**: idea di Michele (17/07/2026), discussa e progettata durante
la Fase 4. **Rinviata da Michele**: prima serve misurare Gemma sul
device (vedi §Rischio).

Sono **tre feature distinte** che convivono:

### 1c. Suono una tantum agganciato all'immagine risolta (FATTO, 22-27/07/2026)
Idea di Michele, molto più semplice delle due sotto: **non tocca
Gemma per niente**. `backgroundImage`/`enemyImage`/`npcImage` sono già
vocabolari chiusi, già risolti in modo affidabile
(`SceneImageCatalog`/`EnemyImageCatalog`/`NpcImageCatalog`) — basta
riusare LO STESSO id per pescare un file audio con lo stesso nome
(`loc_market.mp3`), un colpo secco (SoundPool, non loop), riprodotto
insieme all'immagine, senza toccare la musica di sottofondo. Nessun
campo nuovo su `Scene`, nessun rischio di vocabolario extra nel
prompt. Checklist degli asset da produrre: `doc/SUONI-IMMAGINI.md`
(restano alcuni file loc_*/enemy_*/npc_* non urgenti, degrado
silenzioso già garantito e verificato).

### 1a. Ambience di scena (tappeto sonoro)
Un campo opzionale `ambience` sulla scena (simmetrico a
`backgroundImage`), es. `tavern_crowd`, `rain_storm`, `dungeon_drip`.
Loop di sottofondo per tutta la scena.

- **Chi lo genera**: il **rifinitore LLM dell'ETL** (Fase 6), che già
  produce `locationName` e `toneHints`. Costo runtime ZERO, risultato
  deterministico tra le rigiocate, e passa dalla review
  human-in-the-loop già prevista.
- **Impatto**: un campo opzionale su `Scene` (default null, nessun
  salvataggio rotto) + il player audio.

### 1b. Effetti puntuali inline (il pezzo che solo l'LLM può fare)
Marcatori dentro la prosa generata: la porta cigola *in quel punto del
testo*. Es. `[[sound:door_creak]]`, e col tono comico la stessa porta
può diventare `[[sound:crowd_laugh]]`.

- **Chi li genera**: **Gemma a runtime**, ed è l'unico modo possibile —
  non perché l'ETL non conosca il tono (`toneHints` ce l'ha), ma perché
  **la prosa su cui ancorarli non esiste finché il modello non la
  scrive**. Gemma riscrive la scena ogni volta con parole e ordine
  suoi: solo lei sa dove, nella *sua* versione, la porta cigola.
- È anche l'argomento di Michele a favore: l'inferenza la stai già
  pagando in latenza, tanto vale estrarne più valore.

### Regole di formato (decise, se e quando si farà)
1. **Delimitatore `[[sound:id]]` doppio.** Le parentesi SINGOLE sono già
   usate dal prompt come marcatori di sezione (`[THE STORY SO FAR]`,
   `[CURRENT SCENE …]`): chiedere `[sound:x]` mentre si mostrano
   `[SEZIONE]` invita il modello a confondere i due livelli.
2. **Mai il carattere `|`.** La regola 4 del prompt vieta le pipe nella
   prosa proprio per proteggere il parser delle scelte: i tag audio non
   devono romperla.
3. **Vocabolario CHIUSO**: il modello sceglie tra ID che esistono
   davvero, come per le discipline. ID sconosciuto = **silenzio**, mai
   un errore (il gioco non si blocca mai).
4. **Parsimonia esplicita**: al massimo 2 effetti per scena, solo quando
   l'effetto è ovvio. Meglio zero suoni che una prosa peggiore.
5. **Nel diario si salva il testo GIÀ RIPULITO.** I marcatori sono
   istruzioni di riproduzione effimere, non parte della storia:
   `JourneyEntry.enrichedText` non deve contenerli, altrimenti
   spuntano nel Racconto e nell'export Markdown.

### Rischio che ha motivato il rinvio (parole di Michele)
Istruire Gemma su **tutto** il vocabolario possibile è istruzione che
compete con il compito principale (tradurre e arricchire bene). Gemma 3
4B è piccola e in v1 già sbagliava i formati. **Non si decide prima di
aver misurato**: la milestone della Fase 4 produce i numeri (primo
token, token/s, prompt token, termico). Con quelli in mano si sa quanto
output extra ci si può permettere; senza, è una scommessa.

**Aggiornamento 20/07/2026**: primo token (1,43-1,88 s su GPU) e
velocità (12,1 token/s stabile) sono misurati — vedi DIARIO.md. Restano
**termico su 30-45'** e **drain della batteria**: Michele ha scelto
esplicitamente di aspettare quei due prima di riconsiderare la musica
("meglio saperlo prima di investirci lavoro"). La condizione del rinvio
resta quindi ANCORA APERTA, solo più vicina alla chiusura.

### Costo stimato se approvata (tracciato sul codice reale)
Additivo quasi ovunque: frammento in `config.json` + `PromptFragments` +
sezione in `PromptBuilder` (~15 righe totali, stesso schema di
`enemyFormatText`); 1 campo su `Scene`. L'**unica modifica vera** è in
`ResponseParser` (~30 righe + test): `narrativeOf()` e `parse()` devono
estrarre i marcatori e restituire la prosa ripulita. Servono poi i file
audio e un player (roba di Fase 7).

---

## 2. Reskin grafico ispirato al registro cartaceo di Lupo Solitario

**AGGIORNAMENTO 27/07/2026 (Michele)**: "tutto l'aspetto grafico è
funzionale... le font sono state scelte e sono anche belline". Questa
sezione è da considerarsi sostanzialmente CHIUSA: font agganciate
(`ReadingFont`, Opzioni), location e decorazioni sistemate, sfondi
chiaro/scuro estesi a tutte le schermate di menu (26/07). Restano solo
le 24 location fotografiche più vecchie da rifare nello stesso stile
china/Kai (non urgente, lo dice lo storico sotto) e qualche
decorazione opzionale mai agganciata a uno schermo specifico — nessuna
di queste blocca nulla.

**Origine**: Michele (22/07/2026) ha mandato le foto delle 4 pagine del
registro ufficiale (Diario di Combattimento, Zaino/Borsa/Pasti/Oggetti
Speciali, Combattività/Resistenza/Armamento, Registro di Guerra) e
vuole farle procurare dalla figlia, grafica di professione.

**Cosa**: pergamena invecchiata, bordi a carta strappata, font
gotico/medievale, icone a china per le armi — lo stile del registro
cartaceo, applicato ai pannelli che oggi sono Material3 piatto (scheda
personaggio, Diario di Combattimento, zaino). **Non è una copia**: le
illustrazioni e il logo "LONE WOLF" del registro sono di Mongoose/Joe
Dever, protetti — si prende lo STILE (pergamena, china, bordi
strappati), non il contenuto esatto.

**Due modifiche più economiche già fatte SENZA asset nuovi** (22/07):
`CombatDiaryPanel` mostra il paragrafo/scena in testa e il Rapporto di
Forza in un riquadro bordato; la scheda personaggio scompone
Combattività/Resistenza in Base + Modificatori. Questa voce riguarda
il resto: il vero reskin visivo, che serve asset veri.

### Lista asset per la grafica (quello che Michele ha chiesto di preparare)

**Font**
- Un font "da titolo" gotico/medievale — SOLO per intestazioni, non
  per il testo lungo da leggere (un font decorativo su un paragrafo
  intero di prosa diventa illeggibile).
- Formato `.ttf`/`.otf`, deve includere le lettere accentate italiane
  (à è é ì ò ù). Se serve un peso Bold, meglio un file a parte.
- Licenza libera per uso in app se è un font esistente scelto da lei;
  nessun problema se lo disegna.

**Texture di sfondo (pergamena)** (file Texture di sfondo) — **ASSET
PRONTO, NON ANCORA AGGANCIATO A UNO SCHERMO** (22/07): riesportata da
Michele con sfondo bianco, sfondo rimosso qui (flood-fill a range
fisso) → `res/drawable/parchment_panel.png`. Non è tileable (il file
ha i bordi strappati DISEGNATI su tutti e 4 i lati, un pannello unico
già completo, non un pattern da ripetere) — più comodo così: si usa
com'è come sfondo di una card, non serve calcolare una ripetizione.
**Decisione da prendere prima di agganciarlo**: l'app gira quasi
sempre in tema scuro con testo chiaro — su un fondo di pergamena
chiaro il testo chiaro diventerebbe illeggibile. Serve decidere se
forzare un colore d'inchiostro scuro sopra la pergamena (a
prescindere dal tema) o un'altra soluzione, prima di applicarlo a
qualunque pannello vero.
- Un'immagine "pergamena invecchiata", **tileable** (si ripete senza
  cuciture visibili ai bordi) — più robusta di un'immagine fissa
  perché funziona su qualunque dimensione di schermo.
- **2048×2048 px** (minimo accettabile 1024×1024 px): sul tile piccolo
  la grana si vede se il pannello è grande, meglio abbondare.
- Formato PNG (texture fotografiche/grana fine comprimono meglio in
  PNG che in WebP con artefatti visibili); converto io in WebP dopo.
- Pensata per **tema scuro** (l'app gira quasi sempre in dark mode):
  va bene anche solo per la notte, il giorno è secondario.

**Cornice a bordi strappati**
- Bordo decorativo "carta strappata" da mettere intorno alle card. Il
  formato più comodo per Android è un **9-patch**; se non lo conosce,
  l'alternativa più semplice è **una striscia orizzontale tileable**
  (si ripete solo in larghezza, non in altezza) da mettere sopra e
  sotto un pannello — **1080×150 px**, trasparente tranne il profilo
  strappato.
- Se invece fa il 9-patch vero, la converto io da qualunque dimensione
  di partenza ragionevole (es. 400×400 px), è lei a scegliere lo stile
  del bordo, non la dimensione esatta.
- PNG con trasparenza (canale alpha).

**Icone per arma (9 pezzi)**(file Icone per arma.png) — **FATTO
22/07**: `ic_dagger`/`ic_spear`/`ic_mace`/`ic_short_sword`/
`ic_warhammer`/`ic_sword`/`ic_axe`/`ic_staff`/`ic_broadsword` in
`res/drawable/`, agganciate in `CreationCatalog.weaponTypeIcon` —
sostituite le sei di v1, riempite le tre mancanti (segnaposto
`ic_unknown_item` non serve più per queste). Il file consegnato aveva
lo sfondo a scacchi disegnato nei pixel (non trasparenza vera) — Michele
l'ha riesportato con sfondo bianco pieno, rimosso qui con un flood-fill
a range fisso (dettagli in DIARIO.md). Non ancora vista girare sul
device.
- Asta, Spada, Daga, Martello, Pugnale, Lancia, Spadone, Ascia, Mazza
  — stile china/silhouette, come nel registro.

**Arti marziali, Elmo, Corazza** (file Decorazioni.png, insieme ad
altre già pronte in precedenza) — **FATTO 24/07/2026**: `ic_fists.png`
(gesto "karate chop" al posto del pugno chiuso di v1), `ic_helmet.png`
(elmo con muso di lupo), `ic_armor.png` (gilet di maglia) ritagliati e
sfondo rimosso qui, stesso nome file di prima — nessun codice da
toccare, `CreationCatalog`/`INITIAL_SPECIAL_ITEMS` li puntavano già.
**Mai visto girare sul device.**

**Decorazioni opzionali (non urgenti)**( file Decorazioni opzionali.png )
— **ASSET PRONTI, NON ANCORA AGGANCIATI A UNO SCHERMO**: `res/drawable/`
ha `deco_backpack`/`deco_gold_pouch`/`deco_meal`/`deco_travel_gear`/
`deco_potion`/`deco_combat_emblem`/`deco_arcane_medallion`/`wolf_logo`
(sfondo rimosso, stessa tecnica delle armi) più `ic_map_icon`
sostituita nello stesso stile. Manca la decisione di DOVE usarli
(quale schermo, quale sezione) — nessuna UI li mostra ancora.
- Zaino, pozioni, borsa/corone, pasto, mucchio di equipaggiamento —
  accenti piccoli vicino alle rispettive sezioni.
- Spade incrociate con la testa di lupo (per il riquadro del Rapporto
  di Forza in combattimento) e medaglione arcano occhio+libro
  (decorazione per il Registro discipline).
- Un'illustrazione del lupo per il logo dell'app — ispirata, non
  quella ufficiale Lone Wolf: `wolf_logo.png`, pronta.
- Tutte danno personalità ma non sono indispensabili: si può partire
  senza e aggiungerle quando ci sono.

**Icone eroe — 14 animali (oltre al lupo già fatto)** (file `icone per
personaggi.png`) — **FATTO 24/07/2026**: consegnato un foglio unico
5×3 con tutti e 14 gli animali (più un lupo di riferimento nello
stesso stile); ritagliato e sfondo rimosso qui (stesso flood-fill già
collaudato), agganciato in `CreationCatalog.heroIconRes` — la scelta
è selezionabile in creazione personaggio e mostrata nella card di
stato (`HeroIcon` in `:core:data`). **Mai visto girare sul device.**
- Falco, Aquila, Orso, Volpe, Corvo, Gufo, Leone, Tigre, Pantera,
  Lince, Cinghiale, Cervo (con corna), Serpente/Vipera, Drago —
  tutte e 14 pronte, `res/drawable/hero_*.png`.
- Il foglio aveva anche un lupo nello stesso stile "ombreggiato"
  degli altri 14: **sostituito** `lupo_solitario.png` su richiesta
  di Michele ("uniforma il tutto") — tutte e 15 le icone sono ora
  nella stessa famiglia visiva. Stesso file, quindi anche la faccia
  zero del Dado del Destino (`TenSidedDie`) eredita il lupo nuovo.

**Sfondi di scena (`loc_*`) mancanti — 11 location nuove** (24/07/2026)
— **TUTTE E 11 FATTE 24/07/2026**: emerse mentre si agganciavano i
suoni location (`doc/SUONI-IMMAGINI.md`) — Michele ha consegnato le
immagini (china/Kai, sfondo bianco, 1024×572, esattamente come
richiesto) per `loc_abandoned_keep`, `loc_ancient_ruins`,
`loc_battlefield`, `loc_dungeon`, `loc_haunted_house`, `loc_swamp`,
`loc_temple`, `loc_volcano`, `loc_waterfall`, `loc_wizard_cove`,
`loc_wizard_tower`: tutte ritagliate/salvate in
`res/drawable-nodpi/*.jpg` e agganciate in
`SceneImageCatalog.DESCRIPTIONS` + `sceneBackgroundRes`, stesso schema
delle 24 esistenti (che restano fotografiche, non ancora reskinnate).

- `loc_wizard_cove` non è quello che la descrizione originale
  ipotizzava ("insenatura/baia sul mare") — l'immagine consegnata è
  invece lo studio/laboratorio nascosto di un mago scavato nella
  roccia, scaffali di pozioni e libri, calderone sul fuoco, scala a
  chiocciola. Descrizione in catalogo corretta di conseguenza.
- `loc_temple`: la prima versione aveva un'iconografia cristiana
  esplicita (Cristo in trono, croce sul campanile) — segnalata a
  Michele, che l'ha sostituita con un "Tempio del Sole di Kai" (soli
  incisi, statue guardiane di leone e ariete, teschio animale sopra
  l'ingresso): coerente con l'ambientazione, agganciata.
- Le 24 location fotografiche esistenti restano da rifare nello stesso
  stile china/Kai per coerenza, quando ci sarà tempo (non urgente).

**Cosa NON chiederle**: copiare il logo "LONE WOLF" o le illustrazioni
specifiche del libro originale — quelle sono protette. Lo stile sì,
il contenuto esatto no.

**Pergamena dedicata al Diario di Combattimento** (24/07/2026) —
**RITIRATA lo stesso giorno**: proposta inizialmente (due immagini
dedicate, 1200×640px, senza scudi), ma prima ancora di commissionare
gli asset Michele ha deciso diversamente — "dalla card del combat
rimuovi lo sfondo... intendo nessuna pergamena e basta": il Diario di
Combattimento ora non ha NESSUNO sfondo, né Card Material3 né
pergamena, il contenuto sta direttamente sullo schermo. Di conseguenza:
`ParchmentBackground.kt` (la pila a tre fasce) è stato cancellato,
`ParchmentStyle.topRes`/`bottomRes`/`baseColor` rimossi dall'enum
(erano usati solo lì). Restano da ripulire, se si vuole, i drawable
ormai orfani `parchment_panel_top/bottom.png` e le versioni `_dark`
(non tolti per non allargare la modifica).

### Costo dell'integrazione (una volta pronti gli asset)
Meccanico ma esteso: font in `res/font` + riferimento nel tema Compose
per i soli titoli; texture come sfondo via `Modifier.background` sui
pannelli principali; bordo 9-patch o composizione di strisce; icone
armi come Vector Drawable (conversione da SVG) al posto delle
placeholder attuali. Tocca molte schermate — è un lavoro ampio ma
senza sorprese architetturali, nessun contratto da cambiare.

## 3. Motore alternativo: una libreria Kotlin per modelli GGUF

**Origine**: idea di Michele (22/07/2026), nata dopo il confronto tra
Gemma 4 E4B (qualità migliore, più lento) e 2B abliterated (più
veloce, testo peggiore — vedi DIARIO.md). **Rinviata da Michele
stesso**: "il motore di modello per adesso lo lasciamo selezionabile...
per adesso lasciamo così" — non è un rifiuto, è "non ora".

**Cosa**: oggi `InferenceEngine` ha una sola implementazione,
`LiteRtLmEngine` su `com.google.ai.edge.litertlm` (solo formato
`.litertlm`, GPU/CPU). L'idea è valutare se esiste una libreria Kotlin
matura per il formato **GGUF** (llama.cpp e derivati), che ha un
catalogo di modelli quantizzati molto più ampio — potrebbe includere
varianti più veloci del 2B senza il calo di qualità osservato.

**Perché potrebbe valere la pena**: `InferenceEngine` è già
un'interfaccia (una delle quattro motivate da CLAUDE.md/ARCHITETTURA):
un secondo motore si affiancherebbe senza toccare `SceneNarrator`,
`ResponseParser` o `PromptBuilder`, che parlano solo con
l'interfaccia. Il contratto regge già.

**Cosa va verificato prima di scriverne una riga**:
- Esiste davvero una libreria Kotlin/Android matura per GGUF (non solo
  binding JNI grezzi da mantenere a mano)? Con supporto GPU su
  Snapdragon, o solo CPU?
- I modelli GGUF disponibili per Gemma (o alternative) sono
  davvero più veloci a parità di qualità, o è lo stesso compromesso
  velocità/testo già visto col 2B abliterated, con un formato diverso?
- Due motori vuol dire due cataloghi di modelli, due UI di download,
  due set di parametri avanzati (temperatura/topK/topP potrebbero non
  mappare 1:1) — costo di manutenzione reale, non solo un file in più.

**RICERCA FATTA 27/07/2026** (richiesta di Michele, non ancora una
riga di codice): esistono librerie Kotlin mature, non solo JNI grezzi
da mantenere a mano —
[`kotlinllamacpp`](https://github.com/ljcamargo/kotlinllamacpp)
(binding Kotlin dedicati per Android, supporta anche modelli vision
con mmproj),
[`Llamatik`](https://github.com/ferranpons/llamatik) (Kotlin
Multiplatform — Android/iOS/Desktop/JVM/WASM — su
llama.cpp/whisper.cpp/stable-diffusion.cpp),
[`SmolChat-Android`](https://github.com/shubham0204/SmolChat-Android)
(riferimento di architettura: classe JNI su llama.cpp), oltre alla
guida ufficiale
[`llama.cpp/docs/android.md`](https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md).
Backend GPU su Adreno: llama.cpp ha un backend **OpenCL** dedicato
(oltre a Vulkan) [testato su Snapdragon 8 Gen 1/2/3 e 8
Elite](https://proandroiddev.com/introducing-the-new-opencl-gpu-backend-in-llama-cpp-for-qualcomm-adreno-gpus-4093655d334c) —
il Razr 60 Ultra (nome corretto del device di test, refuso "70 Ultra"
diffuso nei documenti fino al 27/07/2026, corretto ovunque lo stesso
giorno) monta esattamente uno **Snapdragon 8 Elite (Adreno 830)**,
quindi il backend accelerato è supportato sulla carta (Q4_0 è
la quantizzazione più ottimizzata su Adreno oggi; Q4_K_M indicato in
generale come miglior compromesso qualità/dimensione su telefono).
**Resta comunque da fare un prototipo concreto** che carichi un
modello GGUF quantizzato e misuri token/s + qualità di scrittura a
confronto diretto con Gemma 4B su LiteRT-LM, prima di qualunque
decisione.

**AGGIORNAMENTO 27/07/2026 — l'obiettivo NON è la velocità**: chiesto
a Michele l'incremento prestazionale atteso, la ricerca ha trovato che
**Google stessa dichiara LiteRT più veloce di llama.cpp**, sia su CPU
che su GPU, prefill e decode (benchmark su Gemma 3 1B, Galaxy S25
Ultra — [LiteRT: The Universal Framework for On-Device
AI](https://developers.googleblog.com/litert-the-universal-framework-for-on-device-ai/)).
Il motore GGUF probabilmente non renderebbe più veloce lo STESSO
Gemma. Ma Michele ha chiarito che l'obiettivo era sempre stato un
altro: **trovare il modello che scrive meglio in italiano** — Gemma 4B
a volte inventa parole inesistenti (es. "bruffi"), un problema di
fedeltà lessicale in fase di traduzione, non di velocità. Cambia la
domanda da porsi al prototipo: non "quanti token/s in più" ma "quante
parole inventate in meno, a parità di scena tradotta".

**Candidati con italiano nativo/forte trovati (27/07)**:
- **[Minerva 7B](https://nlp.uniroma1.it/minerva)** (Sapienza NLP) —
  il più promettente sulla carta: pre-addestrato DA ZERO (non un
  fine-tune) su 1,5 trilioni di parole italiano+inglese. GGUF non
  ancora verificato/trovato pronto.
- **[LLaMAntino-3-ANITA-8B](https://github.com/marcopoli/LLaMAntino-3-ANITA)**
  — Llama 3 8B instruction-tuned specificamente per l'italiano
  (progetto ANITA), **GGUF già disponibile** su Hugging Face da più
  fonti, pronto da provare.
- **Qwen3.5-4B** — generalista multilingue, stessa taglia del Gemma
  4B attuale (velocità comparabile, a differenza dei due sopra che
  sono 7-8B e quindi presumibilmente più lenti — accettabile sul Razr
  con 16GB RAM, dato che qui non si cerca velocità).

**Strada più economica da provare PRIMA di un secondo motore**: il
vincolo 1 del prompt oggi dice solo "non inventare nuovi eventi,
personaggi, oggetti" — non vieta esplicitamente di inventare PAROLE
che non esistono in italiano. Un ritocco di `PromptFragments`
testabile in un pomeriggio, senza toccare l'infrastruttura.

**Non schedulato**: nessuna azione finché Michele non porta una
libreria concreta da valutare, o decide di testare il ritocco del
prompt.

**CORREZIONE 27/07/2026 — il backend OpenCL/Adreno citato sopra NON è
quello che usiamo**: il ragionamento sul backend OpenCL dedicato di
llama.cpp per Adreno resta vero in generale, ma riguarda il progetto
llama.cpp compilato da soli — non Llamatik, la libreria scelta apposta
per EVITARE quella compilazione. Il README ufficiale di Llamatik
documenta `gpuLayers` come "-1 = all layers (Metal / CUDA)": il
backend GPU della libreria esiste solo su iOS/Desktop, su Android gira
sempre su CPU, qualunque valore riceva `gpuLayers`. Confermato da un
test reale di Michele su Gemma 3 12B Heretic (IQ4_XS): 237s al primo
token, 1,5 token/s — numeri coerenti con un 12B interamente su CPU con
pochi thread, non con un'accelerazione GPU mancata per un bug di
codice nostro. **Unica leva di velocità disponibile senza tornare a
compilare da soli**: `numThreads` che ora usa tutti i core meno 2
(prima fisso a 4) — non testato ancora sul device dopo questo
cambiamento. Se la velocità resta un problema reale (non solo
percepito), le strade restano: modelli più piccoli (i Gemma 3 4B già
in catalogo), un quant K invece di un quant I (le quantizzazioni IQ
sono spesso più lente da decomprimere su CPU a parità di dimensione),
oppure — unica via per la GPU vera — riaprire la porta della
compilazione nativa che Michele aveva scartato a inizio ricerca.

## 4. Altre proposte raccolte

### Già PREDISPOSTE nel design chiuso (i contratti reggono)
- **Effetti oggetto oltre `HEAL:n`**: il formato è dichiarativo ed
  estensibile senza cambiare schema (STATO.md §4.2). Già usato per
  `ENDURANCE:n` (Elmo/Gilet).
- **`requiredRank` sulle scelte**: gradi Kai oggi puramente cosmetici;
  se un libro volesse "serve grado X", si affiancherebbe a
  `requiredItem`/`requiredFlag` (REGOLE.md Blocco 3).
- **MINDSHIELD contro nemici psichici**: predisposizione concettuale,
  nessun campo riservato oggi (REGOLE.md §4.5).
- **Slot multipli di salvataggio**: `SessionStore` salva un file per
  pacchetto; slot multipli cambierebbero solo il nome file
  (STATO.md §1.2).
- **Compagni di viaggio**: il ruolo `COMPANION` esiste già nel
  `Character` unico e la funzione di round è simmetrica; servirebbe la
  seconda inferenza per i commenti, posticipata per design (UI.md).
- **Altri regolamenti oltre Lupo Solitario**: CRT dentro le regole e non
  nel motore, effetti dichiarativi, difficoltà esterna alle regole —
  adattarlo toccherebbe le implementazioni, non i contratti
  (STATO.md §Estensibilità).

### FEATURE NUOVE (allargano il perimetro)
- **Scambio a inventario pieno**: oggi l'oggetto oltre soglia non entra
  in silenzio (STATO.md §4.1). v1 aveva un `InventoryFullDialog` che
  chiedeva cosa scartare: pattern già analizzato e riusabile.
- ~~**Tono narrativo scelto dall'utente**~~ — **FATTO** (21/07/2026,
  poi confermato/blindato con test il 26/07): `NarrativeTonePreferences`
  in Opzioni, il tono scelto SOSTITUISCE (non si somma) i `toneHints`
  della scena quando impostato, altrimenti si legge il JSON come
  sempre. Include anche preset non presenti in v1 (Erotico, Brutale)
  coerenti con la direzione di contenuti adulti del progetto.
- **Mappa logica del diario più ricca**: oggi v0.1 mostra i soli nomi
  dei luoghi. Annotabile in futuro con combattimenti (già derivabili
  dalle Transition WIN/LOSE), NPC importanti e oggetti trovati (UI.md).
- **Scudo come oggetto iniziale**: in v1 era solo un valore dell'enum
  `ItemType`, senza nessun oggetto reale dietro. Se lo si vuole serve
  che Michele decida il bonus.

## 5. Debito tecnico: stringhe UI scritte a mano invece che in `strings.xml`

**Origine**: Michele (27/07/2026), durante il punto della situazione:
"bisognerebbe fare un controllo di tutte le etichette e poi
riscrivere in italiano" — le etichette sono già tutte in italiano
(l'app funziona), il problema è che gran parte non passa da
`strings.xml`.

**Audit fatto (27/07/2026)**: solo 3 file su tutta la UI
(`AdventureScreen.kt`, `CharacterCreationScreen.kt`,
`AdventureSetupScreen.kt`) usano `stringResource(R.string...)`. Tutto
il resto — Opzioni, Modelli LLM, Scheda personaggio/Equipaggiamento,
Diario, Home, e buona parte di Avventura/Combattimento — ha circa
**100 stringhe `Text("...")` scritte dirette in italiano nel codice**,
mentre `strings.xml` ha già **107 voci pronte** (in gran parte usate
solo dai 3 file sopra).

**Perché conta**: viola il vincolo non negoziabile #8 di
`PIANO-SVILUPPO.md`/`CLAUDE.md` ("ID canonici nei dati, nomi
localizzati SOLO in strings.xml") e blocca qualunque localizzazione
futura (una stringa scritta dentro un `Text()` non si può tradurre
senza toccare il codice Kotlin).

**Costo**: meccanico ma ESTESO — tocca quasi ogni schermata, uno
spostamento di stringa alla volta (aggiungere la voce in
`strings.xml`, sostituire il letterale con `stringResource(...)`,
verificare che non ci siano placeholder/pluralizzazioni da adattare).
Nessuna decisione di design richiesta, solo tempo. Candidato buono
per un task **[MICHELE-PROPOSTO]**: delimitato, senza dipendenze
intrecciate, con feedback visibile schermata per schermata — ma la
mole (~100 stringhe) lo rende adatto anche a una sessione dedicata di
Claude Code se Michele preferisce.

**Non schedulato**: in attesa di una decisione di Michele su
chi/quando lo fa.

## 6. Accelerazione NPU (Hexagon, via QNN) — in osservazione

**Origine**: la decisione di NON integrare l'NPU era già stata presa
(vedi DIARIO.md, round in cui Michele ha provato
`gemma-4-E2B-it_qualcomm_sm8750.litertlm`, build compilata apposta per
lo Snapdragon 8 Elite del suo Razr): `LiteRtLmEngine.load()` prova solo
`Backend.GPU()`/`Backend.CPU()`, mai NPU, e non imposta mai
`litert_dispatch_lib_dir`. Il blocco reale: LiteRT-LM richiede un
collante Google (`libLiteRtDispatch_Qualcomm.so`) che l'AAR Android non
include — si otterrebbe solo compilandolo da sorgente con **Bazel**,
toolchain nuovo e diverso da CMake/NDK. Michele allora: *"concordo che
non voglio reintrodurre C++ in questa versione... non stiamo generando
immagini ma testo, e se devo aspettare un po' va bene così"* —
decisione presa, non un rinvio, con nota esplicita di non riaprirla
senza una ragione nuova.

**Riportata a galla (28/07/2026)**: Michele ha segnalato
`DuoNeural/Gemma-4-Abliterated-LiteRT` su Hugging Face come possibile
modello NPU per il suo Snapdragon. Verificato: NON è una build
chip-specifica come la `_qualcomm_sm8750` di prima — è un `.litertlm`
INT4 generico (stesso formato di Gemma 4 E4B/E2B già in catalogo),
abliterazione uncensored, con supporto NPU dichiarato solo
genericamente ("if available") senza binari dedicati. Non risolve
comunque il blocco tecnico sopra, che resta lo stesso.

**Decisione di Michele questa volta**: non un rifiuto e non
un'implementazione ora — vuole **tenerla d'occhio**. Va ripresa in
considerazione come prossima feature papabile appena:
- diventa più semplice da integrare (es. Google include
  `libLiteRtDispatch_Qualcomm.so` nell'AAR Android di LiteRT-LM invece
  di richiedere una compilazione Bazel da sorgente — c'è un'issue
  aperta su google-ai-edge/LiteRT su questo, da ricontrollare ogni
  tanto), OPPURE
- esce qualcosa di ufficiale (non un fine-tune di terzi con claim
  vaghi) — un vero modello/toolchain NPU-ready per Snapdragon
  distribuito da Google/Qualcomm, pronto all'uso senza reinventare un
  bridge C++ a mano.

**Non schedulato**: nessuna azione di codice. Da ricontrollare quando
Michele segnala uno sviluppo, o a inizio di una futura sessione se
emergono novità su LiteRT-LM/QNN degne di nota.

## 7. Immagini animate (GIF/WebP) per libri-fumetto

**Origine (30/07/2026)**: durante il lavoro sull'editor (`doc/EDITOR.md`
§15.3), Michele chiede se il catalogo NPC/bestia/nemico supporta il
formato WebP "così da poter aggiungere immagini un po' più animate".
Segnalata come cosa **importante** da non perdere: a Michele è stato
chiesto (da terzi, non ancora chiaro chi/quale libro) se con questo
motore si possono fare **libri-game da fumetti animati, tipo GIF o
WebP** — un uso più ambizioso della semplice illustrazione statica di
scena, verso qualcosa più vicino a un fumetto animato.

**Verificato prima di rimandare** (non un rinvio alla cieca):
- **WebP statico**: già pienamente supportato oggi, zero lavoro —
  Android tratta `.webp` in `res/drawable-nodpi/` come qualunque altro
  formato raster, `painterResource` non fa differenza.
- **Contenuti ANIMATI (GIF o WebP animato)**: bloccati non dal
  formato ma dal PERCORSO di caricamento attuale.
  `SceneImages.kt`/`NpcImages.kt`/`EnemyImages.kt` passano da
  `painterResource(R.drawable.xxx)`, che mostra sempre un unico
  fotogramma statico per costruzione — nessun formato animerebbe lì.
  Servirebbe (a) l'artefatto `coil-gif` (oggi non incluso, solo
  `coil-compose`) e (b) spostare quei cataloghi dal binario
  `painterResource`/`R.drawable` al percorso Coil già usato per gli
  `url:`, l'unico che sa gestire contenuti animati.
  `minSdk=34` copre già comodamente `ImageDecoder`/
  `AnimatedImageDrawable` (disponibili da API 28 in su), nessun
  problema di compatibilità su questo fronte.
- Coincide con la migrazione del catalogo statico a un registro
  dinamico già discussa e rimandata apposta durante la costruzione
  dell'editor (`doc/EDITOR.md` §15.1) — qui avrebbe un motivo concreto
  in più (l'animazione), non solo la comodità di aggiungere risorse
  senza ricompilare.

**Decisione di Michele**: rimandata di proposito — "prima finiamo
l'editor, non voglio rimettere mano al client per ora". Non è un
rifiuto, è un ordine di priorità: il client resta fermo finché
l'editor (`doc/EDITOR.md` §15 e oltre) non è concluso. Da riprendere
quando si deciderà il passaggio del client al registro risorse
dinamico — stesso punto di decisione, motivazione aggiornata.

## 8. Verifica di esistenza delle risorse `url:` nell'editor

**Origine (30/07/2026)**: durante il lavoro sul pannello "Risorse del
libro" (`doc/EDITOR.md` §15.7), Michele nota che l'editor accetta
qualunque URL scritto a mano per un'immagine o un suono `url:`, senza
controllare che punti davvero a un file esistente/raggiungibile.
Decisione esplicita: **"se l'editor inserisce immagini o mp3 non
esistenti si assume la responsabilità per ora"** — l'autore del libro
resta responsabile di scrivere link corretti, l'editor non blocca né
avvisa.

**Rimandato, non dimenticato**: quando si riprenderà il tema, un
controllo utile sarebbe una richiesta HTTP (HEAD o GET parziale) sugli
URL registrati in `customResources`/usati nelle scene, per segnalare
(come avviso, non errore bloccante — stesso principio del resto della
validazione, §8 `doc/EDITOR.md`) i link morti prima che l'autore lo
scopra giocando sul device. Va deciso quando farlo (a ogni apertura
del libro? su richiesta con un pulsante? è un'operazione di rete, non
istantanea) — nessuna decisione presa, solo l'idea registrata.
