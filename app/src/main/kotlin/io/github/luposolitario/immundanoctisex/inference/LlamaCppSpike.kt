package io.github.luposolitario.immundanoctisex.inference

import com.llamatik.library.platform.LlamaBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// SPIKE (27/07/2026, Michele: "facciamo una prova ma non spenderci
// troppo codice... una cosa statica di esempio"): non è un secondo
// InferenceEngine, solo il minimo per capire se Llamatik carica un GGUF
// e genera testo sul Razr. Prompt fisso, nessuna integrazione con
// SceneNarrator/PromptBuilder — se lo spike regge, si formalizza in un
// vero LlamaCppEngine.
object LlamaCppSpike {

    private const val STATIC_PROMPT =
        "Scrivi due frasi in italiano, in stile fantasy cupo, che descrivano " +
            "l'ingresso buio di un antico magazzino abbandonato."

    suspend fun runStaticExample(modelPath: String): String = withContext(Dispatchers.IO) {
        val loaded = LlamaBridge.initGenerateModel(modelPath)
        require(loaded) { "Modello non caricato da: $modelPath" }
        LlamaBridge.updateGenerateParams(
            temperature = 0.7f,
            maxTokens = 200,
            topP = 0.9f,
            topK = 40,
            repeatPenalty = 1.1f,
            contextLength = 4096,
            numThreads = 4,
            useMmap = true,
            flashAttention = false,
            batchSize = 512,
            // BUG trovato dal primo test di Michele (27/07, log del
            // device): senza questo parametro il default è 0 = tutto su
            // CPU, mai la GPU Adreno — spiega perché il modello grande
            // sembrava lento/bloccato. 99 (convenzione comune in
            // llama.cpp) = scarica tutti i livelli sulla GPU.
            gpuLayers = 99,
        )
        LlamaBridge.generate(STATIC_PROMPT)
    }
}
