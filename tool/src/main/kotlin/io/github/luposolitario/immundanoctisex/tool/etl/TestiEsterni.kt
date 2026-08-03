package io.github.luposolitario.immundanoctisex.tool.etl

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.Scene

// Separa le MECCANICHE dai TESTI di un libro (03/08/2026).
//
// Il motivo è la licenza Project Aon: consente di scaricare i libri per
// uso personale, vieta di ridistribuirli. Le meccaniche però non sono
// prosa — sono identificativi, salti, intervalli di tiro, valori di
// combattimento, modificatori: numeri e struttura, per giunta corretti a
// mano in mesi di lavoro (la scena 343 sistemata a mano, i 48
// `rollModifiers`, le forme di ENDURANCE recuperate a fatica). Quelle
// restano in repository; la prosa no.
//
// Stessa strada di kaichronicles, che versiona `mechanics-N.xml` e fa
// scaricare i testi da Project Aon a chi vuole giocarli.
//
// Un file SOLO con le etichette vuote, invece di due file separati
// (idea di Michele, migliore di quella proposta da Claude): lo schema
// `Scene` non cambia, `PackageSource` nemmeno, l'editor apre lo
// scheletro come qualunque altro libro e ci si lavora sopra. Il
// riempimento è uno strumento a parte, non un pezzo del motore.
object TestiEsterni {

    // Quello che appartiene a Project Aon e va tolto. Tutto il resto —
    // id, nextSceneId, minRoll/maxRoll, combat (numeri), gameMechanics,
    // rollModifiers, immagini, suoni — resta.
    private fun svuota(scene: Scene, idProjectAon: String): Scene = scene.copy(
        narrativeText = "",
        // Il nome del luogo è invenzione loro quanto la prosa.
        locationName = null,
        // Il link al paragrafo originale prende il posto del testo: chi
        // apre il file vede da dove viene ogni scena, e dall'editor la
        // si può confrontare con l'originale.
        source = linkAlParagrafo(idProjectAon, scene.id),
        choices = scene.choices.map { it.copy(choiceText = "") },
        disciplineChoices = scene.disciplineChoices.map { it.copy(choiceText = "") },
        // Del combattimento restano tutti i numeri: se ne va solo il
        // nome del nemico.
        combat = scene.combat?.copy(enemyName = ""),
    )

    // Le Internet Edition pubblicano ogni sezione anche come pagina a sé:
    // .../lw/01fftd/sect12.htm per la scena "12". È solo un riferimento
    // per chi legge il file o vuole confrontare con l'originale: il
    // download vero prende il libro intero in un colpo (xhtml-simple).
    // Le scene con id non numerico (finali fabbricati, rami aggiunti a
    // mano) non hanno un paragrafo originale.
    internal fun linkAlParagrafo(idProjectAon: String, idScena: String): String? =
        if (idScena.toIntOrNull() != null) {
            ProjectAonDownloader.urlPerSezione(idProjectAon, idScena)
        } else {
            null
        }

    // Prepara il libro per il repository: via i testi, dentro il
    // marcatore che dice da dove si recuperano.
    fun scheletro(manifest: Manifest, idProjectAon: String): Manifest = manifest.copy(
        scenes = manifest.scenes.map { svuota(it, idProjectAon) },
        textsFrom = "$SORGENTE_PROJECT_AON:$idProjectAon",
    )

    data class Riempimento(
        val manifest: Manifest,
        // Quello che non ha combaciato: si stampa a fine comando invece
        // di essere indovinato in silenzio, stesso principio del report
        // di `runConvert`.
        val note: List<String>,
    )

    // Innesta i testi presi da una conversione fresca dentro lo
    // scheletro versionato.
    //
    // La direzione conta: le MECCANICHE sono quelle dello scheletro
    // (corrette a mano nel tempo), i TESTI vengono dal parse. Fare il
    // contrario butterebbe via le correzioni a ogni riempimento.
    fun riempi(scheletro: Manifest, conTesti: Manifest): Riempimento {
        val note = mutableListOf<String>()
        val perId = conTesti.scenes.associateBy { it.id }

        val scenePiene = scheletro.scenes.map { scena ->
            val sorgente = perId[scena.id]
            if (sorgente == null) {
                note += "scena ${scena.id}: nessun testo trovato nel libro scaricato, resta vuota"
                return@map scena
            }
            val testiScelte = sorgente.choices.associate { it.id to it.choiceText }
            val testiDiscipline = sorgente.disciplineChoices.associate { it.id to it.choiceText }
            scena.copy(
                narrativeText = sorgente.narrativeText,
                locationName = sorgente.locationName,
                choices = scena.choices.map { scelta ->
                    val testo = testiScelte[scelta.id]
                    if (testo == null) {
                        note += "scena ${scena.id}, scelta ${scelta.id}: testo non trovato"
                        scelta
                    } else {
                        scelta.copy(choiceText = testo)
                    }
                },
                disciplineChoices = scena.disciplineChoices.map { scelta ->
                    val testo = testiDiscipline[scelta.id]
                    if (testo == null) {
                        note += "scena ${scena.id}, scelta per disciplina ${scelta.id}: testo non trovato"
                        scelta
                    } else {
                        scelta.copy(choiceText = testo)
                    }
                },
                combat = scena.combat?.let { combattimento ->
                    val nome = sorgente.combat?.enemyName
                    if (nome.isNullOrBlank()) {
                        note += "scena ${scena.id}: nome del nemico non trovato"
                        combattimento
                    } else {
                        combattimento.copy(enemyName = nome)
                    }
                },
            )
        }

        val inPiu = perId.keys - scheletro.scenes.map { it.id }.toSet()
        if (inPiu.isNotEmpty()) {
            note += "il libro scaricato ha ${inPiu.size} scene che lo scheletro non prevede " +
                "(${inPiu.sorted().take(5).joinToString()}${if (inPiu.size > 5) ", ..." else ""}): ignorate"
        }

        // Riempito non è più uno scheletro: il marcatore se ne va, così
        // il gioco non lo scambia per un libro muto.
        return Riempimento(
            manifest = scheletro.copy(scenes = scenePiene, textsFrom = null),
            note = note,
        )
    }

    // Vero se il libro è uno scheletro da riempire prima di giocarci.
    fun haTestiEsterni(manifest: Manifest): Boolean =
        manifest.textsFrom?.startsWith("$SORGENTE_PROJECT_AON:") == true

    // "projectaon:01fftd" -> "01fftd"
    fun idProjectAonDi(manifest: Manifest): String? =
        manifest.textsFrom?.substringAfter("$SORGENTE_PROJECT_AON:", "")?.takeIf { it.isNotBlank() }

    const val SORGENTE_PROJECT_AON = "projectaon"
}
