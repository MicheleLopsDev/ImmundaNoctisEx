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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
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
import io.github.luposolitario.immundanoctisex.tool.inference.EditorInferenceEngine
import io.github.luposolitario.immundanoctisex.tool.inference.RiassuntorePercorso
import io.github.luposolitario.immundanoctisex.tool.inference.TraduttoreScene
import kotlinx.serialization.json.Json
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
    data class EditorScena(
        val file: File,
        val manifest: Manifest,
        val warnings: List<String>,
        val sceneId: String,
        // §15.5: solo le scene create dalla mappa (non quelle aperte per
        // modifica) hanno la rete di sicurezza verso deathSceneId se
        // restano senza collegamenti in uscita.
        val eNuova: Boolean = false,
    ) : Schermata()
    data class LibroNonValido(val errors: List<String>) : Schermata()
    data object CreaNuovo : Schermata()
    // §16.1 (Michele: "un menu per le impostazioni... mostrare come
    // pulsante nella prima maschera"): raggiungibile solo dall'Avvio.
    data object Impostazioni : Schermata()

    // Il modello locale (02/08/2026): scelta del file .litertlm, carico
    // e prova. Come Impostazioni, si raggiunge solo dall'Avvio — è una
    // configurazione della macchina, non del libro aperto.
    data object Modello : Schermata()
}

// Riassunto di un libro (§17.4, "Libri recenti") — riusata anche
// nell'intestazione dell'esportazione PNG (§19.8, 31/07/2026, Michele:
// "mettiamo in alto il nome del file stesso formato che usiamo per
// caricare i libri"): stessa stringa in entrambi i posti, un solo
// punto da cambiare se il formato evolve.
fun riepilogoLibro(nomeFile: String, manifest: Manifest): String {
    val descrizioneBreve = manifest.description.take(30) + if (manifest.description.length > 30) "…" else ""
    return "$nomeFile - ${manifest.title} - ${manifest.genre} - ${manifest.language} - $descrizioneBreve"
}

// Marcatore di build dell'editor (01/08/2026, Michele: "aggiungi un
// BUILD_MARKER anche per l'editor") — stessa convenzione del client
// (MainActivity.kt): AGGIORNARE ad OGNI modifica di codice di `:tool`
// prima di ricompilare. Qui non c'è logcat: si stampa sulla console di
// `:tool:run` e finisce nel titolo della finestra, così la versione si
// legge a colpo d'occhio anche senza guardare l'output.
const val EDITOR_BUILD_MARKER = "2026-08-07-01 le scene ritoccate dal modello si vedono nell'editor"

fun main() = application {
    println("EDITOR_BUILD_MARKER = $EDITOR_BUILD_MARKER")
    var schermata by remember { mutableStateOf<Schermata>(Schermata.Avvio) }
    // Impostazioni (§16.1, Michele: "importi dal client le font
    // disponibili, il tema selezionato che deve essere persistente e
    // si deve poter aumentare/diminuire la grandezza dei caratteri") —
    // caricate una volta sola da `EditorPreferences` (java.util.prefs,
    // sopravvive alla chiusura dell'editor, a differenza del semplice
    // `remember` di prima che perdeva il tema scelto a ogni riavvio).
    val preferenze = remember { EditorPreferences() }
    // Uno solo per tutta la sessione dell'editor: il modello resta
    // caricato passando da una schermata all'altra (3,7 GB non si
    // ricaricano a ogni navigazione).
    val motoreModello = remember { EditorInferenceEngine() }
    // Uno per sessione, come il motore: contiene il turno che impedisce
    // a due traduzioni di accavallarsi sullo stesso Engine.
    val traduttoreScene = remember { TraduttoreScene(motoreModello) }
    // Stesso motore, altro compito: riassumere il cammino da START a
    // una scena (03/08/2026). Ha un turno suo, ma il motore resta uno.
    val riassuntorePercorso = remember { RiassuntorePercorso(motoreModello) }
    var temaScuro by remember { mutableStateOf(preferenze.temaScuro) }
    var fontScelto by remember { mutableStateOf(preferenze.font) }
    var scalaTesto by remember { mutableStateOf(preferenze.scalaTesto) }
    fun impostaTemaScuro(valore: Boolean) {
        temaScuro = valore
        preferenze.temaScuro = valore
    }
    fun impostaFont(valore: FontEditor) {
        fontScelto = valore
        preferenze.font = valore
    }
    fun impostaScalaTesto(valore: ScalaTesto) {
        scalaTesto = valore
        preferenze.scalaTesto = valore
    }
    // Livelli di backup (§17.5, Michele: "potremmo decidere quanti
    // livelli di backup vogliamo... partirei da 3 fino ad un massimo
    // di 9") — stessa persistenza delle altre impostazioni.
    var livelliBackup by remember { mutableStateOf(preferenze.livelliBackup) }
    fun impostaLivelliBackup(valore: Int) {
        livelliBackup = valore.coerceIn(MAX_BACKUP_MIN, MAX_BACKUP_MAX)
        preferenze.livelliBackup = livelliBackup
    }
    // Libri recenti (§17.4): mirror osservabile da Compose di
    // EditorPreferences.libriRecenti (le Preferences non notificano da
    // sole la ricomposizione) — aggiornato ogni volta che un libro
    // viene aperto o creato con successo.
    var libriRecenti by remember { mutableStateOf(preferenze.libriRecenti) }
    // Stato della vista mappa (zoom/pan/orientamento/posizioni trascinate,
    // MapScreen.kt) ricordato QUI, non dentro MapScreen: quello schermo
    // viene distrutto e ricreato ogni volta che apri e chiudi una scena
    // (ramo diverso del `when` sotto), un `remember` locale si perderebbe
    // a ogni giro — bug segnalato da Michele il 30/07/2026. Dichiarato
    // PRIMA di `apriLibro` (31/07/2026, §19.12): quella funzione deve
    // poter idratare `posizioniManuali` dal libro appena caricato.
    val mapViewState = rememberMapViewState()
    fun apriLibro(file: File) {
        when (val esito = PackageRepository(FilePackageSource(file)).load()) {
            is PackageLoadResult.Success -> {
                preferenze.aggiungiLibroRecente(file.absolutePath)
                libriRecenti = preferenze.libriRecenti
                mapViewState.caricaPosizioniDa(esito.manifest)
                // §19.13: un libro diverso non deve ereditare la
                // cronologia di annulla/ripeti di quello aperto prima
                // nella stessa sessione dell'editor.
                mapViewState.cronologia.reimposta()
                schermata = Schermata.Mappa(file, esito.manifest, esito.warnings)
            }
            is PackageLoadResult.Failure -> schermata = Schermata.LibroNonValido(esito.errors)
        }
    }
    // Conferma di sovrascrittura (30/07/2026, Michele: "la prima volta
    // chiedimi conferma successivamente no") — a livello di sessione, non
    // di singola schermata: passare dalla mappa al pannello di una scena e
    // tornare indietro NON deve far ricomparire la richiesta.
    var salvataggioGiaConfermato by remember { mutableStateOf(false) }

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
        title = "ImmundaNoctisEx — Editor [$EDITOR_BUILD_MARKER]",
        state = windowState,
        resizable = true,
    ) {
        // Grandezza dei caratteri (§16.1): stesso principio
        // dell'impostazione di accessibilità di Android, un
        // moltiplicatore su `LocalDensity.fontScale` che scala TUTTI i
        // testi dell'editor insieme, senza dover toccare ogni singolo
        // `Text()` esistente in `MapScreen.kt`/`SceneEditorScreen.kt`.
        val densitaBase = LocalDensity.current
        val fontFamily = remember(fontScelto) { caricaFontFamily(fontScelto) }
        CompositionLocalProvider(
            LocalDensity provides Density(densitaBase.density, fontScale = scalaTesto.moltiplicatore),
        ) {
        MaterialTheme(
            colorScheme = if (temaScuro) darkColorScheme() else lightColorScheme(),
            typography = tipografiaConFont(fontFamily),
        ) {
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
                            onCaricaLibro = ::apriLibro,
                            onCreaNuovo = { schermata = Schermata.CreaNuovo },
                            onImpostazioni = { schermata = Schermata.Impostazioni },
                            onModello = { schermata = Schermata.Modello },
                            libriRecenti = libriRecenti,
                            onApriRecente = { percorso -> apriLibro(File(percorso)) },
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
                            // §15.5: la nuova scena si aggiunge subito al
                            // manifest (così esiste già quando si apre il
                            // suo pannello) e si apre direttamente il suo
                            // editor, con eNuova=true per la rete di
                            // sicurezza (SceneEditorScreen).
                            onNuovaScena = {
                                val nuova = nuovaScenaVuota(s.manifest)
                                val manifestConNuova = s.manifest.copy(scenes = s.manifest.scenes + nuova)
                                schermata = Schermata.EditorScena(s.file, manifestConNuova, s.warnings, nuova.id, eNuova = true)
                            },
                            // §17.2 (Michele: "duplicare... penso che il
                            // rinomino non ha senso"): copia con un nuovo
                            // ID, si apre subito, NON eredita la rete di
                            // sicurezza (non è una scena "nuova" in quel
                            // senso, è già contenuto scritto).
                            onDuplicaScena = { sceneId ->
                                val originale = s.manifest.scenes.first { it.id == sceneId }
                                val duplicata = duplicaScena(originale, s.manifest)
                                val manifestConDuplicata = s.manifest.copy(scenes = s.manifest.scenes + duplicata)
                                schermata = Schermata.EditorScena(s.file, manifestConDuplicata, s.warnings, duplicata.id, eNuova = false)
                            },
                            // §17.3: un insieme di ID invece di uno solo —
                            // filtrarli TUTTI in un'unica `manifest.copy`
                            // evita il bug di chiudere su un `s.manifest`
                            // ormai superato se si chiamasse una volta per
                            // scena in un ciclo.
                            onEliminaScene = { ids, ricollegaAId ->
                                mapViewState.cronologia.registraCheckpoint(Documento(s.manifest, mapViewState.posizioniManuali.value))
                                val manifestRicollegato = if (ricollegaAId != null) {
                                    ricollegaRiferimenti(s.manifest, ids, ricollegaAId)
                                } else {
                                    s.manifest
                                }
                                val manifestSenzaScene = manifestRicollegato.copy(scenes = manifestRicollegato.scenes.filterNot { it.id in ids })
                                schermata = Schermata.Mappa(s.file, manifestSenzaScene, s.warnings)
                            },
                            // §15.7: cambio generico del manifest senza
                            // navigare via dalla mappa — usato oggi da
                            // "Risorse personalizzate", "Proprietà del
                            // libro", "JSON del libro", lega/rimuovi
                            // legame e duplica gruppo (tutti dentro
                            // MapScreen.kt passano da qui): UN SOLO punto
                            // da istrumentare per la cronologia (§19.13)
                            // copre tutti quei casi insieme.
                            onManifestCambiato = { manifestAggiornato ->
                                mapViewState.cronologia.registraCheckpoint(Documento(s.manifest, mapViewState.posizioniManuali.value))
                                schermata = Schermata.Mappa(s.file, manifestAggiornato, s.warnings)
                            },
                            // §19.13 (Michele: "implementiamo ctrl-z e
                            // ctrl-y"): la cronologia vive in
                            // `mapViewState` (sopravvive al giro mappa ->
                            // scena -> mappa, stesso motivo di
                            // `posizioniManuali`); qui solo l'esecuzione,
                            // che tocca `schermata` (solo EditorMain.kt
                            // può riassegnarla).
                            onAnnullaModifica = {
                                val statoAttuale = Documento(s.manifest, mapViewState.posizioniManuali.value)
                                val precedente = mapViewState.cronologia.annulla(statoAttuale)
                                if (precedente != null) {
                                    mapViewState.posizioniManuali.value = precedente.posizioni
                                    schermata = Schermata.Mappa(s.file, precedente.manifest, s.warnings)
                                }
                            },
                            onRipetiModifica = {
                                val statoAttuale = Documento(s.manifest, mapViewState.posizioniManuali.value)
                                val successivo = mapViewState.cronologia.ripeti(statoAttuale)
                                if (successivo != null) {
                                    mapViewState.posizioniManuali.value = successivo.posizioni
                                    schermata = Schermata.Mappa(s.file, successivo.manifest, s.warnings)
                                }
                            },
                            salvataggioGiaConfermato = salvataggioGiaConfermato,
                            onSalvataggioConfermato = { salvataggioGiaConfermato = true },
                            onFileCambiato = { nuovoFile ->
                                preferenze.aggiungiLibroRecente(nuovoFile.absolutePath)
                                libriRecenti = preferenze.libriRecenti
                                schermata = Schermata.Mappa(nuovoFile, s.manifest, s.warnings)
                            },
                            livelliBackup = livelliBackup,
                            // §17.5 (Michele: "l'annulla ti riporta al
                            // backup -1"): ripristina il file da `.bak1` e
                            // ricarica il manifest da lì — stesso esito di
                            // Failure/Success di un caricamento normale, un
                            // backup corrotto a mano non deve mai crashare.
                            onAnnullaUltimoBackup = {
                                if (ripristinaUltimoBackup(s.file)) {
                                    when (val esito = PackageRepository(FilePackageSource(s.file)).load()) {
                                        is PackageLoadResult.Success -> {
                                            // §19.12: il backup ripristinato può avere
                                            // posizioni diverse da quelle in sessione.
                                            mapViewState.caricaPosizioniDa(esito.manifest)
                                            // §19.13: la cronologia di annulla/ripeti
                                            // precedente non ha più senso su uno stato
                                            // tornato indietro da un backup su disco.
                                            mapViewState.cronologia.reimposta()
                                            schermata = Schermata.Mappa(s.file, esito.manifest, esito.warnings)
                                        }
                                        is PackageLoadResult.Failure ->
                                            schermata = Schermata.LibroNonValido(esito.errors)
                                    }
                                }
                            },
                            // §19.8 (bug 31/07/2026, Michele: "non esporta con
                            // le impostazioni grafiche"): l'esportazione PNG
                            // deve vedere le stesse preferenze del resto
                            // dell'editor, non valori fissi.
                            temaScuro = temaScuro,
                            fontScelto = fontScelto,
                            scalaTesto = scalaTesto,
                            // Presente solo col modello caricato: senza,
                            // la voce nel menu non compare affatto.
                            riassuntore = if (motoreModello.isLoaded) riassuntorePercorso else null,
                            linguaRiassunto = preferenze.linguaOutput,
                        )
                        is Schermata.EditorScena -> {
                            val scena = s.manifest.scenes.first { it.id == s.sceneId }
                            SceneEditorScreen(
                                scene = scena,
                                tutteLeScene = s.manifest.scenes,
                                // Presente solo col modello caricato: senza,
                                // l'editor di scena resta identico a prima.
                                anteprimaModello = if (motoreModello.isLoaded) {
                                    AnteprimaModello(
                                        traduttore = traduttoreScene,
                                        manifest = s.manifest,
                                        lingua = preferenze.linguaOutput,
                                        modalitaTraduzione = preferenze.modalitaTraduzione,
                                    )
                                } else {
                                    null
                                },
                                onSalva = { sceneAggiornata ->
                                    val nuoveScene = s.manifest.scenes.map {
                                        if (it.id == sceneAggiornata.id) sceneAggiornata else it
                                    }
                                    schermata = Schermata.Mappa(s.file, s.manifest.copy(scenes = nuoveScene), s.warnings)
                                },
                                // Su una scena appena creata (eNuova), "Ritorna"
                                // deve ANNULLARE la creazione, non lasciarla a
                                // metà nel manifest — altrimenti creare una scena
                                // e ripensarci lascerebbe un residuo vuoto invisibile
                                // finché non lo si nota sulla mappa.
                                onAnnulla = {
                                    val manifestFinale = if (s.eNuova) {
                                        s.manifest.copy(scenes = s.manifest.scenes.filter { it.id != s.sceneId })
                                    } else {
                                        s.manifest
                                    }
                                    schermata = Schermata.Mappa(s.file, manifestFinale, s.warnings)
                                },
                                deathSceneId = s.manifest.deathSceneId,
                                eNuova = s.eNuova,
                                customResources = s.manifest.customResources,
                            )
                        }
                        is Schermata.LibroNonValido -> LibroNonValidoScreen(
                            errors = s.errors,
                            onTornaAvvio = { schermata = Schermata.Avvio },
                        )
                        is Schermata.CreaNuovo -> CreaNuovoScreen(
                            onCrea = { manifestNuovo ->
                                scegliPercorsoSalvataggio("${manifestNuovo.id}.json")?.let { nuovoFile ->
                                    salvaManifest(nuovoFile, manifestNuovo, livelliBackup)
                                    salvataggioGiaConfermato = true
                                    preferenze.aggiungiLibroRecente(nuovoFile.absolutePath)
                                    libriRecenti = preferenze.libriRecenti
                                    // §19.12: nessuna posizione ancora (libro
                                    // appena creato) — idrata comunque, per
                                    // azzerare eventuali scarti rimasti da un
                                    // libro aperto in precedenza nella stessa
                                    // sessione dell'editor.
                                    mapViewState.caricaPosizioniDa(manifestNuovo)
                                    // §19.13: stesso motivo, niente cronologia
                                    // ereditata da un libro precedente.
                                    mapViewState.cronologia.reimposta()
                                    schermata = Schermata.Mappa(nuovoFile, manifestNuovo, emptyList())
                                }
                            },
                            onTornaAvvio = { schermata = Schermata.Avvio },
                        )
                        is Schermata.Modello -> ModelloScreen(
                            preferenze = preferenze,
                            motore = motoreModello,
                            onTornaAvvio = { schermata = Schermata.Avvio },
                        )
                        is Schermata.Impostazioni -> ImpostazioniScreen(
                            temaScuro = temaScuro,
                            onTemaScuroCambiato = ::impostaTemaScuro,
                            fontScelto = fontScelto,
                            onFontCambiato = ::impostaFont,
                            scalaTesto = scalaTesto,
                            onScalaTestoCambiata = ::impostaScalaTesto,
                            livelliBackup = livelliBackup,
                            onLivelliBackupCambiati = ::impostaLivelliBackup,
                            onTornaAvvio = { schermata = Schermata.Avvio },
                        )
                    }

                    // Sempre in basso a destra, non collide con le barre
                    // strumenti in alto delle varie schermate (es. MapScreen).
                    Button(
                        onClick = { impostaTemaScuro(!temaScuro) },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    ) {
                        Text(if (temaScuro) "☀ Chiaro" else "🌙 Scuro")
                    }
                }
            }
        }
        }
    }
}

// Il benvenuto dell'editor (05/08/2026, richiesta di Michele insieme a
// quello dell'app). Un PANNELLO e non una schermata di passaggio: chi sa
// già cosa fare apre il suo libro e lo ignora; chi lo apre per la prima
// volta scopre le due cose che nessuno gli diceva — che l'editor fa
// libri da zero e non solo conversioni, e che il modello serve solo per
// l'anteprima della traduzione, non per lavorare.
@Composable
private fun BenvenutoEditor() {
    val preferenze = remember { EditorPreferences() }
    var chiuso by remember { mutableStateOf(preferenze.benvenutoChiuso) }
    if (chiuso) return

    Card(modifier = Modifier.widthIn(max = 720.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Da dove si comincia", style = MaterialTheme.typography.titleMedium)
            Text(
                "Da qui si scrivono libri di avventura: scene, scelte, combattimenti e tiri di dado. " +
                    "Puoi partire da zero con «Crea libro nuovo», oppure aprire un libro già esistente.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Il modello linguistico è facoltativo. Serve solo a due cose: mostrarti in anteprima " +
                    "come suonerà una scena tradotta o arricchita, e riassumere un percorso. " +
                    "Per scrivere e per salvare non serve.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    preferenze.benvenutoChiuso = true
                    chiuso = true
                }) {
                    Text("Ho capito")
                }
            }
        }
    }
}

@Composable
private fun AvvioScreen(
    onCaricaLibro: (File) -> Unit,
    onCreaNuovo: () -> Unit,
    onImpostazioni: () -> Unit,
    onModello: () -> Unit,
    libriRecenti: List<String>,
    onApriRecente: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("ImmundaNoctisEx — Editor", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        BenvenutoEditor()
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = { scegliFileJsonDaAprire()?.let(onCaricaLibro) }) {
                Text("Carica libro esistente")
            }
            Button(onClick = onCreaNuovo) {
                Text("Crea libro nuovo")
            }
        }
        Spacer(Modifier.height(16.dp))
        // §16.1 (Michele: "un menu per le impostazioni... mostrare come
        // pulsante nella prima maschera").
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onImpostazioni) {
                Text("⚙ Impostazioni")
            }
            // 02/08/2026: da qui si sceglie e si prova il modello locale,
            // quello che poi traduce le scene e ne riassume i gruppi.
            Button(onClick = onModello) {
                Text("🧠 Modello")
            }
        }
        // §17.4 (Michele: "caricare i libri recenti è una ottima idea"):
        // un percorso che non esiste più (file spostato/cancellato) sparisce
        // silenziosamente dalla lista mostrata, senza toccare quella salvata
        // (potrebbe ricomparire da sola, es. un'unità rimovibile ricollegata)
        // e senza mai dare un errore a sorpresa.
        // 30/07/2026, Michele: "quando mi dai la lista dei libri metti
        // Nome file - Titolo - GENERE - Lingua - Descrizione (solo i
        // primi 30 char)" — lettura "al volo" del solo Manifest (senza
        // passare da PackageRepository/PackageValidator: un libro con
        // errori di validazione resta comunque leggibile qui, non è
        // questo il posto per bloccarlo) solo per mostrare i metadati;
        // se il file non è nemmeno JSON valido, degrado silenzioso allo
        // stesso principio del resto dell'editor — si vede solo il nome
        // del file, niente crash né messaggio d'errore a sorpresa.
        val recentiEsistenti = remember(libriRecenti) {
            libriRecenti.filter { File(it).exists() }.map { percorso ->
                val manifest = runCatching {
                    Json { ignoreUnknownKeys = true }.decodeFromString(Manifest.serializer(), File(percorso).readText())
                }.getOrNull()
                percorso to manifest
            }
        }
        if (recentiEsistenti.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("Libri recenti", style = MaterialTheme.typography.titleMedium)
            recentiEsistenti.forEach { (percorso, manifest) ->
                TextButton(onClick = { onApriRecente(percorso) }) {
                    val nomeFile = File(percorso).name
                    val etichetta = if (manifest != null) {
                        riepilogoLibro(nomeFile, manifest)
                    } else {
                        "$nomeFile (non leggibile)"
                    }
                    Text(etichetta)
                }
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

// Schermata Impostazioni (§16.1, Michele: "importi dal client le font
// disponibili, il tema selezionato che deve essere persistente e si
// deve poter aumentare/diminuire la grandezza dei caratteri"): tema e
// font sono scelte esclusive (RadioButton), la grandezza del testo
// scorre i tre passi fissi di ScalaTesto con due pulsanti +/- — uno
// Slider sarebbe overkill per soli tre valori.
@Composable
private fun ImpostazioniScreen(
    temaScuro: Boolean,
    onTemaScuroCambiato: (Boolean) -> Unit,
    fontScelto: FontEditor,
    onFontCambiato: (FontEditor) -> Unit,
    scalaTesto: ScalaTesto,
    onScalaTestoCambiata: (ScalaTesto) -> Unit,
    livelliBackup: Int,
    onLivelliBackupCambiati: (Int) -> Unit,
    onTornaAvvio: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
    ) {
        Text("Impostazioni", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Text("Tema", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = !temaScuro, onClick = { onTemaScuroCambiato(false) })
            Text("Chiaro")
            Spacer(Modifier.width(16.dp))
            RadioButton(selected = temaScuro, onClick = { onTemaScuroCambiato(true) })
            Text("Scuro")
        }

        Spacer(Modifier.height(24.dp))
        Text("Font", style = MaterialTheme.typography.titleMedium)
        FontEditor.entries.forEach { opzione ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = fontScelto == opzione, onClick = { onFontCambiato(opzione) })
                Text(opzione.displayName, fontFamily = caricaFontFamily(opzione))
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Grandezza testo", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { onScalaTestoCambiata(scalaTesto.precedente()) },
                enabled = scalaTesto != ScalaTesto.entries.first(),
            ) { Text("A-") }
            Spacer(Modifier.width(8.dp))
            Text(scalaTesto.etichetta, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onScalaTestoCambiata(scalaTesto.successivo()) },
                enabled = scalaTesto != ScalaTesto.entries.last(),
            ) { Text("A+") }
        }

        // §17.5 (Michele: "potremmo decidere quanti livelli di backup
        // vogliamo io partirei da 3 fino ad un massimo di 9"): stessi
        // pulsanti +/- della grandezza testo, ma su un intero invece di
        // un enum a passi fissi.
        Spacer(Modifier.height(24.dp))
        Text("Livelli di backup", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { onLivelliBackupCambiati(livelliBackup - 1) },
                enabled = livelliBackup > MAX_BACKUP_MIN,
            ) { Text("-") }
            Spacer(Modifier.width(8.dp))
            Text("$livelliBackup", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onLivelliBackupCambiati(livelliBackup + 1) },
                enabled = livelliBackup < MAX_BACKUP_MAX,
            ) { Text("+") }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onTornaAvvio) { Text("Ritorna") }
    }
}
