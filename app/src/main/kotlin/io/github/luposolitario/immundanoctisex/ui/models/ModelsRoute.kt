package io.github.luposolitario.immundanoctisex.ui.models

import io.github.luposolitario.immundanoctisex.BuildConfig
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import io.github.luposolitario.immundanoctisex.AppContainer
import io.github.luposolitario.immundanoctisex.inference.InferencePreferences
import io.github.luposolitario.immundanoctisex.model.DownloadableModel
import io.github.luposolitario.immundanoctisex.model.EngineType
import io.github.luposolitario.immundanoctisex.model.ModelCatalog
import io.github.luposolitario.immundanoctisex.model.ModelDownloadWorker
import io.github.luposolitario.immundanoctisex.model.ModelPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// I due formati che i due motori sanno caricare (REGOLE tecniche in
// DIARIO.md): un modello .task di MediaPipe o qualunque altra cosa
// compilerebbe ma fallirebbe al primo caricamento, silenziosamente sul
// device di Michele. Meglio rifiutarlo subito, con un messaggio chiaro.
private const val LITERTLM_EXTENSION = ".litertlm"
// GGUF (27/07/2026, Michele: "introdurrei la possibilità di caricare i
// gguf") — LlamaCppEngine via Llamatik.
private const val GGUF_EXTENSION = ".gguf"

// Raccordo della schermata Modelli: avvia il worker, osserva il progresso
// e tiene aggiornata la lista dei modelli già scaricati.
@Composable
fun ModelsRoute(
    container: AppContainer,
    isDarkTheme: Boolean,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val preferences = container.modelPreferences
    val workManager = remember { WorkManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    val inferencePreferences = container.inferencePreferences

    var selectedModelId by remember { mutableStateOf(preferences.selectedModelId) }
    var token by remember { mutableStateOf(preferences.huggingFaceToken.orEmpty()) }
    var customModels by remember { mutableStateOf(preferences.customModels) }
    // Catalogo "Consigliati" (28/07/2026, Michele: "un file json con i
    // link... così possiamo creare dei file con i vari modelli da
    // provare"): i due fissi (ModelCatalog.protected) più i candidati,
    // di fabbrica o importati da un file — sostituisce ModelCatalog.all
    // com'era usato qui prima, che non vedeva mai un catalogo importato.
    var catalogModels by remember { mutableStateOf(preferences.activeModels) }
    var downloadedIds by remember {
        mutableStateOf(
            (catalogModels + customModels).filter { preferences.isDownloaded(it) }.map { it.id }.toSet(),
        )
    }
    var addModelError by remember { mutableStateOf<String?>(null) }
    var isImportingFromStorage by remember { mutableStateOf(false) }
    var catalogError by remember { mutableStateOf<String?>(null) }
    var isImportingCatalog by remember { mutableStateOf(false) }
    // Quale modello e' DAVVERO nel motore ora (non solo selezionato):
    // letto DIRETTAMENTE dal container, mai copiato in uno stato locale
    // (BUG 01/08/2026, vedi AppContainer.loadedModelId): una copia presa
    // alla composizione non si accorgeva mai della fine di un auto-load
    // partito all'avvio dell'app, e le card restavano su "Attiva" mentre
    // il motore stava già traducendo.
    val activeModelId = container.loadedModelId
    var activateError by remember { mutableStateOf<String?>(null) }
    // Il nome digitato prima di aprire il selettore file: il risultato
    // arriva in una callback separata, che non ha più accesso al form.
    var pendingImportName by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isImportingFromStorage = true
        addModelError = null
        scope.launch {
            val result = importModelFromUri(context, preferences, uri, pendingImportName)
            isImportingFromStorage = false
            result.onSuccess { model ->
                preferences.addCustomModel(model)
                customModels = preferences.customModels
                selectedModelId = model.id
                preferences.selectedModelId = model.id
                downloadedIds = downloadedIds + model.id
            }.onFailure { error ->
                addModelError = error.message ?: "Importazione non riuscita."
            }
        }
    }

    // Import dell'INTERO catalogo GGUF (JSON): sostituisce
    // ModelPreferences.candidateModels tutto insieme, non si somma un
    // modello alla volta come customModels sopra.
    val catalogImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isImportingCatalog = true
        catalogError = null
        scope.launch {
            val result = importCatalogFromUri(context, uri)
            isImportingCatalog = false
            result.onSuccess { imported ->
                preferences.candidateModels = imported
                catalogModels = preferences.activeModels
                downloadedIds = (catalogModels + customModels).filter { preferences.isDownloaded(it) }
                    .map { it.id }.toSet()
            }.onFailure { error ->
                catalogError = error.message ?: "Importazione del catalogo non riuscita."
            }
        }
    }

    // Export: JSON dei soli candidati (non i due fissi, che non
    // cambiano mai da un file all'altro).
    val catalogExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        catalogError = null
        scope.launch {
            exportCatalogToUri(context, uri, preferences.candidateModels).onFailure { error ->
                catalogError = error.message ?: "Esportazione non riuscita."
            }
        }
    }
    var advanced by remember {
        mutableStateOf(
            AdvancedSettingsUi(
                maxTokens = inferencePreferences.maxTokens.toString(),
                temperature = inferencePreferences.temperature,
                topK = inferencePreferences.topK.toString(),
                topP = inferencePreferences.topP,
                askImageInPrompt = inferencePreferences.askImageInPrompt,
                translationMode = inferencePreferences.translationMode,
                engineEnabled = inferencePreferences.engineEnabled,
            ),
        )
    }

    val workInfos by workManager
        .getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.WORK_NAME)
        .collectAsState(initial = emptyList())

    val activeWorkInfo = workInfos.firstOrNull()
    val downloadState = activeWorkInfo.toUiState()
    // BUG (24/07/2026, Michele: download "bloccati" — vedi
    // ModelDownloadWorker per il dettaglio): il progresso ora si
    // attribuisce leggendo l'id che il worker rimanda nel progresso,
    // MAI più fidandosi di selectedModelId (poteva appartenere a un
    // modello diverso da quello che sta davvero scaricando).
    val runningModelId = activeWorkInfo?.progress?.getString(ModelDownloadWorker.KEY_MODEL_ID)

    // A download finito la lista si aggiorna: il bottone diventa "Elimina".
    // I modelli personalizzati partono con sizeBytes=0 (ignota finché non
    // si scarica): appena il file esiste, isDownloaded() lo considera
    // valido a prescindere dalla dimensione vera. Fissarla qui col valore
    // reale rende i controlli successivi significativi, non sempre "vero"
    // (bug 22/07: senza questo un download troncato/sbagliato restava
    // segnato "già scaricato" per sempre).
    if (downloadState is DownloadUiState.Done) {
        val justDownloaded = (catalogModels + customModels).firstOrNull { it.id == selectedModelId }
        if (justDownloaded != null && justDownloaded.custom && justDownloaded.sizeBytes <= 0L) {
            val realSize = preferences.fileFor(justDownloaded).length()
            if (realSize > 0L) {
                preferences.addCustomModel(justDownloaded.copy(sizeBytes = realSize))
                customModels = preferences.customModels
            }
        }
        downloadedIds = (catalogModels + customModels).filter { preferences.isDownloaded(it) }.map { it.id }.toSet()
        // Scaricare un modello è un gesto esplicito di volerlo usare
        // (01/08/2026, Michele: "se lo scarico prova ad attivarlo nella
        // prossima sessione"): riaccende l'auto-load anche se il motore
        // era stato spento a mano dall'interruttore. NON carica nulla
        // adesso — "prossima sessione", cioè alla prossima apertura
        // dell'app o entrando in avventura: un download appena finito non
        // implica voler aspettare subito altri 15-20s di caricamento.
        if (!inferencePreferences.engineEnabled) {
            inferencePreferences.engineEnabled = true
            advanced = advanced.copy(engineEnabled = true)
        }
    }

    ModelsScreen(
        isDarkTheme = isDarkTheme,
        models = catalogModels,
        customModels = customModels,
        ggufAvailable = BuildConfig.NATIVE_LLAMA_AVAILABLE,
        selectedModelId = selectedModelId,
        downloadedIds = downloadedIds,
        token = token,
        downloadState = downloadState,
        runningModelId = runningModelId,
        onSelectModel = { model ->
            selectedModelId = model.id
            preferences.selectedModelId = model.id
        },
        activeModelId = activeModelId,
        // Stato condiviso (01/08/2026, corsa fra auto-load all'avvio e
        // "Attiva" manuale): AppContainer.isModelLoading è UNA sola fonte
        // di verità, letta da questa schermata come da AdventureRoute —
        // niente più stato locale che non sapeva di un caricamento
        // partito altrove.
        isActivating = container.isModelLoading,
        activateError = activateError,
        onActivate = { model ->
            // Log al tocco (27/07/2026, Michele: "premo Attiva e non parte
            // nulla"): senza questo, un'attivazione che fallisce prima di
            // entrare nel motore (o un motore che non logga nulla, vedi il
            // bug di LLamaAndroid.isLoad) non lascia traccia in logcat.
            Log.i("ModelsRoute", "onActivate: tocco su ${model.id} (${model.displayName})")
            activateError = null
            scope.launch {
                val result = container.activateModel(model)
                result.onSuccess {
                    selectedModelId = model.id
                    // activeModelId si aggiorna da sé (container.loadedModelId
                    // è osservabile). activateModel() riaccende sempre
                    // engineEnabled: lo stato della card segue la preferenza.
                    advanced = advanced.copy(engineEnabled = true)
                }.onFailure { error ->
                    activateError = error.message ?: "Attivazione non riuscita."
                }
            }
        },
        // Spegnimento dal bottone rosso della card attiva (01/08/2026,
        // Michele: "deve essere possibile disattivare il motore premendo
        // su quello attivato e poi devono vincere le regole che abbiamo
        // detto") — stessa identica strada dell'interruttore in
        // Impostazioni avanzate: da qui in poi vale engineEnabled=false
        // (niente auto-load all'avvio, la scelta in avventura ricompare),
        // finché non si riattiva a mano o si scarica un modello nuovo.
        onDeactivate = {
            activateError = null
            scope.launch {
                container.disableEngine()
                advanced = advanced.copy(engineEnabled = false)
            }
        },
        onTokenChange = { newToken ->
            token = newToken
            preferences.huggingFaceToken = newToken
        },
        onDownload = { model -> startDownload(workManager, container, model) },
        onCancel = { workManager.cancelUniqueWork(ModelDownloadWorker.WORK_NAME) },
        onDelete = { model ->
            preferences.deleteModel(model)
            downloadedIds = downloadedIds - model.id
            // BUG (22/07/2026, Michele: "anche se ho cancellato un modello
            // questo risulta attivo"): il file spariva ma la card
            // continuava a mostrare "In uso ora" per un modello che non
            // esiste più sul telefono. Ora si scarica per davvero dal
            // motore (01/08/2026) invece di azzerare solo lo stato della
            // schermata — il motore ce l'aveva ancora in RAM.
            scope.launch { container.unloadIfLoaded(model) }
        },
        onAddCustomModel = { url, name, requiresToken ->
            if (url.isNotBlank()) {
                val fileName = url.substringBefore('?').substringAfterLast('/').ifBlank { "modello_custom" }
                val error = validateModelFile(fileName)
                if (error != null) {
                    addModelError = error
                } else {
                    addModelError = null
                    val model = buildCustomModel(url, fileName, name, requiresToken)
                    preferences.addCustomModel(model)
                    customModels = preferences.customModels
                    selectedModelId = model.id
                    preferences.selectedModelId = model.id
                    startDownload(workManager, container, model)
                }
            }
        },
        onRemoveCustomModel = { model ->
            preferences.deleteModel(model)
            preferences.removeCustomModel(model.id)
            customModels = preferences.customModels
            downloadedIds = downloadedIds - model.id
        },
        addModelError = addModelError,
        isImportingFromStorage = isImportingFromStorage,
        onPickFromStorage = { name ->
            pendingImportName = name
            addModelError = null
            filePickerLauncher.launch(arrayOf("*/*"))
        },
        storageInfo = storageInfo(downloadedIds.size, occupiedBytes(container, catalogModels + customModels)),
        catalogError = catalogError,
        isImportingCatalog = isImportingCatalog,
        onExportCatalog = { catalogExportLauncher.launch("modelli-immundanoctisex.json") },
        onImportCatalog = {
            catalogError = null
            catalogImportLauncher.launch(arrayOf("application/json"))
        },
        onResetCatalog = {
            preferences.resetCandidatesToDefaults()
            catalogModels = preferences.activeModels
            catalogError = null
            downloadedIds = (catalogModels + customModels).filter { preferences.isDownloaded(it) }
                .map { it.id }.toSet()
        },
        advancedSettings = advanced,
        // I campi numerici accettano solo cifre e si salvano solo quando
        // il valore è sensato: un campo vuoto durante la digitazione non
        // deve scrivere zero nelle preferenze.
        onMaxTokensChange = { raw ->
            if (raw.all { it.isDigit() } && raw.length <= 6) {
                advanced = advanced.copy(maxTokens = raw)
                raw.toIntOrNull()?.takeIf { it >= InferencePreferences.MIN_TOKENS }
                    ?.let { inferencePreferences.maxTokens = it }
            }
        },
        onTemperatureChange = { advanced = advanced.copy(temperature = it) },
        onTemperatureCommit = { inferencePreferences.temperature = advanced.temperature },
        onTopKChange = { raw ->
            if (raw.all { it.isDigit() } && raw.length <= 3) {
                advanced = advanced.copy(topK = raw)
                raw.toIntOrNull()?.takeIf { it > 0 }?.let { inferencePreferences.topK = it }
            }
        },
        onTopPChange = { advanced = advanced.copy(topP = it) },
        onTopPCommit = { inferencePreferences.topP = advanced.topP },
        onAskImageInPromptChange = { enabled ->
            advanced = advanced.copy(askImageInPrompt = enabled)
            inferencePreferences.askImageInPrompt = enabled
        },
        onTranslationModeChange = { enabled ->
            advanced = advanced.copy(translationMode = enabled)
            inferencePreferences.translationMode = enabled
        },
        onEngineEnabledChange = { enabled ->
            advanced = advanced.copy(engineEnabled = enabled)
            if (enabled) {
                // Riaccenderlo qui è solo la preferenza: il caricamento
                // vero riparte da sé al prossimo ensureModelLoaded()
                // (prossima apertura dell'app, o entrando in avventura),
                // niente da fare subito — coerente con "prova ad
                // attivarlo nella prossima sessione" (Michele).
                inferencePreferences.engineEnabled = true
            } else {
                // Spegnerlo invece scarica il motore dalla memoria SUBITO
                // (AppContainer.disableEngine), non aspetta la prossima
                // apertura dell'app.
                scope.launch { container.disableEngine() }
            }
        },
        onResetSettings = {
            inferencePreferences.resetToDefaults()
            advanced = AdvancedSettingsUi(
                maxTokens = inferencePreferences.maxTokens.toString(),
                temperature = inferencePreferences.temperature,
                topK = inferencePreferences.topK.toString(),
                topP = inferencePreferences.topP,
                askImageInPrompt = inferencePreferences.askImageInPrompt,
                translationMode = inferencePreferences.translationMode,
                engineEnabled = inferencePreferences.engineEnabled,
            )
        },
        onClose = onClose,
    )
}

private fun occupiedBytes(container: AppContainer, allModels: List<DownloadableModel>): Long =
    allModels
        .map { container.modelPreferences.fileFor(it) }
        .filter { it.exists() }
        .sumOf { it.length() }

private fun validateModelFile(fileName: String): String? =
    if (!fileName.endsWith(LITERTLM_EXTENSION, ignoreCase = true) &&
        !fileName.endsWith(GGUF_EXTENSION, ignoreCase = true)
    ) {
        "\"$fileName\" non è un formato riconosciuto: serve un file " +
            "$LITERTLM_EXTENSION (LiteRT-LM) o $GGUF_EXTENSION (GGUF)."
    } else {
        null
    }

// Il motore si riconosce dall'estensione, non da una scelta manuale in
// più nel form. BuildConfig.NATIVE_LLAMA_AVAILABLE (27/07/2026): su
// questo branch sperimentale (buildLlama=true) Llamatik non è
// impacchettato — un .gguf incollato a mano instradato su LLAMA_CPP
// fallirebbe in silenzio (motore assente), esattamente il bug capitato
// a Michele con un modello personalizzato. Quando il motore nativo è
// disponibile, i .gguf vanno lì di default.
private fun engineTypeFor(fileName: String): EngineType = when {
    !fileName.endsWith(GGUF_EXTENSION, ignoreCase = true) -> EngineType.LITERT_LM
    BuildConfig.NATIVE_LLAMA_AVAILABLE -> EngineType.LLAMA_CPP_NATIVE
    else -> EngineType.LLAMA_CPP
}

private fun slugFor(fileName: String): String =
    fileName.substringBeforeLast('.').lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

// Dal link incollato costruisce un modello "su misura": il nome del file
// è l'ultimo pezzo del percorso (come fa Hugging Face per il download
// diretto), la dimensione resta ignota finché il download non la scopre
// da sé (stesso trattamento già in uso per i repo riservati del catalogo).
private fun buildCustomModel(url: String, fileName: String, name: String, requiresToken: Boolean): DownloadableModel =
    DownloadableModel(
        id = "custom-${slugFor(fileName)}",
        displayName = name.ifBlank { fileName },
        url = url,
        fileName = fileName,
        sizeBytes = 0L,
        requiresToken = requiresToken,
        note = "Modello personalizzato, aggiunto da un link Hugging Face.",
        custom = true,
        engineType = engineTypeFor(fileName),
    )

private fun queryDisplayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }

// Il motore vuole un java.io.File reale (LiteRtLmEngine.load usa
// modelFile.absolutePath): un Uri content:// del selettore file non
// basta, va copiato per intero nella cartella modelli dell'app. Sono
// GB: gira su Dispatchers.IO, mai sul thread di UI.
private suspend fun importModelFromUri(
    context: Context,
    preferences: ModelPreferences,
    uri: Uri,
    name: String,
): Result<DownloadableModel> = withContext(Dispatchers.IO) {
    runCatching {
        val originalName = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "modello_custom.litertlm"
        validateModelFile(originalName)?.let { throw IllegalArgumentException(it) }

        val model = DownloadableModel(
            id = "custom-${slugFor(originalName)}",
            displayName = name.ifBlank { originalName },
            url = "",
            fileName = originalName,
            sizeBytes = 0L,
            requiresToken = false,
            note = "Modello personalizzato, importato da un file sul telefono.",
            custom = true,
            engineType = engineTypeFor(originalName),
        )
        val destination = preferences.fileFor(model)
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Impossibile leggere il file scelto.")

        // Ora la dimensione è nota per davvero: è quella copiata, non
        // una stima. isDownloaded() la userà per il controllo integrità.
        model.copy(sizeBytes = destination.length())
    }
}

// Catalogo GGUF sperimentale come JSON scambiabile (28/07/2026, Michele:
// "un file json che contiene i link dei vari modelli... così possiamo
// creare dei file con i vari modelli da provare"). Un id che collide con
// uno dei due modelli fissi (ModelCatalog.protected) viene scartato: quei
// due non si toccano mai da un import, altrimenti finiremmo con due card
// per lo stesso Gemma 4 E4B/E2B.
private val catalogJson = Json { prettyPrint = true }

private suspend fun importCatalogFromUri(
    context: Context,
    uri: Uri,
): Result<List<DownloadableModel>> = withContext(Dispatchers.IO) {
    runCatching {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: throw IllegalStateException("Impossibile leggere il file scelto.")
        val parsed = catalogJson.decodeFromString<List<DownloadableModel>>(text)
        if (parsed.isEmpty()) throw IllegalArgumentException("Il file non contiene modelli.")
        val protectedIds = ModelCatalog.protected.map { it.id }.toSet()
        val cleaned = parsed.filterNot { it.id in protectedIds }
        cleaned.firstOrNull { it.id.isBlank() || it.url.isBlank() || it.fileName.isBlank() }?.let {
            throw IllegalArgumentException("Una voce del file non ha id, url o nome file.")
        }
        cleaned
    }
}

private suspend fun exportCatalogToUri(
    context: Context,
    uri: Uri,
    candidates: List<DownloadableModel>,
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(catalogJson.encodeToString(candidates).toByteArray())
        } ?: throw IllegalStateException("Impossibile scrivere il file scelto.")
    }
}

private fun storageInfo(count: Int, bytes: Long): String? {
    if (count == 0 || bytes <= 0L) return null
    return "Modelli sul telefono: $count — %.2f GB occupati".format(bytes / 1_000_000_000.0)
}

private fun startDownload(
    workManager: WorkManager,
    container: AppContainer,
    model: DownloadableModel,
) {
    val preferences = container.modelPreferences
    val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
        .setInputData(
            workDataOf(
                ModelDownloadWorker.KEY_MODEL_ID to model.id,
                ModelDownloadWorker.KEY_URL to model.url,
                ModelDownloadWorker.KEY_DESTINATION to preferences.fileFor(model).absolutePath,
                ModelDownloadWorker.KEY_EXPECTED_SIZE to model.sizeBytes,
                ModelDownloadWorker.KEY_TOKEN to preferences.huggingFaceToken,
            ),
        )
        // Erano GB solo su rete non a consumo, ma Michele (22/07/2026:
        // "non mi fa scaricare anche con il 5G, io ho la connessione flat
        // per cui non mi importa") ha un piano dati flat e vuole scaricare
        // anche via cellulare — la scelta era "in attesa" da giorni
        // (CRITICITA.md), ora è presa: basta una rete qualunque.
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build(),
        )
        .build()

    // REPLACE: un nuovo download sostituisce quello in corso, non ne
    // accoda un secondo sullo stesso file.
    workManager.enqueueUniqueWork(ModelDownloadWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, request)
}

private fun WorkInfo?.toUiState(): DownloadUiState = when (this?.state) {
    null -> DownloadUiState.Idle
    WorkInfo.State.RUNNING -> DownloadUiState.Running(
        downloaded = progress.getLong(ModelDownloadWorker.KEY_BYTES_DOWNLOADED, 0L),
        total = progress.getLong(ModelDownloadWorker.KEY_TOTAL_BYTES, 0L),
    )
    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadUiState.Running(0L, 0L)
    WorkInfo.State.SUCCEEDED -> DownloadUiState.Done
    WorkInfo.State.FAILED -> DownloadUiState.Failed(
        outputData.getString(ModelDownloadWorker.KEY_ERROR)
            ?: "Download non riuscito. Riprova: riprenderà da dove si era fermato.",
    )
    WorkInfo.State.CANCELLED -> DownloadUiState.Idle
}
