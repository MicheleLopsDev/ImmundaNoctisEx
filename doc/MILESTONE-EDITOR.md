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

Resta solo l'ultimo passo, che deve fare Michele: **provarla su una
macchina pulita** (idealmente quella del figlio, senza Android Studio
né JDK installati) per confermare che non serva nient'altro. Se serve
anche l'installer vero (`packageMsi`), il task è pronto ma non ancora
provato.

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
