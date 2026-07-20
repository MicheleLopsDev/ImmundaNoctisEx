package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.core.data.model.GameItem
import io.github.luposolitario.immundanoctisex.core.data.model.ItemType

// Oggetti "sul banco" della scena (comando offerItem — Michele
// 21/07/2026: "il pick deve sempre essere di una singola cosa per volta,
// addItem non può funzionare in maniera silenziosa"). Un pulsante per
// oggetto, disabilitato con un motivo esplicito quando non c'è spazio —
// mai un tocco che silenziosamente non fa nulla.
@Composable
fun PickupZone(state: AdventureState) {
    val items = state.availableItems
    if (items.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Puoi prendere", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            items.forEach { item ->
                PickupRow(item, canPick = state.canPickItem(item), onPick = { state.pickItem(item) })
            }
        }
    }
}

@Composable
private fun PickupRow(item: GameItem, canPick: Boolean, onPick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.Bold)
            val effect = item.effect
            if (effect != null) {
                Text(effect, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = onPick, enabled = canPick) {
            Text(if (canPick) "Prendi" else fullReason(item))
        }
    }
}

private fun fullReason(item: GameItem): String = when (item.type) {
    ItemType.WEAPON -> "Hai già 2 armi"
    ItemType.BACKPACK_ITEM -> "Zaino pieno"
    ItemType.GOLD -> "Borsa piena"
    ItemType.SPECIAL_ITEM -> "Prendi" // sempre disponibile, non capita mai
}
