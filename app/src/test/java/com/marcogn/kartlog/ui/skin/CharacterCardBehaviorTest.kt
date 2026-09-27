package com.marcogn.kartlog.ui.skin

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.kartlog.data.local.KartLogDatabase
import com.marcogn.kartlog.data.local.dao.CharacterProgress
import com.marcogn.kartlog.data.local.entity.CharacterUnlockEntity
import com.marcogn.kartlog.data.seed.SeedAssetLoader
import com.marcogn.kartlog.data.seed.SeedRepository
import com.marcogn.kartlog.data.seed.ExpectedCounts
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * La regola dei colori e dei tap (grigio = ti manca, colorato = ce l'hai) su TUTTI i piloti del seed
 * reale, nei quattro gruppi: di base con/senza outfit, da sbloccare con/senza outfit. Nessun nome di
 * pilota nel test: i gruppi si ricavano dal seed.
 */
@RunWith(RobolectricTestRunner::class)
class CharacterCardBehaviorTest {

    private lateinit var db: KartLogDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KartLogDatabase::class.java).allowMainThreadQueries().build()
        val repository = SeedRepository(SeedAssetLoader(context), db.seedDao(), db.seedMetaDao(), db.userStateDao())
        runBlocking { repository.reseedIfNeeded() }
    }

    @After
    fun tearDown() = db.close()

    private suspend fun characters(): List<CharacterProgress> = db.skinDao().charactersWithProgress().first()

    @Test
    fun `ogni pilota segue la regola prima e dopo lo sblocco`() = runBlocking {
        val initial = characters()
        assertEquals(ExpectedCounts["drivers"], initial.size)
        assertEquals(ExpectedCounts["starter_drivers"], initial.count { !it.isUnlockable })
        // I quattro gruppi esistono tutti nel seed: il test li copre davvero.
        for (withOutfits in listOf(true, false)) for (unlockable in listOf(true, false)) {
            assertTrue(initial.any { it.hasOutfits == withOutfits && it.isUnlockable == unlockable })
        }

        // Stato iniziale: di base colorati, da sbloccare grigi col lucchetto e il popup.
        for (c in initial) {
            val b = cardBehavior(c)
            if (c.isUnlockable) {
                assertEquals(c.id, CardBehavior(dimmed = true, showsLock = true, tap = CardTap.UNLOCK_POPUP), b)
                assertTrue("${c.id}: criterio mancante per il popup", !c.unlockCriteria.isNullOrBlank())
            } else {
                val tap = if (c.hasOutfits) CardTap.OUTFITS else CardTap.NONE
                assertEquals(c.id, CardBehavior(dimmed = false, showsLock = false, tap = tap), b)
            }
        }

        // Tutti sbloccati: tutti colorati e senza lucchetto; chi ha outfit apre la pagina degli
        // outfit, chi è da sbloccare senza outfit riapre il popup (per ribloccarlo).
        initial.filter { it.isUnlockable }.forEach {
            db.userStateDao().setCharacterUnlock(CharacterUnlockEntity(characterId = it.id, unlocked = true))
        }
        for (c in characters()) {
            val tap = when {
                c.hasOutfits -> CardTap.OUTFITS
                c.isUnlockable -> CardTap.UNLOCK_POPUP
                else -> CardTap.NONE
            }
            assertEquals(c.id, CardBehavior(dimmed = false, showsLock = false, tap = tap), cardBehavior(c))
        }

        // Una scelta "bloccato" salvata su un pilota di base non conta: resta colorato.
        val starter = initial.first { !it.isUnlockable }
        db.userStateDao().setCharacterUnlock(CharacterUnlockEntity(characterId = starter.id, unlocked = false))
        val after = characters().first { it.id == starter.id }
        assertEquals(false, cardBehavior(after).dimmed)
    }
}
