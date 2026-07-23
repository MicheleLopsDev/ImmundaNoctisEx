package io.github.luposolitario.immundanoctisex.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.luposolitario.immundanoctisex.R
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme

// Home a riquadri come v1 (UI.md §schermata 1, senza le tile STDF):
// Avventura, Modelli LLM, Impostazioni. Componente stateless: dati in
// ingresso, eventi in uscita — convenzione @Preview (ANALISI-UI-V1.md).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onAdventureClick: () -> Unit,
    onModelsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    // Side-load (20/07/2026, richiesta urgente di Michele per i test —
    // "devo poter caricare vari file"): il titolo del libro ATTUALMENTE
    // caricato, cosi' si sa sempre cosa si sta testando, + l'esito
    // dell'ultimo caricamento (successo o errore, null = nessuno ancora).
    currentBookTitle: String,
    onLoadBookClick: () -> Unit,
    loadFeedback: String? = null,
    loadFeedbackIsError: Boolean = false,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Immunda Noctis Ex") },
                actions = {
                    IconButton(onClick = onLoadBookClick) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Carica libro",
                        )
                    }
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambia tema",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Figura intera dei due eroi (24/07/2026, richiesta Michele:
            // "vorrei che nel menu ci fosse la figura intera" — a
            // differenza dei busti circolari usati altrove, qui va
            // mostrata per intero: ContentScale.Fit, mai Crop).
            Image(
                painter = painterResource(id = R.drawable.hero_banner),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(220.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Libro: $currentBookTitle",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (loadFeedback != null) {
                Text(
                    loadFeedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (loadFeedbackIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                MenuTile(icon = Icons.Default.SportsEsports, label = "Avventura", onClick = onAdventureClick)
                MenuTile(icon = Icons.Default.Psychology, label = "Modelli LLM", onClick = onModelsClick)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                MenuTile(icon = Icons.Default.Settings, label = "Impostazioni", onClick = onSettingsClick)
            }
        }
    }
}

// La tile del menu (MenuIcon di v1, senza tooltip: il nome è già sotto).
@Composable
private fun MenuTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, name = "Home (scuro)")
@Composable
private fun HomeScreenDarkPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        HomeScreen(
            isDarkTheme = true,
            onThemeToggle = {},
            onAdventureClick = {},
            onModelsClick = {},
            onSettingsClick = {},
            currentBookTitle = "The Warehouse Letter",
            onLoadBookClick = {},
            loadFeedback = "Libro caricato: Il vecchio magazzino",
        )
    }
}

@Preview(showBackground = true, name = "Home — errore caricamento")
@Composable
private fun HomeScreenLoadErrorPreview() {
    ImmundaNoctisTheme(darkTheme = true) {
        HomeScreen(
            isDarkTheme = true,
            onThemeToggle = {},
            onAdventureClick = {},
            onModelsClick = {},
            onSettingsClick = {},
            currentBookTitle = "The Warehouse Letter",
            onLoadBookClick = {},
            loadFeedback = "JSON malformato: riga 12",
            loadFeedbackIsError = true,
        )
    }
}

@Preview(showBackground = true, name = "Home (chiaro)")
@Composable
private fun HomeScreenLightPreview() {
    ImmundaNoctisTheme(darkTheme = false) {
        HomeScreen(
            isDarkTheme = false,
            onThemeToggle = {},
            onAdventureClick = {},
            currentBookTitle = "The Warehouse Letter",
            onLoadBookClick = {},
            onModelsClick = {},
            onSettingsClick = {},
        )
    }
}
