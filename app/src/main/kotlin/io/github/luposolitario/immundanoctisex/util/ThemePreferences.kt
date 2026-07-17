package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences

// Preferenza tema (pattern ThemePreferences di v1): tre stati — segui il
// sistema (default), chiaro forzato, scuro forzato. La scelta vive in
// Opzioni (UI.md); il toggle rapido in Home decide solo l'override.
class ThemePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // null = segui il sistema.
    var darkOverride: Boolean?
        get() = if (!prefs.contains(KEY_DARK)) null else prefs.getBoolean(KEY_DARK, false)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_DARK) else putBoolean(KEY_DARK, value)
        }.apply()

    fun useDarkTheme(systemDefault: Boolean): Boolean = darkOverride ?: systemDefault

    private companion object {
        const val PREFS_NAME = "theme_preferences"
        const val KEY_DARK = "dark_override"
    }
}
