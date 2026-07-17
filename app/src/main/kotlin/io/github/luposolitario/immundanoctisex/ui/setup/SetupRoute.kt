package io.github.luposolitario.immundanoctisex.ui.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.luposolitario.immundanoctisex.AppContainer
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData

// Raccordo della schermata Avventura: legge i salvataggi dallo store e
// passa dati puri alla schermata stateless.
@Composable
fun SetupRoute(
    container: AppContainer,
    onContinueSession: (SessionData) -> Unit,
    onNewAdventure: (Difficulty) -> Unit,
) {
    val sessions = remember { container.sessionStore.listSessions() }
    AdventureSetupScreen(
        savedSessions = sessions,
        onContinueSession = onContinueSession,
        onNewAdventure = onNewAdventure,
    )
}
