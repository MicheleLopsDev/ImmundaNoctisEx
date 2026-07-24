package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import io.github.luposolitario.immundanoctisex.R

// Stile pergamena per il Diario di Combattimento (22/07/2026, richiesta
// Michele, dal piano di reskin — "potremmo far scegliere nelle
// opzioni?"; 23/07/2026, AUTO aggiunta su richiesta di Michele: "auto
// mode... sceglie chiaro scuro a seconda del tema del sistema, oppure
// selezioni indipendentemente e in quel caso adatti i colori"). OFF di
// default: chi non la vuole resta sul Material3 piatto di oggi, nessuna
// sorpresa per chi già gioca. AUTO segue il tema effettivo dell'app
// (`isDarkTheme` da `ThemePreferences.useDarkTheme`, non il solo
// sistema: se Michele ha forzato un tema nelle Opzioni, AUTO rispetta
// quella scelta). LIGHT/DARK restano selezionabili a prescindere dal
// tema.
//
// `middleRes` è la fascia neutra usata come sfondo del riquadro di testo
// dentro `NarrationParchmentPanel` (bordi laterali esclusi in fase di
// ritaglio, altrimenti una tacca del bordo si stirerebbe). `topRes`/
// `bottomRes` della vecchia pila a tre fasce sono stati rimossi
// (24/07/2026): servivano solo a `ParchmentBackground.kt`
// (`CombatDiaryPanel`), cancellato quando Michele ha chiesto di togliere
// del tutto lo sfondo dal Diario di Combattimento ("nessuna pergamena e
// basta").
enum class ParchmentStyle(
    val displayName: String,
    // Immagine intera (bordo strappato + scudi): usata come cornice
    // decorativa GRANDE, non allineata pixel-per-pixel al testo (24/07,
    // schizzo di Michele dopo aver bocciato la pila a tre fasce — vedi
    // sotto).
    val fullRes: Int?,
    val middleRes: Int?,
) {
    OFF("Disattivata (default)", null, null),
    AUTO("Automatica (segue il tema)", null, null),
    LIGHT("Pergamena chiara", R.drawable.parchment_panel, R.drawable.parchment_panel_middle),
    DARK("Pergamena scura", R.drawable.parchment_panel_dark, R.drawable.parchment_panel_dark_middle),
    ;

    companion object {
        // Due inchiostri, non uno solo (BUG corretto 23/07/2026: prima
        // un unico INK scuro veniva forzato anche sulla pergamena
        // scura — testo scuro su fondo marrone scuro, illeggibile.
        // La pergamena chiara vuole inchiostro scuro come un registro
        // vero; quella scura vuole un chiaro da pergamena vecchia, non
        // il bianco piatto del tema scuro dell'app).
        val INK_ON_LIGHT = Color(0xFF2A1F14)
        val INK_ON_DARK = Color(0xFFE8DCC5)
    }
}

// AUTO si risolve nella scelta concreta più adatta al tema IN USO in
// quel momento (non nel solo sistema, vedi sopra); OFF/LIGHT/DARK
// restano quello che sono, la scelta esplicita vince sempre sul tema.
fun ParchmentStyle.resolved(isDarkTheme: Boolean): ParchmentStyle =
    if (this == ParchmentStyle.AUTO) {
        if (isDarkTheme) ParchmentStyle.DARK else ParchmentStyle.LIGHT
    } else {
        this
    }

// Da chiamare SEMPRE su uno stile già risolto (mai su AUTO: non ha un
// drawable/inchiostro propri, solo OFF/LIGHT/DARK ce l'hanno).
fun ParchmentStyle.inkColor(): Color =
    if (this == ParchmentStyle.DARK) ParchmentStyle.INK_ON_DARK else ParchmentStyle.INK_ON_LIGHT

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
