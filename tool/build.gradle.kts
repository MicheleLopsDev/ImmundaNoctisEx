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

// Java 21, non 17 come gli altri moduli (02/08/2026): `litertlm-jvm` è
// compilata per la 21 (class file major 65) e su una JVM 17 non si
// carica nemmeno — `UnsupportedClassVersionError` appena si tocca
// `Engine`. Alzare solo QUESTO modulo è sicuro: :tool dipende da
// :core:data e :core:engine (Java 17) e non viceversa, e un modulo 21
// legge senza problemi classi 17.
//
// Conseguenza sul packaging: anche `packagingJdk` in local.properties
// deve essere un JDK 21 con jpackage (il Temurin 17 di prima non sa
// creare un runtime per classi 21). La JBR di Android Studio è una 21
// ma non porta jpackage: per i pacchetti serve un Temurin 21 vero.
kotlin {
    jvmToolchain(21)
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

    // Lo STESSO motore del client, versione JVM (02/08/2026, Michele:
    // "possiamo usare quello che usiamo per il client e lo includiamo
    // perché voglio proprio avere una simulazione di quello che succede
    // sul client"). Stessa versione dell'AAR Android in :app, stesse
    // classi (Engine/Conversation/SamplerConfig), stesso file .litertlm
    // che scarica il telefono: l'anteprima nell'editor non è un
    // surrogato con un altro modello.
    implementation(libs.litertlm.jvm)
    // Il motore espone Flow: i chiamanti nell'editor li raccolgono.
    implementation(libs.kotlinx.coroutines.core)

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

// I libri Project Aon si versionano SENZA prosa (README §15): in
// `doc/LIBRI/` restano le meccaniche, i testi se li scarica chi vuole
// giocarli, sulla propria macchina. Questo task fa quel download per
// tutti i libri in una volta.
//
//   ./gradlew :tool:riempiTesti
//
// I file prodotti finiscono in `build/libri/` — dentro `build/`, quindi
// già fuori da git: non è una svista, è il punto.
// Il gemello di `riempiTesti`: prepara i libri per il repository
// togliendo la prosa. Si esegue UNA VOLTA, quando si decide di
// versionare le sole meccaniche; da lì in poi si lavora sempre sugli
// scheletri. L'id Project Aon si deduce dal nome del file
// (`01fftd.json` -> `01fftd`).
//
//   ./gradlew :tool:svuotaTesti
tasks.register("svuotaTesti") {
    group = "immundanoctisex"
    description = "Toglie la prosa dai libri in doc/LIBRI, lasciando le meccaniche (una tantum)"
    dependsOn(tasks.named("classes"))
    doLast {
        val libri = file("${rootDir}/doc/LIBRI").listFiles { f -> f.extension == "json" }?.sorted().orEmpty()
        if (libri.isEmpty()) {
            println("Nessun libro in doc/LIBRI/.")
            return@doLast
        }
        libri.forEach { libro ->
            println("\n--- ${libro.name} ---")
            javaexec {
                mainClass.set("io.github.luposolitario.immundanoctisex.tool.MainKt")
                classpath = sourceSets["main"].runtimeClasspath
                args = listOf("svuotaTesti", libro.absolutePath, libro.nameWithoutExtension)
                defaultCharacterEncoding = "UTF-8"
                isIgnoreExitValue = true
            }
        }
    }
}

tasks.register("riempiTesti") {
    group = "immundanoctisex"
    description = "Scarica da Project Aon i testi dei libri in doc/LIBRI e li innesta negli scheletri (uso personale)"
    dependsOn(tasks.named("classes"))
    doLast {
        val scheletri = file("${rootDir}/doc/LIBRI").listFiles { f -> f.extension == "json" }?.sorted().orEmpty()
        if (scheletri.isEmpty()) {
            println("Nessun libro in doc/LIBRI/.")
            return@doLast
        }
        println("Trovati ${scheletri.size} libri. I testi restano su questa macchina e non vanno redistribuiti.")
        scheletri.forEach { scheletro ->
            println("\n--- ${scheletro.name} ---")
            javaexec {
                mainClass.set("io.github.luposolitario.immundanoctisex.tool.MainKt")
                classpath = sourceSets["main"].runtimeClasspath
                args = listOf("riempiTesti", scheletro.absolutePath)
                // Senza questo, su Windows la JVM figlia stampa nella
                // codepage di sistema e gli accenti escono a pezzi.
                defaultCharacterEncoding = "UTF-8"
                // Un libro senza `textsFrom` (uno tuo, uno di prova) esce
                // con codice 2: non è un errore del task, è un libro che
                // i testi ce li ha già.
                isIgnoreExitValue = true
            }
        }
    }
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

// Guardia sulla versione del JDK di packaging (02/08/2026). Da quando
// c'è `litertlm-jvm` (compilata per Java 21) un `packagingJdk` a 17
// produce un pacchetto che SI COSTRUISCE E PARTE, ma esplode con
// UnsupportedClassVersionError appena si apre la schermata Modello: la
// JVM imbustata da jlink sarebbe una 17. Un guasto del genere si
// scoprirebbe solo dopo aver installato l'MSI, quindi lo si intercetta
// qui, dove il messaggio può ancora dire cosa fare.
// Vale SOLO per i task che impacchettano: compilare, testare e lanciare
// `:tool:run` continuano a funzionare con qualunque JDK 21 già visto da
// Gradle (la JBR di Android Studio va benissimo), senza pretendere di
// averne uno con jpackage installato.
val versionePackagingJdk = packagingJdk
    ?.let { File(it, "release") }
    ?.takeIf { it.exists() }
    ?.readLines()
    ?.firstOrNull { it.startsWith("JAVA_VERSION=") }
    ?.substringAfter('=')
    ?.trim('"')
val packagingJdkMaggiore = versionePackagingJdk?.substringBefore('.')?.toIntOrNull()
// `javaHome` vale anche per `:tool:run`, non solo per jpackage: puntarlo
// a un JDK 17 farebbe fallire l'avvio dell'editor (classi 21) ancora
// prima di arrivare a impacchettare. Finché il JDK indicato non è
// adeguato lo si ignora del tutto — Gradle usa allora la toolchain 21.
val packagingJdkUtilizzabile = packagingJdk?.takeIf { packagingJdkMaggiore == null || packagingJdkMaggiore >= 21 }

if (packagingJdk != null) {
    if (packagingJdkMaggiore != null && packagingJdkMaggiore < 21) {
        // Solo valori catturati (stringhe), nessun riferimento allo
        // script: la configuration cache di Gradle rifiuterebbe il resto.
        val messaggio = "packagingJdk punta a un JDK $versionePackagingJdk, ma :tool richiede " +
            "Java 21 da quando include litertlm-jvm (il motore del client). Un pacchetto " +
            "costruito con questo JDK si installa e si avvia, ma va in " +
            "UnsupportedClassVersionError appena si apre la schermata Modello: la JVM " +
            "imbustata sarebbe una 17. Scarica un Temurin 21 con jpackage e aggiorna " +
            "packagingJdk in local.properties (ora: $packagingJdk)."
        // `checkRuntime` compreso, ed è il primo della catena: senza, il
        // fallimento arriva da lì con un "jpackage.exe is missing" che
        // punta alla JBR di Android Studio e non dice nulla di utile —
        // il JDK indicato in local.properties viene ignorato apposta
        // (vedi `packagingJdkUtilizzabile` sotto) proprio perché è
        // troppo vecchio.
        tasks.matching {
            it.name in setOf(
                "checkRuntime",
                "createDistributable",
                "packageMsi",
                "packageExe",
                "packageDistributionForCurrentOS",
            )
        }.configureEach { doFirst { throw GradleException(messaggio) } }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.luposolitario.immundanoctisex.tool.editor.EditorMainKt"
        if (packagingJdkUtilizzabile != null) {
            javaHome = packagingJdkUtilizzabile
        }
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "ImmundaNoctisEx-Editor"
            packageVersion = "1.3.0"
        }
    }
}