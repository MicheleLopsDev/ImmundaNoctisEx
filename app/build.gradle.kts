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

        // BUG (27/07/2026, Michele: "il solito bug... non attiva dopo il
        // click"): un .gguf aggiunto a mano (link Hugging Face) finiva
        // sempre su EngineType.LLAMA_CPP (Llamatik) — assente quando
        // buildLlama=true, fallimento silenzioso identico a quello delle
        // vecchie voci di catalogo. ModelsRoute.engineTypeFor() legge
        // questo flag per scegliere il motore nativo di default quando
        // Llamatik non è nemmeno impacchettato.
        buildConfigField("boolean", "NATIVE_LLAMA_AVAILABLE", buildLlamaNative.toString())
    }

    // Firma di release (30/07/2026, Michele: "vanno firmati... altrimenti
    // non girano"): un APK non firmato Android non lo installa affatto —
    // il debug lo è già, in automatico, con una chiave usa-e-getta rigenerata
    // a ogni macchina. Qui invece una chiave VERA, la stessa a ogni build,
    // necessaria per poter aggiornare un'installazione esistente in futuro
    // (chiavi diverse = Android rifiuta l'aggiornamento). Percorso e password
    // SOLO in local.properties (mai in git, stesso principio di
    // packagingJdk/llamaCppDir sopra) — se `releaseStoreFile` manca (es. CI
    // o macchina di qualcun altro), il buildType release semplicemente non
    // ha un signingConfig e resta un APK di release non firmato, senza far
    // fallire la build per chi non deve pubblicare nulla.
    val releaseStoreFile = localProperties.getProperty("releaseStoreFile")
    if (releaseStoreFile != null) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = localProperties.getProperty("releaseStorePassword")
                keyAlias = localProperties.getProperty("releaseKeyAlias")
                keyPassword = localProperties.getProperty("releaseKeyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        getByName("main") {
            // Il libro incluso nell'APK: content/ (scenes.sample.json)
            // montato come cartella asset, niente copie.
            assets.srcDir(rootDir.resolve("content"))
        }
    }

}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:engine"))

    // Serve al catalogo modelli LLM (ModelCatalog.kt) e alle preferenze
    // salvate (ModelPreferences.kt) — non più al prompt (PromptBuilder.kt
    // non legge più nessun JSON esterno, 31/07/2026).
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

    // Caricamento delle immagini url: (29/07/2026, vedi ImageReference):
    // solo per libri di uso personale mai distribuiti (doc/LIBRI/), che
    // linkano l'illustrazione originale invece di impacchettarne una
    // nuova nel catalogo static:.
    implementation(libs.coil.compose)
    // Decodifica GIF/WebP animato (31/07/2026, richiesta di terzi a
    // Michele su libri-fumetto animati, doc/UPGRADE.md §7) — stessa
    // versione di coil-compose.
    implementation(libs.coil.gif)

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
