package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences

// Volume degli effetti sonori (dado, passi, mangiare/bere, inizio
// combattimento, suoni delle immagini/finali) — quarta barra a parte in
// Opzioni (22/07/2026, Michele: "la barra con il volume dei suoni... deve
// essere una barra a parte"). Fino a qui SoundEffectPlayer usava solo
// AudioPreferences.generalVolume: nessun controllo indipendente, a
// differenza di TTS e musica che ce l'hanno già ciascuno il proprio.
class SoundEffectPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var volume: Float
        get() = prefs.getFloat(KEY_VOLUME, DEFAULT_VOLUME)
        set(value) = prefs.edit().putFloat(KEY_VOLUME, value).apply()

    private companion object {
        const val PREFS_NAME = "sound_effect_preferences"
        const val KEY_VOLUME = "volume"
        const val DEFAULT_VOLUME = 0.7f
    }
}
