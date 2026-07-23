package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

// Sfondo della card di stato (UI.md §Card di stato — richiesta Michele
// 21/07/2026, dopo il colore d'accento: "un altro picker per la barra
// di sotto", intendendo proprio questa card, non la navigation bar di
// sistema). DEFAULT (background = null) non tocca nulla: la card resta
// il colore di superficie standard di Material, quello di sempre. Gli
// altri preset sono pastelli chiari con un contentColor scuro abbinato
// ESPLICITO — la card diventerebbe un'isola chiara dentro un tema
// scuro, quindi il testo/le icone che ereditano il colore di contenuto
// devono restare leggibili a prescindere dal tema attivo, non solo
// "quello che viene" da Material.
enum class StatusCardColor(val displayName: String, val background: Color?, val content: Color?) {
    DEFAULT("Come il tema (default)", null, null),
    LAVENDER("Lavanda", Color(0xFFE8DEF8), Color(0xFF1D192B)),
    SKY("Azzurro", Color(0xFFD8E4F5), Color(0xFF16223A)),
    MINT("Menta", Color(0xFFD9F2E3), Color(0xFF0F3324)),
    AMBER("Ambra", Color(0xFFFCE8C7), Color(0xFF3E2E00)),
    ROSE("Rosa", Color(0xFFF9DDE3), Color(0xFF3E1622)),
}

// Default non più "come il tema" alla lettera (24/07/2026, richiesta
// Michele: prima coincideva con la superficie standard di Material, la
// stessa del riquadro di lettura — "cambia il default, non sarà più
// uguale a quello di visualizzazione del testo"): blu navy in tema
// chiaro, marroncino in tema scuro. Solo IL DEFAULT cambia — gli altri
// preset (personalizzazione in Opzioni) restano quello che erano,
// invariati.
//
// Marroncino SCURO, non chiaro (24/07/2026, stesso giorno, Michele sul
// device: "un colore di default per il tema scuro che non si legge
// chiaramente" — un riquadro chiaro e caldo dentro un tema scuro
// spiccava come una toppa fuori posto, invece di un vero difetto di
// contrasto). Invertito: sfondo cuoio scuro + testo crema, si comporta
// da superficie scura in mezzo alle altre invece di stonare.
private val DEFAULT_LIGHT_BG = Color(0xFF1E2A4A)
private val DEFAULT_LIGHT_CONTENT = Color(0xFFE9EDF5)
private val DEFAULT_DARK_BG = Color(0xFF3D2B1F)
private val DEFAULT_DARK_CONTENT = Color(0xFFEFE0C9)

// Da chiamare al posto di leggere `background`/`content` direttamente
// quando serve il colore EFFETTIVO da disegnare: per DEFAULT dipende dal
// tema, per tutti gli altri preset è lo stesso valore fisso di sempre.
fun StatusCardColor.resolvedBackground(isDarkTheme: Boolean): Color? =
    if (this == StatusCardColor.DEFAULT) {
        if (isDarkTheme) DEFAULT_DARK_BG else DEFAULT_LIGHT_BG
    } else {
        background
    }

fun StatusCardColor.resolvedContent(isDarkTheme: Boolean): Color? =
    if (this == StatusCardColor.DEFAULT) {
        if (isDarkTheme) DEFAULT_DARK_CONTENT else DEFAULT_LIGHT_CONTENT
    } else {
        content
    }

class StatusCardColorPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var statusCardColor: StatusCardColor
        get() = prefs.getString(KEY_COLOR, null)
            ?.let { name -> runCatching { StatusCardColor.valueOf(name) }.getOrNull() }
            ?: StatusCardColor.DEFAULT
        set(value) = prefs.edit().putString(KEY_COLOR, value.name).apply()

    private companion object {
        const val PREFS_NAME = "status_card_color_preferences"
        const val KEY_COLOR = "status_card_color"
    }
}
