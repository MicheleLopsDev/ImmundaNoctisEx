// :app — Android: UI Compose, inferenza LiteRT-LM, TTS, storage.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.luposolitario.immundanoctisex"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.luposolitario.immundanoctisex"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:engine"))
}