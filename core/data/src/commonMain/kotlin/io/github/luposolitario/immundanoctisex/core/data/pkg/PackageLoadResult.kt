package io.github.luposolitario.immundanoctisex.core.data.pkg

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest

// Esito del caricamento: il gioco non si blocca mai, un pacchetto rotto
// viene rifiutato con messaggi chiari invece di far esplodere il parsing.
sealed interface PackageLoadResult {
    data class Success(val manifest: Manifest, val warnings: List<String> = emptyList()) : PackageLoadResult
    data class Failure(val errors: List<String>) : PackageLoadResult
}
