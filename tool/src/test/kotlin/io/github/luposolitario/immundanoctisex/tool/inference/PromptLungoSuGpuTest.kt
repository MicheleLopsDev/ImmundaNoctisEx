package io.github.luposolitario.immundanoctisex.tool.inference

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.engine.inference.InferenceConfig
import io.github.luposolitario.immundanoctisex.core.engine.inference.LinguaOutput
import io.github.luposolitario.immundanoctisex.core.engine.inference.ModelCatalog
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test

// Diagnostica del guasto del 02/08/2026 (Michele: "non funziona la
// traduzione"): tradurre una scena vera fa morire la GPU con
// DXGI_ERROR_DEVICE_HUNG, mentre il prompt corto della schermata
// Modello passa senza problemi. Qui si misura DOVE sta il confine.
//
// Si salta da solo senza il modello sul disco, come gli altri test che
// toccano file da gigabyte.
class PromptLungoSuGpuTest {

    private val modello = File(
        File(System.getProperty("user.home"), "ImmundaNoctisEx/modelli"),
        ModelCatalog.GEMMA_4_E4B.fileName,
    )
    private val libro = File("C:/DEV/ImmundaNoctisEx/doc/LIBRI/01fftd.json")

    @Test
    fun unaScenaVeraSuGpuConESenzaContinuazioni() = runBlocking {
        if (!modello.isFile || !libro.isFile) {
            println("SALTATO: manca il modello o il libro di prova")
            return@runBlocking
        }
        val manifest = Json { ignoreUnknownKeys = true }
            .decodeFromString(Manifest.serializer(), libro.readText())
        val scena = manifest.scenes.first { it.id == "141" }

        val motore = EditorInferenceEngine()
        val traduttore = TraduttoreScene(motore)

        // Giro 1: la scena com'è, continuazioni comprese — quello che ha
        // fatto morire la GPU a Michele.
        motore.load(modello, InferenceConfig.TRANSLATION_PRESET).getOrThrow()
        println("--- backend: ${motore.activeBackend}")
        val inizio = System.currentTimeMillis()
        val esito = traduttore.traduci(scena, manifest, LinguaOutput.ITALIANO, modalitaTraduzione = true)
        val secondi = (System.currentTimeMillis() - inizio) / 1000.0
        println(
            "--- CON continuazioni: ${if (esito.isSuccess) "OK" else "FALLITO"} in ${"%.1f".format(secondi)} s" +
                (esito.exceptionOrNull()?.let { " — ${it.message}" } ?: ""),
        )
        esito.getOrNull()?.let { println("    testo: ${it.narrative.take(120)}…") }

        // Giro 2: senza continuazioni (~40% di prompt in meno). Il
        // modello va ricaricato: dopo un device hung l'Engine è morto.
        motore.unload()
        motore.load(modello, InferenceConfig.TRANSLATION_PRESET).getOrThrow()
        val inizio2 = System.currentTimeMillis()
        val esito2 = traduttore.traduci(
            scena, manifest, LinguaOutput.ITALIANO,
            modalitaTraduzione = true, conContinuazioni = false,
        )
        val secondi2 = (System.currentTimeMillis() - inizio2) / 1000.0
        println(
            "--- SENZA continuazioni: ${if (esito2.isSuccess) "OK" else "FALLITO"} in ${"%.1f".format(secondi2)} s" +
                (esito2.exceptionOrNull()?.let { " — ${it.message}" } ?: ""),
        )
        esito2.getOrNull()?.let { println("    testo: ${it.narrative.take(120)}…") }

        motore.unload()
    }
}
