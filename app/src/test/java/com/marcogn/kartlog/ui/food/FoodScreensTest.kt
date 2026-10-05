package com.marcogn.kartlog.ui.food

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marcogn.kartlog.R
import com.marcogn.kartlog.data.local.dao.FoodOutfitRow
import com.marcogn.kartlog.domain.model.AppLocale
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Griglia e dettaglio dei cibi con dati inventati (FAKE_FOR_TESTS), senza rete: restano i segnaposto. */
@RunWith(AndroidJUnit4::class)
class FoodScreensTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun italian() = AppLocale.update(Locale.ITALIAN)

    // Robolectric gira con le risorse della lingua predefinita: i testi si leggono dalle risorse.
    private fun str(@StringRes id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    @Test
    fun `la griglia mostra nome e outfit mancanti e il tocco apre il cibo`() {
        var opened: String? = null
        compose.setContent {
            FoodGridContent(
                tiles = listOf(
                    FoodTile("fake_soup", "Zuppa FAKE_FOR_TESTS", null, missingOutfits = 2, totalOutfits = 3),
                    FoodTile("fake_bread", "Pane FAKE_FOR_TESTS", null, missingOutfits = 0, totalOutfits = 0),
                ),
                onMenuClick = {},
                onFoodClick = { opened = it },
            )
        }
        // La tessera cliccabile unisce i suoi testi in un solo nodo: si cerca per sottostringa.
        compose.onNodeWithText(ApplicationProvider.getApplicationContext<Context>().resources.getQuantityString(R.plurals.food_missing, 2, 2), substring = true).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.food_no_outfits), substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Zuppa FAKE_FOR_TESTS", substring = true).performClick()
        assertEquals("fake_soup", opened)
    }

    @Test
    fun `il dettaglio mostra varianti, luoghi e outfit per personaggio`() {
        val state = FoodDetailUiState(
            name = "Zuppa FAKE_FOR_TESTS",
            variants = listOf(FoodVariantUi("Zuppa grande", null, listOf("SMALL", "LARGE"))),
            sections = listOf(
                FoodStandSection(
                    variantName = null,
                    courses = listOf(FoodCourseStands("c1", "Regione finta › Percorso finto", listOf("Bar finto – Vicino al traguardo"))),
                    roads = listOf("Strada finta"),
                ),
            ),
            characters = listOf(
                FoodCharacterOutfits(
                    "hero", "Eroe FAKE", null,
                    listOf(FoodOutfitRow("hero__red", "Red", "Rosso", null, "hero", "Hero", null, null, 0, owned = false)),
                ),
            ),
            withoutOutfit = listOf("Spalla FAKE"),
        )
        compose.setContent { FoodDetailContent(state, onBack = {}) }

        compose.onNodeWithText(str(R.string.food_boost, str(R.string.food_boost_small) + " / " + str(R.string.food_boost_large))).assertIsDisplayed()
        compose.onNodeWithText("Regione finta › Percorso finto").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("• Bar finto – Vicino al traguardo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("• Strada finta").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Eroe FAKE").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Rosso").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.food_without_outfit, "Spalla FAKE")).performScrollTo().assertIsDisplayed()
    }
}
