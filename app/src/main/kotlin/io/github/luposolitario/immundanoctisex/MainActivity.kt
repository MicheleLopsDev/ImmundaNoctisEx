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

            ImmundaNoctisTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        container = container,
                        isDarkTheme = darkTheme,
                        onThemeToggle = {
                            darkTheme = !darkTheme
                            container.themePreferences.darkOverride = darkTheme
                        },
                    )
                }
            }
        }
    }
}
