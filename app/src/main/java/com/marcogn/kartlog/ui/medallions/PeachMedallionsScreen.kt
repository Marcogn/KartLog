package com.marcogn.kartlog.ui.medallions

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.common.MarkAllConfirmationDialog
import com.marcogn.kartlog.ui.common.RegionHeader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PeachMedallionsScreen(
    onMenuClick: () -> Unit,
    viewModel: MedallionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var expandedRegions by rememberSaveable { mutableStateOf(setOf<String>()) }
    var regionPendingConfirmation by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.medallions_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
                actions = {
                    state.guideUrl?.let { url ->
                        IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }) {
                            Icon(Icons.Filled.MenuBook, contentDescription = stringResource(R.string.medallions_open_guide))
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Text(
                    text = stringResource(R.string.home_counter_format, state.totalCollected, state.totalCount),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(state.regions, key = { it.regionId }) { region ->
                val expanded = region.regionId in expandedRegions
                RegionHeader(
                    name = region.regionName,
                    collected = region.collected,
                    total = region.total,
                    expanded = expanded,
                    onExpandToggle = {
                        expandedRegions = if (expanded) expandedRegions - region.regionId else expandedRegions + region.regionId
                    },
                    onMarkAllClick = { regionPendingConfirmation = region.regionId },
                )
                if (expanded) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    ) {
                        region.medallions.forEach { medallion ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(onClick = { viewModel.onMedallionToggled(medallion.id, !medallion.collected) }),
                            ) {
                                Checkbox(
                                    checked = medallion.collected,
                                    onCheckedChange = { checked -> viewModel.onMedallionToggled(medallion.id, checked) },
                                )
                                Text(stringResource(R.string.medallions_item_label, medallion.index))
                            }
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }

    regionPendingConfirmation?.let { regionId ->
        val regionName = state.regions.firstOrNull { it.regionId == regionId }?.regionName.orEmpty()
        MarkAllConfirmationDialog(
            regionName = regionName,
            onConfirm = {
                viewModel.onMarkAllRequested(regionId)
                regionPendingConfirmation = null
            },
            onDismiss = { regionPendingConfirmation = null },
        )
    }
}
