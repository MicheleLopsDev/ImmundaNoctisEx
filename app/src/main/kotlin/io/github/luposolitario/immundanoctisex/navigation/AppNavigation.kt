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
import io.github.luposolitario.immundanoctisex.ui.home.HomeScreen

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
) {
    var route by rememberSaveable { mutableStateOf(Route.HOME) }
    val backStack = remember { ArrayDeque<Route>() }

    fun navigateTo(destination: Route) {
        backStack.addLast(route)
        route = destination
    }

    BackHandler(enabled = backStack.isNotEmpty()) {
        route = backStack.removeLast()
    }

    when (route) {
        Route.HOME -> HomeScreen(
            isDarkTheme = isDarkTheme,
            onThemeToggle = onThemeToggle,
            onAdventureClick = { navigateTo(Route.ADVENTURE_SETUP) },
            onModelsClick = { navigateTo(Route.MODELS) },
            onSettingsClick = { navigateTo(Route.OPTIONS) },
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
