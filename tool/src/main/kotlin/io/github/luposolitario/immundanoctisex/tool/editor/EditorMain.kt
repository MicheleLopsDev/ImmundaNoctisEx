package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageLoadResult
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageRepository
import io.github.luposolitario.immundanoctisex.tool.FilePackageSource
import java.io.File

// Punto d'ingresso della GUI (doc/EDITOR.md). Separato dalla CLI (Main.kt):
// org.jetbrains.compose registra il proprio task `run` per questa finestra,
// la CLI passa dal task `cli` dedicato (vedi tool/build.gradle.kts).
//
// Schermata di avvio (§5): Carica libro esistente / Crea libro nuovo. Un
// caricamento riuscito porta alla mappa (§6, MapScreen.kt); uno fallito
// mostra gli errori. "Crea libro nuovo" (§9) apre il form dei campi
// globali + la scelta dello scaffold, poi chiede subito dove salvare
// (un libro nuovo non ha ancora un file).
private sealed class Schermata {
    data object Avvio : Schermata()
    data class Mappa(val file: File, val manifest: Manifest, val warnings: List<String>) : Schermata()
    data class EditorScena(val file: File, val manifest: Manifest, val warnings: List<String>, val sceneId: String) : Schermata()
    data class LibroNonValido(val errors: List<String>) : Schermata()
    data object CreaNuovo : Schermata()
}

fun main() = application {
    var schermata by remember { mutableStateOf<Schermata>(Schermata.Avvio) }
    // Tema chiaro/scuro (30/07/2026, Michele: "due temi come per gli
    // smartphone"): solo un interruttore manuale per adesso, sempre
    // visibile sopra qualunque schermata — gli schemi di colore sono
    // quelli di default di Material3, la personalizzazione vera e
    // propria (come i bordi neri dei nodi) resta un pezzo a parte.
    var temaScuro by remember { mutableStateOf(false) }
    // Conferma di sovrascrittura (30/07/2026, Michele: "la prima volta
    // chiedimi conferma successivamente no") — a livello di sessione, non
    // di singola schermata: passare dalla mappa al pannello di una scena e
    // tornare indietro NON deve far ricomparire la richiesta.
    var salvataggioGiaConfermato by remember { mutableStateOf(false) }
    // Stato della vista mappa (zoom/pan/orientamento/posizioni trascinate,
    // MapScreen.kt) ricordato QUI, non dentro MapScreen: quello schermo
    // viene distrutto e ricreato ogni volta che apri e chiudi una scena
    // (ramo diverso del `when` sotto), un `remember` locale si perderebbe
    // a ogni giro — bug segnalato da Michele il 30/07/2026.
    val mapViewState = rememberMapViewState()

    // Dimensione di partenza generosa + resizable esplicito (30/07/2026,
    // Michele: "la finestra non è ridimensionabile ma si può solo
    // massimizzare") — senza uno stato esplicito la finestra parte troppo
    // piccola e il trascinamento dei bordi non è affidabile.
    val windowState = rememberWindowState(
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(1280.dp, 800.dp),
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "ImmundaNoctisEx — Editor",
        state = windowState,
        resizable = true,
    ) {
        MaterialTheme(colorScheme = if (temaScuro) darkColorScheme() else lightColorScheme()) {
            // Sfondo del tema sul contenitore radice (30/07/2026, Michele:
            // "in tema scuro non cambi lo sfondo") — senza, la finestra
            // mostra lo sfondo bianco di default di AWT/Skiko sotto ogni
            // schermata, MaterialTheme colora solo i widget espliciti.
            // 30/07/2026, secondo giro (Michele: "in modalità scura non si
            // leggono i testi perché sono neri anche loro"): un `Box` con
            // `.background(...)` colora lo sfondo ma NON imposta
            // `LocalContentColor` — ogni `Text()` senza un `color`
            // esplicito nel resto dell'editor cadeva quindi sul default di
            // Material3 (nero fisso), illeggibile sopra uno sfondo scuro.
            // `Surface` fa la stessa cosa di sfondo MA imposta anche
            // `LocalContentColor` al contrasto giusto per quel colore
            // (`contentColorFor`) — chiaro su sfondo scuro, scuro su
            // sfondo chiaro, per tutti i testi del resto dell'editor senza
            // dover mettere un colore esplicito uno per uno. Le etichette
            // DENTRO i nodi della mappa restano nere esplicite (già
            // corrette): i loro sfondi verde/rosso sono fissi e chiari
            // indipendentemente dal tema.
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (val s = schermata) {
                        is Schermata.Avvio -> AvvioScreen(
                            onCaricaLibro = { file ->
                                when (val esito = PackageRepository(FilePackageSource(file)).load()) {
                                    is PackageLoadResult.Success ->
                                        schermata = Schermata.Mappa(file, esito.manifest, esito.warnings)
                                    is PackageLoadResult.Failure ->
                                        schermata = Schermata.LibroNonValido(esito.errors)
                                }
                            },
                            onCreaNuovo = { schermata = Schermata.CreaNuovo },
                        )
                        is Schermata.Mappa -> MapScreen(
                            file = s.file,
                            manifest = s.manifest,
                            warnings = s.warnings,
                            mapViewState = mapViewState,
                            onTornaAvvio = { schermata = Schermata.Avvio },
                            onSceneSelected = { sceneId ->
                                schermata = Schermata.EditorScena(s.file, s.manifest, s.warnings, sceneId)
                            },
                            salvataggioGiaConfermato = salvataggioGiaConfermato,
                            onSalvataggioConfermato = { salvataggioGiaConfermato = true },
                            onFileCambiato = { nuovoFile -> schermata = Schermata.Mappa(nuovoFile, s.manifest, s.warnings) },
                        )
                        is Schermata.EditorScena -> {
                            val scena = s.manifest.scenes.first { it.id == s.sceneId }
                            SceneEditorScreen(
                                scene = scena,
                                tutteLeScene = s.manifest.scenes,
                                onSalva = { sceneAggiornata ->
                                    val nuoveScene = s.manifest.scenes.map {
                                        if (it.id == sceneAggiornata.id) sceneAggiornata else it
                                    }
                                    schermata = Schermata.Mappa(s.file, s.manifest.copy(scenes = nuoveScene), s.warnings)
                                },
                                onAnnulla = { schermata = Schermata.Mappa(s.file, s.manifest, s.warnings) },
                            )
                        }
                        is Schermata.LibroNonValido -> LibroNonValidoScreen(
                            errors = s.errors,
                            onTornaAvvio = { schermata = Schermata.Avvio },
                        )
                        is Schermata.CreaNuovo -> CreaNuovoScreen(
                            onCrea = { manifestNuovo ->
                                scegliPercorsoSalvataggio("${manifestNuovo.id}.json")?.let { nuovoFile ->
                                    salvaManifest(nuovoFile, manifestNuovo)
                                    salvataggioGiaConfermato = true
                                    schermata = Schermata.Mappa(nuovoFile, manifestNuovo, emptyList())
                                }
                            },
                            onTornaAvvio = { schermata = Schermata.Avvio },
                        )
                    }

                    // Sempre in basso a destra, non collide con le barre
                    // strumenti in alto delle varie schermate (es. MapScreen).
                    Button(
                        onClick = { temaScuro = !temaScuro },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    ) {
                        Text(if (temaScuro) "☀ Chiaro" else "🌙 Scuro")
                    }
                }
            }
        }
    }
}

@Composable
private fun AvvioScreen(onCaricaLibro: (File) -> Unit, onCreaNuovo: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("ImmundaNoctisEx — Editor", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { scegliFileJsonDaAprire()?.let(onCaricaLibro) }) {
                Text("Carica libro esistente")
            }
            Button(onClick = onCreaNuovo) {
                Text("Crea libro nuovo")
            }
        }
    }
}

@Composable
private fun LibroNonValidoScreen(errors: List<String>, onTornaAvvio: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "Libro NON VALIDO",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(12.dp))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            errors.forEach {
                Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onTornaAvvio) { Text("Torna all'avvio") }
    }
}

// Creazione di un libro nuovo (§9): campi globali del manifest + scelta
// tra i tre scaffold (§9.2, BookScaffolds.kt) — genere e discipline
// restano quelli di default, non c'è ancora bisogno di personalizzarli
// qui. "Crea" chiede subito dove salvare (§5): un libro appena creato
// non ha ancora un file.
@Composable
private fun CreaNuovoScreen(onCrea: (Manifest) -> Unit, onTornaAvvio: () -> Unit) {
    var titolo by remember { mutableStateOf("") }
    // Vocabolario chiuso (§15.2, Michele: "i toni devono essere fissi
    // tra quelli che dispone l'app"): niente più testo libero — si
    // scelgono i toni per NOME (checkbox), le parole grezze che finiscono
    // per davvero in Manifest.toneHints (StaticResourceCatalog.
    // ToneResource.hints) sono un dettaglio che l'autore non deve
    // conoscere a memoria.
    var toniSelezionati by remember { mutableStateOf(setOf<String>()) }
    var scaffoldScelto by remember { mutableStateOf(Scaffold.BASE) }
    var errore by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
    ) {
        Text("Crea un libro nuovo", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = titolo,
            onValueChange = { titolo = it },
            label = { Text("Titolo del libro") },
            modifier = Modifier.fillMaxWidth(),
        )
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
        Text("Da dove iniziare", style = MaterialTheme.typography.titleMedium)
        Scaffold.entries.forEach { opzione ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = scaffoldScelto == opzione, onClick = { scaffoldScelto = opzione })
                Text(opzione.etichetta)
            }
        }
        if (errore != null) {
            Spacer(Modifier.height(8.dp))
            Text("Errore: $errore", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onTornaAvvio) { Text("Ritorna") }
            Button(onClick = {
                if (titolo.isBlank()) {
                    errore = "il titolo non può essere vuoto"
                } else {
                    val toneHints = StaticResourceCatalog.registry.tones
                        .filter { it.id in toniSelezionati }
                        .flatMap { it.hints }
                        .distinct()
                    onCrea(creaManifestNuovo(titolo, "FANTASY", toneHints, scaffoldScelto))
                }
            }) { Text("Crea") }
        }
    }
}
