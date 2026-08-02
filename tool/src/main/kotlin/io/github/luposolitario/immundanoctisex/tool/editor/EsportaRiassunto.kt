package io.github.luposolitario.immundanoctisex.tool.editor

import io.github.luposolitario.immundanoctisex.core.data.model.ImageReference
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import java.io.File

// Esportazione del riassunto in Markdown (02-03/08/2026, Michele:
// "esportarlo in formato MD in un file con il nome che contiene una cosa
// come riassunto_scena_1_x, dentro c'è un titolo come Riassunto di
// NomeLibro - Da scena 1..X - sotto il testo e link alle immagini, se
// sono locali i link disco altrimenti quelli url").
//
// Sulle immagini "locali" serve una precisazione: gli `static:` NON
// hanno un percorso su disco da linkare — vivono dentro l'`.exe`
// dell'editor (`resources/images/`), perché l'editor funzioni anche su
// una macchina senza il repository. Un link a `loc_tavern.jpg` non
// aprirebbe niente. Si copiano quindi accanto al `.md`, in una cartella
// dedicata, e si linkano in relativo: il documento risulta apribile
// ovunque, anche mandato a qualcun altro. Gli `url:` restano URL.
object EsportaRiassunto {

    data class Esito(val fileMd: File, val immaginiCopiate: Int, val immaginiNonTrovate: List<String>)

    fun scrivi(
        destinazione: File,
        manifest: Manifest,
        percorso: List<Scene>,
        riassunto: String,
    ): Result<Esito> = runCatching {
        val md = if (destinazione.extension.equals("md", ignoreCase = true)) {
            destinazione
        } else {
            File(destinazione.parentFile, destinazione.name + ".md")
        }
        // La cartella di destinazione può non esistere ancora (un nome
        // digitato a mano nel dialogo di salvataggio): crearla è meglio
        // che fallire dopo aver fatto lavorare il modello per minuti.
        md.parentFile?.mkdirs()
        val cartellaImmagini = File(md.parentFile, md.nameWithoutExtension + "_immagini")

        val riferimenti = riferimentiImmagine(percorso)
        val copiate = mutableListOf<Pair<String, String>>() // etichetta -> percorso relativo
        val remote = mutableListOf<Pair<String, String>>() // etichetta -> url
        val mancanti = mutableListOf<String>()

        riferimenti.forEach { (etichetta, riferimento) ->
            when (val parsato = ImageReference.parse(riferimento)) {
                is ImageReference.Url -> remote += etichetta to parsato.url
                is ImageReference.Static -> {
                    val copiato = copiaDaRisorse(parsato.catalogId, cartellaImmagini)
                    if (copiato != null) {
                        copiate += etichetta to "${cartellaImmagini.name}/${copiato.name}"
                    } else {
                        mancanti += "$etichetta (${parsato.catalogId})"
                    }
                }
                null -> mancanti += etichetta
            }
        }

        md.writeText(
            componiMarkdown(manifest, percorso, riassunto, copiate, remote, mancanti),
        )
        EditorLog.i(
            TAG,
            "Riassunto esportato in ${md.absolutePath} (${copiate.size} immagini copiate, ${remote.size} link)",
        )
        Esito(md, copiate.size, mancanti)
    }.onFailure { EditorLog.e(TAG, "Esportazione del riassunto non riuscita: ${it.message}", it) }

    // Tutte le immagini citate lungo il percorso, con l'indicazione della
    // scena: nel documento servono a ritrovare il punto, non solo a
    // vedere la figura.
    private fun riferimentiImmagine(percorso: List<Scene>): List<Pair<String, String>> = buildList {
        percorso.forEach { scena ->
            scena.backgroundImage?.takeIf { it.isNotBlank() }?.let { add("Scena ${scena.id} — sfondo" to it) }
            scena.npcImage?.takeIf { it.isNotBlank() }?.let { add("Scena ${scena.id} — personaggio" to it) }
            scena.combat?.enemyImage?.takeIf { it.isNotBlank() }?.let { add("Scena ${scena.id} — nemico" to it) }
        }
    }

    // Le immagini statiche stanno fra le risorse impacchettate (le stesse
    // che l'editor mostra in anteprima): si estraggono su disco perché il
    // .md sia autonomo.
    private fun copiaDaRisorse(catalogId: String, cartella: File): File? {
        val estensioni = listOf("jpg", "png", "webp", "jpeg")
        estensioni.forEach { estensione ->
            val risorsa = "images/$catalogId.$estensione"
            val flusso = EsportaRiassunto::class.java.classLoader.getResourceAsStream(risorsa)
            if (flusso != null) {
                cartella.mkdirs()
                val destinazione = File(cartella, "$catalogId.$estensione")
                flusso.use { ingresso -> destinazione.outputStream().use { ingresso.copyTo(it) } }
                return destinazione
            }
        }
        return null
    }

    private fun componiMarkdown(
        manifest: Manifest,
        percorso: List<Scene>,
        riassunto: String,
        copiate: List<Pair<String, String>>,
        remote: List<Pair<String, String>>,
        mancanti: List<String>,
    ): String = buildString {
        val primo = percorso.first().id
        val ultimo = percorso.last().id
        appendLine("# Riassunto di ${manifest.title} — Da scena $primo a $ultimo")
        appendLine()
        appendLine("*${percorso.size} scene, nell'ordine in cui il giocatore le attraversa.*")
        appendLine()
        appendLine(riassunto.trim())
        appendLine()

        if (copiate.isNotEmpty() || remote.isNotEmpty()) {
            appendLine("## Immagini")
            appendLine()
            copiate.forEach { (etichetta, relativo) ->
                appendLine("- **$etichetta**  ")
                appendLine("  ![$etichetta]($relativo)")
            }
            remote.forEach { (etichetta, url) ->
                appendLine("- **$etichetta**: <$url>")
            }
            appendLine()
        }
        if (mancanti.isNotEmpty()) {
            appendLine("> Immagini dichiarate ma non trovate fra le risorse: ${mancanti.joinToString(", ")}.")
            appendLine()
        }

        // Il percorso in chiaro: serve a rifare la stessa strada nel
        // libro, e a capire da dove viene ogni pezzo del riassunto.
        appendLine("## Percorso")
        appendLine()
        appendLine(percorso.joinToString(" → ") { it.id })
    }

    private const val TAG = "EsportaRiassunto"
}
