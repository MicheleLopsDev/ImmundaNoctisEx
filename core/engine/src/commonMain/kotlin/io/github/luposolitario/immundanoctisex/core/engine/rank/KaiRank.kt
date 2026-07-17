package io.github.luposolitario.immundanoctisex.core.engine.rank

// Gradi Kai (REGOLE.md Blocco 3): puramente cosmetici, titolo calcolato dal
// numero di discipline possedute. ID canonici qui, nomi mostrati SOLO in
// strings.xml (in v1 i nomi italiani erano hardcoded nell'engine —
// corretto). Nessun effetto meccanico.
enum class KaiRank {
    NOVICE,
    INITIATE,
    DISCIPLE,
    WAYFARER,
    WARRIOR,
    MASTER,
    GRAND_MASTER,
    SUPREME_GRAND_MASTER,
    ;

    companion object {
        // Soglie da REGOLE.md Blocco 3: 0-4 Novizio, 5 Iniziato, 6
        // Discepolo, 7 Viandante, 8 Guerriero, 9 Maestro, 10 Gran Maestro,
        // >10 Gran Maestro Supremo.
        fun fromDisciplineCount(count: Int): KaiRank = when {
            count <= 4 -> NOVICE
            count == 5 -> INITIATE
            count == 6 -> DISCIPLE
            count == 7 -> WAYFARER
            count == 8 -> WARRIOR
            count == 9 -> MASTER
            count == 10 -> GRAND_MASTER
            else -> SUPREME_GRAND_MASTER
        }
    }
}
