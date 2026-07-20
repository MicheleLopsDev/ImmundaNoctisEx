package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import io.github.luposolitario.immundanoctisex.R

// Font del testo di lettura (UI.md schermata 7, richiesta Michele
// 17/07/2026: "una rosa di font — la serif di default della pagina di
// libro più alternative").
//
// PRIMA VERSIONE (21/07/2026) usava le famiglie generiche di sistema
// (FontFamily.Serif/SansSerif/Monospace/Cursive) per zero asset — bug
// trovato da Michele giocando: sul suo device le quattro apparivano
// TUTTE IDENTICHE. Le famiglie generiche non garantiscono un typeface
// distinto su ogni produttore Android, si è rivelata un'assunzione
// sbagliata. Sostituite con 4 font veri (Google Fonts, licenza OFL,
// scaricati da github.com/google/fonts) impacchettati in res/font/:
// stesso rendering garantito ovunque, offline, senza dipendere da cosa
// il telefono ha installato.
enum class ReadingFont(val displayName: String, val family: FontFamily) {
    SERIF("Serif (default) — Lora", FontFamily(Font(R.font.lora))),
    SANS_SERIF("Sans serif — Inter", FontFamily(Font(R.font.inter))),
    MONOSPACE("Monospace — Roboto Mono", FontFamily(Font(R.font.roboto_mono))),
    CURSIVE("Corsivo — Caveat", FontFamily(Font(R.font.caveat))),
}

// Grandezza del testo di lettura (richiesta Michele 21/07/2026: un
// pulsante con una lente nell'header, accanto all'icona Home, che
// cicla la taglia — non una schermata a parte). Moltiplicatore sul
// bodyLarge di Material, non una dimensione assoluta: segue comunque
// eventuali cambi di tema tipografico futuri.
enum class TextScale(val multiplier: Float, val icon: String) {
    SMALL(0.85f, "A-"),
    MEDIUM(1f, "A"),
    LARGE(1.2f, "A+"),
    ;

    fun next(): TextScale = entries[(ordinal + 1) % entries.size]
}

class FontPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var readingFont: ReadingFont
        get() = prefs.getString(KEY_FONT, null)
            ?.let { name -> runCatching { ReadingFont.valueOf(name) }.getOrNull() }
            ?: ReadingFont.SERIF
        set(value) = prefs.edit().putString(KEY_FONT, value.name).apply()

    var textScale: TextScale
        get() = prefs.getString(KEY_TEXT_SCALE, null)
            ?.let { name -> runCatching { TextScale.valueOf(name) }.getOrNull() }
            ?: TextScale.MEDIUM
        set(value) = prefs.edit().putString(KEY_TEXT_SCALE, value.name).apply()

    private companion object {
        const val PREFS_NAME = "font_preferences"
        const val KEY_FONT = "reading_font"
        const val KEY_TEXT_SCALE = "text_scale"
    }
}
