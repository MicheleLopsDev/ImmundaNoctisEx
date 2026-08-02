package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences
import io.github.luposolitario.immundanoctisex.core.engine.inference.LinguaOutput
import java.util.Locale

// La lingua in cui Gemma riscrive la scena (UI.md schermata 7).
// L'elenco vive in :core:engine (LinguaOutput, 02/08/2026): le lingue
// selezionabili devono essere le STESSE nel client e nell'editor,
// altrimenti l'anteprima dell'autore mostrerebbe una lingua che il
// giocatore non può scegliere. Qui resta solo la persistenza.
class LanguagePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var outputLanguage: LinguaOutput
        get() = LinguaOutput.daNome(prefs.getString(KEY_LANGUAGE, null))
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value.name).apply()

    private companion object {
        const val PREFS_NAME = "language_preferences"
        const val KEY_LANGUAGE = "output_language"
    }
}

// Il TTS deve parlare la lingua del testo, non sempre italiano: il tag
// BCP-47 di LinguaOutput diventa qui un Locale vero (in :core:engine
// non può esserci, java.util.Locale non esiste nel codice comune).
val LinguaOutput.locale: Locale get() = Locale.forLanguageTag(tag)
