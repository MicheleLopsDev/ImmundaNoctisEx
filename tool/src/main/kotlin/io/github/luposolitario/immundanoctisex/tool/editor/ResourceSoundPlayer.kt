package io.github.luposolitario.immundanoctisex.tool.editor

import javazoom.jl.player.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

// Riproduzione dei suoni delle risorse (§15.6, Michele: "dai la
// possibilità di essere ascoltati"). JLayer decodifica e riproduce
// direttamente da uno stream MP3 su un thread di sfondo — se fallisce
// (stream corrotto, ecc.) si ignora silenziosamente, non è un'azione
// critica.
//
// Classe invece di una funzione sola (30/07/2026, Michele: "se chiudo
// la maschera smetti di suonare il suono"): `Player.play()` di JLayer è
// una chiamata BLOCCANTE — cancellare la coroutine che la ospita (come
// succede automaticamente quando il pannello si chiude e
// rememberCoroutineScope() viene cancellato) NON la interrompe, perché
// la cancellazione si applica solo ai punti di sospensione, non a una
// chiamata sincrona bloccante su un thread. Serve tenere il `Player`
// vivo per poterlo chiudere davvero con `.close()`.
class SoundPlayerController {
    private var playerAttivo: Player? = null

    // `suspend` (non lancia da sé una coroutine, giro precedente,
    // Michele: "non permettere di fare di nuovo click altrimenti...
    // succede un casino"): il chiamante sa quando finisce e può
    // disabilitare il proprio pulsante fino ad allora.
    suspend fun riproduci(url: URL) {
        withContext(Dispatchers.IO) {
            runCatching {
                url.openStream().use { flusso ->
                    val player = Player(flusso)
                    playerAttivo = player
                    player.play()
                }
            }
        }
        playerAttivo = null
    }

    // Interrompe una riproduzione in corso — richiamata alla chiusura
    // del pannello (DisposableEffect in SceneEditorScreen).
    fun ferma() {
        playerAttivo?.close()
        playerAttivo = null
    }
}
