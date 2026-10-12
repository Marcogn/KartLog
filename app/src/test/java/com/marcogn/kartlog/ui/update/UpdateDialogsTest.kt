package com.marcogn.kartlog.ui.update

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marcogn.kartlog.R
import com.marcogn.kartlog.domain.update.AvailableUpdate
import com.marcogn.kartlog.domain.update.ReleaseEvaluation
import com.marcogn.kartlog.domain.update.UpdateFailure
import com.marcogn.kartlog.domain.update.UpdateState
import com.marcogn.kartlog.domain.update.evaluateRelease
import com.marcogn.kartlog.domain.update.parseLatestRelease
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Stato → dialogo con la grafica del gioco, sulla risposta reale di `releases/latest`; testi da `R.string`. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w400dp-h900dp")
class UpdateDialogsTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun str(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)

    private val update: AvailableUpdate = run {
        val json = checkNotNull(javaClass.classLoader?.getResource("update/releases-latest-v1.2.0.json")).readText()
        (evaluateRelease(parseLatestRelease(json), installedVersionName = "1.1.0") as ReleaseEvaluation.Newer).update
    }

    private class Calls {
        var download = 0
        var cancel = 0
        var dismiss = 0
        var install = 0
    }

    private fun show(state: UpdateState, calls: Calls = Calls()) {
        compose.setContent {
            UpdateDialogs(
                state = state,
                onDownload = { calls.download++ },
                onCancelDownload = { calls.cancel++ },
                onDismiss = { calls.dismiss++ },
                onInstall = { calls.install++ },
            )
        }
    }

    @Test
    fun `disponibile mostra titoli, changelog completo e i due pulsanti`() {
        show(UpdateState.Available(update))
        compose.onNodeWithText(str(R.string.update_available_title)).assertIsDisplayed()
        compose.onNodeWithText("• ${update.notes.headlines.first()}").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.update_full_changelog)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.update_download)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.update_not_now)).assertIsDisplayed()
    }

    @Test
    fun `Scarica e Non ora chiamano le azioni giuste`() {
        val calls = Calls()
        show(UpdateState.Available(update), calls)
        compose.onNodeWithText(str(R.string.update_not_now)).performClick()
        assertEquals(1, calls.dismiss)
        compose.onNodeWithText(str(R.string.update_download)).performClick()
        assertEquals(1, calls.download)
    }

    @Test
    fun `durante il download c'è Annulla e la percentuale`() {
        val calls = Calls()
        show(UpdateState.Downloading(update, bytesRead = 50, totalBytes = 100), calls)
        compose.onNodeWithText(str(R.string.update_download_percent, 50)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.update_cancel)).performClick()
        assertEquals(1, calls.cancel)
    }

    @Test
    fun `pronto da installare chiede Installa o Non ora`() {
        val calls = Calls()
        show(UpdateState.ReadyToInstall(update, File("x.apk")), calls)
        compose.onNodeWithText(str(R.string.update_install)).performClick()
        assertEquals(1, calls.install)
        compose.onNodeWithText(str(R.string.update_not_now)).performClick()
        assertEquals(1, calls.dismiss)
    }

    @Test
    fun `errore mostra il motivo e Chiudi`() {
        val calls = Calls()
        show(UpdateState.Failed(UpdateFailure.Http(503)), calls)
        compose.onNodeWithText(str(R.string.update_failed_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.update_error_http, 503)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.update_close)).performClick()
        assertEquals(1, calls.dismiss)
    }

    @Test
    fun `Idle e Checking non mostrano nulla`() {
        show(UpdateState.Checking)
        compose.onNodeWithText(str(R.string.update_available_title)).assertDoesNotExist()
        compose.onNodeWithText(str(R.string.update_failed_title)).assertDoesNotExist()
    }
}
