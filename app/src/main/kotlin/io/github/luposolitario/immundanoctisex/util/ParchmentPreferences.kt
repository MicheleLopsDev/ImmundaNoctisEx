package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import io.github.luposolitario.immundanoctisex.R

// Stile pergamena per il Diario di Combattimento (22/07/2026, richiesta
// Michele, dal piano di reskin — "potremmo far scegliere nelle
// opzioni?"): non un semplice interruttore, una vera scelta a tre, come
// AccentColor. OFF di default: chi non la vuole resta sul Material3
// piatto di oggi, nessuna sorpresa per chi già gioca.
//
// Un solo colore d'inchiostro per ENTRAMBE le varianti (INK, sotto): la
// pergamena chiara e quella scura sono comunque più chiare del testo
// scuro forzato sopra — stesso principio del registro cartaceo, dove
// l'inchiostro è sempre nero a prescindere dal colore della pagina.
enum class ParchmentStyle(val displayName: String, val drawableRes: Int?) {
    OFF("Disattivata (default)", null),
    LIGHT("Pergamena chiara", R.drawable.parchment_panel),
    DARK("Pergamena scura", R.drawable.parchment_panel_dark),
    ;

    companion object {
        // Colore d'inchiostro forzato quando lo stile è attivo: il tema
        // scuro dell'app userebbe testo chiaro, illeggibile su un fondo
        // di pergamena chiaro o scuro ma comunque più chiaro del nero.
        val INK = Color(0xFF2A1F14)
    }
}

class ParchmentPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var style: ParchmentStyle
        get() = prefs.getString(KEY_STYLE, null)
            ?.let { name -> runCatching { ParchmentStyle.valueOf(name) }.getOrNull() }
            ?: ParchmentStyle.OFF
        set(value) = prefs.edit().putString(KEY_STYLE, value.name).apply()

    private companion object {
        const val PREFS_NAME = "parchment_preferences"
        const val KEY_STYLE = "parchment_style"
    }
}
