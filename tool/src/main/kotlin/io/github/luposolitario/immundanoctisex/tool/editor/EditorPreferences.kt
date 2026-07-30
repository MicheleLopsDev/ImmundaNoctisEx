package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import java.util.prefs.Preferences

// Font importati dal client (30/07/2026, Michele: "voglio che importi
// dal client le font disponibili") — stessi 4 file .ttf di
// FontPreferences.kt (:app, Google Fonts, licenza OFL), copiati in
// tool/src/main/resources/fonts/ così l'editor li usa senza dipendere
// da cosa il PC ha installato — stessa idea di StaticResourceCatalog
// per le immagini (§15.1).
enum class FontEditor(val displayName: String, val file: String) {
    ALMENDRA("Calligrafico (default) — Almendra", "almendra.ttf"),
    CINZEL("Imperiale — Cinzel Decorative", "cinzel_decorative.ttf"),
    MEDIEVAL_SHARP("Gotico — MedievalSharp", "medieval_sharp.ttf"),
    UNCIAL("Onciale — Uncial Antiqua", "uncial_antiqua.ttf"),
}

fun caricaFontFamily(font: FontEditor): FontFamily {
    val bytes = font.javaClass.classLoader.getResourceAsStream("fonts/${font.file}")?.readBytes()
        ?: return FontFamily.Default
    return FontFamily(Font("fonts/${font.file}", bytes))
}

// Applica il font scelto a TUTTI gli stili di Typography (30/07/2026):
// MaterialTheme(typography = ...) non basta da solo, ogni Text(style =
// MaterialTheme.typography.X) porta già un fontFamily non-null nel suo
// TextStyle di base, che vince su qualunque ambiente/LocalTextStyle —
// va quindi sovrascritto stile per stile, non a livello globale.
fun tipografiaConFont(fontFamily: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily),
    )
}

// Grandezza dei caratteri (30/07/2026, Michele: "si deve poter
// aumentare e diminuire la grandezza dei caratteri") — moltiplicatore
// applicato a `LocalDensity.fontScale` (§ vedi EditorMain.kt), stessa
// tecnica dell'impostazione di accessibilità di Android: scala TUTTI i
// testi insieme senza dover toccare ogni singolo Text() dell'editor.
enum class ScalaTesto(val moltiplicatore: Float, val etichetta: String) {
    PICCOLO(1f, "A-"),
    MEDIO(1.15f, "A"),
    GRANDE(1.35f, "A+"),
    ;

    fun successivo(): ScalaTesto = entries[(ordinal + 1).coerceAtMost(entries.size - 1)]
    fun precedente(): ScalaTesto = entries[(ordinal - 1).coerceAtLeast(0)]
}

// Persistenza (30/07/2026, Michele: "il tema selezionato che deve
// essere persistente") — `java.util.prefs.Preferences`, già nella JDK
// di base (su Windows usa il registro utente corrente), nessuna
// dipendenza in più: stesso principio delle SharedPreferences che il
// client usa per le proprie (NarrativeTonePreferences/FontPreferences),
// ma senza Context Android.
class EditorPreferences {
    private val prefs = Preferences.userRoot().node("io/github/luposolitario/immundanoctisex/tool/editor")

    var temaScuro: Boolean
        get() = prefs.getBoolean(KEY_TEMA_SCURO, false)
        set(value) = prefs.putBoolean(KEY_TEMA_SCURO, value)

    var font: FontEditor
        get() = runCatching { FontEditor.valueOf(prefs.get(KEY_FONT, FontEditor.ALMENDRA.name)) }.getOrDefault(FontEditor.ALMENDRA)
        set(value) = prefs.put(KEY_FONT, value.name)

    var scalaTesto: ScalaTesto
        get() = runCatching { ScalaTesto.valueOf(prefs.get(KEY_SCALA_TESTO, ScalaTesto.MEDIO.name)) }.getOrDefault(ScalaTesto.MEDIO)
        set(value) = prefs.put(KEY_SCALA_TESTO, value.name)

    private companion object {
        const val KEY_TEMA_SCURO = "tema_scuro"
        const val KEY_FONT = "font"
        const val KEY_SCALA_TESTO = "scala_testo"
    }
}
