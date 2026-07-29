package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
@Composable
fun CatalogOrUrlImage(
    name: String?,
    @DrawableRes staticRes: Int?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val reference = name?.let(ImageReference::parse)
    when {
        reference is ImageReference.Url -> AsyncImage(
            model = reference.url,
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
