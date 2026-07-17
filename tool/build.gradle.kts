// :tool — Compose Desktop, wizard di conversione libri. Placeholder: riempito in Fase 6 (ETL).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:engine"))
}