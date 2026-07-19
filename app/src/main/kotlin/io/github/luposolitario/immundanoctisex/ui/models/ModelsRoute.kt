package io.github.luposolitario.immundanoctisex.ui.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import io.github.luposolitario.immundanoctisex.model.DownloadableModel
import io.github.luposolitario.immundanoctisex.model.ModelCatalog
import io.github.luposolitario.immundanoctisex.model.ModelDownloadWorker

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

    var selectedModelId by remember { mutableStateOf(preferences.selectedModelId) }
    var token by remember { mutableStateOf(preferences.huggingFaceToken.orEmpty()) }
    var downloadedIds by remember {
        mutableStateOf(ModelCatalog.all.filter { preferences.isDownloaded(it) }.map { it.id }.toSet())
    }

    val workInfos by workManager
        .getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.WORK_NAME)
        .collectAsState(initial = emptyList())

    val downloadState = workInfos.firstOrNull().toUiState()

    // A download finito la lista si aggiorna: il bottone diventa "Elimina".
    if (downloadState is DownloadUiState.Done) {
        downloadedIds = ModelCatalog.all.filter { preferences.isDownloaded(it) }.map { it.id }.toSet()
    }

    ModelsScreen(
        models = ModelCatalog.all,
        selectedModelId = selectedModelId,
        downloadedIds = downloadedIds,
        token = token,
        downloadState = downloadState,
        onSelectModel = { model ->
            selectedModelId = model.id
            preferences.selectedModelId = model.id
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
        },
        onClose = onClose,
    )
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
        // Sono GB: solo su rete non a consumo, salvo diversa scelta futura.
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
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
