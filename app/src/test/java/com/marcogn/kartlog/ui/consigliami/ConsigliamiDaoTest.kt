package com.marcogn.kartlog.ui.consigliami

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.kartlog.data.local.KartLogDatabase
import com.marcogn.kartlog.data.local.dao.ConsigliamiDao
import com.marcogn.kartlog.data.local.entity.OwnedOutfitEntity
import com.marcogn.kartlog.data.seed.SeedAssetLoader
import com.marcogn.kartlog.data.seed.SeedRepository
import com.marcogn.kartlog.domain.consigliami.ConsigliamiCharacter
import com.marcogn.kartlog.domain.consigliami.ConsigliamiEvent
import com.marcogn.kartlog.domain.consigliami.ConsigliamiFoodCourse
import com.marcogn.kartlog.domain.consigliami.ConsigliamiOutfit
import com.marcogn.kartlog.domain.consigliami.ConsigliamiRule
import com.marcogn.kartlog.domain.consigliami.ConsigliamiUseCase
import com.marcogn.kartlog.domain.consigliami.RecommendationGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Consigliami con i dati reali del seed, non fixture sintetiche: verifica che l'algoritmo produca
 * raccomandazioni sensate e che si aggiorni davvero quando cambia lo stato degli outfit (SPEC §9).
 */
@RunWith(RobolectricTestRunner::class)
class ConsigliamiDaoTest {

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

    private suspend fun computeGroups(dao: ConsigliamiDao, includeNearby: Boolean = false): List<RecommendationGroup> {
        val characters = dao.characters().first().map { ConsigliamiCharacter(it.id, it.rosterOrder, it.unlocked) }
        val outfits = dao.outfits().first().map { ConsigliamiOutfit(it.id, it.characterId, it.owned) }
        val rules = dao.rules().first().map { ConsigliamiRule(it.outfitId, it.foodGroupId) }
        val foodCourses = dao.foodCourses().first().map { ConsigliamiFoodCourse(it.foodGroupId, it.courseId, it.presence) }
        val stopsByEvent = dao.eventStops().first().groupBy({ it.eventId }, { it.courseId })
        val events = dao.events().first().map { ConsigliamiEvent(it.id, it.type, it.name, it.order, stopsByEvent[it.id].orEmpty()) }
        return ConsigliamiUseCase.compute(characters, outfits, rules, foodCourses, events, includeNearby)
    }

    @Test
    fun `sul seed reale a stato utente vuoto ci sono raccomandazioni utili`() = runBlocking {
        val groups = computeGroups(db.consigliamiDao())
        val top = groups.first().events.first()

        assertTrue("con nessun outfit posseduto ci deve essere almeno un evento utile", top.score > 0)
        assertTrue(top.best != null)
    }

    @Test
    fun `possedere l'outfit consigliato riduce subito il suo gain (SPEC §9)`() = runBlocking {
        val dao = db.consigliamiDao()
        val before = computeGroups(dao).first().events.first()
        val bestCharacterId = requireNotNull(before.best).characterId
        val gainBefore = before.best!!.gain

        val characters = dao.characters().first().map { ConsigliamiCharacter(it.id, it.rosterOrder, it.unlocked) }
        val outfits = dao.outfits().first().map { ConsigliamiOutfit(it.id, it.characterId, it.owned) }
        val rules = dao.rules().first().map { ConsigliamiRule(it.outfitId, it.foodGroupId) }
        val foodCourses = dao.foodCourses().first().map { ConsigliamiFoodCourse(it.foodGroupId, it.courseId, it.presence) }
        val detail = ConsigliamiUseCase.detailFor(before.event, characters, outfits, rules, foodCourses, includeNearby = false)
        val outfitToOwn = detail.first { it.characterId == bestCharacterId }.unlockableOutfitIds.first()

        db.userStateDao().markOutfitOwned(OwnedOutfitEntity(outfitToOwn))

        val after = computeGroups(dao).flatMap { it.events }.first { it.event.id == before.event.id }
        val gainAfter = after.best?.takeIf { it.characterId == bestCharacterId }?.gain
            ?: after.runnersUp.firstOrNull { it.characterId == bestCharacterId }?.gain
            ?: 0

        assertTrue("il gain di $bestCharacterId per ${before.event.id} deve calare dopo aver posseduto l'outfit", gainAfter < gainBefore)
    }
}
