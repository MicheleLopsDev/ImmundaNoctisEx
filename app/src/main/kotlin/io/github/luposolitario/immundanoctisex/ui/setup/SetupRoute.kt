package io.github.luposolitario.immundanoctisex.ui.setup

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.luposolitario.immundanoctisex.AppContainer
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.core.data.model.checkpointBudget
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageLoadResult

// Raccordo della schermata Avventura: legge i salvataggi dallo store e
// passa dati puri alla schermata stateless.
@Composable
fun SetupRoute(
    container: AppContainer,
    onContinueSession: (SessionData) -> Unit,
    onNewAdventure: (Difficulty) -> Unit,
) {
    // Con un solo libro possibile (prima del side-load, 20/07/2026)
    // "tutte le sessioni salvate" coincideva sempre con "le sessioni di
    // QUESTO libro". Ora che si può caricare un libro diverso, senza
    // filtro "Continua" offrirebbe anche sessioni di un altro pacchetto —
    // sceneById cercherebbe un id che nel libro corrente o non esiste
    // (degrada sul finale, grazie al fix del 20/07) o esiste per caso con
    // un contenuto tutto diverso. Si filtra per packageId del pacchetto
    // ATTUALMENTE caricato.
    val currentPackageId = remember {
        (container.packageRepository.load() as? PackageLoadResult.Success)?.manifest?.id
    }
    var sessions by remember {
        mutableStateOf(container.sessionStore.listSessions().filter { it.packageId == currentPackageId })
    }

    // Checkpoint vs posizione attuale (24/07/2026, richiesta Michele:
    // "quando parte l'avventura mi deve chiedere se voglio caricare
    // l'ultimo checkpoint salvato oppure il punto a cui ero arrivato") —
    // solo se il pacchetto ha almeno un checkpoint piazzato: senza,
    // "Continua" si comporta come sempre, niente popup per chi non ne
    // ha mai piazzato uno.
    var pendingChoice by remember { mutableStateOf<Pair<SessionData, Int>?>(null) }

    pendingChoice?.let { (session, slot) ->
        AlertDialog(
            onDismissRequest = { pendingChoice = null },
            title = { Text("Hai un checkpoint salvato") },
            text = {
                Text(
                    "Vuoi riprendere dal checkpoint o continuare da dove eri arrivato " +
                        "(scena ${session.currentSceneId})?",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingChoice = null
                    // Stessa regola del ricaricamento alla morte (Michele
                    // 20/07/2026, "le vite sono davvero finite"): usare il
                    // checkpoint lo consuma sempre, anche qui.
                    val checkpoint = container.sessionStore.loadCheckpoint(session.packageId, slot)
                    if (checkpoint != null) {
                        container.sessionStore.deleteCheckpoint(session.packageId, slot)
                        onContinueSession(checkpoint)
                    } else {
                        onContinueSession(session)
                    }
                }) { Text("Carica il checkpoint") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingChoice = null
                    onContinueSession(session)
                }) { Text("Continua da dove ero") }
            },
        )
    }

    AdventureSetupScreen(
        savedSessions = sessions,
        onContinueSession = { session ->
            val budget = session.difficulty.checkpointBudget()
            val lastSlot = (1..budget).lastOrNull {
                container.sessionStore.loadCheckpoint(session.packageId, it) != null
            }
            if (lastSlot != null) {
                pendingChoice = session to lastSlot
            } else {
                onContinueSession(session)
            }
        },
        onNewAdventure = onNewAdventure,
        onDeleteSession = { session ->
            container.sessionStore.deleteAdventure(session.packageId)
            sessions = sessions - session
        },
    )
}
