package io.github.luposolitario.immundanoctisex.tool.etl

import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

// Confronta il grafo dei percorsi che abbiamo estratto da un libro con
// quello UFFICIALE che Project Aon pubblica per ogni volume
// (`/en/svg/lw/01fftd.svgz`, generato con Graphviz).
//
// Idea di Michele (03/08/2026). È una **fonte indipendente**: fino ad
// allora la conversione si poteva verificare solo con sé stessa —
// contare quante scelte erano state estratte non dice se sono quelle
// giuste. Il grafo dice quali collegamenti il libro ha davvero, e il
// confronto ha trovato in due giorni 28 archi persi che nessun altro
// controllo aveva visto.
object ConfrontoGrafo {

    data class Esito(
        val archiUfficiali: Int,
        // Archi che il libro ha e noi no: ogni voce è un pezzo di
        // avventura irraggiungibile.
        val mancanti: List<Pair<String, String>>,
        // Archi che abbiamo noi e il libro no. Dovrebbero essere zero:
        // se ce ne sono, la conversione si è inventata un collegamento.
        val inPiu: List<Pair<String, String>>,
    ) {
        val trovati: Int get() = archiUfficiali - mancanti.size
        val copertura: Double
            get() = if (archiUfficiali == 0) 100.0 else 100.0 * trovati / archiUfficiali
    }

    // Gli archi come li vede il nostro JSON: ogni uscita di ogni scena.
    internal fun archiDi(manifest: Manifest): Map<String, Set<String>> {
        val archi = mutableMapOf<String, MutableSet<String>>()
        fun collega(da: String, a: String?) {
            if (!a.isNullOrBlank()) archi.getOrPut(da) { mutableSetOf() }.add(a)
        }
        manifest.scenes.forEach { scena ->
            scena.choices.forEach { collega(scena.id, it.nextSceneId) }
            scena.disciplineChoices.forEach { collega(scena.id, it.nextSceneId) }
            scena.combat?.let { combat ->
                collega(scena.id, combat.winSceneId)
                collega(scena.id, combat.winSceneIdRapido)
                collega(scena.id, combat.loseSceneId)
                collega(scena.id, combat.evadeSceneId)
                collega(scena.id, combat.seColpitoSceneId)
            }
            // I salti dichiarati dalle meccaniche (checkItemAndJump,
            // ifStat, skillCheck...) vivono nei `params`, che sono un
            // JsonObject libero: si guardano le chiavi che nominano una
            // scena, non tutte.
            scena.gameMechanics.forEach { meccanica ->
                destinazioniDi(meccanica.params).forEach { collega(scena.id, it) }
            }
        }
        return archi
    }

    // Le scene RAGGIUNGIBILI da `da` attraversando i nodi che non
    // esistono nel libro originale.
    //
    // Serve perché la nostra conversione a volte spezza in due quello
    // che il libro fa in un passo: un combattimento con più nemici
    // diventa una catena ("112" -> "112-nemico2" -> "33"), un finale
    // fabbricato prende un id come "17-vittoria". Il grafo ufficiale
    // conosce solo i paragrafi numerati, quindi 112 -> 33 va cercato
    // saltando i nodi intermedi — altrimenti si segnalerebbero come
    // "mancanti" archi che ci sono, solo scritti in due tempi.
    internal fun raggiungibiliNumeriche(archi: Map<String, Set<String>>, da: String): Set<String> {
        val visti = mutableSetOf<String>()
        val daVedere = ArrayDeque(archi[da].orEmpty())
        val numeriche = mutableSetOf<String>()
        while (daVedere.isNotEmpty()) {
            val nodo = daVedere.removeFirst()
            if (!visti.add(nodo)) continue
            if (nodo.eNumerica()) numeriche += nodo else daVedere.addAll(archi[nodo].orEmpty())
        }
        return numeriche
    }

    private fun String.eNumerica(): Boolean = toIntOrNull() != null

    // Le scene nominate dentro i `params` di una meccanica. Sono le due
    // chiavi che il motore legge davvero (`MechanicsExecutor`,
    // `StatMechanics`), cercate anche dentro `action` e negli `outcomes`
    // della tabella dei risultati — lì il salto sta annidato.
    // `answerSceneId`/`wrongSceneId`/`giveUpSceneId` sono le tre uscite
    // di un enigma numerico: il grafo ufficiale le conosce tutte e tre,
    // quindi vanno contate anche qui.
    private val CHIAVI_DI_SCENA = listOf(
        "nextSceneId", "targetScene", "answerSceneId", "wrongSceneId", "giveUpSceneId",
    )

    internal fun destinazioniDi(params: kotlinx.serialization.json.JsonObject): List<String> {
        val trovate = mutableListOf<String>()
        fun scava(oggetto: kotlinx.serialization.json.JsonObject) {
            CHIAVI_DI_SCENA.forEach { chiave ->
                (oggetto[chiave] as? JsonPrimitive)?.contentOrNull?.let { trovate += it }
            }
            oggetto.values.forEach { valore ->
                runCatching { scava(valore.jsonObject) }
                runCatching { valore.jsonArray.forEach { voce -> runCatching { scava(voce.jsonObject) } } }
            }
        }
        scava(params)
        return trovate
    }

    fun confronta(manifest: Manifest, archiUfficiali: Set<Pair<String, String>>): Esito {
        // Solo archi fra paragrafi numerati: il grafo ufficiale non
        // conosce i nodi che aggiungiamo noi.
        val ufficiali = archiUfficiali.filter { it.first.eNumerica() && it.second.eNumerica() }.toSet()
        val nostri = archiDi(manifest)

        val mancanti = ufficiali.filterNot { (da, a) -> a in raggiungibiliNumeriche(nostri, da) }
        val nostriNumerici = nostri.flatMap { (da, verso) ->
            verso.filter { da.eNumerica() && it.eNumerica() }.map { da to it }
        }.toSet()

        return Esito(
            archiUfficiali = ufficiali.size,
            mancanti = mancanti.sortedBy { it.first.toIntOrNull() ?: 0 },
            inPiu = (nostriNumerici - ufficiali).sortedBy { it.first.toIntOrNull() ?: 0 },
        )
    }
}
