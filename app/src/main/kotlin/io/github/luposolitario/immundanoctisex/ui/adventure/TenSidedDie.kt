package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.graphicsLayer
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
// Animazione "a scatti" in stile 3D (24/07/2026, disegnata da Michele —
// icona `ic_d10` a facce ombreggiate + una prova standalone in
// `origina_res/algoritmo_di_roll_e_ui_kotlin_compose.kt`): sostituisce
// la semplice rotazione 2D di un cerchio colorato con un `graphicsLayer`
// che ruota di 3 giri completi (1080°) e "pompa" fino al 130% a metà
// corsa, mentre le facce si susseguono a passi via via più lenti (effetto
// attrito) invece che a intervallo costante. Il contratto verso chi
// chiama resta identico (onRoll/onTap/initialFace), solo l'aspetto e il
// timing sono cambiati.
//
// `onRoll` è chiamato SOLO a fine animazione, mai al tocco: se il
// chiamante muta lo stato di gioco dentro `onRoll` (come fa il
// combattimento, sincrono), quella mutazione deve arrivare dopo che il
// dado ha smesso di girare — altrimenti i valori a schermo cambiano
// mentre il dado sta ancora "decidendo", rovinando l'effetto.
//
// `initialFace` (Michele 22/07/2026: "dopo che premo il pulsante il
// numero scompare"): nel combattimento questo componente vive dentro
// un `key(combatTick)` che lo ricrea da zero ad ogni round (serve per
// far aggiornare RES/CS, letti da una CombatSession non osservata da
// Compose) — la ricreazione azzerava anche `face`, che è memoria
// LOCALE di questo composable. Seminare `face` dall'ultimo tiro noto
// (tenuto invece in AdventureState.lastRound, che sopravvive alla
// ricreazione) fa sì che il numero resti a schermo invece di sparire.
@Composable
fun TenSidedDie(onRoll: () -> Int?, onTap: () -> Unit = {}, initialFace: Int? = null) {
    var rolling by remember { mutableStateOf(false) }
    var face by remember { mutableStateOf(initialFace) }
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    val rotationZ by animateFloatAsState(
        targetValue = if (rolling) 1080f else 0f,
        animationSpec = tween(durationMillis = ROLL_DURATION_MS, easing = FastOutSlowInEasing),
        label = "dice_roll_rotation",
    )
    // keyframes: parte da 1f, tocca 1.3f a metà corsa, torna a 1f
    // (il target) alla fine — l'effetto "pop" del lancio.
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = keyframes {
            durationMillis = ROLL_DURATION_MS
            1.3f at ROLL_DURATION_MS / 2
        },
        label = "dice_roll_scale",
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer {
                this.rotationZ = rotationZ
                this.scaleX = scale
                this.scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !rolling,
            ) {
                onTap()
                scope.launch {
                    rolling = true
                    // Passi crescenti invece di intervallo costante
                    // (Michele: rallenta "come per attrito" verso la fine).
                    for (step in 1..ROLL_STEPS) {
                        face = Random.nextInt(0, 10)
                        delay(ROLL_STEP_BASE_MS + step * ROLL_STEP_GROWTH_MS)
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
                modifier = Modifier.size(48.dp).clip(CircleShape),
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_d10),
                contentDescription = "Dado a 10 facce",
                modifier = Modifier.size(64.dp),
            )
            Text(
                face?.toString() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// 20 passi con ritardo crescente invece di 8 a intervallo fisso —
// somma ~1,4s, sincronizzata con la rotazione/scala sopra (1,2s).
private const val ROLL_STEPS = 20
private const val ROLL_STEP_BASE_MS = 50L
private const val ROLL_STEP_GROWTH_MS = 2L
private const val ROLL_DURATION_MS = 1200

@Preview(showBackground = true, name = "Dado a 10 facce")
@Composable
private fun TenSidedDiePreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        TenSidedDie(onRoll = { Random.nextInt(0, 10) })
    }
}
