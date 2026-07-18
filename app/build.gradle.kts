// :app — Android: UI Compose, inferenza LiteRT-LM, TTS, storage.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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
    buildFeatures {
        compose = true
    }
    sourceSets {
        getByName("main") {
            // Il libro incluso nell'APK: content/ (scenes.sample.json,
            // config.json) montato come cartella asset, niente copie.
            assets.srcDir(rootDir.resolve("content"))
        }
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:engine"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Le classi pure di :app (PromptBuilder, ResponseParser) si testano
    // da terminale come i moduli core: stesso stile kotlin.test.
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    debugImplementation(libs.androidx.ui.tooling)
}
