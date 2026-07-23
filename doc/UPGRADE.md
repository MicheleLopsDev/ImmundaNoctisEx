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

### 1c. Suono una tantum agganciato all'immagine risolta (NUOVO, 22/07/2026)
Idea di Michele, molto più semplice delle due sotto: **non tocca
Gemma per niente**. `backgroundImage`/`enemyImage`/`npcImage` sono già
vocabolari chiusi, già risolti in modo affidabile
(`SceneImageCatalog`/`EnemyImageCatalog`/`NpcImageCatalog`) — basta
riusare LO STESSO id per pescare un file audio con lo stesso nome
(`loc_market.mp3`), un colpo secco (SoundPool, non loop), riprodotto
insieme all'immagine, senza toccare la musica di sottofondo. Nessun
campo nuovo su `Scene`, nessun rischio di vocabolario extra nel
prompt: il rinvio motivato dal rischio (sotto) non si applica a
questa. Checklist degli asset da produrre: `doc/SUONI-IMMAGINI.md`.
**In corso**: struttura ancora da scrivere (caricamento pigro per
nome, cartella `assets/sfx/images/`), in attesa dei primi file mp3.

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

**Cosa NON chiederle**: copiare il logo "LONE WOLF" o le illustrazioni
specifiche del libro originale — quelle sono protette. Lo stile sì,
il contenuto esatto no.

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

**Non schedulato**: nessuna azione finché Michele non porta una
libreria concreta da valutare.

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
- **Tono narrativo scelto dall'utente**: v1 aveva in Opzioni un menu
  (originale/horror/epico/…) iniettato nel prompt. In Ex i toni sono
  dell'autore via `toneHints`: darlo all'utente è una scelta di design
  diversa, non una svista.
- **Mappa logica del diario più ricca**: oggi v0.1 mostra i soli nomi
  dei luoghi. Annotabile in futuro con combattimenti (già derivabili
  dalle Transition WIN/LOSE), NPC importanti e oggetti trovati (UI.md).
- **Scudo come oggetto iniziale**: in v1 era solo un valore dell'enum
  `ItemType`, senza nessun oggetto reale dietro. Se lo si vuole serve
  che Michele decida il bonus.
