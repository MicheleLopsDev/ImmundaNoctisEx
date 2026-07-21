// :app — Android: UI Compose, inferenza LiteRT-LM, TTS, storage.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Serve al @Serializable di DownloadableModel (ModelPreferences.customModels):
    // solo la libreria runtime non basta, senza il plugin il serializer non
    // viene generato e Json.encodeToString fallisce a runtime (crash 22/07).
    alias(libs.plugins.kotlin.serialization)
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
    buildFeatures {
        compose = true
    }
    sourceSets {
        getByName("main") {
            // Il libro incluso nell'APK: content/ (scenes.sample.json,
            // config.json) montato come cartella asset, niente copie.
            assets.srcDir(rootDir.resolve("content"))
        }
        getByName("test") {
            // Gli stessi contenuti sul classpath dei test JVM: i parser si
            // verificano contro i file VERI, non contro copie da allineare.
            resources.srcDir(rootDir.resolve("content"))
        }
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:engine"))

    // Serve a leggere i frammenti del prompt da content/config.json.
    implementation(libs.kotlinx.serialization.json)

    // Download del modello in background con notifica (Fase 4).
    implementation(libs.androidx.work.runtime.ktx)

    // Motore di inferenza on-device (LiteRT-LM).
    implementation(libs.litertlm.android)

    // LiteRT-LM dichiara coroutines 1.9.0 nel POM ma è compilato con
    // Kotlin 2.3: chiama `SendChannel.close$default` come metodo statico
    // dell'interfaccia, forma che 1.9.0 (compilato con Kotlin più
    // vecchio) non espone -> NoSuchMethodError a fine generazione.
    // Si forza una versione costruita con Kotlin recente.
    implementation(libs.kotlinx.coroutines.android)

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

// Kotlin 2.3: il vecchio blocco kotlinOptions è un errore, si usa il DSL
// compilerOptions (aggiornamento imposto da LiteRT-LM, compilato con 2.3).
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
