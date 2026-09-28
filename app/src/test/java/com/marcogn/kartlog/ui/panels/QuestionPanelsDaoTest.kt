package com.marcogn.kartlog.ui.panels

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.kartlog.data.local.KartLogDatabase
import com.marcogn.kartlog.data.local.entity.ActivatedQuestionPanelEntity
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

/** Pannelli "?": la lista uno per uno condivide lo stato con la mappa. */
@RunWith(RobolectricTestRunner::class)
class QuestionPanelsDaoTest {

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
    fun `tutti i pannelli in ordine, nessuno attivato`() = runBlocking {
        val all = db.questionPanelsDao().allPanels().first()
        assertEquals(ExpectedCounts["question_panels"], all.size)
        assertEquals((1..all.size).toList(), all.map { it.index })
        assertTrue(all.none { it.activated })
    }

    @Test
    fun `un pannello attivato dalla lista risulta fatto anche sulla mappa`() = runBlocking {
        val panel = db.questionPanelsDao().allPanels().first()[4]
        db.userStateDao().markQuestionPanelActivated(ActivatedQuestionPanelEntity(panel.panelId))

        assertEquals(listOf(panel.panelId), db.questionPanelsDao().allPanels().first().filter { it.activated }.map { it.panelId })
        assertTrue(db.mapDao().allPoints().first().single { it.id == panel.panelId }.done)
    }
}
