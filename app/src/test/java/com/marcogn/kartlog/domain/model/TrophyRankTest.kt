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
}
