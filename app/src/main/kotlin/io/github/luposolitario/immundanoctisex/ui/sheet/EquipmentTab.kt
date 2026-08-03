package io.github.luposolitario.immundanoctisex.ui.sheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.StatModifier
import io.github.luposolitario.immundanoctisex.core.data.model.StatType
import io.github.luposolitario.immundanoctisex.core.data.model.WeaponType
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveCombatSkill
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveMaxEndurance
import io.github.luposolitario.immundanoctisex.core.engine.stats.itemEnduranceBonus
import io.github.luposolitario.immundanoctisex.core.engine.stats.weaponskillBonus
import io.github.luposolitario.immundanoctisex.ui.creation.disciplineName
import io.github.luposolitario.immundanoctisex.ui.creation.weaponTypeIcon
import io.github.luposolitario.immundanoctisex.ui.creation.weaponTypeName

// Equipaggiamento (UI.md §Inventario operativo): Combattività/Resistenza
// SCOMPOSTE (mockup approvato da Michele 20/07/2026 — riferimento
// fotografato dal registro cartaceo: BASE + i modificatori uno per uno,
// non solo il totale) + i 2 slot armi + zaino + oggetti speciali.
@Composable
fun EquipmentTab(
    hero: Character,
    onEquipWeapon: (String) -> Unit,
    onUnequipWeapon: () -> Unit,
    onConsumeItem: (String) -> Unit,
    onDiscardItem: (String) -> Unit,
) {
    StatsBreakdownCard(hero)
    WeaponsCard(hero, onEquipWeapon, onUnequipWeapon)
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
        Text(
            stringResource(R.string.stat_combat_skill),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("${effectiveCombatSkill(hero)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        BreakdownLine(stringResource(R.string.sheet_base, hero.baseCombatSkill))
        val weaponBonus = weaponskillBonus(hero)
        if (weaponBonus != 0) {
            val weaponName = hero.weaponSkillType?.let { stringResource(weaponTypeName(it)) } ?: ""
            BreakdownLine(stringResource(R.string.sheet_weaponskill_bonus, weaponBonus, weaponName))
        }
        hero.activeModifiers.filter { it.stat == StatType.COMBAT_SKILL }.forEach {
            BreakdownLine(modifierLabel(it))
        }
    }
}

@Composable
private fun EnduranceBreakdown(hero: Character, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            stringResource(R.string.stat_endurance),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "${hero.currentEndurance} / ${effectiveMaxEndurance(hero)}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        BreakdownLine(stringResource(R.string.sheet_base, hero.maxEndurance))
        hero.inventory.forEach { item ->
            val bonus = itemEnduranceBonus(item)
            if (bonus != 0) BreakdownLine(stringResource(R.string.sheet_item_bonus, bonus, item.name))
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
private fun WeaponsCard(hero: Character, onEquipWeapon: (String) -> Unit, onUnequipWeapon: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.sheet_weapons), style = MaterialTheme.typography.titleLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val weapons = hero.inventory.filter { it.type == ItemType.WEAPON }
                repeat(Inventory.MAX_WEAPONS) { index ->
                    WeaponSlot(
                        slotNumber = index + 1,
                        weapon = weapons.getOrNull(index),
                        weaponCount = weapons.size,
                        hero = hero,
                        onEquip = onEquipWeapon,
                        onUnequip = onUnequipWeapon,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                stringResource(R.string.sheet_weapons_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Slot vuoto = Arti Marziali (26/07/2026, richiesta Michele: "invece di
// vuoto scrivi pugni o arti marziali" — nome coerente con la creazione,
// stessa stringa/icona di WeaponType.UNARMED, non un testo ad hoc), ora
// tappabile per disequipaggiare (Inventory.unequipWeapon esisteva già nel
// motore ma non era collegato a nessuna UI). Bordo oro sull'impugnata e
// sfondo verde sulla specializzazione WEAPONSKILL (anche qui indipendenti
// tra loro, stessa convenzione di WeaponCell in creazione): un'arma può
// essere la specializzazione senza essere impugnata ora.
@Composable
private fun WeaponSlot(
    slotNumber: Int,
    weapon: GameItem?,
    // Quante armi possiede DAVVERO (26/07/2026): con zero armi in
    // inventario (scelta "Arti Marziali" in creazione) ENTRAMBI gli slot
    // risultano weapon == null — senza questo, i due slot vuoti si
    // marcherebbero entrambi come "impugnato"/specializzazione, un
    // doppione visivo. Solo il primo slot oltre le armi possedute
    // (slotNumber == weaponCount + 1) rappresenta le mani nude.
    weaponCount: Int,
    hero: Character,
    onEquip: (String) -> Unit,
    onUnequip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUnarmedSlot = weapon == null && slotNumber == weaponCount + 1
    val equippedHere = if (weapon != null) {
        weapon.name.equals(hero.equippedWeapon, ignoreCase = true)
    } else {
        isUnarmedSlot && hero.equippedWeapon == null
    }
    val iconType = weapon?.weaponType ?: WeaponType.UNARMED
    val isSpecialization = if (weapon != null) {
        hero.weaponSkillType == iconType
    } else {
        isUnarmedSlot && hero.weaponSkillType == WeaponType.UNARMED
    }
    val borderColor = if (equippedHere) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
    OutlinedCard(
        onClick = { if (weapon != null) onEquip(weapon.name) else onUnequip() },
        modifier = modifier,
        border = BorderStroke(if (equippedHere) 3.dp else 1.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(
            containerColor = when {
                isSpecialization -> Color(0xFF2E7D32).copy(alpha = 0.35f)
                equippedHere -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$slotNumber", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Image(
                painter = painterResource(id = weaponTypeIcon(iconType)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp),
            )
            Text(
                weapon?.name ?: stringResource(weaponTypeName(WeaponType.UNARMED)),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (equippedHere) {
                val bonus = weaponskillBonus(hero)
                Text(
                    if (bonus != 0) {
                        stringResource(R.string.sheet_wielded_bonus, bonus)
                    } else {
                        stringResource(R.string.sheet_wielded)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

