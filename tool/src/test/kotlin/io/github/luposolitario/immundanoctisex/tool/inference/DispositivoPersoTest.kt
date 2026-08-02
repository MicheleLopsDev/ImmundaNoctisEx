package io.github.luposolitario.immundanoctisex.tool.inference

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Riconoscere la GPU persa è ciò che decide se ritentare: sbagliare in
// un senso fa aspettare il doppio per niente, nell'altro butta via una
// traduzione che al secondo giro sarebbe passata.
//
// I messaggi sono quelli veri raccolti dal log di Michele (02/08/2026).
class DispositivoPersoTest {

    @Test
    fun riconosceIlTimeoutSulReadback() {
        val errore = IllegalStateException(
            "Status Code: 10. Message: ABORTED: The timeout was reached while reading back data.",
        )

        assertTrue(EditorInferenceEngine.eDispositivoPerso(errore))
    }

    @Test
    fun riconosceIlDeviceHungDiDirect3D() {
        val errore = RuntimeException("Device removed reason: DXGI_ERROR_DEVICE_HUNG (0x887A0006)")

        assertTrue(EditorInferenceEngine.eDispositivoPerso(errore))
    }

    // Il messaggio utile spesso non è nell'eccezione in cima ma in una
    // delle sue cause: cercarlo solo al primo livello lo mancherebbe.
    @Test
    fun guardaAncheDentroLeCauseAnnidate() {
        val profondo = IOException("Failed Direct3D call. Error: 0x887a0005")
        val errore = IllegalStateException("Generazione non riuscita", RuntimeException("wrapper", profondo))

        assertTrue(EditorInferenceEngine.eDispositivoPerso(errore))
    }

    @Test
    fun unErroreQualunqueNonEUnaGpuPersa() {
        assertFalse(EditorInferenceEngine.eDispositivoPerso(IllegalStateException("Il modello non ha prodotto testo.")))
        assertFalse(EditorInferenceEngine.eDispositivoPerso(IOException("File del modello non trovato")))
        assertFalse(EditorInferenceEngine.eDispositivoPerso(null))
    }
}
