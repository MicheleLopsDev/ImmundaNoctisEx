# Setup locale per :llama (llama.cpp con backend OpenCL/Adreno)

> Branch sperimentale `feature/llama-cpp-adreno` (27/07/2026). Compilazione
> nativa **spenta di default** — questo file serve solo a chi la accende per
> provare il motore vero su device. Non riguarda `:app`/`LlamaCppEngine`
> (quello usa Llamatik, CPU-only su Android, nessun setup nativo richiesto).

## Perché questo file esiste

La build con `-DGGML_OPENCL=ON` compila da zero llama.cpp con lo stesso
CMake usato per il resto del progetto Android, ma tocca pezzi che l'AGP di
solito non richiede: un SDK OpenCL per la cross-compilazione e un
interprete Python per incorporare i kernel `.cl` nel binario. Questa è la
procedura verificata il 27/07/2026 su Windows 10, NDK 27.0.12077973, senza
WSL.

## 1. `local.properties` (non versionato, per-macchina)

```properties
buildLlama=true
llamaCppDir=C:/DEV/llama.cpp
pythonExecutable=C:/Users/<utente>/AppData/Local/Microsoft/WindowsApps/python3.exe
```

- **`llamaCppDir`**: checkout locale del fork
  `https://github.com/MicheleLopsDev/llama.cpp` (shallow clone va bene,
  `git clone --depth 1 ...`). Va FUORI dal repo di ImmundaNoctisEx (nessun
  submodule): evita di portarsi dietro tutta la storia di llama.cpp nel
  nostro git.
- **`pythonExecutable`**: solo se `find_package(Python3)` di CMake non
  trova un python di sistema. Su Windows con Python installato dal
  Microsoft Store, l'eseguibile reale sta in
  `...\AppData\Local\Microsoft\WindowsApps\python3.exe` ma CMake non lo
  cerca lì di default — va indicato esplicitamente. Se hai un Python
  "vero" (python.org, non Store) già sul PATH, puoi ometterlo.

## 2. Header e libreria OpenCL nel sysroot dell'NDK (una tantum)

CMake cerca un SDK OpenCL per compilare (`CL/cl.h` + una libreria da
linkare) — su un PC Windows normale non c'è, e non serve installarne uno
completo: bastano gli header Khronos e un ICD loader compilato con lo
stesso NDK, piazzati dentro il sysroot dell'NDK stesso (stessa procedura
documentata in `llama.cpp/docs/backend/OPENCL.md` per Android).

```bash
NDK="C:/android/SDK/ndk/27.0.12077973"   # adatta alla tua versione NDK
SYSROOT="$NDK/toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr"

# Header
git clone --depth 1 https://github.com/KhronosGroup/OpenCL-Headers
cp -r OpenCL-Headers/CL "$SYSROOT/include/"

# ICD loader (libOpenCL.so), compilato con lo stesso NDK/toolchain
git clone --depth 1 https://github.com/KhronosGroup/OpenCL-ICD-Loader
cd OpenCL-ICD-Loader
cmake -B build -GNinja \
  -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-34 \
  -DOPENCL_ICD_LOADER_HEADERS_DIR="$(pwd)/../OpenCL-Headers" \
  -DCMAKE_BUILD_TYPE=Release
cmake --build build
cp build/libOpenCL.so "$SYSROOT/lib/aarch64-linux-android/"
```

Questi due passi modificano solo l'installazione NDK sul disco locale
(fuori da qualunque repo git): niente da committare, ma vanno rifatti se
l'NDK viene reinstallato o aggiornato a una versione diversa.

## 3. Verifica

```bash
./gradlew :llama:assembleDebug
```

Se va a buon fine, `llama/build/intermediates/cmake/debug/obj/arm64-v8a/`
contiene `libggml-opencl.so` oltre a `libllama-android.so` — controllabile
anche nella CMakeCache generata (`llama/.cxx/**/CMakeCache.txt`):
`GGML_AVAILABLE_BACKENDS` deve elencare `ggml-opencl`, non solo
`ggml-cpu`.

## Stato: non ancora provato su device

La compilazione qui sopra è verificata (27/07/2026). **Non ancora
verificato**: caricare un modello vero tramite `LLamaAndroid.kt`, misurare
token/s reali sul Razr, e confermare che `n_gpu_layers = 999` (impostato in
`llama-android.cpp`, correzione rispetto a v1) faccia davvero scaricare i
livelli sulla GPU Adreno a runtime — la compilazione riuscita prova solo
che il backend OpenCL è STATO INCLUSO nel binario, non che venga usato
efficacemente all'inferenza.
