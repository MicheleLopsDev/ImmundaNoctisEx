// :tool — Compose Desktop, wizard di conversione libri. Fase 6 (ETL).
// Primo pezzo reale (29/07/2026, Michele: "un processo che per adesso sarà
// lanciato da un main kotlin che valida un json"): un validatore a riga di
// comando, prima ancora della UI Compose Desktop — riusa PackageRepository/
// PackageValidator di :core:data, stessa validazione dell'app.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("io.github.luposolitario.immundanoctisex.tool.MainKt")
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:engine"))
    // Parser HTML per la conversione libri Project Aon (29/07/2026): l'XHTML
    // "Internet Edition" è pulito ma resta HTML vero (entità, tag annidati,
    // commenti) — un parser robusto invece di regex fatte a mano.
    implementation(libs.jsoup)
    // Manifest/Scene referenziano JsonObject (GameMechanic.params):
    // :core:data lo dichiara "implementation", non transitivo a :tool.
    implementation(libs.kotlinx.serialization.json)
}