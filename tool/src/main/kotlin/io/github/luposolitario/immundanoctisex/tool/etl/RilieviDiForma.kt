package io.github.luposolitario.immundanoctisex.tool.etl

import io.github.luposolitario.immundanoctisex.core.data.model.EndingOutcome
import io.github.luposolitario.immundanoctisex.tool.etl.FormaDelGrafo.Misura
import kotlin.math.ceil
import kotlin.math.roundToInt

// Il giudizio sulla forma misurata da `FormaDelGrafo`, e soprattutto
// **quanto** manca per rientrare (06/08/2026, dal ciclo descritto da
// Michele: "il modello puo' suggerire se inserire altri nodi scelta
// perche' il loro numero e' basso, aggiungere nuovi finali di morte...
// nel rispetto delle regole statistiche").
//
// Un avviso che dice "il grado e' 1,18, troppo basso" costringe chi
// legge a rifare il conto per sapere cosa fare. Un avviso che dice
// "servono ~7 bivi in piu'" e' un'istruzione: si puo' girare a un
// modello, o a una persona, e si sa quando si e' finito.
//
// Le quantita' puntano alla MEDIA misurata (doc/FORMA-DEI-GRAFI.md),
// non al minimo accettabile: chiedere il minimo lascerebbe il libro
// appena sopra la soglia, e al giro dopo ci si torna.
object RilieviDiForma {

    enum class Gravita { ERRORE, AVVISO }

    data class Rilievo(
        val gravita: Gravita,
        val messaggio: String,
        // L'istruzione quantificata, quando la si sa calcolare.
        val cosaFare: String? = null,
    )

    fun di(m: Misura): List<Rilievo> = buildList {
        strutturali(m)
        if (m.scene < FormaDelGrafo.SCENE_MINIME) return@buildList
        statistici(m)
    }

    // Il BILANCIO di un giro: dove si e' arrivati rispetto ai bersagli,
    // detto anche quando va tutto bene (06/08/2026, Michele: "se vede
    // che lo scrittore sta andando troppo in la', gli dice attenzione,
    // non aggiungete piu' nuove sottotrame, oppure attenzione il numero
    // delle scelte e' ormai compiuto").
    //
    // I rilievi qui sopra parlano solo quando manca qualcosa. In un
    // ciclo iterativo serve anche il segnale opposto — "basta cosi'" —
    // altrimenti si continua ad aggiungere finche' qualcuno non decide a
    // occhio che e' abbastanza, ed e' il modo in cui un libro si gonfia.
    fun bilancio(m: Misura): List<String> = buildList {
        if (m.scene < FormaDelGrafo.SCENE_MINIME) {
            add("$COMPLETO ${m.scene} scene: troppo poche per giudicare la forma (da ${FormaDelGrafo.SCENE_MINIME} in su)")
            return@buildList
        }
        // Il bersaglio 40-60 viene dal nostro prompt di generazione, non
        // dai grafi: i libri di Dever ne hanno 350, che e' un'altra
        // scala editoriale. Oltre il doppio del bersaglio non si dice
        // "stop" — non e' un libro in costruzione col nostro metodo.
        add(
            when {
                m.scene > SCENE_MAX * 2 -> "$COMPLETO ${m.scene} scene (libro di scala editoriale, non del nostro formato)"
                m.scene > SCENE_MAX ->
                    "$OLTRE ${m.scene} scene (bersaglio $SCENE_MIN-$SCENE_MAX): non aggiungerne altre, " +
                        "collega quelle che ci sono"
                m.scene < SCENE_MIN ->
                    "$SOTTO ${m.scene} scene: ce n'e' spazio per altre ${SCENE_MIN - m.scene}-${SCENE_MAX - m.scene}"
                else -> "$COMPLETO ${m.scene} scene, dentro il bersaglio $SCENE_MIN-$SCENE_MAX"
            },
        )
        add(
            if (m.grado >= GRADO_TARGET) {
                "$COMPLETO %.2f uscite per scena: il numero delle scelte e' compiuto".format(m.grado)
            } else {
                "$SOTTO %.2f uscite per scena (bersaglio %.2f)".format(m.grado, GRADO_TARGET)
            },
        )
        add(
            if (m.riconvergenza >= RICONVERGENZA_TARGET) {
                "$COMPLETO %.0f%% di scene raggiunte da piu' vie: i rami rientrano a sufficienza".format(m.riconvergenza)
            } else {
                "$SOTTO %.0f%% di scene raggiunte da piu' vie (bersaglio %.0f%%)".format(m.riconvergenza, RICONVERGENZA_TARGET)
            },
        )
        // Le sconfitte in PROPORZIONE, non in numero fisso: nei libri
        // veri sono il 3-7% delle scene (12-27 su 350), che su un libro
        // da 50 fa 2-4. Un numero fisso direbbe "troppe" a un libro di
        // Dever e "abbastanza" a uno di trecento scene con tre morti.
        // Sono anche la misura delle sottotrame aperte: un ramo che non
        // rientra e' un finale, non una storia parallela.
        val morti = m.finaliPerEsito[EndingOutcome.DEFEAT] ?: 0
        val attese = (m.scene * MORTI_QUOTA_MIN / 100.0).roundToInt().coerceAtLeast(MORTI_MINIME)
        val massime = (m.scene * MORTI_QUOTA_MAX / 100.0).roundToInt().coerceAtLeast(MORTI_MINIME + 2)
        add(
            when {
                morti > massime ->
                    "$OLTRE $morti finali di sconfitta su ${m.scene} scene (attesi $attese-$massime): " +
                        "niente nuove sottotrame, falle riconfluire"
                morti >= attese -> "$COMPLETO $morti finali di sconfitta, nel bersaglio $attese-$massime"
                else -> "$SOTTO $morti finali di sconfitta (bersaglio $attese-$massime)"
            },
        )
    }

    // Valgono a qualunque dimensione: sono difetti, non scostamenti.
    private fun MutableList<Rilievo>.strutturali(m: Misura) {
        if (m.irraggiungibili.isNotEmpty()) {
            add(
                errore(
                    "${m.irraggiungibili.size} scene che nessuno raggiunge: ${elenco(m.irraggiungibili)}",
                    "collega ognuna a una scena esistente, oppure toglila",
                ),
            )
        }
        if (m.vicoliCiechi.isNotEmpty()) {
            add(
                errore(
                    "${m.vicoliCiechi.size} scene senza uscita che non sono finali: ${elenco(m.vicoliCiechi)}",
                    "dai a ognuna almeno una scelta, o marcala come ENDING con il suo outcome",
                ),
            )
        }
        if (m.finaliPerEsito[EndingOutcome.VICTORY] == null) {
            add(errore("nessun finale di vittoria: l'avventura non si puo' vincere", "marca VICTORY la scena conclusiva"))
        }
        val morti = m.finaliPerEsito[EndingOutcome.DEFEAT] ?: 0
        if (morti == 0) {
            add(avviso("nessun finale di sconfitta: nessuna scelta costa niente", "aggiungi $MORTI_ATTESE finali di sconfitta, sparsi, almeno uno nella prima meta'"))
        } else if (m.scene >= FormaDelGrafo.SCENE_MINIME && morti < MORTI_MINIME) {
            add(
                avviso(
                    "solo $morti finale/i di sconfitta su ${m.scene} scene",
                    "portane il numero a $MORTI_ATTESE: sotto, il lettore non crede al pericolo",
                ),
            )
        }
    }

    // Scostamenti dalla forma misurata: si applicano solo da
    // SCENE_MINIME in su, perche' una percentuale su sei elementi non e'
    // una misura.
    private fun MutableList<Rilievo>.statistici(m: Misura) {
        val ramificabili = m.scene - m.finali
        when {
            m.grado < FormaDelGrafo.GRADO_MIN -> {
                val mancanti = ceil(GRADO_TARGET * ramificabili - m.collegamenti).toInt()
                add(
                    avviso(
                        "%.2f uscite per scena: e' un racconto lineare travestito da librogame".format(m.grado),
                        "aggiungi ~$mancanti scelte: $mancanti scene su $ramificabili diventano bivi " +
                            "(nei libri veri e' circa una su due)",
                    ),
                )
            }
            m.grado > FormaDelGrafo.GRADO_MAX ->
                add(
                    avviso(
                        "%.2f uscite per scena: cosi' il libro esplode e non lo si finisce".format(m.grado),
                        "accorpa le scelte che portano allo stesso posto senza differenza",
                    ),
                )
        }
        if (m.riconvergenza < FormaDelGrafo.RICONVERGENZA_MIN) {
            val attese = (RICONVERGENZA_TARGET / 100.0 * m.scene).roundToInt()
            val ora = (m.riconvergenza / 100.0 * m.scene).roundToInt()
            add(
                avviso(
                    "%.0f%% di scene raggiunte da piu' percorsi: i rami non rientrano".format(m.riconvergenza),
                    "fai rientrare ~${attese - ora} rami in piu' su scene che gia' esistono, invece di crearne di nuove",
                ),
            )
        }
        val quotaTardivi = percentuale(m.rientriTardivi.size, m.bivi)
        if (quotaTardivi > FormaDelGrafo.TARDIVI_MAX) {
            val daAccorciare = m.rientriTardivi.size - (FormaDelGrafo.TARDIVI_MAX / 100.0 * m.bivi).toInt()
            add(
                avviso(
                    "%.0f%% dei bivi rientra dopo piu' di ${FormaDelGrafo.RIENTRO_MAX} tappe: %s"
                        .format(quotaTardivi, elenco(m.rientriTardivi)),
                    "accorcia ~$daAccorciare di questi rami a 2-3 tappe, che e' la mediana dei libri veri",
                ),
            )
        }
        m.quotaViva?.let { viva ->
            if (viva < FormaDelGrafo.QUOTA_VIVA_MIN) {
                val morte = ((100 - viva) / 100.0 * m.scene).roundToInt()
                add(
                    avviso(
                        "solo %.0f%% delle scene puo' ancora arrivare alla vittoria".format(viva),
                        "$morte scene sono partite gia' perse: NON servono scene nuove, serve far " +
                            "RIENTRARE quei rami nel percorso invece di lasciarli finire in una morte",
                    ),
                )
            }
        }
        m.quotaObbligata?.let { quota ->
            if (quota < FormaDelGrafo.QUOTA_MIN) {
                add(
                    avviso(
                        "solo %.0f%% del cammino e' obbligato: la storia non ha spina dorsale".format(quota),
                        "il lettore puo' evitare quasi tutto: alcune tappe devono stare su OGNI percorso",
                    ),
                )
            } else if (quota > FormaDelGrafo.QUOTA_MAX) {
                val scorciatoie = ceil((quota - QUOTA_TARGET) / 100.0 * m.profondita / TAPPE_PER_SCORCIATOIA).toInt()
                add(
                    avviso(
                        "%.0f%% del cammino e' obbligato: le scelte contano poco".format(quota),
                        "servono ~$scorciatoie scorciatoie che aggirino una tappa di acquisizione " +
                            "(un oggetto, un alleato, un'informazione) e rientrino 2-3 tappe dopo",
                    ),
                )
            }
        }
    }

    private fun percentuale(parte: Int, totale: Int) = if (totale == 0) 0.0 else 100.0 * parte / totale

    private fun elenco(ids: List<String>) =
        ids.take(10).joinToString(", ") + if (ids.size > 10) ", ... e altre ${ids.size - 10}" else ""

    private fun errore(messaggio: String, cosaFare: String? = null) = Rilievo(Gravita.ERRORE, messaggio, cosaFare)
    private fun avviso(messaggio: String, cosaFare: String? = null) = Rilievo(Gravita.AVVISO, messaggio, cosaFare)

    // Bersagli: la MEDIA misurata, non il minimo accettabile.
    private const val GRADO_TARGET = 1.65
    private const val RICONVERGENZA_TARGET = 32.0
    private const val QUOTA_TARGET = 46.0

    // Bersagli del bilancio. SCENE_MIN/MAX vengono dal prompt di
    // generazione (40-60 tappe), non dai grafi: i libri di Dever ne
    // hanno 350, ma sono di un'altra scala editoriale.
    private const val SCENE_MIN = 40
    private const val SCENE_MAX = 60

    // Quota di sconfitte misurata sui librogame veri: 12-27 finali su
    // 350 scene. Proporzionale, cosi' vale a ogni scala.
    private const val MORTI_QUOTA_MIN = 3.0
    private const val MORTI_QUOTA_MAX = 7.0

    private const val COMPLETO = "[ok]   "
    private const val SOTTO = "[manca]"
    private const val OLTRE = "[stop] "

    // Ogni scorciatoia aggira 2-3 tappe (rientro mediano misurato: 3).
    private const val TAPPE_PER_SCORCIATOIA = 2.5

    private const val MORTI_MINIME = 2
    private const val MORTI_ATTESE = "3-5"
}
