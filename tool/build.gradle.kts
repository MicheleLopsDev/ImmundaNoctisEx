// :tool — CLI di conversione libri (Fase 6, ETL) + editor grafico Compose
// Desktop (doc/EDITOR.md), stesso modulo per entrambi (Michele: "vorrei
// lasciare il tool proprio unico"). Primo pezzo reale (29/07/2026): un
// validatore a riga di comando, poi il convertitore Project Aon — riusano
// PackageRepository/PackageValidator di :core:data, stessa validazione
// dell'app.
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    // 30/07/2026: mancava — la libreria runtime kotlinx-serialization-json
    // era già una dipendenza, ma senza questo plugin le classi @Serializable
    // DEFINITE dentro :tool (non quelle di :core:data, generate lì) non
    // hanno un serializzatore generato a compile-time ("Serializer for
    // class 'X' is not found" a runtime, silenzioso se avvolto in
    // runCatching — scoperto scrivendo StaticResourceCatalog.kt).
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:engine"))
    // Parser HTML per la conversione libri Project Aon (29/07/2026): l'XHTML
    // "Internet Edition" è pulito ma resta HTML vero (entità, tag annidati,
    // commenti) — un parser robusto invece di regex fatte a mano.
    implementation(libs.jsoup)
    // Manifest/Scene referenziano JsonObject (GameMechanic.params):
    // :core:data lo dichiara "implementation", non transitivo a :tool.
    implementation(libs.kotlinx.serialization.json)

    // Editor grafico (30/07/2026, doc/EDITOR.md): Compose Desktop, stessa
    // famiglia concettuale di Jetpack Compose già in :app.
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    // Ascolto dei suoni delle risorse nell'editor (§15.6, 30/07/2026):
    // javax.sound di base non decodifica MP3 — JLayer è un decoder MP3
    // puro Java, gratuito (LGPL), lo standard de facto per questo caso
    // d'uso, nessun servizio esterno.
    implementation(libs.jlayer)

    // La logica pura (es. SceneGraph) si testa da terminale come i moduli
    // core, senza bisogno di avviare la GUI.
    testImplementation(kotlin("test"))
}

// Il plugin org.jetbrains.compose registra da sé un task `run` (lancia la
// finestra dell'editor) — in conflitto col task `run` del plugin
// `application` se lo tenessimo per la CLI (stesso nome, Gradle rifiuta di
// registrarne due). La CLI passa quindi da un JavaExec dedicato con nome
// diverso invece che dal plugin `application`.
tasks.register<JavaExec>("cli") {
    group = "application"
    description = "Esegue la CLI di :tool (validate/convert/illustrazioni), vedi Main.kt"
    mainClass.set("io.github.luposolitario.immundanoctisex.tool.MainKt")
    classpath = sourceSets["main"].runtimeClasspath
}

// jpackage (usato da createDistributable/packageMsi/packageExe) richiede un
// JDK completo, non il JBR imbustato in Android Studio — a quello manca
// jpackage.exe (verificato 30/07/2026: "Failed to check JDK distribution:
// 'jpackage.exe' is missing"). Percorso opzionale in local.properties
// (`packagingJdk=...`, stesso pattern di buildLlama/llamaCppDir sopra):
// se assente, si continua a usare qualunque JDK stia già usando Gradle
// (compilare/testare/`:tool:run` non ne hanno bisogno, solo impacchettare).
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val packagingJdk = localProperties.getProperty("packagingJdk")

compose.desktop {
    application {
        mainClass = "io.github.luposolitario.immundanoctisex.tool.editor.EditorMainKt"
        if (packagingJdk != null) {
            javaHome = packagingJdk
        }
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "ImmundaNoctisEx-Editor"
            packageVersion = "1.1.0"
        }
    }
}