package com.marcogn.kartlog.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrophyRankTest {

    @Test
    fun `la scala e bronzo, argento, oro, oro 1-2-3 stelle`() {
        assertEquals((1..TrophyRank.MAX_LEVEL).toList(), TrophyRank.entries.map { it.level })
        assertEquals(TrophyRank.GOLD_3_STARS, TrophyRank.entries.maxBy { it.level })
    }

    @Test
    fun `conversione dal vecchio modello stelle più posizione`() {
        assertEquals(TrophyRank.GOLD, TrophyRank.fromLegacy(placement = 1, stars = 0))
        assertEquals(TrophyRank.GOLD_2_STARS, TrophyRank.fromLegacy(placement = 1, stars = 2))
        assertEquals(TrophyRank.GOLD_3_STARS, TrophyRank.fromLegacy(placement = 1, stars = 7))
        assertEquals(TrophyRank.SILVER, TrophyRank.fromLegacy(placement = 2, stars = 3))
        assertEquals(TrophyRank.BRONZE, TrophyRank.fromLegacy(placement = 3, stars = 0))
        assertNull(TrophyRank.fromLegacy(placement = 4, stars = 0))
        assertNull(TrophyRank.fromLegacy(placement = null, stars = 0)) // eliminato in un KO
    }

    @Test
    fun `un trofeo vale anche per le cilindrate inferiori, mai per le superiori`() {
        val ranks = mapOf(Cc.CC_150 to TrophyRank.SILVER, Cc.CC_50 to TrophyRank.GOLD_1_STAR)

        assertEquals(TrophyRank.GOLD_1_STAR, effectiveRank(ranks, Cc.CC_50))
        assertEquals(TrophyRank.SILVER, effectiveRank(ranks, Cc.CC_100))
        assertEquals(TrophyRank.SILVER, effectiveRank(ranks, Cc.CC_150))
        assertNull(effectiveRank(ranks, Cc.MIRROR))
    }

    @Test
    fun `lo Specchio vale per tutte le cilindrate`() {
        val ranks = mapOf(Cc.MIRROR to TrophyRank.GOLD_3_STARS, Cc.CC_100 to TrophyRank.BRONZE)

        Cc.entries.forEach { assertEquals(TrophyRank.GOLD_3_STARS, effectiveRank(ranks, it)) }
    }
}
