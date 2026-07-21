package io.github.luposolitario.immundanoctisex.sfx

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import io.github.luposolitario.immundanoctisex.util.AudioPreferences

// Effetti sonori brevi (22/07/2026, richiesta Michele: il tocco sul dado
// del destino in combattimento deve sentirsi). SoundPool invece di
// MediaPlayer: un colpo secco deve partire SUBITO al tocco, non dopo la
// latenza di preparazione di un MediaPlayer pensato per file lunghi in
// loop (quello lo usa MusicPlayer). Il volume segue il generale
// (AudioPreferences): stesso principio di TTS e musica, un solo cursore
// comune per tutto l'audio dell'app.
class SoundEffectPlayer(context: Context) {

    private val audioPreferences = AudioPreferences(context)

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private var diceRollSoundId = 0
    private var diceRollLoaded = false

    init {
        runCatching {
            context.assets.openFd(DICE_ROLL_ASSET).use { afd ->
                diceRollSoundId = pool.load(afd, 1)
            }
        }
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (sampleId == diceRollSoundId && status == 0) diceRollLoaded = true
        }
    }

    fun playDiceRoll() {
        if (!diceRollLoaded) return
        val volume = audioPreferences.generalVolume.coerceIn(0f, 1f)
        runCatching { pool.play(diceRollSoundId, volume, volume, 1, 0, 1f) }
    }

    fun release() {
        runCatching { pool.release() }
    }

    private companion object {
        const val DICE_ROLL_ASSET = "sfx/dice_roll.mp3"
    }
}
