package com.marcogn.kartlog.ui.consigliami

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marcogn.kartlog.R
import com.marcogn.kartlog.domain.consigliami.CharacterGain
import com.marcogn.kartlog.domain.consigliami.ConsigliamiEvent
import com.marcogn.kartlog.domain.consigliami.EventScore
import com.marcogn.kartlog.domain.consigliami.RecommendationGroup
import com.marcogn.kartlog.domain.consigliami.RelevantFood
import com.marcogn.kartlog.domain.model.AppLocale
import com.marcogn.kartlog.domain.model.EventType
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Lista di Consigliami con dati inventati (FAKE_FOR_TESTS), senza rete: restano i segnaposto. */
@RunWith(AndroidJUnit4::class)
class ConsigliamiScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun italian() = AppLocale.update(Locale.ITALIAN)

    // Robolectric gira con le risorse della lingua predefinita: i testi si leggono dalle risorse.
    private fun str(@StringRes id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private fun score(id: String, type: EventType, name: String, best: String, foods: List<RelevantFood> = emptyList()) = EventScore(
        event = ConsigliamiEvent(id, type, name, order = 1, courseIds = listOf("fake_course")),
        score = 1.0,
        total = 2,
        best = CharacterGain(best, gain = 2, missingTotal = 2, rosterOrder = 1),
        runnersUp = emptyList(),
        relevantStops = 1,
        relevantFoods = foods,
        improvement = 1.0,
    )

    private val state = ConsigliamiUiState(
        groups = listOf(
            RecommendationGroup(
                1,
                listOf(score("fake_gp", EventType.CUP, "GP FAKE_FOR_TESTS", "fake_char", listOf(RelevantFood("fake_food", "fake_course")))),
                emptySet(),
            ),
            RecommendationGroup(
                2,
                listOf(
                    score("fake_ko1", EventType.RALLY, "KO uno FAKE_FOR_TESTS", "fake_char"),
                    score("fake_ko2", EventType.RALLY, "KO due FAKE_FOR_TESTS", "fake_char"),
                ),
                setOf("fake_course"),
            ),
        ),
        characterNames = mapOf("fake_char" to "Pilota FAKE_FOR_TESTS"),
        courseNames = mapOf("fake_course" to "Percorso FAKE_FOR_TESTS"),
        foodGroupNames = mapOf("fake_food" to "Zuppa FAKE_FOR_TESTS"),
    )

    private fun show(
        s: ConsigliamiUiState = state,
        onEvent: (String) -> Unit = {},
        onFilter: (ConsigliamiEventFilter) -> Unit = {},
    ) = compose.setContent {
        ConsigliamiContent(
            state = s,
            onMenuClick = {},
            onEventClick = onEvent,
            onEventFilterChanged = onFilter,
            onOnlyUsefulChanged = {},
            onResultsEnabledChanged = {},
            onWeightChanged = {},
            onReferenceCcChanged = {},
        )
    }

    @Test
    fun `mostra le card, i cibi possibili e il tocco apre l'evento`() {
        var opened: String? = null
        show(onEvent = { opened = it })
        compose.onNodeWithText(str(R.string.consigliami_foods_maybe), substring = true).assertIsDisplayed()
        compose.onNodeWithText("Zuppa FAKE_FOR_TESTS · Percorso FAKE_FOR_TESTS").assertIsDisplayed()
        compose.onNodeWithText("GP FAKE_FOR_TESTS").performClick()
        assertEquals("fake_gp", opened)
    }

    @Test
    fun `il gruppo a pari merito si apre e si chiude`() {
        show(state.copy(groups = state.groups.reversed()))
        compose.onNodeWithText(str(R.string.consigliami_group_title_common, 2, "Percorso FAKE_FOR_TESTS")).assertIsDisplayed()
        compose.onNodeWithText("KO uno FAKE_FOR_TESTS").assertDoesNotExist()
        compose.onNodeWithContentDescription(str(R.string.consigliami_group_expand_cd)).performClick()
        compose.onNodeWithText("KO uno FAKE_FOR_TESTS").assertIsDisplayed()
        compose.onNodeWithContentDescription(str(R.string.consigliami_group_collapse_cd)).performClick()
        compose.onNodeWithText("KO uno FAKE_FOR_TESTS").assertDoesNotExist()
    }

    @Test
    fun `i filtri notificano la scelta e la i apre l'avviso sugli stand`() {
        var filter: ConsigliamiEventFilter? = null
        show(onFilter = { filter = it })
        compose.onNodeWithText(str(R.string.consigliami_filter_rally)).performClick()
        assertEquals(ConsigliamiEventFilter.RALLY, filter)
        compose.onNodeWithContentDescription(str(R.string.consigliami_info_cd)).performClick()
        compose.onNodeWithText(str(R.string.consigliami_banner), substring = true).assertIsDisplayed()
    }

    @Test
    fun `lo slider del peso compare solo con i risultati attivi`() {
        show(state.copy(resultsEnabled = true))
        compose.onNodeWithText(str(R.string.consigliami_reference_cc)).assertIsDisplayed()
    }

    @Test
    fun `senza consigli mostra il messaggio vuoto`() {
        show(ConsigliamiUiState())
        compose.onNodeWithText(str(R.string.consigliami_no_recommendations)).assertIsDisplayed()
    }
}
