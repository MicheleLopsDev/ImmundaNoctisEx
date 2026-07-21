package io.github.luposolitario.immundanoctisex.music

import android.content.Context
import android.media.MediaPlayer
import io.github.luposolitario.immundanoctisex.util.BundledTrack

// Player musicale a scope APPLICAZIONE (AppContainer, non una singola
// Route): sopravvive alla navigazione tra schermate. Prima viveva dentro
// OptionsRoute e moriva col DisposableEffect appena si usciva da Opzioni
// (Michele 22/07/2026: "quando esco dalle opzioni smette di suonare" -
// confermato nel log, error(-38,0): un metodo chiamato su un MediaPlayer
// già rilasciato). Restare vivo per tutta l'app significa anche suonare
// durante l'Avventura, con Gemma attivo: la condizione di Michele sul
// carico batteria (20/07) torna rilevante, ma è lui stesso ad aver
// chiesto ora che la musica non si fermi uscendo da una schermata.
class MusicPlayer(private val context: Context) {

    private val player = MediaPlayer()
    private var currentTrackId: String? = null
    private var isPrepared = false

    // Stessa traccia già pronta: riprende invece di ricaricare da capo
    // (evita un buco di silenzio ogni volta che si riapre Opzioni).
    fun play(track: BundledTrack, volume: Float) {
        if (currentTrackId == track.id && isPrepared) {
            setVolume(volume)
            runCatching { if (!player.isPlaying) player.start() }
            return
        }
        runCatching {
            player.reset()
            isPrepared = false
            context.assets.openFd(track.assetPath).use { afd ->
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            player.isLooping = true
            player.setVolume(volume, volume)
            player.setOnPreparedListener {
                isPrepared = true
                it.start()
            }
            player.prepareAsync()
            currentTrackId = track.id
        }
    }

    fun pause() {
        runCatching { if (player.isPlaying) player.pause() }
    }

    fun setVolume(volume: Float) {
        runCatching { player.setVolume(volume, volume) }
    }

    fun release() {
        runCatching { player.release() }
    }
}
