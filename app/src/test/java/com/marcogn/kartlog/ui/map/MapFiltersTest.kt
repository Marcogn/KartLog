package com.marcogn.kartlog.ui.map

import android.content.Context
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marcogn.kartlog.R
import com.marcogn.kartlog.domain.model.MapPointType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * I filtri della mappa hanno le etichette visibili: in una riga scorrevole (larghezza infinita) il
 * testo dei pulsantoni finiva a larghezza zero e i pulsanti apparivano vuoti (visto dall'autore).
 */
@RunWith(AndroidJUnit4::class)
class MapFiltersTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun everyFilterShowsItsLabelAndToggles() {
        var toggled: MapPointType? = null
        var doneToggled = false
        val state = MapUiState(
            counters = mapOf(
                MapPointType.MEDALLION to MapCounter(3, 200),
                MapPointType.P_SWITCH to MapCounter(0, 394),
                MapPointType.QUESTION_PANEL to MapCounter(1, 150),
            ),
        )
        compose.setContent { MapFilters(state, onTypeToggled = { toggled = it }, onShowDoneToggled = { doneToggled = true }) }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val pSwitches = context.getString(R.string.map_filter_pswitches) + " 0/394"
        val showDone = context.getString(R.string.map_filter_show_done)
        listOf(context.getString(R.string.map_filter_medallions) + " 3/200", pSwitches,
            context.getString(R.string.map_filter_panels) + " 1/150", showDone).forEach {
            compose.onNodeWithText(it).assertWidthIsAtLeast(30.dp)
        }
        compose.onNodeWithText(pSwitches).performClick()
        assertEquals(MapPointType.P_SWITCH, toggled)
        compose.onNodeWithText(showDone).performClick()
        assertEquals(true, doneToggled)
    }
}
