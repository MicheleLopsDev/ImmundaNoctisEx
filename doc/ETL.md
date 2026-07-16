# Specifica 5 — ETL: conversione libri in pacchetti

Stato: **CHIUSA** (sessione 17/07/2026).

Contesto storico: la conversione dei libri fu il secondo blocco di v1.
Cause identificate: modello di estrazione poco capace × fonte sbagliata
(HTML, da cui il modello doveva ricostruire anche la struttura) ×
nessuna rete di validazione. Oggi tutti e tre i fattori sono ribaltati.

---

## Scoperta chiave: la Stele di Rosetta

**Kai Chronicles** (github.com/tonib/kaichronicles, GPL v3) contiene le
meccaniche dei libri 1-13 **codificate a mano** in XML
(`www/data/mechanics-X.xml`) più `objects.xml` (catalogo completo di
armi/oggetti, bilingue en/es, immagini mappate per libro). Verificato
sul libro 1: `<pick objectId=.../>`, `<randomTable>` con casi 0-9
(≡ nostro rollOnItemTable a intervalli espliciti), `<test
hasDiscipline=...>` (≡ nostre scelte-disciplina), `<setSkills/>`,
`<setDisciplines/>`.

Il lavoro che in v1 si chiedeva all'LLM (classificare la prosa
meccanica) **esiste già, fatto a mano dalla community**: il problema
da "classificazione fuzzy" diventa "traduzione deterministica tra due
formati".

## Le fonti (le scarica sempre L'UTENTE)

1. **XML Project Aon** (projectaon.org, licenza non commerciale):
   struttura del libro — sezioni numerate, testi, scelte con
   destinazioni esplicite, statistiche di combattimento marcate.
2. **mechanics-X.xml di Kai Chronicles** (GPL v3): le meccaniche.
3. **Downloader integrato nel tool** (primo passo del wizard,
   AVVIATO DALL'UTENTE): scarica i soli file necessari per i libri
   1-3 (XML libro + mechanics + objects.xml) nella cartella del
   tool. Precedente di community: Kai Chronicles stesso scarica i
   libri da Project Aon via HTTP. Buona educazione obbligatoria:
   una richiesta alla volta, cache locale (mai riscaricare),
   User-Agent onesto, e fallback manuale sempre visibile ("scarica
   da questi link e metti i file qui") se il sito non risponde o
   cambia. Niente script .bat/.sh esterni: il downloader vive nel
   tool Compose (una sola base di codice multipiattaforma, errori
   gestiti con UI vera).
4. Il tool non INCORPORA libri, meccaniche o chiavi API nel
   distribuibile: il contenuto arriva sempre sulla macchina
   dell'utente per iniziativa dell'utente.

## Modello legale

- Si distribuisce **lo strumento**, mai il contenuto (modello
  emulatore). L'app include solo il sample originale.
- I pacchetti convertiti sono per **uso personale** e non si
  distribuiscono mai (regola già sancita; compatibile sia con la
  licenza Project Aon sia con la GPL di KC, che vincola la
  distribuzione, non l'uso).
- Il tool resta nostro: legge formati, non linka codice GPL.
- Chi vuole la rifinitura LLM usa la propria chiave API o il proprio
  modello locale.

## Architettura del tool: Kotlin Multiplatform

La decisione "engine e data zero-Android" paga qui:

- `:core:data` (modelli, schema, **validatori**) → modulo KMP
  condiviso: il validatore è LO STESSO codice nell'app e nel tool.
  Mai più "il tool lo accetta ma l'app lo rifiuta".
- `:core:engine` → condiviso: il tool può **simulare** il libro
  convertito col vero motore (percorrere il grafo, verificare i
  combat, contare i finali raggiungibili).
- `:app` Android; `:tool` **Compose Desktop** (stessa sintassi UI
  dell'app) nello stesso repo.
- Plugin Kotlin Multiplatform già installato nell'IDE di Michele.

## Pipeline a stadi

1. **Struttura** (deterministico, zero LLM): parser dell'XML Aon →
   scheletro scene: id (= numero sezione), narrativeText, scelte con
   destinazioni, blocco combat (CS/END del nemico), sceneType.
2. **Meccaniche** (deterministico, zero LLM): convertitore
   mechanics-KC → tag Ex. Mappa iniziale: `pick`→addItem,
   `randomTable` a casi→rollOnItemTable/randomChoiceTable,
   `test hasDiscipline`→scelte-disciplina/salti condizionati,
   `object references`→catalogo da objects.xml. La mappa si estende
   costrutto per costrutto, guidata dal libro pilota.
3. **Rifinitura** (LLM, opzionale): SOLO gli arricchimenti che
   nessuna fonte ha — `locationName`, `toneHints`, assegnazione
   `backgroundImage` — e proposte per eventuali buchi. Dietro
   interfaccia (chiave API dell'utente o endpoint locale); il tool
   funziona anche SENZA LLM (arricchimenti a default).
4. **Validazione**: i validatori condivisi KMP (grafo chiuso,
   discipline canoniche, intervalli completi, destinazioni esistenti,
   winSceneId nei combat...).
5. **Simulazione**: il motore vero percorre il pacchetto (finali
   raggiungibili, scene orfane, combat impossibili).
6. **Report + revisione (human-in-the-loop)**: schermata Compose che
   elenca ogni tag generato CON la frase sorgente accanto —
   approva/correggi — più la lista dei costrutti KC non ancora
   mappati. ~350 sezioni a libro: rivedere i dubbi è un pomeriggio.

## Perimetro v0.1 (esperienza diretta di Michele dal primo tentativo)

- **Libro 1 pilota** ("Flight from the Dark"): conversione perfetta,
  costrutti mappati man mano che il libro li usa. Il libro 1 completo
  e giocabile in Ex è la prova regina dell'intero progetto.
- **Poi libri 2 e 3**: stessa struttura, scorrono con la stessa
  pipeline (verificato sul campo nel primo tentativo).
- **Oltre il 3: fuori perimetro v0.1.** Le meccaniche divergono
  (e dal 6 parte il ciclo Magnakai: discipline e gradi nuovi).
  Lavoro futuro, aggancia la nota di estensibilità di STATO.md
  (set di discipline per serie, adattamento ad altri sistemi).

## Code generate da questa specifica

- [ ] Piano di sviluppo: il tool entra nell'ordine di costruzione
  (dopo l'engine e i validatori, che riusa; prima o in parallelo alla
  UI app). La ristrutturazione del repo in moduli Gradle KMP
  (:core:data, :core:engine, :app, :tool) va decisa nel piano.
- [ ] Specifica 6: dimensione pacchetto libro completo (~350 scene:
  stimare JSON e impatto memoria/parsing su Razr); lunghezza
  narrativeText reali vs contesto Gemma.
- [ ] Manifest: il convertitore genera anche il manifest (id, title,
  language "en", disciplineChoices, deathSceneId — la sezione morte
  canonica del libro).
