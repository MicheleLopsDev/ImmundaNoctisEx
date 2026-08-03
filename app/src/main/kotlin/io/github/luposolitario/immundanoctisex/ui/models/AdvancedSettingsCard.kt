package io.github.luposolitario.immundanoctisex.ui.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.luposolitario.immundanoctisex.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.engine.inference.InferenceConfig
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// I valori mostrati e modificati dalla card (stato sollevato: la card
// resta stateless e previewabile).
data class AdvancedSettingsUi(
    val maxTokens: String,
    val temperature: Float,
    val topK: String,
    val topP: Float,
    val askImageInPrompt: Boolean,
    val translationMode: Boolean = false,
    val engineEnabled: Boolean = true,
)

// "Impostazioni avanzate" ereditate da ModelActivity di v1, incluse le
// DESCRIZIONI ONESTE con l'impatto dichiarato su CPU e memoria: è il
// pezzo migliore di quella schermata e si conserva tale e quale.
// Differenza da v1: lì serviva riavviare la partita, qui no — si apre
// una sessione nuova a ogni scena, quindi vale dalla prossima.
@Composable
fun AdvancedSettingsCard(
    settings: AdvancedSettingsUi,
    onMaxTokensChange: (String) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onTemperatureCommit: () -> Unit,
    onTopKChange: (String) -> Unit,
    onTopPChange: (Float) -> Unit,
    onTopPCommit: () -> Unit,
    onAskImageInPromptChange: (Boolean) -> Unit,
    onTranslationModeChange: (Boolean) -> Unit,
    onEngineEnabledChange: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.advanced_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.advanced_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Interruttore generale del motore (01/08/2026, Michele:
            // "dovresti darmi la possibilità di disattivare il modello") —
            // in cima a tutta la card, governa anche l'avvio automatico
            // all'apertura dell'app: spento, resta spento finché non lo
            // riaccendi (anche chiudendo e riaprendo l'app), niente
            // caricamenti automatici né richieste in avventura.
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    SettingLabel(
                        title = stringResource(R.string.advanced_engine_on),
                        explanation = stringResource(R.string.advanced_engine_on_text),
                    )
                }
                Switch(checked = settings.engineEnabled, onCheckedChange = onEngineEnabledChange)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()

            // Modalità Traduzione (31/07/2026, Michele): in cima alla card,
            // così è chiaro che governa i controlli sotto — quando attiva
            // Gemma si limita a tradurre (nessun arricchimento) e i
            // parametri sotto passano a un preset fisso, non più modificabili.
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    SettingLabel(
                        title = stringResource(R.string.advanced_translation_mode),
                        explanation = stringResource(R.string.advanced_translation_mode_text),
                    )
                }
                Switch(checked = settings.translationMode, onCheckedChange = onTranslationModeChange)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()

            Spacer(Modifier.height(12.dp))
            SettingLabel(
                title = stringResource(R.string.advanced_max_tokens),
                explanation = stringResource(R.string.advanced_max_tokens_text),
            )
            OutlinedTextField(
                value = settings.maxTokens,
                onValueChange = onMaxTokensChange,
                enabled = !settings.translationMode,
                label = { Text(stringResource(R.string.advanced_max_tokens)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            SettingLabel(
                title = stringResource(R.string.advanced_temperature),
                explanation = stringResource(R.string.advanced_temperature_text),
            )
            Slider(
                value = settings.temperature,
                onValueChange = onTemperatureChange,
                onValueChangeFinished = onTemperatureCommit,
                enabled = !settings.translationMode,
                valueRange = 0f..1f,
            )
            ValueLabel("%.2f".format(settings.temperature))

            Spacer(Modifier.height(12.dp))
            SettingLabel(
                title = stringResource(R.string.advanced_top_p),
                explanation = stringResource(R.string.advanced_top_p_text),
            )
            Slider(
                value = settings.topP,
                onValueChange = onTopPChange,
                onValueChangeFinished = onTopPCommit,
                enabled = !settings.translationMode,
                valueRange = 0f..1f,
            )
            ValueLabel("%.2f".format(settings.topP))

            Spacer(Modifier.height(12.dp))
            SettingLabel(
                title = stringResource(R.string.advanced_top_k),
                explanation = stringResource(R.string.advanced_top_k_text),
            )
            OutlinedTextField(
                value = settings.topK,
                onValueChange = onTopKChange,
                enabled = !settings.translationMode,
                label = { Text(stringResource(R.string.advanced_top_k)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    SettingLabel(
                        title = stringResource(R.string.advanced_model_picks_background),
                        explanation = stringResource(R.string.advanced_model_picks_background_text),
                    )
                }
                Switch(checked = settings.askImageInPrompt, onCheckedChange = onAskImageInPromptChange)
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.advanced_reset))
            }
        }
    }
}

@Composable
private fun SettingLabel(title: String, explanation: String) {
    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    Text(
        explanation,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ValueLabel(value: String) {
    Text(
        value,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Preview(showBackground = true, name = "Impostazioni avanzate (scuro)", heightDp = 800)
@Composable
private fun AdvancedSettingsPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        AdvancedSettingsCard(
            settings = AdvancedSettingsUi(
                maxTokens = InferenceConfig.DEFAULT_MAX_TOKENS.toString(),
                temperature = InferenceConfig.DEFAULT_TEMPERATURE,
                topK = InferenceConfig.DEFAULT_TOP_K.toString(),
                topP = InferenceConfig.DEFAULT_TOP_P,
                askImageInPrompt = false,
                translationMode = false,
                engineEnabled = true,
            ),
            onMaxTokensChange = {},
            onTemperatureChange = {},
            onTemperatureCommit = {},
            onTopKChange = {},
            onTopPChange = {},
            onTopPCommit = {},
            onAskImageInPromptChange = {},
            onTranslationModeChange = {},
            onEngineEnabledChange = {},
            onReset = {},
        )
    }
}

@Preview(showBackground = true, name = "Impostazioni avanzate — Modalità Traduzione (scuro)", heightDp = 800)
@Composable
private fun AdvancedSettingsTranslationModePreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        AdvancedSettingsCard(
            settings = AdvancedSettingsUi(
                maxTokens = InferenceConfig.DEFAULT_MAX_TOKENS.toString(),
                temperature = InferenceConfig.TRANSLATION_PRESET.temperature,
                topK = InferenceConfig.TRANSLATION_PRESET.topK.toString(),
                topP = InferenceConfig.TRANSLATION_PRESET.topP,
                askImageInPrompt = false,
                translationMode = true,
                engineEnabled = true,
            ),
            onMaxTokensChange = {},
            onTemperatureChange = {},
            onTemperatureCommit = {},
            onTopKChange = {},
            onTopPChange = {},
            onTopPCommit = {},
            onAskImageInPromptChange = {},
            onTranslationModeChange = {},
            onEngineEnabledChange = {},
            onReset = {},
        )
    }
}

@Preview(showBackground = true, name = "Impostazioni avanzate — motore spento (scuro)", heightDp = 800)
@Composable
private fun AdvancedSettingsEngineDisabledPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        AdvancedSettingsCard(
            settings = AdvancedSettingsUi(
                maxTokens = InferenceConfig.DEFAULT_MAX_TOKENS.toString(),
                temperature = InferenceConfig.DEFAULT_TEMPERATURE,
                topK = InferenceConfig.DEFAULT_TOP_K.toString(),
                topP = InferenceConfig.DEFAULT_TOP_P,
                askImageInPrompt = false,
                translationMode = false,
                engineEnabled = false,
            ),
            onMaxTokensChange = {},
            onTemperatureChange = {},
            onTemperatureCommit = {},
            onTopKChange = {},
            onTopPChange = {},
            onTopPCommit = {},
            onAskImageInPromptChange = {},
            onTranslationModeChange = {},
            onEngineEnabledChange = {},
            onReset = {},
        )
    }
}
