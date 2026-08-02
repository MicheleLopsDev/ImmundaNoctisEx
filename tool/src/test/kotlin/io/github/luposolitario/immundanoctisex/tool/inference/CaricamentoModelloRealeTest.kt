package io.github.luposolitario.immundanoctisex.tool.inference

import io.github.luposolitario.immundanoctisex.core.engine.inference.InferenceConfig
import io.github.luposolitario.immundanoctisex.core.engine.inference.ModelCatalog
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

// L'unica prova che dice davvero se il motore funziona: carica il
// modello VERO da qualche gigabyte e riferisce su quale backend è
// finito. Non può stare nella suite normale — il file non è in
// repository e nessuno vuole aspettare mezzo minuto a ogni build —
// quindi **si salta da solo** quando il modello non c'è.
//
// Serve a rispondere a una domanda precisa (02/08/2026): dopo aver
// messo `dxil.dll`/`dxcompiler.dll` in ~/ImmundaNoctisEx/dxc, la GPU
// viene accettata o si continua a ripiegare su CPU?
class CaricamentoModelloRealeTest {

    private val modello = File(
        File(System.getProperty("user.home"), "ImmundaNoctisEx/modelli"),
        ModelCatalog.GEMMA_4_E4B.fileName,
    )

    @Test
    fun ilModelloRealeSiCaricaEDiceSuQualeBackend() = runBlocking {
        if (!modello.isFile) {
            println("SALTATO: nessun modello in ${modello.absolutePath}")
            return@runBlocking
        }

        println("Precaricamento shader compiler: ${SupportoGpuWindows.preparaShaderCompiler()}")

        val motore = EditorInferenceEngine()
        val inizio = System.currentTimeMillis()
        val esito = motore.load(modello, InferenceConfig.TRANSLATION_PRESET)
        val secondi = (System.currentTimeMillis() - inizio) / 1000.0

        println("Caricamento: ${"%.1f".format(secondi)} s — backend ${motore.activeBackend}")
        motore.motivoRipiegoCpu?.let { println("Motivo del ripiego: $it") }

        assertTrue(esito.isSuccess, "Caricamento fallito: ${esito.exceptionOrNull()?.message}")
        assertTrue(motore.isLoaded)

        // La misura che decide se il backend attuale è utilizzabile per
        // le funzioni dell'editor: tradurre una scena vera, non dire
        // "ciao". Il numero conta più di qualunque opinione sul motore.
        motore.newSession()
        val prompt = "Translate the following gamebook scene into Italian, staying as close as " +
            "possible to the original wording. Answer with the translation only.\n\n" +
            "You are standing at the edge of the Fryelund Forest. The trail ahead is narrow and " +
            "overgrown, and the light is failing fast. Somewhere behind you, a branch snaps."
        val inizioGen = System.currentTimeMillis()
        var primoTokenMs: Long? = null
        val risposta = StringBuilder()
        motore.generate(prompt).collect { pezzo ->
            if (primoTokenMs == null) primoTokenMs = System.currentTimeMillis() - inizioGen
            risposta.append(pezzo)
        }
        val totaleSec = (System.currentTimeMillis() - inizioGen) / 1000.0
        // Stima grossolana ma sufficiente per un confronto fra backend.
        val tokenStimati = risposta.length / 4.0

        println("--- MISURA su ${motore.activeBackend}")
        println("primo token: ${primoTokenMs?.let { "$it ms" } ?: "mai"}")
        println("totale: ${"%.1f".format(totaleSec)} s per ${risposta.length} caratteri")
        println("velocità stimata: ${"%.1f".format(tokenStimati / totaleSec)} token/s")
        println("--- RISPOSTA\n${risposta.toString().trim()}")

        assertTrue(risposta.isNotEmpty(), "Il modello non ha prodotto testo")

        motore.unload()
    }
}
