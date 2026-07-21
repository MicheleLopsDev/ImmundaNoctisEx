package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences

// Preferenze della musica di sottofondo (UI.md schermata 7): traccia
// scelta dall'utente (Uri content://, non un file copiato: MediaPlayer
// legge direttamente da lì, come i modelli scaricati leggono da un File
// vero solo perché LiteRT-LM lo richiede — qui non serve) e volume.
// SOLO configurazione (22/07/2026, richiesta Michele): la riproduzione
// vera durante la partita è un passo separato, non ancora collegato —
// stesso trattamento di TtsPreferences prima della Tappa 2. Quando
// arriverà: LOOP sempre attivo (Michele: "ovviamente vanno in loop"),
// `isLooping = true` sul MediaPlayer — non c'è nulla da configurare qui,
// non è un'opzione per l'utente.
class MusicPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var musicEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    // L'Uri persistito (takePersistableUriPermission preso al momento
    // della scelta, stesso pattern di AppContainer.loadSideloadedPackage):
    // resta leggibile anche dopo il riavvio dell'app. Null = nessuna
    // scelta esplicita dell'utente: si usa la traccia inclusa di default
    // (DEFAULT_TRACK_ASSET), non un file esterno.
    var selectedTrackUri: String?
        get() = prefs.getString(KEY_TRACK_URI, null)
        set(value) = prefs.edit().putString(KEY_TRACK_URI, value).apply()

    // Il nome del file, salvato a parte: l'Uri content:// non è
    // leggibile per l'utente, serve un'etichetta per la UI.
    var selectedTrackName: String?
        get() = prefs.getString(KEY_TRACK_NAME, null)
        set(value) = prefs.edit().putString(KEY_TRACK_NAME, value).apply()

    // Per la UI: il nome da mostrare, scelto dall'utente o quello della
    // traccia inclusa di default.
    val effectiveTrackName: String get() = selectedTrackName ?: DEFAULT_TRACK_DISPLAY_NAME

    // Basso di proposito (Michele 22/07/2026: "il volume è molto basso al
    // 15%"): la musica accompagna la lettura, non deve competere col TTS
    // né distrarre da un testo che richiede attenzione.
    var volume: Float
        get() = prefs.getFloat(KEY_VOLUME, DEFAULT_VOLUME)
        set(value) = prefs.edit().putFloat(KEY_VOLUME, value).apply()

    companion object {
        // Una delle 4 tracce che Michele ha composto (origina_res/,
        // copiate in assets/music/ il 22/07/2026): scelta come default
        // perché il gioco passa la maggior parte del tempo fuori dal
        // combattimento — un sottofondo che deve reggere ovunque, non
        // solo in un momento specifico. Le altre tre (combattimento,
        // mercato, romantico) sono nello stesso assets/music/, pronte se
        // in futuro si vorrà musica contestuale per scena — non oggi.
        const val DEFAULT_TRACK_ASSET = "music/esplorazione_Where_The_Statues_Kneel.mp3"
        const val DEFAULT_TRACK_DISPLAY_NAME = "Esplorazione (inclusa)"

        private const val PREFS_NAME = "music_preferences"
        private const val KEY_ENABLED = "music_enabled"
        private const val KEY_TRACK_URI = "selected_track_uri"
        private const val KEY_TRACK_NAME = "selected_track_name"
        private const val KEY_VOLUME = "volume"
        private const val DEFAULT_VOLUME = 0.15f
    }
}
