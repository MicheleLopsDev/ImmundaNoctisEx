package io.github.luposolitario.immundanoctisex.util

import android.content.Context
import android.content.SharedPreferences
import io.github.luposolitario.immundanoctisex.core.data.model.Gender

// Preferenze TTS (UI.md schermata 7), riuso quasi invariato di
// TtsPreferences v1 (ANALISI-RIUSO-V1.md): auto-lettura, velocità,
// pitch, voce per genere. Differenza da v1: il genere è l'enum Gender
// di Ex (MALE/FEMALE), non una stringa libera da confrontare a mano.
class TtsPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Con l'auto-lettura ACCESA l'icona "leggi" nel blocco del narratore
    // è grigia (legge già tutto da sé) — dettaglio della schermata
    // Avventura, non di qui.
    var autoReadEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_READ, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_READ, value).apply()

    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply()

    var pitch: Float
        get() = prefs.getFloat(KEY_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_PITCH, value).apply()

    // Volume della voce (22/07/2026, richiesta Michele: tre barre in
    // Opzioni - TTS, musica, generale). Applicato come KEY_PARAM_VOLUME
    // sull'utterance: non c'e' un equivalente di setSpeechRate/setPitch
    // sull'oggetto TextToSpeech per il volume.
    var volume: Float
        get() = prefs.getFloat(KEY_VOLUME, DEFAULT_VOLUME)
        set(value) = prefs.edit().putFloat(KEY_VOLUME, value).apply()

    // null = nessuna preferenza esplicita, TtsService prova a indovinare
    // dal nome della voce (stesso fallback di v1).
    fun voiceFor(gender: Gender): String? =
        prefs.getString(keyFor(gender), null)

    fun setVoiceFor(gender: Gender, voiceName: String?) {
        prefs.edit().apply {
            if (voiceName == null) remove(keyFor(gender)) else putString(keyFor(gender), voiceName)
        }.apply()
    }

    private fun keyFor(gender: Gender): String = when (gender) {
        Gender.MALE -> KEY_VOICE_MALE
        Gender.FEMALE -> KEY_VOICE_FEMALE
    }

    private companion object {
        const val PREFS_NAME = "tts_preferences"
        const val KEY_AUTO_READ = "auto_read_enabled"
        const val KEY_SPEECH_RATE = "speech_rate"
        const val KEY_PITCH = "pitch"
        const val KEY_VOICE_MALE = "selected_voice_male"
        const val KEY_VOICE_FEMALE = "selected_voice_female"
        const val KEY_VOLUME = "volume"
        const val DEFAULT_VOLUME = 0.75f
    }
}
