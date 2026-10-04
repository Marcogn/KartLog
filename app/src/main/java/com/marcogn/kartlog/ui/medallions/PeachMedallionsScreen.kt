package com.marcogn.kartlog.ui.medallions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.common.KartChoiceButton
import com.marcogn.kartlog.ui.common.KartPopup
import com.marcogn.kartlog.ui.common.KartPopupText
import com.marcogn.kartlog.ui.common.MapPointListItem
import com.marcogn.kartlog.ui.common.MapPointListScreen

/** Le 200 Monete Peach di mkworld-checklist, una per una (vedi [MapPointListScreen]). */
@Composable
fun PeachMedallionsScreen(
    onMenuClick: () -> Unit,
    onOpenMap: (focusId: String?) -> Unit,
    viewModel: MedallionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    MapPointListScreen(
        title = stringResource(R.string.medallions_title),
        items = state.medallions.map {
            MapPointListItem(it.medallionId, stringResource(R.string.map_point_medallion_format, it.index), it.hint, it.collected)
        },
        onMenuClick = onMenuClick,
        onToggle = viewModel::onMedallionToggled,
        onOpenMap = onOpenMap,
    )

    state.resetNoticeCount?.let { count ->
        KartPopup(title = stringResource(R.string.medallions_reset_notice_title), onDismiss = viewModel::onResetNoticeDismissed) {
            KartPopupText(pluralStringResource(R.plurals.medallions_reset_notice_text, count, count))
            KartChoiceButton(
                text = stringResource(R.string.medallions_reset_notice_ok),
                selected = true,
                onClick = viewModel::onResetNoticeDismissed,
            )
        }
    }
}
