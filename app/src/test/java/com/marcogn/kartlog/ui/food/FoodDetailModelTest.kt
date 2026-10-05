package com.marcogn.kartlog.ui.food

import com.marcogn.kartlog.data.local.dao.FoodCharacterRow
import com.marcogn.kartlog.data.local.dao.FoodOutfitRow
import com.marcogn.kartlog.data.local.dao.FoodStandRow
import com.marcogn.kartlog.data.local.entity.FoodVariantEntity
import java.util.Locale
import com.marcogn.kartlog.domain.model.AppLocale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Raggruppamento di luoghi e personaggi della sezione Cibi, su dati inventati (FAKE_FOR_TESTS). */
class FoodDetailModelTest {

    @Before
    fun english() = AppLocale.update(Locale.ENGLISH)

    @After
    fun reset() = AppLocale.update(Locale.getDefault())

    private fun stand(
        id: String, course: String?, region: String?, location: String, food: String? = null, establishment: String? = null,
    ) = FoodStandRow(
        standId = id, courseId = course, courseName = course?.let { "Course $it" }, courseNameIt = null,
        regionId = region, regionName = region?.let { "Region $it" }, regionNameIt = null, regionOrder = null,
        establishment = establishment, establishmentIt = null, location = location, locationIt = null, food = food,
    )

    private fun variant(name: String, order: Int) = FoodVariantEntity("v$order", "group", order, name, null, listOf("SMALL"), null)

    @Test
    fun `senza nome di variante c'è una sola sezione, con percorsi raggruppati e strade a parte`() {
        val sections = buildStandSections(
            listOf(
                stand("a", "c1", "r1", "North gate", establishment = "Fake bar"),
                stand("b", null, null, "Fake road"),
                stand("c", "c1", "r1", "South gate"),
                stand("d", null, "r2", "Fake bridge"),
            ),
            listOf(variant("Fake soup", 1)),
        )

        assertEquals(1, sections.size)
        val section = sections.single()
        assertNull(section.variantName)
        assertEquals(listOf("Region r1 › Course c1"), section.courses.map { it.title })
        assertEquals(listOf("Fake bar – North gate", "South gate"), section.courses.single().places)
        assertEquals(listOf("Fake road", "Region r2: Fake bridge"), section.roads)
    }

    @Test
    fun `quando la fonte nomina la variante i luoghi si dividono per variante nell'ordine della tabella`() {
        val sections = buildStandSections(
            listOf(
                stand("a", "c1", "r1", "Pier", food = "Fake roll"),
                stand("b", "c2", "r1", "Dock", food = "Fake fish"),
                stand("c", "c2", "r1", "Quay", food = "Fake fish"),
                stand("d", null, null, "Fake road"),
            ),
            listOf(variant("Fake fish", 1), variant("Fake roll", 2)),
        )

        assertEquals(listOf(null, "Fake fish", "Fake roll"), sections.map { it.variantName })
        assertEquals(listOf("Fake road"), sections.first().roads)
        assertEquals(listOf("Dock", "Quay"), sections[1].courses.single().places)
    }

    @Test
    fun `nessuno stand, nessuna sezione`() {
        assertEquals(emptyList<FoodStandSection>(), buildStandSections(emptyList(), emptyList()))
    }

    @Test
    fun `prima i personaggi a cui manca l'outfit, poi quelli che ce l'hanno, a parità ordine di roster`() {
        val characters = listOf("a", "b", "c").map { FoodCharacterRow(it, it.uppercase(), null, null) }
        fun outfit(ch: String, owned: Boolean) =
            FoodOutfitRow("${ch}__x", "X", null, null, ch, ch, null, null, 0, owned)

        val (withFood, _) = buildCharacterOutfits(
            listOf(outfit("a", true), outfit("b", false), outfit("c", false)),
            characters,
        )

        assertEquals(listOf("b", "c", "a"), withFood.map { it.characterId })
    }

    @Test
    fun `i personaggi senza outfit da questo cibo vanno in fondo, in ordine di roster`() {
        val characters = listOf(
            FoodCharacterRow("hero", "Hero", "Eroe", null),
            FoodCharacterRow("sidekick", "Sidekick", null, null),
            FoodCharacterRow("rival", "Rival", null, null),
        )
        val outfits = listOf(
            FoodOutfitRow("rival__red", "Red", null, null, "rival", "Rival", null, null, 2, owned = true),
        )

        val (withFood, without) = buildCharacterOutfits(outfits, characters)

        assertEquals(listOf("rival"), withFood.map { it.characterId })
        assertEquals(listOf("Hero", "Sidekick"), without)
    }
}
