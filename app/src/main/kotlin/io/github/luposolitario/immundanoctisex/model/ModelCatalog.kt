package io.github.luposolitario.immundanoctisex.model

import kotlinx.serialization.Serializable

// Quale InferenceEngine sa caricare questo modello (27/07/2026, Michele:
// "introdurrei la possibilità di caricare i gguf" — un motore vero e
// selezionabile, non solo lo spike). Default LITERT_LM: i modelli già
// serializzati nelle preferenze di chi usava l'app prima di questo giro
// non hanno questo campo nel JSON salvato, kotlinx.serialization lo
// riempie col default senza rompere nulla.
// LLAMA_CPP_NATIVE (27/07/2026, branch sperimentale feature/llama-cpp-adreno):
// llama.cpp compilato da noi (:llama) con backend OpenCL/Adreno vero,
// contro LLAMA_CPP che gira su Llamatik (sempre CPU su Android). Vedi
// doc/LLAMA-CPP-ADRENO-SETUP.md per la compilazione nativa (spenta di
// default, buildLlama in local.properties) — un modello con questo tipo
// fallisce al caricamento se non è stata compilata.
@Serializable
enum class EngineType { LITERT_LM, LLAMA_CPP, LLAMA_CPP_NATIVE }

// Un modello scaricabile (erede di Downloadable di v1, con in più la
// dimensione attesa e il flag "serve un token").
// @Serializable: i modelli aggiunti da Michele con un link Hugging Face
// (ModelPreferences.customModels) si salvano come JSON nelle preferenze.
@Serializable
data class DownloadableModel(
    val id: String,
    val displayName: String,
    val url: String,
    val fileName: String,
    val sizeBytes: Long,
    val requiresToken: Boolean,
    val note: String,
    val custom: Boolean = false,
    val engineType: EngineType = EngineType.LITERT_LM,
) {
    val sizeGigabytes: Double get() = sizeBytes / 1_000_000_000.0
}

// Il catalogo dei modelli offerti dall'app. Dimensioni e stato di gating
// VERIFICATI con richieste HEAD il 19/07/2026, non stimati.
// Nota: v1 usava google/gemma-3n-E4B (repo GATED: senza token risponde
// 401 e si scaricherebbe una pagina d'errore al posto del modello).
// I due litert-community sono aperti, così l'app funziona anche a chi non
// ha un account Hugging Face.
object ModelCatalog {

    val GEMMA_4_E4B = DownloadableModel(
        id = "gemma-4-e4b-it",
        displayName = "Gemma 4 E4B (consigliato)",
        url = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
        fileName = "gemma-4-E4B-it.litertlm",
        sizeBytes = 3_659_530_240L,
        requiresToken = false,
        note = "Qualità migliore. È il fratello del modello già provato su v1.",
    )

    val GEMMA_4_E2B = DownloadableModel(
        id = "gemma-4-e2b-it",
        displayName = "Gemma 4 E2B (leggero)",
        url = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        fileName = "gemma-4-E2B-it.litertlm",
        sizeBytes = 2_588_147_712L,
        requiresToken = false,
        note = "Più piccolo e veloce: la scelta se il telefono scotta o la memoria stringe.",
    )

    // Il modello di v1. Resta nel catalogo per continuità, ma serve un
    // token Hugging Face con la licenza Gemma accettata.
    val GEMMA_3N_E4B_GATED = DownloadableModel(
        id = "gemma-3n-e4b-it",
        displayName = "Gemma 3n E4B (repo riservato)",
        url = "https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task",
        fileName = "gemma-3n-E4B-it-int4.task",
        sizeBytes = 0L, // sconosciuta: il repo non risponde senza token
        requiresToken = true,
        note = "Il modello usato in v1. Richiede un token Hugging Face e la licenza accettata.",
    )

    // GGUF via llama.cpp (27/07/2026, Michele l'ha provato in LM Studio:
    // prosa italiana fluida e in formato corretto, a fronte di una
    // velocità però mai misurata su device). Quantizzazione IQ4_XS: la
    // stessa già testata e validata da Michele, non una "migliore" mai
    // provata. Solo testo (niente file mmproj): qui non serve il
    // multimodale. Dimensione VERIFICATA con richiesta HEAD il 27/07/2026.
    val GEMMA_3_12B_HERETIC_GGUF = DownloadableModel(
        id = "gemma-3-12b-heretic-gguf",
        displayName = "Gemma 3 12B Heretic Uncensored (GGUF)",
        url = "https://huggingface.co/mradermacher/gemma-3-12b-it-ultra-uncensored-heretic-GGUF/resolve/main/gemma-3-12b-it-ultra-uncensored-heretic.IQ4_XS.gguf",
        fileName = "gemma-3-12b-it-ultra-uncensored-heretic.IQ4_XS.gguf",
        sizeBytes = 6_606_262_336L,
        requiresToken = false,
        note = "Motore GGUF (llama.cpp): prosa più ricca del 4B nei test su LM Studio. Solo testo, nessun supporto immagini.",
        engineType = EngineType.LLAMA_CPP,
    )

    // Gemma 3 4B "di serie" in GGUF, direttamente da Google (QAT: Quantization
    // Aware Training, la qualità resta vicina al bfloat16 nonostante il
    // Q4_0). Stesso repo GATED del vecchio Gemma 3n (401 senza token, verificato
    // il 27/07/2026 con richiesta HEAD) — dimensione sconosciuta finché non si
    // scarica con un token valido, stesso trattamento di GEMMA_3N_E4B_GATED.
    val GEMMA_3_4B_GOOGLE_GGUF = DownloadableModel(
        id = "gemma-3-4b-google-gguf",
        displayName = "Gemma 3 4B (GGUF, ufficiale Google)",
        url = "https://huggingface.co/google/gemma-3-4b-it-qat-q4_0-gguf/resolve/main/gemma-3-4b-it-q4_0.gguf",
        fileName = "gemma-3-4b-it-q4_0.gguf",
        sizeBytes = 0L, // sconosciuta: il repo non risponde senza token
        requiresToken = true,
        note = "Motore GGUF (llama.cpp), modello Google senza fine-tuning di terzi. Richiede un token Hugging Face e la licenza Gemma accettata.",
        // BUG (27/07/2026, Michele: "si blocca sempre"): questa voce è
        // nata PRIMA del branch feature/llama-cpp-adreno, con l'unico
        // motore GGUF che esisteva allora (Llamatik). Su questo branch
        // Llamatik è compileOnly (non impacchettato quando buildLlama=true,
        // vedi app/build.gradle.kts) — attivarla con EngineType.LLAMA_CPP
        // instradava su un motore assente a runtime, fallendo in silenzio
        // (NoClassDefFoundError catturato da runCatching, nessun log).
        // Q4_0 è proprio il formato per cui vogliamo provare il motore
        // nativo con GPU Adreno.
        engineType = EngineType.LLAMA_CPP_NATIVE,
    )

    // Abliterated: tecnica diversa dal fine-tuning "uncensored" di Heretic
    // sopra (rimuove la direzione di rifiuto nei pesi invece di riaddestrare
    // su un dataset) — di mlabonne, autore di riferimento per questa tecnica,
    // quantizzati da bartowski. Q4_K_M: compromesso qualità/dimensione
    // standard di bartowski, non ancora provato da Michele su scene vere.
    // Dimensioni VERIFICATE con richiesta HEAD il 27/07/2026.
    val GEMMA_3_4B_ABLITERATED_GGUF = DownloadableModel(
        id = "gemma-3-4b-abliterated-gguf",
        displayName = "Gemma 3 4B Abliterated (GGUF)",
        url = "https://huggingface.co/bartowski/mlabonne_gemma-3-4b-it-abliterated-GGUF/resolve/main/mlabonne_gemma-3-4b-it-abliterated-Q4_K_M.gguf",
        fileName = "mlabonne_gemma-3-4b-it-abliterated-Q4_K_M.gguf",
        sizeBytes = 2_489_894_304L,
        requiresToken = false,
        note = "Motore GGUF (llama.cpp): censura rimossa per abliterazione, non per fine-tuning. Non ancora provato su scene vere.",
        // Stesso bug/correzione di GEMMA_3_4B_GOOGLE_GGUF sopra: su questo
        // branch Llamatik non è impacchettato, LLAMA_CPP fallirebbe in
        // silenzio.
        engineType = EngineType.LLAMA_CPP_NATIVE,
    )

    val GEMMA_3_12B_ABLITERATED_GGUF = DownloadableModel(
        id = "gemma-3-12b-abliterated-gguf",
        displayName = "Gemma 3 12B Abliterated (GGUF)",
        url = "https://huggingface.co/bartowski/mlabonne_gemma-3-12b-it-abliterated-GGUF/resolve/main/mlabonne_gemma-3-12b-it-abliterated-Q4_K_M.gguf",
        fileName = "mlabonne_gemma-3-12b-it-abliterated-Q4_K_M.gguf",
        sizeBytes = 7_300_778_656L,
        requiresToken = false,
        note = "Motore GGUF (llama.cpp): censura rimossa per abliterazione, non per fine-tuning. Non ancora provato su scene vere.",
        // Stesso bug/correzione di GEMMA_3_4B_GOOGLE_GGUF sopra: su questo
        // branch Llamatik non è impacchettato, LLAMA_CPP fallirebbe in
        // silenzio.
        engineType = EngineType.LLAMA_CPP_NATIVE,
    )

    // Stesso file di GEMMA_3_12B_HERETIC_GGUF sopra (stesso url/fileName):
    // se è già scaricato, questa voce risulta già pronta, nessun secondo
    // download da 6,6GB. Cambia solo il motore che lo carica — per il
    // primo confronto reale CPU (Llamatik) vs GPU Adreno (:llama),
    // Michele, 27/07: "usiamo gemma 3-12b".
    val GEMMA_3_12B_HERETIC_NATIVE = DownloadableModel(
        id = "gemma-3-12b-heretic-native",
        displayName = "Gemma 3 12B Heretic — llama.cpp nativo (GPU Adreno, sperimentale)",
        url = GEMMA_3_12B_HERETIC_GGUF.url,
        fileName = GEMMA_3_12B_HERETIC_GGUF.fileName,
        sizeBytes = GEMMA_3_12B_HERETIC_GGUF.sizeBytes,
        requiresToken = false,
        note = "Come sopra, ma tramite :llama compilato con backend OpenCL/Adreno vero invece di Llamatik (CPU-only). Richiede buildLlama=true in local.properties.",
        engineType = EngineType.LLAMA_CPP_NATIVE,
    )

    val all = listOf(
        GEMMA_4_E4B,
        GEMMA_4_E2B,
        GEMMA_3N_E4B_GATED,
        GEMMA_3_12B_HERETIC_GGUF,
        GEMMA_3_4B_GOOGLE_GGUF,
        GEMMA_3_4B_ABLITERATED_GGUF,
        GEMMA_3_12B_ABLITERATED_GGUF,
        GEMMA_3_12B_HERETIC_NATIVE,
    )

    val default = GEMMA_4_E4B

    fun byId(id: String?): DownloadableModel? = all.firstOrNull { it.id == id }
}
