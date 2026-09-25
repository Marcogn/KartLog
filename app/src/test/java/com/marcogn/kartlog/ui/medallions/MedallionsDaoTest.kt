package com.marcogn.kartlog.ui.medallions

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

/** SPEC §2.4 (Monete Peach): raggruppamento per regione e "segna tutti". */
@RunWith(RobolectricTestRunner::class)
class MedallionsDaoTest {

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
    fun `tutti i 200 medaglioni sono presenti in 10 regioni, nessuno raccolto`() = runBlocking {
        val all = db.medallionsDao().allMedallions().first()

        assertEquals(200, all.size)
        assertEquals(10, all.map { it.regionId }.distinct().size)
        assertTrue(all.none { it.collected })
    }

    @Test
    fun `segna tutti marca completa solo la regione indicata`() = runBlocking {
        val all = db.medallionsDao().allMedallions().first()
        val regionId = all.first().regionId
        val totalInRegion = all.count { it.regionId == regionId }

        db.userStateDao().markAllMedallionsCollected(regionId)

        val updated = db.medallionsDao().allMedallions().first()
        assertEquals(totalInRegion, updated.count { it.regionId == regionId && it.collected })
        assertTrue("le altre regioni non devono essere toccate", updated.none { it.regionId != regionId && it.collected })
    }
}
