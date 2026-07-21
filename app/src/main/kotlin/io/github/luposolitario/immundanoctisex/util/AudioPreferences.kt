package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences

// Volume GENERALE (22/07/2026, richiesta Michele: tre barre in Opzioni —
// TTS, musica, generale): un master separato da TtsPreferences.volume e
// MusicPreferences.volume, moltiplicato coi due per il volume effettivo
// di ciascuno. Classe a parte perché non appartiene né al TTS né alla
// musica: è il terzo controllo indipendente, non un dettaglio dell'uno
// o dell'altra.
class AudioPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var generalVolume: Float
        get() = prefs.getFloat(KEY_GENERAL_VOLUME, DEFAULT_VOLUME)
        set(value) = prefs.edit().putFloat(KEY_GENERAL_VOLUME, value).apply()

    private companion object {
        const val PREFS_NAME = "audio_preferences"
        const val KEY_GENERAL_VOLUME = "general_volume"
        const val DEFAULT_VOLUME = 0.8f
    }
}
