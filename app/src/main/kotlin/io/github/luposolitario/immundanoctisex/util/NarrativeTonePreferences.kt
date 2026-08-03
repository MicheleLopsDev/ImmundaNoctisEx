package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import io.github.luposolitario.immundanoctisex.R

// Tono della narrazione (UI.md schermata 7, richiesta Michele
// 21/07/2026): oggi il tono lo decide SOLO l'autore del libro
// (Scene.toneHints, con fallback su Manifest.toneHints) — questa
// preferenza lascia al giocatore la possibilità di FORZARLO, per tutta
// la sessione. AUTHOR (default) non cambia nulla: `hints` è null,
// SceneNarrator continua a usare quelli della scena. Le altre voci
// SOSTITUISCONO i toneHints dell'autore, non si sommano — un
// "avventuroso" scelto dal giocatore su una scena scritta "grim" deve
// vincere chiaramente, non mischiarsi in un risultato ambiguo.
//
// `labelRes` è il nome MOSTRATO (strings.xml, tradotto); `hints` sono i
// termini che finiscono nel prompt e restano in inglese SEMPRE, in
// qualunque lingua giochi: sono istruzioni per il modello, non testo da
// leggere.
enum class NarrativeTone(@StringRes val labelRes: Int, val hints: List<String>?) {
    AUTHOR(R.string.tone_author, null),
    DARK(R.string.tone_dark, listOf("dark", "grim")),
    ADVENTUROUS(R.string.tone_adventurous, listOf("adventurous", "bold")),
    MYSTERIOUS(R.string.tone_mysterious, listOf("mysterious", "eerie")),
    HEROIC(R.string.tone_heroic, listOf("heroic", "epic")),
    LIGHT(R.string.tone_light, listOf("light-hearted", "playful")),
    GRITTY(R.string.tone_gritty, listOf("gritty", "violent")),
    EROTIC(R.string.tone_erotic, listOf("erotic", "sexy", "explict")),
    BRUTAL(R.string.tone_brutal, listOf("brutal", "graphic", "unflinching")),
}

class NarrativeTonePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var narrativeTone: NarrativeTone
        get() = prefs.getString(KEY_TONE, null)
            ?.let { name -> runCatching { NarrativeTone.valueOf(name) }.getOrNull() }
            ?: NarrativeTone.AUTHOR
        set(value) = prefs.edit().putString(KEY_TONE, value.name).apply()

    private companion object {
        const val PREFS_NAME = "narrative_tone_preferences"
        const val KEY_TONE = "narrative_tone"
    }
}
