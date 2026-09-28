package com.marcogn.kartlog.ui.map

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.kartlog.data.local.KartLogDatabase
import com.marcogn.kartlog.data.local.entity.ActivatedQuestionPanelEntity
import com.marcogn.kartlog.data.local.entity.CompletedPSwitchEntity
import com.marcogn.kartlog.data.seed.ExpectedCounts
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

/** Mappa dei collezionabili: tutti i punti del seed reale, con lo stato condiviso con le liste. */
@RunWith(RobolectricTestRunner::class)
class MapDaoTest {

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
    fun `la mappa ha tutte le monete, i pulsanti P e i pannelli`() = runBlocking {
        val byType = db.mapDao().allPoints().first().groupBy { it.type }.mapValues { it.value.size }

        assertEquals(ExpectedCounts["peach_medallions"], byType["MEDALLION"])
        assertEquals(ExpectedCounts["p_switches"], byType["P_SWITCH"])
        assertEquals(ExpectedCounts["question_panels"], byType["QUESTION_PANEL"])
    }

    @Test
    fun `un pulsante P segnato nella lista risulta fatto sulla mappa e viceversa per i pannelli`() = runBlocking {
        val pSwitch = db.pSwitchesDao().allPSwitches().first().first()
        db.userStateDao().markPSwitchCompleted(CompletedPSwitchEntity(pSwitch.pSwitchId))
        val panel = db.mapDao().allPoints().first().first { it.type == "QUESTION_PANEL" }
        db.userStateDao().markQuestionPanelActivated(ActivatedQuestionPanelEntity(panel.id))

        val points = db.mapDao().allPoints().first()
        assertEquals(setOf(pSwitch.pSwitchId, panel.id), points.filter { it.done }.map { it.id }.toSet())
        val onMap = points.single { it.id == pSwitch.pSwitchId }
        assertEquals(pSwitch.name, onMap.name)
        assertTrue("il pulsante P ha il suo luogo", !onMap.locationName.isNullOrBlank())
    }
}
