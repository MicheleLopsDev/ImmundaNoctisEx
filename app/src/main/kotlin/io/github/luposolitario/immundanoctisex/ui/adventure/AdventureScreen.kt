package io.github.luposolitario.immundanoctisex.ui.adventure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.ui.creation.disciplineIcon

// La scena teatrale in forma minima (Fase 3, "prima funziona poi è
// bello"): header con titolo e numero scena, testo ORIGINALE del
// pacchetto come pagina di libro, zona scelte (normali + discipline),
// combat rapido, card di stato coi valori effettivi dell'engine.
@Composable
fun AdventureScreen(
    state: AdventureState,
    onExitToHome: () -> Unit,
    onReloadCheckpoint: (Int) -> Unit,
) {
    // Scheda e Diario come overlay dentro la route (stato condiviso;
    // diventeranno destinazioni proprie in Fase 5).
    var showSheet by remember { mutableStateOf(false) }
    var showJournal by remember { mutableStateOf(false) }
    if (showSheet) {
        io.github.luposolitario.immundanoctisex.ui.sheet.CharacterSheetScreen(
            hero = state.hero,
            onEquipWeapon = state::equipWeapon,
            onConsumeItem = state::consumeItem,
            onClose = { showSheet = false },
        )
        return
    }
    if (showJournal) {
        val context = androidx.compose.ui.platform.LocalContext.current
        io.github.luposolitario.immundanoctisex.ui.journal.JournalScreen(
            journey = state.gameState.session.journey,
            onExport = {
                val markdown = io.github.luposolitario.immundanoctisex.ui.journal.journeyToMarkdown(
                    state.bookTitle,
                    state.gameState.session.journey,
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, markdown)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Esporta diario"))
            },
            onClose = { showJournal = false },
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Header(state, onJournalClick = { showJournal = true })

        // Il palcoscenico: sfondo + ritratti, col cerchio d'oro su chi
        // "parla" (il narratore mentre scrive, altrimenti l'eroe).
        AdventureBanner(
            heroName = state.hero.name,
            heroGender = state.hero.gender,
            narratorSpeaking = state.isGenerating,
            // L'alone pulsa solo finché non c'è nulla da leggere: appena
            // arriva il primo pezzo di testo torna fermo.
            narratorThinking = state.isGenerating && state.narrative.isBlank(),
        )

        Card(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            if (state.narrative.isBlank() && state.isGenerating) {
                // Il narratore sta scrivendo: nessun testo originale da
                // leggere, solo l'attesa RACCONTATA (UI.md §Flusso).
                NarratorThinking(loadingModel = state.isLoadingModel)
            } else {
                Text(
                    // Il finale fabbricato dal motore nasce senza testo: lo
                    // scrive il narratore. Se non ha potuto (modello assente
                    // o generazione fallita) si mette quello fisso, perché
                    // una schermata vuota non è un finale.
                    text = state.narrative.ifBlank { stringResource(R.string.ending_synthetic_fallback) },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                )
            }
        }

        StatusCard(hero = state.hero, onClick = { showSheet = true })
        Spacer(Modifier.height(8.dp))

        when {
            state.combatSession != null -> CombatActiveZone(state)
            // Finché il narratore scrive non si mostrano scelte né nemico:
            // apparirebbero col testo originale per poi cambiare sotto gli
            // occhi (UI.md: prima lo streaming, POI i pulsanti).
            state.isGenerating -> Unit
            state.currentScene.combat != null -> CombatEntryZone(state)
            state.isEnding -> EndingZone(state, onExitToHome, onReloadCheckpoint)
            state.requiresRoll -> DiceZone(state)
            else -> {
                ChoicesZone(state)
                // Piazzamento checkpoint dal menu (STATO.md Blocco 2): fuori
                // dal combattimento, col budget della difficoltà visibile.
                if (state.checkpointsRemaining > 0) {
                    Spacer(Modifier.height(6.dp))
                    androidx.compose.material3.TextButton(onClick = { state.placeCheckpoint() }) {
                        Text("Piazza checkpoint (rimasti: ${state.checkpointsRemaining})")
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: AdventureState, onJournalClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Semaforo di stato del motore (UI.md §Header): verde pronto,
            // giallo generazione, rosso contesto quasi pieno.
            val tokenInfo = state.tokenInfo
            val color = when {
                state.isGenerating -> MaterialTheme.colorScheme.tertiary
                tokenInfo != null && tokenInfo.percentage > 60 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
            Icon(
                imageVector = Icons.Default.Circle,
                contentDescription = "Stato del narratore",
                tint = color,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(state.bookTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.TextButton(onClick = onJournalClick) {
                Text("Diario")
            }
            Text(
                "Scena ${state.currentScene.id}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChoicesZone(state: AdventureState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        state.availableChoices.forEach { choice ->
            Button(onClick = { state.takeChoice(choice) }, modifier = Modifier.fillMaxWidth()) {
                Text(state.choiceText(choice))
            }
        }
        state.availableDisciplineChoices.forEach { choice ->
            OutlinedCard(
                onClick = { state.useDiscipline(choice) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = disciplineIcon(choice.disciplineId),
                        contentDescription = choice.disciplineId,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(state.disciplineChoiceText(choice), fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Il Dado del Destino fuori dal combattimento (REGOLE.md Blocco 6): il
// gioco si ferma, le scelte spariscono, si tira e POI si va. v0.1 è un
// bottone; l'overlay animato è Fase 7.
@Composable
private fun DiceZone(state: AdventureState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val roll = state.lastChoiceRoll
        if (roll == null) {
            Text("Il destino decide: tira il Dado.", fontWeight = FontWeight.Bold)
            Button(onClick = { state.rollForChoice() }, modifier = Modifier.fillMaxWidth()) {
                Text("Tira il Dado del Destino")
            }
        } else {
            Text(
                "Hai tirato: $roll",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Button(onClick = { state.resolveRolledChoice() }, modifier = Modifier.fillMaxWidth()) {
                Text("Continua")
            }
        }
    }
}

@Composable
private fun EndingZone(
    state: AdventureState,
    onExitToHome: () -> Unit,
    onReloadCheckpoint: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // UN FINALE DICHIARA SEMPRE COM'È ANDATA (richiesta Michele
        // 20/07/2026): prima si arrivava in fondo al libro e si tornava al
        // menu senza sapere se si era vinto o perso.
        // Teschio per la morte, sole nascente per la vittoria (richiesta
        // Michele 20/07/2026). NEUTRAL non ha immagine: non c'è niente da
        // celebrare né da piangere.
        EndingBadge(
            outcome = state.endingOutcome,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.adventureDeleted) {
            Text(
                "Morte in IRON: la sessione è stata cancellata.",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
        }
        // Alla morte (fuori da IRON) si offrono i checkpoint piazzati:
        // ricaricabili illimitatamente, il diario si tronca alla fotografia.
        if (state.isDeathEnding && !state.adventureDeleted) {
            state.placedCheckpoints().forEach { slot ->
                Button(onClick = { onReloadCheckpoint(slot) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ricarica il checkpoint $slot")
                }
            }
        }
        Button(onClick = onExitToHome, modifier = Modifier.fillMaxWidth()) {
            Text("Torna alla Home")
        }
    }
}
