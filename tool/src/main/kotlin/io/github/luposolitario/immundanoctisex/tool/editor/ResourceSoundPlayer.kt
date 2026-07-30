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
// `suspend` invece di lanciare da sé una coroutine (30/07/2026, Michele:
// "se fa play del sound non permettere di fare di nuovo click" — con
// più riproduzioni in corso insieme il suono va in un casino): così
// il chiamante sa ESATTAMENTE quando la riproduzione finisce e può
// disabilitare il pulsante fino ad allora, invece di lanciarne una
// nuova ogni volta che clicchi.
suspend fun riproduciSuono(url: URL) {
    withContext(Dispatchers.IO) {
        runCatching {
            url.openStream().use { Player(it).play() }
        }
    }
}
