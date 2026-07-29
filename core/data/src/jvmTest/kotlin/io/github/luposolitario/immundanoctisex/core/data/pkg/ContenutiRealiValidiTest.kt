package io.github.luposolitario.immundanoctisex.core.data.pkg

import io.github.luposolitario.immundanoctisex.core.data.StringPackageSource
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

// Rete di sicurezza (29/07/2026, Michele: "questo validatore ci servirà
// sempre e dovrà essere integrato nei nostri test"): ogni libro che
// spediamo davvero in content/ deve superare PackageValidator, non solo
// le fixture scritte a mano per i test dei singoli validatori
// (PackageValidatorTest). Legge i file VERI dal repo via la system
// property configurata in build.gradle.kts — non una copia duplicata
// sotto src/jvmTest/resources, che è già andata fuori sincrono una volta
// (la vecchia fixture "scenes.sample.json" lì dentro aveva backgroundImage
// non canonici e mancava outcome/enemyImage). Un file rotto in content/
// fa fallire la build subito, prima che qualcuno lo scopra giocando.
class ContenutiRealiValidiTest {

    private val contentDir = File(
        requireNotNull(System.getProperty("immundanoctisex.contentDir")) {
            "systemProperty immundanoctisex.contentDir non impostata (vedi core/data/build.gradle.kts)"
        },
    )

    // config.json è il registro tag del prompt, non un libro (Manifest) —
    // unico file JSON di content/ da escludere dalla validazione.
    private fun libriReali(): List<File> =
        contentDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" && it.name != "config.json" }
            .toList()

    @Test
    fun ogniLibroInContentEValido() {
        val libri = libriReali()
        assertTrue(libri.isNotEmpty(), "Nessun file JSON trovato in ${contentDir.path}")

        val fallimenti = libri.mapNotNull { file ->
            when (val result = PackageRepository(StringPackageSource(file.readText())).load()) {
                is PackageLoadResult.Failure -> "${file.relativeTo(contentDir)}: ${result.errors.joinToString("; ")}"
                is PackageLoadResult.Success -> null
            }
        }

        assertTrue(fallimenti.isEmpty(), "Libri non validi:\n${fallimenti.joinToString("\n")}")
    }
}
