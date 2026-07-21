package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences

// Preferenze della musica di sottofondo (UI.md schermata 7): quale delle
// tracce incluse è attiva e a che volume. SOLO configurazione per la
// partita vera (22/07/2026): la riproduzione durante l'avventura è un
// passo separato, non ancora collegato - stesso trattamento già dato al
// TTS prima della sua Tappa 2. L'ANTEPRIMA in questa schermata (Michele:
// "quando seleziono una combo questa parte per provarla") è un caso a
// parte, gestita localmente da OptionsRoute - non tocca queste preferenze
// oltre a salvare la scelta.
class MusicPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var musicEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    // Null = nessuna scelta esplicita, si usa BundledMusicCatalog.default.
    var selectedTrackId: String?
        get() = prefs.getString(KEY_TRACK_ID, null)
        set(value) = prefs.edit().putString(KEY_TRACK_ID, value).apply()

    val effectiveTrack: BundledTrack get() = BundledMusicCatalog.byId(selectedTrackId)

    // Basso di proposito (Michele 22/07/2026: "il volume è molto basso al
    // 15%"): la musica accompagna la lettura, non deve competere col TTS
    // né distrarre da un testo che richiede attenzione.
    var volume: Float
        get() = prefs.getFloat(KEY_VOLUME, DEFAULT_VOLUME)
        set(value) = prefs.edit().putFloat(KEY_VOLUME, value).apply()

    // LOOP sempre attivo quando arriverà la riproduzione vera durante la
    // partita (Michele: "ovviamente vanno in loop") - isLooping = true sul
    // MediaPlayer, non c'è nulla da configurare qui: non è un'opzione.

    private companion object {
        const val PREFS_NAME = "music_preferences"
        const val KEY_ENABLED = "music_enabled"
        const val KEY_TRACK_ID = "selected_track_id"
        const val KEY_VOLUME = "volume"
        const val DEFAULT_VOLUME = 0.15f
    }
}
