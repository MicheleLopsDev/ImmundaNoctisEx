# Milestone: Client Android

**Cosa NON è questo documento**: non è un elenco di idee (quello è
`doc/UPGRADE.md`, esplicitamente "non un impegno"). Qui entrano solo
obiettivi che Michele ha deciso di perseguire per il prossimo
traguardo del client — se un'idea non è ancora decisa resta in
`UPGRADE.md` finché non viene promossa qui.

**Ora è anche una vera GitHub Milestone** (30/07/2026, dopo il login
di Michele con `gh auth login`):
[Milestone "Client Android"](https://github.com/MicheleLopsDev/ImmundaNoctisEx/milestone/1),
con un'issue per voce:
[#2 leak di memoria](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/2),
[#3 migrazione strings.xml](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/3),
[#4 motore GGUF alternativo](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/4),
[#5 firma di release](https://github.com/MicheleLopsDev/ImmundaNoctisEx/issues/5).
Al 03/08/2026 resta aperta **solo la #2** (leak di memoria): #3 chiusa
quel giorno, #4 e #5 già chiuse il 30/07.
Questo file resta la versione leggibile/discorsiva con tutto il
perché — le issue rimandano qui per il dettaglio, non lo duplicano.

**Origine (30/07/2026)**: dopo il merge di `develop` su `main` (tag
`v0.1.0`, client sostanzialmente completo + editor grafico alla prima
base), Michele chiede di iniziare a pensare alla prossima milestone
per il client, separata da quella dell'editor (`doc/MILESTONE-EDITOR.md`).

---

## Obiettivo di questa milestone

Chiudere i debiti tecnici già noti e documentati (non bloccanti, ma
aperti da tempo) prima di allargare ancora il perimetro del client con
feature nuove. Nessuna feature nuova qui dentro: solo qualità di quello
che già c'è.

## Cosa entra (proposto da Claude, da confermare/correggere)

### 1. Leak di memoria nativa (~140MB/partita)

Rilevato durante la Fase 4/misure di `CRITICITA.md`, rinviato
consapevolmente perché non si sente su 15,5GB di RAM del device di
riferimento anche su partite ripetute. Resta comunque un leak vero,
non un rumore di misura — da individuare (probabile sospetto: sessione
Gemma per scena, `doc/PIANO-SVILUPPO.md` — "inferenza senza memoria")
e chiudere prima di considerare il client stabile.

### 2. Migrazione delle stringhe UI hard-coded a `strings.xml` — FATTO (03/08/2026)

Prima: 9 file su 79 usavano `stringResource`, contro 109 voci già
pronte in `strings.xml`; il resto (~114 stringhe) era scritto a mano
nel codice Kotlin.

Ora tutta la UI ci passa: **355 chiavi in inglese, 354 in italiano**
(l'unica differenza è `app_name`, identico nelle due lingue e tenuto
solo nel file di ripiego).

**Il problema più grosso non era il conteggio**: `values/strings.xml`
conteneva l'ITALIANO, e su Android quella cartella è il ripiego per
qualunque lingua senza una `values-xx` dedicata — chi non aveva il
telefono in italiano si ritrovava comunque l'italiano. Ora `values/` è
inglese e l'italiano sta in `values-it/`.

I termini di gioco (COMBAT SKILL, ENDURANCE, Gold Crowns, Chainmail
Waistcoat, Potion of Laumspur, le nove armi) sono quelli CANONICI dei
libri, verificati sui testi Project Aon in `doc/LIBRI/` prima di
scriverli. Sei enum di preferenze passano da `displayName: String` a
`@StringRes labelRes: Int`; `LinguaOutput` no, sta in `:core:engine`
senza Android e i nomi delle lingue sono endonimi. 220 test verdi.

`strings.xml` resta impalcato da Claude: la rifinitura dei testi è di
Michele (già dichiarato in `doc/DIARIO.md`).

**Fuori di proposito**: spagnolo, francese e tedesco. La struttura è
pronta, ma senza utenti veri è lavoro speculativo — stesso argomento
già usato da Michele per l'accessibilità.

### 3. Valutazione del motore GGUF alternativo a LiteRT-LM

`doc/UPGRADE.md` §3: verificare se un motore Kotlin per modelli GGUF
regge il confronto con LiteRT-LM sul device di riferimento (Razr 60
Ultra, SM8750). Questo è un obiettivo di **decisione** (si/no/rimandato),
non necessariamente di sostituzione completa del motore attuale —
LiteRT-LM resta il motore di produzione finché la valutazione non dice
altrimenti.

### 4. Firma di release dell'APK — FATTO (30/07/2026)

Michele: "vanno firmati sia gli apk che gli exe... altrimenti non
girano sui vari sistemi". Precisazione tecnica: un APK non firmato
Android non lo installa affatto (già vero per il debug, firmato in
automatico con una chiave usa-e-getta); quello che mancava era una
chiave di **release** vera e stabile, la stessa a ogni build — senza,
un aggiornamento futuro di un'installazione esistente verrebbe
rifiutato da Android (chiavi diverse = pacchetti "diversi").

Fatto: keystore RSA 2048 generata con `keytool` (validità 10.000
giorni, ~2053), riferita da `app/build.gradle.kts` tramite
`signingConfigs`/`buildTypes.release` che legge percorso e password da
`local.properties` (mai in git, stesso principio già usato per
`packagingJdk`/`llamaCppDir`) — se quelle chiavi mancano (altra
macchina, CI) il build type `release` resta semplicemente senza
signingConfig, niente fallisce per chi non deve firmare nulla.
`:app:assembleRelease` verificato con `apksigner verify --print-certs`:
firma valida (schema v2), certificato `CN=Michele, OU=ImmundaNoctisEx`.

**Promemoria per Michele**: la keystore (`keystore/immundanoctisex-release.jks`)
e le password in `local.properties` sono **volutamente fuori da git** —
se si perdono, non sarà più possibile pubblicare un aggiornamento
sopra un'installazione esistente con la stessa identità. Vanno
salvate altrove (una copia offline, un password manager) a cura tua,
non di questo repository.

Per l'`.exe` (jpackage/editor): **decisione di non firmarlo**. Windows
esegue un eseguibile non firmato senza problemi tecnici — l'unico
effetto è l'avviso SmartScreen "Windows ha protetto il PC" al primo
avvio (bypassabile con "Ulteriori informazioni → Esegui comunque"). Un
certificato di code-signing toglierebbe l'avviso ma è un servizio a
pagamento, spesso ricorrente — contro il vincolo di questo progetto
(niente servizi a pagamento, vedi memoria di sessione). Non incluso in
questa milestone.

## Cosa NON entra (resta in `doc/UPGRADE.md`, non promosso)

- Reskin grafico ispirato al registro cartaceo (§2) — gran parte della
  Fase 7 di abbellimento è già fatta fuori piano; il resto è rifinitura
  estetica, non debito.
- Audio narrativo ambientale (§1) — feature nuova, allarga il
  perimetro, esplicitamente rimandata.
- Accelerazione NPU (§6) — "in osservazione": nessuna azione possibile
  finché LiteRT-LM non include il collante Qualcomm (vedi promemoria in
  memoria di sessione).
- Immagini animate GIF/WebP (§7) — Michele ha già deciso di rimandare
  finché l'editor non è concluso.

## Come si chiude

Suite di test verde su tutti i moduli (oggi 239+ test) dopo ogni voce
chiusa, aggiornamento di `doc/DIARIO.md` punto per punto come per ogni
altro lavoro di questa sessione. Quando tutte le voci sopra sono
chiuse (o esplicitamente rimosse da Michele), questa milestone si
considera raggiunta e se ne apre una nuova.
