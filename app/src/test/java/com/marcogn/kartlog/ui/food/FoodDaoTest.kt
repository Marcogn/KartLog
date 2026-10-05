package com.marcogn.kartlog.ui.food

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.kartlog.data.local.KartLogDatabase
import com.marcogn.kartlog.data.local.dao.SeedContent
import com.marcogn.kartlog.data.local.entity.CharacterEntity
import com.marcogn.kartlog.data.local.entity.CourseEntity
import com.marcogn.kartlog.data.local.entity.FoodGroupEntity
import com.marcogn.kartlog.data.local.entity.FoodVariantEntity
import com.marcogn.kartlog.data.local.entity.OutfitEntity
import com.marcogn.kartlog.data.local.entity.OutfitFoodRuleEntity
import com.marcogn.kartlog.data.local.entity.OwnedOutfitEntity
import com.marcogn.kartlog.data.local.entity.RegionEntity
import com.marcogn.kartlog.data.local.entity.YoshiStandEntity
import com.marcogn.kartlog.data.local.entity.YoshiStandFoodEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * API della sezione Cibi (fase C2, preparata in C1) su un seed FAKE_FOR_TESTS: nomi e id inventati,
 * nessun dato di gioco.
 */
@RunWith(RobolectricTestRunner::class)
class FoodDaoTest {

    private lateinit var db: KartLogDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KartLogDatabase::class.java).allowMainThreadQueries().build()
        runBlocking { db.seedDao().replaceAll(FAKE_FOR_TESTS) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `la griglia ha un gruppo per tessera con prima immagine e outfit mancanti`() = runBlocking {
        db.userStateDao().markOutfitOwned(OwnedOutfitEntity("hero__red"))

        val groups = db.foodDao().foodGroups().first()

        assertEquals(listOf("fake_soup", "fake_bread"), groups.map { it.id })  // ordine del seed
        val soup = groups.first()
        assertEquals("https://example.invalid/soup_1.png", soup.imageUrl)
        assertEquals(2, soup.totalOutfits)
        assertEquals(1, soup.missingOutfits)
        assertEquals(0, groups.last().totalOutfits)
    }

    @Test
    fun `le varianti seguono l'ordine della tabella e portano i livelli di boost`() = runBlocking {
        val variants = db.foodDao().variants("fake_soup").first()

        assertEquals(listOf("fake_soup_1", "fake_soup_2"), variants.map { it.id })
        assertEquals(listOf("SMALL", "MEDIUM", "LARGE"), variants.last().boost)
    }

    @Test
    fun `gli stand di un cibo arrivano con percorso, bioma e luogo, le strade senza`() = runBlocking {
        val stands = db.foodDao().stands("fake_soup").first()

        assertEquals(listOf("stand_course", "stand_route"), stands.map { it.standId })
        val onCourse = stands.first()
        assertEquals("Fake Course", onCourse.courseName)
        assertEquals("Percorso finto", onCourse.courseNameIt)
        assertEquals("Fake Region", onCourse.regionName)
        assertEquals("Vicino al traguardo finto", onCourse.locationIt)
        assertEquals("Fake soup bowl", onCourse.food)
        val onRoute = stands.last()
        assertEquals(null, onRoute.courseId)
        assertEquals(null, onRoute.regionId)
        assertEquals(null, onRoute.food)
        assertEquals("Fake road", onRoute.location)
    }

    @Test
    fun `gli outfit di un cibo sono per personaggio con lo stato posseduto`() = runBlocking {
        db.userStateDao().markOutfitOwned(OwnedOutfitEntity("hero__red"))

        val outfits = db.foodDao().outfits("fake_soup").first()

        assertEquals(listOf("hero__red" to true, "sidekick__blue" to false), outfits.map { it.outfitId to it.owned })
        assertEquals("Hero", outfits.first().characterName)
    }

    @Test
    fun `i personaggi con outfit sono quelli che ne hanno oltre al default`() = runBlocking {
        val characters = db.foodDao().charactersWithOutfits().first()

        assertEquals(listOf("hero", "sidekick"), characters.map { it.id })
    }

    private companion object {
        /** Seed inventato per i test: FAKE_FOR_TESTS, nessun dato di gioco. */
        val FAKE_FOR_TESTS = SeedContent(
            characters = listOf(
                CharacterEntity("hero", "Hero", "Eroe", rosterOrder = 0),
                CharacterEntity("sidekick", "Sidekick", null, rosterOrder = 1),
            ),
            outfits = listOf(
                OutfitEntity("hero__default", "hero", null, null, isDefault = true),
                OutfitEntity("hero__red", "hero", "Red", "Rosso", isDefault = false),
                OutfitEntity("sidekick__blue", "sidekick", "Blue", null, isDefault = false),
            ),
            foodGroups = listOf(
                FoodGroupEntity("fake_soup", "Fake soup", listOf("Fake soup"), revertsToDefault = false),
                FoodGroupEntity("fake_bread", "Fake bread", listOf("Fake bread"), revertsToDefault = true),
            ),
            outfitFoodRules = listOf(
                OutfitFoodRuleEntity("hero__red", "fake_soup"),
                OutfitFoodRuleEntity("sidekick__blue", "fake_soup"),
            ),
            foodVariants = listOf(
                FoodVariantEntity("fake_soup_1", "fake_soup", 1, "Fake soup", "Zuppa finta", listOf("SMALL"), "https://example.invalid/soup_1.png"),
                FoodVariantEntity("fake_soup_2", "fake_soup", 2, "Fake soup bowl", null, listOf("SMALL", "MEDIUM", "LARGE"), null),
                FoodVariantEntity("fake_bread_1", "fake_bread", 1, "Fake bread", null, listOf("MEDIUM"), null),
            ),
            yoshiStands = listOf(
                YoshiStandEntity("stand_course", 0, "fake_course", "fake_region", "Fake bar", "Bar finto", "Near the fake finish", "Vicino al traguardo finto"),
                YoshiStandEntity("stand_route", 1, null, null, null, null, "Fake road", "Strada finta"),
            ),
            yoshiStandFoods = listOf(
                YoshiStandFoodEntity("stand_course", "fake_soup", "Fake soup bowl"),
                YoshiStandFoodEntity("stand_route", "fake_soup", null),
                YoshiStandFoodEntity("stand_route", "fake_bread", null),
            ),
            courses = listOf(CourseEntity("fake_course", "Fake Course", "Percorso finto", "fake_region")),
            regions = listOf(RegionEntity("fake_region", "Fake Region", 0, "Regione finta")),
            areas = emptyList(),
            peachMedallions = emptyList(),
            pSwitches = emptyList(),
            events = emptyList(),
            eventStops = emptyList(),
        )
    }
}
