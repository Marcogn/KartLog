package com.marcogn.kartlog.ui.pswitches

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.kartlog.data.local.KartLogDatabase
import com.marcogn.kartlog.data.seed.SeedAssetLoader
import com.marcogn.kartlog.data.seed.SeedRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** SPEC §2.4 (Pulsanti P): ogni missione ha un percorso/luogo, raggruppamento e "segna tutti". */
@RunWith(RobolectricTestRunner::class)
class PSwitchesDaoTest {

    private lateinit var db: KartLogDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KartLogDatabase::class.java).allowMainThreadQueries().build()
        val repository = SeedRepository(SeedAssetLoader(context), db.seedDao(), db.seedMetaDao(), db.userStateDao())
        runBlocking { repository.reseedIfNeeded() }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `tutti i 394 pulsanti P hanno un percorso o luogo`() = runBlocking {
        val all = db.pSwitchesDao().allPSwitches().first()

        assertEquals(394, all.size)
        all.forEach { assertTrue("${it.pSwitchId} senza percorso/luogo", it.locationName.isNotBlank()) }
        assertTrue(all.none { it.completed })
    }

    @Test
    fun `segna tutti marca completa solo la regione indicata`() = runBlocking {
        val all = db.pSwitchesDao().allPSwitches().first()
        val regionId = all.first().regionId
        val totalInRegion = all.count { it.regionId == regionId }

        db.userStateDao().markAllPSwitchesCompleted(regionId)

        val updated = db.pSwitchesDao().allPSwitches().first()
        assertEquals(totalInRegion, updated.count { it.regionId == regionId && it.completed })
        assertTrue("le altre regioni non devono essere toccate", updated.none { it.regionId != regionId && it.completed })
    }
}
