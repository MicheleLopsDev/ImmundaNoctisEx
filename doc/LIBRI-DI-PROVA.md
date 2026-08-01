# Libri di prova (`content/test-books/`)

Piccoli libri scritti apposta per verificare **una cosa per volta** sul
device: due o tre scene che mettono il giocatore davanti al caso da
provare senza doverci arrivare giocando mezz'ora. Nascono sessione per
sessione, ogni volta che una feature nuova andava vista funzionare
davvero (01/08/2026, Michele: *"lasciali sempre lì, possiamo riusarli
un giorno"*).

Sono **versionati apposta** — a differenza dei libri Project Aon, che
restano fuori dall'APK: qui non c'è nulla di protetto, e averli in git
significa poter tornare indietro se una modifica li rompe.

## Come si usano

I libri stanno in `content/test-books/`, che il build monta come
cartella asset: l'APK carica di default solo `scenes.sample.json`.
Tutti gli altri si aprono col **side-load** — l'icona cartella in Home.

Per averli sul telefono senza copiarli a mano:

```bash
./gradlew pushTestBooks
```

Copia l'intera cartella in `/sdcard/Download/ImmundaNoctisEx/` sul
device collegato. È idempotente: rilancialo ogni volta che aggiungi o
modifichi un libro.

---

## Il libro di esempio

### `scenes.sample.json` — "The Warehouse Letter"
7 scene, un combattimento, scelte-disciplina. **Non è un test**: è il
libro incluso nell'APK e caricato all'avvio, l'unico che l'utente vede
senza fare side-load. (Le scene di prova delle immagini animate sono state spostate in
`test-webp-animato.json` quando la verifica si è chiusa: l'asset di
prova pesava 1,2 MB nell'APK.)

> **Non rinominarlo né spostarlo**: `AppContainer` lo apre come asset
> per percorso fisso (`test-books/scenes.sample.json`). Fino al
> 01/08/2026 ne esistevano due copie — una in `content/` e una qui, con
> la seconda rimasta indietro di quattro immagini: ora è una sola.

---

## Immagini

La famiglia più numerosa: nasce dall'esperimento del 20-21/07/2026 in
cui Gemma poteva scegliere lo sfondo di scena da un vocabolario chiuso
(oggi dietro l'interruttore *"Il modello sceglie lo sfondo"*, spento di
default).

| Libro | Cosa verifica |
|---|---|
| `test_image_author_wins.json` | `backgroundImage` valido dichiarato dall'autore: Gemma **non** deve essere interpellata. L'autore vince sempre. |
| `test_image_dead_placeholder.json` | `backgroundImage` che **non esiste** nel catalogo (un bug vero del 20/07): va trattato come assente, non come valido. |
| `test_image_gemma_picks.json` / `test_home_gemma_picks.json` | Sfondo assente e testo che punta chiaramente a una location del catalogo: Gemma dovrebbe scegliere `loc_tavern`. |
| `test_image_no_match.json` | Ambientazione **fuori catalogo** (il ponte di una nave): Gemma deve scrivere `IMAGE|xxx`, non forzare un id sbagliato. |
| `test_image_no_match_desert.json` | Stesso caso con un soggetto diverso (un deserto). Serve a **triangolare**: un solo campione non basta a distinguere il comportamento vero dal colpo di fortuna. |
| `test_image_similar_pair.json` | Due portali di pietra quasi identici (`loc_black_gate` vs `loc_helgedad_gate`): la descrizione nel dizionario basta a distinguerli? |
| `test_image_with_combat.json` | Combattimento **senza** sfondo dichiarato: il blocco tag deve contenere `ENEMY` e `IMAGE` nella stessa risposta. |
| `test_image_enemy_npc.json` | I ritratti (22/07/2026): `Combat.enemyImage` accanto al nome, `Scene.npcImage` sotto il testo. La **stessa** immagine compare prima come incontro pacifico e poi come nemico — è il campo scelto dall'autore a decidere, non l'immagine. La scena 7 ha un combattimento *senza* ritratto, per il fallback. |
| `test_image_url.json` | Riferimento `url:` (29/07/2026): deve produrre un **avviso**, mai un errore. Usa `example.invalid` (RFC 2606), che non risolve mai: in app l'immagine semplicemente non appare. Un link vero non si mette qui perché questo file finisce nell'APK. |

### `test-webp-animato.json` — "Prova delle immagini animate"
Un `url:` verso un WebP **animato** open-source: l'immagine sotto il
testo deve muoversi, non restare un fotogramma fisso. Usa solo `url:`
perché la variante `static:` richiederebbe un asset dentro l'APK — la
convenzione per quelli è il suffisso `_anim` nel nome del drawable
(vedi `CatalogOrUrlImage.kt`).

---

## Suoni

### `test-sfx.json` — "Prova dei suoni personalizzati"
5 scene per `Scene.sfx` con sorgenti `url:` (tracce SoundHelix, generate
proceduralmente, libere da diritti). Copre in sequenza: **download** dalla
rete alla prima scena, **cambio** di suono (il primo si ferma, mai due
sovrapposti), **ritorno** al suono automatico per-immagine in una scena
senza `sfx`, e **riproduzione da cache** riusando la prima traccia — quella
va notevolmente più veloce, e funziona anche offline.

### `test_eat_meal_sound.json` — "Suono pasto obbligatorio"
Il suono `EAT` sul consumo **obbligatorio** di un pasto (`requireAction
EAT_MEAL` dichiarato nel JSON). Il file porta con sé due avvertenze
guadagnate sul campo:

- **Non scegliere HUNTING** in creazione: quella disciplina auto-soddisfa
  il pasto prima ancora di toccare l'inventario, e non sentiresti né
  suono né cura.
- Nessun `addItem` in scena 1 di proposito: l'eroe parte già con 2 Meal,
  che bastano a esaurirsi in due tappe e arrivare davvero al ramo *senza
  pasto* (scena 4: nessun suono, −3 Resistenza).

---

## Regole e meccaniche

### `test-rollmodifier.json` — "Prova dei tiri con bonus"
7 scene per `Scene.rollModifiers` (01/08/2026). Tre prove in fila:

1. **Bonus fisso `+5`**, incondizionato: deterministico, verifica solo
   che il numero venga *spiegato* (`Hai tirato: 3 +5 = 8`).
2. **Bonus `+2` dal Sesto Senso**: la prova vera. Una scena è
   raggiungibile **solo** col bonus (intervallo `10–11`, impossibile con
   un dado 0-9): se ci arrivi senza quella disciplina, c'è un bug. Le
   scene di arrivo riportano indietro apposta, per tirare più volte.
3. **Malus `−3` con ENDURANCE sotto 10**: difficile da innescare a
   comando, sta lì per mostrare che una condizione sullo **stato** del
   personaggio viene letta al momento giusto.

> Il confronto che vale di più: rigiocarlo con un personaggio **senza**
> Sesto Senso — nessun `+2` mostrato, e l'esito alto non deve mai uscire.

### `test_5_choices_kai_combat.json`
Verifica **visiva** di `ChoicesZone` con 5 opzioni insieme: quattro
scelte-disciplina più una normale che porta a un combattimento. Serve un
eroe con esattamente quelle 4 discipline (Mimetismo, Caccia, Sesto Senso,
Tracciamento) — **sceglile a mano** in creazione, il random non le
garantisce.

### `test_items_and_weapons.json`
Tre armi in bella vista con soli 2 slot disponibili, più altri oggetti
offerti: il giocatore prende un oggetto alla volta col pulsante
*"Prendi"*. Nasce dalla richiesta del 21/07/2026 di non avere più
`addItem` automatici che scartano in silenzio ciò che eccede lo spazio.

---

## Aggiungerne uno nuovo

1. Scrivilo in `content/test-books/`, con un `description` che dica
   **cosa** verifica e **cosa guardare** — è quello che rende il libro
   riusabile fra sei mesi.
2. Validalo prima di provarlo:
   `./gradlew :tool:cli --args="validate <percorso assoluto>"`
3. `./gradlew pushTestBooks` per averlo sul telefono.
4. Aggiungi una riga qui.

Se il caso da provare sta a molti passi dall'inizio di un libro vero, o
dipende da tiri di dado fortunati, **conviene sempre un libro dedicato**:
i `rollModifiers` avrebbero richiesto 13 passi e 3 tiri favorevoli in
*Fire on the Water*, qui si provano al primo tocco.
