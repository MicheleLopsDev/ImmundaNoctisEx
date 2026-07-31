package io.github.luposolitario.immundanoctisex.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import io.github.luposolitario.immundanoctisex.util.AccentColor

// Tema portato da v1 (ui/theme/Theme.kt): schemi chiaro/scuro completi.
// Niente dynamic color: l'estetica del libro-game è la stessa su ogni
// device. La tipografia resta Material default fino alla Fase 7 (l'opzione
// font di UI.md arriverà lì).
//
// primary/onPrimary/primaryContainer/onPrimaryContainer sono ora
// PARAMETRI (richiesta Michele 21/07/2026, "una selezione dei colori"
// dopo aver visto la card di stato): il resto della palette — secondary,
// tertiary, error, superfici — resta fisso per ogni scelta, cambia solo
// l'accento. I valori di default sono quelli di sempre (AccentColor.BLUE).
private fun darkScheme(accent: AccentColor) = darkColorScheme(
    primary = accent.darkPrimary,
    onPrimary = accent.darkOnPrimary,
    primaryContainer = accent.darkPrimaryContainer,
    onPrimaryContainer = accent.darkOnPrimaryContainer,
    secondary = Color(0xFFBCC6DC),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F8),
    tertiary = Color(0xFFD7BEE4),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF3DAFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
)

private fun lightScheme(accent: AccentColor) = lightColorScheme(
    primary = accent.lightPrimary,
    onPrimary = accent.lightOnPrimary,
    primaryContainer = accent.lightPrimaryContainer,
    onPrimaryContainer = accent.lightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
)

// Tutto il testo in grassetto (01/08/2026, Michele: su alcuni sfondi
// illustrati la parte alta dei testi normali non si legge bene) — invece
// di toccare ogni singolo Text() in giro per la UI, si ridefinisce QUI la
// tipografia di default: ogni composable che legge
// MaterialTheme.typography.qualcosa (la stragrande maggioranza, senza
// override) diventa bold da solo. I pochi Text() che già forzavano
// fontWeight = FontWeight.Bold a mano restano bold, nessun conflitto.
private val BoldTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Bold),
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold),
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontWeight = FontWeight.Bold),
        bodyMedium = base.bodyMedium.copy(fontWeight = FontWeight.Bold),
        bodySmall = base.bodySmall.copy(fontWeight = FontWeight.Bold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Bold),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
fun ImmundaNoctisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: AccentColor = AccentColor.BLUE,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkScheme(accentColor) else lightScheme(accentColor),
        typography = BoldTypography,
        content = content,
    )
}
