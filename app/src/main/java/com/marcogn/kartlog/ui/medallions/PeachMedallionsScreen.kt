package com.marcogn.kartlog.ui.medallions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.data.local.dao.MedallionRow
import com.marcogn.kartlog.ui.common.KartChoiceButton
import com.marcogn.kartlog.ui.common.KartPopup
import com.marcogn.kartlog.ui.common.KartPopupText
import com.marcogn.kartlog.ui.common.KartTitle
import com.marcogn.kartlog.ui.common.KartTopBar
import com.marcogn.kartlog.ui.common.kartSky
import com.marcogn.kartlog.ui.map.KartMapButton
import com.marcogn.kartlog.ui.theme.isKartDarkTheme

/**
 * Le 200 Monete Peach di mkworld-checklist, ognuna con le sue istruzioni (in inglese) e il tasto per
 * vederla sulla mappa; il pulsantone in basso apre la mappa con le sole monete.
 */
@Composable
fun PeachMedallionsScreen(
    onMenuClick: () -> Unit,
    onOpenMap: (focusId: String?) -> Unit,
    viewModel: MedallionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.kartSky(isKartDarkTheme()),
        containerColor = Color.Transparent,
        topBar = {
            KartTopBar(
                title = { KartTitle(stringResource(R.string.medallions_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                // Spazio in fondo per il pulsantone della mappa.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.home_counter_format, state.totalCollected, state.medallions.size),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
                items(state.medallions, key = { it.medallionId }) { medallion ->
                    MedallionCard(
                        medallion = medallion,
                        onToggle = { viewModel.onMedallionToggled(medallion.medallionId, it) },
                        onShowOnMap = { onOpenMap(medallion.medallionId) },
                    )
                }
            }
            KartMapButton(onClick = { onOpenMap(null) }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp))
        }
    }

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

@Composable
private fun MedallionCard(medallion: MedallionRow, onToggle: (Boolean) -> Unit, onShowOnMap: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(checked = medallion.collected, onCheckedChange = onToggle)
            Column(Modifier.weight(1f).padding(top = 12.dp)) {
                Text(
                    stringResource(R.string.map_point_medallion_format, medallion.index),
                    style = MaterialTheme.typography.titleMedium,
                )
                medallion.hint?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onShowOnMap) {
                Icon(Icons.Filled.Place, contentDescription = stringResource(R.string.map_show_on_map))
            }
        }
    }
}
