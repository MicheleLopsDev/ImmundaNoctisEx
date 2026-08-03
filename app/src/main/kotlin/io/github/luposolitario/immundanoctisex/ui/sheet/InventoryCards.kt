package io.github.luposolitario.immundanoctisex.ui.sheet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.R
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
            title = { Text(stringResource(R.string.sheet_discard_title, item.name)) },
            text = { Text(stringResource(R.string.sheet_discard_text)) },
            confirmButton = {
                TextButton(onClick = { onDiscardItem(item.name); pendingDiscard = null }) {
                    Text(stringResource(R.string.sheet_discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDiscard = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.sheet_backpack), style = MaterialTheme.typography.titleLarge)
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
                            // Icona sopra, nome e modificatore sotto
                            // (24/07/2026, richiesta Michele: prima solo
                            // testo, come le celle arma/oggetto speciale
                            // altrove). Solo Pasti e pozioni la hanno oggi
                            // (`deco_meal`/`deco_potion`, dal foglio
                            // decorazioni già pronto ma non ancora
                            // agganciato) — un oggetto sconosciuto (es. da
                            // un ADD_ITEM di un libro) resta senza icona,
                            // non un segnaposto rotto.
                            backpackItemIcon(item)?.let { iconRes ->
                                // 28dp -> 34dp (24/07/2026, richiesta
                                // Michele: "un pelo più grandi").
                                Image(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp),
                                )
                                Spacer(Modifier.height(2.dp))
                            }
                            Text(
                                item?.name ?: stringResource(R.string.sheet_empty_slot),
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

// Icona per lo slot zaino (24/07/2026): solo per gli oggetti comuni che
// hanno già un'illustrazione pronta dal foglio decorazioni. Null per
// tutto il resto (oggetti da libro non previsti qui) — niente icona,
// mai un segnaposto rotto.
private fun backpackItemIcon(item: GameItem?): Int? = when {
    item == null -> null
    item.name == "Meal" -> R.drawable.deco_meal
    item.name.contains("Potion", ignoreCase = true) -> R.drawable.deco_potion
    else -> null
}

// Oggetti speciali e Corone.
@Composable
fun SpecialItemsCard(hero: Character) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.sheet_special_items), style = MaterialTheme.typography.titleLarge)
            hero.inventory.filter { it.type == ItemType.SPECIAL_ITEM }.forEach {
                Text(
                    if (it.quantity > 1) {
                        stringResource(R.string.sheet_item_bullet_qty, it.name, it.quantity)
                    } else {
                        stringResource(R.string.sheet_item_bullet, it.name)
                    },
                )
            }
            // "Gold Crowns" è l'ID canonico nei dati, non un testo mostrato.
            Text(
                stringResource(
                    R.string.sheet_gold_line,
                    Inventory.countOf(hero, "Gold Crowns"),
                    Inventory.MAX_GOLD,
                ),
            )
        }
    }
}
