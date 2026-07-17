// :core:engine — GameState, regole, combat, comandi. KMP puro, zero dipendenze Android.
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
            implementation(project(":core:data"))
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        // Il test di milestone della Fase 2 gioca il libro di esempio vero:
        // content/ entra nel classpath di test senza copie da tenere allineate.
        jvmTest {
            resources.srcDir(rootDir.resolve("content"))
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