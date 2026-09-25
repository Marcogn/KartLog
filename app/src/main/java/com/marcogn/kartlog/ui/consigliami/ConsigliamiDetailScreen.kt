package com.marcogn.kartlog.ui.consigliami

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.domain.consigliami.CharacterDetail

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
                title = { Text(state.eventName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        if (state.details.isEmpty()) {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Text(
                    text = stringResource(R.string.consigliami_detail_no_characters),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                items(state.details, key = { it.characterId }) { detail ->
                    CharacterDetailCard(detail, state.characterNames, state.outfitNames)
                }
            }
        }
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
