package io.github.luposolitario.immundanoctisex.core.engine.choice

import io.github.luposolitario.immundanoctisex.core.data.model.Scene
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

// L'enigma numerico: il libro chiede al giocatore un NUMERO che deve
// dedurre, e lo manda al paragrafo che quel numero indica.
//
//   "If you know the correct number that will open the bronze door,
//    turn to that section number.
//    If you choose the wrong combination, turn immediately to 98.
//    If you do not know the number, turn to 156."   (05sots 58)
//
//   "The first of the three numbers is equal to the number of oases on
//    the trail between Ikaresh and Bir Rabalou... When you have broken
//    the code, write the numbers in order and turn to the entry number
//    that they indicate."                            (05sots 331)
//
// È l'unico caso in cui la destinazione NON sta scritta nel libro: sta
// nella testa di chi legge. Per questo non si può convertire come una
// scelta normale — scriverla fra le opzioni la regalerebbe.
//
// `risposta` non è deducibile dal testo e il convertitore non la
// inventa: la si completa a mano (sono due scene su 2984). Senza,
// l'enigma non è giocabile e la scena degrada sulle scelte normali —
// il gioco non si blocca mai.
data class NumberPuzzle(
    val risposta: String?,
    // Dove si finisce sbagliando. Assente = il libro non prevede un
    // errore (05sots 331: o risolvi, o resti lì).
    val sceneSbagliata: String?,
    // Dove si finisce rinunciando a provare.
    val sceneRinuncia: String?,
) {
    val giocabile: Boolean get() = !risposta.isNullOrBlank()

    // Dove va il giocatore che scrive `numero`. Null = non si muove
    // (ha sbagliato ma il libro non dice dove mandarlo: resta a
    // rileggere, come col libro di carta in mano).
    fun destinazionePer(numero: String): String? =
        if (numero.trim() == risposta?.trim()) risposta else sceneSbagliata

    companion object {
        const val COMMAND = "numberPuzzle"

        private fun String?.nonVuoto(): String? = this?.takeIf { it.isNotBlank() }

        fun di(scene: Scene): NumberPuzzle? {
            val meccanica = scene.gameMechanics.firstOrNull { it.command == COMMAND } ?: return null
            fun param(nome: String) =
                (meccanica.params[nome] as? JsonPrimitive)?.contentOrNull.nonVuoto()
            return NumberPuzzle(
                risposta = param("answerSceneId"),
                sceneSbagliata = param("wrongSceneId"),
                sceneRinuncia = param("giveUpSceneId"),
            )
        }
    }
}
