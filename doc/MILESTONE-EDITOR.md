# Milestone: Tool di authoring (`:tool`)

**Cosa NON è questo documento**: come `doc/MILESTONE-CLIENT.md`, non è
un elenco di idee libere — quelle restano in `doc/EDITOR.md` §13
(fuori perimetro) o in `doc/UPGRADE.md` finché non vengono promosse
qui da una decisione esplicita di Michele.

**Ora è anche una vera GitHub Milestone** (30/07/2026, dopo il login
di Michele con `gh auth login`):
[Milestone "Tool editor"](https://github.com/MicheleLopsDev/ImmundaNoctisEx/milestone/2),
con un'issue per voce:
[#6 pacchettizzazione jpackage](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/6),
[#7 limite ~350 scene](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/7),
[#8 editor dei frammenti di prompt](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/8).
Questo file resta la versione leggibile/discorsiva con tutto il
perché — le issue rimandano qui per il dettaglio, non lo duplicano.

**Origine (30/07/2026)**: seconda delle due milestone chieste da
Michele dopo il merge `develop` → `main` (tag `v0.1.0`). L'editor
grafico ha appena chiuso l'intero giro di `doc/EDITOR.md` §15 (risorse,
interazione, qualità di scrittura) e §16 (impostazioni) — questa
milestone guarda a cosa resta prima di considerarlo pronto per un uso
reale al di fuori delle prove di Michele.

---

## Obiettivo di questa milestone

Rendere l'editor **consegnabile** (a chi scrive libri, non solo a chi
lo sviluppa) e verificare che regga sui libri reali già disponibili
(i 5 di `doc/LIBRI/`, 362-406 scene ciascuno), prima di aprire il
prossimo grande capitolo (editor dei prompt).

## Cosa entra (proposto da Claude, da confermare/correggere)

### 1. Completare la pacchettizzazione standalone (jpackage) — FATTO (31/07/2026)

Obiettivo dichiarato da Michele (`doc/EDITOR.md` §12): passare
l'editor a suo figlio **senza accesso al repository Git/GitHub**.
`packagingJdk` in `local.properties` puntava già a un JDK Temurin 17
completo (con `jpackage.exe`): `:tool:createDistributable` genera con
successo una cartella autonoma in
`tool/build/compose/binaries/main/app/ImmundaNoctisEx-Editor/`
(~153 MB, JVM inclusa) e l'`.exe` dentro si avvia correttamente
(verificato 30/07/2026: processo partito e stabile, chiuso dopo la
prova). Non tracciata in git (dentro `build/`, già ignorato) — va
rigenerata con lo stesso comando su chi la costruisce.

Anche l'installer vero (`packageMsi`) generato con successo (WiX
scaricato automaticamente dal plugin, nessun setup manuale servito):
`ImmundaNoctisEx-Editor-1.0.0.msi` (~95 MB).

Entrambi pubblicati come [GitHub Release
`editor-v1.0.0`](https://github.com/MicheleLopsDev/ImmundaNoctisEx/releases/tag/editor-v1.0.0)
(30/07/2026, Michele: "possiamo mettere i packages su... prima
facciamo un msi"): cartella portatile zippata + installer `.msi`. Il
repository è pubblico, quindi anche questi pacchetti lo sono — nessun
problema di copyright secondo Michele (nessun contenuto di terzi che
lo violi).

Rigenerati e ripubblicati il 31/07/2026 (stessa release, stessa
versione 1.0.0) con tutto il codice del giro §19 (voce 4 sotto).
**Ultimo passo confermato da Michele il 31/07/2026**: provato su una
macchina pulita, funziona.

**Seconda release, [`editor-v1.1.0`](https://github.com/MicheleLopsDev/ImmundaNoctisEx/releases/tag/editor-v1.1.0)
(31/07/2026, Michele: "prepariamoci a fare una nuova release")**:
versione bump a 1.1.0 (`tool/build.gradle.kts`) — non solo un
refresh degli asset come le volte precedenti, ma un numero di
versione nuovo: dalla v1.0.0 il giro §19 completo (selezione a
rettangolo, duplica/allinea/trascina gruppo, Ctrl+Z/Ctrl+Y, posizioni
dei nodi persistite nel libro, esportazione PNG riscritta più volte
dopo i bug trovati da Michele durante i test) è abbastanza sostanza da
meritare una versione minore nuova, non solo un refresh silenzioso
della v1.0.0.

**Terza release, `editor-v1.2.0` (01/08/2026)**: bump a 1.2.0 in
`tool/build.gradle.kts`. Dalla v1.1.0 l'editor ha guadagnato tre cose,
tutte nate da lavoro sui libri veri e non da rifinitura:

- **`EDITOR_BUILD_MARKER`** stampato all'avvio e scritto nel titolo
  della finestra (`EditorMain.kt`), sullo stampo del `BUILD_MARKER`
  dell'app: la stessa convenzione che nella sessione del 01/08 ha
  smascherato due volte un test fatto su una build vecchia.
- **Tabella dei Numeri Casuali riconosciuta in tutte le forme reali**
  presenti nei libri, dopo due giri di correzione (il secondo per una
  regressione mia, che aveva perso le forme che il primo prendeva —
  scoperta solo da un controllo esaustivo, non dal confronto dei
  totali).
- **Estrazione dei `rollModifiers`** dal testo inglese: le scene in cui
  il tiro va modificato prima del confronto (Disciplina Kai, ENDURANCE,
  oggetto, flag) ora si convertono da sole invece di essere scartate
  di proposito, e ciò che il convertitore non sa tradurre finisce nel
  report finale invece di essere indovinato in silenzio.

Pacchetti generati e verificati localmente:
`ImmundaNoctisEx-Editor-1.2.0.msi` (96 MB) e la cartella portatile
zippata `ImmundaNoctisEx-Editor-1.2.0-portable.zip` (94 MB), entrambi
in `tool/build/compose/binaries/main/`. **Il caricamento sulla GitHub
Release lo fa Michele**: la `gh` CLI non è installata su questa
macchina.

### 2. Verificare il limite dimensionale (~350 scene)

`doc/EDITOR.md` §11 fissa un tetto auto-imposto di ~350 scene per
libro "da confermare in fase di implementazione". I 5 libri Project
Aon in `doc/LIBRI/` (362-406 scene, ora versionati apposta per questo)
sono già sopra soglia — buona occasione per aprirli davvero
nell'editor e verificare che restino usabili (mappa, ricerca, editing
di una scena) oltre il limite, invece di lasciarlo come ipotesi non
provata.

### 3. Editor dei frammenti di prompt (`content/config.json`)

Rimandato esplicitamente in `doc/EDITOR.md` §13 e in memoria di
sessione ("tool_authoring_scope_ampliato": "`:tool` non è solo ETL,
serve anche editor scene da zero + editor prompt"). L'editor scene da
zero è ormai fatto (`CreaNuovoScreen`, scaffold, §9); manca la parte
prompt. È il pezzo più grande e meno definito di questa milestone:
**serve prima una specifica dedicata** (come già deciso), non si parte
dal codice — piano ancora da ricevere da Michele.

**Pulizia preliminare fatta il 31/07/2026** (Michele: "per prima cosa
credo vada ripulito, c'è tutta una parte xml che non serve"): prima
di pensare alla schermata, tolte dal file 15 entry `gameMechanic` con
regex che matchavano tag XML-like (`<addItem>`, `<statMod>`,
`<skillCheck>`, ecc.) — verificato nel codice che **nessuna** fosse
davvero letta a runtime: `PromptFragments.fromConfig()` estrae solo
l'entry `start_adventure_prompt`, il parsing reale della risposta di
Gemma (`ResponseParser.kt`) usa un formato a pipe completamente
diverso (introdotto apposta perché "in v1 erano tag XML e il modello
li sbagliava"), e il prompt stesso ordina esplicitamente a Gemma di
non generare mai quei tag. Motivazione di Michele, tecnicamente
corretta e allineata al vincolo "si serializzano i fatti, i bonus si
calcolano": *"un LLM locale resta abbastanza stupido e può supportare
solo scelte semplici non tag, poi il file scene gestisce tutto e se
serve aggiungerò lì gli attributi"* — i meccanismi di gioco restano
dichiarati staticamente in `Scene.gameMechanics`, mai decisi
dall'output del modello. Restano solo le 4 entry davvero usate:
`choice_line`, `discipline_line`, `start_adventure_prompt`,
`end_guff_tag`. Il file scende da 309 a un centinaio di righe — la
futura schermata dell'editor avrà meno terreno da coprire.

### 4. Interazioni avanzate sulla mappa (`doc/EDITOR.md` §19, 30/07/2026) — FATTO (31/07/2026)

Giro nato da un confronto diretto con Michele su cosa manca ancora
all'editor, non da un debito già noto — proposto e affinato insieme
prima di scrivere codice, stesso metodo delle fasi precedenti. Sette
voci, ciascuna una GitHub Issue a parte:

1. **[#9](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/9)
   Legare/rimuovere un legame diretto** tra due scene selezionate
   (§19.1) — dal menu tasto destro, senza aprire la scheda di
   nessuna delle due.
2. **[#10](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/10)
   Duplicare un gruppo** mantenendo i collegamenti interni (§19.2).
3. **[#11](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/11)
   Trascinare un gruppo** di scene insieme (§19.3).
4. **[#12](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/12)
   Centrare la vista sulla selezione** (§19.4).
5. **[#13](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/13)
   Ricollegamento opzionale** alla cancellazione, verso una scena
   scelta invece di lasciare riferimenti rotti (§19.5).
6. **[#14](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/14)
   Evidenziare scene orfane e vicoli ciechi** sulla mappa (§19.6 +
   §19.7, stesso tipo di segnalazione visiva).
7. **[#15](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/15)
   Esportare la mappa come immagine PNG** (§19.8) — valutata anche
   la stampa vera multipagina, sensibilmente più complessa
   (impaginazione su più fogli per un libro grande): PNG copre la
   maggior parte dei casi d'uso reali con uno sforzo molto minore.

**Bassa priorità esplicita, non in questo giro** (§19.9): salti minimi
START→scena/scena→END più vicina (tecnicamente economico, un BFS —
rimandato su richiesta di Michele, non per difficoltà) e stampa
multipagina vera (solo se emerge una libreria Kotlin/JVM che
semplifichi l'impaginazione).

Tutte e sette le issue chiuse il 31/07/2026, ciascuna con un commit
dedicato (§19.1/§19.4 nello stesso commit, stesso giro di lavoro;
tutte le altre una per una) — dettaglio in `doc/DIARIO.md`.

## Cosa NON entra (per ora)

- Immagini animate GIF/WebP (`doc/UPGRADE.md` §7) — Michele ha già
  deciso di rimandarle a dopo la chiusura dell'editor; restano legate
  anche a un cambiamento lato client (migrazione del catalogo statico
  a Coil), fuori perimetro di questa milestone.
- Registro risorse dinamico che sostituisce `static-resources.json`
  (idea discussa e scalata volutamente durante `doc/EDITOR.md` §15.1)
  — nessuna decisione presa di riprenderla ora.

## Come si chiude

`:tool:compileKotlin`/`:tool:test` verdi a ogni voce chiusa (pattern
già seguito per tutto `doc/EDITOR.md` §15/§16), un pacchetto standalone
provato con successo su una macchina diversa da quella di sviluppo, e
almeno uno dei 5 libri di `doc/LIBRI/` aperto e navigato per intero
nell'editor senza problemi. Quando tutte e tre le voci sopra sono
chiuse (o esplicitamente rimosse da Michele), questa milestone si
considera raggiunta e se ne apre una nuova.
