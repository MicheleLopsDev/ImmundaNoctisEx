# Le dieci Discipline: cosa fanno davvero

Fotografia di come le Discipline Kai vivono **oggi** nel codice, scritta
per poterle rimpiazzare con un sistema originale (Michele, 05/08/2026:
*"per tradurle in un sistema coerente e slegarci dai libri"*).

Il punto di questo documento è distinguere **tre cose che di solito si
confondono**:

1. **L'effetto meccanico** — cosa il motore fa davvero. È l'unica parte
   che, cambiando sistema, va riprogettata.
2. **Il nome mostrato** — sta in `strings.xml`, si cambia in un file
   solo, non tocca il codice.
3. **L'ID interno** — `MINDBLAST`, `SIXTH_SENSE`. Il giocatore non lo
   vede mai: compare solo nei JSON dei libri e nell'enum.

---

## Il quadro

| ID interno | Nome mostrato (it) | Effetto MECCANICO reale | Usi nei 5 libri |
|---|---|---|---|
| `SIXTH_SENSE` | Sesto Senso | **nessuno**: solo chiave di scelta | **68** |
| `TRACKING` | Orientamento | **nessuno**: solo chiave di scelta | 39 |
| `HUNTING` | Caccia | annulla l'obbligo di consumare un Pasto | 37 |
| `CAMOUFLAGE` | Mimetismo | **nessuno**: solo chiave di scelta | 30 |
| `MIND_OVER_MATTER` | Telecinesi | **nessuno**: solo chiave di scelta | 22 |
| `ANIMAL_KINSHIP` | Affinità Animale | **nessuno**: solo chiave di scelta | 20 |
| `HEALING` | Guarigione | +1 Resistenza a ogni scena senza combattimento | 20 |
| `WEAPONSKILL` | Scherma | +2 Combattività con l'arma della specializzazione | 7 |
| `MINDSHIELD` | Schermo Psichico | **nessuno** nel motore: lo consuma il libro | 6 |
| `MINDBLAST` | Attacco Psichico | +2 Combattività, attivabile una volta per scontro | 4 |

### La cosa che salta all'occhio

**Sei discipline su dieci non hanno alcun effetto meccanico.** Non è una
mancanza: è come funziona il genere. Servono come **chiave che apre una
porta** — il libro dichiara *"se hai il Sesto Senso, vai a 45"* e quella
scelta compare solo a chi ce l'ha (`disciplineChoices`), oppure aggiunge
un bonus al tiro del dado (`rollModifiers`).

Di conseguenza le più **usate** sono proprio quelle senza effetto: il
Sesto Senso apre 68 porte nei cinque libri, l'Attacco Psichico — che ha
un effetto vero — solo 4.

Chi progetta un sistema nuovo ha quindi due leve indipendenti:
- **quante porte apre** una dote (dipende da come scrivi i libri)
- **cosa fa quando è attiva** (dipende dal motore)

Oggi le due leve sono sbilanciate in direzioni opposte.

---

## Le quattro con un effetto vero

### `WEAPONSKILL` — +2 Combattività
`core/engine/stats/EffectiveStats.kt`

Alla creazione si sceglie **un tipo d'arma** fra le nove (o "a mani
nude"). Il bonus scatta solo impugnando quell'arma; a mani nude scatta
solo se la specializzazione è proprio "nessuna arma". È l'unica
disciplina che richiede una **scelta secondaria** al momento della
creazione del personaggio.

### `MINDBLAST` — +2 Combattività, una volta per scontro
`core/engine/combat/CombatSession.kt`

Si attiva **durante** il combattimento, dura fino alla fine e poi decade.
Un nemico può esserne **immune** (`combat.immuneToMindblast`), e in quel
caso il pulsante resta visibile ma disattivato col motivo scritto. È
l'unica disciplina con un'attivazione esplicita del giocatore.

### `HEALING` — +1 Resistenza a scena
`core/engine/transition/TransitionEngine.kt`

Passiva: a ogni transizione verso una scena **senza combattimento**,
+1 Resistenza fino al massimo. È l'unica che agisce fuori dal
combattimento e nel tempo.

### `HUNTING` — niente Pasti
`core/engine/mechanics/StatMechanics.kt`

Quando il libro impone di consumare un Pasto, chi ha Caccia non lo
consuma e non subisce la penalità. È una **negazione di un costo**, non
un bonus.

## Le sei senza effetto

`SIXTH_SENSE`, `TRACKING`, `CAMOUFLAGE`, `MIND_OVER_MATTER`,
`ANIMAL_KINSHIP`, `MINDSHIELD`.

Il motore non sa cosa facciano: sa solo **se le possiedi**. Tutto il
resto lo decide il libro, in due modi:

```json
"disciplineChoices": [
  { "id": "d1", "disciplineId": "SIXTH_SENSE",
    "choiceText": "Un istinto ti avverte...", "nextSceneId": "45" }
]
```

```json
"rollModifiers": [
  { "amount": 2,
    "condition": { "type": "DISCIPLINE", "values": ["SIXTH_SENSE"] } }
]
```

`MINDSHIELD` è un caso a parte: nel canone protegge dagli attacchi
psichici, e i libri lo usano scrivendo la penalità nel testo della scena
(*"a meno che tu non abbia lo Schermo Psichico, -2 Combattività"*). Nel
nostro motore non c'è nulla di dedicato.

---

## Regole di contorno che un sistema nuovo eredita

- **Se ne scelgono 5** alla creazione (`creation_disciplines_title`).
- Il **Rango** sale col numero di discipline possedute
  (`KaiRank.fromDisciplineCount`), ed è **puramente cosmetico**: nessun
  effetto meccanico, per decisione esplicita.
- Passando di libro in libro il personaggio ne **guadagna una nuova**
  (`TrasportoPersonaggio.disciplineDaAssegnare`).
- La lista è un **vocabolario chiuso**: il validatore rifiuta un libro
  che nomini una disciplina fuori enum
  (`Manifest: disciplina 'X' non canonica`).

---

## Cosa costa cambiare sistema

| cosa | dove | costo |
|---|---|---|
| **Nomi e descrizioni** | `values/strings.xml`, `values-it/strings.xml` | 20 voci per lingua, mezz'ora |
| **ID interni** | `Discipline.kt` + i JSON dei libri | rinominare l'enum e riconvertire |
| **Quante se ne scelgono** | costante nella creazione | una riga |
| **Effetti nuovi** | `EffectiveStats`, `CombatSession`, `TransitionEngine`, `StatMechanics` | dipende da cosa inventi |
| **Numero di discipline** | enum + creazione + validatore | contenuto |

**Nota importante**: i cinque libri Project Aon in `doc/LIBRI/` usano gli
ID attuali. Cambiare l'enum significa o riconvertirli con una tabella di
corrispondenza, o tenere i due mondi separati — un sistema originale per
i libri nuovi, quello attuale per i cinque convertiti.

### Un gancio che c'è ma non è collegato

Il `Manifest` ha `disciplineChoices: List<DisciplineDescriptor>`, con
`id`, `name` e `description`: sulla carta è **esattamente** il posto in
cui un libro dichiarerebbe le proprie doti col proprio nome, senza
toccare il motore.

**Oggi però la UI non lo legge.** I nomi mostrati vengono da
`CreationCatalog.kt`, che li prende da `strings.xml`
(`R.string.discipline_weaponskill`, …). Il descrittore del manifest
serve solo al convertitore e al prompt di Gemma.

È una buona notizia per chi vuole cambiare sistema: il campo giusto
esiste già nello schema e i libri lo portano — manca solo che la
schermata di creazione preferisca il nome del libro, quando c'è, al
nome di `strings.xml`. È un lavoro piccolo e ben delimitato, ed è **la
strada che consiglierei** per slegarsi dai libri senza toccare né
l'enum né i cinque volumi già convertiti: ogni libro porta con sé il
proprio vocabolario, il motore resta com'è.
