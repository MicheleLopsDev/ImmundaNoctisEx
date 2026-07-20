package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.text.font.FontFamily

// Font del testo di lettura (UI.md schermata 7, richiesta Michele
// 17/07/2026: "una rosa di font — la serif di default della pagina di
// libro più alternative"). Solo famiglie di sistema (Serif/SansSerif/
// Monospace/Cursive): zero asset da scaricare, disponibili su ogni
// device. Un font custom si aggiunge qui il giorno che serve davvero.
enum class ReadingFont(val displayName: String, val family: FontFamily) {
    SERIF("Serif (default)", FontFamily.Serif),
    SANS_SERIF("Sans serif", FontFamily.SansSerif),
    MONOSPACE("Monospace", FontFamily.Monospace),
    CURSIVE("Corsivo", FontFamily.Cursive),
}

class FontPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var readingFont: ReadingFont
        get() = prefs.getString(KEY_FONT, null)
            ?.let { name -> runCatching { ReadingFont.valueOf(name) }.getOrNull() }
            ?: ReadingFont.SERIF
        set(value) = prefs.edit().putString(KEY_FONT, value.name).apply()

    private companion object {
        const val PREFS_NAME = "font_preferences"
        const val KEY_FONT = "reading_font"
    }
}
