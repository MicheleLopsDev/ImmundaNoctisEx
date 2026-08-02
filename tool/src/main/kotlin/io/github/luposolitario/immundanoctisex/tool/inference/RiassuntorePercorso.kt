package io.github.luposolitario.immundanoctisex.tool.inference

import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import io.github.luposolitario.immundanoctisex.core.engine.inference.LinguaOutput
import io.github.luposolitario.immundanoctisex.core.engine.inference.ripulisciTokenDiServizio
import io.github.luposolitario.immundanoctisex.tool.editor.EditorLog
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Il riassunto di un cammino: dalla scena START fino a quella scelta
// (03/08/2026, Michele: "selezionando una scena lui fa il riassunto di
// tutte le scene partendo da start a quella selezionata").
//
// Perché a blocchi. Misurati i cinque libri Project Aon: un percorso
// tipico è di 15-32 scene per 5.000-20.000 caratteri, ma il peggiore
// arriva a 73 scene e 53.000 caratteri — oltre il contesto del modello
// (10240 token, all'incirca 40.000 caratteri, prompt e risposta
// compresi). Un colpo solo funzionerebbe quasi sempre e fallirebbe
// proprio sui casi interessanti, quelli lunghi.
//
// Quindi: le scene si raggruppano finché stanno in un blocco, ogni
// blocco diventa un riassunto parziale, e se i blocchi sono più d'uno i
// parziali vengono ricuciti in un riassunto solo. È l'idea che Michele
// aveva già anticipato — *"come se fossero dei piccoli riassunti"*.
class RiassuntorePercorso(private val motore: EditorInferenceEngine) {

    private val turno = Mutex()

    // Avanzamento leggibile per la finestra: quale passo, su quanti.
    data class Avanzamento(val passo: Int, val totale: Int, val descrizione: String)

    suspend fun riassumi(
        scene: List<Scene>,
        titoloLibro: String,
        lingua: LinguaOutput,
        onAvanzamento: (Avanzamento) -> Unit = {},
    ): Result<String> {
        if (!motore.isLoaded) {
            return Result.failure(IllegalStateException("Modello non caricato: aprilo dalla schermata Modello."))
        }
        if (scene.isEmpty()) {
            return Result.failure(IllegalStateException("Nessuna scena da riassumere."))
        }

        return turno.withLock {
            runCatching {
                val blocchi = raggruppa(scene)
                // Un passo per blocco, più la ricucitura finale quando i
                // blocchi sono più d'uno.
                val passi = if (blocchi.size > 1) blocchi.size + 1 else 1
                EditorLog.i(TAG, "Riassunto di ${scene.size} scene in ${blocchi.size} blocchi")

                val parziali = blocchi.mapIndexed { indice, blocco ->
                    onAvanzamento(
                        Avanzamento(
                            indice + 1,
                            passi,
                            "Scene ${blocco.first().id}-${blocco.last().id} (${blocco.size})",
                        ),
                    )
                    genera(promptDiBlocco(blocco, titoloLibro, lingua, blocchi.size > 1))
                }

                if (parziali.size == 1) {
                    parziali.first()
                } else {
                    onAvanzamento(Avanzamento(passi, passi, "Unisco i ${parziali.size} riassunti parziali"))
                    genera(promptDiUnione(parziali, titoloLibro, lingua))
                }
            }.onFailure { EditorLog.e(TAG, "Riassunto non riuscito: ${it.message}", it) }
        }
    }

    // Blocchi il più grandi possibile: meno passi, meno attese. Una
    // scena da sola più lunga del tetto finisce comunque nel suo blocco —
    // meglio un prompt sopra misura che perdere del testo.
    private fun raggruppa(scene: List<Scene>): List<List<Scene>> {
        val blocchi = mutableListOf<MutableList<Scene>>()
        var corrente = mutableListOf<Scene>()
        var pesoCorrente = 0

        scene.forEach { scena ->
            val peso = scena.narrativeText.length
            if (corrente.isNotEmpty() && pesoCorrente + peso > TETTO_BLOCCO) {
                blocchi.add(corrente)
                corrente = mutableListOf()
                pesoCorrente = 0
            }
            corrente.add(scena)
            pesoCorrente += peso
        }
        if (corrente.isNotEmpty()) blocchi.add(corrente)
        return blocchi
    }

    private suspend fun genera(prompt: String): String {
        motore.newSession()
        val grezzo = StringBuilder()
        motore.generate(prompt).collect { grezzo.append(it) }
        val testo = ripulisciTokenDiServizio(grezzo.toString()).trim()
        if (testo.isBlank()) error("Il modello non ha prodotto testo.")
        return testo
    }

    // Il prompt è in inglese come tutti gli altri del progetto (i libri
    // sorgente lo sono, e il modello segue meglio istruzioni in inglese
    // anche quando scrive in italiano).
    private fun promptDiBlocco(
        blocco: List<Scene>,
        titoloLibro: String,
        lingua: LinguaOutput,
        eParziale: Boolean,
    ): String = buildString {
        append("You are summarising a gamebook playthrough of '$titoloLibro'.\n\n")
        append(
            if (eParziale) {
                "Below is PART of a path through the book, scene by scene, in the order the " +
                    "player walked it. Write a compact summary of THIS part only, in the order " +
                    "the events happen.\n\n"
            } else {
                "Below is a complete path through the book, scene by scene, in the order the " +
                    "player walked it. Write a compact summary of the whole journey.\n\n"
            },
        )
        append("[SCENES]\n")
        blocco.forEach { scena ->
            append("--- scene ${scena.id} ---\n")
            append(scena.narrativeText.trim())
            scena.combat?.let { append("\n(combat against ${it.enemyName})") }
            append("\n\n")
        }
        append(
            "Rules:\n" +
                "1. Write in ${lingua.promptValue}.\n" +
                "2. Keep every fact, place, character and item that appears: this is a summary, " +
                "not a retelling — do NOT invent anything that is not written above.\n" +
                "3. Follow the order of the scenes: it is the order in which things happened.\n" +
                "4. Prose, not a list. No scene numbers in the text.\n" +
                "5. Answer with the summary only, nothing before and nothing after.\n\n",
        )
        append("SUMMARY (in ${lingua.promptValue}):")
    }

    private fun promptDiUnione(
        parziali: List<String>,
        titoloLibro: String,
        lingua: LinguaOutput,
    ): String = buildString {
        append("You are assembling the summary of a gamebook playthrough of '$titoloLibro'.\n\n")
        append(
            "Below are ${parziali.size} partial summaries, in order: each covers a stretch of " +
                "the same journey. Join them into ONE continuous summary.\n\n",
        )
        parziali.forEachIndexed { indice, parziale ->
            append("--- part ${indice + 1} ---\n$parziale\n\n")
        }
        append(
            "Rules:\n" +
                "1. Write in ${lingua.promptValue}.\n" +
                "2. Keep the order and every fact: do NOT add anything that is not in the parts above.\n" +
                "3. Remove repetitions where two parts say the same thing.\n" +
                "4. Prose, one continuous text.\n" +
                "5. Answer with the summary only.\n\n",
        )
        append("SUMMARY (in ${lingua.promptValue}):")
    }

    private companion object {
        const val TAG = "RiassuntorePercorso"

        // Caratteri di testo di scena per blocco. Tenuto sotto un quarto
        // del contesto: al prompt si aggiungono istruzioni e risposta, e
        // un blocco troppo grosso è anche un prefill più lungo.
        const val TETTO_BLOCCO = 10_000
    }
}
