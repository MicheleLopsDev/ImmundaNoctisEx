package io.github.luposolitario.immundanoctisex.image

import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder

// Immagini animate (31/07/2026, richiesta di terzi a Michele su libri-
// fumetto animati, doc/UPGRADE.md §7): senza questo decoder, Coil (già
// usato per le immagini url: dell'autore) mostra un GIF/WebP animato come
// un singolo fotogramma fisso, esattamente come painterResource oggi.
// ImageDecoderDecoder (non il vecchio GifDecoder) perché minSdk=34 copre
// già comodamente l'API 28+ richiesta, e decodifica sia GIF sia WebP
// animato con lo stesso decoder — un solo componente per entrambi i
// formati.
//
// Installato UNA VOLTA per processo, non nel costruttore di AppContainer:
// quel contenitore si ricrea a ogni rotazione schermo (`by lazy`
// sull'istanza Activity, vedi MainActivity.kt), un Coil.setImageLoader lì
// dentro ributterebbe via la cache immagini a ogni rotazione. Il guard
// idempotente rende innocuo richiamarlo più volte.
object AnimatedImageLoader {
    @Volatile
    private var installed = false

    fun installOnce(context: Context) {
        if (installed) return
        installed = true
        Coil.setImageLoader(
            ImageLoader.Builder(context.applicationContext)
                .components { add(ImageDecoderDecoder.Factory()) }
                .build(),
        )
    }
}
