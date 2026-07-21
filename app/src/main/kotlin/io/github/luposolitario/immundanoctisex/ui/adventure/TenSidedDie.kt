package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Il dado a 10 facce ufficiale di Lupo Solitario: la faccia dello ZERO
// porta il simbolo del lupo al posto del numero (dettaglio del dado
// fisico originale). Componente autonomo, non solo per il combattimento:
// è candidato a diventare l'innesco del Dado del Destino generale che
// UI.md assegna a Fase 7 (skillCheck, randomChoiceTable, evasione).
//
// `onRoll` è chiamato SOLO a fine animazione, mai al tocco: se il
// chiamante muta lo stato di gioco dentro `onRoll` (come fa il
// combattimento, sincrono), quella mutazione deve arrivare dopo che il
// dado ha smesso di girare — altrimenti i valori a schermo cambiano
// mentre il dado sta ancora "decidendo", rovinando l'effetto.
@Composable
fun TenSidedDie(onRoll: () -> Int?, onTap: () -> Unit = {}) {
    var rolling by remember { mutableStateOf(false) }
    var face by remember { mutableStateOf<Int?>(null) }
    var spin by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(56.dp)
            .rotate(spin)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, MaterialTheme.colorScheme.tertiary, CircleShape)
            .clickable(enabled = !rolling) {
                onTap()
                scope.launch {
                    rolling = true
                    repeat(ROLL_STEPS) {
                        face = Random.nextInt(0, 10)
                        spin += 360f / ROLL_STEPS
                        delay(STEP_MS)
                    }
                    face = onRoll()
                    rolling = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (face == 0) {
            Image(
                painter = painterResource(id = R.drawable.lupo_solitario),
                contentDescription = "Zero",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
        } else {
            Text(
                face?.toString() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val ROLL_STEPS = 8
private const val STEP_MS = 70L

@Preview(showBackground = true, name = "Dado a 10 facce")
@Composable
private fun TenSidedDiePreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        TenSidedDie(onRoll = { Random.nextInt(0, 10) })
    }
}
