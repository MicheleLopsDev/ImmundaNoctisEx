// File di build radice: dichiara i plugin condivisi, nessuno applicato qui.
import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// Libri di prova sul telefono (01/08/2026, Michele: "tutti i file di
// test devono essere spostati in test-books e poi copiati in una
// cartella sotto la sd nella cartella download, aggiungi un task che lo
// fa quando aggiungiamo un file di test"): il side-load dell'app legge
// da un file scelto col picker di sistema, quindi ogni libro nuovo va
// prima portato sul device a mano. Questo task lo fa in un colpo solo.
//
//   ./gradlew pushTestBooks
//
// Copia TUTTO content/test-books/ in /sdcard/Download/ImmundaNoctisEx/
// (adb push sovrascrive: rilanciarlo dopo ogni modifica di un libro di
// prova è sempre sicuro). Non è agganciato a nessun altro task di
// proposito: dipende da un device collegato, non deve far fallire una
// build normale.
tasks.register<Exec>("pushTestBooks") {
    // Tutto risolto QUI, in fase di configurazione, e catturato come
    // valori semplici: un lambda che leggesse variabili dello script
    // romperebbe la configuration cache ("cannot serialize Gradle script
    // object references").
    val destinazione = "/sdcard/Download/ImmundaNoctisEx"
    val sorgente = layout.projectDirectory.dir("content/test-books").asFile
    val adb = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }.getProperty("sdk.dir")
        ?.let { File(it, "platform-tools/adb${if (System.getProperty("os.name").startsWith("Windows")) ".exe" else ""}") }

    group = "immundanoctisex"
    description = "Copia i libri di prova (content/test-books) in $destinazione sul telefono collegato."

    // Il "/." finale è essenziale: `adb push <cartella> <dest>` copia il
    // CONTENUTO solo se <dest> non esiste ancora, altrimenti annida
    // (dest/test-books/...) e al secondo lancio ti ritrovi i libri in
    // due posti. Con "<cartella>/." copia sempre e solo il contenuto,
    // quindi il task è idempotente: rilanciarlo dopo aver aggiunto o
    // modificato un libro di prova sovrascrive e basta.
    commandLine(adb?.absolutePath ?: "adb", "push", "${sorgente.absolutePath}/.", destinazione)

    doFirst {
        require(sorgente.isDirectory) { "Cartella non trovata: $sorgente" }
        require(adb != null && adb.exists()) {
            "adb non trovato: sdk.dir manca in local.properties o platform-tools non è installato."
        }
    }
    doLast { logger.lifecycle("Libri di prova copiati in $destinazione (side-load dall'icona cartella in Home).") }
}