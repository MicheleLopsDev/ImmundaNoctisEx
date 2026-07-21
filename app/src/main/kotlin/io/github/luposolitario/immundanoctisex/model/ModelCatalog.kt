package io.github.luposolitario.immundanoctisex.model

import kotlinx.serialization.Serializable

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

    val all = listOf(GEMMA_4_E4B, GEMMA_4_E2B, GEMMA_3N_E4B_GATED)

    val default = GEMMA_4_E4B

    fun byId(id: String?): DownloadableModel? = all.firstOrNull { it.id == id }
}
