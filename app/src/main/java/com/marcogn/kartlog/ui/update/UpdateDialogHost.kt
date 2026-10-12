package com.marcogn.kartlog.ui.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.marcogn.kartlog.BuildConfig
import com.marcogn.kartlog.R
import com.marcogn.kartlog.domain.update.AvailableUpdate
import com.marcogn.kartlog.domain.update.UpdateState
import com.marcogn.kartlog.ui.common.KartChoiceButton
import com.marcogn.kartlog.ui.common.KartPopup
import com.marcogn.kartlog.ui.common.KartPopupText

/**
 * I dialoghi degli aggiornamenti, per tutta l'app e pilotati solo da `UpdateManager.state`:
 * disponibile (titoli, "Changelog completo", Scarica / Non ora) → avanzamento (Annulla) → pronto da
 * installare → conferma di sistema → errore. Sta sopra il grafo di navigazione, così il controllo
 * all'avvio può chiedere su qualunque schermata. Grafica del gioco (`KartPopup`); la X e il tocco
 * fuori fanno quello che fa il pulsante di rinuncia (Annulla / Non ora / Chiudi).
 */
@Composable
fun UpdateDialogHost(viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val permissionIntent by viewModel.permissionIntent.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.onPermissionScreenClosed() }
    LaunchedEffect(permissionIntent) {
        permissionIntent?.let {
            permissionLauncher.launch(it)
            viewModel.onPermissionScreenLaunched()
        }
    }

    UpdateDialogs(
        state = state,
        onDownload = viewModel::onDownload,
        onCancelDownload = viewModel::onCancelDownload,
        onDismiss = viewModel::onDismiss,
        onInstall = viewModel::onInstall,
    )
}

/** Stato → dialogo, senza ViewModel né Hilt: è ciò che provano i test. */
@Composable
internal fun UpdateDialogs(
    state: UpdateState,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
) {
    val context = LocalContext.current
    when (val current = state) {
        is UpdateState.Available -> AvailableDialog(
            update = current.update,
            onDownload = onDownload,
            onNotNow = onDismiss,
        )
        is UpdateState.Downloading -> KartPopup(
            title = stringResource(R.string.update_downloading_title, current.update.version.toString()),
            onDismiss = onCancelDownload,
        ) {
            if (current.totalBytes > 0) {
                val fraction = (current.bytesRead.toFloat() / current.totalBytes).coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                KartPopupText(stringResource(R.string.update_download_percent, (fraction * 100).toInt()))
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            KartChoiceButton(
                text = stringResource(R.string.update_cancel),
                selected = false,
                onClick = onCancelDownload,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is UpdateState.ReadyToInstall -> KartPopup(
            title = stringResource(R.string.update_ready_title),
            onDismiss = onDismiss,
        ) {
            KartPopupText(stringResource(R.string.update_ready_message, current.update.version.toString()))
            KartChoiceButton(
                text = stringResource(R.string.update_install),
                selected = true,
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth(),
            )
            KartChoiceButton(
                text = stringResource(R.string.update_not_now),
                selected = false,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is UpdateState.Installing -> KartPopup(
            title = stringResource(R.string.update_installing_title),
            onDismiss = onDismiss,
        ) {
            KartPopupText(stringResource(R.string.update_installing_message))
            // "Chiudi" è l'uscita se il sistema non riporta mai uno stato finale (UpdateManager.dismiss).
            KartChoiceButton(
                text = stringResource(R.string.update_close),
                selected = false,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is UpdateState.Failed -> KartPopup(
            title = stringResource(R.string.update_failed_title),
            onDismiss = onDismiss,
        ) {
            KartPopupText(current.failure.toMessage(context))
            KartChoiceButton(
                text = stringResource(R.string.update_close),
                selected = false,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        UpdateState.Idle, UpdateState.Checking -> Unit
    }
}

@Composable
private fun AvailableDialog(update: AvailableUpdate, onDownload: () -> Unit, onNotNow: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    KartPopup(title = stringResource(R.string.update_available_title), onDismiss = onNotNow) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KartPopupText(
                stringResource(R.string.update_available_versions, update.version.toString(), BuildConfig.VERSION_NAME),
                Modifier.fillMaxWidth(),
            )
            update.notes.headlines.forEach {
                KartPopupText("• $it", Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            }
        }
        update.notes.fullChangelogUrl?.let { url ->
            KartChoiceButton(
                text = stringResource(R.string.update_full_changelog),
                selected = false,
                onClick = { runCatching { uriHandler.openUri(url) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        KartChoiceButton(
            text = stringResource(R.string.update_download),
            selected = true,
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth(),
        )
        KartChoiceButton(
            text = stringResource(R.string.update_not_now),
            selected = false,
            onClick = onNotNow,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
