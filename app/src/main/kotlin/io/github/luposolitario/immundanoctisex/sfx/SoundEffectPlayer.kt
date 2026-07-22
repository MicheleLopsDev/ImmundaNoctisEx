package io.github.luposolitario.immundanoctisex.sfx

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import io.github.luposolitario.immundanoctisex.util.AudioPreferences

// Effetti sonori brevi: il tiro del dado (22/07/2026, richiesta Michele),
// poi mangiare/bere (stesso giorno, "ho aggiunto altri 2 suoni... se le
// associ alle azioni specifiche andrebbe bene"), poi i passi nelle
// transizioni (22/07/2026, "un suono per il roll del dado e uno di passi
// da usare nelle transizioni"). SoundPool invece di MediaPlayer: un colpo
// secco deve partire SUBITO al tocco, non dopo la latenza di preparazione
// di un player pensato per file lunghi in loop (quello lo usa
// MusicPlayer). Volume legato al generale (AudioPreferences), stesso
// principio già in uso per TTS e musica.
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

class SoundEffectPlayer(private val context: Context) {

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

    // Suoni "a nome libero" (22/07/2026, Michele: "ogni risorsa immagine
    // ha un corrispettivo mp3" + "una voce di gioia quando termina
    // l'avventura e un grido quando muore"): troppi per un enum fisso
    // (50+ immagini più i finali), caricati al bisogno invece che tutti
    // insieme all'avvio. null in cache = già provato, il file non c'è —
    // non si ritenta ad ogni chiamata (vedi playNamed).
    private val namedSoundIds = mutableMapOf<String, Int?>()

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded += sampleId
        }
        SoundEffect.entries.forEach { effect ->
            runCatching {
                context.assets.openFd(effect.assetPath).use { afd ->
                    soundIds[effect] = pool.load(afd, 1)
                }
            }
        }
    }

    fun play(effect: SoundEffect) {
        val id = soundIds[effect] ?: return
        if (id !in loaded) return
        val volume = audioPreferences.generalVolume.coerceIn(0f, 1f)
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
        if (id !in loaded) return
        val volume = audioPreferences.generalVolume.coerceIn(0f, 1f)
        runCatching { pool.play(id, volume, volume, 1, 0, 1f) }
    }

    fun release() {
        runCatching { pool.release() }
    }
}
