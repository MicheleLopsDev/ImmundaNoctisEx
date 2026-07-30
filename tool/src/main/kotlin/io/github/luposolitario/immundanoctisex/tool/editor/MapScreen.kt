package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.validation.PackageValidator
import io.github.luposolitario.immundanoctisex.core.data.validation.ValidationResult
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.abs
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MapScreen(
    file: File,
    manifest: Manifest,
    warnings: List<String>,
    onTornaAvvio: () -> Unit,
    onSceneSelected: (String) -> Unit,
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

    var zoom by remember { mutableStateOf(1f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    // Evidenziazione del vicinato (§6.1): al passaggio del mouse su un
    // nodo (non al click, che apre già il pannello di editing), i suoi
    // collegamenti diretti restano a piena opacità e il resto della
    // mappa si attenua. Calcolato dagli stessi `graph.edges` già usati
    // per disegnare gli archi, nessuna struttura dati nuova.
    var nodoSottoMouse by remember { mutableStateOf<String?>(null) }
    val vicinato = remember(nodoSottoMouse, graph) {
        val centro = nodoSottoMouse
        if (centro == null) {
            emptySet()
        } else {
            buildSet {
                add(centro)
                graph.edges.forEach { edge ->
                    if (edge.fromSceneId == centro) add(edge.toSceneId)
                    if (edge.toSceneId == centro) add(edge.fromSceneId)
                }
            }
        }
    }
    // Contorno di un percorso a richiesta (§6.2): "a richiesta" qui
    // significa "mentre passi il mouse su una scena", non un pulsante
    // dedicato — riusa lo stesso hover del vicinato invece di aggiungere
    // un'altra interazione. Un esempio di cammino da START a quella
    // scena (percorsoDaStart, SceneGraph.kt), sempre lo stesso finché non
    // cambi nodo sotto il mouse.
    val archiPercorso = remember(nodoSottoMouse, graph) {
        nodoSottoMouse?.let { percorsoDaStart(graph, it).zipWithNext().toSet() } ?: emptySet()
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
    var orizzontale by remember { mutableStateOf(false) }

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
    // Azzerati quando cambia il grafo o l'orientamento, stesso criterio
    // di `positions`: una disposizione diversa rende gli scarti vecchi
    // privi di senso.
    var posizioniManuali by remember(graph, orizzontale) { mutableStateOf<Map<String, Offset>>(emptyMap()) }
    fun posizioneEffettiva(id: String): Offset? = posizioniManuali[id] ?: positions[id]
    // Un solo accumulatore condiviso (30/07/2026): un solo puntatore alla
    // volta può trascinare, non serve uno stato per nodo — distingue un
    // trascinamento vero da un semplice click (che deve continuare ad
    // aprire il pannello di editing, come già collaudato da Michele).
    var trascinamentoAccumulato by remember { mutableStateOf(0f) }

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
                }
                Text(
                    "${manifest.title} — ${manifest.scenes.size} scene, ${warnings.size} avvisi al caricamento",
                    style = MaterialTheme.typography.bodySmall,
                )
                messaggioSalvataggio?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { orizzontale = !orizzontale }) {
                    Text(if (orizzontale) "↕ Verticale" else "↔ Orizzontale")
                }
                Button(onClick = { zoom = 1f; panX = 0f; panY = 0f; posizioniManuali = emptyMap() }) { Text("⟳ Riordina") }
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                // Sfondo del tema (30/07/2026: prima era un grigio fisso,
                // restava chiaro anche col tema scuro attivo).
                .background(MaterialTheme.colorScheme.surfaceVariant)
                // Dimensione reale del riquadro (30/07/2026, §6.1): serve
                // solo per centrare la ricerca — vedi eseguiRicerca().
                .onGloballyPositioned { viewportSize = it.size }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        panX += dragAmount.x
                        panY += dragAmount.y
                    }
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
                    // Vicinato (§6.1): fuori dal mouse-over, tutti a piena
                    // opacità; con un nodo sotto mouse, solo lui e i suoi
                    // collegati diretti restano leggibili.
                    val opacitaNodo = if (nodoSottoMouse == null || nodo.sceneId in vicinato) 1f else 0.25f
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                            .size(NODE_WIDTH, NODE_HEIGHT)
                            .alpha(opacitaNodo)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (nodo.healthy) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                            // Bordo nero di default (30/07/2026, Michele: "rendi i
                            // bordi... visibili" — la personalizzazione grafica vera
                            // e propria è un pezzo a parte, per adesso solo leggibile).
                            .border(
                                width = when {
                                    isAttiva -> 3.dp
                                    isCorrispondenza -> 2.dp
                                    else -> 1.dp
                                },
                                color = when {
                                    isAttiva -> Color(0xFF1E88E5)
                                    isCorrispondenza -> Color(0xFFFF9800)
                                    else -> Color.Black
                                },
                                RoundedCornerShape(6.dp),
                            )
                            .onPointerEvent(PointerEventType.Enter) { nodoSottoMouse = nodo.sceneId }
                            .onPointerEvent(PointerEventType.Exit) {
                                if (nodoSottoMouse == nodo.sceneId) nodoSottoMouse = null
                            }
                            // Trascinamento manuale (§6.1): un click vero
                            // (spostamento sotto soglia) apre ancora il
                            // pannello di editing come prima; superata la
                            // soglia si considera un trascinamento e sposta
                            // il nodo invece di aprirlo.
                            .pointerInput(nodo.sceneId) {
                                detectDragGestures(
                                    onDragStart = { trascinamentoAccumulato = 0f },
                                    onDragEnd = {
                                        if (trascinamentoAccumulato < 4f) onSceneSelected(nodo.sceneId)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        trascinamentoAccumulato += abs(dragAmount.x) + abs(dragAmount.y)
                                        val base = posizioneEffettiva(nodo.sceneId) ?: return@detectDragGestures
                                        posizioniManuali = posizioniManuali +
                                            (nodo.sceneId to Offset(base.x + dragAmount.x, base.y + dragAmount.y))
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        // Colore fisso, non legato al tema: gli sfondi
                        // verde/rosso restano chiari per il significato di
                        // salute (§6.2) indipendentemente dal tema attivo.
                        val scena = scenesById[nodo.sceneId]
                        val etichetta = if (scena != null) "${nodo.sceneId} · ${codiceScena(scena)}" else nodo.sceneId
                        // Riga 1: chiave (id+codice). Riga 2: estratto del
                        // testo narrato — riconoscere la scena a colpo
                        // d'occhio senza doverla aprire (30/07/2026,
                        // Michele).
                        Column(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                etichetta,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                scena?.narrativeText?.replace("\n", " ")?.trim().orEmpty(),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                color = Color.DarkGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
