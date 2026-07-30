package io.github.luposolitario.immundanoctisex.tool.editor

import javazoom.jl.player.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL

// Riproduzione dei suoni delle risorse (§15.6, Michele: "dai la
// possibilità di essere ascoltati"). JLayer decodifica e riproduce
// direttamente da uno stream MP3 — un `Player` per ogni riproduzione,
// nella sua coroutine di sfondo: se fallisce (stream corrotto, ecc.) si
// ignora silenziosamente, non è un'azione critica.
fun riproduciSuono(scope: CoroutineScope, url: URL) {
    scope.launch(Dispatchers.IO) {
        runCatching {
            url.openStream().use { Player(it).play() }
        }
    }
}
