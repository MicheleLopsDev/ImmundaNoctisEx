package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
@androidx.annotation.DrawableRes
private fun endingImageRes(outcome: EndingOutcome): Int? = when (outcome) {
    EndingOutcome.VICTORY -> R.drawable.ic_ending_victory
    EndingOutcome.DEFEAT -> R.drawable.ic_ending_defeat
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
                modifier = Modifier.size(88.dp),
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

@Preview(showBackground = true, name = "Esiti — scuro")
@Composable
private fun EndingBadgeDarkPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(16.dp)) {
            EndingBadge(EndingOutcome.VICTORY)
            EndingBadge(EndingOutcome.DEFEAT)
            EndingBadge(EndingOutcome.NEUTRAL)
        }
    }
}

// Il teschio deve reggere anche su fondo chiaro: è il motivo del contorno
// scuro nel vettoriale.
@Preview(showBackground = true, name = "Esiti — chiaro")
@Composable
private fun EndingBadgeLightPreview() {
    ImmundaNoctisTheme(darkTheme = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(16.dp)) {
            EndingBadge(EndingOutcome.VICTORY)
            EndingBadge(EndingOutcome.DEFEAT)
            EndingBadge(EndingOutcome.NEUTRAL)
        }
    }
}
