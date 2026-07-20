package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

// Lingua in cui Gemma riscrive la scena (UI.md schermata 7). Il valore
// è il testo libero che finisce nel prompt inglese ("Rewrite... in
// {user_language}"): l'inglese è la lingua del NOME, non una traduzione
// da fare — Gemma segue meglio istruzioni in inglese anche per output
// italiano (PromptFragments.kt). `locale` serve al TTS (Tappa 2): la
// voce deve parlare la stessa lingua del testo, non sempre italiano.
enum class OutputLanguage(val displayName: String, val promptValue: String, val locale: Locale) {
    ITALIAN("Italiano", "Italian", Locale.ITALIAN),
    ENGLISH("English", "English", Locale.ENGLISH),
    SPANISH("Español", "Spanish", Locale("es")),
    FRENCH("Français", "French", Locale.FRENCH),
    GERMAN("Deutsch", "German", Locale.GERMAN),
}

class LanguagePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var outputLanguage: OutputLanguage
        get() = prefs.getString(KEY_LANGUAGE, null)
            ?.let { name -> runCatching { OutputLanguage.valueOf(name) }.getOrNull() }
            ?: OutputLanguage.ITALIAN
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value.name).apply()

    private companion object {
        const val PREFS_NAME = "language_preferences"
        const val KEY_LANGUAGE = "output_language"
    }
}
