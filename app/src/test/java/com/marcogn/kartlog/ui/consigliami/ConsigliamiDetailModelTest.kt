package com.marcogn.kartlog.ui.consigliami

import com.marcogn.kartlog.domain.consigliami.CharacterDetail
import com.marcogn.kartlog.domain.consigliami.RelevantFood
import com.marcogn.kartlog.domain.consigliami.UnlockableOutfit
import org.junit.Assert.assertEquals
import org.junit.Test

/** Raggruppamento delle righe "outfit – cibo · percorsi" con dati inventati (FAKE_FOR_TESTS). */
class ConsigliamiDetailModelTest {

    private val details = listOf(
        CharacterDetail(
            characterId = "fake_char",
            gain = 1,
            outfits = listOf(
                UnlockableOutfit(
                    "fake_outfit",
                    listOf(
                        RelevantFood("fake_soup", "fake_course_b"),
                        RelevantFood("fake_bread", "fake_course_a"),
                        RelevantFood("fake_soup", "fake_course_c"),
                        RelevantFood("fake_soup", "fake_course_b"),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun `lo stesso cibo su più percorsi sta in una riga, nell'ordine di tappa`() {
        val ui = detailCharactersUi(
            details = details,
            characterNames = mapOf("fake_char" to "Pilota"),
            characterImages = emptyMap(),
            outfitNames = mapOf("fake_outfit" to "Vestito"),
            foodGroupNames = mapOf("fake_soup" to "Zuppa", "fake_bread" to "Pane"),
            foodGroupImages = mapOf("fake_soup" to "https://example.invalid/zuppa.png"),
            courseNames = mapOf("fake_course_a" to "A", "fake_course_b" to "B", "fake_course_c" to "C"),
        )
        val outfit = ui.single().outfits.single()
        assertEquals("Vestito", outfit.name)
        assertEquals(listOf("Zuppa", "Pane"), outfit.foods.map { it.name })
        assertEquals(listOf("B", "C"), outfit.foods[0].courseNames)
        assertEquals("https://example.invalid/zuppa.png", outfit.foods[0].imageUrl)
        assertEquals(listOf("A"), outfit.foods[1].courseNames)
    }

    @Test
    fun `senza nomi restano gli id`() {
        val ui = detailCharactersUi(details, emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
        assertEquals("fake_char", ui.single().name)
        assertEquals("fake_soup", ui.single().outfits.single().foods.first().name)
    }
}
