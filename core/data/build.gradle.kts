// :core:data — modelli, schema pacchetto, validatori. KMP puro, zero dipendenze Android.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
    jvm()
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
        }
    }
}

android {
    namespace = "io.github.luposolitario.immundanoctisex.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 34
    }
}