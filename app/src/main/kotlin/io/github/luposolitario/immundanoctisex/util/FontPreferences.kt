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
//
// SOSTITUITI di nuovo (22/07/2026, Michele: quattro font "fantasy
// classico" trovati su Google Fonts, "puoi scaricarle e sostituirle
// alle font esistenti") — stessa fonte (github.com/google/fonts, OFL),
// stesso meccanismo, contenuto diverso: da font generici leggibili a
// font a tema Lupo Solitario/medievale. Scelta Almendra (non la
// variante Display) come default: Michele stesso ha notato che la
// Display "non è l'ideale per testi lunghi" mentre la normale lo è —
// qui il font copre il testo di lettura vero, non solo i titoli.
enum class ReadingFont(val displayName: String, val family: FontFamily) {
    ALMENDRA("Calligrafico (default) — Almendra", FontFamily(Font(R.font.almendra))),
    CINZEL("Imperiale — Cinzel Decorative", FontFamily(Font(R.font.cinzel_decorative))),
    MEDIEVAL_SHARP("Gotico — MedievalSharp", FontFamily(Font(R.font.medieval_sharp))),
    UNCIAL("Onciale — Uncial Antiqua", FontFamily(Font(R.font.uncial_antiqua))),
}

// Grandezza del testo di lettura (richiesta Michele 21/07/2026: un
// pulsante con una lente nell'header, accanto all'icona Home, che
// cicla la taglia — non una schermata a parte). Moltiplicatore sul
// bodyLarge di Material, non una dimensione assoluta: segue comunque
// eventuali cambi di tema tipografico futuri.
//
// Valori alzati lo stesso giorno ("aumenta le dimensioni del font"):
// la prima terna (0.85/1/1.2) partiva dal bodyLarge di Material com'è
// — troppo vicina alla taglia normale per sentirsi davvero un
// cambiamento quando si tocca il pulsante.
enum class TextScale(val multiplier: Float, val icon: String) {
    SMALL(1f, "A-"),
    MEDIUM(1.25f, "A"),
    LARGE(1.5f, "A+"),
    ;

    fun next(): TextScale = entries[(ordinal + 1) % entries.size]
}

class FontPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var readingFont: ReadingFont
        get() = prefs.getString(KEY_FONT, null)
            ?.let { name -> runCatching { ReadingFont.valueOf(name) }.getOrNull() }
            ?: ReadingFont.ALMENDRA
        set(value) = prefs.edit().putString(KEY_FONT, value.name).apply()

    var textScale: TextScale
        get() = prefs.getString(KEY_TEXT_SCALE, null)
            ?.let { name -> runCatching { TextScale.valueOf(name) }.getOrNull() }
            ?: TextScale.MEDIUM
        set(value) = prefs.edit().putString(KEY_TEXT_SCALE, value.name).apply()

    // Grassetto (richiesta Michele 21/07/2026): un interruttore in più
    // nella stessa card del font, non una scelta a sé.
    var boldText: Boolean
        get() = prefs.getBoolean(KEY_BOLD, false)
        set(value) = prefs.edit().putBoolean(KEY_BOLD, value).apply()

    private companion object {
        const val PREFS_NAME = "font_preferences"
        const val KEY_FONT = "reading_font"
        const val KEY_TEXT_SCALE = "text_scale"
        const val KEY_BOLD = "bold_text"
    }
}
