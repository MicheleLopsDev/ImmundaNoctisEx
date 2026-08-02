package io.github.luposolitario.immundanoctisex.tool.inference

import io.github.luposolitario.immundanoctisex.tool.editor.EditorLog
import java.io.File

// Il backend GPU di LiteRT-LM su Windows non è OpenCL come sul telefono:
// è WebGPU (Dawn) su Direct3D 12, e per compilare e firmare gli shader
// gli servono `dxil.dll` e `dxcompiler.dll` del DirectX Shader Compiler.
// Non fanno parte di Windows: senza, il caricamento ripiega su CPU con
// un errore che si legge solo nell'output nativo (02/08/2026, log di
// Michele: "DynamicLib.Open: dxil.dll Windows Error: 87").
//
// Il trucco per farle trovare: caricarle NOI per percorso assoluto prima
// di inizializzare il motore. Quando poi Dawn chiama `LoadLibrary
// ("dxil.dll")` senza percorso, Windows gli restituisce il modulo già
// caricato con quel nome invece di cercarlo per il sistema — nessun
// PATH da modificare, nessuna copia accanto a java.exe. (Provata anche
// la strada ovvia, copiare le DLL nella cartella di lavoro: non basta,
// Dawn continua a non caricarle.)
//
// LA VERSIONE CONTA, ed è costata due giri (02/08/2026). Con la release
// più recente del DXC (`v1.9.2607`, luglio 2026) la `dxcompiler.dll`
// **non si carica**: `System.load` fallisce con "routine di
// inizializzazione della DLL non riuscita" e Dawn dà `Windows Error 87`.
// Con `v1.9.2602.24` (asset `dxc_2026_05_27.zip`) funziona tutto.
// Misure sullo stesso PC (Ryzen 7 7730U + Radeon integrata, Gemma 4 E4B):
// primo token 3,7 s su GPU contro 14,4 s su CPU, generazione tre volte
// più veloce. Se un giorno la GPU smettesse di partire dopo un
// aggiornamento delle DLL, è il primo posto da guardare.
object SupportoGpuWindows {

    // Nell'ordine giusto: dxcompiler carica dxil per firmare gli shader,
    // quindi dxil deve già essere in memoria.
    private val LIBRERIE = listOf("dxil.dll", "dxcompiler.dll")

    // Cartella predefinita, la stessa dove l'editor tiene i modelli:
    // roba scaricata a parte che non appartiene né al programma né ai libri.
    val cartellaPredefinita: File
        get() = File(System.getProperty("user.home"), "ImmundaNoctisEx/dxc")

    private var esitoRegistrato: String? = null

    // Esito leggibile, buono sia per il log sia per la UI. Idempotente:
    // caricare due volte la stessa DLL non fa danni, ma non serve.
    fun preparaShaderCompiler(cartella: File = cartellaPredefinita): String {
        esitoRegistrato?.let { return it }

        val esito = when {
            !System.getProperty("os.name").orEmpty().startsWith("Windows") ->
                "Non su Windows: nessuna libreria da precaricare."
            !cartella.isDirectory ->
                "GPU non disponibile: manca ${cartella.absolutePath} con dxil.dll e dxcompiler.dll. " +
                    "Prendile da $RELEASE_CONSIGLIATA — non dall'ultima release, la sua " +
                    "dxcompiler.dll non si carica. Senza, il motore gira su CPU (tre volte più lento)."
            else -> caricaDa(cartella)
        }
        EditorLog.i(TAG, esito)
        esitoRegistrato = esito
        return esito
    }

    private fun caricaDa(cartella: File): String {
        val mancanti = LIBRERIE.filterNot { File(cartella, it).isFile }
        if (mancanti.isNotEmpty()) {
            return "GPU non disponibile: in ${cartella.absolutePath} mancano ${mancanti.joinToString()}."
        }
        LIBRERIE.forEach { nome ->
            val libreria = File(cartella, nome)
            runCatching { System.load(libreria.absolutePath) }
                .onFailure { errore ->
                    // Un fallimento qui non è fatale: si prosegue e il
                    // motore ripiegherà su CPU come faceva prima.
                    return "GPU non disponibile: $nome non caricabile (${errore.message}). Si userà la CPU."
                }
        }
        return "DirectX Shader Compiler caricato da ${cartella.absolutePath}: la GPU può essere tentata."
    }

    private const val TAG = "SupportoGpuWindows"

    // Non "l'ultima": QUESTA. Vedi il commento in testa al file.
    private const val RELEASE_CONSIGLIATA =
        "github.com/microsoft/DirectXShaderCompiler/releases/tag/v1.9.2602.24 (bin/x64)"
}
