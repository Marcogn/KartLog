package com.marcogn.kartlog.ui.skin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.data.local.dao.CharacterProgress
import com.marcogn.kartlog.ui.common.CharacterAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinScreen(
    onMenuClick: () -> Unit,
    onCharacterClick: (String) -> Unit,
    viewModel: SkinListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.skin_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = state.filterMode == SkinFilterMode.ALL,
                    onClick = { viewModel.onFilterModeSelected(SkinFilterMode.ALL) },
                    label = { Text(stringResource(R.string.skin_filter_all)) },
                )
                FilterChip(
                    selected = state.filterMode == SkinFilterMode.INCOMPLETE,
                    onClick = { viewModel.onFilterModeSelected(SkinFilterMode.INCOMPLETE) },
                    label = { Text(stringResource(R.string.skin_filter_incomplete)) },
                )
                SortMenu(
                    sortMode = state.sortMode,
                    onSortModeSelected = viewModel::onSortModeSelected,
                    modifier = Modifier.weight(1f),
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.characters, key = { it.id }) { character ->
                    CharacterCard(character, onClick = { onCharacterClick(character.id) })
                }
            }
        }
    }
}

@Composable
private fun sortModeLabel(mode: SkinSortMode): String = when (mode) {
    SkinSortMode.ROSTER -> stringResource(R.string.skin_sort_roster)
    SkinSortMode.ALPHABETICAL -> stringResource(R.string.skin_sort_alphabetical)
    SkinSortMode.COMPLETION -> stringResource(R.string.skin_sort_completion)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortMenu(sortMode: SkinSortMode, onSortModeSelected: (SkinSortMode) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        TextField(
            value = sortModeLabel(sortMode),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.skin_sort_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SkinSortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(sortModeLabel(mode)) },
                    onClick = { onSortModeSelected(mode); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun CharacterCard(character: CharacterProgress, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = if (character.isComplete) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CharacterAvatar(name = character.name, dimmed = character.isComplete)
            Text(
                text = character.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = if (character.isComplete) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.home_counter_format, character.ownedOutfits, character.totalOutfits),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
