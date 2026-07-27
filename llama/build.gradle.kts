// :llama — motore GGUF via llama.cpp compilato da noi (branch sperimentale
// feature/llama-cpp-adreno, 27/07/2026), con backend OpenCL/Adreno vero.
// Porting da ImmundaNoctis v1 (branch develop). Alternativa a Llamatik
// (usato da LlamaCppEngine in :app), che su Android gira sempre su CPU:
// qui compiliamo noi il fork github.com/MicheleLopsDev/llama.cpp con
// GGML_OPENCL=ON per uno scarico GPU reale sull'Adreno del Razr.
//
// Compilazione nativa SPENTA di default (buildLlama=false in
// local.properties): un modulo con CMake da compilare non deve rallentare
// né rompere la build di chi non sta lavorando su questo esperimento.
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val buildLlama = localProperties.getProperty("buildLlama")?.toBoolean() ?: false
val llamaCppDir = localProperties.getProperty("llamaCppDir")
// Necessario perché GGML_OPENCL_EMBED_KERNELS incorpora i kernel .cl via
// script Python. Opzionale: se il python di sistema è su PATH, CMake lo
// trova da solo. Su Windows con Python installato dal Microsoft Store,
// l'alias in WindowsApps non viene rilevato da find_package(Python3) —
// va indicato esplicitamente qui.
val pythonExecutable = localProperties.getProperty("pythonExecutable")

android {
    namespace = "android.llama.cpp"
    compileSdk = 36

    defaultConfig {
        minSdk = 34

        ndk {
            abiFilters.add("arm64-v8a")
        }

        if (buildLlama) {
            check(!llamaCppDir.isNullOrBlank()) {
                "buildLlama=true ma llamaCppDir non è impostato in local.properties " +
                    "(percorso del checkout locale di github.com/MicheleLopsDev/llama.cpp)."
            }
            externalNativeBuild {
                cmake {
                    arguments += "-DLLAMA_CPP_DIR=$llamaCppDir"
                    arguments += "-DLLAMA_CURL=OFF"
                    arguments += "-DLLAMA_BUILD_COMMON=ON"
                    arguments += "-DGGML_LLAMAFILE=OFF"
                    arguments += "-DCMAKE_BUILD_TYPE=Release"

                    // Backend OpenCL per Adreno (v1 dimenticava il flag
                    // master GGML_OPENCL: senza, i due sotto-flag restano
                    // senza effetto e il binario resta CPU-only).
                    arguments += "-DGGML_OPENCL=ON"
                    arguments += "-DGGML_OPENCL_F16=1"
                    arguments += "-DGGML_OPENCL_EMBED_KERNELS=ON"
                    arguments += "-DGGML_OPENCL_USE_ADRENO_KERNELS=ON"

                    if (!pythonExecutable.isNullOrBlank()) {
                        arguments += "-DPython3_EXECUTABLE=$pythonExecutable"
                    }
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    if (buildLlama) {
        externalNativeBuild {
            cmake {
                path("src/main/cpp/CMakeLists.txt")
                version = "3.22.1"
            }
        }
    } else {
        logger.lifecycle(":llama — compilazione nativa disattivata (buildLlama=false in local.properties)")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}

// Kotlin 2.3: kotlinOptions è un errore, si usa il DSL compilerOptions
// (stessa convenzione di app/build.gradle.kts).
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
