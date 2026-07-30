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

### 1. Completare la pacchettizzazione standalone (jpackage) — QUASI FATTO

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

Resta solo l'ultimo passo, che deve fare Michele: **provare uno dei
due su una macchina pulita** (idealmente quella del figlio, senza
Android Studio né JDK installati) per confermare che non serva
nient'altro.

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
