package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// L'attesa RACCONTATA al posto della scritta ferma (UI.md §Flusso
// centrale, richiesta di Michele): è l'unico momento in cui il giocatore
// aspetta davvero qualche secondo senza nulla da leggere.
//
// Copre i DUE momenti, che durano molto diversamente:
// - il CARICAMENTO del modello, una volta sola per partita, parecchi
//   secondi: qui l'attesa va giustificata, altrimenti sembra un blocco;
// - la GENERAZIONE di ogni scena, breve.
//
// L'altra metà dell'animazione vive nel banner: l'alone d'oro che pulsa
// attorno al ritratto del narratore (vedi AdventureBanner).
@Composable
fun NarratorThinking(loadingModel: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = if (loadingModel) {
                "Il narratore apre il libro…"
            } else {
                "Il narratore scrive…"
            },
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        ThinkingDots()
    }
}

// Tre puntini che si accendono in sequenza: la stessa animazione per
// entrambe le attese, così il giocatore riconosce "sta lavorando" senza
// doverlo leggere.
@Composable
private fun ThinkingDots() {
    val transition = rememberInfiniteTransition(label = "puntini")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(DOT_COUNT) { index ->
            val alpha by transition.animateFloat(
                initialValue = MIN_ALPHA,
                targetValue = 1f,
                animationSpec = pulseSpec(index),
                label = "puntino$index",
            )
            Spacer(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
            )
        }
    }
}

// Ogni puntino parte sfasato di un terzo del ciclo: l'onda va da sinistra
// a destra invece di far lampeggiare i tre insieme.
private fun pulseSpec(index: Int): InfiniteRepeatableSpec<Float> = infiniteRepeatable(
    animation = tween(durationMillis = CYCLE_MS, easing = LinearEasing),
    repeatMode = RepeatMode.Reverse,
    initialStartOffset = StartOffset(offsetMillis = index * CYCLE_MS / DOT_COUNT),
)

private const val DOT_COUNT = 3
private const val CYCLE_MS = 600
private const val MIN_ALPHA = 0.2f

@Preview(showBackground = true, name = "Attesa — caricamento del modello")
@Composable
private fun NarratorThinkingLoadingPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        NarratorThinking(loadingModel = true)
    }
}

@Preview(showBackground = true, name = "Attesa — generazione della scena")
@Composable
private fun NarratorThinkingGeneratingPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        NarratorThinking(loadingModel = false)
    }
}
