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
// Ripulito il 27/07/2026 (Michele: "fai pulizia dei modelli vecchi e
// lascia solo i nuovi"): tolti i tentativi GGUF non più rilevanti per il
// branch feature/llama-cpp-adreno — restano i due LiteRT-LM di base e i
// due candidati nativi attivamente in prova (12B IQ4_XS, 4B/12B Q4_0).
// Rimossi: Gemma 3n E4B (v1, gated), Gemma 3 12B Heretic su Llamatik
// (motore assente su questo branch), Gemma 3 4B ufficiale Google,
// Gemma 3 4B/12B Abliterated (mai provati, superati dal Q4_0 di Gemma 4).
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

    // Motore GGUF nativo (:llama, backend OpenCL/Adreno vero). Prima
    // GGUF provata su device: carica e gira, ma con n_gpu_layers=999
    // (tutti i 49 livelli) risultava più lenta della CPU (0,7 token/s) —
    // ipotesi principale, IQ4_XS non ha kernel Adreno ottimizzati come
    // Q4_0. Con l'offload parziale (16 livelli, vedi
    // NativeLlamaCppEngine) carica comunque, velocità ancora da
    // confermare in quella configurazione. Dimensione VERIFICATA con
    // richiesta HEAD il 27/07/2026.
    val GEMMA_3_12B_HERETIC_NATIVE = DownloadableModel(
        id = "gemma-3-12b-heretic-native",
        displayName = "Gemma 3 12B Heretic — llama.cpp nativo (GPU Adreno, sperimentale)",
        url = "https://huggingface.co/mradermacher/gemma-3-12b-it-ultra-uncensored-heretic-GGUF/resolve/main/gemma-3-12b-it-ultra-uncensored-heretic.IQ4_XS.gguf",
        fileName = "gemma-3-12b-it-ultra-uncensored-heretic.IQ4_XS.gguf",
        sizeBytes = 6_606_262_336L,
        requiresToken = false,
        note = "Motore GGUF nativo (llama.cpp, GPU Adreno): IQ4_XS, più lento della CPU con offload completo — in prova con offload parziale.",
        engineType = EngineType.LLAMA_CPP_NATIVE,
    )

    // Gemma 4 12B (27/07/2026, cercato dopo il buon risultato dell'offload
    // parziale sul 4B Q4_0): stessa generazione di Gemma 4 E4B/E2B sopra,
    // base "Gemma-4-12B-it-qat-q4_0-unquantized" di Google (QAT —
    // quantizzazione allenata, non naive), abliterato con lo strumento
    // Heretic v1.2.0 (ARA sui layer attn.o_proj 24-35): rifiuti da
    // 99/100 a 7/100, MMLU quasi invariato (74,52% -> 74,18%). Già in
    // Q4_0 — il formato per cui i kernel Adreno sono ottimizzati, stessa
    // taglia del 12B IQ4_XS sopra ma potenzialmente più veloce su questo
    // backend. Dimensione VERIFICATA con richiesta HEAD il 27/07/2026,
    // repo aperto (nessun token).
    val GEMMA_4_12B_HERETIC_Q4_0_GGUF = DownloadableModel(
        id = "gemma-4-12b-heretic-q4_0-gguf",
        displayName = "Gemma 4 12B Heretic Q4_0 (GGUF, nativo)",
        url = "https://huggingface.co/llmfan46/gemma-4-12B-it-qat-q4_0-uncensored-heretic-GGUF/resolve/main/gemma-4-12B-it-qat-q4_0-uncensored-heretic-Q4_0.gguf",
        fileName = "gemma-4-12B-it-qat-q4_0-uncensored-heretic-Q4_0.gguf",
        sizeBytes = 7_596_302_496L,
        requiresToken = false,
        note = "Motore GGUF nativo (llama.cpp, GPU Adreno): Gemma 4 12B, QAT Google + abliterazione Heretic, Q4_0. Non ancora provato su scene vere.",
        engineType = EngineType.LLAMA_CPP_NATIVE,
    )

    val all = listOf(
        GEMMA_4_E4B,
        GEMMA_4_E2B,
        GEMMA_3_12B_HERETIC_NATIVE,
        GEMMA_4_12B_HERETIC_Q4_0_GGUF,
    )

    val default = GEMMA_4_E4B

    fun byId(id: String?): DownloadableModel? = all.firstOrNull { it.id == id }
}
