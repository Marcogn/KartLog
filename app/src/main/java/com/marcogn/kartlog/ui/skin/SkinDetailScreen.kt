package com.marcogn.kartlog.ui.skin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.data.local.dao.OutfitProgress
import com.marcogn.kartlog.domain.model.localizedName
import com.marcogn.kartlog.ui.common.CharacterPortrait

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
                CharacterPortrait(
                    name = state.characterName,
                    imageUrl = state.characterImageUrl,
                    modifier = Modifier.width(64.dp),
                )
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
            LazyVerticalGrid(
                // Due outfit per riga su un telefono: l'immagine deve essere abbastanza grande da
                // distinguere gli outfit, che spesso cambiano solo nei dettagli.
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.outfits, key = { it.outfitId }) { outfit ->
                    OutfitCard(outfit, onToggle = { owned -> viewModel.onOutfitToggled(outfit.outfitId, owned) })
                }
            }
        }
    }
}

@Composable
private fun OutfitCard(outfit: OutfitProgress, onToggle: (Boolean) -> Unit) {
    val name = outfit.outfitName?.let { localizedName(it, outfit.outfitNameIt) }
        ?: stringResource(R.string.skin_default_outfit_name)
    val foodLabel = outfit.foodGroups ?: stringResource(R.string.skin_unknown_food)
    Card(
        // L'outfit di default è sempre posseduto e non spuntabile (SPEC §2.3): la card non reagisce al tap.
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardDefaults.shape)
            .clickable(enabled = !outfit.isDefault) { onToggle(!outfit.owned) },
        colors = if (outfit.owned) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box {
                CharacterPortrait(name = name, imageUrl = outfit.imageUrl, modifier = Modifier.fillMaxWidth())
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                ) {
                    Checkbox(
                        checked = outfit.owned || outfit.isDefault,
                        onCheckedChange = if (outfit.isDefault) null else onToggle,
                        enabled = !outfit.isDefault,
                    )
                }
            }
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
            Text(
                foodLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
