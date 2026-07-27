pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ImmundaNoctisEx"

include(":core:data")
include(":core:engine")
include(":app")
include(":tool")

// Sperimentale (branch feature/llama-cpp-adreno, 27/07/2026): motore GGUF
// compilato da noi con backend OpenCL/Adreno, alternativa a Llamatik.
// Compilazione nativa spenta di default, vedi llama/build.gradle.kts.
include(":llama")