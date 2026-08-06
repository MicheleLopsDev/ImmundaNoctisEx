package io.github.luposolitario.immundanoctisex.tool.etl

import io.github.luposolitario.immundanoctisex.core.data.model.EndingOutcome
import io.github.luposolitario.immundanoctisex.core.data.model.Manifest
import io.github.luposolitario.immundanoctisex.core.data.model.SceneType

// La forma di un libro, misurata e confrontata con quella dei librogame
// pubblicati (`doc/FORMA-DEI-GRAFI.md`: 37 opere, tre serie, due autori,
// due generi).
//
// Serve alla seconda passata sui libri generati da un'IA remota
// (06/08/2026, Michele: "possiamo avere dei binari per evitare che il
// modello [sbandi]... tanto poi li correggiamo noi in seconda passata").
// Il prompt può chiedere quello che vuole, un modello può ignorarlo: una
// misura fatta dopo no. Il difetto tipico di un testo generato è il
// racconto lineare travestito da librogame — un bivio ogni tanto, i rami
// che non rientrano mai, nessuna morte. Si vede tutto da qui.
//
// Complementare a ConfrontoGrafo, che confronta con una fonte esterna:
// questo giudica un libro che non ha nessun originale con cui confrontarsi.
object FormaDelGrafo {

    data class Misura(
        val scene: Int,
        val collegamenti: Int,
        val finali: Int,
        val profondita: Int,
        // Scene con più di un ingresso: i punti in cui le strade si
        // ricongiungono. Sotto il 20% il libro raddoppia a ogni bivio.
        val riconvergenza: Double,
        // Quota del cammino principale che ogni percorso deve attraversare.
        // null se il libro non dichiara nessuna vittoria.
        val quotaObbligata: Double?,
        val rientroMediano: Int?,
        // Quota di scene da cui la vittoria e' ANCORA raggiungibile.
        // Nei librogame pubblicati e' il 95% (min 86%): perdere e'
        // l'eccezione, non la norma, e non esiste "la" strada giusta —
        // il cammino vincente piu' lungo e' il doppio del piu' corto.
        // null se il libro non dichiara nessuna vittoria.
        val quotaViva: Double?,
        val irraggiungibili: List<String>,
        val vicoliCiechi: List<String>,
        val bivi: Int,
        val rientriTardivi: List<String>,
        val finaliPerEsito: Map<EndingOutcome, Int>,
    ) {
        // Uscite per scena, contando SOLO le scene che possono averne: un
        // finale non ha uscite per definizione, e tenerlo al
        // denominatore abbassa la media in proporzione a quanto il libro
        // e' corto. Su 350 scene con 12 finali sposta il 3%, su un libro
        // di 6 scene con 2 finali il 33% — abbastanza da far gridare
        // "racconto lineare" a un libro sano. Misurato con questa
        // formula sulle 37 opere: media 1,65, da 1,45 a 2,18.
        val grado: Double
            get() = if (scene == finali) 0.0 else collegamenti.toDouble() / (scene - finali)
    }

    enum class Gravita { ERRORE, AVVISO }

    data class Rilievo(val gravita: Gravita, val messaggio: String)

    fun misura(manifest: Manifest): Misura {
        val archi = ConfrontoGrafo.archiDi(manifest)
        val scene = manifest.scenes.map { it.id }
        val noti = scene.toSet()
        // Solo destinazioni che esistono: le altre sono un errore di
        // grafo, che PackageValidator segnala già come tale.
        val uscite = scene.associateWith { archi[it].orEmpty().filter { d -> d in noti }.toSet() }
        val ingressi = uscite.values.flatten().groupingBy { it }.eachCount()

        val partenza = manifest.scenes.firstOrNull { it.sceneType == SceneType.START }?.id
        val distanze = distanzeDa(uscite, partenza)

        val finali = manifest.scenes.filter { it.sceneType == SceneType.ENDING }
        val vittoria = finali.firstOrNull { it.outcome == EndingOutcome.VICTORY }?.id

        return Misura(
            scene = scene.size,
            collegamenti = uscite.values.sumOf { it.size },
            finali = finali.size,
            profondita = distanze.values.maxOrNull() ?: 0,
            riconvergenza = percentuale(scene.count { (ingressi[it] ?: 0) > 1 }, scene.size),
            quotaObbligata = quotaObbligata(uscite, partenza, vittoria, distanze),
            quotaViva = vittoria?.let { percentuale(risalgonoA(uscite, distanze.keys, it).size, distanze.size) },
            rientroMediano = rientri(uscite).sorted().let { if (it.isEmpty()) null else it[it.size / 2] },
            irraggiungibili = scene.filterNot { it in distanze }.sorted(),
            vicoliCiechi = manifest.scenes
                .filter { it.sceneType != SceneType.ENDING && uscite[it.id].isNullOrEmpty() }
                .map { it.id }.sorted(),
            bivi = bivi(uscite).size,
            rientriTardivi = bivi(uscite).filter { (rientroDa(uscite, it) ?: 0) > RIENTRO_MAX }.sorted(),
            finaliPerEsito = finali.mapNotNull { it.outcome }.groupingBy { it }.eachCount(),
        )
    }

    // I rilievi, dal più grave. Errore = il libro non si gioca com'è;
    // avviso = si gioca, ma non ha la forma di un librogame.
    //
    // I controlli STRUTTURALI (scene orfane, vicoli ciechi, finali
    // mancanti) valgono a qualunque dimensione. Quelli STATISTICI
    // (grado, riconvergenza, quota obbligata, rientri) solo da
    // SCENE_MINIME in su: su un libro di sei scene "il 17% riconverge"
    // vuol dire "una scena", e la quota obbligata viene 100% per pura
    // aritmetica. Segnalare lì sarebbe rumore garantito sui libri di
    // prova, ed e' cosi' che si impara a ignorare gli avvisi.
    fun rilievi(m: Misura): List<Rilievo> = buildList {
        if (m.irraggiungibili.isNotEmpty()) {
            add(errore("${m.irraggiungibili.size} scene che nessuno raggiunge: ${elenco(m.irraggiungibili)}"))
        }
        if (m.vicoliCiechi.isNotEmpty()) {
            add(errore("${m.vicoliCiechi.size} scene senza uscita che non sono finali: ${elenco(m.vicoliCiechi)}"))
        }
        if (m.finaliPerEsito[EndingOutcome.VICTORY] == null) {
            add(errore("nessun finale di vittoria: l'avventura non si può vincere"))
        }
        if (m.finaliPerEsito[EndingOutcome.DEFEAT] == null) {
            add(avviso("nessun finale di sconfitta: nessuna scelta costa niente"))
        }
        if (m.scene < SCENE_MINIME) return@buildList
        when {
            m.grado < GRADO_MIN -> add(
                avviso(
                    "%.2f uscite per scena: sotto %.1f e' un racconto lineare travestito da librogame"
                        .format(m.grado, GRADO_MIN),
                ),
            )
            m.grado > GRADO_MAX -> add(
                avviso(
                    "%.2f uscite per scena: sopra %.1f il libro esplode e non lo si finisce"
                        .format(m.grado, GRADO_MAX),
                ),
            )
        }
        if (m.riconvergenza < RICONVERGENZA_MIN) {
            add(
                avviso(
                    "%.0f%% di scene raggiunte da piu' percorsi (i libri veri: 26-35%%): i rami non rientrano"
                        .format(m.riconvergenza),
                ),
            )
        }
        // In proporzione, non in numero assoluto: anche un libro sano ha
        // una minoranza di rami lunghi (nei cinque Project Aon fra il 5%
        // e il 12% dei bivi). Contarli e basta segnalerebbe ogni libro
        // vero, ed e' cosi' che si impara a ignorare gli avvisi.
        val quotaTardivi = percentuale(m.rientriTardivi.size, m.bivi)
        if (quotaTardivi > TARDIVI_MAX) {
            add(
                avviso(
                    "%.0f%% dei bivi rientra dopo piu' di $RIENTRO_MAX tappe (nei libri veri la mediana e' 3): %s"
                        .format(quotaTardivi, elenco(m.rientriTardivi)),
                ),
            )
        }
        // Il controllo piu' severo sui libri generati (06/08/2026,
        // dall'intuizione di Michele "e se il libro fosse il percorso
        // lineare, quello piu' semplice e corretto per arrivare alla
        // fine?"). I dati dicono l'opposto: nei librogame veri il 95%
        // delle scene puo' ancora vincere, e il cammino vincente piu'
        // lungo e' il doppio del piu' corto. Non esiste "la" strada
        // giusta. Un libro molto sotto questa quota e' un "indovina il
        // percorso": il lettore cammina per pagine senza sapere di aver
        // gia' perso.
        m.quotaViva?.let { viva ->
            if (viva < QUOTA_VIVA_MIN) {
                add(
                    avviso(
                        ("solo %.0f%% delle scene puo' ancora arrivare alla vittoria (i libri veri: 86-100%%): " +
                            "chi sbaglia una scelta cammina senza saperlo verso una sconfitta obbligata")
                            .format(viva),
                    ),
                )
            }
        }
        m.quotaObbligata?.let { quota ->
            if (quota < QUOTA_MIN) {
                add(avviso("solo %.0f%% del cammino e' obbligato (i libri veri: 42-50%%): la storia non ha spina dorsale".format(quota)))
            } else if (quota > QUOTA_MAX) {
                add(avviso("%.0f%% del cammino e' obbligato (i libri veri: 42-50%%): le scelte contano poco".format(quota)))
            }
        }
    }

    // --- misure di supporto ---

    private fun distanzeDa(uscite: Map<String, Set<String>>, partenza: String?): Map<String, Int> {
        if (partenza == null) return emptyMap()
        val distanze = mutableMapOf(partenza to 0)
        val coda = ArrayDeque(listOf(partenza))
        while (coda.isNotEmpty()) {
            val nodo = coda.removeFirst()
            uscite[nodo].orEmpty().forEach { prossimo ->
                if (prossimo !in distanze) {
                    distanze[prossimo] = distanze.getValue(nodo) + 1
                    coda.addLast(prossimo)
                }
            }
        }
        return distanze
    }

    // Le scene che OGNI percorso verso la vittoria attraversa (in gergo:
    // i dominatori). Sono lo scheletro della storia, quello che il
    // lettore non può evitare comunque scelga. Punto fisso per
    // intersezione: i grafi in gioco sono piccoli, non serve di meglio.
    private fun quotaObbligata(
        uscite: Map<String, Set<String>>,
        partenza: String?,
        vittoria: String?,
        distanze: Map<String, Int>,
    ): Double? {
        if (partenza == null || vittoria == null || vittoria !in distanze) return null
        val profondita = distanze.values.maxOrNull()?.takeIf { it > 0 } ?: return null
        val raggiungibili = distanze.keys
        val ingressi = raggiungibili.associateWith { nodo ->
            raggiungibili.filter { nodo in uscite[it].orEmpty() }
        }
        val dominatori = raggiungibili.associateWith { raggiungibili.toMutableSet() }.toMutableMap()
        dominatori[partenza] = mutableSetOf(partenza)
        var cambiato = true
        while (cambiato) {
            cambiato = false
            raggiungibili.filterNot { it == partenza }.forEach { nodo ->
                val precedenti = ingressi.getValue(nodo)
                if (precedenti.isEmpty()) return@forEach
                val nuovo = precedenti
                    .map { dominatori.getValue(it) }
                    .reduce { a, b -> a.intersect(b).toMutableSet() }
                    .toMutableSet().apply { add(nodo) }
                if (nuovo != dominatori[nodo]) {
                    dominatori[nodo] = nuovo
                    cambiato = true
                }
            }
        }
        return percentuale(dominatori.getValue(vittoria).size, profondita)
    }

    // Le scene da cui si arriva ancora a `meta`: si risale il grafo
    // all'indietro partendo dalla vittoria. Quelle fuori sono partite
    // gia' perse — il lettore cammina ancora ma non puo' piu' vincere.
    private fun risalgonoA(
        uscite: Map<String, Set<String>>,
        raggiungibili: Set<String>,
        meta: String,
    ): Set<String> {
        val entranti = mutableMapOf<String, MutableList<String>>()
        uscite.forEach { (da, verso) ->
            if (da in raggiungibili) verso.forEach { entranti.getOrPut(it) { mutableListOf() }.add(da) }
        }
        val vive = mutableSetOf(meta)
        val coda = ArrayDeque(listOf(meta))
        while (coda.isNotEmpty()) {
            entranti[coda.removeFirst()].orEmpty().forEach { if (vive.add(it)) coda.addLast(it) }
        }
        return vive intersect raggiungibili
    }

    private fun bivi(uscite: Map<String, Set<String>>): List<String> =
        uscite.filterValues { it.size >= 2 }.keys.toList()

    private fun rientri(uscite: Map<String, Set<String>>): List<Int> =
        bivi(uscite).mapNotNull { rientroDa(uscite, it) }

    // Dopo quante tappe i rami di un bivio si ritrovano: la distanza dal
    // bivio al primo nodo che TUTTI i rami raggiungono. null se non si
    // ritrovano mai (deviazione definitiva, cioè un finale).
    private fun rientroDa(uscite: Map<String, Set<String>>, bivio: String): Int? {
        val perRamo = uscite.getValue(bivio).map { distanzeDa(uscite, it) }
        if (perRamo.size < 2) return null
        val comuni = perRamo.map { it.keys }.reduce { a, b -> a intersect b }
        return comuni.minOfOrNull { comune -> perRamo.maxOf { it.getValue(comune) } }?.plus(1)
    }

    private fun percentuale(parte: Int, totale: Int) =
        if (totale == 0) 0.0 else 100.0 * parte / totale

    private fun elenco(ids: List<String>) =
        ids.take(10).joinToString(", ") + if (ids.size > 10) ", ... e altre ${ids.size - 10}" else ""

    private fun errore(messaggio: String) = Rilievo(Gravita.ERRORE, messaggio)
    private fun avviso(messaggio: String) = Rilievo(Gravita.AVVISO, messaggio)

    // Soglie da doc/FORMA-DEI-GRAFI.md. Larghe di proposito: segnalano
    // un libro fuori forma, non uno diverso dai gusti di Dever. Sulle 37
    // opere il grado (senza i finali al denominatore, vedi sopra) va da
    // 1,45 a 2,18, la riconvergenza da 23% a 44%, la quota obbligata da
    // 42% a 50% (con un outlier al 6%).
    const val GRADO_MIN = 1.3
    const val GRADO_MAX = 2.3
    const val RICONVERGENZA_MIN = 20.0
    const val RIENTRO_MAX = 5

    // Quota di bivi che possono rientrare tardi senza che sia un
    // difetto: misurata fra 5% e 12% sui cinque libri Project Aon, la
    // soglia sta sopra il caso peggiore osservato.
    const val TARDIVI_MAX = 15.0
    const val QUOTA_MIN = 20.0
    const val QUOTA_MAX = 75.0

    // Misurato fra 86% e 100% sulle 37 opere: la soglia sta sotto il
    // caso peggiore osservato, cosi' segnala solo i libri davvero
    // punitivi.
    const val QUOTA_VIVA_MIN = 80.0

    // Sotto questa soglia si misura ma non si giudica (vedi `rilievi`).
    // Venti scene e' il minimo perche' una percentuale voglia dire
    // qualcosa; il prompt di generazione punta a 40-60 tappe.
    const val SCENE_MINIME = 20
}
