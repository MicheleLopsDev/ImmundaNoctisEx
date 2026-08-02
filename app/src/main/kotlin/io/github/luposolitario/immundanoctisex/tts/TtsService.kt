package io.github.luposolitario.immundanoctisex.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import io.github.luposolitario.immundanoctisex.core.data.model.Gender
import io.github.luposolitario.immundanoctisex.util.AudioPreferences
import io.github.luposolitario.immundanoctisex.util.TtsPreferences
import io.github.luposolitario.immundanoctisex.util.locale
import java.util.Locale

// TTS di sistema (riuso quasi invariato di TtsService v1,
// ANALISI-RIUSO-V1.md): incapsula TextToSpeech, seleziona la voce per
// genere, legge il testo. Rispetto a v1: Gender è l'enum di Ex, non una
// stringa; e c'è un UtteranceProgressListener (v1 non lo aveva — UI.md
// §Stato del narratore unificato: senza di lui non esiste modo di sapere
// quando il TTS ha FINITO di parlare, quindi lo stato SPEAKING del
// narratore non potrebbe mai tornare a IDLE da solo). Tappa 2 (22/07/2026,
// integrazione nel flusso scena) FATTA: AdventureState collega
// onSpeakingStarted/onSpeakingFinished al proprio isSpeaking.
class TtsService(
    private val context: Context,
    onReady: () -> Unit,
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false
    private var onReadyCallback: (() -> Unit)? = onReady
    private val preferences = TtsPreferences(context)
    private val audioPreferences = AudioPreferences(context)

    var onSpeakingStarted: (() -> Unit)? = null
    var onSpeakingFinished: (() -> Unit)? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e(TAG, "Inizializzazione TTS fallita, status=$status")
            return
        }
        isReady = true
        tts?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeakingStarted?.invoke()
                }
                override fun onDone(utteranceId: String?) {
                    onSpeakingFinished?.invoke()
                }
                @Deprecated("Richiesto dall'interfaccia, il codice di errore arriva da onError(String, Int)")
                override fun onError(utteranceId: String?) {
                    onSpeakingFinished?.invoke()
                }
                // BUG (28/07/2026, scoperto implementando il sottofondo SFX
                // in loop legato al TTS): stop() interrompe un'utterance
                // ancora in corso — onDone/onError NON arrivano mai in quel
                // caso, arriva SOLO onStop. Senza questo override, isSpeaking
                // restava bloccato a true per sempre dopo la primissima
                // interruzione manuale (moveTo() chiama ttsService.stop() ad
                // ogni cambio scena) — e con esso, il volume degli SFX
                // restava sempre "abbassato per il TTS" e nessun sottofondo
                // in loop si sarebbe più fermato da solo.
                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    onSpeakingFinished?.invoke()
                }
            },
        )
        onReadyCallback?.invoke()
        onReadyCallback = null
    }

    // Legge il testo con la voce preferita per genere (se salvata in
    // TtsPreferences), altrimenti prova a indovinarla dal nome, altrimenti
    // degrada sulla sola lingua — non blocca mai, nel peggiore dei casi
    // legge con la voce di sistema di default per quella lingua.
    fun speak(text: String, gender: Gender, locale: Locale) {
        val engine = tts
        if (!isReady || engine == null) {
            Log.w(TAG, "TTS non pronto, lettura saltata.")
            return
        }
        engine.setSpeechRate(preferences.speechRate)
        engine.setPitch(preferences.pitch)

        val preferredVoiceName = preferences.voiceFor(gender)
        val voice = preferredVoiceName?.let { name -> engine.voices?.find { it.name == name } }
            ?: findVoiceByGenderKeyword(engine, gender, locale)

        if (voice != null) {
            engine.voice = voice
        } else {
            engine.language = locale
        }
        // Volume TTS moltiplicato per il generale (22/07/2026, tre barre
        // in Opzioni): KEY_PARAM_VOLUME e' l'unico modo di controllare il
        // volume per singola utterance, non esiste un setVolume() sul
        // TextToSpeech come per rate/pitch.
        val effectiveVolume = (preferences.volume * audioPreferences.generalVolume).coerceIn(0f, 1f)
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, effectiveVolume) }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
    }

    fun stop() {
        tts?.stop()
    }

    // Rete di sicurezza (28/07/2026, Michele: passando a un'altra app il
    // TTS smetteva davvero di parlare ma il sottofondo SFX continuava
    // all'infinito): interroga il motore direttamente invece di fidarsi
    // solo dei callback di UtteranceProgressListener, che a quanto pare
    // possono non arrivare mai se il sistema operativo interrompe l'audio
    // sotto il livello dei callback (perdita di focus/audio in background)
    // invece che tramite una stop() esplicita.
    fun isCurrentlySpeaking(): Boolean = tts?.isSpeaking ?: false

    // Per la schermata Opzioni: le voci disponibili per una lingua, da
    // proporre come scelta esplicita (maschile/femminile).
    fun availableVoices(locale: Locale): List<Voice> {
        if (!isReady) return emptyList()
        return tts?.voices?.filter { it.locale.language == locale.language }.orEmpty()
    }

    private fun findVoiceByGenderKeyword(engine: TextToSpeech, gender: Gender, locale: Locale): Voice? {
        val keyword = if (gender == Gender.MALE) "male" else "female"
        return engine.voices
            ?.filter { it.locale.language == locale.language && it.name.contains(keyword, ignoreCase = true) }
            ?.minByOrNull { it.latency }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }

    private companion object {
        const val TAG = "TtsService"
        const val UTTERANCE_ID = "immunda_noctis_narration"
    }
}
