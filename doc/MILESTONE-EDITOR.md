# Milestone: Tool di authoring (`:tool`)

**Cosa NON è questo documento**: come `doc/MILESTONE-CLIENT.md`, non è
un elenco di idee libere — quelle restano in `doc/EDITOR.md` §13
(fuori perimetro) o in `doc/UPGRADE.md` finché non vengono promosse
qui da una decisione esplicita di Michele.

**Perché un documento e non una GitHub Milestone**: stessa ragione
della milestone client — `gh` CLI non è installato su questa macchina,
il login è un passo interattivo da fare da Michele. Ogni voce sotto è
scritta in modo da poter diventare un'issue 1:1 in seguito.

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

### 1. Completare la pacchettizzazione standalone (jpackage)

Obiettivo dichiarato da Michele (`doc/EDITOR.md` §12): passare
l'editor a suo figlio **senza accesso al repository Git/GitHub**. Il
plugin `org.jetbrains.compose` è già configurato in
`tool/build.gradle.kts` (`nativeDistributions`, target `Exe`/`Msi`),
ma `jpackage` richiede un JDK completo — quello imbustato in Android
Studio (JBR) non lo include (`'jpackage.exe' is missing`, verificato
30/07/2026). Resta da: puntare `packagingJdk` in `local.properties` a
un JDK completo, generare davvero un pacchetto con
`createDistributable`/`packageMsi`, e provarlo su una macchina pulita
(idealmente quella del figlio di Michele) per confermare che si avvii
senza altre dipendenze.

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
