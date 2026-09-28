package com.marcogn.kartlog.ui.medallions

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.kartlog.data.local.KartLogDatabase
import com.marcogn.kartlog.data.local.entity.CollectedMedallionEntity
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

/** Monete Peach: le 200 monete di mkworld-checklist, una per una. */
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
    fun `tutte le 200 monete sono presenti in ordine, nessuna raccolta`() = runBlocking {
        val all = db.medallionsDao().allMedallions().first()

        assertEquals(200, all.size)
        assertEquals((1..200).toList(), all.map { it.index })
        assertTrue(all.none { it.collected })
    }

    @Test
    fun `segnare e togliere una moneta cambia solo quella`() = runBlocking {
        val (first, second) = db.medallionsDao().allMedallions().first()

        db.userStateDao().markMedallionCollected(CollectedMedallionEntity(second.medallionId))
        val after = db.medallionsDao().allMedallions().first()
        assertEquals(listOf(second.medallionId), after.filter { it.collected }.map { it.medallionId })
        assertEquals(1, db.userStateDao().countCollectedMedallions().first())

        db.userStateDao().markMedallionNotCollected(second.medallionId)
        assertTrue(db.medallionsDao().allMedallions().first().none { it.collected })
        assertTrue(first.medallionId != second.medallionId)
    }
}
