package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.Combat
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

// L'esportazione si verifica per intero senza modello: il riassunto è
// solo una stringa in ingresso, tutto il resto è composizione di testo.
class EsportaRiassuntoTest {

    private val cartella = File(System.getProperty("java.io.tmpdir"), "riassunto-${System.nanoTime()}")

    private val manifest = Manifest(
        id = "01fftd", version = "1.0.0", title = "Flight from the Dark",
        description = "Prova", language = "English", genre = "FANTASY", scenes = emptyList(),
    )

    private fun scena(id: String, testo: String = "Testo della scena $id") = Scene(
        id = id,
        sceneType = SceneType.TRANSITION,
        genre = "FANTASY",
        narrativeText = testo,
    )

    @AfterTest
    fun pulisci() {
        cartella.deleteRecursively()
    }

    @Test
    fun ilTitoloDiceLibroEIntervalloDiScene() {
        val percorso = listOf(scena("1"), scena("85"), scena("141"))

        val esito = EsportaRiassunto.scrivi(
            File(cartella, "riassunto_scene_1-141.md"),
            manifest,
            percorso,
            "Il viaggio comincia al monastero.",
        )

        assertTrue(esito.isSuccess, esito.exceptionOrNull()?.message.orEmpty())
        val testo = esito.getOrThrow().fileMd.readText()
        assertContains(testo, "# Riassunto di Flight from the Dark — Da scena 1 a 141")
        assertContains(testo, "Il viaggio comincia al monastero.")
        // Il percorso in chiaro serve a rifare la stessa strada nel libro.
        assertContains(testo, "1 → 85 → 141")
    }

    @Test
    fun aggiungeLEstensioneMdQuandoManca() {
        val esito = EsportaRiassunto.scrivi(File(cartella, "senza-estensione"), manifest, listOf(scena("1")), "Testo.")

        assertTrue(esito.getOrThrow().fileMd.name.endsWith(".md"))
    }

    // Gli `url:` restano link, senza tentare copie: puntano fuori dal
    // programma e devono restare tali.
    @Test
    fun leImmaginiRemoteRestanoUrl() {
        val percorso = listOf(
            scena("1").copy(backgroundImage = "url:https://example.invalid/bosco.png"),
            scena("2").copy(
                combat = Combat(
                    enemyName = "Giak",
                    enemyCombatSkill = 12,
                    enemyEndurance = 20,
                    winSceneId = "3",
                    enemyImage = "url:https://example.invalid/giak.png",
                ),
            ),
        )

        val testo = EsportaRiassunto.scrivi(File(cartella, "r.md"), manifest, percorso, "Testo.")
            .getOrThrow().fileMd.readText()

        assertContains(testo, "<https://example.invalid/bosco.png>")
        assertContains(testo, "Scena 2 — nemico")
        assertContains(testo, "<https://example.invalid/giak.png>")
    }

    // Un id statico che non esiste fra le risorse non deve far fallire
    // l'esportazione: si segnala in fondo e il documento resta valido.
    @Test
    fun unImmagineStaticaSconosciutaVieneSegnalataSenzaRompere() {
        val percorso = listOf(scena("1").copy(backgroundImage = "static:loc_inesistente"))

        val esito = EsportaRiassunto.scrivi(File(cartella, "r.md"), manifest, percorso, "Testo.")

        assertTrue(esito.isSuccess)
        assertTrue(esito.getOrThrow().immaginiNonTrovate.isNotEmpty())
        assertContains(esito.getOrThrow().fileMd.readText(), "loc_inesistente")
    }

    // Le immagini del catalogo vivono dentro il pacchetto dell'editor:
    // vanno estratte accanto al .md, altrimenti il link non aprirebbe
    // nulla su un'altra macchina.
    @Test
    fun unImmagineStaticaVeraFinisceNellaCartellaAccanto() {
        val idVero = StaticResourceCatalog.registry.locations.firstOrNull()?.id
        if (idVero == null) {
            println("SALTATO: nessuna location nel catalogo")
            return
        }
        val percorso = listOf(scena("1").copy(backgroundImage = "static:$idVero"))

        val esito = EsportaRiassunto.scrivi(File(cartella, "r.md"), manifest, percorso, "Testo.").getOrThrow()

        assertTrue(esito.immaginiCopiate == 1, "L'immagine doveva essere copiata accanto al documento")
        val cartellaImmagini = File(cartella, "r_immagini")
        assertTrue(cartellaImmagini.isDirectory && cartellaImmagini.listFiles()!!.isNotEmpty())
        assertContains(esito.fileMd.readText(), "r_immagini/")
    }
}
