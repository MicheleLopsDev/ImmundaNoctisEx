package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences

// Tono della narrazione (UI.md schermata 7, richiesta Michele
// 21/07/2026): oggi il tono lo decide SOLO l'autore del libro
// (Scene.toneHints, con fallback su Manifest.toneHints) — questa
// preferenza lascia al giocatore la possibilità di FORZARLO, per tutta
// la sessione. AUTHOR (default) non cambia nulla: `hints` è null,
// SceneNarrator continua a usare quelli della scena. Le altre voci
// SOSTITUISCONO i toneHints dell'autore, non si sommano — un
// "avventuroso" scelto dal giocatore su una scena scritta "grim" deve
// vincere chiaramente, non mischiarsi in un risultato ambiguo.
enum class NarrativeTone(val displayName: String, val hints: List<String>?) {
    AUTHOR("Come l'autore (default)", null),
    DARK("Cupo", listOf("dark", "grim")),
    ADVENTUROUS("Avventuroso", listOf("adventurous", "bold")),
    MYSTERIOUS("Misterioso", listOf("mysterious", "eerie")),
    HEROIC("Eroico", listOf("heroic", "epic")),
    LIGHT("Leggero", listOf("light-hearted", "playful")),
    GRITTY("Duro e crudo", listOf("gritty", "violent")),
    EROTIC("Erotico", listOf("erotic","sexy", "explict")),
    BRUTAL("Brutale", listOf("brutal", "graphic", "unflinching"))
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
