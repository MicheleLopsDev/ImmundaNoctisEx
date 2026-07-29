package io.github.luposolitario.immundanoctisex.tool

import io.github.luposolitario.immundanoctisex.core.data.pkg.PackageSource
import java.io.File
import java.io.InputStream

// Implementazione desktop di PackageSource (una delle quattro interfacce
// motivate, ARCHITETTURA.md): un file qualunque del filesystem locale.
// L'app Android ha le sue (AssetPackageSource/UriPackageSource in
// AppContainer.kt) — questa vive qui perché la usa solo il tool.
class FilePackageSource(private val file: File) : PackageSource {
    override fun open(): InputStream = file.inputStream()
}
