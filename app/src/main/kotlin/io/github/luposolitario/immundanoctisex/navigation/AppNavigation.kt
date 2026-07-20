package io.github.luposolitario.immundanoctisex.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.luposolitario.immundanoctisex.AppContainer
import io.github.luposolitario.immundanoctisex.core.data.model.Difficulty
import io.github.luposolitario.immundanoctisex.core.data.model.SessionData
import io.github.luposolitario.immundanoctisex.ui.adventure.AdventureRoute
import io.github.luposolitario.immundanoctisex.ui.creation.CreationRoute
import io.github.luposolitario.immundanoctisex.ui.home.HomeRoute
import io.github.luposolitario.immundanoctisex.ui.models.ModelsRoute
import io.github.luposolitario.immundanoctisex.ui.options.OptionsRoute
import io.github.luposolitario.immundanoctisex.ui.setup.SetupRoute
import io.github.luposolitario.immundanoctisex.util.AccentColor

// Le destinazioni dell'app (le 7 schermate di UI.md). Solo routing qui
// (ARCHITETTURA.md: ~100 righe max): niente logica, niente stato di gioco.
enum class Route {
    HOME,
    ADVENTURE_SETUP,
    CHARACTER_CREATION,
    ADVENTURE,
    CHARACTER_SHEET,
    JOURNAL,
    OPTIONS,
    MODELS,
}

@Composable
fun AppNavigation(
    container: AppContainer,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onThemeOverrideChange: (Boolean?) -> Unit,
    onAccentColorChange: (AccentColor) -> Unit,
) {
    var route by rememberSaveable { mutableStateOf(Route.HOME) }
    val backStack = remember { ArrayDeque<Route>() }
    // Parametri di navigazione della partita in corso (solo routing:
    // la logica vive nelle route delle schermate).
    var newDifficulty by remember { mutableStateOf(Difficulty.NORMAL) }
    var activeSession by remember { mutableStateOf<SessionData?>(null) }

    fun navigateTo(destination: Route) {
        backStack.addLast(route)
        route = destination
    }

    BackHandler(enabled = backStack.isNotEmpty()) {
        route = backStack.removeLast()
    }

    when (route) {
        Route.HOME -> HomeRoute(
            container = container,
            isDarkTheme = isDarkTheme,
            onThemeToggle = onThemeToggle,
            onAdventureClick = { navigateTo(Route.ADVENTURE_SETUP) },
            onModelsClick = { navigateTo(Route.MODELS) },
            onSettingsClick = { navigateTo(Route.OPTIONS) },
        )

        Route.ADVENTURE_SETUP -> SetupRoute(
            container = container,
            onContinueSession = { session ->
                activeSession = session
                navigateTo(Route.ADVENTURE)
            },
            onNewAdventure = { difficulty ->
                newDifficulty = difficulty
                navigateTo(Route.CHARACTER_CREATION)
            },
        )

        Route.CHARACTER_CREATION -> CreationRoute(
            container = container,
            difficulty = newDifficulty,
            onSessionCreated = { session ->
                activeSession = session
                navigateTo(Route.ADVENTURE)
            },
        )

        Route.ADVENTURE -> {
            val session = activeSession
            if (session == null) {
                PlaceholderScreen(route)
            } else {
                AdventureRoute(
                    container = container,
                    session = session,
                    onExitToHome = {
                        activeSession = null
                        backStack.clear()
                        route = Route.HOME
                    },
                )
            }
        }

        Route.MODELS -> ModelsRoute(
            container = container,
            onClose = { if (backStack.isNotEmpty()) route = backStack.removeLast() },
        )

        Route.OPTIONS -> OptionsRoute(
            container = container,
            darkOverride = container.themePreferences.darkOverride,
            onThemeOverrideChange = onThemeOverrideChange,
            onAccentColorChange = onAccentColorChange,
            onModelsClick = { navigateTo(Route.MODELS) },
            onClose = { if (backStack.isNotEmpty()) route = backStack.removeLast() },
        )

        // Segnaposto: si riempiono nei prossimi task della Fase 3/5.
        else -> PlaceholderScreen(route)
    }
}

@Composable
private fun PlaceholderScreen(route: Route) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Schermata $route — in costruzione (Fase 3)")
    }
}
