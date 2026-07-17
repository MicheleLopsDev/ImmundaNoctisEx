package io.github.luposolitario.immundanoctisex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Scheletro Fase 0: single-activity, schermata segnaposto. Le 7 schermate vere
// arrivano in Fase 5 (doc/UI.md); qui si verifica solo che l'app parta.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImmundaNoctisExPlaceholder()
        }
    }
}

@Composable
private fun ImmundaNoctisExPlaceholder() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("ImmundaNoctisEx — Fase 0")
            }
        }
    }
}
