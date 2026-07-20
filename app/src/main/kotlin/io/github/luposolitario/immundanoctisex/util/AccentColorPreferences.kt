package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

// Colore d'accento dell'app (UI.md schermata 7, richiesta Michele
// 21/07/2026, dopo aver visto la card di stato: "voglio una selezione
// dei colori"). Non un color picker RGB libero — una rosa di preset,
// come per il font: ogni voce porta la coppia primary/onPrimary/
// primaryContainer/onPrimaryContainer già bilanciata per contrasto, sia
// per il tema scuro sia per il chiaro. BLUE è il colore attuale
// dell'app (Theme.kt), invariato: sceglierlo non cambia nulla.
enum class AccentColor(
    val displayName: String,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
) {
    BLUE(
        "Blu (default)",
        darkPrimary = Color(0xFFA8C8FF), darkOnPrimary = Color(0xFF00325A),
        darkPrimaryContainer = Color(0xFF00497D), darkOnPrimaryContainer = Color(0xFFD4E3FF),
        lightPrimary = Color(0xFF0061A4), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFD1E4FF), lightOnPrimaryContainer = Color(0xFF001D36),
    ),
    GOLD(
        "Oro",
        darkPrimary = Color(0xFFFFD54F), darkOnPrimary = Color(0xFF3E2E00),
        darkPrimaryContainer = Color(0xFF5A4300), darkOnPrimaryContainer = Color(0xFFFFE08C),
        lightPrimary = Color(0xFF7C5800), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFFFDDA1), lightOnPrimaryContainer = Color(0xFF261A00),
    ),
    GREEN(
        "Verde",
        darkPrimary = Color(0xFFA6D785), darkOnPrimary = Color(0xFF0F3900),
        darkPrimaryContainer = Color(0xFF245200), darkOnPrimaryContainer = Color(0xFFC2F1A0),
        lightPrimary = Color(0xFF3C6B00), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFBBF191), lightOnPrimaryContainer = Color(0xFF0F2000),
    ),
    CORAL(
        "Corallo",
        darkPrimary = Color(0xFFFFB4A0), darkOnPrimary = Color(0xFF5F1600),
        darkPrimaryContainer = Color(0xFF852200), darkOnPrimaryContainer = Color(0xFFFFDBCF),
        lightPrimary = Color(0xFFA53E00), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFFFFDBCF), lightOnPrimaryContainer = Color(0xFF360F00),
    ),
    TEAL(
        "Turchese",
        darkPrimary = Color(0xFF4FD8EB), darkOnPrimary = Color(0xFF00363D),
        darkPrimaryContainer = Color(0xFF004F58), darkOnPrimaryContainer = Color(0xFF97F0FF),
        lightPrimary = Color(0xFF006874), lightOnPrimary = Color(0xFFFFFFFF),
        lightPrimaryContainer = Color(0xFF97F0FF), lightOnPrimaryContainer = Color(0xFF001F24),
    ),
}

class AccentColorPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var accentColor: AccentColor
        get() = prefs.getString(KEY_ACCENT, null)
            ?.let { name -> runCatching { AccentColor.valueOf(name) }.getOrNull() }
            ?: AccentColor.BLUE
        set(value) = prefs.edit().putString(KEY_ACCENT, value.name).apply()

    private companion object {
        const val PREFS_NAME = "accent_color_preferences"
        const val KEY_ACCENT = "accent_color"
    }
}
