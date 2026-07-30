package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.ui.geometry.Offset
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

// Dimensioni del nodo duplicate da MapScreen.kt (NODE_WIDTH/NODE_HEIGHT,
// in dp): qui servono come Float puri, java.awt.Graphics2D non conosce
// Dp — un solo riuso in pixel non vale l'astrazione di condividerle.
private const val NODE_W = 190f
private const val NODE_H = 72f
private const val MARGINE = 40f

// §19.8 (Michele: "esportare la mappa come immagine"): disegna l'INTERA
// mappa logica (tutti i nodi/archi, indipendente da pan/zoom/viewport
// della mappa a schermo) su un file PNG, riusando la stessa palette di
// colori del disegno interattivo (MapScreen.kt) — ma con
// java.awt.Graphics2D invece di Canvas Compose: qui non c'è una
// finestra su cui comporre, serve un rendering offscreen indipendente.
// Niente immagini di copertina dei nodi (§15.3): caricarle tutte in
// memoria per un'esportazione, spesso di centinaia di scene, è un
// costo che non vale il beneficio per una mappa pensata per
// l'orientamento, non per sostituire la lettura del libro. Le
// evidenziazioni legate al mouse (vicinato, percorso di ricerca) non
// hanno senso su un'immagine statica e restano fuori.
fun esportaMappaComeImmagine(
    file: File,
    nodi: List<GraphNode>,
    archi: List<GraphEdge>,
    scenesById: Map<String, Scene>,
    posizioni: Map<String, Offset>,
) {
    val posizioniValide = nodi.mapNotNull { nodo -> posizioni[nodo.sceneId]?.let { nodo.sceneId to it } }.toMap()
    if (posizioniValide.isEmpty()) return

    val minX = posizioniValide.values.minOf { it.x }
    val minY = posizioniValide.values.minOf { it.y }
    val maxX = posizioniValide.values.maxOf { it.x } + NODE_W
    val maxY = posizioniValide.values.maxOf { it.y } + NODE_H
    fun px(offset: Offset) = Offset(offset.x - minX + MARGINE, offset.y - minY + MARGINE)

    val larghezza = (maxX - minX + MARGINE * 2).toInt().coerceAtLeast(1)
    val altezza = (maxY - minY + MARGINE * 2).toInt().coerceAtLeast(1)

    val immagine = BufferedImage(larghezza, altezza, BufferedImage.TYPE_INT_ARGB)
    val g = immagine.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.color = Color.WHITE
    g.fillRect(0, 0, larghezza, altezza)

    // Archi sotto i nodi — stessa palette verde/rosso di risoluzione già
    // usata sulla mappa interattiva (§6.2).
    g.stroke = BasicStroke(2f)
    archi.forEach { arco ->
        val da = posizioniValide[arco.fromSceneId]?.let(::px) ?: return@forEach
        val a = posizioniValide[arco.toSceneId]?.let(::px) ?: return@forEach
        g.color = if (arco.resolved) Color(0x4C, 0xAF, 0x50) else Color(0xE5, 0x39, 0x35)
        g.drawLine(
            (da.x + NODE_W / 2).toInt(), (da.y + NODE_H / 2).toInt(),
            (a.x + NODE_W / 2).toInt(), (a.y + NODE_H / 2).toInt(),
        )
    }

    g.font = Font("SansSerif", Font.BOLD, 13)
    val fontEtichetta = Font("SansSerif", Font.ITALIC, 10)
    nodi.forEach { nodo ->
        val pos = posizioniValide[nodo.sceneId]?.let(::px) ?: return@forEach
        val scena = scenesById[nodo.sceneId]
        // Stesso criterio di priorità colore di MapScreen.kt: tipo di
        // scena prima, salute (riferimenti risolti) come fallback.
        val coloreTipo = when (scena?.sceneType) {
            SceneType.START -> Color(0xFC, 0xE4, 0xEC)
            SceneType.ENDING -> Color(0xFF, 0xF9, 0xC4)
            else -> null
        }
        g.color = coloreTipo ?: (if (nodo.healthy) Color(0xE8, 0xF5, 0xE9) else Color(0xFF, 0xEB, 0xEE))
        g.fillRoundRect(pos.x.toInt(), pos.y.toInt(), NODE_W.toInt(), NODE_H.toInt(), 12, 12)
        g.color = coloreTipo ?: Color.BLACK
        g.drawRoundRect(pos.x.toInt(), pos.y.toInt(), NODE_W.toInt(), NODE_H.toInt(), 12, 12)

        g.font = Font("SansSerif", Font.BOLD, 13)
        g.color = Color.BLACK
        g.drawString(nodo.sceneId, pos.x.toInt() + 8, pos.y.toInt() + 20)

        // §19.6/§19.7: stessi due avvisi della mappa interattiva, come
        // etichetta testuale invece che badge emoji — Graphics2D non
        // garantisce di avere un font con glifi emoji a colori
        // disponibile su ogni sistema, un'etichetta testuale è più
        // affidabile su un'immagine esportata.
        val isOrfana = nodo.level == Int.MAX_VALUE
        val isVicoloCieco = scena?.sceneType == SceneType.TRANSITION &&
            scena.choices.isEmpty() && scena.disciplineChoices.isEmpty() && scena.combat == null
        if (isOrfana || isVicoloCieco) {
            g.font = fontEtichetta
            g.color = Color(0x60, 0x60, 0x60)
            val etichetta = listOfNotNull(
                "orfana".takeIf { isOrfana },
                "vicolo cieco".takeIf { isVicoloCieco },
            ).joinToString(" · ")
            g.drawString(etichetta, pos.x.toInt() + 8, pos.y.toInt() + NODE_H.toInt() - 8)
        }
    }

    g.dispose()
    ImageIO.write(immagine, "png", file)
}
