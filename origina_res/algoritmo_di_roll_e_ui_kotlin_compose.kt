package com.example.diceapp 

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun D10DiceRoller() {
    // Stati per tenere traccia del numero attuale e se il dado sta ruotando
    var currentFace by remember { mutableIntStateOf(10) }
    var isRolling by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Rimuove il ripple effect (l'ombra grigia del tocco) per un'esperienza più pulita
    val interactionSource = remember { MutableInteractionSource() }

    // Animazione di rotazione: 3 giri completi (1080 gradi) se sta rollando
    val rotationZ by animateFloatAsState(
        targetValue = if (isRolling) 1080f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "dice_roll_rotation"
    )

    // Animazione di scala: il dado si ingrandisce (effetto pop) a metà del roll
    val scale by animateFloatAsState(
        targetValue = if (isRolling) 1f else 1f,
        animationSpec = keyframes {
            durationMillis = 1200
            1.3f at 600 // Raggiunge il 130% della dimensione a 600ms
        },
        label = "dice_roll_scale"
    )

    fun rollDice() {
        if (isRolling) return // Previene lanci multipli mentre è già in esecuzione
        isRolling = true
        
        coroutineScope.launch {
            // Ciclo per simulare il rapido cambio di facce durante il roll
            for (i in 1..20) {
                // Genera un numero casuale da 1 a 10
                currentFace = (1..10).random()
                // Attesa progressivamente più lunga per simulare l'attrito (il dado rallenta)
                delay(50L + (i * 2)) 
            }
            // Assicura che l'animazione sia considerata conclusa
            isRolling = false 
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        // Centra verticalmente il tutto
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center 
    ) {
        // Box sovrappone gli elementi: il testo andrà sopra l'immagine del dado
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null, // Disabilita l'effetto click di default
                    onClick = { rollDice() }
                )
                // Applica le animazioni di scala e rotazione all'intero Box
                .graphicsLayer {
                    this.rotationZ = rotationZ
                    this.scaleX = scale
                    this.scaleY = scale
                }
        ) {
            Image(
                // Assicurati che 'ic_d10' corrisponda al nome del file XML creato
                painter = painterResource(id = R.drawable.ic_d10), 
                contentDescription = "Dado a 10 facce",
                modifier = Modifier.size(160.dp)
            )
            
            Text(
                text = currentFace.toString(),
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                // Spostiamo leggermente il testo in alto per centrarlo perfettamente nel "Kite"
                modifier = Modifier.offset(y = (-8).dp) 
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = if (isRolling) "Rolling..." else "Tocca il dado per lanciare!",
            fontSize = 18.sp,
            color = Color.Gray
        )
    }
}