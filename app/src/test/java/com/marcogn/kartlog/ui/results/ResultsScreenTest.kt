package com.marcogn.kartlog.ui.results

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marcogn.kartlog.R
import com.marcogn.kartlog.domain.model.AppLocale
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.domain.model.TrophyRank
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Risultati con la grafica del gioco, dati inventati (FAKE_FOR_TESTS), senza rete. */
@RunWith(AndroidJUnit4::class)
class ResultsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun italian() = AppLocale.update(Locale.ITALIAN)

    private fun str(@StringRes id: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id)

    private val state = ResultsUiState(
        cc = Cc.CC_150,
        cups = listOf(
            ResultRow("cup_a", "Coppa A FAKE_FOR_TESTS", TrophyRank.GOLD),
            ResultRow("cup_b", "Coppa B FAKE_FOR_TESTS", null),
        ),
        rallies = listOf(ResultRow("rally_a", "Rally A FAKE_FOR_TESTS", null)),
    )

    private fun show(onRank: (String, TrophyRank?) -> Unit = { _, _ -> }) {
        compose.setContent {
            ResultsContent(state = state, onMenuClick = {}, onCcChanged = {}, onRankChanged = onRank)
        }
    }

    @Test
    fun `le righe mostrano evento e trofeo, le sezioni hanno il titolo`() {
        show()
        compose.onNodeWithText(str(R.string.results_section_cup)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.results_section_rally)).assertIsDisplayed()
        compose.onNodeWithText("Coppa A FAKE_FOR_TESTS").assertIsDisplayed()
        compose.onNodeWithText("Coppa B FAKE_FOR_TESTS").assertIsDisplayed()
    }

    @Test
    fun `il popup salva il trofeo scelto`() {
        var saved: Pair<String, TrophyRank?>? = null
        show { id, rank -> saved = id to rank }
        compose.onNodeWithText("Coppa B FAKE_FOR_TESTS").performClick()
        compose.onNode(hasText(str(R.string.rank_silver)) and isSelectable()).performClick()
        assertEquals("cup_b" to TrophyRank.SILVER, saved)
        // Dopo la scelta il popup si chiude.
        compose.onNodeWithText(str(R.string.rank_silver)).assertDoesNotExist()
    }

    @Test
    fun `Nessun trofeo nel popup cancella il risultato`() {
        var saved: Pair<String, TrophyRank?>? = null
        show { id, rank -> saved = id to rank }
        compose.onNodeWithText("Coppa A FAKE_FOR_TESTS").performClick()
        // Con il popup aperto "Nessun trofeo" compare nel popup e nelle righe senza trofeo.
        compose.onNode(hasText(str(R.string.results_no_trophy)) and isSelectable()).performClick()
        assertEquals("cup_a" to null, saved)
    }
}
