# Milestone: Client Android

**Cosa NON è questo documento**: non è un elenco di idee (quello è
`doc/UPGRADE.md`, esplicitamente "non un impegno"). Qui entrano solo
obiettivi che Michele ha deciso di perseguire per il prossimo
traguardo del client — se un'idea non è ancora decisa resta in
`UPGRADE.md` finché non viene promossa qui.

**Perché un documento e non una GitHub Milestone**: la scelta iniziale
era GitHub Milestone + Issues, ma `gh` (GitHub CLI) non è installato
su questa macchina e il login è un passo interattivo che deve fare
Michele. Fino a quel momento questo file fa da milestone; ogni voce
sotto è scritta in modo da poter diventare un'issue 1:1 quando `gh`
sarà disponibile (titolo breve + corpo con perché/riferimenti).

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

### 2. Migrazione delle stringhe UI hard-coded a `strings.xml`

Solo 3 file su tutta la UI usano `stringResource`, contro 107 voci già
pronte in `strings.xml` — il resto (~100 stringhe) è ancora scritto a
mano nel codice Kotlin. `strings.xml` è impalcato da Claude; la
rifinitura dei testi resta di Michele (già dichiarato in
`doc/DIARIO.md`). Prerequisito per qualunque localizzazione futura e
per coerenza interna (oggi le stesse parole potrebbero comparire
scritte in due punti diversi senza essere la stessa stringa).

### 3. Valutazione del motore GGUF alternativo a LiteRT-LM

`doc/UPGRADE.md` §3: verificare se un motore Kotlin per modelli GGUF
regge il confronto con LiteRT-LM sul device di riferimento (Razr 60
Ultra, SM8750). Questo è un obiettivo di **decisione** (si/no/rimandato),
non necessariamente di sostituzione completa del motore attuale —
LiteRT-LM resta il motore di produzione finché la valutazione non dice
altrimenti.

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
altro lavoro di questa sessione. Quando tutte e tre le voci sopra sono
chiuse (o esplicitamente rimosse da Michele), questa milestone si
considera raggiunta e se ne apre una nuova.
