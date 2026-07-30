package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import io.github.luposolitario.immundanoctisex.core.data.model.CustomResourceEntry
import io.github.luposolitario.immundanoctisex.core.data.model.ImageReference
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import io.github.luposolitario.immundanoctisex.core.data.validation.PackageValidator
import io.github.luposolitario.immundanoctisex.core.data.validation.ValidationResult
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
    val posizioniManuali = mutableStateOf<Map<String, Offset>>(emptyMap())
    // Scena selezionata con un click singolo (30/07/2026, Michele:
    // "quando clicco una scena devi contornarla di un blu") — diversa
    // dall'hover (nodoSottoMouse, si perde appena sposti il mouse) e dal
    // doppio click (che apre il pannello): un click singolo la marca e
    // resta marcata finché non clicchi altrove.
    val sceneSelezionata = mutableStateOf<String?>(null)
}

@Composable
fun rememberMapViewState(): MapViewState = remember { MapViewState() }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MapScreen(
    file: File,
    manifest: Manifest,
    warnings: List<String>,
    mapViewState: MapViewState,
    onTornaAvvio: () -> Unit,
    onSceneSelected: (String) -> Unit,
    onNuovaScena: () -> Unit,
    onEliminaScena: (String) -> Unit,
    onManifestCambiato: (Manifest) -> Unit,
    salvataggioGiaConfermato: Boolean,
    onSalvataggioConfermato: () -> Unit,
    onFileCambiato: (File) -> Unit,
) {
    val graph = remember(manifest) { buildSceneGraph(manifest) }
    val scenesById = remember(manifest) { manifest.scenes.associateBy { it.id } }
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
    // Conferma di eliminazione (§15.5, Michele: "la possibilità di
    // cancellare... le scene selezionate") — azione distruttiva (anche
    // se recuperabile dai backup, §10), stesso principio della conferma
    // di sovrascrittura sopra: null = nessun dialogo aperto.
    var sceneDaEliminare by remember { mutableStateOf<String?>(null) }
    // Risorse personalizzate (§15.7, Michele: "risorse... fornite da chi
    // crea il libro"): pannello a parte, non un dialogo di conferma —
    // niente di distruttivo qui, si apre e si chiude liberamente.
    var mostraRisorsePersonalizzate by remember { mutableStateOf(false) }

    fun salvaSu(destinazione: File) {
        val json = Json { prettyPrint = true }.encodeToString(Manifest.serializer(), manifest)
        salvaLibro(destinazione, json)
        messaggioSalvataggio = "✓ Salvato (backup in ${destinazione.name}.bak1)"
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
    var sceneSelezionata by mapViewState.sceneSelezionata
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

    // Livello -> riga/colonna; le scene orfane (non raggiungibili da
    // START, vedi SceneGraph.kt) finiscono tutte sull'ultimo livello
    // invece che sparire.
    val orphanLevel = (graph.nodes.filter { it.level != Int.MAX_VALUE }.maxOfOrNull { it.level } ?: 0) + 1
    val byLevel = graph.nodes
        .groupBy { if (it.level == Int.MAX_VALUE) orphanLevel else it.level }
        .mapValues { (_, nodi) -> nodi.sortedBy { it.sceneId.toIntOrNull() ?: Int.MAX_VALUE } }

    val positions = remember(graph, orizzontale) {
        // La spaziatura "tra livelli" resta legata alla dimensione del
        // nodo lungo l'asse su cui i livelli finiscono, non all'asse in
        // sé: ruotando la disposizione di 90°, chi prima spaziava le
        // colonne (H_SPACING, pensato per la larghezza del nodo) ora
        // spazia le righe, e viceversa per V_SPACING.
        val spazioLivelli = if (orizzontale) H_SPACING.value else V_SPACING.value
        val spazioFratelli = if (orizzontale) V_SPACING.value else H_SPACING.value
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
        panX = viewportSize.width / 2f - (pos.x + NODE_WIDTH.value / 2f) * zoom
        panY = viewportSize.height / 2f - (pos.y + NODE_HEIGHT.value / 2f) * zoom
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onTornaAvvio) { Text("← Torna all'avvio") }
                    Button(onClick = { salvaOChiediConferma(file) }) { Text("💾 Salva libro") }
                    Button(onClick = {
                        scegliPercorsoSalvataggio(file.name)?.let(::salvaOChiediConferma)
                    }) { Text("Salva con nome…") }
                    Button(onClick = { risultatoValidazione = PackageValidator.validate(manifest) }) {
                        Text("🔍 Valida libro")
                    }
                    // §15.5 (Michele: "la possibilità di cancellare o
                    // aggiungere le scene selezionate"): "Nuova scena"
                    // sempre attivo, "Elimina" solo con una scena
                    // selezionata (contorno blu, già costruito).
                    Button(onClick = onNuovaScena) { Text("+ Nuova scena") }
                    Button(
                        onClick = { sceneSelezionata?.let { sceneDaEliminare = it } },
                        enabled = sceneSelezionata != null,
                    ) { Text("🗑 Elimina scena") }
                    Button(onClick = { mostraRisorsePersonalizzate = true }) { Text("🔗 Risorse url:") }
                }
                Text(
                    "${manifest.title} — ${manifest.scenes.size} scene, ${warnings.size} avvisi al caricamento",
                    style = MaterialTheme.typography.bodySmall,
                )
                // Legenda colori (30/07/2026, Michele: "non capisco cosa
                // rappresenta... che vuol dire viola e verde?"): sempre
                // visibile invece di lasciarla solo a parole in chat, così
                // resta consultabile ogni volta che serve.
                Text(
                    "🟩 collegamento valido  🟥 collegamento a scena inesistente  " +
                        "🟪 percorso da START alla scena sotto il mouse  " +
                        "grigio = fuori da quel percorso (anche se il collegamento esiste)  " +
                        "🩷 scena START  💛 scena ENDING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                messaggioSalvataggio?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { orizzontale = !orizzontale; posizioniManuali = emptyMap() }) {
                    Text(if (orizzontale) "↕ Verticale" else "↔ Orizzontale")
                }
                Button(onClick = ::riordina) { Text("⟳ Riordina") }
                Button(onClick = { zoom = (zoom - 0.1f).coerceAtLeast(0.2f) }) { Text("−") }
                Text("${(zoom * 100).roundToInt()}%")
                Button(onClick = { zoom = (zoom + 0.1f).coerceAtMost(3f) }) { Text("+") }
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

        sceneDaEliminare?.let { id ->
            AlertDialog(
                onDismissRequest = { sceneDaEliminare = null },
                title = { Text("Eliminare la scena $id?") },
                text = {
                    Text(
                        "I collegamenti di altre scene verso $id resteranno come riferimenti a una " +
                            "scena non più esistente (si vedono rossi sulla mappa, non vengono corretti da soli).",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onEliminaScena(id)
                        if (sceneSelezionata == id) sceneSelezionata = null
                        sceneDaEliminare = null
                    }) { Text("Elimina") }
                },
                dismissButton = {
                    TextButton(onClick = { sceneDaEliminare = null }) { Text("Annulla") }
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
                            onRegistra = { voce ->
                                onManifestCambiato(
                                    manifest.copy(
                                        customResources = manifest.customResources.copy(
                                            images = manifest.customResources.images + voce,
                                        ),
                                    ),
                                )
                            },
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
                        SezioneRisorsePersonalizzate(
                            titolo = "Suoni personalizzati",
                            voci = manifest.customResources.sounds,
                            onCambia = { nuoveVoci ->
                                onManifestCambiato(manifest.copy(customResources = manifest.customResources.copy(sounds = nuoveVoci)))
                            },
                        )
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
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        if (nodoTrascinato == null) {
                            change.consume()
                            panX += dragAmount.x
                            panY += dragAmount.y
                        }
                    }
                }
                // Click sullo sfondo (fuori da qualunque nodo) toglie la
                // selezione con contorno blu — coerente con l'aspettativa
                // che "clicco altrove" deselezioni.
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { sceneSelezionata = null })
                },
        ) {
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
                        // Arco estraneo al vicinato del nodo sotto il
                        // mouse -> attenuato, stessa idea dei nodi sotto.
                        val estraneo = nodoSottoMouse != null &&
                            edge.fromSceneId != nodoSottoMouse && edge.toSceneId != nodoSottoMouse
                        // Arco sul percorso da START al nodo sotto il
                        // mouse -> viola e più spesso, sopra il
                        // verde/rosso di risoluzione (§6.2).
                        val suPercorso = (edge.fromSceneId to edge.toSceneId) in archiPercorso
                        drawLine(
                            color = if (suPercorso) {
                                Color(0xFF8E24AA)
                            } else {
                                (if (edge.resolved) Color(0xFF4CAF50) else Color(0xFFE53935))
                                    .copy(alpha = if (estraneo) 0.15f else 1f)
                            },
                            start = Offset(from.x + NODE_WIDTH.value / 2, from.y + NODE_HEIGHT.value / 2),
                            end = Offset(to.x + NODE_WIDTH.value / 2, to.y + NODE_HEIGHT.value / 2),
                            strokeWidth = if (suPercorso) 4f else 2f,
                        )
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
                    // blu") — stesso blu della corrispondenza di ricerca
                    // attiva, priorità più bassa: se le due coincidono non
                    // cambia nulla a vista.
                    val isSelezionata = nodo.sceneId == sceneSelezionata
                    // Vicinato (§6.1): fuori dal mouse-over, tutti a piena
                    // opacità; con un nodo sotto mouse, solo lui e i suoi
                    // collegati diretti restano leggibili.
                    val opacitaNodo = if (nodoSottoMouse == null || nodo.sceneId in vicinato) 1f else 0.25f
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
                            .size(NODE_WIDTH, NODE_HEIGHT)
                            .alpha(opacitaNodo)
                            .clip(RoundedCornerShape(6.dp))
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
                                RoundedCornerShape(6.dp),
                            )
                            .onPointerEvent(PointerEventType.Enter) { nodoSottoMouse = nodo.sceneId }
                            .onPointerEvent(PointerEventType.Exit) {
                                if (nodoSottoMouse == nodo.sceneId) nodoSottoMouse = null
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
                                    onPress = { sceneSelezionata = nodo.sceneId },
                                    onDoubleTap = { onSceneSelected(nodo.sceneId) },
                                )
                            }
                            .pointerInput(nodo.sceneId) {
                                // Ancora locale alla coroutine del gesto
                                // (non allo stato Compose): accumula qui lo
                                // scarto e scrive SOLO su posizioniManuali,
                                // non lo rilegge mai — evita qualunque
                                // deriva dovuta ai tempi della
                                // ricomposizione tra una lettura e la
                                // scrittura successiva.
                                var ancora = Offset.Zero
                                detectDragGestures(
                                    onDragStart = {
                                        nodoTrascinato = nodo.sceneId
                                        ancora = posizioneEffettiva(nodo.sceneId) ?: Offset.Zero
                                    },
                                    onDragEnd = { nodoTrascinato = null },
                                    onDragCancel = { nodoTrascinato = null },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        ancora = Offset(ancora.x + dragAmount.x, ancora.y + dragAmount.y)
                                        posizioniManuali = posizioniManuali + (nodo.sceneId to ancora)
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
                        // Colore fisso, non legato al tema: gli sfondi
                        // verde/rosso restano chiari per il significato di
                        // salute (§6.2) indipendentemente dal tema attivo.
                        // Sopra un'immagine invece il testo passa a
                        // bianco con uno scrim scuro dietro, altrimenti
                        // resterebbe illeggibile su una foto qualunque.
                        val etichetta = if (scenaNodo != null) "${nodo.sceneId} · ${codiceScena(scenaNodo)}" else nodo.sceneId
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
                                scenaNodo?.narrativeText?.replace("\n", " ")?.trim().orEmpty(),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                color = if (immagineNodo != null) Color(0xFFE0E0E0) else Color.DarkGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
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
@Composable
private fun SezioneImmaginiInUso(manifest: Manifest, onRegistra: (CustomResourceEntry) -> Unit) {
    val inUso = remember(manifest) {
        manifest.scenes.flatMap { listOfNotNull(it.backgroundImage, it.npcImage, it.combat?.enemyImage) }.distinct()
    }
    Text("Immagini in uso nelle scene (${inUso.size})", style = MaterialTheme.typography.titleMedium)
    if (inUso.isEmpty()) {
        Text("Nessuna immagine ancora usata in questo libro.", style = MaterialTheme.typography.bodySmall)
    }
    inUso.forEach { valore ->
        val registrata = manifest.customResources.images.firstOrNull { "${ImageReference.URL_PREFIX}${it.url}" == valore }
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
                    onRegistra(CustomResourceEntry(idProposto, url))
                }) { Text("+ Registra") }
            }
        }
    }
}

// Una sezione del pannello "Risorse personalizzate" (§15.7): elenco con
// pulsante di rimozione per voce + riga per aggiungerne una nuova.
// Uguale per immagini e suoni, cambia solo l'elenco passato.
@Composable
private fun SezioneRisorsePersonalizzate(
    titolo: String,
    voci: List<CustomResourceEntry>,
    onCambia: (List<CustomResourceEntry>) -> Unit,
) {
    var nuovoId by remember { mutableStateOf("") }
    var nuovoUrl by remember { mutableStateOf("") }

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
        OutlinedTextField(
            value = nuovoUrl,
            onValueChange = { nuovoUrl = it },
            label = { Text("https://...") },
            modifier = Modifier.weight(2f),
            singleLine = true,
        )
        Button(onClick = {
            if (nuovoId.isNotBlank() && nuovoUrl.isNotBlank()) {
                onCambia(voci + CustomResourceEntry(nuovoId, nuovoUrl))
                nuovoId = ""
                nuovoUrl = ""
            }
        }) { Text("+ Aggiungi") }
    }
}
