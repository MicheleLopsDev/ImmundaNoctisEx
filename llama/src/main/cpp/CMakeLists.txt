# Porting da ImmundaNoctis v1 (branch develop, commit ce0602f) sul branch
# sperimentale feature/llama-cpp-adreno (27/07/2026).
#
# CORREZIONE rispetto a v1: mancava il flag master che accende davvero il
# backend OpenCL — GGML_OPENCL è OFF di default in llama.cpp (verificato
# nel fork di Michele, github.com/MicheleLopsDev/llama.cpp,
# ggml/CMakeLists.txt riga 212). v1 impostava solo i due sotto-flag
# (EMBED_KERNELS, USE_ADRENO_KERNELS) senza il master switch: senza
# GGML_OPENCL=ON restano senza effetto, il binario compila comunque ma
# resta CPU-only. Rimosso anche -DLLAMA_CLBLAST=ON: non esiste più come
# opzione nel fork attuale (CLBlast era un'integrazione BLAS separata e
# più vecchia, non il backend OpenCL nativo di ggml) — CMake lo
# accetterebbe silenziosamente come variabile inutilizzata, meglio non
# portare avanti un flag morto.
cmake_minimum_required(VERSION 3.22.1)

project("llama-android")

# LLAMA_CPP_DIR: percorso del checkout locale del fork llama.cpp
# (github.com/MicheleLopsDev/llama.cpp), passato da llama/build.gradle.kts
# leggendo la proprietà Gradle "llamaCppDir" da local.properties — a
# differenza di v1 (percorso relativo fisso "../../../../../../dev/
# llama.cpp-master"), così il checkout non deve stare in una posizione
# fissa sul disco di chi compila.
if(NOT DEFINED LLAMA_CPP_DIR)
    message(FATAL_ERROR "LLAMA_CPP_DIR non impostato: aggiungi llamaCppDir=<percorso al checkout locale del fork> in local.properties.")
endif()

add_subdirectory(${LLAMA_CPP_DIR} build-llama)

add_library(${CMAKE_PROJECT_NAME} SHARED
        llama-android.cpp
)

target_include_directories(${CMAKE_PROJECT_NAME} PUBLIC
        # Per gli header pubblici
        "${LLAMA_CPP_DIR}"
        # Per llama-sampling.h e altri header interni
        "${LLAMA_CPP_DIR}/src"
        # common.h è stato spostato qui in una riorganizzazione di
        # llama.cpp successiva al fork originale (27/07/2026, checkout
        # aggiornato per il supporto all'architettura gemma4)
        "${LLAMA_CPP_DIR}/common"
)

target_link_libraries(${CMAKE_PROJECT_NAME}
        llama
        # Rinominata da "common" a "llama-common" nella stessa
        # riorganizzazione (27/07/2026, checkout aggiornato per gemma4).
        llama-common
        android
        log
)
