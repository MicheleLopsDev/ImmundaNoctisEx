package io.github.luposolitario.immundanoctisex.sfx

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import io.github.luposolitario.immundanoctisex.core.data.model.ImageReference
import io.github.luposolitario.immundanoctisex.music.MusicPlayer
import io.github.luposolitario.immundanoctisex.util.AudioPreferences
import io.github.luposolitario.immundanoctisex.util.SoundEffectPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Effetti sonori brevi: il tiro del dado (22/07/2026, richiesta Michele),
// poi mangiare/bere (stesso giorno, "ho aggiunto altri 2 suoni... se le
// associ alle azioni specifiche andrebbe bene"), poi i passi nelle
// transizioni (22/07/2026, "un suono per il roll del dado e uno di passi
// da usare nelle transizioni"). SoundPool invece di MediaPlayer: un colpo
// secco deve partire SUBITO al tocco, non dopo la latenza di preparazione
// di un player pensato per file lunghi in loop (quello lo usa
// MusicPlayer). Volume = effettiSuoni × generale (22/07/2026, richiesta
// Michele: "la barra con il volume dei suoni... deve essere una barra a
// parte" — prima usava solo il generale, senza un proprio controllo come
// TTS e musica ce l'hanno già).
enum class SoundEffect(val assetPath: String) {
    DICE_ROLL("sfx/dice_roll.mp3"),
    EAT("sfx/eat.mp3"),
    DRINK("sfx/drink.mp3"),
    FOOTSTEPS("sfx/footsteps.mp3"),
    // Il vecchio scontro di spade, liberato dal cambio del dado (22/07/2026,
    // Michele: "lo possiamo usare per l'inizio delle scene di
    // combattimento?") — riuso di un file già presente, nessun nuovo asset.
    COMBAT_START("sfx/combat_start.mp3"),
}

class SoundEffectPlayer(
    private val context: Context,
    private val soundEffectPreferences: SoundEffectPreferences = SoundEffectPreferences(context),
    // Per mettere in pausa la musica durante i suoni "a nome libero"
    // (24/07/2026, richiesta Michele: "durante il play dei suoni o dei
    // loc la musica vada in pausa... così da non confondere il
    // giocatore") — solo per questi, non per i brevi SoundEffect
    // dell'enum (dado/passi/mangiare), che durano meno di un secondo e
    // non giustificano un'interruzione della musica. Null nei test/
    // @Preview dove non serve.
    private val musicPlayer: MusicPlayer? = null,
    // SoundEffectPlayer non conosce le preferenze musica: chi lo
    // costruisce (AppContainer) decide come rispondere a "la musica
    // dovrebbe essere accesa in questo momento?".
    private val shouldResumeMusic: () -> Boolean = { true },
) {

    private val audioPreferences = AudioPreferences(context)

    // Condivisi fra SoundPool e il MediaPlayer degli sfx url: (01/08/2026):
    // stesso canale audio per tutti gli effetti, qualunque player li suoni.
    private val sfxAudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(sfxAudioAttributes)
        .build()

    private val soundIds = mutableMapOf<SoundEffect, Int>()
    private val loaded = mutableSetOf<Int>()

    // BUG (24/07/2026, Michele: "a volte non partono i suoni... sembra
    // che se il tts è manuale a volte non parte l'audio"): SoundPool
    // carica in modo ASINCRONO — la primissima volta che si richiede un
    // nome, `pool.load()` parte ma il campione non è ancora in `loaded`
    // quando arriva la richiesta di suonarlo subito dopo, e la richiesta
    // spariva in silenzio per sempre (mai più ritentata, l'id restava in
    // cache). Non è legato al TTS: è più visibile quando l'auto-lettura
    // è spenta perché in quel caso non c'è altro lavoro a tenere occupato
    // il thread principale abbastanza da lasciare il tempo alla
    // decodifica di finire per puro caso. Ora, se il campione non è
    // ancora pronto, la richiesta si mette in coda e parte da sola non
    // appena la decodifica finisce, invece di sparire.
    private val pendingPlayOnLoad = mutableMapOf<Int, () -> Unit>()

    // Suoni "a nome libero" (22/07/2026, Michele: "ogni risorsa immagine
    // ha un corrispettivo mp3" + "una voce di gioia quando termina
    // l'avventura e un grido quando muore"): troppi per un enum fisso
    // (50+ immagini più i finali), caricati al bisogno invece che tutti
    // insieme all'avvio. null in cache = già provato, il file non c'è —
    // non si ritenta ad ogni chiamata (vedi playNamed).
    private val namedSoundIds = mutableMapOf<String, Int?>()

    // Sottofondo al TTS (28/07/2026, Michele: "vorrei che gli audio sfx
    // facessero da audio di sottofondo al tts con un valore di volume
    // basso", poi "se il suono di sottofondo è corto mettilo in loop fino
    // a che il tts si spegne"): i suoni "a nome libero" (ambientazioni
    // delle location, finali) ora vanno in loop indefinito invece che una
    // volta sola, a volume ridotto mentre il narratore legge, e restano
    // attivi finché il TTS non finisce di parlare — non più legati a una
    // durata stimata. Lo stream id di ogni suono ancora in loop è la
    // fonte di verità di "sta ancora suonando" (sostituisce la vecchia
    // stima via MediaMetadataRetriever, non più necessaria): niente
    // seconda copia sopra la prima (playNamed), e permette di
    // abbassare/fermare il volume AL VOLO (setDuckedByTts,
    // stopBackgroundSounds). I brevi SoundEffect dell'enum
    // (dado/passi/mangiare/combattimento) restano invariati: durano meno
    // di un secondo, non vanno mai in loop e non si abbassano mai.
    private var duckedByTts = false
    private val namedSoundStreamIds = mutableMapOf<String, Int>()

    private val sfxDownloadCache = SfxDownloadCache(File(context.cacheDir, "sfx-cache"))

    // Scene.sfx con valore url: su MediaPlayer, NON su SoundPool
    // (01/08/2026): SoundPool decomprime l'intero campione in memoria come
    // PCM ed è pensato per colpi secchi di pochi secondi — un mp3 di 6
    // minuti (i file di prova scelti da Michele) diventerebbe ~60MB di
    // PCM e verrebbe rifiutato. Un url: è un file arbitrario dell'autore,
    // quindi potenzialmente un tappeto musicale lungo: stesso ragionamento
    // per cui la musica di sottofondo usa già MediaPlayer (MusicPlayer).
    // Gli sfx static: restano su SoundPool: sono gli asset brevi
    // sfx/images/<id>.mp3 bundlati nell'APK.
    //
    // Uno solo alla volta, come per i loop di SoundPool: una scena nuova
    // sostituisce il suono della precedente, mai due sovrapposti.
    private var customSfxPlayer: MediaPlayer? = null

    // Job del download url: in corso, per poterlo annullare se la scena
    // cambia di nuovo prima che finisca (A→B→A non deve far partire in
    // ritardo il suono di una scena non più corrente). Cancellato insieme
    // allo scope in release().
    private var customSfxJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loaded += sampleId
                pendingPlayOnLoad.remove(sampleId)?.invoke()
            }
        }
        SoundEffect.entries.forEach { effect ->
            runCatching {
                context.assets.openFd(effect.assetPath).use { afd ->
                    soundIds[effect] = pool.load(afd, 1)
                }
            }
        }
    }

    private fun effectiveVolume(ducked: Boolean = false): Float {
        val base = (soundEffectPreferences.volume * audioPreferences.generalVolume).coerceIn(0f, 1f)
        return if (ducked) base * NAMED_SOUND_DUCK_FACTOR else base
    }

    // Chiamato da AdventureState sugli stessi eventi che pilotano
    // isSpeaking (TtsService.onSpeakingStarted/onSpeakingFinished). Il TTS
    // che parte abbassa AL VOLO il volume di ogni loop già in corso; il
    // TTS che finisce li ferma del tutto — "in loop fino a che il tts si
    // spegne", non solo più piano dopo.
    fun setDuckedByTts(speaking: Boolean) {
        if (duckedByTts == speaking) return
        duckedByTts = speaking
        if (speaking) {
            val volume = effectiveVolume(ducked = true)
            namedSoundStreamIds.values.forEach { streamId ->
                runCatching { pool.setVolume(streamId, volume, volume) }
            }
            runCatching { customSfxPlayer?.setVolume(volume, volume) }
        } else {
            stopBackgroundSounds()
        }
    }

    // Ferma tutti i loop di sottofondo attivi e riprende la musica se era
    // stata messa in pausa per lasciarli sentire. Chiamato sia da qui (TTS
    // che finisce di parlare) sia da AdventureState ad ogni cambio scena
    // (moveTo), per non lasciarne mai uno a girare per sempre nel caso il
    // TTS non parli affatto in quella scena (auto-lettura spenta).
    fun stopBackgroundSounds() {
        if (namedSoundStreamIds.isEmpty() && customSfxPlayer == null) return
        namedSoundStreamIds.values.forEach { streamId -> runCatching { pool.stop(streamId) } }
        namedSoundStreamIds.clear()
        // Un download/preparazione ancora in corso non deve far partire il
        // suono di una scena ormai lasciata (01/08/2026).
        customSfxJob?.cancel()
        stopCustomSfx()
        if (shouldResumeMusic()) musicPlayer?.resume()
    }

    fun play(effect: SoundEffect) {
        val id = soundIds[effect] ?: return
        val volume = effectiveVolume()
        if (id !in loaded) {
            pendingPlayOnLoad[id] = { runCatching { pool.play(id, volume, volume, 1, 0, 1f) } }
            return
        }
        runCatching { pool.play(id, volume, volume, 1, 0, 1f) }
    }

    // Vocabolario APERTO (a differenza di SoundEffect): qualunque nome si
    // prova, `sfx/$folder/$name.mp3`. File mancante = silenzio, mai un
    // errore — Michele: "l'importante è che non vada in errore", così può
    // procurare gli asset con calma senza rompere nulla nel frattempo.
    fun playNamed(name: String, folder: String = "images") {
        playLooping(name, namedSoundIds) {
            context.assets.openFd("sfx/$folder/$name.mp3").use { afd -> pool.load(afd, 1) }
        }
    }

    // Scene.sfx (31/07/2026): sovrascrive il meccanismo automatico
    // per-immagine con il suono scelto dall'autore per questa scena.
    // static: riusa playNamed esistente invariato (stesso asset
    // sfx/images/<id>.mp3 già usato dal meccanismo automatico). url:
    // scarica (rete, quindi Dispatchers.IO) e torna esplicitamente sul
    // thread main prima di toccare pool/le mappe condivise — invariante da
    // preservare: oggi SoundPool è toccato sempre e solo dal thread main
    // "per caso", non per progetto.
    fun playCustomSfx(reference: ImageReference) {
        when (reference) {
            is ImageReference.Static -> playNamed(reference.catalogId)
            is ImageReference.Url -> {
                customSfxJob?.cancel()
                customSfxJob = scope.launch {
                    val file = sfxDownloadCache.localFileFor(reference.url) ?: return@launch
                    // Preparazione QUI, su Dispatchers.IO: legge e decodifica
                    // dal disco, non deve bloccare il thread della UI.
                    val player = runCatching {
                        MediaPlayer().apply {
                            setAudioAttributes(sfxAudioAttributes)
                            setDataSource(file.absolutePath)
                            isLooping = true
                            prepare()
                        }
                    }.getOrNull() ?: return@launch
                    // Scena già lasciata mentre si scaricava/preparava: il
                    // player va rilasciato subito, altrimenti resterebbe
                    // orfano (withContext sotto non verrebbe mai eseguito).
                    if (!isActive) {
                        runCatching { player.release() }
                        return@launch
                    }
                    withContext(Dispatchers.Main) { startCustomSfx(player) }
                }
            }
        }
    }

    // Solo sul thread main: tocca musicPlayer e il player condiviso, come
    // tutto il resto di questa classe.
    private fun startCustomSfx(player: MediaPlayer) {
        stopCustomSfx()
        customSfxPlayer = player
        val volume = effectiveVolume(ducked = duckedByTts)
        runCatching {
            player.setVolume(volume, volume)
            player.start()
        }
        // Stessa regola dei suoni "a nome libero" (24/07/2026): la musica
        // si mette in pausa finché dura il sottofondo, e riparte da
        // stopBackgroundSounds().
        musicPlayer?.pause()
    }

    private fun stopCustomSfx() {
        customSfxPlayer?.let { player ->
            runCatching { if (player.isPlaying) player.stop() }
            runCatching { player.release() }
        }
        customSfxPlayer = null
    }

    // Carica (se non già in cache), mette in loop e gestisce ducking/pausa
    // musica per i suoni su SoundPool: i brevi asset bundlati, sia quelli
    // automatici per-immagine sia un Scene.sfx "static:". Gli url: hanno
    // un percorso tutto loro (startCustomSfx, MediaPlayer).
    private fun playLooping(key: String, loadedIds: MutableMap<String, Int?>, loadSample: () -> Int?) {
        val id = if (loadedIds.containsKey(key)) {
            loadedIds[key]
        } else {
            val loadedId = runCatching { loadSample() }.getOrNull()
            loadedIds[key] = loadedId
            loadedId
        } ?: return

        // Già in loop: lo si lascia continuare, niente seconda copia sopra.
        if (key in namedSoundStreamIds) return

        // Musica in pausa finché dura questo sottofondo — non più un timer
        // sulla durata stimata del file (ora è in loop, potrebbe durare
        // ben più a lungo del singolo giro), ma la ripresa esplicita in
        // stopBackgroundSounds() quando il loop finisce davvero.
        musicPlayer?.pause()

        val volume = effectiveVolume(ducked = duckedByTts)
        val playAction: () -> Unit = {
            // loop = -1: gira all'infinito finché non arriva uno stop
            // esplicito (setDuckedByTts quando il TTS finisce di parlare,
            // o stopBackgroundSounds al cambio scena).
            runCatching { pool.play(id, volume, volume, 1, -1, 1f) }
                .onSuccess { streamId -> if (streamId != 0) namedSoundStreamIds[key] = streamId }
        }
        if (id !in loaded) {
            pendingPlayOnLoad[id] = playAction
            return
        }
        playAction()
    }

    fun release() {
        scope.cancel()
        stopCustomSfx()
        runCatching { pool.release() }
    }

    private companion object {
        // "Basso" (Michele 28/07/2026): un terzo del volume normale basta
        // a farlo restare un sottofondo riconoscibile senza coprire la
        // voce del narratore.
        const val NAMED_SOUND_DUCK_FACTOR = 0.35f
    }
}
