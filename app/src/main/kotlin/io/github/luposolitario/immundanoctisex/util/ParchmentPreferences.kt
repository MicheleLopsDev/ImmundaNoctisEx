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
// Tre risorse, non una sola (23/07/2026, Michele sul device: "il testo
// sfora... si potrebbe tagliare in due ed avere la parte centrale che si
// allunga"): la pergamena è un ritaglio a bordi strappati su TUTTI e
// quattro i lati, non solo alto/basso — una singola immagine con
// ContentScale.Crop tiene fisso il bordo strappato mentre il testo può
// essere più lungo del riquadro e sforare oltre. `topRes`/`bottomRes`
// tengono lo strappo + gli scudi degli angoli a dimensione fissa;
// `middleRes` è una fascia sottile e neutra (bordi laterali esclusi in
// fase di ritaglio, altrimenti una tacca del bordo si stirerebbe su
// tutta l'altezza centrale) che si allunga in verticale fino
// all'altezza vera del testo — stesso principio di un nine-patch
// Android, fatto con tre `Image` impilate invece di un `.9.png`.
enum class ParchmentStyle(
    val displayName: String,
    // Immagine intera (bordo strappato + scudi): usata come cornice
    // decorativa GRANDE, non allineata pixel-per-pixel al testo (24/07,
    // schizzo di Michele dopo aver bocciato la pila a tre fasce — vedi
    // sotto).
    val fullRes: Int?,
    val topRes: Int?,
    val middleRes: Int?,
    val bottomRes: Int?,
    // Colore pieno dietro alla pila di immagini (23/07/2026, Michele sul
    // device: "svegli" e "verso" finivano su sfondo nero): il bordo
    // strappato ha denti profondi e irregolari su tutti e quattro i lati,
    // alcuni quasi a metà della fascia — tentare di "sanare" i denti più
    // estremi via script ha introdotto righe verticali indesiderate
    // (colore del bordo esteso per errore fino al margine). Molto più
    // robusto: un colore pieno, vicino alla media della texture, dietro a
    // tutto — qualunque dente, per quanto profondo, mostra questo colore
    // invece del nero del tema, a prescindere dalla profondità.
    val baseColor: Color?,
) {
    OFF("Disattivata (default)", null, null, null, null, null),
    AUTO("Automatica (segue il tema)", null, null, null, null, null),
    LIGHT(
        "Pergamena chiara",
        R.drawable.parchment_panel,
        R.drawable.parchment_panel_top,
        R.drawable.parchment_panel_middle,
        R.drawable.parchment_panel_bottom,
        Color(0xFFDEC9AB),
    ),
    DARK(
        "Pergamena scura",
        R.drawable.parchment_panel_dark,
        R.drawable.parchment_panel_dark_top,
        R.drawable.parchment_panel_dark_middle,
        R.drawable.parchment_panel_dark_bottom,
        Color(0xFF554334),
    ),
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
