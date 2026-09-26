package com.marcogn.kartlog.ui.medallions

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.common.MarkAllConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeachMedallionsScreen(
    onMenuClick: () -> Unit,
    viewModel: MedallionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
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
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.home_counter_format, state.totalCollected, state.totalCount),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            items(state.regions, key = { it.regionId }) { region ->
                RegionCounterCard(
                    region = region,
                    onDecrement = { viewModel.onDecrement(region.regionId) },
                    onIncrement = { viewModel.onIncrement(region.regionId) },
                    onMarkAll = { regionPendingConfirmation = region.regionId },
                )
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
private fun RegionCounterCard(
    region: RegionMedallions,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onMarkAll: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(region.regionName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                FilledTonalIconButton(onClick = onDecrement, enabled = region.collected > 0) {
                    Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.medallions_decrement))
                }
                Text(
                    stringResource(R.string.home_counter_format, region.collected, region.total),
                    style = MaterialTheme.typography.titleMedium,
                )
                FilledTonalIconButton(onClick = onIncrement, enabled = region.collected < region.total) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.medallions_increment))
                }
                IconButton(onClick = onMarkAll, enabled = region.collected < region.total) {
                    Icon(Icons.Filled.DoneAll, contentDescription = stringResource(R.string.collectible_mark_all))
                }
            }
            LinearProgressIndicator(
                progress = { if (region.total == 0) 0f else region.collected.toFloat() / region.total },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}
