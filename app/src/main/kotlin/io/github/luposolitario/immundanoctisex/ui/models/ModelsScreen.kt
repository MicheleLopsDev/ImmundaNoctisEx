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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.engine.inference.DownloadableModel
import io.github.luposolitario.immundanoctisex.core.engine.inference.EngineType
import io.github.luposolitario.immundanoctisex.core.engine.inference.ModelCatalog
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctisex.ui.theme.ThemedBackground

// Verde per "Attiva" (27/07/2026): non collide col rosso di "Attivato"
// né col blu di MaterialTheme.colorScheme.primary già usato ovunque
// per i bottoni generici (es. "Scarica").
private val ACTIVATE_BUTTON_GREEN = Color(0xFF2E7D32)

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
    isDarkTheme: Boolean,
    models: List<DownloadableModel>,
    customModels: List<DownloadableModel>,
    // Build senza motore GGUF (31/07/2026, Michele: "senza guff tutto il
    // resto è inutile"): BuildConfig.NATIVE_LLAMA_AVAILABLE è fisso per
    // build, quindi arriva qui come parametro (non letto direttamente nel
    // composable) — permette anche di vedere ENTRAMBI gli stati nei due
    // @Preview sotto, senza dover ricompilare con buildLlama diverso.
    ggufAvailable: Boolean,
    selectedModelId: String,
    downloadedIds: Set<String>,
    token: String,
    downloadState: DownloadUiState,
    // A quale modello appartiene DAVVERO il downloadState (24/07/2026,
    // bug corretto — vedi ModelDownloadWorker/ModelsRoute): mai più
    // selectedModelId, che poteva essere un modello diverso da quello
    // che sta scaricando per davvero.
    runningModelId: String?,
    storageInfo: String?,
    advancedSettings: AdvancedSettingsUi,
    onSelectModel: (DownloadableModel) -> Unit,
    // Quale modello e' DAVVERO in uso nel motore ora (Michele 22/07/2026:
    // "un tasto per rendere attivo uno dei motori che scarico"), distinto
    // dalla sola selezione salvata: puo' scaricarne piu' di uno e passare
    // dall'uno all'altro con un tocco, anche a partita in corso.
    activeModelId: String?,
    isActivating: Boolean,
    activateError: String?,
    onActivate: (DownloadableModel) -> Unit,
    onDeactivate: () -> Unit,
    onTokenChange: (String) -> Unit,
    onDownload: (DownloadableModel) -> Unit,
    onCancel: () -> Unit,
    onDelete: (DownloadableModel) -> Unit,
    onAddCustomModel: (url: String, name: String, requiresToken: Boolean) -> Unit,
    onRemoveCustomModel: (DownloadableModel) -> Unit,
    addModelError: String?,
    isImportingFromStorage: Boolean,
    onPickFromStorage: (name: String) -> Unit,
    // Catalogo GGUF come file JSON scambiabile (28/07/2026, Michele: "un
    // file json che contiene i link... così possiamo creare dei file con
    // i vari modelli da provare"): esporta/importa la lista intera di
    // "Consigliati" (esclusi i due fissi Gemma 4 E4B/E2B), più un
    // ripristino ai quattro di fabbrica.
    catalogError: String?,
    isImportingCatalog: Boolean,
    onExportCatalog: () -> Unit,
    onImportCatalog: () -> Unit,
    onResetCatalog: () -> Unit,
    onMaxTokensChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onTemperatureCommit: () -> Unit,
    onTopKChange: (String) -> Unit,
    onTopPChange: (Float) -> Unit,
    onTopPCommit: () -> Unit,
    onAskImageInPromptChange: (Boolean) -> Unit,
    onTranslationModeChange: (Boolean) -> Unit,
    onEngineEnabledChange: (Boolean) -> Unit,
    onResetSettings: () -> Unit,
    onClose: () -> Unit,
) {
    ThemedBackground(isDarkTheme = isDarkTheme) {
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
            if (!ggufAvailable) {
                Text(
                    "Questa build include solo Gemma 4 (LiteRT-LM): il supporto per modelli " +
                        "GGUF personalizzati è disattivato.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Senza motore GGUF, mostrare solo i modelli LiteRT-LM (i due
            // fissi Gemma 4 E4B/E2B): il resto del catalogo/dei modelli
            // personalizzati non caricherebbe mai col motore giusto.
            val visibleModels = if (ggufAvailable) models else models.filter { it.engineType == EngineType.LITERT_LM }
            val visibleCustomModels = if (ggufAvailable) {
                customModels
            } else {
                customModels.filter { it.engineType == EngineType.LITERT_LM }
            }

            activateError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Un download in corso blocca il bottone "Scarica" su TUTTE le
            // altre card (24/07/2026 — vedi commento su runningModelId): senza
            // questo, un tocco su un'altra card CANCELLAVA quello in corso
            // (WorkManager REPLACE) invece di essere semplicemente ignorato.
            val anyDownloadRunning = downloadState is DownloadUiState.Running

            Text("Consigliati", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            visibleModels.forEach { model ->
                ModelCard(
                    model = model,
                    selected = model.id == selectedModelId,
                    active = model.id == activeModelId,
                    downloaded = model.id in downloadedIds,
                    isActivating = isActivating,
                    downloadState = downloadState.takeIf { model.id == runningModelId } ?: DownloadUiState.Idle,
                    downloadBlockedByOther = anyDownloadRunning && model.id != runningModelId,
                    onSelect = { onSelectModel(model) },
                    onActivate = { onActivate(model) },
                    onDeactivate = onDeactivate,
                    onDownload = { onDownload(model) },
                    onCancel = onCancel,
                    onDelete = { onDelete(model) },
                    onRemove = null,
                )
            }

            if (ggufAvailable) {
                CatalogManagementCard(
                    error = catalogError,
                    isImporting = isImportingCatalog,
                    onExport = onExportCatalog,
                    onImport = onImportCatalog,
                    onReset = onResetCatalog,
                )
            }

            if (visibleCustomModels.isNotEmpty()) {
                Text("I tuoi modelli", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                visibleCustomModels.forEach { model ->
                    ModelCard(
                        model = model,
                        selected = model.id == selectedModelId,
                        active = model.id == activeModelId,
                        downloaded = model.id in downloadedIds,
                        isActivating = isActivating,
                        downloadState = downloadState.takeIf { model.id == runningModelId } ?: DownloadUiState.Idle,
                        downloadBlockedByOther = anyDownloadRunning && model.id != runningModelId,
                        onSelect = { onSelectModel(model) },
                        onActivate = { onActivate(model) },
                        onDeactivate = onDeactivate,
                        onDownload = { onDownload(model) },
                        onCancel = onCancel,
                        onDelete = { onDelete(model) },
                        onRemove = { onRemoveCustomModel(model) },
                    )
                }
            }

            if (ggufAvailable) {
                AddCustomModelCard(
                    error = addModelError,
                    isImporting = isImportingFromStorage,
                    onAdd = onAddCustomModel,
                    onPickFromStorage = onPickFromStorage,
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
                onAskImageInPromptChange = onAskImageInPromptChange,
                onTranslationModeChange = onTranslationModeChange,
                onEngineEnabledChange = onEngineEnabledChange,
                onReset = onResetSettings,
            )

            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Chiudi") }
        }
    }
}

@Composable
private fun ModelCard(
    model: DownloadableModel,
    selected: Boolean,
    active: Boolean,
    downloaded: Boolean,
    isActivating: Boolean,
    downloadState: DownloadUiState,
    downloadBlockedByOther: Boolean,
    onSelect: () -> Unit,
    onActivate: () -> Unit,
    // Lo stesso bottone rosso spegne il motore (01/08/2026, Michele:
    // "deve essere possibile disattivare il motore premendo su quello
    // attivato"): prima era solo un'etichetta di stato disabilitata.
    onDeactivate: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onRemove: (() -> Unit)?,
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
            if (active) {
                Text(
                    "In uso ora",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

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
                        // Disabilitato mentre un ALTRO modello sta
                        // scaricando (24/07/2026): prima restava sempre
                        // attivo, e un tocco qui cancellava il download in
                        // corso invece di limitarsi a essere ignorato.
                        Button(onClick = onDownload, enabled = !downloadBlockedByOther) { Text("Scarica") }
                    } else {
                        // Cambia il motore a caldo (Michele 22/07/2026):
                        // scaricati più modelli, si passa dall'uno
                        // all'altro con un tocco, anche a partita in corso.
                        // Attivo = rosso pieno con scritta "Attivato"
                        // (24/07/2026, richiesta Michele: il grigio
                        // disabilitato di Material passava inosservato —
                        // disabledContainerColor/disabledContentColor
                        // espliciti, altrimenti Material spegne comunque
                        // il rosso con la sua trasparenza di default sui
                        // bottoni disabilitati). Non ancora attivo = verde
                        // pieno (27/07/2026, Michele: il blu di default era
                        // lo stesso di "Scarica" e di troppi altri bottoni,
                        // non si distingueva come azione pronta).
                        Button(
                            // Attivo -> lo stesso bottone spegne il motore
                            // (01/08/2026): il badge "In uso ora" sopra
                            // resta a dire QUALE modello è caricato, così
                            // il bottone può portare l'azione invece dello
                            // stato.
                            onClick = if (active) onDeactivate else onActivate,
                            enabled = !isActivating,
                            colors = if (active) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                    disabledContainerColor = MaterialTheme.colorScheme.error,
                                    disabledContentColor = MaterialTheme.colorScheme.onError,
                                )
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = ACTIVATE_BUTTON_GREEN,
                                    contentColor = Color.White,
                                    disabledContainerColor = ACTIVATE_BUTTON_GREEN,
                                    disabledContentColor = Color.White,
                                )
                            },
                        ) {
                            Text(
                                when {
                                    isActivating -> "Attivazione…"
                                    active -> "Disattiva"
                                    else -> "Attiva"
                                },
                            )
                        }
                        OutlinedButton(onClick = onDelete, enabled = !isActivating) { Text("Elimina") }
                    }
                    // Solo i modelli aggiunti da un link: i "Consigliati"
                    // restano sempre nella lista.
                    if (onRemove != null) {
                        TextButton(onClick = onRemove, enabled = !isActivating) { Text("Rimuovi dalla lista") }
                    }
                }
            }
        }
    }
}

// Catalogo "Consigliati" come file JSON scambiabile (28/07/2026,
// Michele: "un file json che contiene i link... così possiamo creare dei
// file con i vari modelli da provare"). I due modelli fissi (Gemma 4
// E4B/E2B) non sono in questo file: importare o ripristinare non li
// tocca mai.
@Composable
private fun CatalogManagementCard(
    error: String?,
    isImporting: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Catalogo modelli", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "I \"Consigliati\" sopra (esclusi Gemma 4 E4B/E2B) si possono esportare in un " +
                    "file, o sostituire importandone uno — comodo per creare set diversi di " +
                    "modelli da provare senza modificare l'app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onExport, enabled = !isImporting) { Text("Esporta") }
                OutlinedButton(onClick = onImport, enabled = !isImporting) {
                    Text(if (isImporting) "Importazione…" else "Importa")
                }
            }
            TextButton(onClick = onReset, enabled = !isImporting) { Text("Ripristina predefiniti") }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AddCustomModelCard(
    error: String?,
    isImporting: Boolean,
    onAdd: (url: String, name: String, requiresToken: Boolean) -> Unit,
    onPickFromStorage: (name: String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var requiresToken by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Aggiungi un modello", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Formato LiteRT-LM (estensione .litertlm) o GGUF (estensione .gguf, motore " +
                    "llama.cpp) — riconosciuto da solo dall'estensione. Altri formati " +
                    "(es. .task di MediaPipe) vengono rifiutati.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome (opzionale)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Da un link Hugging Face", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Link diretto (…resolve/main/…)") },
                singleLine = true,
                enabled = !isImporting,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Switch(checked = requiresToken, onCheckedChange = { requiresToken = it }, enabled = !isImporting)
                Text("Repository riservato (richiede token)", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    onAdd(url.trim(), name.trim(), requiresToken)
                    url = ""
                    requiresToken = false
                },
                enabled = url.isNotBlank() && !isImporting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Aggiungi e scarica") }

            Text("Da un file già sul telefono", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = { onPickFromStorage(name.trim()) },
                enabled = !isImporting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (isImporting) "Importazione…" else "Scegli file .litertlm o .gguf") }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
            isDarkTheme = true,
            models = ModelCatalog.all,
            customModels = emptyList(),
            ggufAvailable = true,
            selectedModelId = ModelCatalog.default.id,
            downloadedIds = emptySet(),
            token = "",
            downloadState = DownloadUiState.Running(downloaded = 1_200_000_000, total = 3_659_530_240),
            runningModelId = ModelCatalog.default.id,
            storageInfo = "Modelli sul telefono: 1 — 3,66 GB occupati",
            advancedSettings = AdvancedSettingsUi(
                maxTokens = "10240",
                temperature = 0.7f,
                topK = "40",
                topP = 0.9f,
                askImageInPrompt = false,
            ),
            onSelectModel = {},
            activeModelId = null,
            isActivating = false,
            activateError = null,
            onActivate = {},
            onDeactivate = {},
            onTokenChange = {},
            onDownload = {},
            onCancel = {},
            onDelete = {},
            onAddCustomModel = { _, _, _ -> },
            onRemoveCustomModel = {},
            addModelError = null,
            isImportingFromStorage = false,
            onPickFromStorage = {},
            catalogError = null,
            isImportingCatalog = false,
            onExportCatalog = {},
            onImportCatalog = {},
            onResetCatalog = {},
            onMaxTokensChange = {},
            onTemperatureChange = {},
            onTemperatureCommit = {},
            onTopKChange = {},
            onTopPChange = {},
            onTopPCommit = {},
            onAskImageInPromptChange = {},
            onTranslationModeChange = {},
            onEngineEnabledChange = {},
            onResetSettings = {},
            onClose = {},
        )
    }
}

// Build senza motore GGUF (31/07/2026, buildLlama=false): solo i due
// Gemma 4 fissi, niente Consigliati/gestione catalogo/modello personalizzato.
@Preview(showBackground = true, name = "Modelli — solo Gemma 4 (scuro)", heightDp = 700)
@Composable
private fun ModelsScreenSoloGemmaPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        ModelsScreen(
            isDarkTheme = true,
            models = ModelCatalog.all,
            customModels = emptyList(),
            ggufAvailable = false,
            selectedModelId = ModelCatalog.default.id,
            downloadedIds = emptySet(),
            token = "",
            downloadState = DownloadUiState.Idle,
            runningModelId = null,
            storageInfo = null,
            advancedSettings = AdvancedSettingsUi(
                maxTokens = "10240",
                temperature = 0.7f,
                topK = "40",
                topP = 0.9f,
                askImageInPrompt = false,
            ),
            onSelectModel = {},
            activeModelId = null,
            isActivating = false,
            activateError = null,
            onActivate = {},
            onDeactivate = {},
            onTokenChange = {},
            onDownload = {},
            onCancel = {},
            onDelete = {},
            onAddCustomModel = { _, _, _ -> },
            onRemoveCustomModel = {},
            addModelError = null,
            isImportingFromStorage = false,
            onPickFromStorage = {},
            catalogError = null,
            isImportingCatalog = false,
            onExportCatalog = {},
            onImportCatalog = {},
            onResetCatalog = {},
            onMaxTokensChange = {},
            onTemperatureChange = {},
            onTemperatureCommit = {},
            onTopKChange = {},
            onTopPChange = {},
            onTopPCommit = {},
            onAskImageInPromptChange = {},
            onTranslationModeChange = {},
            onEngineEnabledChange = {},
            onResetSettings = {},
            onClose = {},
        )
    }
}
