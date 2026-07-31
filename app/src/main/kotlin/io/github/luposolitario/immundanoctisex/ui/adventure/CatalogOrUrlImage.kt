package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.annotation.DrawableRes
import coil.compose.AsyncImage
import io.github.luposolitario.immundanoctisex.core.data.model.ImageReference

// Unico punto in cui un url: (vedi ImageReference) diventa davvero
// un'immagine caricata in rete (29/07/2026, Michele: libri di uso
// personale che linkano l'illustrazione originale invece di crearne una
// nuova per il catalogo static:). static: continua a passare per
// painterResource come sempre; un link che fallisce a caricare non fa
// crashare nulla, semplicemente Coil non mostra niente al posto suo,
// come un ID sconosciuto nel catalogo.
//
// Immagini ANIMATE (31/07/2026, doc/UPGRADE.md §7): un drawable statico il
// cui NOME finisce per "_anim" (es. loc_tavern_anim.webp) passa da Coil
// invece che painterResource, per beneficiare dello stesso ImageLoader
// globale con ImageDecoderDecoder (AnimatedImageLoader.kt) che già anima
// il ramo url: sopra. Convenzione sul nome invece di un parametro nuovo
// (Michele, scelta esplicita): AsyncImage passa SEMPRE dalla pipeline
// asincrona di Coil anche per una risorsa locale — instradarci solo le
// immagini animate evita un flicker di caricamento su tutto il resto del
// catalogo esistente (mai animato, mostrato oggi in modo istantaneo).
@Composable
fun CatalogOrUrlImage(
    name: String?,
    @DrawableRes staticRes: Int?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val reference = name?.let(ImageReference::parse)
    val context = LocalContext.current
    val isAnimatedStatic = staticRes != null &&
        remember(staticRes) { context.resources.getResourceEntryName(staticRes).endsWith("_anim") }
    when {
        reference is ImageReference.Url -> AsyncImage(
            model = reference.url,
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier,
        )
        staticRes != null && isAnimatedStatic -> AsyncImage(
            model = staticRes,
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier,
        )
        staticRes != null -> Image(
            painter = painterResource(id = staticRes),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier,
        )
        else -> Unit
    }
}
