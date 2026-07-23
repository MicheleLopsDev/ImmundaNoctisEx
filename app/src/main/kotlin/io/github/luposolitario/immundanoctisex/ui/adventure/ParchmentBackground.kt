package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import io.github.luposolitario.immundanoctisex.util.ParchmentStyle

// Sfondo pergamena a tre fasce (23/07/2026, Michele sul device: "il
// testo sfora... si potrebbe tagliare in due ed avere la parte
// centrale che si allunga"): alto e basso (strappo + scudi) a
// dimensione fissa, il centro si stira in verticale fino all'altezza
// vera del testo — stesso principio di un nine-patch Android, fatto
// con tre Image impilate. Estensione di BoxScope (non una funzione
// libera): `Modifier.matchParentSize()` esiste solo dentro un `Box`,
// va richiamata da lì. Condivisa da CombatDiaryPanel e AdventureScreen
// per non duplicare la pila due volte.
@Composable
fun BoxScope.ParchmentBackground(style: ParchmentStyle) {
    val top = style.topRes
    val middle = style.middleRes
    val bottom = style.bottomRes
    if (top == null || middle == null || bottom == null) return
    // Colore pieno PRIMA della pila di immagini: qualunque dente del
    // bordo strappato, per quanto profondo, mostra questo colore invece
    // del nero del tema sotto (vedi ParchmentStyle.baseColor).
    style.baseColor?.let { color ->
        Spacer(modifier = Modifier.matchParentSize().background(color))
    }
    Column(modifier = Modifier.matchParentSize()) {
        Image(
            painter = painterResource(id = top),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )
        Image(
            painter = painterResource(id = middle),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        Image(
            painter = painterResource(id = bottom),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
