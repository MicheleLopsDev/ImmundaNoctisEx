// :core:engine — GameState, regole, combat, comandi. KMP puro, zero dipendenze Android.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    jvmToolchain(17)
    jvm()
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:data"))
        }
        commonTest.dependencies {
        }
    }
}

android {
    namespace = "io.github.luposolitario.immundanoctisex.core.engine"
    compileSdk = 35

    defaultConfig {
        minSdk = 34
    }
}