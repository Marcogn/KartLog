package com.marcogn.kartlog.ui.consigliami

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.data.local.entity.RaceResultEntity
import com.marcogn.kartlog.domain.consigliami.CharacterDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsigliamiDetailScreen(
    onBack: () -> Unit,
    viewModel: ConsigliamiDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.eventName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showForm = true }, icon = { Icon(Icons.Filled.Add, contentDescription = null) }, text = { Text(stringResource(R.string.consigliami_register_result)) })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            if (state.details.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.consigliami_detail_no_characters),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                items(state.details, key = { it.characterId }) { detail ->
                    CharacterDetailCard(detail, state.characterNames, state.outfitNames)
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text(stringResource(R.string.consigliami_history_title), style = MaterialTheme.typography.titleMedium)
            }
            if (state.history.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.consigliami_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                items(state.history, key = { it.id }) { result ->
                    HistoryRow(result, state.characterNames, onDelete = { viewModel.onResultDeleted(result) })
                }
            }
        }
    }

    if (showForm) {
        RaceResultBottomSheet(
            eventType = state.eventType,
            characterNames = state.characterNames,
            onSave = { cc, stars, placement, eliminatedAt, characterId ->
                viewModel.onResultSaved(cc, stars, placement, eliminatedAt, characterId)
                showForm = false
            },
            onDismiss = { showForm = false },
        )
    }
}

@Composable
private fun CharacterDetailCard(detail: CharacterDetail, characterNames: Map<String, String>, outfitNames: Map<String, String>) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.consigliami_gain_format, detail.gain).let { "${characterNames[detail.characterId] ?: detail.characterId} · $it" },
                style = MaterialTheme.typography.titleMedium,
            )
            detail.unlockableOutfitIds.forEach { outfitId ->
                Text("• ${outfitNames[outfitId] ?: outfitId}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun HistoryRow(result: RaceResultEntity, characterNames: Map<String, String>, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            val placementText = result.placement?.let { stringResource(R.string.consigliami_history_placement_format, it) }
                ?: result.eliminatedAt?.let { stringResource(R.string.consigliami_history_eliminated_format, it) }
                ?: ""
            Text("${ccLabel(result.cc)} · ${"★".repeat(result.stars)}${"☆".repeat(3 - result.stars)} · $placementText", style = MaterialTheme.typography.bodyMedium)
            result.characterId?.let { characterId ->
                Text(characterNames[characterId] ?: characterId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete))
        }
    }
}
