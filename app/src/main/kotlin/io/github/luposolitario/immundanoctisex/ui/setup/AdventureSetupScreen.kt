package io.github.luposolitario.immundanoctisex.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.core.data.model.CharacterRole
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Schermata Avventura (UI.md §schermata 1-2): se esistono salvataggi si
// sceglie quale continuare; altrimenti/in più si parte con una nuova
// avventura scegliendo la difficoltà (spiegazione ONESTA di IRON,
// STATO.md Blocco 2). Stateless: sessioni in ingresso, eventi in uscita.
@Composable
fun AdventureSetupScreen(
    savedSessions: List<SessionData>,
    onContinueSession: (SessionData) -> Unit,
    onNewAdventure: (Difficulty) -> Unit,
    // Prima l'unico modo di liberarsi di un salvataggio era side-load un
    // file diverso (Michele 22/07/2026, dopo che quello smise di essere
    // un effetto collaterale: "adesso come cancello la sessione, mi ci
    // vuole un tasto?") — sì, un tasto esplicito, qui.
    onDeleteSession: (SessionData) -> Unit = {},
) {
    // Conferma prima di eliminare per davvero: è un'azione irreversibile,
    // stesso trattamento già dato all'uscita dalla scena (showExitConfirm
    // in AdventureScreen).
    var sessionPendingDelete by remember { mutableStateOf<SessionData?>(null) }
    sessionPendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionPendingDelete = null },
            title = { Text("Eliminare il salvataggio?") },
            text = { Text("\"${session.packageId}\" — scena ${session.currentSceneId}. Non si può recuperare.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSession(session)
                    sessionPendingDelete = null
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { sessionPendingDelete = null }) { Text("Annulla") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (savedSessions.isNotEmpty()) {
            Text(stringResource(R.string.setup_continue_title), style = MaterialTheme.typography.headlineMedium)
            savedSessions.forEach { session ->
                SavedSessionCard(
                    session = session,
                    onContinue = { onContinueSession(session) },
                    onDelete = { sessionPendingDelete = session },
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.setup_choose_difficulty), style = MaterialTheme.typography.titleMedium)

        DifficultyCard(
            nameRes = R.string.difficulty_normal,
            descriptionRes = R.string.difficulty_normal_desc,
            onClick = { onNewAdventure(Difficulty.NORMAL) },
        )
        DifficultyCard(
            nameRes = R.string.difficulty_hard,
            descriptionRes = R.string.difficulty_hard_desc,
            onClick = { onNewAdventure(Difficulty.HARD) },
        )
        DifficultyCard(
            nameRes = R.string.difficulty_iron,
            descriptionRes = R.string.difficulty_iron_desc,
            isDangerous = true,
            onClick = { onNewAdventure(Difficulty.IRON) },
        )
    }
}

@Composable
private fun SavedSessionCard(session: SessionData, onContinue: () -> Unit, onDelete: () -> Unit) {
    val formattedDate = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        .format(Date(session.lastUpdate))

    // Nome dell'eroe nella lista salvataggi (24/07/2026, richiesta
    // Michele): l'eroe è unico per costruzione (stesso presupposto di
    // GameState.hero — una sessione senza eroe è un pacchetto rotto,
    // intercettato a monte dai validatori).
    val heroName = session.characters.first { it.role == CharacterRole.HERO }.name

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(session.packageId, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                heroName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.setup_last_save, formattedDate),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "${session.difficulty} — scena ${session.currentSceneId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onContinue, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.setup_continue_session))
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Elimina") }
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    nameRes: Int,
    descriptionRes: Int,
    isDangerous: Boolean = false,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isDangerous) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(nameRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(stringResource(descriptionRes), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true, name = "Setup senza salvataggi (scuro)")
@Composable
private fun AdventureSetupEmptyPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        AdventureSetupScreen(savedSessions = emptyList(), onContinueSession = {}, onNewAdventure = {})
    }
}

@Preview(showBackground = true, name = "Setup con salvataggio (chiaro)")
@Composable
private fun AdventureSetupWithSessionPreview() {
    ImmundaNoctisTheme(darkTheme = false) {
        AdventureSetupScreen(
            savedSessions = listOf(previewSession()),
            onContinueSession = {},
            onNewAdventure = {},
        )
    }
}

private fun previewSession() = SessionData(
    saveFormatVersion = 1,
    packageId = "sample-adventure",
    packageVersion = "1.0.0",
    difficulty = Difficulty.NORMAL,
    currentSceneId = "3",
    lastUpdate = 1_700_000_000_000,
    characters = listOf(
        io.github.luposolitario.immundanoctisex.core.data.model.Character(
            role = CharacterRole.HERO,
            name = "Lupo",
            baseCombatSkill = 18,
            currentEndurance = 25,
            maxEndurance = 25,
        ),
    ),
)
