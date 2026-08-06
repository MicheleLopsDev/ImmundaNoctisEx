package io.github.luposolitario.immundanoctisex.tool.etl

import io.github.luposolitario.immundanoctisex.core.data.model.EndingOutcome
import io.github.luposolitario.immundanoctisex.tool.etl.FormaDelGrafo.Misura
import kotlin.math.roundToInt

// Le sonde semaforiche della barra in cima all'editor (06/08/2026,
// Michese: "sarebbe utile mettere delle sonde con colori semaforici che
// rappresentano la distanza dal valore voluto in percentuale... così
// sarebbe ad occhio per l'editor vedere se la cosa va o non va").
//
// Qui c'e' solo il CALCOLO, senza Compose: la barra che le disegna sta
// in `editor/SondeDiFormaBar.kt`. Cosi' la regola con cui un numero
// diventa rosso o verde e' verificabile da un test, invece di essere
// sepolta in una @Composable.
//
// La `salute` non e' il valore: e' quanto ci si e' avvicinati al
// bersaglio, da 0 (fermo al punto di partenza) a 1 (arrivato). Serve
// perche' "1,07 uscite per scena" non dice niente a colpo d'occhio,
// mentre una barra rossa si'.
object SondeDiForma {

    data class Sonda(
        val etichetta: String,
        val valore: String,
        val bersaglio: String,
        val salute: Float,
    )

    fun di(m: Misura): List<Sonda> = buildList {
        // Il bersaglio 40-60 e' del NOSTRO formato, non dei libri di
        // Dever, che ne hanno 350: su un libro di quella scala la sonda
        // resterebbe rossa per sempre segnalando un difetto inesistente.
        // Sopra il doppio del bersaglio si mostra il numero e basta.
        val fuoriScala = m.scene > SCENE_MAX * 2
        add(
            Sonda(
                etichetta = "scene",
                valore = "${m.scene}",
                bersaglio = if (fuoriScala) "—" else "$SCENE_MIN-$SCENE_MAX",
                // Una scena in piu' del bersaglio non e' un difetto come
                // esserne a meta': si esce dal verde piano.
                salute = if (fuoriScala) {
                    1f
                } else {
                    saluteInIntervallo(m.scene.toDouble(), SCENE_MIN.toDouble(), SCENE_MAX.toDouble(), tolleranza = 0.5)
                },
            ),
        )
        add(
            Sonda(
                etichetta = "uscite per scena",
                valore = decimale(m.grado),
                bersaglio = decimale(GRADO_TARGET),
                // Si parte da 1,0: una catena pura, dove ogni scena ha
                // una sola uscita. E' lo zero naturale della misura.
                salute = saluteVersoAlto(m.grado, partenza = 1.0, bersaglio = GRADO_TARGET),
            ),
        )
        add(
            Sonda(
                etichetta = "scene con piu' vie",
                valore = percento(m.riconvergenza),
                bersaglio = percento(RICONVERGENZA_TARGET),
                salute = saluteVersoAlto(m.riconvergenza, partenza = 0.0, bersaglio = RICONVERGENZA_TARGET),
            ),
        )
        m.rientroMediano?.let { rientro ->
            add(
                Sonda(
                    etichetta = "rientro dei rami",
                    valore = "$rientro tappe",
                    bersaglio = "$RIENTRO_TARGET",
                    // Qui il bersaglio e' un valore centrale: allontanarsi
                    // in su (rami lunghi) o in giu' (bivi finti che
                    // rientrano subito) peggiora allo stesso modo.
                    salute = saluteAttornoA(rientro.toDouble(), RIENTRO_TARGET.toDouble(), massimoScarto = 3.0),
                ),
            )
        }
        m.quotaObbligata?.let { quota ->
            add(
                Sonda(
                    etichetta = "cammino obbligato",
                    valore = percento(quota),
                    bersaglio = percento(QUOTA_TARGET),
                    salute = saluteAttornoA(quota, QUOTA_TARGET, massimoScarto = 40.0),
                ),
            )
        }
        m.quotaViva?.let { viva ->
            add(
                Sonda(
                    etichetta = "puo' ancora vincere",
                    valore = percento(viva),
                    bersaglio = "≥ ${percento(VIVA_TARGET)}",
                    salute = saluteVersoAlto(viva, partenza = 40.0, bersaglio = VIVA_TARGET),
                ),
            )
        }
        val morti = m.finaliPerEsito[EndingOutcome.DEFEAT] ?: 0
        val attese = (m.scene * MORTI_QUOTA_MIN / 100.0).roundToInt().coerceAtLeast(2)
        val massime = (m.scene * MORTI_QUOTA_MAX / 100.0).roundToInt().coerceAtLeast(4)
        add(
            Sonda(
                etichetta = "finali di sconfitta",
                valore = "$morti",
                bersaglio = "$attese-$massime",
                salute = saluteInIntervallo(morti.toDouble(), attese.toDouble(), massime.toDouble(), tolleranza = 1.0),
            ),
        )
    }

    // --- le tre forme di salute ---

    // Piu' e' alto meglio e': 0 alla partenza, 1 al bersaglio.
    private fun saluteVersoAlto(valore: Double, partenza: Double, bersaglio: Double): Float =
        (((valore - partenza) / (bersaglio - partenza))).coerceIn(0.0, 1.0).toFloat()

    // Dentro l'intervallo e' 1. I due lati NON sono simmetrici, di
    // proposito: mancarne meta' e' peggio che averne un po' troppi.
    // - sotto il minimo: la frazione raggiunta, cosi' zero sconfitte su
    //   due attese fa zero e non mezzo (un libro senza morti dev'essere
    //   rosso pieno, non giallo);
    // - sopra il massimo: cala piano, in multipli dell'ampiezza.
    private fun saluteInIntervallo(valore: Double, min: Double, max: Double, tolleranza: Double): Float {
        if (valore in min..max) return 1f
        if (valore < min) return (valore / min).coerceIn(0.0, 1.0).toFloat()
        val ampiezza = (max - min).coerceAtLeast(1.0)
        return (1.0 - (valore - max) / (ampiezza * tolleranza * 2)).coerceIn(0.0, 1.0).toFloat()
    }

    // Bersaglio centrale: 1 sul punto, 0 a `massimoScarto` di distanza,
    // da qualunque lato.
    private fun saluteAttornoA(valore: Double, bersaglio: Double, massimoScarto: Double): Float {
        val scarto = kotlin.math.abs(valore - bersaglio)
        return (1.0 - scarto / massimoScarto).coerceIn(0.0, 1.0).toFloat()
    }

    private fun decimale(v: Double) = "%.2f".format(v)
    private fun percento(v: Double) = "${v.roundToInt()}%"

    // Bersagli: gli stessi di RilieviDiForma, da doc/FORMA-DEI-GRAFI.md.
    const val GRADO_TARGET = 1.65
    const val RICONVERGENZA_TARGET = 32.0
    const val QUOTA_TARGET = 46.0
    const val VIVA_TARGET = 95.0
    const val RIENTRO_TARGET = 3
    const val SCENE_MIN = 40
    const val SCENE_MAX = 60
    const val MORTI_QUOTA_MIN = 3.0
    const val MORTI_QUOTA_MAX = 7.0
}
