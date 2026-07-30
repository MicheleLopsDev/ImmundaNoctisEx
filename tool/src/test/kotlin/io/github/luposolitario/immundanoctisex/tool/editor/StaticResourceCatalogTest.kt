package io.github.luposolitario.immundanoctisex.tool.editor

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StaticResourceCatalogTest {

    @Test
    fun ilRegistroSiCaricaConTutteLeCategorie() {
        val registro = StaticResourceCatalog.registry
        assertTrue(registro.tones.isNotEmpty())
        assertTrue(registro.locations.isNotEmpty())
        assertTrue(registro.npcs.isNotEmpty())
        assertTrue(registro.enemies.isNotEmpty())
    }

    @Test
    fun nessunTonoENiAutorNeSenzaSuggerimenti() {
        StaticResourceCatalog.registry.tones.forEach {
            assertTrue(it.id != "AUTHOR", "AUTHOR non è una scelta di scrittura, non deve comparire")
            assertTrue(it.hints.isNotEmpty(), "tono senza suggerimenti: ${it.id}")
        }
    }

    @Test
    fun ogniLocationHaUnaDescrizioneNonVuota() {
        StaticResourceCatalog.registry.locations.forEach {
            assertTrue(it.description.isNotBlank(), "descrizione mancante per ${it.id}")
        }
    }

    @Test
    fun leImmaginiBundledSiTrovanoPerOgniIdDelRegistro() {
        val tuttiGliId = StaticResourceCatalog.registry.locations.map { it.id } +
            StaticResourceCatalog.registry.npcs +
            StaticResourceCatalog.registry.enemies
        tuttiGliId.toSet().forEach { id ->
            assertNotNull(StaticResourceCatalog.percorsoImmagine(id), "immagine mancante per $id")
        }
    }

    @Test
    fun unIdSconosciutoNonTrovaAlcunaImmagine() {
        assertNull(StaticResourceCatalog.percorsoImmagine("id_inesistente_xyz"))
    }
}
