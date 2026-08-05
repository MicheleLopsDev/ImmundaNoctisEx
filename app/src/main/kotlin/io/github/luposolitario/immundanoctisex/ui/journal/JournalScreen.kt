package io.github.luposolitario.immundanoctisex.ui.journal

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import io.github.luposolitario.immundanoctisex.ui.adventure.sceneBackgroundRes
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.content.Context
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.core.data.model.AutoJumpReason
import io.github.luposolitario.immundanoctisex.core.data.model.CombatOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.JourneyEntry
import io.github.luposolitario.immundanoctisex.core.data.model.Transition
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// Diario del viaggio (UI.md §schermata 6), due viste: Racconto (rilettura
// voce per voce) e Mappa logica (i luoghi nell'ordine del viaggio, v0.1
// solo il nome). Stateless: voci in ingresso, export in uscita.
@Composable
fun JournalScreen(
    journey: List<JourneyEntry>,
    onExport: () -> Unit,
    onClose: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.journal_tab_story)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.journal_tab_map)) })
        }
        Spacer(Modifier.height(12.dp))

        if (tab == 0) StoryView(journey, Modifier.weight(1f)) else MapView(journey, Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.journal_export))
            }
            Button(onClick = onClose, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.common_close))
            }
        }
    }
}

@Composable
private fun StoryView(journey: List<JourneyEntry>, modifier: Modifier) {
    val context = LocalContext.current
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(journey) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.adventure_scene, entry.sceneId),
                            fontWeight = FontWeight.Bold,
                        )
                        entry.locationName?.let {
                            Text(it, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    Text(entry.enrichedText, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        transitionText(context, entry.transition),
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

// Una tappa del viaggio: un luogo, quante scene ci si è passate e da
// quale porta se ne è usciti. `uscita` è null solo per l'ultima tappa —
// da lì non si è ancora usciti, si è lì adesso.
// internal, non private: il raggruppamento è la sola logica vera di
// questa schermata ed è coperto da JournalMapTest.
internal data class Tappa(
    val luogo: String?,
    val scene: Int,
    val uscita: Transition?,
    // Lo sfondo della PRIMA scena della tappa: è l'immagine con cui il
    // luogo si è presentato al giocatore. Null se quelle scene non ne
    // avevano uno, o se il salvataggio è precedente al 05/08/2026.
    val sfondo: String? = null,
)

// Le voci raggruppate per luogo CONSECUTIVO. Una voce senza
// `locationName` non viene scartata (com'era prima): eredita il luogo
// della tappa in corso, altrimenti il conto delle scene direbbe il
// falso. Derivata dal diario-grafo a ogni apertura, mai salvata.
internal fun tappeDi(journey: List<JourneyEntry>): List<Tappa> {
    val tappe = mutableListOf<Tappa>()
    journey.forEachIndexed { index, entry ->
        val ultima = index == journey.lastIndex
        val uscita = if (ultima) null else entry.transition
        val precedente = tappe.lastOrNull()
        // Cambio tappa solo su un luogo NUOVO e dichiarato: senza nome
        // si resta dove si era.
        val cambiaLuogo = entry.locationName != null && entry.locationName != precedente?.luogo
        if (precedente == null || cambiaLuogo) {
            tappe += Tappa(
                luogo = entry.locationName ?: precedente?.luogo,
                scene = 1,
                uscita = uscita,
                sfondo = entry.backgroundImage,
            )
        } else {
            tappe[tappe.lastIndex] = precedente.copy(
                scene = precedente.scene + 1,
                uscita = uscita,
                // La prima immagine incontrata resta: se la scena che
                // apre la tappa non ne aveva una, la prende la prima che
                // ce l'ha.
                sfondo = precedente.sfondo ?: entry.backgroundImage,
            )
        }
    }
    return tappe
}

@Composable
private fun MapView(journey: List<JourneyEntry>, modifier: Modifier) {
    val tappe = remember(journey) { tappeDi(journey) }
    LazyColumn(modifier = modifier) {
        items(tappe.withIndex().toList()) { (index, tappa) ->
            RigaTappa(
                tappa = tappa,
                primaTappa = index == 0,
                ultimaTappa = index == tappe.lastIndex,
            )
        }
    }
}

// Il filo verticale che tiene insieme le tappe: due segmenti (sopra e
// sotto il pallino) disegnati a parte, così la prima tappa non ha filo
// sopra e l'ultima non ce l'ha sotto — il viaggio ha un inizio e una
// fine visibili, non una linea che esce dallo schermo.
@Composable
private fun RigaTappa(tappa: Tappa, primaTappa: Boolean, ultimaTappa: Boolean) {
    val coloreFilo = MaterialTheme.colorScheme.outlineVariant
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp).fillMaxHeight(),
        ) {
            Box(
                Modifier.width(2.dp).height(10.dp)
                    .background(if (primaTappa) Color.Transparent else coloreFilo),
            )
            Box(
                Modifier.size(12.dp).clip(CircleShape).background(
                    if (ultimaTappa) MaterialTheme.colorScheme.primary else coloreFilo,
                ),
            )
            Box(
                Modifier.width(2.dp).weight(1f)
                    .background(if (ultimaTappa) Color.Transparent else coloreFilo),
            )
        }
        // La miniatura del luogo (05/08/2026, richiesta di Michele): il
        // viaggio si riconosce a colpo d'occhio dalle immagini, non solo
        // leggendo i nomi. Assente sui salvataggi precedenti, e la riga
        // si stringe senza lasciare un buco.
        val sfondo = tappa.sfondo
        if (sfondo != null) {
            Image(
                painter = painterResource(sceneBackgroundRes(sfondo)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(end = 8.dp, top = 2.dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp, bottom = 12.dp)) {
            Text(
                tappa.luogo ?: stringResource(R.string.journal_no_location),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pluralStringResource(R.plurals.journal_stop_scenes, tappa.scene, tappa.scene),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val uscita = tappa.uscita
                if (uscita != null) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = iconaUscita(uscita),
                        contentDescription = stringResource(descrizioneUscita(uscita)),
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.journal_here_now),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

// Come si è usciti da una tappa, a colpo d'occhio. Le stesse quattro
// porte di `Transition`: scelta, disciplina, combattimento (con l'esito),
// destino.
private fun iconaUscita(transition: Transition): ImageVector = when (transition) {
    is Transition.ChoiceTaken -> Icons.AutoMirrored.Filled.ArrowForward
    is Transition.DisciplineUsed -> Icons.Default.AutoAwesome
    is Transition.CombatResolved -> when (transition.outcome) {
        CombatOutcome.WIN -> Icons.Default.MilitaryTech
        CombatOutcome.LOSE -> Icons.Default.HeartBroken
        // AutoMirrored: in una lingua che si legge da destra a sinistra
        // deve correre nell'altro verso.
        CombatOutcome.EVADE -> Icons.AutoMirrored.Filled.DirectionsRun
    }
    is Transition.AutoJump -> Icons.Default.Casino
}

@StringRes
private fun descrizioneUscita(transition: Transition): Int = when (transition) {
    is Transition.ChoiceTaken -> R.string.journal_exit_choice
    is Transition.DisciplineUsed -> R.string.journal_exit_discipline
    is Transition.CombatResolved -> when (transition.outcome) {
        CombatOutcome.WIN -> R.string.journal_exit_win
        CombatOutcome.LOSE -> R.string.journal_exit_lose
        CombatOutcome.EVADE -> R.string.journal_exit_evade
    }
    is Transition.AutoJump -> R.string.journal_exit_fate
}

// Prende il Context invece di essere @Composable perché serve anche
// all'export in Markdown, che gira fuori dalla composizione.
private fun transitionText(context: Context, transition: Transition): String = when (transition) {
    is Transition.ChoiceTaken ->
        context.getString(R.string.journal_transition_choice, transition.choiceId)
    is Transition.DisciplineUsed ->
        context.getString(R.string.journal_transition_discipline, transition.disciplineId)
    is Transition.CombatResolved -> context.getString(
        when (transition.outcome) {
            CombatOutcome.WIN -> R.string.journal_transition_combat_win
            CombatOutcome.LOSE -> R.string.journal_transition_combat_lose
            CombatOutcome.EVADE -> R.string.journal_transition_combat_evade
        },
    )
    is Transition.AutoJump ->
        context.getString(R.string.journal_transition_autojump, transition.reason)
}

// Il diario è già un generatore di racconto (STATO.md Blocco 3).
// I marcatori Markdown (#, ##, *) restano nel codice: sono sintassi, non
// testo da tradurre — in strings.xml sta solo quello che si legge.
fun journeyToMarkdown(context: Context, bookTitle: String, journey: List<JourneyEntry>): String = buildString {
    appendLine("# " + context.getString(R.string.journal_export_title, bookTitle))
    appendLine()
    journey.forEach { entry ->
        append("## " + context.getString(R.string.adventure_scene, entry.sceneId))
        entry.locationName?.let { append(" — $it") }
        appendLine()
        appendLine()
        appendLine(entry.enrichedText)
        appendLine()
        appendLine("*${transitionText(context, entry.transition)}*")
        appendLine()
    }
}

// Un viaggio con tutte e quattro le porte d'uscita e una tappa di più
// scene (le due voci senza `locationName` restano nel Vecchio Quartiere,
// non spariscono): serve a vedere la mappa nel caso peggiore, non in
// quello facile.
@Preview(showBackground = true, name = "Diario — mappa del viaggio", heightDp = 700)
@Composable
private fun MapViewPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        MapView(
            journey = listOf(
                JourneyEntry("1", "", Transition.ChoiceTaken("c1"), "Riverside Inn", "static:loc_tavern"),
                JourneyEntry("2", "", Transition.CombatResolved(CombatOutcome.WIN), "Harbour Town", "static:loc_harbor"),
                JourneyEntry("3", "", Transition.DisciplineUsed("SIXTH_SENSE", "d1"), "Old Quarter", "static:loc_alley"),
                // Le due senza luogo restano nel Vecchio Quartiere e non
                // portano immagine: la miniatura è quella della prima.
                JourneyEntry("4", "", Transition.CombatResolved(CombatOutcome.EVADE), null),
                JourneyEntry("5", "", Transition.AutoJump(AutoJumpReason.RANDOM_CHOICE), null),
                // Tappa senza sfondo: la riga si stringe, niente buco.
                JourneyEntry("6", "", Transition.ChoiceTaken("c9"), "Ruanon"),
            ),
            modifier = Modifier.padding(12.dp),
        )
    }
}

@Preview(showBackground = true, name = "Diario (scuro)", heightDp = 700)
@Composable
private fun JournalPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        JournalScreen(
            journey = listOf(
                JourneyEntry("1", "La lettera ti aspetta sul tavolo della locanda.", Transition.ChoiceTaken("c1"), "Riverside Inn"),
                JourneyEntry("2", "Le strade del porto brulicano di marinai.", Transition.ChoiceTaken("c2"), "Harbour Town"),
                JourneyEntry("3", "Il vicolo si stringe verso il magazzino.", Transition.DisciplineUsed("SIXTH_SENSE", "d1"), "Old Quarter"),
            ),
            onExport = {},
            onClose = {},
        )
    }
}
