package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.tool.etl.FormaDelGrafo
import io.github.luposolitario.immundanoctisex.tool.etl.SondeDiForma

// La striscia di sonde in cima alla mappa (06/08/2026, Michele:
// "vorrei che le informazioni statistiche venissero integrate
// nell'editor, così appena carico il file mi dice esattamente quelle
// informazioni... sarebbe bello che questa cosa venga messa in alto
// così sarebbe a occhio per l'editor vedere se la cosa va o non va").
//
// Sono gli stessi numeri del comando `forma` (doc/FORMA-DEI-GRAFI.md,
// 37 librogame misurati), ma qui non si legge: si guarda. Il colore
// dice la distanza dal bersaglio, il numero sotto dice quale.
//
// Il calcolo sta in `etl/SondeDiForma.kt`, senza Compose: qui c'e' solo
// il disegno.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SondeDiFormaBar(manifest: Manifest, modifier: Modifier = Modifier) {
    val misura = FormaDelGrafo.misura(manifest)

    // Sotto le venti scene le percentuali non vogliono dire niente (su
    // sei scene "il 17% riconverge" vuol dire "una scena"): si dice
    // perche' le sonde non ci sono, invece di mostrarne di false.
    if (misura.scene < FormaDelGrafo.SCENE_MINIME) {
        Text(
            "Forma: ${misura.scene} scene, troppo poche per misurarla " +
                "(da ${FormaDelGrafo.SCENE_MINIME} in su)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SondeDiForma.di(misura).forEach { sonda ->
            // Larghezza generosa e `maxLines = 1` sull'etichetta: con
            // 132.dp "cammino obbligato" e "puo' ancora vincere"
            // andavano a capo, i riquadri crescevano di un'altezza
            // diversa dagli altri e le barrette non erano piu' allineate
            // — che e' proprio quello che rende una fila di indicatori
            // leggibile a colpo d'occhio (06/08/2026, sul primo libro).
            Column(
                modifier = Modifier
                    .width(158.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // maxLines anche qui: "16 / 11-25" e' corto, ma su
                    // un libro con numeri a quattro cifre no.
                    Text(
                        sonda.valore,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = coloreSemaforico(sonda.salute),
                    )
                    Text(
                        "  / ${sonda.bersaglio}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    sonda.etichetta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // La barretta: la stessa informazione del colore, ma
                // leggibile anche da chi non distingue rosso e verde.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(sonda.salute.coerceAtLeast(0.02f))
                            .height(4.dp)
                            .background(coloreSemaforico(sonda.salute)),
                    )
                }
            }
        }
    }
}

// Rosso → giallo → verde, continuo e non a gradini (Michele: "passando
// da rosso giallo verde con percentuali varie"): due interpolazioni
// attaccate al giallo, che sta a meta'. I toni sono scuriti rispetto ai
// primari puri per restare leggibili sul tema chiaro.
private fun coloreSemaforico(salute: Float): Color {
    val rosso = Color(0xFFC62828)
    val giallo = Color(0xFFEF9A00)
    val verde = Color(0xFF2E7D32)
    return if (salute < 0.5f) {
        lerp(rosso, giallo, (salute * 2f).coerceIn(0f, 1f))
    } else {
        lerp(giallo, verde, ((salute - 0.5f) * 2f).coerceIn(0f, 1f))
    }
}
