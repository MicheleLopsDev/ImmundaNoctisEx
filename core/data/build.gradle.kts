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
            implementation(kotlin("test"))
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

// Percorso reale di content/ (non una copia sotto jvmTest/resources, che è
// già andata fuori sincrono una volta — vedi ContenutiRealiValidiTest):
// così il test jvmTest legge sempre gli stessi file che spediamo davvero,
// qualunque sia la working directory con cui gira Gradle.
tasks.withType<Test>().configureEach {
    systemProperty("immundanoctisex.contentDir", rootProject.layout.projectDirectory.dir("content").asFile.absolutePath)
}