package com.marcogn.kartlog.ui.common

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Il menu a tendina misura le voci con le misure intrinseche: un testo che si rimpicciolisce con
 * BoxWithConstraints lì dentro fa crashare l'app (bug visto dall'autore su "Ordina per").
 */
@RunWith(AndroidJUnit4::class)
class KartDropdownTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun openingTheDropdownShowsEveryOptionAndSelects() {
        var selected = "Uno"
        compose.setContent {
            KartDropdown(
                options = listOf("Uno", "Due molto lungo da stringere", "Tre"),
                selected = selected,
                label = { it },
                onSelected = { selected = it },
            )
        }
        compose.onNodeWithText("Uno").performClick()
        compose.onAllNodesWithText("Tre")[0].performClick()
        assertEquals("Tre", selected)
    }
}
