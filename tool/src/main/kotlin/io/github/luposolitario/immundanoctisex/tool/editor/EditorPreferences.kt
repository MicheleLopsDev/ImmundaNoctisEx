package io.github.luposolitario.immundanoctisex.tool.editor

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import io.github.luposolitario.immundanoctisex.core.engine.inference.LinguaOutput
import java.io.File
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

    // Livelli di backup (§17.5, Michele: "potremmo decidere quanti
    // livelli di backup vogliamo io partirei da 3 fino ad un massimo di
    // 9"): coerceIn ad ogni lettura/scrittura, così un valore corrotto
    // scritto da una versione futura/precedente non sfugge mai
    // dall'intervallo dichiarato.
    var livelliBackup: Int
        get() = prefs.getInt(KEY_LIVELLI_BACKUP, MAX_BACKUP_DEFAULT).coerceIn(MAX_BACKUP_MIN, MAX_BACKUP_MAX)
        set(value) = prefs.putInt(KEY_LIVELLI_BACKUP, value.coerceIn(MAX_BACKUP_MIN, MAX_BACKUP_MAX))

    // Libri recenti (§17.4): un solo valore stringa con i percorsi
    // separati da "\n" — Preferences non ha un tipo lista nativo, e un
    // percorso di file non contiene mai un ritorno a capo. Il più
    // recente è sempre il primo.
    var libriRecenti: List<String>
        get() = prefs.get(KEY_LIBRI_RECENTI, "").split("\n").filter { it.isNotBlank() }
        private set(value) = prefs.put(KEY_LIBRI_RECENTI, value.joinToString("\n"))

    fun aggiungiLibroRecente(percorso: String) {
        libriRecenti = (listOf(percorso) + libriRecenti.filterNot { it == percorso }).take(MAX_LIBRI_RECENTI)
    }

    // Percorso del file .litertlm scelto dall'autore (02/08/2026): il
    // modello NON viaggia con l'editor (3,7 GB per il 4B), si punta a
    // quello già sul disco. Vuoto finché non se ne sceglie uno: le
    // funzioni che dipendono dal modello restano semplicemente spente.
    var percorsoModello: String
        get() = prefs.get(KEY_PERCORSO_MODELLO, "")
        set(value) = prefs.put(KEY_PERCORSO_MODELLO, value)

    // Dove finiscono i modelli scaricati dall'editor (02/08/2026). Di
    // default una cartella sotto la home dell'utente: file da GB non si
    // mettono accanto ai libri né dentro la cartella del programma.
    var cartellaModelli: String
        get() = prefs.get(KEY_CARTELLA_MODELLI, "")
            .ifBlank { File(System.getProperty("user.home"), "ImmundaNoctisEx/modelli").absolutePath }
        set(value) = prefs.put(KEY_CARTELLA_MODELLI, value)

    // Lingua in cui il modello traduce o riscrive (02/08/2026, Michele:
    // "il selettore delle lingue come una proprietà degli llm"): sta
    // accanto alle altre impostazioni del modello, non fra quelle
    // dell'editor, perché descrive COME genera — non come si presenta
    // il programma.
    var linguaOutput: LinguaOutput
        get() = LinguaOutput.daNome(prefs.get(KEY_LINGUA_OUTPUT, null))
        set(value) = prefs.put(KEY_LINGUA_OUTPUT, value.name)

    // Le stesse due modalità del client (InferencePreferences.
    // translationMode): traduzione fedele oppure arricchimento. Il preset
    // dei parametri lo sceglie InferenceConfig, non questa preferenza —
    // qui si registra solo COSA si vuole vedere.
    var modalitaTraduzione: Boolean
        get() = prefs.getBoolean(KEY_MODALITA_TRADUZIONE, true)
        set(value) = prefs.putBoolean(KEY_MODALITA_TRADUZIONE, value)

    private companion object {
        const val KEY_TEMA_SCURO = "tema_scuro"
        const val KEY_FONT = "font"
        const val KEY_SCALA_TESTO = "scala_testo"
        const val KEY_LIVELLI_BACKUP = "livelli_backup"
        const val KEY_LIBRI_RECENTI = "libri_recenti"
        const val KEY_PERCORSO_MODELLO = "percorso_modello"
        const val KEY_CARTELLA_MODELLI = "cartella_modelli"
        const val KEY_LINGUA_OUTPUT = "lingua_output"
        const val KEY_MODALITA_TRADUZIONE = "modalita_traduzione"
        const val MAX_LIBRI_RECENTI = 8
    }
}
