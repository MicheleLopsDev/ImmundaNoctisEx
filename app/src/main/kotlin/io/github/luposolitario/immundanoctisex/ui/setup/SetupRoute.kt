package io.github.luposolitario.immundanoctisex.ui.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.luposolitario.immundanoctisex.AppContainer
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
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
    val sessions = remember {
        container.sessionStore.listSessions().filter { it.packageId == currentPackageId }
    }
    AdventureSetupScreen(
        savedSessions = sessions,
        onContinueSession = onContinueSession,
        onNewAdventure = onNewAdventure,
    )
}
