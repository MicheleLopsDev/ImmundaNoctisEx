package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable

// Blocco combat minimale della scena (REGOLE.md §1.5): l'autore scrive solo
// nome, due statistiche e destinazioni; a runtime il motore idrata un
// Character unico per il nemico. loseSceneId assente => fallback su
// deathSceneId del manifest; winSceneId è sempre obbligatorio.
@Serializable
data class Combat(
    val enemyName: String,
    // ID canonico dal catalogo immagini (EnemyImageCatalog), dichiarato
    // dall'autore come backgroundImage per le location: niente tag Gemma,
    // niente costo nel prompt (decisione Michele 22/07/2026). Null o non
    // valido = nessun ritratto, solo il nome in testo come prima.
    val enemyImage: String? = null,
    val enemyCombatSkill: Int,
    val enemyEndurance: Int,
    val immuneToMindblast: Boolean = false,
    val evadeAfterRound: Int = 0,
    val winSceneId: String,
    // Vittoria RAPIDA (05/08/2026). I libri distinguono spesso due
    // esiti a seconda di quanto è durato lo scontro:
    //
    //   "If you win the combat in seven rounds or less, turn to 272.
    //    If you win the combat in more than seven rounds, turn to 324."
    //
    // Misurato: 10 scene sui 5 libri Project Aon convertiti. Senza
    // questo campo il parser leggeva entrambe le righe e la seconda
    // sovrascriveva la prima — chi vinceva in fretta finiva comunque
    // nella scena "lenta", perdendo la ricompensa che il libro gli
    // aveva previsto (trovato col grafo ufficiale dei percorsi).
    //
    // `winSceneId` resta la destinazione normale: questa vale SOLO se
    // il combattimento si chiude entro `winEntroRound` round inclusi.
    // Assente (default) = comportamento di prima, un esito solo.
    val winSceneIdRapido: String? = null,
    val winEntroRound: Int? = null,
    val loseSceneId: String? = null,
    val evadeSceneId: String? = null,
    // "Se ti fanno male, il combattimento finisce QUI" (05/08/2026).
    // Quattro scene sui 5 libri, tutte con la stessa frase:
    //
    //   "If you lose any ENDURANCE points during this combat, even when
    //    attempting to evade, turn immediately to 66."
    //
    // Non è un esito finale come vittoria/sconfitta: è un'uscita
    // IMMEDIATA che scatta appena il giocatore subisce un punto di
    // danno, e batte tutto il resto — anche l'evasione, come dice il
    // testo. Serve per gli scontri in cui il libro premia solo chi ne
    // esce illeso (03tcok 138 e 263, 04tcod 133).
    val seColpitoSceneId: String? = null,
)
