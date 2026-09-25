package com.marcogn.kartlog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R

private data class HomeTile(
    val icon: ImageVector,
    val title: String,
    val counter: String?,
    val color: Color,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMenuClick: () -> Unit,
    onSkinClick: () -> Unit,
    onMedallionsClick: () -> Unit,
    onPSwitchesClick: () -> Unit,
    onConsigliamiClick: () -> Unit,
    onResultsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val notAvailable = stringResource(R.string.home_counter_not_available)

    // "n/d" se il totale è 0 (SPEC §8, fase 3): i pulsanti P possono mancare finché la fase 2
    // di seedgen non gira, un vero "0 / 0" non avrebbe senso da mostrare.
    @Composable
    fun counter(owned: Int, total: Int): String =
        if (total == 0) notAvailable else stringResource(R.string.home_counter_format, owned, total)

    val tiles = listOf(
        HomeTile(
            icon = Icons.Filled.Checkroom,
            title = stringResource(R.string.home_option_skin_title),
            counter = counter(state.ownedOutfits, state.totalOutfits),
            color = MaterialTheme.colorScheme.primary,
            onClick = onSkinClick,
        ),
        HomeTile(
            icon = Icons.Filled.MonetizationOn,
            title = stringResource(R.string.home_option_medallions_title),
            counter = counter(state.collectedMedallions, state.totalMedallions),
            color = MaterialTheme.colorScheme.secondary,
            onClick = onMedallionsClick,
        ),
        HomeTile(
            icon = Icons.Filled.TouchApp,
            title = stringResource(R.string.home_option_pswitches_title),
            counter = counter(state.completedPSwitches, state.totalPSwitches),
            color = MaterialTheme.colorScheme.tertiary,
            onClick = onPSwitchesClick,
        ),
        HomeTile(
            icon = Icons.Filled.Lightbulb,
            title = stringResource(R.string.home_option_consigliami_title),
            counter = null,
            color = MaterialTheme.colorScheme.primary,
            onClick = onConsigliamiClick,
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(tiles) { tile -> HomeTileCard(tile) }
            // Pulsante largo a tutta riga (SPEC §2.1): i risultati alimentano Consigliami ma si
            // registrano qui, non da dentro Consigliami.
            item(span = { GridItemSpan(maxLineSpan) }) {
                ResultsButton(
                    counter = if (state.totalEvents == 0) {
                        notAvailable
                    } else {
                        stringResource(R.string.home_results_counter_format, state.eventsWithResult, state.totalEvents)
                    },
                    onClick = onResultsClick,
                )
            }
        }
    }
}

@Composable
private fun ResultsButton(counter: String, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.secondary
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.16f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(56.dp)
                    .background(color = color.copy(alpha = 0.18f), shape = MaterialTheme.shapes.large)
                    .padding(12.dp),
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(stringResource(R.string.home_option_results_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.home_option_results_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    counter,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeTileCard(tile: HomeTile) {
    Card(
        onClick = tile.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = tile.color.copy(alpha = 0.16f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = null,
                tint = tile.color,
                modifier = Modifier
                    .size(56.dp)
                    .background(color = tile.color.copy(alpha = 0.18f), shape = MaterialTheme.shapes.large)
                    .padding(12.dp),
            )
            Text(
                text = tile.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
            if (tile.counter != null) {
                Text(
                    text = tile.counter,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
