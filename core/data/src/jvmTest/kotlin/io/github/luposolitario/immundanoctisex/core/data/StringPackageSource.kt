package io.github.luposolitario.immundanoctisex.core.data

import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageSource

// PackageSource di test: il pacchetto è già in memoria come stringa
// (fixture reale letta da risorsa, o JSON rotto scritto a mano nel test).
class StringPackageSource(private val json: String) : PackageSource {
    override fun open() = json.byteInputStream()
}
