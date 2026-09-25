package com.marcogn.kartlog.ui.skin

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.data.local.dao.OutfitProgress
import com.marcogn.kartlog.domain.model.localizedName
import com.marcogn.kartlog.ui.common.CharacterAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinDetailScreen(
    onBack: () -> Unit,
    viewModel: SkinDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.characterName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CharacterAvatar(name = state.characterName)
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.characterName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.home_counter_format, state.ownedCount, state.totalCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.skin_unlock_switch), style = MaterialTheme.typography.labelSmall)
                    Switch(checked = state.unlocked, onCheckedChange = viewModel::onUnlockToggled)
                }
            }
            HorizontalDivider()
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(state.outfits, key = { it.outfitId }) { outfit ->
                    OutfitRow(outfit, onToggle = { owned -> viewModel.onOutfitToggled(outfit.outfitId, owned) })
                }
            }
        }
    }
}

@Composable
private fun OutfitRow(outfit: OutfitProgress, onToggle: (Boolean) -> Unit) {
    val name = outfit.outfitName?.let { localizedName(it, outfit.outfitNameIt) }
        ?: stringResource(R.string.skin_default_outfit_name)
    val foodLabel = outfit.foodGroups ?: stringResource(R.string.skin_unknown_food)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (outfit.isDefault) {
            // Sempre posseduto, non spuntabile (SPEC §2.3): niente checkbox interattiva.
            Checkbox(checked = true, onCheckedChange = null, enabled = false, colors = CheckboxDefaults.colors())
        } else {
            Checkbox(checked = outfit.owned, onCheckedChange = onToggle)
        }
        Column {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(foodLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
