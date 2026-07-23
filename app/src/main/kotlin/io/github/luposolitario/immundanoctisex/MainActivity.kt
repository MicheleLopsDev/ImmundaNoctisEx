package io.github.luposolitario.immundanoctisex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.luposolitario.immundanoctisex.navigation.AppNavigation
import io.github.luposolitario.immundanoctisex.ui.theme.ImmundaNoctisTheme
import io.github.luposolitario.immundanoctisex.util.AccentColor

// Single-activity (ARCHITETTURA.md): la MainActivity costruisce
// l'AppContainer e monta il routing; tutto il resto sono composable.
class MainActivity : ComponentActivity() {

    private val container by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkTheme by remember {
                mutableStateOf(container.themePreferences.useDarkTheme(systemDark))
            }
            // Colore d'accento (Michele 21/07/2026, "una selezione dei
            // colori" vista la card di stato): stesso pattern del tema,
            // stato qui perché deve applicarsi SUBITO senza riavviare.
            var accentColor by remember {
                mutableStateOf(container.accentColorPreferences.accentColor)
            }

            ImmundaNoctisTheme(darkTheme = darkTheme, accentColor = accentColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        container = container,
                        isDarkTheme = darkTheme,
                        onThemeToggle = {
                            darkTheme = !darkTheme
                            container.themePreferences.darkOverride = darkTheme
                        },
                        // Le Opzioni offrono un terzo stato ("segui il
                        // sistema", override = null) che il toggle rapido
                        // di Home non ha bisogno di esprimere.
                        onThemeOverrideChange = { override ->
                            darkTheme = override ?: systemDark
                            container.themePreferences.darkOverride = override
                        },
                        onAccentColorChange = { selected ->
                            accentColor = selected
                            container.accentColorPreferences.accentColor = selected
                        },
                    )
                }
            }
        }
    }

    // BUG (24/07/2026, Michele: "anche se chiusa continuavo a sentire la
    // musica... ho dovuto riavviare il telefono"): MusicPlayer vive a
    // scope applicazione (un MediaPlayer grezzo, nessun Service, nessuna
    // notifica) apposta per sopravvivere alla navigazione tra schermate —
    // di suo, chiudendo l'app il processo morirebbe e la musica con lui.
    // Ma un download in corso usa un Foreground Service (giustamente, i
    // download DEVONO sopravvivere in background) che tiene in vita
    // l'INTERO processo — musica compresa, che invece non ha alcun
    // motivo di restare accesa. onDestroy la ferma esplicitamente:
    // quando l'utente chiude davvero l'app (swipe dai recenti), Android
    // chiama sempre onDestroy sull'Activity, A PRESCINDERE dal fatto che
    // il processo sopravviva per il download. Se l'Activity viene
    // ricreata (rotazione) si prende una AppContainer/MusicPlayer nuovi
    // di zecca (by lazy sull'istanza Activity, non condiviso).
    override fun onDestroy() {
        container.musicPlayer.release()
        super.onDestroy()
    }
}
