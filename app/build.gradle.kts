// :app — Android: UI Compose, inferenza LiteRT-LM, TTS, storage.
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // Serve al @Serializable di DownloadableModel (ModelPreferences.customModels):
    // solo la libreria runtime non basta, senza il plugin il serializer non
    // viene generato e Json.encodeToString fallisce a runtime (crash 22/07).
    alias(libs.plugins.kotlin.serialization)
}

// Sperimentale (27/07/2026, branch feature/llama-cpp-adreno): Llamatik e
// :llama impacchettano ENTRAMBI .so ggml/llama.cpp con gli stessi nomi ma
// da build DIVERSE (versioni di llama.cpp diverse) — non solo un
// conflitto di nomi al build, ma un'INCOMPATIBILITÀ BINARIA vera: col
// primo tentativo di convivenza (pickFirst) l'app compilava, ma
// LlamaBridge di Llamatik crashava a runtime
// (UnsatisfiedLinkError: "cannot locate symbol llama_model_n_embd_inp")
// perché si ritrovava linkato contro IL NOSTRO libggml, non il suo.
// Le due dipendenze sono quindi MUTUAMENTE ESCLUSIVE, non convivono nello
// stesso APK: con buildLlama=true si perde temporaneamente il motore
// Llamatik (LlamaCppEngine) per guadagnare quello nativo con GPU Adreno
// vera (NativeLlamaCppEngine) — tradeoff accettabile solo su questo
// branch sperimentale, per isolare il confronto CPU/Llamatik vs
// GPU/nativo un motore alla volta.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val buildLlamaNative = localProperties.getProperty("buildLlama")?.toBoolean() ?: false

android {
    namespace = "io.github.luposolitario.immundanoctisex"
    // 36, non più 35 (27/07/2026): richiesto da Llamatik (spike GGUF),
    // che dichiara AAR metadata con minCompileSdk 36. Non tocca
    // minSdk/targetSdk (restano 34) — compileSdk più alto del target è
    // normale e supportato, cambia solo contro quali API si compila.
    compileSdk = 36

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

    // Llamatik SEMPRE disponibile a compile-time (compileOnly): così
    // LlamaCppEngine.kt/AppContainer.kt compilano invariati in entrambi i
    // casi, senza codice condizionale sparso. A runtime/pacchetto invece
    // è mutuamente esclusivo con :llama (vedi `buildLlamaNative` sopra):
    // runtimeOnly lo aggiunge SOLO quando :llama non è quello attivo,
    // altrimenti i due .so ggml incompatibili finiscono insieme
    // nell'APK e Llamatik crasha al primo uso
    // (UnsatisfiedLinkError: "cannot locate symbol llama_model_n_embd_inp").
    compileOnly("com.llamatik:library:1.9.1")
    if (buildLlamaNative) {
        implementation(project(":llama"))
    } else {
        // 1.9.1 (27/07/2026): changelog ufficiale "Solved generateStream
        // emoji crash" — verificare se risolve anche il crash UTF-8 su
        // caratteri accentati italiani trovato con 1.7.0 (vedi DIARIO.md).
        runtimeOnly("com.llamatik:library:1.9.1")
    }

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
