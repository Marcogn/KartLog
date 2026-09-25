package com.marcogn.kartlog.ui.pswitches

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.common.MarkAllConfirmationDialog
import com.marcogn.kartlog.ui.common.RegionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PSwitchesScreen(
    onMenuClick: () -> Unit,
    viewModel: PSwitchesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var expandedRegions by rememberSaveable { mutableStateOf(setOf<String>()) }
    var regionPendingConfirmation by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pswitches_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
            )
        },
    ) { padding ->
        if (!state.dataAvailable) {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                Text(
                    text = stringResource(R.string.pswitches_no_data),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.padding(padding).fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Text(
                    text = stringResource(R.string.home_counter_format, state.totalCompleted, state.totalCount),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                )
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChanged,
                    label = { Text(stringResource(R.string.pswitches_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
                if (state.regions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.pswitches_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(state.regions, key = { it.regionId }) { region ->
                val expanded = region.regionId in expandedRegions || state.query.isNotBlank()
                RegionHeader(
                    name = region.regionName,
                    collected = region.completed,
                    total = region.total,
                    expanded = expanded,
                    onExpandToggle = {
                        expandedRegions = if (expanded) expandedRegions - region.regionId else expandedRegions + region.regionId
                    },
                    onMarkAllClick = { regionPendingConfirmation = region.regionId },
                )
                if (expanded) {
                    region.locations.forEach { location ->
                        Text(
                            text = location.locationName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 4.dp),
                        )
                        location.pSwitches.forEach { mission ->
                            PSwitchRowItem(mission, onToggle = { checked -> viewModel.onPSwitchToggled(mission.id, checked) })
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

@Composable
private fun PSwitchRowItem(mission: PSwitchRow, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
    ) {
        Checkbox(checked = mission.completed, onCheckedChange = onToggle)
        Text(mission.name, style = MaterialTheme.typography.bodyMedium)
    }
}
