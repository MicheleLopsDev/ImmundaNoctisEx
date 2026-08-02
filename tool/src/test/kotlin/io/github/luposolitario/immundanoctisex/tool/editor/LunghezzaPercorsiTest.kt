package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test

// Quanto è lungo un percorso da START a una scena qualunque, e quanto
// testo porta con sé? Da questo dipende se il riassunto sta in una sola
// richiesta al modello o va spezzato in blocchi.
class LunghezzaPercorsiTest {

    @Test
    fun misuraIPercorsiDeiLibriVeri() {
        val libri = File("C:/DEV/ImmundaNoctisEx/doc/LIBRI").listFiles { f -> f.extension == "json" }
            ?.sortedBy { it.name } ?: return
        val json = Json { ignoreUnknownKeys = true }

        libri.forEach { file ->
            val manifest = runCatching { json.decodeFromString(Manifest.serializer(), file.readText()) }
                .getOrNull() ?: return@forEach
            val grafo = buildSceneGraph(manifest)
            val testiPerId = manifest.scenes.associate { it.id to it.narrativeText }

            val misure = manifest.scenes.mapNotNull { scena ->
                val percorso = percorsoDaStart(grafo, scena.id).takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                percorso.size to percorso.sumOf { (testiPerId[it]?.length ?: 0) }
            }
            if (misure.isEmpty()) return@forEach

            val piuLungo = misure.maxByOrNull { it.first }!!
            val piuPesante = misure.maxByOrNull { it.second }!!
            println(
                "${file.name}: ${misure.size} scene raggiungibili — " +
                    "percorso medio ${misure.map { it.first }.average().toInt()} scene / " +
                    "${misure.map { it.second }.average().toInt()} car; " +
                    "più lungo ${piuLungo.first} scene; più pesante ${piuPesante.second} car",
            )
        }
    }
}
