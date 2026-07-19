package io.github.luposolitario.immundanoctisex.ui.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.inference.InferenceConfig
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// I valori mostrati e modificati dalla card (stato sollevato: la card
// resta stateless e previewabile).
data class AdvancedSettingsUi(
    val maxTokens: String,
    val temperature: Float,
    val topK: String,
    val topP: Float,
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
    onReset: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Impostazioni avanzate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Cambiano il comportamento del modello. Hanno effetto dalla prossima scena.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))
            SettingLabel(
                title = "Token massimi",
                explanation = "Quanto può essere lungo il contesto di una scena. Valori alti " +
                    "permettono testi più lunghi ma consumano più memoria e tempo. " +
                    "Impatto su CPU/memoria: medio.",
            )
            OutlinedTextField(
                value = settings.maxTokens,
                onValueChange = onMaxTokensChange,
                label = { Text("Token massimi") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            SettingLabel(
                title = "Temperatura (creatività)",
                explanation = "Valori alti (0.9) rendono la prosa più creativa, valori bassi (0.2) " +
                    "più fedele e prevedibile. Impatto su CPU/memoria: nullo.",
            )
            Slider(
                value = settings.temperature,
                onValueChange = onTemperatureChange,
                onValueChangeFinished = onTemperatureCommit,
                valueRange = 0f..1f,
            )
            ValueLabel("%.2f".format(settings.temperature))

            Spacer(Modifier.height(12.dp))
            SettingLabel(
                title = "Top-P (campionamento nucleo)",
                explanation = "Un valore alto (0.95) considera più parole, uno basso è più " +
                    "restrittivo. Impatto su CPU/memoria: basso.",
            )
            Slider(
                value = settings.topP,
                onValueChange = onTopPChange,
                onValueChangeFinished = onTopPCommit,
                valueRange = 0f..1f,
            )
            ValueLabel("%.2f".format(settings.topP))

            Spacer(Modifier.height(12.dp))
            SettingLabel(
                title = "Top-K (campionamento vocabolario)",
                explanation = "Considera solo le K parole più probabili. Alto (50) dà più varietà, " +
                    "basso (10) è più prudente. Impatto su CPU/memoria: basso.",
            )
            OutlinedTextField(
                value = settings.topK,
                onValueChange = onTopKChange,
                label = { Text("Top-K") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onReset) {
                Text("Ripristina i valori consigliati")
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
            ),
            onMaxTokensChange = {},
            onTemperatureChange = {},
            onTemperatureCommit = {},
            onTopKChange = {},
            onTopPChange = {},
            onTopPCommit = {},
            onReset = {},
        )
    }
}
