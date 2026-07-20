package io.github.luposolitario.immundanoctisex.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.StatModifier
import io.github.luposolitario.immundanoctisex.core.data.model.StatType
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveCombatSkill
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveMaxEndurance
import io.github.luposolitario.immundanoctisex.core.engine.stats.itemEnduranceBonus
import io.github.luposolitario.immundanoctisex.core.engine.stats.weaponskillBonus
import io.github.luposolitario.immundanoctisex.ui.creation.disciplineName
import io.github.luposolitario.immundanoctisex.ui.creation.weaponTypeName

// Equipaggiamento (UI.md §Inventario operativo): Combattività/Resistenza
// SCOMPOSTE (mockup approvato da Michele 20/07/2026 — riferimento
// fotografato dal registro cartaceo: BASE + i modificatori uno per uno,
// non solo il totale) + i 2 slot armi + zaino + oggetti speciali.
@Composable
fun EquipmentTab(
    hero: Character,
    onEquipWeapon: (String) -> Unit,
    onConsumeItem: (String) -> Unit,
    onDiscardItem: (String) -> Unit,
) {
    StatsBreakdownCard(hero)
    WeaponsCard(hero, onEquipWeapon)
    BackpackCard(hero, onConsumeItem, onDiscardItem)
    SpecialItemsCard(hero)
}

// La scomposizione legge le STESSE funzioni che calcolano il numero finale
// (weaponskillBonus, itemEnduranceBonus): non si ricalcola nulla qui, si
// spiega solo cosa l'engine ha già deciso — l'errore di v1 (LoneWolfRules
// che sommava i modificatori per conto suo) non si ripete.
@Composable
private fun StatsBreakdownCard(hero: Character) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CombatSkillBreakdown(hero, modifier = Modifier.weight(1f))
            EnduranceBreakdown(hero, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CombatSkillBreakdown(hero: Character, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("Combattività", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${effectiveCombatSkill(hero)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        BreakdownLine("Base ${hero.baseCombatSkill}")
        val weaponBonus = weaponskillBonus(hero)
        if (weaponBonus != 0) {
            val weaponName = hero.weaponSkillType?.let { stringResource(weaponTypeName(it)) } ?: ""
            BreakdownLine("+$weaponBonus WEAPONSKILL ($weaponName)")
        }
        hero.activeModifiers.filter { it.stat == StatType.COMBAT_SKILL }.forEach {
            BreakdownLine(modifierLabel(it))
        }
    }
}

@Composable
private fun EnduranceBreakdown(hero: Character, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("Resistenza", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "${hero.currentEndurance} / ${effectiveMaxEndurance(hero)}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        BreakdownLine("Base ${hero.maxEndurance}")
        hero.inventory.forEach { item ->
            val bonus = itemEnduranceBonus(item)
            if (bonus != 0) BreakdownLine("+$bonus ${item.name}")
        }
        hero.activeModifiers.filter { it.stat == StatType.ENDURANCE }.forEach {
            BreakdownLine(modifierLabel(it))
        }
    }
}

@Composable
private fun BreakdownLine(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun modifierLabel(modifier: StatModifier): String {
    val sign = if (modifier.amount >= 0) "+" else ""
    val name = disciplineName(modifier.sourceType)?.let { stringResource(it) } ?: modifier.sourceType
    return "$sign${modifier.amount} $name"
}

// Armi: SOLO 2 slot (Inventory.MAX_WEAPONS), tocco = impugna. Lo slot
// impugnato mostra anche il bonus WEAPONSKILL se scatta con quell'arma —
// la nota sotto spiega la regola una volta sola, come nel cartaceo.
@Composable
private fun WeaponsCard(hero: Character, onEquipWeapon: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Armi", style = MaterialTheme.typography.titleLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val weapons = hero.inventory.filter { it.type == ItemType.WEAPON }
                repeat(Inventory.MAX_WEAPONS) { index ->
                    WeaponSlot(
                        slotNumber = index + 1,
                        weapon = weapons.getOrNull(index),
                        hero = hero,
                        onEquip = onEquipWeapon,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                "+2 con l'arma della tua specializzazione · -4 senza armi",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeaponSlot(slotNumber: Int, weapon: GameItem?, hero: Character, onEquip: (String) -> Unit, modifier: Modifier = Modifier) {
    val equipped = weapon != null && weapon.name.equals(hero.equippedWeapon, ignoreCase = true)
    OutlinedCard(
        onClick = { weapon?.let { onEquip(it.name) } },
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (equipped) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$slotNumber", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                weapon?.name ?: "Vuoto",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (equipped) {
                val bonus = weaponskillBonus(hero)
                Text(
                    if (bonus != 0) "Impugnata · +$bonus" else "Impugnata",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

