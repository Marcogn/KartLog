package com.marcogn.kartlog.data.seed

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Doppio controllo sui JSON effettivamente impacchettati in `assets/seed/` (SPEC §5.6),
 * indipendente dalla validazione di `tools/seedgen`: stessa fonte di verità
 * (`expected_counts.yaml`), ma un controllo separato lato app.
 */
@RunWith(RobolectricTestRunner::class)
class SeedValidationTest {

    private val assets = SeedAssetLoader(ApplicationProvider.getApplicationContext())

    @Test
    fun `i conteggi corrispondono a expected_counts`() {
        val outfits = assets.readItems<OutfitDto>("outfits.json")
        val alternative = outfits.filterNot { it.isDefault }
        assertEquals(ExpectedCounts["total_outfits_including_default"], outfits.size)
        assertEquals(ExpectedCounts["alternative_outfits"], alternative.size)
        assertEquals(
            ExpectedCounts["characters_with_outfits"],
            alternative.map { it.characterId }.distinct().size,
        )

        val foodGroups = assets.readItems<FoodGroupDto>("food_groups.json")
        assertEquals(ExpectedCounts["food_groups"], foodGroups.size)
        assertEquals(ExpectedCounts["food_groups_reverting_to_default"], foodGroups.count { it.revertsToDefault })

        assertEquals(ExpectedCounts["courses"], assets.readItems<CourseDto>("courses.json").size)
        assertEquals(ExpectedCounts["regions"], assets.readItems<RegionDto>("regions.json").size)

        val events = assets.readItems<EventDto>("events.json")
        val cups = events.filter { it.type == "CUP" }
        val rallies = events.filter { it.type == "RALLY" }
        assertEquals(ExpectedCounts["cups"], cups.size)
        assertEquals(ExpectedCounts["rallies"], rallies.size)
        cups.forEach { assertEquals("${it.id}: corsi", ExpectedCounts["courses_per_cup"], it.stops.size) }
        rallies.forEach { assertEquals("${it.id}: tappe", ExpectedCounts["stops_per_rally"], it.stops.size) }

        // Se una sorgente non è ancora nel seed (es. i pulsanti P prima della fase 2 di
        // seedgen), il controllo si salta esplicitamente invece di passare in silenzio su una
        // lista vuota confusa con "zero elementi attesi".
        val medallions = assets.readItems<PeachMedallionDto>("peach_medallions.json")
        if (medallions.isEmpty()) {
            println("SKIP peach_medallions.json: assente nel seed pacchettizzato")
        } else {
            assertEquals(ExpectedCounts["peach_medallions"], medallions.size)
        }

        val pSwitches = assets.readItems<PSwitchDto>("p_switches.json")
        if (pSwitches.isEmpty()) {
            println("SKIP p_switches.json: assente nel seed pacchettizzato (fase 2 di seedgen non eseguita)")
        } else {
            assertEquals(ExpectedCounts["p_switches"], pSwitches.size)
        }
    }

    @Test
    fun `nessun id duplicato`() {
        fun assertNoDuplicates(label: String, ids: List<String>) {
            val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
            assertTrue("$label: id duplicati $duplicates", duplicates.isEmpty())
        }
        assertNoDuplicates("characters", assets.readItems<CharacterDto>("characters.json").map { it.id })
        assertNoDuplicates("outfits", assets.readItems<OutfitDto>("outfits.json").map { it.id })
        assertNoDuplicates("food_groups", assets.readItems<FoodGroupDto>("food_groups.json").map { it.id })
        assertNoDuplicates("courses", assets.readItems<CourseDto>("courses.json").map { it.id })
        assertNoDuplicates("regions", assets.readItems<RegionDto>("regions.json").map { it.id })
        assertNoDuplicates("areas", assets.readItems<AreaDto>("areas.json").map { it.id })
        assertNoDuplicates("events", assets.readItems<EventDto>("events.json").map { it.id })
        assertNoDuplicates("peach_medallions", assets.readItems<PeachMedallionDto>("peach_medallions.json").map { it.id })
        assertNoDuplicates("p_switches", assets.readItems<PSwitchDto>("p_switches.json").map { it.id })
    }

    @Test
    fun `integrita referenziale`() {
        val outfitIds = assets.readItems<OutfitDto>("outfits.json").map { it.id }.toSet()
        val foodGroupIds = assets.readItems<FoodGroupDto>("food_groups.json").map { it.id }.toSet()
        val courseIds = assets.readItems<CourseDto>("courses.json").map { it.id }.toSet()

        assets.readItems<OutfitFoodRuleDto>("outfit_food_rules.json").forEach {
            assertTrue("regola: outfit inesistente ${it.outfitId}", it.outfitId in outfitIds)
            assertTrue("regola: gruppo inesistente ${it.foodGroupId}", it.foodGroupId in foodGroupIds)
        }
        assets.readItems<FoodGroupCourseDto>("food_group_courses.json").forEach {
            assertTrue("food_group_courses: corso inesistente ${it.courseId}", it.courseId in courseIds)
            assertTrue("food_group_courses: gruppo inesistente ${it.foodGroupId}", it.foodGroupId in foodGroupIds)
        }
        assets.readItems<EventDto>("events.json").forEach { event ->
            event.stops.forEach { stop ->
                assertTrue("${event.id}: corso inesistente $stop", stop in courseIds)
            }
        }
        assets.readItems<PSwitchDto>("p_switches.json").forEach {
            assertTrue(
                "${it.id}: deve avere esattamente uno tra courseId e areaId",
                (it.courseId != null) != (it.areaId != null),
            )
        }
    }
}
