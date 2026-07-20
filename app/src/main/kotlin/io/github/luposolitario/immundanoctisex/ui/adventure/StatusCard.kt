package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.Gender
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.rank.KaiRank
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveCombatSkill
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveEndurance
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveMaxEndurance
import io.github.luposolitario.immundanoctisex.ui.creation.disciplineIcon
import io.github.luposolitario.immundanoctisex.ui.sheet.kaiRankName
import io.github.luposolitario.immundanoctisex.util.StatusCardColor

// La card di stato in fondo alla scena (UI.md §Card di stato), con le
// icone di v1 al posto del solo testo: ritratto-lupo tondo, medaglia
// dorata del grado Kai, spada per la Combattività, cuore per la
// Resistenza, monete per le Corone e la riga delle discipline possedute.
// Il tocco apre la Scheda personaggio.
private val KAI_GOLD = Color(0xFFFFD700)

@Composable
fun StatusCard(
    hero: Character,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Scelto in Opzioni (StatusCardColorPreferences, 21/07/2026): DEFAULT
    // non passa colori a Card, resta la superficie standard di Material.
    cardColor: StatusCardColor = StatusCardColor.DEFAULT,
) {
    val colors = if (cardColor.background != null && cardColor.content != null) {
        CardDefaults.cardColors(containerColor = cardColor.background, contentColor = cardColor.content)
    } else {
        CardDefaults.cardColors()
    }
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = colors) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.lupo_solitario),
                contentDescription = hero.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(CircleShape).border(2.dp, KAI_GOLD, CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(hero.name, fontWeight = FontWeight.Bold)
                KaiRankRow(hero)
                Spacer(Modifier.height(4.dp))
                VitalsRow(hero)
                DisciplinesRow(hero)
            }
        }
    }
}

// Il grado è puramente cosmetico (REGOLE.md Blocco 3) e si calcola dal
// numero di discipline: qui si mostra solo, non si serializza.
@Composable
private fun KaiRankRow(hero: Character) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.MilitaryTech,
            contentDescription = null,
            tint = KAI_GOLD,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(kaiRankName(KaiRank.fromDisciplineCount(hero.kaiDisciplines.size))),
            style = MaterialTheme.typography.labelMedium,
            color = KAI_GOLD,
        )
    }
}

// Valori EFFETTIVI: i bonus si calcolano, non si serializzano.
@Composable
private fun VitalsRow(hero: Character) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_sword),
            contentDescription = "Combattività",
            modifier = Modifier.size(16.dp),
        )
        Text("${effectiveCombatSkill(hero)}", style = MaterialTheme.typography.bodyMedium)

        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Resistenza",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp),
        )
        Text(
            "${effectiveEndurance(hero)}/${effectiveMaxEndurance(hero)}",
            style = MaterialTheme.typography.bodyMedium,
        )

        Image(
            painter = painterResource(id = R.drawable.ic_gold),
            contentDescription = "Corone",
            modifier = Modifier.size(16.dp),
        )
        Text(
            "${Inventory.countOf(hero, "Gold Crowns")}",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
    }
}

// Le discipline possedute a colpo d'occhio, senza aprire la scheda: le
// icone sono le stesse della creazione, così il giocatore le riconosce.
@Composable
private fun DisciplinesRow(hero: Character) {
    if (hero.kaiDisciplines.isEmpty()) return
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        hero.kaiDisciplines.forEach { id ->
            Icon(
                imageVector = disciplineIcon(id),
                contentDescription = id,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Preview(showBackground = true, name = "Card di stato — Maestro Kai")
@Composable
private fun StatusCardPreview() {
    io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme(darkTheme = true) {
        StatusCard(
            hero = Character(
                role = CharacterRole.HERO,
                name = "Lupo Solitario",
                gender = Gender.MALE,
                baseCombatSkill = 18,
                currentEndurance = 22,
                maxEndurance = 25,
                kaiDisciplines = listOf("HEALING", "MINDBLAST", "TRACKING", "HUNTING", "SIXTH_SENSE"),
            ),
            onClick = {},
        )
    }
}
