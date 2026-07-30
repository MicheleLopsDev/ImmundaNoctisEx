package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.luposolitario.immundanoctisex.core.data.model.ImageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

// Anteprima grafica di un valore backgroundImage/npcImage/enemyImage
// (§15.3, Michele: "se ci sono immagini nella scena devono essere
// caricate e reindirizzate anche graficamente nella scheda della
// scena"). Due sorgenti possibili, stesso discriminante di
// ImageReference.parse già in uso dal client:
// - "static:<id>" -> immagine bundled nell'editor (StaticResourceCatalog,
//   §15.1), caricamento istantaneo, sempre offline.
// - "url:<link>" -> scaricata al volo con un timeout breve, su un thread
//   di sfondo — se fallisce o scade il tempo, degrado silenzioso su un
//   segnaposto, mai un blocco dell'interfaccia (stessa filosofia del
//   client: un'immagine mancante non è mai un errore).
// `mostraSegnaposto`: la scheda di scena vuole un riquadro sempre
// visibile (grigio con "…"/"nessuna anteprima"); il nodo della mappa
// (§15.3) preferisce restare trasparente finché non c'è un'immagine
// vera, per lasciar vedere il colore di salute sotto invece di un
// grigio piatto durante il caricamento o in caso di fallimento.
// `ingrandibile` (30/07/2026, Michele: "nel editor di scena se faccio
// click sul immagine si apre in un popup a dimensione intera"): click
// sull'anteprima per vederla grande — attivo SOLO nella scheda scena,
// non sul nodo della mappa (lì il click ha già un altro significato,
// aprire/spostare la scena, §15.5 e turni precedenti).
@Composable
fun AnteprimaImmagineRisorsa(
    valore: String,
    modifier: Modifier = Modifier,
    mostraSegnaposto: Boolean = true,
    ingrandibile: Boolean = false,
) {
    val riferimento = remember(valore) { ImageReference.parse(valore) }
    var bitmap by remember(valore) { mutableStateOf<ImageBitmap?>(null) }
    var caricamentoFallito by remember(valore) { mutableStateOf(false) }
    var ingrandita by remember(valore) { mutableStateOf(false) }

    LaunchedEffect(valore) {
        bitmap = null
        caricamentoFallito = false
        val url: URL? = when (val rif = riferimento) {
            is ImageReference.Static -> StaticResourceCatalog.percorsoImmagine(rif.catalogId)
            is ImageReference.Url -> runCatching { URL(rif.url) }.getOrNull()
            null -> null
        }
        if (url == null) {
            caricamentoFallito = true
            return@LaunchedEffect
        }
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val connessione = url.openConnection()
                connessione.connectTimeout = 3000
                connessione.readTimeout = 3000
                connessione.getInputStream().use { loadImageBitmap(it) }
            }.getOrNull()
        }
        if (bitmap == null) caricamentoFallito = true
    }

    val bitmapCorrente = bitmap
    Box(
        modifier = (if (mostraSegnaposto) modifier.background(Color(0xFFD8D8D8)) else modifier)
            .let { if (ingrandibile && bitmapCorrente != null) it.clickable { ingrandita = true } else it },
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmapCorrente != null -> Image(
                bitmap = bitmapCorrente,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            mostraSegnaposto && caricamentoFallito -> Text("nessuna anteprima", style = MaterialTheme.typography.labelSmall)
            mostraSegnaposto -> Text("…", style = MaterialTheme.typography.labelSmall)
        }
    }

    if (ingrandita && bitmapCorrente != null) {
        Dialog(
            onDismissRequest = { ingrandita = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { ingrandita = false },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = bitmapCorrente,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}
