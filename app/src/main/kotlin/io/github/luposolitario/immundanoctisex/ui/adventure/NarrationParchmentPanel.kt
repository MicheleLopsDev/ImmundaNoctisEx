package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = fullRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .padding(28.dp)
                .border(2.dp, FRAME_BORDER_COLOR, RoundedCornerShape(8.dp))
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

// Marrone cuoio scuro: un bordo unico per entrambe le varianti, come la
// rilegatura di un registro vero — non l'inchiostro (che cambia tra
// chiaro/scuro), un elemento di cornice a parte.
private val FRAME_BORDER_COLOR = Color(0xFF4A3524)
