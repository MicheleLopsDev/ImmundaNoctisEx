package io.github.luposolitario.immundanoctisex.tool.inference

import io.github.luposolitario.immundanoctisex.core.engine.inference.InferenceConfig
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Il modello vero pesa 3,7 GB e non sta in repository: questi test non
// generano testo, verificano che l'IMPALCATURA regga — che è poi la
// parte che può rompersi in silenzio.
class EditorInferenceEngineTest {

    @Test
    fun unFileCheNonEsisteDaUnErrorePulitoInveceDiUnEccezione() = runBlocking {
        val motore = EditorInferenceEngine()
        val esito = motore.load(File("modello-che-non-esiste.litertlm"), InferenceConfig())

        assertTrue(esito.isFailure)
        assertTrue(
            esito.exceptionOrNull()?.message?.contains("non trovato") == true,
            "Il messaggio deve dire che il file manca: ${esito.exceptionOrNull()?.message}",
        )
        assertFalse(motore.isLoaded)
    }

    // Il test che conta davvero: `litertlm-jvm` porta le native per
    // quattro piattaforme dentro un unico jar, e quella giusta viene
    // scelta a runtime. Se la .dll per windows-x86_64 non ci fosse (o
    // non fosse caricabile), qui uscirebbe un UnsatisfiedLinkError
    // invece di un fallimento ordinato — e lo si scoprirebbe solo il
    // giorno in cui si prova ad aprire un modello da 3,7 GB.
    //
    // Il file è finto e vuoto: NON deve caricarsi. Il punto è arrivare
    // fino al tentativo sui backend, cioè attraversare tutto lo strato
    // nativo, e riceverne indietro un errore invece di un crash.
    @Test
    fun laLibreriaNativaSiCaricaEIlBackendRispondeAnchePerUnModelloFinto() = runBlocking {
        val finto = File.createTempFile("modello-finto", ".litertlm").apply { deleteOnExit() }
        val motore = EditorInferenceEngine()

        val esito = motore.load(finto, InferenceConfig())

        assertTrue(esito.isFailure, "Un file vuoto non può essere un modello valido")
        val messaggio = esito.exceptionOrNull()?.message.orEmpty()
        assertTrue(
            messaggio.contains("Nessun backend disponibile"),
            "Ci si aspetta il fallimento DOPO aver interrogato i backend (nativa caricata): $messaggio",
        )
        assertFalse(motore.isLoaded)
    }

    @Test
    fun scaricareUnMotoreMaiCaricatoNonEsplode() = runBlocking {
        val motore = EditorInferenceEngine()
        motore.unload()

        assertFalse(motore.isLoaded)
    }
}
