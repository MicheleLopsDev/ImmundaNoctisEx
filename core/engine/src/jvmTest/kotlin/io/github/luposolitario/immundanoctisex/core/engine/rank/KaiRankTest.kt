package io.github.luposolitario.immundanoctisex.core.engine.rank

import kotlin.test.Test
import kotlin.test.assertEquals

class KaiRankTest {

    @Test
    fun leSoglieDelBlocco3() {
        assertEquals(KaiRank.NOVICE, KaiRank.fromDisciplineCount(0))
        assertEquals(KaiRank.NOVICE, KaiRank.fromDisciplineCount(4))
        assertEquals(KaiRank.INITIATE, KaiRank.fromDisciplineCount(5))
        assertEquals(KaiRank.DISCIPLE, KaiRank.fromDisciplineCount(6))
        assertEquals(KaiRank.WAYFARER, KaiRank.fromDisciplineCount(7))
        assertEquals(KaiRank.WARRIOR, KaiRank.fromDisciplineCount(8))
        assertEquals(KaiRank.MASTER, KaiRank.fromDisciplineCount(9))
        assertEquals(KaiRank.GRAND_MASTER, KaiRank.fromDisciplineCount(10))
        assertEquals(KaiRank.SUPREME_GRAND_MASTER, KaiRank.fromDisciplineCount(11))
    }
}
