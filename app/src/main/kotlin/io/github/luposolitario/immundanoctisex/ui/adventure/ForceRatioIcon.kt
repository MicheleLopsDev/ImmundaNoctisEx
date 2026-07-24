package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Icona 3D del Rapporto di Forza (24/07/2026, richiesta Michele: "lo
// rappresenti graficamente con un'immagine vettoriale disegnata in stile
// 3d da colorare... <  Hero più forte, = il rapporto è 0, > più forte il
// nemico... da 1 a 3 verde da 4 a 7 giallo da 8 a 9 o superiori rosso").
// I tre segni (< / = / >) arrivano da Michele come frammenti di path
// grezzi (origina_res/minore.xml, uguale.xml, maggiore.xml — tre facce
// per un effetto smussato/3D: superiore chiara, laterale d'ombra, bordo
// profondo scuro), tutti consegnati nello STESSO verde d'esempio: qui si
// ricolorano secondo la fascia, tenendo la stessa geometria e lo stesso
// schema tonale (chiaro/medio/scuro) per ogni colore.

enum class RatioTier(val top: Color, val side: Color, val edge: Color) {
    GREEN(Color(0xFF4CAF50), Color(0xFF2E7D32), Color(0xFF1B5E20)),
    YELLOW(Color(0xFFFFC107), Color(0xFFFF8F00), Color(0xFFFF6F00)),
    RED(Color(0xFFF44336), Color(0xFFC62828), Color(0xFFB71C1C)),
    ;

    companion object {
        // 0 non ha una fascia dichiarata da Michele: resta verde, è la
        // situazione meno rischiosa di tutte (Rapporto di Forza pari).
        fun forAbsRatio(absRatio: Int): RatioTier = when {
            absRatio <= 3 -> GREEN
            absRatio <= 7 -> YELLOW
            else -> RED
        }
    }
}

// Geometria esatta consegnata da Michele (path a tre facce, viewport
// 0-100 su entrambi gli assi) — SOLO la geometria viene riusata da qui,
// il colore arriva da RatioTier.
private val MINORE_PATHS = listOf(
    "M 80 15 L 25 50 L 80 85 L 85 80 L 35 50 L 85 20 Z", // faccia laterale (ombra)
    "M 25 50 L 25 55 L 80 90 L 80 85 Z", // bordo inferiore (spessore)
    "M 25 50 L 25 55 L 80 20 L 80 15 Z", // bordo superiore interno (spessore)
    "M 75 10 L 20 45 L 75 80 L 85 75 L 35 45 L 85 15 Z", // faccia principale (top)
)
private val MAGGIORE_PATHS = listOf(
    "M 20 15 L 75 50 L 20 85 L 15 80 L 65 50 L 15 20 Z",
    "M 75 50 L 75 55 L 20 90 L 20 85 Z",
    "M 75 50 L 75 55 L 20 20 L 20 15 Z",
    "M 25 10 L 80 45 L 25 80 L 15 75 L 65 45 L 15 15 Z",
)
private val UGUALE_PATHS = listOf(
    "M 15 25 L 85 25 L 85 45 L 15 45 Z",
    "M 15 40 L 85 40 L 85 45 L 15 45 Z",
    "M 10 20 L 80 20 L 80 40 L 10 40 Z",
    "M 15 65 L 85 65 L 85 85 L 15 85 Z",
    "M 15 80 L 85 80 L 85 85 L 15 85 Z",
    "M 10 60 L 80 60 L 80 80 L 10 80 Z",
)

// Ordine facce -> tono: le prime 3 (o 6 per "=") alternano ombra/bordo/top,
// stesso ordine di Michele nei file originali.
private fun tonesFor(pathCount: Int, tier: RatioTier): List<Color> =
    if (pathCount == 4) {
        listOf(tier.side, tier.edge, tier.edge, tier.top)
    } else {
        // uguale.xml: due barre, ognuna ombra/bordo/top.
        listOf(tier.side, tier.edge, tier.top, tier.side, tier.edge, tier.top)
    }

private fun buildIcon(name: String, paths: List<String>, tier: RatioTier): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 40.dp,
        defaultHeight = 40.dp,
        viewportWidth = 100f,
        viewportHeight = 100f,
    )
    val tones = tonesFor(paths.size, tier)
    paths.forEachIndexed { index, pathData ->
        builder.addPath(pathData = addPathNodes(pathData), fill = SolidColor(tones[index]))
    }
    return builder.build()
}

// I tre esiti del confronto (24/07/2026, mappatura esplicita di Michele):
// "< Hero più forte, = il rapporto è 0, > più forte il nemico" — non
// l'ordine matematico che ci si aspetterebbe da un normale confronto,
// ma quello che Michele ha chiesto parola per parola.
@Composable
fun ForceRatioIcon(ratio: Int, modifier: Modifier = Modifier) {
    val tier = RatioTier.forAbsRatio(kotlin.math.abs(ratio))
    // Segni invertiti rispetto al primo giro (24/07/2026, Michele sul
    // device: "sono invertiti i segni < >, inverti le condizioni") —
    // ora ">" (maggiore) è l'eroe più forte, "<" (minore) il nemico più
    // forte: la lettura matematica naturale (CS eroe > CS nemico).
    val icon = when {
        ratio > 0 -> remember(tier) { buildIcon("maggiore", MAGGIORE_PATHS, tier) }
        ratio < 0 -> remember(tier) { buildIcon("minore", MINORE_PATHS, tier) }
        else -> remember(tier) { buildIcon("uguale", UGUALE_PATHS, tier) }
    }
    Image(imageVector = icon, contentDescription = null, modifier = modifier)
}
