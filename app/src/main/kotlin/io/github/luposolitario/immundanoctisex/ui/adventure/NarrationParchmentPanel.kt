package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.util.ParchmentStyle
import io.github.luposolitario.immundanoctisex.util.inkColor

// Pannello di narrazione (24/07/2026, schizzo di Michele dopo aver
// bocciato la pila a tre fasce vista sul device — "non mi piace nulla,
// deve essere una cosa così"): la pergamena grande resta un'illustrazione
// DECORATIVA, senza doversi allineare pixel-per-pixel al testo — bordo
// strappato e scudi degli angoli a piena immagine, `ContentScale.Crop`
// sull'intero riquadro. Il testo vive in un riquadro PIÙ PICCOLO e
// SEPARATO, con margine dalla cornice, sfondo = la stessa texture piatta
// del centro (senza denti strappati: qui non serve nessun trucco da
// nine-patch, il riquadro ha un'altezza fissa, non deve inseguire la
// lunghezza del testo) e un bordo vero che lo separa dalla pergamena.
// Lo scroll torna INTERNO al riquadro (Michele, stessa richiesta): le
// scelte sotto restano sempre visibili, non serve più scorrere l'intera
// schermata.
@Composable
fun NarrationParchmentPanel(
    style: ParchmentStyle,
    // Serve solo al colore del bordo (24/07/2026, Michele, dalla foto
    // con il rettangolo arancione disegnato sopra: "un bordo che in
    // notturna può essere oro e in tema chiaro argento" — segue il
    // TEMA dell'app, non lo stile pergamena scelto: chi ha "Pergamena
    // chiara" col telefono in tema scuro vuole comunque il bordo oro).
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val fullRes = style.fullRes
    val middleRes = style.middleRes
    if (fullRes == null || middleRes == null) {
        Card(modifier = modifier, elevation = CardDefaults.cardElevation(2.dp)) {
            content()
        }
        return
    }
    val borderBrush = if (isDarkTheme) goldBorderBrush() else silverBorderBrush()
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = fullRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Margine PROPORZIONALE (fillMaxSize(fraction), non un padding
        // fisso in dp): gli scudi degli angoli occupano fino al ~19-22%
        // dell'immagine dagli angoli (misurato con Pillow) — un padding
        // fisso lasciava il testo a coprirli su schermi diversi da
        // quello di prova. Al 68% il riquadro resta ben dentro, scudi e
        // strappo restano visibili e non coperti dal testo (Michele,
        // foto col rettangolo arancione: "verifica che i loghi... non
        // siano sovrascritte da testo").
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.68f)
                .border(5.dp, borderBrush, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)),
        ) {
            Image(
                painter = painterResource(id = middleRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            CompositionLocalProvider(LocalContentColor provides style.inkColor()) {
                content()
            }
        }
    }
}

// Oro in tema scuro, argento in tema chiaro (24/07/2026, richiesta
// esplicita di Michele) — non l'inchiostro (che segue lo stile
// pergamena scelto, non il tema): un elemento di cornice a parte.
// Gradiente, non colore piatto (24/07/2026, stesso giorno: "potrebbe
// usare una texture?" — una texture vera su un bordo di pochi dp non
// si vedrebbe, un gradiente diagonale chiaro/scuro/chiaro imita il
// riflesso della luce su un bordo metallico, molto più convincente di
// una linea piatta). Colori PIENI, non pastello (richiesta esplicita:
// "usa colori solidi").
private fun goldBorderBrush() = Brush.linearGradient(
    listOf(Color(0xFFFFE9A8), Color(0xFFB8860B), Color(0xFFFFD700), Color(0xFF8B6508), Color(0xFFFFE9A8)),
)

private fun silverBorderBrush() = Brush.linearGradient(
    listOf(Color(0xFFF2F2F2), Color(0xFF8A8A8A), Color(0xFFD9D9D9), Color(0xFF6E6E6E), Color(0xFFF2F2F2)),
)
