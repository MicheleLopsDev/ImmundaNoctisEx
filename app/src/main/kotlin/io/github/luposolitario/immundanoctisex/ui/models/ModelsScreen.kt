package io.github.luposolitario.immundanoctisex.ui.models

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.model.DownloadableModel
import io.github.luposolitario.immundanoctisex.model.ModelCatalog
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// Stato osservabile del download, mostrato dalla schermata.
sealed interface DownloadUiState {
    data object Idle : DownloadUiState
    data class Running(val downloaded: Long, val total: Long) : DownloadUiState
    data object Done : DownloadUiState
    data class Failed(val message: String) : DownloadUiState
}

// Schermata "Modelli LLM" (UI.md §schermata 1): scelta del modello,
// download con progresso, token Hugging Face per i repo riservati.
// Stateless: dati in ingresso, eventi in uscita.
@Composable
fun ModelsScreen(
    models: List<DownloadableModel>,
    selectedModelId: String,
    downloadedIds: Set<String>,
    token: String,
    downloadState: DownloadUiState,
    storageInfo: String?,
    advancedSettings: AdvancedSettingsUi,
    onSelectModel: (DownloadableModel) -> Unit,
    onTokenChange: (String) -> Unit,
    onDownload: (DownloadableModel) -> Unit,
    onCancel: () -> Unit,
    onDelete: (DownloadableModel) -> Unit,
    onMaxTokensChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onTemperatureCommit: () -> Unit,
    onTopKChange: (String) -> Unit,
    onTopPChange: (Float) -> Unit,
    onTopPCommit: () -> Unit,
    onResetSettings: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Modelli LLM", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Il modello gira sul telefono: nessun testo esce da qui. " +
                "Serve una connessione solo per scaricarlo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        models.forEach { model ->
            ModelCard(
                model = model,
                selected = model.id == selectedModelId,
                downloaded = model.id in downloadedIds,
                downloadState = downloadState.takeIf { model.id == selectedModelId } ?: DownloadUiState.Idle,
                onSelect = { onSelectModel(model) },
                onDownload = { onDownload(model) },
                onCancel = onCancel,
                onDelete = { onDelete(model) },
            )
        }

        // Con file da GB, sapere quanto stai occupando è informazione
        // dovuta (in v1 il percorso si vedeva solo per le scene).
        storageInfo?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        TokenCard(token = token, onTokenChange = onTokenChange)

        AdvancedSettingsCard(
            settings = advancedSettings,
            onMaxTokensChange = onMaxTokensChange,
            onTemperatureChange = onTemperatureChange,
            onTemperatureCommit = onTemperatureCommit,
            onTopKChange = onTopKChange,
            onTopPChange = onTopPChange,
            onTopPCommit = onTopPCommit,
            onReset = onResetSettings,
        )

        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Chiudi") }
    }
}

@Composable
private fun ModelCard(
    model: DownloadableModel,
    selected: Boolean,
    downloaded: Boolean,
    downloadState: DownloadUiState,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    OutlinedCard(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(model.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(model.note, style = MaterialTheme.typography.bodySmall)
            Text(
                buildString {
                    if (model.sizeBytes > 0) append("%.2f GB".format(model.sizeGigabytes)) else append("dimensione ignota")
                    if (model.requiresToken) append(" — richiede token")
                    if (downloaded) append(" — già scaricato")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (downloadState) {
                is DownloadUiState.Running -> {
                    val percent = if (downloadState.total > 0) {
                        (downloadState.downloaded * 100 / downloadState.total).toInt()
                    } else {
                        0
                    }
                    Text("Scaricamento… $percent%")
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = onCancel) { Text("Annulla (riprenderà da qui)") }
                }

                is DownloadUiState.Failed -> Text(
                    downloadState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )

                else -> Unit
            }

            if (downloadState !is DownloadUiState.Running) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!downloaded) {
                        Button(onClick = onDownload) { Text("Scarica") }
                    } else {
                        OutlinedButton(onClick = onDelete) { Text("Elimina") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TokenCard(token: String, onTokenChange: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Token Hugging Face", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Serve solo per i modelli su repository riservati. " +
                    "Resta su questo telefono e non viene mai mostrato nei log.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                label = { Text("hf_…") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(showBackground = true, name = "Modelli (scuro)", heightDp = 900)
@Composable
private fun ModelsScreenPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        ModelsScreen(
            models = ModelCatalog.all,
            selectedModelId = ModelCatalog.default.id,
            downloadedIds = emptySet(),
            token = "",
            downloadState = DownloadUiState.Running(downloaded = 1_200_000_000, total = 3_659_530_240),
            storageInfo = "Modelli sul telefono: 1 — 3,66 GB occupati",
            advancedSettings = AdvancedSettingsUi(
                maxTokens = "10240",
                temperature = 0.7f,
                topK = "40",
                topP = 0.9f,
            ),
            onSelectModel = {},
            onTokenChange = {},
            onDownload = {},
            onCancel = {},
            onDelete = {},
            onMaxTokensChange = {},
            onTemperatureChange = {},
            onTemperatureCommit = {},
            onTopKChange = {},
            onTopPChange = {},
            onTopPCommit = {},
            onResetSettings = {},
            onClose = {},
        )
    }
}
