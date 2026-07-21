package io.github.luposolitario.immundanoctisex.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.luposolitario.immundanoctisex.AppContainer
import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageLoadResult

// Side-load del libro (20/07/2026, richiesta urgente di Michele — "devo
// poter caricare vari file, serve per i test"): PackageSource lo
// prevedeva già come terza implementazione, mancava solo il collegamento
// al picker di sistema (SAF). Nessun path scelto a mano, nessun permesso
// da gestire fuori da qui: rememberLauncherForActivityResult fa tutto.
//
// Valida SUBITO al momento della scelta (non aspetta che CreationRoute/
// AdventureRoute lo scoprano più avanti): un file rotto lo si sa qui,
// con un messaggio, invece che navigando alla cieca in una schermata che
// dice solo "pacchetto non valido".
@Composable
fun HomeRoute(
    container: AppContainer,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onAdventureClick: () -> Unit,
    onModelsClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val context = LocalContext.current
    var currentTitle by remember { mutableStateOf(currentBookTitle(container)) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackIsError by remember { mutableStateOf(false) }

    val pickBook = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        container.loadSideloadedPackage(context, uri)
        when (val result = container.packageRepository.load()) {
            is PackageLoadResult.Success -> {
                // Un salvataggio vecchio con lo stesso packageId (Michele
                // 22/07/2026: "se carico un file json cancella il
                // salvataggio corrente se c'è, non ha senso") non deve
                // sopravvivere al side-load: senza questo, SetupRoute lo
                // trova ancora (filtra solo per packageId, non sa che il
                // CONTENUTO del file è cambiato) e offre "Continua" su
                // scene che magari non esistono più nella versione appena
                // caricata. Stesso deleteAdventure già usato da
                // CreationRoute per "nuova avventura".
                container.sessionStore.deleteAdventure(result.manifest.id)
                currentTitle = result.manifest.title
                feedback = "Libro caricato: ${result.manifest.title}"
                feedbackIsError = false
            }
            is PackageLoadResult.Failure -> {
                feedback = result.errors.firstOrNull() ?: "Pacchetto non valido"
                feedbackIsError = true
            }
        }
    }

    HomeScreen(
        isDarkTheme = isDarkTheme,
        onThemeToggle = onThemeToggle,
        onAdventureClick = onAdventureClick,
        onModelsClick = onModelsClick,
        onSettingsClick = onSettingsClick,
        currentBookTitle = currentTitle,
        onLoadBookClick = { pickBook.launch(arrayOf("application/json")) },
        loadFeedback = feedback,
        loadFeedbackIsError = feedbackIsError,
    )
}

// Il titolo del libro attualmente caricato, per il primo disegno della
// Home: se il pacchetto di default (asset) è già valido lo si mostra
// subito, senza aspettare un caricamento manuale.
private fun currentBookTitle(container: AppContainer): String =
    when (val result = container.packageRepository.load()) {
        is PackageLoadResult.Success -> result.manifest.title
        is PackageLoadResult.Failure -> "nessuno"
    }
