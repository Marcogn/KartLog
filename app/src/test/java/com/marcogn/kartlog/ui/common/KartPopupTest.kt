package com.marcogn.kartlog.ui.common

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marcogn.kartlog.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Popup rosso: testi visibili, interruttore funzionante, la X chiude senza toccare l'interruttore. */
@RunWith(AndroidJUnit4::class)
class KartPopupTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun switchTogglesAndCloseButtonDismissesWithoutChanges() {
        var dismissed = false
        var unlocked = false
        var changes = 0
        compose.setContent {
            KartPopup(title = "Pilota FAKE_FOR_TESTS", onDismiss = { dismissed = true }) {
                KartPopupText("Criterio FAKE_FOR_TESTS")
                KartSwitchRow("Sbloccato", checked = unlocked, onCheckedChange = { unlocked = it; changes++ })
            }
        }
        compose.onNodeWithText("Criterio FAKE_FOR_TESTS").assertIsDisplayed()
        compose.onNodeWithText("Sbloccato").performClick()  // l'etichetta da sola non cambia nulla
        assertEquals(0, changes)
        val close = ApplicationProvider.getApplicationContext<Context>().getString(R.string.cd_close)
        compose.onNodeWithContentDescription(close).performClick()
        assertEquals(true, dismissed)
        assertEquals(0, changes)
    }
}
