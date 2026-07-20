package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.core.data.model.EndingOutcome
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// L'insegna dell'esito, isolata dalla EndingZone SOLO per poterla vedere
// in @Preview: la zona vera dipende da AdventureState, che in preview non
// si costruisce. Così i due disegni si guardano senza device.
//
// Le illustrazioni sono TAVOLE A CHINA fornite da Michele (20/07/2026),
// ritagliate da un'unica immagine. Fondo bianco e cornice fanno parte
// del disegno e si tengono: è l'estetica dei libri di Lupo Solitario,
// una tavola stampata dentro la pagina.
@androidx.annotation.DrawableRes
private fun endingImageRes(outcome: EndingOutcome): Int? = when (outcome) {
    EndingOutcome.VICTORY -> R.drawable.ending_victory
    EndingOutcome.DEFEAT -> R.drawable.ending_defeat
    EndingOutcome.NEUTRAL -> null
}

@androidx.annotation.StringRes
private fun endingTitleRes(outcome: EndingOutcome): Int = when (outcome) {
    EndingOutcome.VICTORY -> R.string.ending_victory_title
    EndingOutcome.DEFEAT -> R.string.ending_defeat_title
    EndingOutcome.NEUTRAL -> R.string.ending_neutral_title
}

@Composable
fun EndingBadge(outcome: EndingOutcome, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        endingImageRes(outcome)?.let { image ->
            Image(
                painter = painterResource(id = image),
                contentDescription = null,
                // Altezza contenuta: la zona del finale ospita anche i
                // pulsanti (ricarica checkpoint, Torna alla Home) e su uno
                // schermo stretto un'illustrazione grande li spinge fuori.
                contentScale = ContentScale.Fit,
                modifier = Modifier.heightIn(max = 150.dp),
            )
        }
        Text(
            text = stringResource(endingTitleRes(outcome)),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = when (outcome) {
                EndingOutcome.VICTORY -> Color(0xFFFFD700)
                EndingOutcome.DEFEAT -> MaterialTheme.colorScheme.error
                EndingOutcome.NEUTRAL -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Preview(showBackground = true, name = "Vittoria")
@Composable
private fun EndingVictoryPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        EndingBadge(EndingOutcome.VICTORY, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true, name = "Sconfitta")
@Composable
private fun EndingDefeatPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        EndingBadge(EndingOutcome.DEFEAT, modifier = Modifier.padding(16.dp))
    }
}

// Senza esito dichiarato: nessuna tavola, ma l'avventura dice comunque
// che è finita.
@Preview(showBackground = true, name = "Neutro")
@Composable
private fun EndingNeutralPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        EndingBadge(EndingOutcome.NEUTRAL, modifier = Modifier.padding(16.dp))
    }
}
