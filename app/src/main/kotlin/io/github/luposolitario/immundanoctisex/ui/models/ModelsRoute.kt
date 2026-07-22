package io.github.luposolitario.immundanoctisex.ui.models

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import io.github.luposolitario.immundanoctisex.model.ModelCatalog
import io.github.luposolitario.immundanoctisex.model.ModelDownloadWorker
import io.github.luposolitario.immundanoctisex.model.ModelPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// L'unico formato che LiteRtLmEngine sa caricare (REGOLE tecniche in
// DIARIO.md): un modello .task di MediaPipe o qualunque altra cosa
// compilerebbe ma fallirebbe al primo caricamento, silenziosamente sul
// device di Michele. Meglio rifiutarlo subito, con un messaggio chiaro.
private const val LITERTLM_EXTENSION = ".litertlm"

// Raccordo della schermata Modelli: avvia il worker, osserva il progresso
// e tiene aggiornata la lista dei modelli già scaricati.
@Composable
fun ModelsRoute(
    container: AppContainer,
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
    var downloadedIds by remember {
        mutableStateOf(
            (ModelCatalog.all + customModels).filter { preferences.isDownloaded(it) }.map { it.id }.toSet(),
        )
    }
    var addModelError by remember { mutableStateOf<String?>(null) }
    var isImportingFromStorage by remember { mutableStateOf(false) }
    // Quale modello e' DAVVERO nel motore ora (non solo selezionato):
    // null finche' non si e' ancora giocata/attivata una scena in questa
    // esecuzione dell'app.
    var activeModelId by remember { mutableStateOf(container.loadedModelId) }
    var isActivating by remember { mutableStateOf(false) }
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
    var advanced by remember {
        mutableStateOf(
            AdvancedSettingsUi(
                maxTokens = inferencePreferences.maxTokens.toString(),
                temperature = inferencePreferences.temperature,
                topK = inferencePreferences.topK.toString(),
                topP = inferencePreferences.topP,
            ),
        )
    }

    val workInfos by workManager
        .getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.WORK_NAME)
        .collectAsState(initial = emptyList())

    val downloadState = workInfos.firstOrNull().toUiState()

    // A download finito la lista si aggiorna: il bottone diventa "Elimina".
    // I modelli personalizzati partono con sizeBytes=0 (ignota finché non
    // si scarica): appena il file esiste, isDownloaded() lo considera
    // valido a prescindere dalla dimensione vera. Fissarla qui col valore
    // reale rende i controlli successivi significativi, non sempre "vero"
    // (bug 22/07: senza questo un download troncato/sbagliato restava
    // segnato "già scaricato" per sempre).
    if (downloadState is DownloadUiState.Done) {
        val justDownloaded = (ModelCatalog.all + customModels).firstOrNull { it.id == selectedModelId }
        if (justDownloaded != null && justDownloaded.custom && justDownloaded.sizeBytes <= 0L) {
            val realSize = preferences.fileFor(justDownloaded).length()
            if (realSize > 0L) {
                preferences.addCustomModel(justDownloaded.copy(sizeBytes = realSize))
                customModels = preferences.customModels
            }
        }
        downloadedIds = (ModelCatalog.all + customModels).filter { preferences.isDownloaded(it) }.map { it.id }.toSet()
    }

    ModelsScreen(
        models = ModelCatalog.all,
        customModels = customModels,
        selectedModelId = selectedModelId,
        downloadedIds = downloadedIds,
        token = token,
        downloadState = downloadState,
        onSelectModel = { model ->
            selectedModelId = model.id
            preferences.selectedModelId = model.id
        },
        activeModelId = activeModelId,
        isActivating = isActivating,
        activateError = activateError,
        onActivate = { model ->
            isActivating = true
            activateError = null
            scope.launch {
                val result = container.activateModel(model)
                isActivating = false
                result.onSuccess {
                    selectedModelId = model.id
                    activeModelId = model.id
                }.onFailure { error ->
                    activateError = error.message ?: "Attivazione non riuscita."
                }
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
            // questo risulta attivo"): il file spariva ma activeModelId
            // restava quello, e la card continuava a mostrare "In uso ora"
            // per un modello che non esiste più sul telefono.
            if (activeModelId == model.id) activeModelId = null
        },
        onAddCustomModel = { url, name, requiresToken ->
            if (url.isNotBlank()) {
                val fileName = url.substringBefore('?').substringAfterLast('/').ifBlank { "modello_custom" }
                val error = validateLitertlm(fileName)
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
        storageInfo = storageInfo(downloadedIds.size, occupiedBytes(container, customModels)),
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
        onResetSettings = {
            inferencePreferences.resetToDefaults()
            advanced = AdvancedSettingsUi(
                maxTokens = inferencePreferences.maxTokens.toString(),
                temperature = inferencePreferences.temperature,
                topK = inferencePreferences.topK.toString(),
                topP = inferencePreferences.topP,
            )
        },
        onClose = onClose,
    )
}

private fun occupiedBytes(container: AppContainer, customModels: List<DownloadableModel>): Long =
    (ModelCatalog.all + customModels)
        .map { container.modelPreferences.fileFor(it) }
        .filter { it.exists() }
        .sumOf { it.length() }

private fun validateLitertlm(fileName: String): String? =
    if (!fileName.endsWith(LITERTLM_EXTENSION, ignoreCase = true)) {
        "\"$fileName\" non è un modello LiteRT-LM: serve un file con estensione " +
            "$LITERTLM_EXTENSION, l'unico formato che questo motore sa caricare."
    } else {
        null
    }

private fun slugFor(fileName: String): String =
    fileName.substringBeforeLast('.').lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')

// Dal link incollato costruisce un modello "su misura": il nome del file
// è l'ultimo pezzo del percorso (come fa Hugging Face per il download
// diretto), la dimensione resta ignota finché il download non la scopre
// da sé (stesso trattamento già in uso per GEMMA_3N_E4B_GATED).
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
        validateLitertlm(originalName)?.let { throw IllegalArgumentException(it) }

        val model = DownloadableModel(
            id = "custom-${slugFor(originalName)}",
            displayName = name.ifBlank { originalName },
            url = "",
            fileName = originalName,
            sizeBytes = 0L,
            requiresToken = false,
            note = "Modello personalizzato, importato da un file sul telefono.",
            custom = true,
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
