package com.marcogn.kartlog.ui.skin

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marcogn.kartlog.R
import com.marcogn.kartlog.data.local.dao.OutfitProgress
import com.marcogn.kartlog.domain.model.AppLocale
import androidx.compose.ui.Modifier
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Il cibo di un outfit nel dettaglio personaggio è toccabile e non spunta l'outfit (dati FAKE_FOR_TESTS). */
// Schermo alto: con quello predefinito (320×470 px) il secondo cibo finirebbe fuori dalla finestra.
@Config(qualifiers = "w400dp-h900dp")
@RunWith(AndroidJUnit4::class)
class OutfitCardFoodTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun italian() = AppLocale.update(Locale.ITALIAN)

    private fun outfit(foods: String?, ids: String?, foodsIt: String? = foods) = OutfitProgress(
        outfitId = "fake__red", outfitName = "Red", outfitNameIt = "Rosso", isDefault = false, imageUrl = null,
        owned = false, foodGroups = foods, foodGroupsIt = foodsIt, foodGroupIds = ids,
    )

    @Test
    fun `foods divide nomi e ID e salta se non combaciano`() {
        val o = outfit("Zuppa FAKE · Pane FAKE", "fake_soup · fake_bread", "Zuppa IT · Pane IT")
        assertEquals(listOf("fake_soup", "fake_bread"), o.foods.map { it.foodGroupId })
        assertEquals(listOf("Zuppa IT", "Pane IT"), o.foods.map { it.name })
        assertTrue(outfit(null, null).foods.isEmpty())
        assertTrue(outfit("A · B", "a").foods.isEmpty())
    }

    @Test
    fun `il tocco sul cibo apre il cibo e non spunta l'outfit`() {
        var opened: String? = null
        var toggled = false
        compose.setContent {
            OutfitCard(
                outfit("Zuppa FAKE · Pane FAKE", "fake_soup · fake_bread"),
                accent = androidx.compose.ui.graphics.Color.Red,
                onToggle = { toggled = true },
                onFoodClick = { opened = it },
                modifier = Modifier,
            )
        }
        compose.onNodeWithText("Pane FAKE", useUnmergedTree = true).performClick()
        assertEquals("fake_bread", opened)
        assertEquals(false, toggled)
    }

    @Test
    fun `senza regole il cibo sconosciuto non è toccabile e il tocco sulla polaroid spunta`() {
        var opened: String? = null
        var toggled: Boolean? = null
        compose.setContent {
            OutfitCard(
                outfit(null, null),
                accent = androidx.compose.ui.graphics.Color.Red,
                onToggle = { toggled = it },
                onFoodClick = { opened = it },
                modifier = Modifier,
            )
        }
        val unknown = ApplicationProvider.getApplicationContext<Context>().getString(R.string.skin_unknown_food)
        compose.onNodeWithText(unknown).assertIsDisplayed().performClick()
        assertEquals(null, opened)
        assertEquals(true, toggled)
    }
}
