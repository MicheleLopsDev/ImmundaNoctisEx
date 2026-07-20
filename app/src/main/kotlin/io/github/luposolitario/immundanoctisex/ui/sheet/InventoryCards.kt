package io.github.luposolitario.immundanoctisex.ui.sheet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.data.model.Character
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType
import io.github.luposolitario.immundanoctisex.core.engine.inventory.Inventory

// Zaino: gli 8 posti DISEGNATI anche vuoti (UI.md). Tocco = consuma
// (se ha un effetto), tocco lungo = scarta con conferma (Michele
// 21/07/2026: "manca la possibilità di scartare tenendo premuto" —
// l'engine aveva già Inventory.removeItem, mancava solo il gancio UI).
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BackpackCard(hero: Character, onConsumeItem: (String) -> Unit, onDiscardItem: (String) -> Unit) {
    var pendingDiscard by remember { mutableStateOf<GameItem?>(null) }
    pendingDiscard?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDiscard = null },
            title = { Text("Scartare ${item.name}?") },
            text = { Text("Non potrai più usarlo, a meno di trovarlo di nuovo.") },
            confirmButton = {
                TextButton(onClick = { onDiscardItem(item.name); pendingDiscard = null }) { Text("Scarta") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDiscard = null }) { Text("Annulla") }
            },
        )
    }

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
                        modifier = Modifier.aspectRatio(1f).combinedClickable(
                            onClick = { item?.let { onConsumeItem(it.name) } },
                            onLongClick = { item?.let { pendingDiscard = it } },
                        ),
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                item?.name ?: "Vuoto",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = if (item == null) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            if (item?.effect != null) {
                                Text(
                                    item.effect!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Oggetti speciali e Corone.
@Composable
fun SpecialItemsCard(hero: Character) {
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
