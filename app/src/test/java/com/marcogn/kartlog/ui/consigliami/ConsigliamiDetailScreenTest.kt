package com.marcogn.kartlog.ui.consigliami

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

/** Dettaglio evento con dati inventati (FAKE_FOR_TESTS), senza rete: restano i segnaposto. */
@RunWith(AndroidJUnit4::class)
class ConsigliamiDetailScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun italian() = AppLocale.update(Locale.ITALIAN)

    // Robolectric gira con le risorse della lingua predefinita: i testi si leggono dalle risorse.
    private fun str(@StringRes id: Int, vararg args: Any): String =
        ApplicationProvider.getApplicationContext<Context>().getString(id, *args)

    private val state = ConsigliamiDetailUiState(
        eventName = "GP FAKE_FOR_TESTS",
        characters = listOf(
            DetailCharacterUi(
                characterId = "fake_char",
                name = "Pilota FAKE_FOR_TESTS",
                imageUrl = null,
                gain = 1,
                outfits = listOf(
                    DetailOutfitUi(
                        "fake_outfit",
                        "Vestito FAKE_FOR_TESTS",
                        listOf(DetailFoodUi("fake_soup", "Zuppa FAKE_FOR_TESTS", null, listOf("Percorso A", "Percorso B"))),
                    ),
                ),
            ),
        ),
        bestRankByCc = mapOf(Cc.CC_150 to TrophyRank.GOLD),
    )

    @Test
    fun `mostra avviso, riga outfit cibo percorsi e il tocco sul cibo apre la sezione Cibi`() {
        var opened: String? = null
        compose.setContent { ConsigliamiDetailContent(state, onBack = {}, onFoodClick = { opened = it }) }
        compose.onNodeWithText(str(R.string.consigliami_detail_maybe)).assertIsDisplayed()
        compose.onNodeWithText("Vestito FAKE_FOR_TESTS –").assertIsDisplayed()
        // Il pulsante del cibo unisce i suoi testi in un solo nodo: si cerca per sottostringa.
        compose.onNodeWithText("Percorso A, Percorso B", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Zuppa FAKE_FOR_TESTS", substring = true).performClick()
        assertEquals("fake_soup", opened)
    }

    @Test
    fun `mostra i risultati per cilindrata`() {
        compose.setContent { ConsigliamiDetailContent(state, onBack = {}, onFoodClick = {}) }
        compose.onNodeWithText(str(R.string.rank_gold)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `senza personaggi utili mostra il messaggio`() {
        compose.setContent { ConsigliamiDetailContent(ConsigliamiDetailUiState(eventName = "KO"), onBack = {}, onFoodClick = {}) }
        compose.onNodeWithText(str(R.string.consigliami_detail_no_characters)).assertIsDisplayed()
    }
}
