package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import io.github.luposolitario.immundanoctisex.tool.inference.RiassuntorePercorso
import io.github.luposolitario.immundanoctisex.core.engine.inference.LinguaOutput
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.luposolitario.immundanoctisex.core.data.model.Choice
import io.github.luposolitario.immundanoctisex.core.data.model.CustomResourceEntry
import io.github.luposolitario.immundanoctisex.core.data.model.ImageReference
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.ScenePosition
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import io.github.luposolitario.immundanoctisex.core.data.validation.PackageValidator
import io.github.luposolitario.immundanoctisex.core.data.validation.ValidationResult
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.roundToInt

// Mappa del libro (doc/EDITOR.md §6). Auto-layout gerarchico come punto
// di partenza, trascinabile a mano scena per scena (§6.1); "Riordina
// automaticamente" scarta gli spostamenti manuali e ricentra pan/zoom.
// Pan libero sullo sfondo, zoom a pulsanti, colorazione verde/rosso dei
// nodi (§6.2), ricerca con ciclo tra corrispondenze, evidenziazione del
// vicinato al passaggio del mouse. Non ancora fatto: contorno di un
// percorso a richiesta.
// Nodo allargato e alzato (30/07/2026, Michele: "metti anche una parte
// della narrazione... dividi la chiave sulla prima riga e il testo su
// una seconda riga sotto") per mostrare id+codice (riga 1) ed estratto
// del testo narrato (riga 2) invece del solo id — spaziatura cresciuta
// di conseguenza per mantenere il distacco tra colonne/righe.
private val NODE_WIDTH = 190.dp
private val NODE_HEIGHT = 72.dp
private val H_SPACING = 230.dp
private val V_SPACING = 130.dp

// Vista compatta (06/08/2026, Michele: "non sembra leggibile facilmente
// da un umano"). Il problema non era il layout — provate e MISURATE tre
// strategie di posizionamento, tutte peggiori della griglia, vedi DIARIO
// 06/08 — ma la DIMENSIONE dei nodi: a zoom 20% "275 · SC-TRANS-275" è
// illeggibile lo stesso, e intanto occupa 190x72 punti. I grafi di
// Project Aon hanno nodi piccoli col solo numero, ed è per questo che a
// colpo d'occhio si vede la forma della storia invece di un muro.
private val NODE_WIDTH_COMPATTO = 62.dp
private val NODE_HEIGHT_COMPATTO = 34.dp
private val H_SPACING_COMPATTO = 78.dp
private val V_SPACING_COMPATTO = 56.dp

// Stato della vista mappa (zoom/pan/orientamento/posizioni trascinate a
// mano), tenuto DA CHI CHIAMA MapScreen (EditorMain.kt) invece che come
// `remember` locale (30/07/2026, bug segnalato da Michele: "dopo che ho
// aperto il dettaglio di scena cambia l'orientamento" — in realtà
// l'intero MapScreen viene distrutto e ricreato ogni volta che apri e
// chiudi una scena, essendo un ramo diverso del `when` in EditorMain,
// quindi ogni `remember` locale si azzerava: zoom, pan, orientamento,
// posizioni trascinate, tutto perso solo per aver guardato una scena).
// Con questo oggetto ricordato un livello sopra, sopravvive al giro
// mappa -> scena -> mappa.
class MapViewState {
    val zoom = mutableStateOf(1f)
    val panX = mutableStateOf(0f)
    val panY = mutableStateOf(0f)
    val orizzontale = mutableStateOf(false)
    // Vista compatta: nodi piccoli col solo numero, per vedere la forma
    // dell'intero libro invece dei dettagli di poche scene.
    val compatto = mutableStateOf(false)
    // NOTA (06/08/2026): qui stava per nascere un interruttore per
    // nascondere gli archi che tornano indietro (il 32% del primo libro,
    // e l'unica causa del groviglio: in avanti nessun arco salta piu' di
    // un livello). Sospeso: la strada giusta e' ELK, che instrada gli
    // archi invece di lasciarli attraversare la mappa in linea retta.
    // Vedi DIARIO 06/08 e ProvaElkTest.
    val posizioniManuali = mutableStateOf<Map<String, Offset>>(emptyMap())
    // Scene selezionate (30/07/2026, Michele: "quando clicco una scena
    // devi contornarla di un blu"; §17.3: "una multi selezione tenendo
    // premuto ctrl") — un insieme invece di un solo ID: click semplice lo
    // sostituisce con un singolo elemento, Ctrl+click aggiunge/rimuove.
    // Diversa dall'hover (nodoSottoMouse, si perde appena sposti il
    // mouse) e dal doppio click (che apre il pannello).
    val sceneSelezionate = mutableStateOf<Set<String>>(emptySet())
    // §19.13 (Ctrl+Z/Ctrl+Y): non uno stato Compose — nessuna UI legge
    // direttamente le pile della cronologia, solo `annulla`/`ripeti`,
    // che invece FANNO scattare la ricomposizione riassegnando
    // `schermata`/`posizioniManuali` (stati Compose veri) nel chiamante.
    val cronologia = CronologiaDocumento()
}

@Composable
fun rememberMapViewState(): MapViewState = remember { MapViewState() }

// §19.12 (Michele: "una mappa in testa con id e posizioni che viene
// saltata dal client"): hydrate di `posizioniManuali` da
// `Manifest.scenePositions` — chiamata SOLO nei punti in cui
// `EditorMain.kt` carica un libro da zero (apertura, ripristino
// backup, creazione nuova), mai nelle mutazioni in-sessione del
// manifest (altrimenti sovrascriverebbe posizioni appena trascinate
// con quelle, magari vuote, dell'ultimo salvataggio su disco). Il
// verso opposto (persistere `posizioniManuali` in `Manifest`) resta
// SOLO al momento del salvataggio (`salvaSu` sotto) — nessun altro
// posto tiene i due sincronizzati durante la sessione, di proposito.
// Nome del campo in `Manifest` in inglese (31/07/2026, Michele:
// "tutti gli attributi devono essere per standard scritti in
// inglese"): stessa convenzione già rispettata da ogni altro campo
// del modello dati, `posizioniMappa`/`PosizioneScena` erano
// un'eccezione introdotta per errore con §19.12, corretta qui.
fun MapViewState.caricaPosizioniDa(manifest: Manifest) {
    posizioniManuali.value = manifest.scenePositions.mapValues { (_, p) -> Offset(p.x, p.y) }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MapScreen(
    file: File,
    manifest: Manifest,
    warnings: List<String>,
    mapViewState: MapViewState,
    onTornaAvvio: () -> Unit,
    onSceneSelected: (String) -> Unit,
    onNuovaScena: () -> Unit,
    onDuplicaScena: (String) -> Unit,
    onEliminaScene: (Set<String>, String?) -> Unit,
    onManifestCambiato: (Manifest) -> Unit,
    salvataggioGiaConfermato: Boolean,
    onSalvataggioConfermato: () -> Unit,
    onFileCambiato: (File) -> Unit,
    livelliBackup: Int,
    onAnnullaUltimoBackup: () -> Unit,
    // §19.13 (Ctrl+Z/Ctrl+Y): la logica di annulla/ripeti vive in
    // EditorMain.kt (solo lì `schermata` è riassegnabile) — qui solo il
    // tasto scorciatoia, stesso pattern di tutti gli altri callback.
    onAnnullaModifica: () -> Unit,
    onRipetiModifica: () -> Unit,
    // §19.8 (bug 31/07/2026, Michele: "non esporta con le impostazioni
    // grafiche"): l'esportazione PNG deve rispecchiare tema/font/scala
    // testo correnti dell'editor, non valori fissi — ricevuti da
    // EditorMain.kt, dove vivono le preferenze (§16.1).
    temaScuro: Boolean,
    fontScelto: FontEditor,
    scalaTesto: ScalaTesto,
    // Null col modello non caricato: la voce "Riassumi il percorso" non
    // compare affatto, invece di comparire e non funzionare.
    riassuntore: RiassuntorePercorso? = null,
    linguaRiassunto: LinguaOutput = LinguaOutput.DEFAULT,
) {
    val graph = remember(manifest) { buildSceneGraph(manifest) }
    val scenesById = remember(manifest) { manifest.scenes.associateBy { it.id } }

    // Riassunto del cammino da START alla scena scelta (03/08/2026).
    // Lo stato vive qui perché la finestra deve restare aperta mentre si
    // naviga la mappa sotto, e il lavoro dura minuti.
    var riassuntoAperto by remember { mutableStateOf<StatoRiassunto?>(null) }
    val ambitoRiassunto = rememberCoroutineScope()
    fun avviaRiassunto(sceneId: String) {
        val motore = riassuntore ?: return
        val percorsoIds = percorsoDaStart(graph, sceneId)
        if (percorsoIds.isEmpty()) {
            riassuntoAperto = StatoRiassunto(
                sceneId = sceneId,
                errore = "La scena $sceneId non è raggiungibile dalla START: non esiste un percorso da riassumere.",
            )
            return
        }
        val scenePercorso = percorsoIds.mapNotNull { scenesById[it] }
        riassuntoAperto = StatoRiassunto(sceneId = sceneId, percorso = scenePercorso, inCorso = true)
        ambitoRiassunto.launch {
            val esito = motore.riassumi(
                scene = scenePercorso,
                titoloLibro = manifest.title,
                lingua = linguaRiassunto,
                onAvanzamento = { avanzamento ->
                    riassuntoAperto = riassuntoAperto?.copy(avanzamento = avanzamento)
                },
            )
            riassuntoAperto = riassuntoAperto?.copy(
                inCorso = false,
                testo = esito.getOrNull(),
                errore = esito.exceptionOrNull()?.message,
            )
        }
    }

    riassuntoAperto?.let { stato ->
        FinestraRiassunto(
            stato = stato,
            manifest = manifest,
            onChiudi = { riassuntoAperto = null },
        )
    }
    // Messaggio di conferma dopo "Salva libro" (30/07/2026, §10): niente
    // sparizione automatica col tempo, resta finché non tocchi altro —
    // niente coroutine/timer per un dettaglio così piccolo.
    var messaggioSalvataggio by remember { mutableStateOf<String?>(null) }
    // Conferma di sovrascrittura (30/07/2026, Michele: "se stai
    // sovrascrivendo la prima volta chiedimi conferma successivamente
    // no") — lo stato "già confermato" vive un livello sopra
    // (EditorMain.kt); qui solo QUALE file è in attesa di conferma (null =
    // nessun dialogo aperto). Vale sia per "Salva libro" sia per "Salva
    // con nome" quando il percorso scelto esiste già — la prima versione
    // si affidava all'avviso nativo di Windows sul FileDialog di
    // salvataggio, ma Michele ha verificato che non compare in pratica.
    var fileDaConfermare by remember { mutableStateOf<File?>(null) }
    // Validazione globale a richiesta (§8): il conteggio avvisi in testa
    // alla mappa resta quello del caricamento iniziale (non si ricalcola
    // da solo dopo una modifica) — questo pulsante rilancia
    // PackageValidator sull'intero manifest corrente, stesso codice della
    // CLI `validate`, zero logica duplicata.
    var risultatoValidazione by remember { mutableStateOf<ValidationResult?>(null) }
    // Editing del JSON completo del libro (§7.4, mai implementato finché
    // Michele non l'ha chiesto esplicitamente: "un tasto che nella
    // schermata principale ti permette di vedere tutto il file json") —
    // a differenza della vista JSON di una scena (§7.2, dentro
    // SceneEditorScreen), qui si vede/modifica l'intero Manifest in un
    // colpo solo. Stesso principio del resto dell'editor: NESSUNA
    // scrittura su disco senza validazione — "Applica" richiama
    // PackageValidator sull'intero manifest, un errore blocca
    // l'applicazione e resta a video (gli avvisi invece non bloccano,
    // come ovunque altrove).
    var mostraJsonLibro by remember { mutableStateOf(false) }
    var jsonLibroTesto by remember { mutableStateOf("") }
    var erroreJsonLibro by remember { mutableStateOf<String?>(null) }
    // Proprietà globali del libro (§7.5, Michele: "ci vuole un modo per
    // cambiare le proprietà globali del JSON"): prima fase, solo i campi
    // scalari semplici (titolo/descrizione/lingua/genere/id/versione),
    // i toni (stesso vocabolario chiuso di "Crea libro nuovo") e
    // deathSceneId. `disciplineChoices`/`globalRules` rimandati di
    // proposito — Michele: "quel tipo di informazioni devono essere
    // concordati con modifiche al client", non semplici campi di testo
    // isolati. Restano comunque raggiungibili dal "📄 JSON del libro"
    // appena sopra, per chi ne ha bisogno nel frattempo.
    var mostraProprietaLibro by remember { mutableStateOf(false) }
    var erroreProprietaLibro by remember { mutableStateOf<String?>(null) }
    // Conferma di eliminazione (§15.5/§17.3, Michele: "la possibilità di
    // cancellare... le scene selezionate", poi estesa a un insieme con
    // Ctrl+click) — azione distruttiva (anche se recuperabile dai
    // backup, §10), stesso principio della conferma di sovrascrittura
    // sopra: insieme vuoto = nessun dialogo aperto.
    var sceneIdsDaEliminare by remember { mutableStateOf<Set<String>>(emptySet()) }
    // Risorse personalizzate (§15.7, Michele: "risorse... fornite da chi
    // crea il libro"): pannello a parte, non un dialogo di conferma —
    // niente di distruttivo qui, si apre e si chiude liberamente.
    var mostraRisorsePersonalizzate by remember { mutableStateOf(false) }
    // "↩ Annulla" (§17.5, Michele: "l'annulla ti riporta al backup -1"):
    // disponibile solo se esiste già un backup per QUESTO file — niente
    // errore a sorpresa su un libro appena aperto senza salvataggi in
    // questa sessione. Aggiornato a true subito dopo ogni salvataggio
    // riuscito (che crea sempre almeno .bak1).
    var backupDisponibile by remember(file) { mutableStateOf(esisteBackup(file)) }
    var mostraConfermaAnnulla by remember { mutableStateOf(false) }

    fun salvaSu(destinazione: File) {
        // §19.12: le posizioni vive stanno in `mapViewState.posizioniManuali`
        // per tutta la sessione (mai scritte nel `manifest` in memoria, vedi
        // `caricaPosizioniDa` sopra) — finiscono nel `Manifest.scenePositions`
        // solo qui, nell'istante in cui si scrive davvero su disco.
        val manifestConPosizioni = manifest.copy(
            scenePositions = mapViewState.posizioniManuali.value.mapValues { (_, offset) ->
                ScenePosition(offset.x, offset.y)
            },
        )
        val json = Json { prettyPrint = true }.encodeToString(Manifest.serializer(), manifestConPosizioni)
        salvaLibro(destinazione, json, livelliBackup)
        messaggioSalvataggio = "✓ Salvato (backup in ${destinazione.name}.bak1)"
        backupDisponibile = true
    }

    fun salvaOChiediConferma(destinazione: File) {
        if (salvataggioGiaConfermato || !destinazione.exists()) {
            salvaSu(destinazione)
            if (destinazione != file) onFileCambiato(destinazione)
            onSalvataggioConfermato()
        } else {
            fileDaConfermare = destinazione
        }
    }

    var zoom by mapViewState.zoom
    var panX by mapViewState.panX
    var panY by mapViewState.panY
    var sceneSelezionate by mapViewState.sceneSelezionate
    // Tasto Ctrl tenuto premuto (§17.3): tracciato da un ascoltatore di
    // tastiera separato dal gesto di click, perché `detectTapGestures`
    // non espone i modificatori di tastiera nel suo `onPress`. Letto al
    // volo dentro `onPress` di ogni nodo.
    var ctrlPremuto by remember { mutableStateOf(false) }
    // Menu contestuale (§17.1, Michele: "tasto destro e di lì menu per
    // cancellare, duplicare"): quale scena l'ha aperto, null = chiuso.
    var menuContestualePer by remember { mutableStateOf<String?>(null) }
    // Focus da tastiera per Canc/Esc/Ctrl (§17.1/§17.3): richiesto al
    // primo click su un nodo o sullo sfondo, così i tasti funzionano
    // subito dopo un'interazione con la mappa senza un click a vuoto in
    // più; si perde naturalmente cliccando un campo di testo altrove
    // (es. la ricerca), dove i tasti non devono agire sulla mappa.
    val focusMappa = remember { FocusRequester() }
    // Spostamento enorme segnalato da Michele trascinando un nodo
    // (confermato: cursore e riquadro finivano in punti lontanissimi tra
    // loro, non solo un'impressione). Due rimedi insieme: (1) questo
    // stato tiene anche QUALE nodo è sotto trascinamento — non solo un
    // booleano — per bloccare il pan dello sfondo mentre trascini
    // (evita che i due effetti si sommino) e per mostrarlo nell'overlay
    // di debug delle coordinate; (2) il gesto stesso ora ancora la
    // posizione di partenza una sola volta (`onDragStart`) e accumula lo
    // scarto in una variabile locale alla coroutine invece di rileggere
    // `posizioneEffettiva` a ogni fotogramma — elimina la possibilità
    // che una lettura intermedia dello stato (soggetta ai tempi della
    // ricomposizione) introduca una deriva tra letture e scritture
    // successive.
    var nodoTrascinato by remember { mutableStateOf<String?>(null) }
    // Coordinate del mouse a schermo (30/07/2026, Michele: "cosi la
    // prossima volta quando prendo una schermata è più semplice capire
    // dove ero") — posizione grezza nel riquadro della mappa (stessa
    // unità già usata da `viewportSize`), non nella mappa "logica"
    // (post pan/zoom): è quella che si vede identica nello screenshot.
    var posizioneMouse by remember { mutableStateOf<Offset?>(null) }
    // Rettangolo di selezione (§19.10, Michele: "serve la multi
    // selezione tramite rettangolo quando ci sono tante foglie, è più
    // comodo"): coppia (inizio, punto attuale) in coordinate SCHERMO
    // (le stesse di `posizioneMouse`, prima di pan/zoom) — serve solo
    // per disegnare l'overlay, la conversione a coordinate logiche
    // (quelle di `posizioneEffettiva`) avviene al momento del test di
    // intersezione contro i nodi, non qui.
    var rettangoloSelezione by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    // Evidenziazione del vicinato (§6.1): al passaggio del mouse su un
    // nodo (non al click, che apre già il pannello di editing), i suoi
    // collegamenti diretti restano a piena opacità e il resto della
    // mappa si attenua. Calcolato dagli stessi `graph.edges` già usati
    // per disegnare gli archi, nessuna struttura dati nuova.
    var nodoSottoMouse by remember { mutableStateOf<String?>(null) }
    // Contorno di un percorso a richiesta (§6.2): "a richiesta" qui
    // significa "mentre passi il mouse su una scena", non un pulsante
    // dedicato — riusa lo stesso hover del vicinato invece di aggiungere
    // un'altra interazione. Un esempio di cammino da START a quella
    // scena (percorsoDaStart, SceneGraph.kt), sempre lo stesso finché non
    // cambi nodo sotto il mouse.
    val archiPercorso = remember(nodoSottoMouse, graph) {
        nodoSottoMouse?.let { percorsoDaStart(graph, it).zipWithNext().toSet() } ?: emptySet()
    }
    // Le coppie di scene collegate nei DUE versi: le loro linee vanno
    // scostate di lato, o si sovrappongono e se ne vede una sola
    // (06/08/2026). Calcolate una volta per grafo, non per ogni arco.
    val coppieDoppie = remember(graph) {
        GeometriaArchi.coppieBidirezionali(graph.edges.map { it.fromSceneId to it.toSceneId })
    }
    // Nodi sul percorso (30/07/2026, dopo la segnalazione di Michele "non
    // capisco... perché lo start alle volte è grigio e alle volte no"): il
    // vicinato (attenuazione) DEVE includere gli stessi nodi che la linea
    // viola già attraversa, altrimenti il grigio e il viola raccontano due
    // storie diverse. Prima il vicinato copriva solo i collegamenti
    // diretti (un salto), mentre il percorso poteva estendersi per più
    // salti: uno START lontano restava grigio anche con la linea viola che
    // lo raggiungeva. Unendo i due insiemi, START si attenua solo se la
    // scena sotto il mouse non è affatto raggiungibile da START (scena
    // orfana) — un caso raro e sensato da segnalare col grigio.
    val nodiPercorso = remember(archiPercorso) {
        archiPercorso.flatMap { (a, b) -> listOf(a, b) }.toSet()
    }
    // Vicinato SEMPLIFICATO (30/07/2026, dopo la terza segnalazione di
    // Michele sulla stessa confusione — "non capisco perché il
    // collegamento tra 6 -> 5 anche se sto selezionando il 6"): prima
    // restavano illuminati sia i nodi sul percorso viola SIA i vicini
    // diretti del nodo sotto il mouse, due criteri sovrapposti senza un
    // modo per distinguerli a vista — la scena 5 è davvero collegata
    // alla 6 (una scelta della 5 porta alla 6), quindi restava verde e
    // non attenuata pur non facendo parte del percorso da START mostrato
    // in viola, e sembrava un errore. Un solo criterio ora: illuminato
    // solo il nodo sotto il mouse e i nodi sul percorso viola: un
    // collegamento reale ma fuori da quel percorso si vede comunque
    // (l'arco resta disegnato) ma attenuato, coerente con "grigio = fuori
    // da quel percorso" della legenda.
    val vicinato = remember(nodoSottoMouse, nodiPercorso) {
        val centro = nodoSottoMouse
        if (centro == null) emptySet() else nodiPercorso + centro
    }
    // Ricerca (§6.1, prima solo nel mockup): per ID o per testo nel
    // codiceScena/narrativeText. Tutte le corrispondenze restano
    // evidenziate; ogni pressione del pulsante avanza a quella successiva
    // (ordine per ID crescente), ricominciando dalla prima dopo l'ultima
    // — stesso comportamento del Ctrl+F di un browser (30/07/2026,
    // Michele: "che ne pensi... uno trova e uno trova il successivo" ->
    // un solo pulsante che cicla, niente azione in più da imparare).
    var testoRicerca by remember { mutableStateOf("") }
    var ultimaRicerca by remember { mutableStateOf<String?>(null) }
    var corrispondenze by remember { mutableStateOf<List<Scene>>(emptyList()) }
    var indiceCorrente by remember { mutableStateOf(0) }
    var ricercaFallita by remember { mutableStateOf(false) }
    // Dimensione reale del riquadro della mappa (px), presa al volo
    // (onGloballyPositioned) — serve solo per calcolare il pan che centra
    // il nodo trovato, stessa unità "in pixel" già usata da
    // graphicsLayer/Modifier.offset in questo file.
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    // Orientamento dell'auto-layout (30/07/2026, Michele): finché non c'è
    // il trascinamento manuale dei singoli nodi (§6.1, "riordinare come
    // uno vuole"), questo pulsante è l'unico modo di cambiare la
    // disposizione — livelli in colonna (verticale, default) o in riga
    // (orizzontale).
    var orizzontale by mapViewState.orizzontale
    var compatto by mapViewState.compatto
    // Misure effettive del nodo: cambiano tutte insieme con la vista
    // compatta, perché dimensione e spaziatura devono restare in
    // proporzione o i nodi si toccano.
    val larghezzaNodo = if (compatto) NODE_WIDTH_COMPATTO else NODE_WIDTH
    val altezzaNodo = if (compatto) NODE_HEIGHT_COMPATTO else NODE_HEIGHT
    val passoH = if (compatto) H_SPACING_COMPATTO else H_SPACING
    val passoV = if (compatto) V_SPACING_COMPATTO else V_SPACING

    // Livello -> riga/colonna; le scene orfane (non raggiungibili da
    // START, vedi SceneGraph.kt) finiscono tutte sull'ultimo livello
    // invece che sparire.
    val orphanLevel = (graph.nodes.filter { it.level != Int.MAX_VALUE }.maxOfOrNull { it.level } ?: 0) + 1
    // L'ordine DENTRO ogni livello non e' piu' quello degli id
    // (deterministico ma cieco al grafo: due scene vicine di numero
    // possono stare ai capi opposti della storia, e ogni collegamento
    // fra loro attraversava tutta la mappa). Ora e' il baricentro dei
    // vicini, come fa Graphviz nei grafi che Project Aon pubblica —
    // vedi LayoutGerarchico.kt. Il numero resta come ordine di
    // PARTENZA, cosi' il risultato e' sempre lo stesso a parita' di
    // libro (§ordinamento deterministico per ID).
    val byLevel = remember(graph) {
        val gruppi = graph.nodes
            .groupBy { if (it.level == Int.MAX_VALUE) orphanLevel else it.level }
            .mapValues { (_, nodi) -> nodi.sortedBy { it.sceneId.toIntOrNull() ?: Int.MAX_VALUE } }
        val perId = graph.nodes.associateBy { it.sceneId }
        val ordinato = LayoutGerarchico.ordina(
            livelli = gruppi.mapValues { (_, nodi) -> nodi.map { it.sceneId } },
            archi = graph.edges.filter { it.resolved }.map { it.fromSceneId to it.toSceneId },
        )
        ordinato.mapValues { (_, ids) -> ids.mapNotNull { perId[it] } }
    }

    val positions = remember(graph, orizzontale) {
        // La spaziatura "tra livelli" resta legata alla dimensione del
        // nodo lungo l'asse su cui i livelli finiscono, non all'asse in
        // sé: ruotando la disposizione di 90°, chi prima spaziava le
        // colonne (H_SPACING, pensato per la larghezza del nodo) ora
        // spazia le righe, e viceversa per V_SPACING.
        val spazioLivelli = if (orizzontale) passoH.value else passoV.value
        val spazioFratelli = if (orizzontale) passoV.value else passoH.value
        buildMap {
            byLevel.forEach { (level, nodi) ->
                nodi.forEachIndexed { index, nodo ->
                    val posizione = if (orizzontale) {
                        Offset(level * spazioLivelli, index * spazioFratelli)
                    } else {
                        Offset(index * spazioFratelli, level * spazioLivelli)
                    }
                    put(nodo.sceneId, posizione)
                }
            }
        }
    }

    // Trascinamento manuale dei nodi (§6.1, "riordinare come uno vuole"):
    // scarti dalla posizione auto-calcolata, non salvati nel JSON (le
    // scene non hanno coordinate nello schema) — si perdono ricaricando
    // il libro o premendo "Riordina automaticamente", di proposito.
    // Azzerati esplicitamente al cambio di orientamento (pulsante
    // ↔/↕) e da "Riordina" — non più legati a un `remember` con chiave,
    // perché ora questo stato è ricordato un livello sopra (vedi
    // `MapViewState`) e sopravvive al giro mappa -> scena -> mappa.
    var posizioniManuali by mapViewState.posizioniManuali
    fun posizioneEffettiva(id: String): Offset? = posizioniManuali[id] ?: positions[id]

    // §19.10: geometria pura in RettangoloSelezione.kt (testata a
    // parte) — qui solo l'adattamento allo stato di questa schermata
    // (pan/zoom correnti, posizioni effettive dei nodi).
    fun nodiNelRettangoloAttuale(inizio: Offset, fine: Offset): Set<String> {
        val pan = Offset(panX, panY)
        val a = schermoALogico(inizio, pan, zoom)
        val b = schermoALogico(fine, pan, zoom)
        val posizioni = graph.nodes.mapNotNull { nodo -> posizioneEffettiva(nodo.sceneId)?.let { nodo.sceneId to it } }.toMap()
        return nodiNelRettangolo(graph.nodes, posizioni, a, b, larghezzaNodo.value, altezzaNodo.value)
    }

    // §15.4 (Michele: "riordino automatico alla pressione del tasto
    // centrale"): stessa azione del pulsante "⟳ Riordina", estratta qui
    // per essere richiamata anche dal tasto centrale del mouse — una
    // sola implementazione, due modi di attivarla.
    fun riordina() {
        zoom = 1f
        panX = 0f
        panY = 0f
        posizioniManuali = emptyMap()
    }

    fun eseguiRicerca() {
        val query = testoRicerca.trim()
        if (query.isBlank()) return
        if (query != ultimaRicerca) {
            // Query nuova (o prima ricerca): ricalcola tutte le
            // corrispondenze, per ID crescente, e riparte dalla prima.
            corrispondenze = manifest.scenes.filter {
                it.id.contains(query, ignoreCase = true) ||
                    codiceScena(it).contains(query, ignoreCase = true) ||
                    it.narrativeText.contains(query, ignoreCase = true)
            }.sortedBy { it.id.toIntOrNull() ?: Int.MAX_VALUE }
            indiceCorrente = 0
            ultimaRicerca = query
        } else if (corrispondenze.isNotEmpty()) {
            // Stessa query di prima: avanza alla corrispondenza
            // successiva, ricominciando dalla prima dopo l'ultima.
            indiceCorrente = (indiceCorrente + 1) % corrispondenze.size
        }
        val trovata = corrispondenze.getOrNull(indiceCorrente)
        val pos = trovata?.let { posizioneEffettiva(it.id) }
        if (trovata == null || pos == null) {
            ricercaFallita = true
            return
        }
        ricercaFallita = false
        panX = viewportSize.width / 2f - (pos.x + larghezzaNodo.value / 2f) * zoom
        panY = viewportSize.height / 2f - (pos.y + altezzaNodo.value / 2f) * zoom
    }

    // §19.4 (Michele: "centrare la vista sulla selezione"): calcola il
    // rettangolo che contiene tutte le scene selezionate e regola
    // pan/zoom per inquadrarle per intero, con un margine — stessa idea
    // del centraggio della ricerca sopra, ma su un insieme invece che su
    // un singolo nodo, e con lo zoom ricalcolato (non tenuto fisso)
    // perché un gruppo grande potrebbe non starci allo zoom corrente.
    fun centraSuSelezione() {
        val posizioni = sceneSelezionate.mapNotNull { posizioneEffettiva(it) }
        if (posizioni.isEmpty() || viewportSize.width == 0 || viewportSize.height == 0) return
        val minX = posizioni.minOf { it.x }
        val minY = posizioni.minOf { it.y }
        val maxX = posizioni.maxOf { it.x } + larghezzaNodo.value
        val maxY = posizioni.maxOf { it.y } + altezzaNodo.value
        val margine = 80f
        val zoomCheStaNellaLarghezza = (viewportSize.width - margine) / (maxX - minX).coerceAtLeast(1f)
        val zoomCheStaNellAltezza = (viewportSize.height - margine) / (maxY - minY).coerceAtLeast(1f)
        zoom = minOf(zoomCheStaNellaLarghezza, zoomCheStaNellAltezza).coerceIn(0.2f, 3f)
        panX = viewportSize.width / 2f - (minX + maxX) / 2f * zoom
        panY = viewportSize.height / 2f - (minY + maxY) / 2f * zoom
    }

    // §19.1 (Michele: "se seleziono due scene non legate mi dai 2
    // opzioni... Lega scena X->Y... se già legate Rimuovi legame X->Y"):
    // aggiunge/toglie una Choice ORDINARIA — scelte per disciplina e
    // link di combattimento restano da fare nella scheda della scena
    // (§19.1, deliberatamente fuori perimetro). "Lega" usa un testo
    // segnaposto (come le scene create da zero, §9): non c'è modo di
    // sapere cosa scrivere al posto dell'autore, solo che il
    // collegamento deve esistere.
    fun legaScena(daId: String, aId: String) {
        val nuoveScene = manifest.scenes.map { scena ->
            if (scena.id == daId) {
                scena.copy(choices = scena.choices + Choice(id = "link_$aId", choiceText = "Vai avanti...", nextSceneId = aId))
            } else {
                scena
            }
        }
        onManifestCambiato(manifest.copy(scenes = nuoveScene))
    }

    fun rimuoviLegame(daId: String, aId: String) {
        val nuoveScene = manifest.scenes.map { scena ->
            if (scena.id == daId) scena.copy(choices = scena.choices.filterNot { it.nextSceneId == aId }) else scena
        }
        onManifestCambiato(manifest.copy(scenes = nuoveScene))
    }

    // §19.2 (Michele: azioni utili sui gruppi): duplica l'intera
    // selezione in un colpo solo, con gli ID nuovi allocati TUTTI
    // insieme (`duplicaGruppo`, NuovaScena.kt) — a differenza di
    // "Duplica" singola, i collegamenti TRA scene del gruppo puntano
    // alle copie, non agli originali (altrimenti il gruppo duplicato
    // sarebbe solo un ammasso di nodi scollegati fra loro). Il nuovo
    // gruppo diventa la selezione corrente, non apre alcun editor —
    // resta sulla mappa per un eventuale trascina/lega successivo.
    fun duplicaGruppoESeleziona(ids: Set<String>) {
        val originali = manifest.scenes.filter { it.id in ids }
        val duplicati = duplicaGruppo(originali, manifest)
        onManifestCambiato(manifest.copy(scenes = manifest.scenes + duplicati))
        sceneSelezionate = duplicati.map { it.id }.toSet()
    }

    // §19.8 (Michele: "esportare la mappa come immagine PNG"): la mappa
    // LOGICA intera, non lo screenshot del riquadro — indipendente da
    // pan/zoom correnti, usa `posizioneEffettiva` (auto-layout + scarti
    // manuali) come la vista interattiva. Nome suggerito dal file del
    // libro, stessa cartella.
    fun esportaImmagine() {
        val nomeSuggerito = file.nameWithoutExtension + ".png"
        val destinazione = scegliPercorsoEsportazioneImmagine(nomeSuggerito) ?: return
        val conEstensione = if (destinazione.extension.equals("png", ignoreCase = true)) {
            destinazione
        } else {
            File(destinazione.parentFile, "${destinazione.name}.png")
        }
        val posizioni = graph.nodes.associate { it.sceneId to (posizioneEffettiva(it.sceneId) ?: Offset.Zero) }
        // 31/07/2026 (Michele: "mettiamo in alto il nome del file
        // stesso formato che usiamo per caricare i libri"): stessa
        // stringa di "Libri recenti" (`riepilogoLibro`, EditorMain.kt).
        esportaMappaComeImmagine(
            conEstensione, graph.nodes, graph.edges, scenesById, posizioni, temaScuro, fontScelto, scalaTesto,
            intestazione = riepilogoLibro(file.name, manifest),
        )
    }

    // §19.11 (Michele: "un'opzione per riarrangiare solo i nodi
    // selezionati in verticale o in orizzontale"): geometria in
    // `AllineamentoGruppo.kt` (testata) — qui solo l'adattamento allo
    // stato di questa schermata (posizioni effettive, spaziatura della
    // stessa griglia usata dall'auto-layout). Scrive su
    // `posizioniManuali` come il trascinamento a mano: uno scarto
    // dall'auto-layout, non salvato nel JSON, si perde con "Riordina".
    fun allineaSelezione(inRiga: Boolean) {
        if (sceneSelezionate.size < 2) return
        mapViewState.cronologia.registraCheckpoint(Documento(manifest, posizioniManuali))
        val posizioniAttuali = sceneSelezionate.associateWith { posizioneEffettiva(it) ?: Offset.Zero }
        val spaziatura = if (inRiga) passoH.value else passoV.value
        posizioniManuali = posizioniManuali + allineaGruppo(sceneSelezionate, posizioniAttuali, orizzontale = inRiga, spaziatura = spaziatura)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            // La forma del libro, appena caricato (06/08/2026, Michele:
            // "sarebbe bello che questa cosa venga messa in alto così
            // sarebbe a occhio per l'editor vedere se la cosa va o non
            // va"). Prima di ogni pulsante: e' la prima cosa da vedere.
            SondeDiFormaBar(manifest, modifier = Modifier.padding(bottom = 8.dp))
            // 30/07/2026, Michele: prima due Row separate (una a sinistra
            // coi pulsanti, una a destra con orizzontale/riordina/zoom)
            // dentro una Row esterna con SpaceBetween — con la finestra
            // ridimensionata più stretta della somma di tutti i pulsanti,
            // una Row normale non va a capo da sola: il gruppo di destra
            // finiva schiacciato o fuori dalla vista invece di restare
            // leggibile. FlowRow va a capo da solo quando serve, a
            // qualunque larghezza della finestra — stessi pulsanti, ora
            // tutti in un unico gruppo che si adatta.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(onClick = onTornaAvvio) { Text("← Torna all'avvio") }
                Button(onClick = { salvaOChiediConferma(file) }) { Text("💾 Salva libro") }
                Button(onClick = {
                    scegliPercorsoSalvataggio(file.name)?.let(::salvaOChiediConferma)
                }) { Text("Salva con nome…") }
                // §17.5 (Michele: "l'annulla ti riporta al backup
                // -1"): disabilitato senza un .bak1 per questo file,
                // conferma esplicita prima di eseguire (distruttivo
                // per lo stato non salvato).
                Button(
                    onClick = { mostraConfermaAnnulla = true },
                    enabled = backupDisponibile,
                ) { Text("↩ Annulla") }
                Button(onClick = { risultatoValidazione = PackageValidator.validate(manifest) }) {
                    Text("🔍 Valida libro")
                }
                // §7.4 (Michele: "un tasto che nella schermata principale
                // ti permette di vedere tutto il file json").
                Button(onClick = {
                    jsonLibroTesto = Json { prettyPrint = true }.encodeToString(Manifest.serializer(), manifest)
                    erroreJsonLibro = null
                    mostraJsonLibro = true
                }) { Text("📄 JSON del libro") }
                // §7.5 (Michele: "un modo per cambiare le proprietà
                // globali del JSON").
                Button(onClick = {
                    erroreProprietaLibro = null
                    mostraProprietaLibro = true
                }) { Text("⚙ Proprietà del libro") }
                // §15.5/§17.3 (Michele: "la possibilità di cancellare
                // o aggiungere le scene selezionate", poi estesa a un
                // insieme): "Nuova scena" sempre attivo, "Elimina"
                // solo con almeno una scena selezionata.
                Button(onClick = onNuovaScena) { Text("+ Nuova scena") }
                Button(
                    onClick = { if (sceneSelezionate.isNotEmpty()) sceneIdsDaEliminare = sceneSelezionate },
                    enabled = sceneSelezionate.isNotEmpty(),
                ) { Text("🗑 Elimina scena") }
                // §19.4 (Michele: "centrare la vista sulla selezione").
                Button(
                    onClick = { centraSuSelezione() },
                    enabled = sceneSelezionate.isNotEmpty(),
                ) { Text("🎯 Centra selezione") }
                // §19.11 (Michele: "un'opzione per riarrangiare solo i
                // nodi selezionati in verticale o in orizzontale") —
                // attivi solo con 2+ selezionate, un ordinamento da solo
                // non ha senso.
                Button(
                    onClick = { allineaSelezione(inRiga = true) },
                    enabled = sceneSelezionate.size >= 2,
                ) { Text("↔ Allinea in riga") }
                Button(
                    onClick = { allineaSelezione(inRiga = false) },
                    enabled = sceneSelezionate.size >= 2,
                ) { Text("↕ Allinea in colonna") }
                Button(onClick = { mostraRisorsePersonalizzate = true }) { Text("🔗 Risorse url:") }
                Button(onClick = ::esportaImmagine) { Text("🖼 Esporta come immagine") }
                Button(onClick = { orizzontale = !orizzontale; posizioniManuali = emptyMap() }) {
                    Text(if (orizzontale) "↕ Verticale" else "↔ Orizzontale")
                }
                // Vista compatta (06/08/2026): nodi piccoli col solo
                // numero, come i grafi di Project Aon — per guardare la
                // forma del libro invece del contenuto delle scene.
                // Azzera le posizioni trascinate, che sono calcolate
                // sulla spaziatura precedente.
                Button(onClick = { compatto = !compatto; posizioniManuali = emptyMap() }) {
                    Text(if (compatto) "⬜ Nodi grandi" else "▫ Nodi compatti")
                }
                Button(onClick = ::riordina) { Text("⟳ Riordina") }
                Button(onClick = { zoom = (zoom - 0.1f).coerceAtLeast(0.2f) }) { Text("−") }
                Text("${(zoom * 100).roundToInt()}%", modifier = Modifier.align(Alignment.CenterVertically))
                Button(onClick = { zoom = (zoom + 0.1f).coerceAtMost(3f) }) { Text("+") }
            }
            Text(
                "${manifest.title} — ${manifest.scenes.size} scene, ${warnings.size} avvisi al caricamento",
                style = MaterialTheme.typography.bodySmall,
            )
            // §17.3: contatore visibile solo con più di una scena
            // selezionata, altrimenti il contorno blu sui nodi basta
            // da solo (comportamento di oggi, invariato).
            if (sceneSelezionate.size > 1) {
                Text(
                    "${sceneSelezionate.size} scene selezionate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            // Legenda colori (30/07/2026, Michele: "non capisco cosa
            // rappresenta... che vuol dire viola e verde?"): sempre
            // visibile invece di lasciarla solo a parole in chat, così
            // resta consultabile ogni volta che serve.
            Text(
                "🟩 collegamento valido  🟥 collegamento a scena inesistente  " +
                    "🟪 percorso da START alla scena sotto il mouse  " +
                    "grigio = fuori da quel percorso (anche se il collegamento esiste)  " +
                    "🩷 scena START  💛 scena ENDING  " +
                    "👻 scena orfana (non raggiungibile da START)  " +
                    "⛔ vicolo cieco (nessuna uscita)  " +
                    "➤ la punta indica dove porta il collegamento; due linee affiancate " +
                    "= le scene si raggiungono a vicenda",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            messaggioSalvataggio?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = testoRicerca,
                onValueChange = {
                    testoRicerca = it
                    ricercaFallita = false
                    // Testo cambiato -> la prossima pressione del pulsante
                    // deve ripartire dalla prima corrispondenza, non
                    // avanzare su risultati ormai superati.
                    ultimaRicerca = null
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Cerca scena per ID o testo...") },
            )
            Button(onClick = { eseguiRicerca() }) { Text("🔍 Trova") }
            if (ricercaFallita) {
                Text("Nessuna corrispondenza", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            } else if (corrispondenze.isNotEmpty() && testoRicerca == ultimaRicerca) {
                Text(
                    "${indiceCorrente + 1} di ${corrispondenze.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        fileDaConfermare?.let { destinazione ->
            AlertDialog(
                onDismissRequest = { fileDaConfermare = null },
                title = { Text("Sovrascrivere il file?") },
                text = { Text("Stai per sovrascrivere ${destinazione.name}. Verrà creato un backup automatico prima di scrivere. Questa domanda non verrà più chiesta per il resto della sessione.") },
                confirmButton = {
                    TextButton(onClick = {
                        salvaSu(destinazione)
                        if (destinazione != file) onFileCambiato(destinazione)
                        onSalvataggioConfermato()
                        fileDaConfermare = null
                    }) { Text("Sovrascrivi") }
                },
                dismissButton = {
                    TextButton(onClick = { fileDaConfermare = null }) { Text("Annulla") }
                },
            )
        }

        if (sceneIdsDaEliminare.isNotEmpty()) {
            val idsOrdinati = sceneIdsDaEliminare.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
            // §19.5 (Michele: "Ricollegamento opzionale alla
            // cancellazione"): vuoto di default = comportamento invariato
            // (riferimenti restano rossi). `remember(sceneIdsDaEliminare)`
            // riparte da vuoto ogni volta che si apre il dialogo su un
            // insieme diverso di scene.
            var idRicollegamento by remember(sceneIdsDaEliminare) { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { sceneIdsDaEliminare = emptySet() },
                title = {
                    Text(
                        if (idsOrdinati.size == 1) "Eliminare la scena ${idsOrdinati.first()}?"
                        else "Eliminare ${idsOrdinati.size} scene?",
                    )
                },
                text = {
                    Column {
                        Text(
                            if (idsOrdinati.size == 1) {
                                "I collegamenti di altre scene verso ${idsOrdinati.first()} resteranno come riferimenti a una " +
                                    "scena non più esistente (si vedono rossi sulla mappa, non vengono corretti da soli)."
                            } else {
                                "Scene coinvolte: ${idsOrdinati.joinToString(", ")}. I collegamenti di altre scene verso di " +
                                    "loro resteranno come riferimenti a scene non più esistenti (si vedono rossi sulla mappa, " +
                                    "non vengono corretti da soli)."
                            },
                        )
                        Spacer(Modifier.height(12.dp))
                        DestinazioneField(
                            valore = idRicollegamento,
                            tutteLeScene = manifest.scenes.filter { it.id !in sceneIdsDaEliminare },
                            onValueChange = { idRicollegamento = it },
                            etichetta = "Ricollega i riferimenti in ingresso verso: (vuoto = non ricollegare)",
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onEliminaScene(sceneIdsDaEliminare, idRicollegamento.ifBlank { null })
                        sceneSelezionate = sceneSelezionate - sceneIdsDaEliminare
                        sceneIdsDaEliminare = emptySet()
                    }) { Text("Elimina") }
                },
                dismissButton = {
                    TextButton(onClick = { sceneIdsDaEliminare = emptySet() }) { Text("Annulla") }
                },
            )
        }

        // §17.5 (Michele: "l'annulla ti riporta al backup -1"):
        // distruttivo per lo stato non salvato, conferma esplicita come
        // per l'eliminazione di una scena — non un'azione silenziosa.
        if (mostraConfermaAnnulla) {
            AlertDialog(
                onDismissRequest = { mostraConfermaAnnulla = false },
                title = { Text("Tornare all'ultimo backup?") },
                text = {
                    Text(
                        "Il libro tornerà com'era prima dell'ultimo salvataggio. Qualunque modifica fatta da " +
                            "allora (salvata o no) andrà persa.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        mostraConfermaAnnulla = false
                        onAnnullaUltimoBackup()
                    }) { Text("Annulla il salvataggio") }
                },
                dismissButton = {
                    TextButton(onClick = { mostraConfermaAnnulla = false }) { Text("Chiudi") }
                },
            )
        }

        risultatoValidazione?.let { risultato ->
            AlertDialog(
                onDismissRequest = { risultatoValidazione = null },
                title = {
                    Text(
                        if (risultato.errors.isEmpty()) "Libro VALIDO (${risultato.warnings.size} avvisi)" else "Libro NON VALIDO",
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        risultato.errors.forEach {
                            Text("• $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        risultato.warnings.forEach {
                            Text("• $it", style = MaterialTheme.typography.bodySmall)
                        }
                        if (risultato.errors.isEmpty() && risultato.warnings.isEmpty()) {
                            Text("Nessun errore, nessun avviso.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { risultatoValidazione = null }) { Text("Chiudi") }
                },
            )
        }

        // §7.4 (Michele: "un tasto che nella schermata principale ti
        // permette di vedere tutto il file json"): stesso principio
        // della vista JSON di una scena (SceneEditorScreen §7.2), ma per
        // l'intero libro in un colpo solo — mai scritto su disco senza
        // validazione, "Applica" richiama PackageValidator sull'intero
        // manifest e blocca solo su errori veri (gli avvisi passano,
        // come ovunque nel resto dell'editor).
        if (mostraJsonLibro) {
            Dialog(
                onDismissRequest = { mostraJsonLibro = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .fillMaxHeight(0.9f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                ) {
                    Text("JSON completo del libro", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonLibroTesto,
                        onValueChange = { jsonLibroTesto = it },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        label = { Text("Manifest (JSON)") },
                    )
                    erroreJsonLibro?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val manifestDecodificato = try {
                                Json.decodeFromString(Manifest.serializer(), jsonLibroTesto)
                            } catch (e: SerializationException) {
                                erroreJsonLibro = "JSON non valido — ${e.message}"
                                null
                            }
                            if (manifestDecodificato != null) {
                                val risultato = PackageValidator.validate(manifestDecodificato)
                                if (risultato.errors.isNotEmpty()) {
                                    erroreJsonLibro = risultato.errors.joinToString("\n") { errore -> "• $errore" }
                                } else {
                                    onManifestCambiato(manifestDecodificato)
                                    mostraJsonLibro = false
                                }
                            }
                        }) { Text("Applica") }
                        TextButton(onClick = { mostraJsonLibro = false }) { Text("Chiudi") }
                    }
                }
            }
        }

        // §7.5 (Michele: "un modo per cambiare le proprietà globali del
        // JSON"): prima fase, solo campi scalari + toni (vocabolario
        // chiuso, stesso di "Crea libro nuovo") + deathSceneId.
        // disciplineChoices/globalRules rimandati apposta — Michele:
        // "quel tipo di informazioni devono essere concordati con
        // modifiche al client" — restano raggiungibili dal "📄 JSON del
        // libro" sopra. Stesso principio del resto dell'editor: nessuna
        // scrittura senza validazione.
        if (mostraProprietaLibro) {
            var titolo by remember(mostraProprietaLibro) { mutableStateOf(manifest.title) }
            var descrizione by remember(mostraProprietaLibro) { mutableStateOf(manifest.description) }
            var lingua by remember(mostraProprietaLibro) { mutableStateOf(manifest.language) }
            var genere by remember(mostraProprietaLibro) { mutableStateOf(manifest.genre) }
            var idLibro by remember(mostraProprietaLibro) { mutableStateOf(manifest.id) }
            var versione by remember(mostraProprietaLibro) { mutableStateOf(manifest.version) }
            var deathSceneId by remember(mostraProprietaLibro) { mutableStateOf(manifest.deathSceneId.orEmpty()) }
            // Un tono è "selezionato" se almeno una delle sue parole
            // grezze è già in toneHints — stesso criterio usato al
            // salvataggio per ricostruirle (§15.2), semplice e coerente.
            var toniSelezionati by remember(mostraProprietaLibro) {
                mutableStateOf(
                    StaticResourceCatalog.registry.tones
                        .filter { tono -> tono.hints.any { it in manifest.toneHints } }
                        .map { it.id }
                        .toSet(),
                )
            }

            Dialog(
                onDismissRequest = { mostraProprietaLibro = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .fillMaxHeight(0.85f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text("Proprietà del libro", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Discipline e regole globali non sono qui: vanno concordate con eventuali " +
                            "modifiche al client — usa \"📄 JSON del libro\" per quelle nel frattempo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = titolo,
                        onValueChange = { titolo = it },
                        label = { Text("Titolo") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = descrizione,
                        onValueChange = { descrizione = it },
                        label = { Text("Descrizione") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = lingua,
                            onValueChange = { lingua = it },
                            label = { Text("Lingua") },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = genere,
                            onValueChange = { genere = it },
                            label = { Text("Genere") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = idLibro,
                            onValueChange = { idLibro = it },
                            label = { Text("ID libro") },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = versione,
                            onValueChange = { versione = it },
                            label = { Text("Versione") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    Text("Tono narrativo (puoi sceglierne più di uno)", style = MaterialTheme.typography.titleMedium)
                    StaticResourceCatalog.registry.tones.forEach { tono ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = tono.id in toniSelezionati,
                                onCheckedChange = { selezionato ->
                                    toniSelezionati = if (selezionato) toniSelezionati + tono.id else toniSelezionati - tono.id
                                },
                            )
                            Text(tono.displayName)
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    Text("Scena di morte fuori combattimento (deathSceneId)", style = MaterialTheme.typography.titleMedium)
                    DestinazioneField(
                        valore = deathSceneId,
                        tutteLeScene = manifest.scenes,
                        onValueChange = { deathSceneId = it },
                        etichetta = "ID scena (vuoto = nessuna)",
                    )

                    erroreProprietaLibro?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val toneHints = StaticResourceCatalog.registry.tones
                                .filter { it.id in toniSelezionati }
                                .flatMap { it.hints }
                                .distinct()
                            val manifestAggiornato = manifest.copy(
                                id = idLibro,
                                version = versione,
                                title = titolo,
                                description = descrizione,
                                language = lingua,
                                genre = genere,
                                toneHints = toneHints,
                                deathSceneId = deathSceneId.ifBlank { null },
                            )
                            val risultato = PackageValidator.validate(manifestAggiornato)
                            if (risultato.errors.isNotEmpty()) {
                                erroreProprietaLibro = risultato.errors.joinToString("\n") { errore -> "• $errore" }
                            } else {
                                onManifestCambiato(manifestAggiornato)
                                mostraProprietaLibro = false
                            }
                        }) { Text("Applica") }
                        TextButton(onClick = { mostraProprietaLibro = false }) { Text("Chiudi") }
                    }
                }
            }
        }

        // §15.7, secondo giro (Michele: "vorrei poter anche vedere la
        // lista delle risorse già presenti... direi di trasformare la
        // popup"): non più un piccolo `AlertDialog` con solo il modulo
        // per aggiungere, ma un pannello più ampio (`Dialog` a schermo
        // quasi pieno) con anche l'elenco di quello che il libro usa
        // già — con anteprima, non solo testo.
        if (mostraRisorsePersonalizzate) {
            Dialog(
                onDismissRequest = { mostraRisorsePersonalizzate = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .fillMaxHeight(0.85f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                ) {
                    Text("Risorse del libro", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Le voci personalizzate sono solo un promemoria per te: qui scegli " +
                            "un'immagine/suono dalla lista, il campo della scena salva comunque " +
                            "url:<link> per intero.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        SezioneImmaginiInUso(
                            manifest = manifest,
                            onManifestCambiato = onManifestCambiato,
                        )
                        Spacer(Modifier.height(16.dp))
                        // 30/07/2026, Michele: "non capisco perché ci sono
                        // due volte gli url delle immagini" — dopo una
                        // bonifica (§15.7) il registro contiene le stesse
                        // immagini già mostrate sopra come "in uso", quindi
                        // comparivano due volte con lo stesso significato.
                        // Qui restano SOLO le voci registrate che NESSUNA
                        // scena usa ancora — un elenco diverso da quello
                        // sopra, non un doppione: risorse pronte per quando
                        // servirà agganciarle a una scena.
                        val urlGiaInUso = remember(manifest) {
                            manifest.scenes
                                .flatMap { listOfNotNull(it.backgroundImage, it.npcImage, it.combat?.enemyImage) }
                                .filter { it.startsWith(ImageReference.URL_PREFIX) }
                                .map { it.removePrefix(ImageReference.URL_PREFIX) }
                                .toSet()
                        }
                        SezioneRisorsePersonalizzate(
                            titolo = "Altre immagini registrate (non ancora usate in una scena)",
                            voci = manifest.customResources.images.filter { it.url !in urlGiaInUso },
                            onCambia = { nuoveVociNonInUso ->
                                val vociInUso = manifest.customResources.images.filter { it.url in urlGiaInUso }
                                onManifestCambiato(
                                    manifest.copy(customResources = manifest.customResources.copy(images = vociInUso + nuoveVociNonInUso)),
                                )
                            },
                        )
                        Spacer(Modifier.height(16.dp))
                        // §18.4 (Michele: "puoi associare un url vero e
                        // proprio oppure una risorsa statica presente nel
                        // apk... avrai un id univoco che userai
                        // indipendentemente dal tipo richiamato"): a
                        // differenza delle immagini (sempre un url nudo),
                        // un suono personalizzato può valere anche
                        // "static:<location>" — riusa i suoni ambientali
                        // già bundlati, così Scene.sfx punta sempre solo
                        // a un ID senza sapere cosa c'è dietro.
                        val suoniStaticiDisponibili = remember { suoniStaticiDiDefault().map { it.id } }
                        SezioneRisorsePersonalizzate(
                            titolo = "Suoni personalizzati",
                            voci = manifest.customResources.sounds,
                            onCambia = { nuoveVoci ->
                                onManifestCambiato(manifest.copy(customResources = manifest.customResources.copy(sounds = nuoveVoci)))
                            },
                            suggerimentiRisorseStatiche = suoniStaticiDisponibili,
                        )
                        // §18.4 (Michele: "mi crei per default già tutti
                        // gli id per i suoni statici presenti nel apk"):
                        // per i libri creati PRIMA di questa modifica (o
                        // aperti da fuori l'editor), un pulsante fa la
                        // stessa cosa in un click — solo le voci che
                        // mancano ancora, non tocca quelle già presenti
                        // (magari modificate a mano nel frattempo).
                        val idGiaRegistrati = remember(manifest) { manifest.customResources.sounds.map { it.id }.toSet() }
                        val vociMancanti = remember(idGiaRegistrati) {
                            suoniStaticiDiDefault().filter { it.id !in idGiaRegistrati }
                        }
                        if (vociMancanti.isNotEmpty()) {
                            TextButton(onClick = {
                                onManifestCambiato(
                                    manifest.copy(
                                        customResources = manifest.customResources.copy(
                                            sounds = manifest.customResources.sounds + vociMancanti,
                                        ),
                                    ),
                                )
                            }) { Text("+ Aggiungi tutti i suoni del catalogo (${vociMancanti.size})") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { mostraRisorsePersonalizzate = false },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("Chiudi") }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                // Focus da tastiera (§17.1/§17.3): serve per ricevere
                // Canc/Esc/Ctrl. Richiesto esplicitamente sotto, non
                // automatico all'apertura della mappa — coerente con
                // "il focus segue l'ultima area cliccata", non un
                // furto di focus a sorpresa da un campo di testo.
                .focusRequester(focusMappa)
                .focusable()
                .onKeyEvent { evento ->
                    when {
                        evento.key == Key.CtrlLeft || evento.key == Key.CtrlRight -> {
                            ctrlPremuto = evento.type == KeyEventType.KeyDown
                            false
                        }
                        evento.type != KeyEventType.KeyDown -> false
                        // §17.1/§17.3 (Michele: "la seleziono e premo
                        // canc"): stesso comportamento del pulsante "🗑
                        // Elimina scena", singola o multipla.
                        evento.key == Key.Delete || evento.key == Key.Backspace -> {
                            if (sceneSelezionate.isNotEmpty()) {
                                sceneIdsDaEliminare = sceneSelezionate
                                true
                            } else {
                                false
                            }
                        }
                        // Deseleziona tutto (oggi solo il click sullo
                        // sfondo lo fa) — comodo con la multi-selezione.
                        evento.key == Key.Escape -> {
                            sceneSelezionate = emptySet()
                            true
                        }
                        // §19.13 (Michele: "implementiamo ctrl-z e
                        // ctrl-y"): cronologia lineare in memoria, non il
                        // ripristino da backup su disco di "↩ Annulla"
                        // (§17.5) — copre le azioni di struttura sulla
                        // mappa (trascinamenti, allineamenti, elimina,
                        // lega/rimuovi legame, duplica gruppo, pannelli
                        // che passano da `onManifestCambiato`), non
                        // ancora le modifiche dentro la scheda di una
                        // singola scena (fuori perimetro di questo primo
                        // giro, §19.13).
                        evento.key == Key.Z && ctrlPremuto -> {
                            onAnnullaModifica()
                            true
                        }
                        evento.key == Key.Y && ctrlPremuto -> {
                            onRipetiModifica()
                            true
                        }
                        else -> false
                    }
                }
                // 30/07/2026, Michele: "le immagini centrali potessero
                // uscire fuori dallo sfondo e andare sopra i controlli"
                // — senza un clip esplicito, un nodo che il pan porta
                // sopra il bordo superiore del riquadro (specie con
                // un'immagine di sfondo piena, §15.3) non veniva
                // ritagliato: disegnandosi DOPO la barra strumenti nella
                // composizione, ci finiva sopra invece che restarne
                // coperto. Il riquadro ora ritaglia per davvero i propri
                // contenuti ai propri confini.
                .clipToBounds()
                // Sfondo del tema (30/07/2026: prima era un grigio fisso,
                // restava chiaro anche col tema scuro attivo).
                .background(MaterialTheme.colorScheme.surfaceVariant)
                // Dimensione reale del riquadro (30/07/2026, §6.1): serve
                // solo per centrare la ricerca — vedi eseguiRicerca().
                .onGloballyPositioned { viewportSize = it.size }
                .onPointerEvent(PointerEventType.Move) {
                    posizioneMouse = it.changes.firstOrNull()?.position
                }
                .onPointerEvent(PointerEventType.Exit) { posizioneMouse = null }
                // §15.4 (Michele): rotella del mouse -> zoom, stesso
                // effetto dei pulsanti −/+ (non li sostituisce). Verso
                // in alto (scrollDelta.y negativo) avvicina, in basso
                // allontana — la stessa convenzione di mappe/editor
                // grafici comuni.
                .onPointerEvent(PointerEventType.Scroll) {
                    val scarto = it.changes.firstOrNull()?.scrollDelta?.y ?: return@onPointerEvent
                    zoom = (zoom - scarto * 0.1f).coerceIn(0.2f, 3f)
                }
                // §15.4 (Michele): tasto centrale -> stesso "⟳ Riordina"
                // del pulsante in barra, una scorciatoia in più.
                .onPointerEvent(PointerEventType.Press) {
                    if (it.button == PointerButton.Tertiary) riordina()
                }
                // 31/07/2026: `matcher` esplicito al solo tasto sinistro
                // (stessa correzione già fatta per `detectTapGestures`,
                // vedi §19.1) — necessario ORA che il tasto destro sullo
                // sfondo ha un gesto proprio (rettangolo di selezione,
                // sotto): senza il filtro, un trascinamento col destro
                // farebbe pan E rettangolo insieme, i due gesti in
                // competizione sugli stessi eventi.
                .pointerInput(Unit) {
                    detectDragGestures(matcher = PointerMatcher.Primary) { delta ->
                        if (nodoTrascinato == null) {
                            panX += delta.x
                            panY += delta.y
                        }
                    }
                }
                // §19.10 (Michele: "serve la multi selezione tramite
                // rettangolo quando ci sono tante foglie, è più comodo"):
                // trascinamento col tasto DESTRO sullo sfondo. Non parte
                // se il trascinamento comincia SOPRA un nodo
                // (`nodoSottoMouse != null`): in quel caso il tasto destro
                // apre già il menu contestuale del nodo (vedi
                // `onPointerEvent(Press)` più sotto, che non consuma
                // l'evento) — senza questa guardia i due gestori
                // reagirebbero entrambi allo stesso trascinamento.
                // Ctrl tenuto premuto AGGIUNGE alla selezione esistente
                // (stessa convenzione del click singolo), altrimenti la
                // sostituisce — calcolato una volta sola all'inizio,
                // non ricalcolato mentre si trascina.
                .pointerInput(Unit) {
                    var baseSelezione = emptySet<String>()
                    detectDragGestures(
                        matcher = PointerMatcher.mouse(PointerButton.Secondary),
                        onDragStart = { inizio ->
                            if (nodoSottoMouse == null) {
                                baseSelezione = if (ctrlPremuto) sceneSelezionate else emptySet()
                                rettangoloSelezione = inizio to inizio
                            }
                        },
                        onDragEnd = { rettangoloSelezione = null },
                        onDragCancel = { rettangoloSelezione = null },
                        onDrag = { delta ->
                            val (inizio, attuale) = rettangoloSelezione ?: return@detectDragGestures
                            val nuovoFine = attuale + delta
                            rettangoloSelezione = inizio to nuovoFine
                            sceneSelezionate = baseSelezione + nodiNelRettangoloAttuale(inizio, nuovoFine)
                        },
                    )
                }
                // Click sullo sfondo (fuori da qualunque nodo) toglie la
                // selezione con contorno blu — coerente con l'aspettativa
                // che "clicco altrove" deselezioni. Richiede anche il
                // focus da tastiera (§17.1/§17.3): dopo aver cliccato la
                // mappa, Canc/Esc agiscono subito.
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusMappa.requestFocus()
                        sceneSelezionate = emptySet()
                    })
                },
        ) {
            LaunchedEffect(Unit) { focusMappa.requestFocus() }
            Box(
                modifier = Modifier.graphicsLayer(
                    scaleX = zoom,
                    scaleY = zoom,
                    translationX = panX,
                    translationY = panY,
                ),
            ) {
                // Archi sotto i nodi (30/07/2026: dimensione del canvas
                // fissa, sufficiente per i libri di prova attuali — da
                // rivedere quando si passa a libri da 350+ scene).
                Canvas(modifier = Modifier.size(4000.dp)) {
                    graph.edges.forEach { edge ->
                        val from = posizioneEffettiva(edge.fromSceneId) ?: return@forEach
                        val to = posizioneEffettiva(edge.toSceneId) ?: return@forEach
                        if (GeometriaArchi.eCappio(edge.fromSceneId, edge.toSceneId)) return@forEach
                        // Arco estraneo al vicinato del nodo sotto il
                        // mouse -> attenuato, stessa idea dei nodi sotto.
                        val estraneo = nodoSottoMouse != null &&
                            edge.fromSceneId != nodoSottoMouse && edge.toSceneId != nodoSottoMouse
                        // Arco sul percorso da START al nodo sotto il
                        // mouse -> viola e più spesso, sopra il
                        // verde/rosso di risoluzione (§6.2).
                        val suPercorso = (edge.fromSceneId to edge.toSceneId) in archiPercorso
                        val colore = if (suPercorso) {
                            Color(0xFF8E24AA)
                        } else {
                            (if (edge.resolved) Color(0xFF4CAF50) else Color(0xFFE53935))
                                .copy(alpha = if (estraneo) 0.15f else 1f)
                        }
                        val spessore = if (suPercorso) 4f else 2f

                        val centroDa = Offset(from.x + larghezzaNodo.value / 2, from.y + altezzaNodo.value / 2)
                        val centroA = Offset(to.x + larghezzaNodo.value / 2, to.y + altezzaNodo.value / 2)
                        // Coppia collegata nei due versi: le due linee si
                        // sovrapporrebbero esattamente, e si vedrebbe un
                        // tratto solo. Si scostano di lato, ognuna con la
                        // sua punta (06/08/2026).
                        val scarto = if ((edge.fromSceneId to edge.toSceneId) in coppieDoppie) {
                            GeometriaArchi.scostamento(centroDa, centroA)
                        } else {
                            Offset.Zero
                        }
                        // La punta si ferma sul BORDO dell'ellisse: i nodi
                        // sono disegnati sopra questo canvas, al centro
                        // sarebbe invisibile.
                        val inizio = GeometriaArchi.bordoEllisse(
                            centroDa, centroA, larghezzaNodo.value / 2, altezzaNodo.value / 2,
                        ) + scarto
                        val fine = GeometriaArchi.bordoEllisse(
                            centroA, centroDa, larghezzaNodo.value / 2, altezzaNodo.value / 2,
                        ) + scarto

                        drawLine(color = colore, start = inizio, end = fine, strokeWidth = spessore)
                        // Niente punta sui collegamenti cortissimi: la
                        // coprirebbe invece di indicarla.
                        if (GeometriaArchi.abbastanzaLungo(inizio, fine)) {
                            val (sinistra, destra) = GeometriaArchi.alettePunta(inizio, fine)
                            drawLine(color = colore, start = fine, end = sinistra, strokeWidth = spessore)
                            drawLine(color = colore, start = fine, end = destra, strokeWidth = spessore)
                        }
                    }
                }

                graph.nodes.forEach { nodo ->
                    val pos = posizioneEffettiva(nodo.sceneId) ?: return@forEach
                    // Ricerca (§6.1): arancione su TUTTE le corrispondenze,
                    // blu più spesso solo su quella attiva (centrata in
                    // vista in questo momento) — così si vede a colpo
                    // d'occhio quante sono e quale delle tante è "questa".
                    val ricercaAttiva = testoRicerca == ultimaRicerca
                    val isCorrispondenza = ricercaAttiva && corrispondenze.any { it.id == nodo.sceneId }
                    val isAttiva = ricercaAttiva && corrispondenze.getOrNull(indiceCorrente)?.id == nodo.sceneId
                    // Selezione con click singolo (30/07/2026, Michele:
                    // "quando clicco una scena devi contornarla di un
                    // blu"; §17.3: multi-selezione con Ctrl+click) —
                    // stesso blu della corrispondenza di ricerca attiva,
                    // priorità più bassa: se le due coincidono non cambia
                    // nulla a vista.
                    val isSelezionata = nodo.sceneId in sceneSelezionate
                    // Vicinato (§6.1): fuori dal mouse-over, tutti a piena
                    // opacità; con un nodo sotto mouse, solo lui e i suoi
                    // collegati diretti restano leggibili.
                    // 31/07/2026 (Michele, bug di test: passando il mouse
                    // sui nodi selezionati durante una multi-selezione
                    // "non devono diventare grigi"): un nodo selezionato
                    // resta sempre a piena opacità, non importa dove sia
                    // il mouse — la selezione ha priorità sull'attenuazione
                    // del vicinato, altrimenti selezionare/ispezionare più
                    // nodi in sequenza li fa sembrare deselezionati mentre
                    // il mouse passa altrove.
                    val opacitaNodo = if (nodoSottoMouse == null || nodo.sceneId in vicinato || isSelezionata) 1f else 0.25f
                    // Immagine di copertina del nodo (§15.3, Michele: "le
                    // immagini nel caso ci siano devono essere
                    // renderizzate come sfondo del grafo... se ci sono
                    // NPC o NEMICO o BESTIA o LOCATION usando questo
                    // ordine di preferenza"): NPC prima, poi il nemico del
                    // combattimento (copre anche "bestia", stesso campo
                    // `combat.enemyImage` per entrambi), infine lo sfondo
                    // di location come ultima scelta. Una sola immagine
                    // per nodo, mai tutte e tre insieme (a differenza
                    // della scheda di scena, §15.3, dove convivono).
                    val scenaNodo = scenesById[nodo.sceneId]
                    val immagineNodo = scenaNodo?.npcImage
                        ?: scenaNodo?.combat?.enemyImage
                        ?: scenaNodo?.backgroundImage
                    // §19.6 (Michele: "evidenziare le scene orfane"): non
                    // raggiungibile da START — oggi si notava solo dalla
                    // posizione nell'ultimo livello della griglia, nessun
                    // segno dedicato.
                    val isOrfana = nodo.level == Int.MAX_VALUE
                    // §19.7 (Michele: "segnalare i vicoli ciechi"): stessa
                    // regola del nuovo avviso in GraphValidator
                    // (core:data) — TRANSITION senza scelte, scelte-
                    // disciplina né combattimento, il giocatore ci resta
                    // bloccato salvo un salto d'ufficio non modellato dal
                    // grafo visivo (stesso limite già esistente, non
                    // nuovo).
                    val isVicoloCieco = scenaNodo?.sceneType == SceneType.TRANSITION &&
                        scenaNodo.choices.isEmpty() &&
                        scenaNodo.disciplineChoices.isEmpty() &&
                        scenaNodo.combat == null
                    // Colore per tipo di scena (30/07/2026, Michele: "colora
                    // di giallino chiaro gli end e di rosa chiaro gli start
                    // così saltano subito all'occhio") — priorità sulla
                    // salute quando non c'è un'immagine (START/ENDING
                    // restano riconoscibili a colpo d'occhio anche se
                    // capita raramente che uno dei due sia rosso); quando
                    // c'è un'immagine di copertina che copre il
                    // riempimento, lo stesso colore torna come bordo di
                    // base invece che nero, unico posto dove può ancora
                    // comparire.
                    val coloreTipo = when (scenaNodo?.sceneType) {
                        SceneType.START -> Color(0xFFFCE4EC)
                        SceneType.ENDING -> Color(0xFFFFF9C4)
                        else -> null
                    }
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                            .size(larghezzaNodo, altezzaNodo)
                            .alpha(opacitaNodo)
                            // Ovale come i grafi di Graphviz (vedi
                            // FormaOvale in LayoutGerarchico.kt).
                            .clip(FormaOvale)
                            // Resta come fallback anche quando c'è
                            // un'immagine di copertina (§15.3): se
                            // l'immagine sta ancora caricando o fallisce
                            // (mostraSegnaposto=false, vedi
                            // AnteprimaImmagineRisorsa più sotto), questo
                            // colore continua a essere quello visibile,
                            // esattamente come prima di questo colore per
                            // tipo di scena.
                            .background(coloreTipo ?: (if (nodo.healthy) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)))
                            // Bordo nero di default (30/07/2026, Michele: "rendi i
                            // bordi... visibili" — la personalizzazione grafica vera
                            // e propria è un pezzo a parte, per adesso solo leggibile).
                            .border(
                                width = when {
                                    isAttiva || isSelezionata -> 3.dp
                                    isCorrispondenza -> 2.dp
                                    else -> 1.dp
                                },
                                color = when {
                                    isAttiva || isSelezionata -> Color(0xFF1E88E5)
                                    isCorrispondenza -> Color(0xFFFF9800)
                                    else -> coloreTipo ?: Color.Black
                                },
                                FormaOvale,
                            )
                            .onPointerEvent(PointerEventType.Enter) { nodoSottoMouse = nodo.sceneId }
                            .onPointerEvent(PointerEventType.Exit) {
                                if (nodoSottoMouse == nodo.sceneId) nodoSottoMouse = null
                            }
                            // §17.1 (Michele: "tasto destro e di lì menu
                            // per cancellare, duplicare"): il menu agisce
                            // SEMPRE sul solo nodo cliccato col tasto
                            // destro (30/07/2026, corretto dopo aver
                            // scoperto `PointerMatcher.Primary` più sotto,
                            // vedi commento su `detectTapGestures`): un
                            // tasto destro su un nodo GIÀ nella
                            // selezione multipla la preserva intera (il
                            // menu può quindi offrire azioni di gruppo,
                            // §19.1/§19.2); su un nodo FUORI dalla
                            // selezione la sostituisce con quel solo nodo
                            // — stessa convenzione di Explorer/Finder.
                            .onPointerEvent(PointerEventType.Press) {
                                if (it.button == PointerButton.Secondary) {
                                    if (nodo.sceneId !in sceneSelezionate) sceneSelezionate = setOf(nodo.sceneId)
                                    menuContestualePer = nodo.sceneId
                                }
                            }
                            // Interazione nodo (30/07/2026, Michele: "questo
                            // si dovrebbe aprire con il double click e
                            // permettere il movimento con un solo click") —
                            // ribaltato rispetto al tentativo precedente
                            // (click singolo per aprire, soglia per capire
                            // se era un trascinamento): con i ruoli
                            // separati non serve più distinguere click da
                            // trascinamento nello stesso rilevatore, due
                            // `pointerInput` indipendenti bastano e sono
                            // il pattern standard di Compose per "doppio
                            // click qui, trascinamento qui" sullo stesso
                            // elemento — detectTapGestures ignora da solo
                            // un gesto che diventa trascinamento (vede lo
                            // spostamento consumato dall'altro rilevatore
                            // e si ritira), quindi non c'è conflitto tra i
                            // due.
                            // 30/07/2026, Michele: "c'è un certo lag nella
                            // selezione, il blu non è immediato" — con
                            // `onTap`, Compose ASPETTA il tempo limite del
                            // doppio click prima di confermare che era un
                            // singolo tap (altrimenti non potrebbe
                            // distinguerli), quindi il ritardo era
                            // strutturale. `onPress` invece scatta
                            // all'istante, alla pressione, senza aspettare
                            // di sapere se diventerà un tap, un doppio tap
                            // o un trascinamento — stesso motivo per cui
                            // ora seleziona anche a inizio trascinamento,
                            // coerente con "sto spostando la scena 4, non
                            // dovrebbe restare selezionata la 5".
                            .pointerInput(nodo.sceneId) {
                                detectTapGestures(
                                    // 30/07/2026: `matcher` di default è
                                    // button-agnostic (scatta anche col
                                    // tasto destro) — questa versione
                                    // desktop-specifica di
                                    // `detectTapGestures` (skiko,
                                    // `@ExperimentalFoundationApi`)
                                    // accetta invece un `PointerMatcher`
                                    // esplicito: `Primary` lo limita al
                                    // solo tasto sinistro, lasciando il
                                    // destro all'`onPointerEvent(Press)`
                                    // sopra senza che i due si
                                    // sovrappongano più.
                                    matcher = PointerMatcher.Primary,
                                    // §17.3 (Michele: "una multi selezione
                                    // tenendo premuto ctrl"): Ctrl aggiunge/
                                    // toglie questo nodo dall'insieme
                                    // selezionato invece di sostituirlo.
                                    // 31/07/2026 (Michele, bug di test:
                                    // "sposta solo il primo nodo" trascinando
                                    // un nodo già selezionato): `onPress`
                                    // scatta PRIMA che il gestore di
                                    // trascinamento sotto (§19.3) legga
                                    // `sceneSelezionate` per capire se
                                    // muovere un gruppo — senza questa
                                    // guardia, premere un nodo già dentro
                                    // una selezione di 2+ la collassava
                                    // (o, con Ctrl, lo toglieva dal gruppo)
                                    // un istante prima che il trascinamento
                                    // potesse vederla intera. Stessa
                                    // convenzione già in uso per il tasto
                                    // destro (§17.1): premere un nodo GIÀ
                                    // nella selezione multipla la preserva
                                    // sempre, non importa se poi diventa un
                                    // trascinamento o resta un semplice
                                    // click.
                                    onPress = {
                                        focusMappa.requestFocus()
                                        val giaNelGruppo = nodo.sceneId in sceneSelezionate && sceneSelezionate.size >= 2
                                        sceneSelezionate = when {
                                            giaNelGruppo -> sceneSelezionate
                                            ctrlPremuto -> if (nodo.sceneId in sceneSelezionate) {
                                                sceneSelezionate - nodo.sceneId
                                            } else {
                                                sceneSelezionate + nodo.sceneId
                                            }
                                            else -> setOf(nodo.sceneId)
                                        }
                                    },
                                    onDoubleTap = { onSceneSelected(nodo.sceneId) },
                                )
                            }
                            .pointerInput(nodo.sceneId) {
                                // Ancore locali alla coroutine del gesto
                                // (non allo stato Compose): accumulano qui lo
                                // scarto e scrivono SOLO su posizioniManuali,
                                // non le rileggono mai — evita qualunque
                                // deriva dovuta ai tempi della
                                // ricomposizione tra una lettura e la
                                // scrittura successiva.
                                // §19.3 (Michele, azioni di gruppo):
                                // trascinare un nodo che fa parte della
                                // selezione (2+) sposta l'intero gruppo,
                                // mantenendo le posizioni relative — un'
                                // ancora per nodo del gruppo, tutte spostate
                                // dello stesso scarto a ogni frame.
                                // Trascinare un nodo non selezionato continua
                                // a muovere solo quel nodo, come prima.
                                // 31/07/2026: `matcher` esplicito al solo
                                // tasto sinistro (§19.10) — il destro sui
                                // nodi resta riservato al menu contestuale
                                // (`onPointerEvent(Press)` sopra) e al
                                // rettangolo di selezione quando parte
                                // dallo sfondo; senza il filtro un
                                // trascinamento col destro iniziato su un
                                // nodo avrebbe anche spostato il nodo
                                // mentre il menu era aperto.
                                var ancore = emptyMap<String, Offset>()
                                detectDragGestures(
                                    matcher = PointerMatcher.Primary,
                                    onDragStart = {
                                        // §19.13: un solo scatto di cronologia
                                        // per l'INTERO trascinamento, registrato
                                        // qui prima che `onDrag` inizi a
                                        // scrivere su `posizioniManuali` — mai
                                        // dentro `onDrag` stesso (un fotogramma
                                        // per scatto sarebbe inutilizzabile).
                                        mapViewState.cronologia.registraCheckpoint(Documento(manifest, posizioniManuali))
                                        nodoTrascinato = nodo.sceneId
                                        val gruppo = if (nodo.sceneId in sceneSelezionate && sceneSelezionate.size >= 2) {
                                            sceneSelezionate
                                        } else {
                                            setOf(nodo.sceneId)
                                        }
                                        ancore = gruppo.associateWith { posizioneEffettiva(it) ?: Offset.Zero }
                                    },
                                    onDragEnd = { nodoTrascinato = null },
                                    onDragCancel = { nodoTrascinato = null },
                                    onDrag = { delta ->
                                        ancore = ancore.mapValues { (_, pos) -> Offset(pos.x + delta.x, pos.y + delta.y) }
                                        posizioniManuali = posizioniManuali + ancore
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        // Immagine di copertina (§15.3): riempie il nodo
                        // SOPRA lo sfondo verde/rosso già impostato più in
                        // alto — se il caricamento fallisce o è ancora in
                        // corso, il colore di salute sotto resta visibile
                        // come prima (nessuna regressione, solo un livello
                        // in più quando c'è un'immagine da mostrare).
                        if (immagineNodo != null) {
                            AnteprimaImmagineRisorsa(immagineNodo, Modifier.fillMaxSize(), mostraSegnaposto = false)
                            // Pallino di salute (30/07/2026): con
                            // un'immagine di sfondo il riempimento
                            // verde/rosso non si vede più, quindi la
                            // salute del nodo (§6.2) resta leggibile in un
                            // segno a parte invece di sparire.
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (nodo.healthy) Color(0xFF4CAF50) else Color(0xFFE53935))
                                    .border(1.dp, Color.White, RoundedCornerShape(50)),
                            )
                        }
                        // §19.6/§19.7: badge indipendenti dall'immagine di
                        // copertina (a differenza del pallino di salute
                        // sopra, che serve solo quando l'immagine copre il
                        // colore di sfondo) — sempre in basso, per non
                        // sovrapporsi al pallino di salute in alto.
                        if (isOrfana) {
                            Text(
                                "👻",
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.BottomStart).padding(2.dp),
                            )
                        }
                        if (isVicoloCieco) {
                            Text(
                                "⛔",
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp),
                            )
                        }
                        // Colore fisso, non legato al tema: gli sfondi
                        // verde/rosso restano chiari per il significato di
                        // salute (§6.2) indipendentemente dal tema attivo.
                        // Sopra un'immagine invece il testo passa a
                        // bianco con uno scrim scuro dietro, altrimenti
                        // resterebbe illeggibile su una foto qualunque.
                        // In vista compatta solo il numero: il codice non
                        // ci starebbe e a quella scala non si leggerebbe
                        // comunque — è la forma del grafo che si guarda,
                        // non il contenuto delle scene.
                        val etichetta = when {
                            compatto -> nodo.sceneId
                            scenaNodo != null -> "${nodo.sceneId} · ${codiceScena(scenaNodo)}"
                            else -> nodo.sceneId
                        }
                        // Riga 1: chiave (id+codice). Riga 2: estratto del
                        // testo narrato — riconoscere la scena a colpo
                        // d'occhio senza doverla aprire (30/07/2026,
                        // Michele).
                        Column(
                            modifier = Modifier
                                .let { if (immagineNodo != null) it.background(Color.Black.copy(alpha = 0.55f)) else it }
                                .padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                etichetta,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = if (immagineNodo != null) Color.White else Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                // La seconda riga sparisce in vista
                                // compatta: in 34 punti d'altezza non ci
                                // sta, e sarebbe illeggibile.
                                if (compatto) "" else scenaNodo?.narrativeText?.replace("\n", " ")?.trim().orEmpty(),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                color = if (immagineNodo != null) Color(0xFFE0E0E0) else Color.DarkGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        // §17.1/§17.2/§19.1 (Michele: "tasto destro e di
                        // lì menu per cancellare, duplicare"; poi "se
                        // seleziono due scene... mi dai 2 opzioni... allo
                        // stesso livello di duplica cancella"): ora che
                        // il tasto destro preserva la multi-selezione
                        // esistente invece di collassarla (vedi
                        // `PointerMatcher.Primary` sopra), il menu può
                        // davvero distinguere selezione singola/doppia/
                        // multipla invece di agire sempre sul solo nodo
                        // cliccato.
                        DropdownMenu(
                            expanded = menuContestualePer == nodo.sceneId,
                            onDismissRequest = { menuContestualePer = null },
                        ) {
                            // 30/07/2026, Michele: "diamo per scontato
                            // che si può duplicare tutto tranne le scene
                            // start" — un libro potrebbe avere più START
                            // legittime in futuro (a seconda del
                            // personaggio scelto), ma è una feature a
                            // parte non ancora progettata; per ora
                            // duplicare uno START creerebbe una seconda
                            // scena START ambigua, mai raggiungibile
                            // giocando (il motore prende sempre la prima
                            // che trova) e non segnalata dal validatore.
                            // §17.2: "Duplica" resta un'azione a singola
                            // scena — con una multi-selezione attiva non
                            // compare, sostituita da "Duplica gruppo"
                            // (§19.2, sotto).
                            if (sceneSelezionate.size <= 1 && scenaNodo?.sceneType != SceneType.START) {
                                DropdownMenuItem(
                                    text = { Text("Duplica") },
                                    onClick = {
                                        menuContestualePer = null
                                        onDuplicaScena(nodo.sceneId)
                                    },
                                )
                            }
                            // Riassunto del cammino (03/08/2026, Michele:
                            // "selezionando una scena lui fa il riassunto
                            // di tutte le scene partendo da start a quella
                            // selezionata"). Su UNA scena: è il percorso
                            // fino a lei, non un gruppo.
                            if (riassuntore != null && sceneSelezionate.size <= 1) {
                                DropdownMenuItem(
                                    text = { Text("📖 Riassumi il percorso fino a qui") },
                                    onClick = {
                                        menuContestualePer = null
                                        avviaRiassunto(nodo.sceneId)
                                    },
                                )
                            }
                            // §19.2: speculare a "Duplica" — compare SOLO
                            // con 2+ selezionate (la singola resta
                            // "Duplica" sopra), stessa esclusione delle
                            // scene START (stesso motivo: seconda START
                            // ambigua e non segnalata).
                            if (sceneSelezionate.size >= 2 && sceneSelezionate.none { scenesById[it]?.sceneType == SceneType.START }) {
                                DropdownMenuItem(
                                    text = { Text("Duplica gruppo (${sceneSelezionate.size})") },
                                    onClick = {
                                        menuContestualePer = null
                                        duplicaGruppoESeleziona(sceneSelezionate)
                                    },
                                )
                            }
                            // §19.11 (Michele, bug di test: "tenendo
                            // premuto ctrl faccio click con il dx non
                            // appare l'opzione di arrangio automatico" —
                            // esisteva solo in barra, non nel menu
                            // contestuale): stesso criterio di "Duplica
                            // gruppo" sopra, 2+ selezionate, nessun
                            // vincolo sulle scene START (l'allineamento
                            // non tocca collegamenti né duplica nulla).
                            if (sceneSelezionate.size >= 2) {
                                DropdownMenuItem(
                                    text = { Text("↔ Allinea in riga") },
                                    onClick = {
                                        menuContestualePer = null
                                        allineaSelezione(inRiga = true)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("↕ Allinea in colonna") },
                                    onClick = {
                                        menuContestualePer = null
                                        allineaSelezione(inRiga = false)
                                    },
                                )
                            }
                            // §19.1: solo con ESATTAMENTE due scene
                            // selezionate — un'opzione per direzione,
                            // "Lega" se il collegamento non esiste
                            // ancora, "Rimuovi legame" se esiste già.
                            // Ordine stabile (per ID numerico) così le
                            // etichette non "ballano" da un'apertura del
                            // menu all'altra.
                            if (sceneSelezionate.size == 2) {
                                val (idA, idB) = sceneSelezionate.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
                                val aggiungiVoceLegame: @Composable (String, String) -> Unit = { da, a ->
                                    val giaLegate = scenesById[da]?.choices?.any { it.nextSceneId == a } == true
                                    DropdownMenuItem(
                                        text = { Text(if (giaLegate) "Rimuovi legame $da→$a" else "Lega scena $da→$a") },
                                        onClick = {
                                            menuContestualePer = null
                                            if (giaLegate) rimuoviLegame(da, a) else legaScena(da, a)
                                        },
                                    )
                                }
                                aggiungiVoceLegame(idA, idB)
                                aggiungiVoceLegame(idB, idA)
                            }
                            DropdownMenuItem(
                                text = { Text(if (sceneSelezionate.size > 1) "Elimina (${sceneSelezionate.size})" else "Elimina") },
                                onClick = {
                                    menuContestualePer = null
                                    sceneIdsDaEliminare = sceneSelezionate.ifEmpty { setOf(nodo.sceneId) }
                                },
                            )
                        }
                    }
                }
            }

            // §19.10: overlay del rettangolo di selezione, fuori dal
            // `graphicsLayer` di pan/zoom apposta (stesso motivo delle
            // coordinate del mouse sotto) — le coordinate sono già in
            // spazio schermo, disegnarlo dentro il graphicsLayer lo
            // scalerebbe/sposterebbe insieme alla mappa invece di
            // restare fisso rispetto al gesto del mouse.
            rettangoloSelezione?.let { (inizio, fine) ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val angolo = Offset(minOf(inizio.x, fine.x), minOf(inizio.y, fine.y))
                    val dimensione = Size(kotlin.math.abs(fine.x - inizio.x), kotlin.math.abs(fine.y - inizio.y))
                    drawRect(color = Color(0xFF1E88E5).copy(alpha = 0.12f), topLeft = angolo, size = dimensione)
                    drawRect(color = Color(0xFF1E88E5), topLeft = angolo, size = dimensione, style = Stroke(width = 1.5f))
                }
            }

            // Coordinate del mouse (30/07/2026, Michele): fuori dal
            // graphicsLayer di pan/zoom apposta, così restano leggibili
            // in un angolo fisso indipendentemente da dove sei sulla
            // mappa — la stessa posizione che si vede in uno screenshot.
            // 30/07/2026, secondo giro: la riga della scena mostrava solo
            // il nodo IN QUEL MOMENTO trascinato (`nodoTrascinato`), che
            // torna null appena rilasci il tasto — quindi spariva proprio
            // nell'istante in cui Michele faceva lo screenshot per
            // controllare il risultato. Passato a `nodoSottoMouse` (il
            // hover) per quello.
            // 30/07/2026, terzo giro: durante un trascinamento vero
            // Michele segnala che la riga della scena non compare affatto
            // — `nodoSottoMouse` dipende da eventi Enter/Exit basati sulla
            // posizione ATTUALE del nodo, e mentre il nodo si sposta sotto
            // il cursore durante il drag questi eventi non sono affidabili
            // (il nodo è un frame indietro rispetto al cursore). Priorità
            // a `nodoTrascinato` quando presente (deterministico, deciso
            // da noi in onDragStart/onDragEnd, non dall'hit-test) — resta
            // affidabile DURANTE il trascinamento; ricade su
            // `nodoSottoMouse` solo dopo il rilascio, quando torna a
            // essere l'hover a decidere.
            val nodoDaMostrare = nodoTrascinato ?: nodoSottoMouse
            if (posizioneMouse != null || nodoDaMostrare != null) {
                val etichettaScena = nodoDaMostrare?.let { id ->
                    val pos = posizioneEffettiva(id)
                    if (pos != null) " · scena $id -> (${pos.x.roundToInt()}, ${pos.y.roundToInt()})" else " · scena $id"
                }.orEmpty()
                Text(
                    "🖱 ${posizioneMouse?.let { "(${it.x.roundToInt()}, ${it.y.roundToInt()})" } ?: "(fuori dal riquadro)"}" +
                        etichettaScena,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

// Rimuove un valore immagine (qualunque prefisso, static: o url:) da
// TUTTE le scene che lo referenziano — usato dal ✕ di
// SezioneImmaginiInUso (30/07/2026, Michele: "dal menu delle risorse
// devi darmi la possibilità di cancellarle").
private fun sceneSenzaImmagine(scenes: List<Scene>, valore: String): List<Scene> = scenes.map { scena ->
    scena.copy(
        backgroundImage = scena.backgroundImage.takeUnless { it == valore },
        npcImage = scena.npcImage.takeUnless { it == valore },
        combat = scena.combat?.let { c -> if (c.enemyImage == valore) c.copy(enemyImage = null) else c },
    )
}

// Come sopra ma sostituisce il valore invece di azzerarlo — usato dalla
// modifica (solo per `url:`, un `static:` è un ID del catalogo fisso,
// non testo libero da riscrivere).
private fun sceneConImmagineSostituita(scenes: List<Scene>, vecchio: String, nuovo: String): List<Scene> =
    scenes.map { scena ->
        scena.copy(
            backgroundImage = if (scena.backgroundImage == vecchio) nuovo else scena.backgroundImage,
            npcImage = if (scena.npcImage == vecchio) nuovo else scena.npcImage,
            combat = scena.combat?.let { c -> if (c.enemyImage == vecchio) c.copy(enemyImage = nuovo) else c },
        )
    }

// Immagini GIÀ in uso nelle scene del libro (30/07/2026, Michele:
// "vorrei poter anche vedere la lista delle risorse già presenti") —
// scandite dal vivo da `manifest.scenes`, non dal registro
// `customResources` (che potrebbe non conoscerle ancora). Ogni voce
// mostra un'anteprima piccola; se corrisponde già a una risorsa
// personalizzata registrata lo dice, altrimenti (solo per gli `url:`,
// i `static:` non hanno bisogno di un ID a parte: sono già il
// catalogo) un pulsante rapido la registra con un ID proposto
// dall'ultimo pezzo del link — stessa idea del comando CLI 'bonifica'
// (ConvertMain.kt), qui a portata di click invece che da terminale.
//
// Cancella/modifica (30/07/2026, Michele: "dal menu delle risorse devi
// darmi la possibilità di cancellarle oppure di selezionarle e
// modificarle"): un'immagine "in uso" è identificata dal suo VALORE
// (può comparire in più scene contemporaneamente, da qui il
// `.distinct()`), quindi cancellare/modificare agisce su tutte le
// scene che la referenziano in un colpo solo, non su una singola —
// altrimenti l'elenco (deduplicato) e l'azione (per-scena) si
// contraddirebbero. Se il valore era anche registrato in
// customResources, il registro viene aggiornato di pari passo (stesso
// ID, URL nuovo o voce rimossa).
@Composable
private fun SezioneImmaginiInUso(manifest: Manifest, onManifestCambiato: (Manifest) -> Unit) {
    val inUso = remember(manifest) {
        manifest.scenes.flatMap { listOfNotNull(it.backgroundImage, it.npcImage, it.combat?.enemyImage) }.distinct()
    }
    var valoreInModifica by remember { mutableStateOf<String?>(null) }
    var testoInModifica by remember { mutableStateOf("") }

    fun registra(voce: CustomResourceEntry) {
        onManifestCambiato(
            manifest.copy(customResources = manifest.customResources.copy(images = manifest.customResources.images + voce)),
        )
    }

    fun elimina(valore: String) {
        val urlSenzaPrefisso = valore.removePrefix(ImageReference.URL_PREFIX)
        onManifestCambiato(
            manifest.copy(
                scenes = sceneSenzaImmagine(manifest.scenes, valore),
                customResources = manifest.customResources.copy(
                    images = manifest.customResources.images.filterNot { it.url == urlSenzaPrefisso },
                ),
            ),
        )
    }

    fun salvaModifica(vecchio: String, nuovoUrl: String) {
        if (nuovoUrl.isBlank()) return
        val nuovoValore = "${ImageReference.URL_PREFIX}$nuovoUrl"
        val vecchioUrlSenzaPrefisso = vecchio.removePrefix(ImageReference.URL_PREFIX)
        onManifestCambiato(
            manifest.copy(
                scenes = sceneConImmagineSostituita(manifest.scenes, vecchio, nuovoValore),
                customResources = manifest.customResources.copy(
                    images = manifest.customResources.images.map {
                        if (it.url == vecchioUrlSenzaPrefisso) it.copy(url = nuovoUrl) else it
                    },
                ),
            ),
        )
        valoreInModifica = null
    }

    Text("Immagini in uso nelle scene (${inUso.size})", style = MaterialTheme.typography.titleMedium)
    if (inUso.isEmpty()) {
        Text("Nessuna immagine ancora usata in questo libro.", style = MaterialTheme.typography.bodySmall)
    }
    inUso.forEach { valore ->
        val registrata = manifest.customResources.images.firstOrNull { "${ImageReference.URL_PREFIX}${it.url}" == valore }
        if (valoreInModifica == valore) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            ) {
                OutlinedTextField(
                    value = testoInModifica,
                    onValueChange = { testoInModifica = it },
                    label = { Text("https://...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                TextButton(onClick = { salvaModifica(valore, testoInModifica) }) { Text("💾 Salva") }
                TextButton(onClick = { valoreInModifica = null }) { Text("✕ Annulla") }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            ) {
                AnteprimaImmagineRisorsa(valore, Modifier.size(48.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (registrata != null) "$valore  (${registrata.id})" else valore,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (registrata == null && valore.startsWith(ImageReference.URL_PREFIX)) {
                    TextButton(onClick = {
                        val url = valore.removePrefix(ImageReference.URL_PREFIX)
                        val idProposto = url.substringAfterLast('/').substringBeforeLast('.').ifBlank { "risorsa" }
                        registra(CustomResourceEntry(idProposto, url))
                    }) { Text("+ Registra") }
                }
                if (valore.startsWith(ImageReference.URL_PREFIX)) {
                    TextButton(onClick = {
                        valoreInModifica = valore
                        testoInModifica = valore.removePrefix(ImageReference.URL_PREFIX)
                    }) { Text("✎") }
                }
                TextButton(onClick = { elimina(valore) }) { Text("🗑") }
            }
        }
    }
}

// Una sezione del pannello "Risorse personalizzate" (§15.7): elenco con
// pulsante di rimozione per voce + riga per aggiungerne una nuova.
// Uguale per immagini e suoni, cambia solo l'elenco passato.
//
// Modifica in-place (30/07/2026, Michele: "dal menu delle risorse devi
// darmi la possibilità di cancellarle oppure di selezionarle e
// modificarle"): "✎" carica la voce nel modulo sotto (stesso modulo
// usato per aggiungere), lo trasforma temporaneamente in un modulo di
// modifica ("💾 Salva"/"✕ Annulla") identificato dall'ID originale —
// evita di dover cancellare e riaggiungere una voce solo per
// correggerne l'URL.
@Composable
private fun SezioneRisorsePersonalizzate(
    titolo: String,
    voci: List<CustomResourceEntry>,
    onCambia: (List<CustomResourceEntry>) -> Unit,
    // §18.4: solo per i suoni — quando non vuoto, il campo valore
    // accetta anche "static:<id>" (riferimento a un suono già bundlato)
    // oltre a un url: scritto a mano, con suggerimenti cliccabili sotto
    // il campo. Vuoto (default, caso immagini) = comportamento invariato
    // di sempre, solo un url nudo.
    suggerimentiRisorseStatiche: List<String> = emptyList(),
) {
    var nuovoId by remember { mutableStateOf("") }
    var nuovoUrl by remember { mutableStateOf("") }
    var idInModifica by remember { mutableStateOf<String?>(null) }
    var mostraSuggerimentiStatici by remember { mutableStateOf(false) }

    fun annullaModifica() {
        idInModifica = null
        nuovoId = ""
        nuovoUrl = ""
        mostraSuggerimentiStatici = false
    }

    Text(titolo, style = MaterialTheme.typography.titleMedium)
    voci.forEach { voce ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${voce.id} → ${voce.url}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = {
                idInModifica = voce.id
                nuovoId = voce.id
                nuovoUrl = voce.url
            }) { Text("✎") }
            TextButton(onClick = { onCambia(voci - voce) }) { Text("✕") }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = nuovoId,
            onValueChange = { nuovoId = it },
            label = { Text("ID") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        Column(modifier = Modifier.weight(2f)) {
            OutlinedTextField(
                value = nuovoUrl,
                onValueChange = {
                    nuovoUrl = it
                    if (suggerimentiRisorseStatiche.isNotEmpty()) mostraSuggerimentiStatici = it.isNotBlank()
                },
                label = { Text(if (suggerimentiRisorseStatiche.isNotEmpty()) "static:<id> oppure url:<link>" else "https://...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            if (mostraSuggerimentiStatici) {
                val termine = nuovoUrl.removePrefix(ImageReference.STATIC_PREFIX)
                val corrispondenze = suggerimentiRisorseStatiche.filter { it.contains(termine, ignoreCase = true) }.take(6)
                if (corrispondenze.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        corrispondenze.forEach { id ->
                            Text(
                                "${ImageReference.STATIC_PREFIX}$id",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        nuovoUrl = "${ImageReference.STATIC_PREFIX}$id"
                                        mostraSuggerimentiStatici = false
                                    }
                                    .padding(8.dp),
                            )
                        }
                    }
                }
            }
        }
        Button(onClick = {
            if (nuovoId.isNotBlank() && nuovoUrl.isNotBlank()) {
                val idOriginale = idInModifica
                if (idOriginale != null) {
                    onCambia(voci.map { if (it.id == idOriginale) CustomResourceEntry(nuovoId, nuovoUrl) else it })
                } else {
                    onCambia(voci + CustomResourceEntry(nuovoId, nuovoUrl))
                }
                annullaModifica()
            }
        }) { Text(if (idInModifica != null) "💾 Salva" else "+ Aggiungi") }
        if (idInModifica != null) {
            TextButton(onClick = { annullaModifica() }) { Text("✕ Annulla") }
        }
    }
}
