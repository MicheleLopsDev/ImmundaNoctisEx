package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.core.data.model.Gender
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// Il banner della scena teatrale (UI.md §Banner), ereditato da v1:
// sfondo a tutta larghezza con i ritratti circolari sovrapposti al bordo
// inferiore. Il NARRATORE è una presenza puramente visiva, non un
// personaggio nei dati.
//
// CERCHIO D'ORO SU CHI PARLA (convenzione di v1): sul narratore mentre
// scrive, sull'eroe quando tocca a lui agire.
@Composable
fun AdventureBanner(
    heroName: String,
    heroGender: Gender,
    narratorSpeaking: Boolean,
    modifier: Modifier = Modifier,
    // Il narratore sta pensando (carica il modello o genera la scena):
    // il suo alone d'oro pulsa finché non c'è qualcosa da leggere.
    narratorThinking: Boolean = false,
    // Nome dal catalogo (SceneImageCatalog) o null: sceneBackgroundRes
    // degrada sulla mappa di default per qualunque nome che non riconosce
    // (esperimento 20/07/2026 — il pacchetto decide se dichiarato,
    // altrimenti Gemma può suggerirlo con vocabolario chiuso).
    backgroundImageName: String? = null,
) {
    Box(modifier = modifier.fillMaxWidth().height(150.dp)) {
        Image(
            painter = painterResource(id = sceneBackgroundRes(backgroundImageName)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(110.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            PortraitBadge(
                imageRes = R.drawable.portrait_dm,
                label = "Narratore",
                speaking = narratorSpeaking,
                thinking = narratorThinking,
            )
            PortraitBadge(
                // 24/07/2026: sostituiti class_warrior_male/female
                // (fotorealistici, fuori stile) coi busti a china
                // ritagliati da hero.png — stesso file usato per il
                // ritratto in creazione personaggio.
                imageRes = if (heroGender == Gender.MALE) {
                    R.drawable.hero_portrait_male
                } else {
                    R.drawable.hero_portrait_female
                },
                label = heroName,
                speaking = !narratorSpeaking,
            )
        }
    }
}

@Composable
private fun PortraitBadge(
    imageRes: Int,
    label: String,
    speaking: Boolean,
    // Il narratore che PENSA: l'oro non è fermo, respira. È la metà
    // visiva dell'attesa raccontata (l'altra è NarratorThinking).
    thinking: Boolean = false,
) {
    // Oro su chi parla, grigio sugli altri: la stessa convenzione usata
    // per l'arma impugnata e i ritratti della creazione.
    val borderColor = if (speaking) Color(0xFFFFD700) else Color.DarkGray
    // Fuori dall'attesa il valore resta fisso: nessuna animazione accesa
    // a vuoto per tutta la scena.
    val transition = rememberInfiniteTransition(label = "alone")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "spessore",
    )
    val borderWidth = when {
        thinking -> pulse.dp
        speaking -> 3.dp
        else -> 2.dp
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(borderWidth, borderColor, CircleShape),
        )
        Text(
            text = label,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Preview(showBackground = true, name = "Banner — il narratore parla")
@Composable
private fun BannerNarratorPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        AdventureBanner(heroName = "Lupo Solitario", heroGender = Gender.MALE, narratorSpeaking = true)
    }
}

@Preview(showBackground = true, name = "Banner — il narratore pensa")
@Composable
private fun BannerThinkingPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        AdventureBanner(
            heroName = "Lupo Solitario",
            heroGender = Gender.MALE,
            narratorSpeaking = true,
            narratorThinking = true,
        )
    }
}

@Preview(showBackground = true, name = "Banner — tocca all'eroe")
@Composable
private fun BannerHeroPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        AdventureBanner(heroName = "Lupa Solitaria", heroGender = Gender.FEMALE, narratorSpeaking = false)
    }
}
