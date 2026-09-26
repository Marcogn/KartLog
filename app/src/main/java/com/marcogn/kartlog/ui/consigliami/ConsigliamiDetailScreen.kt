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
import androidx.compose.material3.Card
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.domain.consigliami.CharacterDetail
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.ui.common.CharacterAvatar
import com.marcogn.kartlog.ui.common.EventIcon
import com.marcogn.kartlog.ui.common.ccLabel
import com.marcogn.kartlog.ui.common.rankLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsigliamiDetailScreen(
    onBack: () -> Unit,
    viewModel: ConsigliamiDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EventIcon(name = state.eventName, imageUrl = state.eventImageUrl, size = 32.dp)
                        Text(state.eventName)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
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
                    CharacterDetailCard(detail, state.characterNames, state.characterImages, state.outfitNames)
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text(stringResource(R.string.consigliami_detail_results_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.consigliami_detail_results_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(Cc.entries, key = { it.name }) { cc ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(ccLabel(cc), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        state.bestRankByCc[cc]?.let { rankLabel(it) } ?: stringResource(R.string.consigliami_detail_results_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterDetailCard(
    detail: CharacterDetail,
    characterNames: Map<String, String>,
    characterImages: Map<String, String>,
    outfitNames: Map<String, String>,
) {
    val name = characterNames[detail.characterId] ?: detail.characterId
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CharacterAvatar(name = name, imageUrl = characterImages[detail.characterId], size = 40.dp)
                Text(
                    text = stringResource(R.string.consigliami_gain_format, detail.gain).let { "$name · $it" },
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            detail.unlockableOutfitIds.forEach { outfitId ->
                Text("• ${outfitNames[outfitId] ?: outfitId}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
