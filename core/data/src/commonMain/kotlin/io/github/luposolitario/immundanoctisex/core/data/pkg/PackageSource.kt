package io.github.luposolitario.immundanoctisex.core.data.pkg

import java.io.InputStream

// Una delle quattro interfacce motivate (ARCHITETTURA.md): più implementazioni
// reali esistono davvero (asset dell'APK, file side-load scelto dal picker
// SAF, file temporaneo nei test). PackageRepository non conosce Context.
interface PackageSource {
    fun open(): InputStream
}
