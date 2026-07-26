package io.github.luposolitario.immundanoctisex.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import io.github.luposolitario.immundanoctisex.R

// Sfondo dell'app (26/07/2026, richiesta Michele: "puoi usare gli sfondi
// per tutte le finestre che abbiamo creato?" — prima solo la Home).
// Consegnato come un'unica immagine divisa a metà chiaro/scuro (con
// "NOCTIS SECRETUM" impresso nella metà scura): tagliata in due asset,
// uno per tema, scelto con lo stesso isDarkTheme già in uso per il
// toggle sole/luna. Non va sull'AdventureScreen (Fase 4): quella ha già
// i propri sfondi di scena (loc_*), sovrapporne un altro sarebbe rumore
// oltre che invisibile la maggior parte del tempo.
@Composable
fun ThemedBackground(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(
                id = if (isDarkTheme) R.drawable.home_background_dark else R.drawable.home_background_light,
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        content()
    }
}
