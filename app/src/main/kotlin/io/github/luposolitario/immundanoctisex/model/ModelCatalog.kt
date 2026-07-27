package io.github.luposolitario.immundanoctisex.model

import kotlinx.serialization.Serializable

// Quale InferenceEngine sa caricare questo modello (27/07/2026, Michele:
// "introdurrei la possibilità di caricare i gguf" — un motore vero e
// selezionabile, non solo lo spike). Default LITERT_LM: i modelli già
// serializzati nelle preferenze di chi usava l'app prima di questo giro
// non hanno questo campo nel JSON salvato, kotlinx.serialization lo
// riempie col default senza rompere nulla.
@Serializable
enum class EngineType { LITERT_LM, LLAMA_CPP }

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

    val all = listOf(GEMMA_4_E4B, GEMMA_4_E2B, GEMMA_3N_E4B_GATED, GEMMA_3_12B_HERETIC_GGUF)

    val default = GEMMA_4_E4B

    fun byId(id: String?): DownloadableModel? = all.firstOrNull { it.id == id }
}
