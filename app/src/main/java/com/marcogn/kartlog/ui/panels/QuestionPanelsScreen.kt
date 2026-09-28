package com.marcogn.kartlog.ui.panels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.common.MapPointListItem
import com.marcogn.kartlog.ui.common.MapPointListScreen

/** I 150 pannelli "?" di mkworld-checklist, uno per uno, come le Monete Peach (richiesta dell'autore). */
@Composable
fun QuestionPanelsScreen(
    onMenuClick: () -> Unit,
    onOpenMap: (focusId: String?) -> Unit,
    viewModel: QuestionPanelsViewModel = hiltViewModel(),
) {
    val panels by viewModel.panels.collectAsState()
    MapPointListScreen(
        title = stringResource(R.string.panels_title),
        items = panels.map {
            MapPointListItem(it.panelId, stringResource(R.string.map_point_panel_format, it.index), it.hint, it.activated)
        },
        onMenuClick = onMenuClick,
        onToggle = viewModel::onPanelToggled,
        onOpenMap = onOpenMap,
    )
}
