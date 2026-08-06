package io.github.luposolitario.immundanoctisex.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctisex.ui.theme.ThemedBackground

// Il primo schermo che si vede (05/08/2026, richiesta di Michele).
//
// Non è una presentazione: è una risposta a due domande che chi apre
// l'app si fa subito e a cui prima nessuno rispondeva — «devo scaricare
// qualcosa per giocare?» e «a cosa serve quella voce Modelli LLM?».
//
// Il punto è di Michele: *"se il libro è scritto nella tua stessa lingua
// e non vuoi arricchimento la parte llm potrebbe non servire"*. Quando è
// così l'app lo dice, invece di far sembrare che manchi qualcosa: chi
// vuole solo leggere l'avventura parte e basta.
@Composable
fun BenvenutoScreen(
    isDarkTheme: Boolean,
    // Vero quando il libro caricato è già nella lingua del telefono: in
    // quel caso il modello non aggiunge nulla se non l'arricchimento.
    modelloSuperfluo: Boolean,
    onInizia: () -> Unit,
    onVaiAiModelli: () -> Unit,
) {
    ThemedBackground(isDarkTheme = isDarkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.hero_banner),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
            Text(
                stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.welcome_what_it_is),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            // Tre punti, non un muro di testo: cosa puoi fare adesso,
            // cosa aggiunge il modello, e che resta tutto sul telefono.
            Punto(Icons.Default.AutoStories, stringResource(R.string.welcome_play_now))
            Punto(
                Icons.Default.Bolt,
                stringResource(
                    if (modelloSuperfluo) R.string.welcome_model_not_needed else R.string.welcome_model_optional,
                ),
            )
            Punto(Icons.Default.CloudOff, stringResource(R.string.welcome_offline))

            Spacer(Modifier.height(8.dp))

            Button(onClick = onInizia, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.welcome_start))
            }
            OutlinedButton(onClick = onVaiAiModelli, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.welcome_go_to_models))
            }
            Text(
                stringResource(R.string.welcome_change_later),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Punto(icona: ImageVector, testo: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icona,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Text(testo, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Il caso in cui il modello NON serve: libro già nella lingua del
// telefono. È quello che vedrà la maggior parte di chi prova l'app.
@Preview(showBackground = true, name = "Benvenuto — modello superfluo", heightDp = 800)
@Composable
private fun BenvenutoModelloSuperfluoPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        BenvenutoScreen(isDarkTheme = true, modelloSuperfluo = true, onInizia = {}, onVaiAiModelli = {})
    }
}

@Preview(showBackground = true, name = "Benvenuto — modello utile", heightDp = 800)
@Composable
private fun BenvenutoModelloUtilePreview() {
    ImmundaNoctisTheme(darkTheme = false) {
        BenvenutoScreen(isDarkTheme = false, modelloSuperfluo = false, onInizia = {}, onVaiAiModelli = {})
    }
}
