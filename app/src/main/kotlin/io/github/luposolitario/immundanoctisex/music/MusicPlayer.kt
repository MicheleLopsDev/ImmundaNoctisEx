package io.github.luposolitario.immundanoctisex.music

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import io.github.luposolitario.immundanoctisex.util.BundledMusicCatalog
import io.github.luposolitario.immundanoctisex.util.BundledTrack
import io.github.luposolitario.immundanoctisex.util.MusicPreferences

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

    // Modalità casuale (24/07/2026, richiesta Michele: "un tasto che se
    // selezionato fa il random dei brani musicali, non riprodurre due
    // volte la stessa musica"): niente loop sulla singola traccia, si
    // passa alla successiva (mai la stessa di fila) quando quella
    // corrente finisce — vedi playShuffle/playNextShuffled sotto.
    private var isShuffleMode = false
    private var shuffleTracks: List<BundledTrack> = emptyList()
    private var lastShuffledTrackId: String? = null

    // Stessa traccia già pronta: riprende invece di ricaricare da capo
    // (evita un buco di silenzio ogni volta che si riapre Opzioni).
    fun play(track: BundledTrack, volume: Float) {
        isShuffleMode = false
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
            // Uscendo dalla modalità casuale non deve restare agganciato
            // un listener che salterebbe a un'altra traccia da solo.
            player.setOnCompletionListener(null)
            player.setOnPreparedListener {
                isPrepared = true
                it.start()
            }
            player.prepareAsync()
            currentTrackId = track.id
        }
    }

    // Avvia (o riprende) la modalità casuale: se è già quella attiva e
    // pronta, riprende sul posto come play() con una traccia fissa —
    // altrimenti ne pesca una nuova.
    fun playShuffle(tracks: List<BundledTrack>, volume: Float) {
        if (tracks.isEmpty()) return
        shuffleTracks = tracks
        if (isShuffleMode && isPrepared) {
            setVolume(volume)
            runCatching { if (!player.isPlaying) player.start() }
            return
        }
        isShuffleMode = true
        playNextShuffled(volume)
    }

    // Chiama sé stessa via OnCompletionListener: la catena continua finché
    // isShuffleMode resta true (play() la spegne uscendo dalla modalità).
    private fun playNextShuffled(volume: Float) {
        val pool = if (shuffleTracks.size > 1) {
            shuffleTracks.filter { it.id != lastShuffledTrackId }
        } else {
            shuffleTracks
        }
        val next = pool.random()
        lastShuffledTrackId = next.id
        runCatching {
            player.reset()
            isPrepared = false
            context.assets.openFd(next.assetPath).use { afd ->
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            player.isLooping = false
            player.setVolume(volume, volume)
            player.setOnCompletionListener { playNextShuffled(volume) }
            player.setOnPreparedListener {
                isPrepared = true
                it.start()
            }
            player.prepareAsync()
            currentTrackId = next.id
        }
    }

    // Punto unico per "avvia la musica secondo le preferenze salvate":
    // decide da solo se è una traccia fissa o la modalità casuale, così
    // le route (Opzioni, avvio app, tasto rapido in Avventura) non
    // devono ripetere lo stesso if/else ognuna per conto suo.
    fun playConfigured(preferences: MusicPreferences, volume: Float) {
        if (preferences.selectedTrackId == BundledMusicCatalog.RANDOM_ID) {
            playShuffle(BundledMusicCatalog.TRACKS, volume)
        } else {
            play(preferences.effectiveTrack, volume)
        }
    }

    fun pause() {
        runCatching { if (player.isPlaying) player.pause() }
    }

    // Ripresa dopo una pausa "tecnica" (vedi duckFor sotto) — non un
    // play(track) da capo, solo far ripartire lo stesso MediaPlayer da
    // dove si era fermato (funziona uguale su traccia fissa e casuale,
    // è sempre lo stesso player sotto).
    fun resume() {
        runCatching { if (!player.isPlaying) player.start() }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var duckResumeRunnable: Runnable? = null

    // Pausa "tecnica" per lasciar sentire un suono nominato senza che si
    // accavalli alla musica (24/07/2026, richiesta Michele: "durante il
    // play dei suoni o dei loc la musica vada in pausa... così da non
    // confondere il giocatore"). Riprende da sola dopo circa la durata
    // del suono — ma SOLO se la musica stava DAVVERO suonando (altrimenti
    // "riprenderebbe" una musica che l'utente aveva già spento) e solo se
    // `shouldResume()` è ancora vero al momento buono (l'utente potrebbe
    // aver spento la musica a mano nel frattempo — controllato da chi
    // chiama, SoundEffectPlayer non sa nulla delle preferenze musica).
    fun duckFor(durationMs: Long, shouldResume: () -> Boolean) {
        duckResumeRunnable?.let { handler.removeCallbacks(it) }
        val wasPlaying = player.isPlaying
        pause()
        if (!wasPlaying || durationMs <= 0) return
        val runnable = Runnable { if (shouldResume()) resume() }
        duckResumeRunnable = runnable
        handler.postDelayed(runnable, durationMs)
    }

    fun setVolume(volume: Float) {
        runCatching { player.setVolume(volume, volume) }
    }

    fun release() {
        duckResumeRunnable?.let { handler.removeCallbacks(it) }
        runCatching { player.release() }
    }
}
