package io.github.luposolitario.immundanoctisex.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.data.model.WeaponType
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory
import io.github.luposolitario.immundanoctisex.core.engine.rank.KaiRank
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveCombatSkill
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveEndurance
import io.github.luposolitario.immundanoctisex.core.engine.stats.effectiveMaxEndurance
import io.github.luposolitario.immundanoctisex.ui.creation.disciplineDescription
import io.github.luposolitario.immundanoctisex.ui.creation.disciplineIcon
import io.github.luposolitario.immundanoctisex.ui.creation.disciplineName
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// Scheda personaggio (UI.md §schermata 5), due tab. Stateless: eroe in
// ingresso, azioni in uscita — le stat mostrate sono le EFFETTIVE
// dell'engine, mai ricalcolate qui (il difetto di v1 non si ripete).
@Composable
fun CharacterSheetScreen(
    hero: Character,
    onEquipWeapon: (String) -> Unit,
    onConsumeItem: (String) -> Unit,
    onClose: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Stats e Discipline") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Equipaggiamento") })
        }
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (tab == 0) StatsTab(hero) else EquipmentTab(hero, onEquipWeapon, onConsumeItem)
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Chiudi")
        }
    }
}

@Composable
private fun StatsTab(hero: Character) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(hero.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                stringResource(kaiRankName(KaiRank.fromDisciplineCount(hero.kaiDisciplines.size))),
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Text("Combattività: ${effectiveCombatSkill(hero)}", fontWeight = FontWeight.Bold)
                Text("Resistenza: ${effectiveEndurance(hero)}/${effectiveMaxEndurance(hero)}", fontWeight = FontWeight.Bold)
            }
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Discipline Kai", style = MaterialTheme.typography.titleLarge)
            hero.kaiDisciplines.forEach { id ->
                // Prima si mostrava l'ID canonico grezzo ("MINDBLAST"):
                // nome e descrizione erano già in strings.xml, inutilizzati.
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = disciplineIcon(id),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = disciplineName(id)?.let { stringResource(it) } ?: id,
                                fontWeight = FontWeight.Bold,
                            )
                            if (id == "WEAPONSKILL" && hero.weaponSkillType != null) {
                                Text(
                                    " (${hero.weaponSkillType})",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        disciplineDescription(id)?.let { desc ->
                            Text(
                                text = stringResource(desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipmentTab(
    hero: Character,
    onEquipWeapon: (String) -> Unit,
    onConsumeItem: (String) -> Unit,
) {
    // Armi: 2 slot, tocco = impugna (bordo evidenziato sull'impugnata).
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Armi", style = MaterialTheme.typography.titleLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val weapons = hero.inventory.filter { it.type == ItemType.WEAPON }
                repeat(Inventory.MAX_WEAPONS) { index ->
                    val weapon = weapons.getOrNull(index)
                    val equipped = weapon != null && weapon.name.equals(hero.equippedWeapon, ignoreCase = true)
                    OutlinedCard(
                        onClick = { weapon?.let { onEquipWeapon(it.name) } },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
                            containerColor = if (equipped) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(weapon?.name ?: "Vuoto", fontWeight = FontWeight.Bold)
                            if (equipped) Text("Impugnata", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    // Zaino: gli 8 posti DISEGNATI anche vuoti (UI.md).
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Zaino", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            val slots = buildList {
                hero.inventory.filter { it.type == ItemType.BACKPACK_ITEM }
                    .forEach { item -> repeat(item.quantity) { add(item) } }
                while (size < Inventory.MAX_BACKPACK_SLOTS) add(null)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(190.dp),
            ) {
                items(slots.size) { index ->
                    val item = slots[index]
                    OutlinedCard(
                        onClick = { item?.let { onConsumeItem(it.name) } },
                        modifier = Modifier.aspectRatio(1f),
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                item?.name ?: "Vuoto",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (item == null) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            if (item?.effect != null) {
                                Text(item.effect!!, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    // Oggetti speciali e Corone.
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Oggetti speciali e borsa", style = MaterialTheme.typography.titleLarge)
            hero.inventory.filter { it.type == ItemType.SPECIAL_ITEM }.forEach {
                Text("• ${it.name}${if (it.quantity > 1) " x${it.quantity}" else ""}")
            }
            Text("Corone d'oro: ${Inventory.countOf(hero, "Gold Crowns")} / ${Inventory.MAX_GOLD}")
        }
    }
}

// internal: il grado Kai si mostra anche nella card di stato della scena.
internal fun kaiRankName(rank: KaiRank): Int = when (rank) {
    KaiRank.NOVICE -> R.string.kai_rank_novice
    KaiRank.INITIATE -> R.string.kai_rank_initiate
    KaiRank.DISCIPLE -> R.string.kai_rank_disciple
    KaiRank.WAYFARER -> R.string.kai_rank_wayfarer
    KaiRank.WARRIOR -> R.string.kai_rank_warrior
    KaiRank.MASTER -> R.string.kai_rank_master
    KaiRank.GRAND_MASTER -> R.string.kai_rank_grand_master
    KaiRank.SUPREME_GRAND_MASTER -> R.string.kai_rank_supreme_grand_master
}

@Preview(showBackground = true, name = "Scheda (scuro)", heightDp = 900)
@Composable
private fun CharacterSheetPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        CharacterSheetScreen(
            hero = Character(
                role = CharacterRole.HERO,
                name = "Lupo Solitario",
                baseCombatSkill = 17,
                currentEndurance = 22,
                maxEndurance = 25,
                kaiDisciplines = listOf("WEAPONSKILL", "HEALING", "SIXTH_SENSE", "HUNTING", "MINDBLAST"),
                weaponSkillType = WeaponType.SWORD,
                inventory = listOf(
                    GameItem(name = "Sword", type = ItemType.WEAPON, weaponType = WeaponType.SWORD),
                    GameItem(name = "Meal", type = ItemType.BACKPACK_ITEM, quantity = 2),
                    GameItem(name = "Laumspur Potion", type = ItemType.BACKPACK_ITEM, effect = "HEAL:4"),
                    GameItem(name = "Seal of Hammerdal", type = ItemType.SPECIAL_ITEM),
                    GameItem(name = "Gold Crowns", type = ItemType.GOLD, quantity = 12),
                ),
                equippedWeapon = "Sword",
            ),
            onEquipWeapon = {},
            onConsumeItem = {},
            onClose = {},
        )
    }
}
