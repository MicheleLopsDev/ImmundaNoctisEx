package io.github.luposolitario.immundanoctisex.sfx

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import io.github.luposolitario.immundanoctisex.music.MusicPlayer
import io.github.luposolitario.immundanoctisex.util.AudioPreferences
import io.github.luposolitario.immundanoctisex.util.SoundEffectPreferences

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

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
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
        if (namedSoundStreamIds.isEmpty()) return
        namedSoundStreamIds.values.forEach { streamId -> runCatching { pool.stop(streamId) } }
        namedSoundStreamIds.clear()
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
        val id = if (namedSoundIds.containsKey(name)) {
            namedSoundIds[name]
        } else {
            val loadedId = runCatching {
                context.assets.openFd("sfx/$folder/$name.mp3").use { afd -> pool.load(afd, 1) }
            }.getOrNull()
            namedSoundIds[name] = loadedId
            loadedId
        } ?: return

        // Già in loop: lo si lascia continuare, niente seconda copia sopra.
        if (name in namedSoundStreamIds) return

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
                .onSuccess { streamId -> if (streamId != 0) namedSoundStreamIds[name] = streamId }
        }
        if (id !in loaded) {
            pendingPlayOnLoad[id] = playAction
            return
        }
        playAction()
    }

    fun release() {
        runCatching { pool.release() }
    }

    private companion object {
        // "Basso" (Michele 28/07/2026): un terzo del volume normale basta
        // a farlo restare un sottofondo riconoscibile senza coprire la
        // voce del narratore.
        const val NAMED_SOUND_DUCK_FACTOR = 0.35f
    }
}
