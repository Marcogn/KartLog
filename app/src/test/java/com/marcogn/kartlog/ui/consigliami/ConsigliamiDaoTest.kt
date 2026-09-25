package com.marcogn.kartlog.ui.consigliami

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.kartlog.data.local.KartLogDatabase
import com.marcogn.kartlog.data.local.dao.ConsigliamiDao
import com.marcogn.kartlog.data.local.entity.OwnedOutfitEntity
import com.marcogn.kartlog.data.local.entity.RaceResultEntity
import com.marcogn.kartlog.data.seed.SeedAssetLoader
import com.marcogn.kartlog.data.seed.SeedRepository
import com.marcogn.kartlog.domain.consigliami.ConsigliamiCharacter
import com.marcogn.kartlog.domain.consigliami.ConsigliamiEvent
import com.marcogn.kartlog.domain.consigliami.ConsigliamiFoodCourse
import com.marcogn.kartlog.domain.consigliami.ConsigliamiOutfit
import com.marcogn.kartlog.domain.consigliami.ConsigliamiRule
import com.marcogn.kartlog.domain.consigliami.ConsigliamiUseCase
import com.marcogn.kartlog.domain.consigliami.RecommendationGroup
import com.marcogn.kartlog.domain.model.Cc
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

    private suspend fun computeGroups(
        dao: ConsigliamiDao,
        includeNearby: Boolean = false,
        resultsEnabled: Boolean = false,
        weight: Double = 0.3,
        referenceCc: Cc = Cc.CC_150,
    ): List<RecommendationGroup> {
        val characters = dao.characters().first().map { ConsigliamiCharacter(it.id, it.rosterOrder, it.unlocked) }
        val outfits = dao.outfits().first().map { ConsigliamiOutfit(it.id, it.characterId, it.owned) }
        val rules = dao.rules().first().map { ConsigliamiRule(it.outfitId, it.foodGroupId) }
        val foodCourses = dao.foodCourses().first().map { ConsigliamiFoodCourse(it.foodGroupId, it.courseId, it.presence) }
        val stopsByEvent = dao.eventStops().first().groupBy({ it.eventId }, { it.courseId })
        val events = dao.events().first().map { ConsigliamiEvent(it.id, it.type, it.name, it.order, stopsByEvent[it.id].orEmpty()) }
        val bestStarsByEvent = db.userStateDao().allRaceResults().first()
            .filter { it.cc == referenceCc }
            .groupBy { it.eventId }
            .mapValues { (_, rows) -> rows.maxOf { it.stars } }
        return ConsigliamiUseCase.compute(
            characters, outfits, rules, foodCourses, events, includeNearby,
            resultsEnabled = resultsEnabled, weight = weight,
            bestStarsForEvent = { eventId -> bestStarsByEvent[eventId] },
        )
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

    @Test
    fun `sul seed reale con w=1 l'ordinamento dipende solo da improvement (fase 7)`() = runBlocking {
        val dao = db.consigliamiDao()
        val disabled = computeGroups(dao).flatMap { it.events }
        val highGainEvent = disabled.maxBy { it.score }
        // Un evento con gain minore del migliore, per verificare che con w=1 il gain smetta di contare.
        val lowGainEvent = disabled.filter { it.event.id != highGainEvent.event.id && it.score < highGainEvent.score }
            .maxBy { it.score }

        // Tre stelle sull'evento col gain più alto -> improvement 0. Nessun risultato sull'altro -> improvement 1.
        db.userStateDao().insertRaceResult(
            RaceResultEntity(
                eventId = highGainEvent.event.id,
                cc = Cc.CC_150,
                stars = 3,
                placement = 1,
                eliminatedAt = null,
                characterId = null,
                timestamp = 0L,
            )
        )

        val weighted = computeGroups(dao, resultsEnabled = true, weight = 1.0, referenceCc = Cc.CC_150).flatMap { it.events }
        val positionOf = { eventId: String -> weighted.indexOfFirst { it.event.id == eventId } }

        assertTrue(
            "con w=1 l'evento senza risultati (improvement 1) deve precedere quello con 3 stelle (improvement 0), " +
                "anche se il suo gain è più basso",
            positionOf(lowGainEvent.event.id) < positionOf(highGainEvent.event.id),
        )
    }

    @Test
    fun `un evento a punteggio 0, nascosto da 'Solo utili', resta registrabile (SPEC §2_6)`() = runBlocking {
        val dao = db.consigliamiDao()
        // Tutti gli outfit posseduti -> gain 0 su ogni evento, esattamente quello che "Solo utili"
        // (SPEC §6.3) nasconderebbe dalla lista. Il form di registrazione (RaceResultBottomSheet)
        // elenca però sempre TUTTI gli eventi di dao.events(), non solo quelli con score > 0: è il
        // fix per il "buco di analisi" segnalato dall'autore dopo la 0.1.1 (CLAUDE.md, "Aperto").
        dao.outfits().first().forEach { db.userStateDao().markOutfitOwned(OwnedOutfitEntity(it.id)) }
        val zeroScoreEvent = computeGroups(dao).flatMap { it.events }.first()
        assertTrue("con tutti gli outfit posseduti ogni evento deve avere score 0", zeroScoreEvent.score == 0.0)

        val allEventIds = dao.events().first().map { it.id }.toSet()
        assertTrue("l'evento a score 0 deve comunque comparire in dao.events()", zeroScoreEvent.event.id in allEventIds)

        db.userStateDao().insertRaceResult(
            RaceResultEntity(
                eventId = zeroScoreEvent.event.id,
                cc = Cc.CC_150,
                stars = 2,
                placement = 3,
                eliminatedAt = null,
                characterId = null,
                timestamp = 0L,
            )
        )

        val history = db.userStateDao().raceResultsForEvent(zeroScoreEvent.event.id).first()
        assertTrue("il risultato deve essere registrabile anche per un evento a punteggio 0", history.isNotEmpty())
    }
}
