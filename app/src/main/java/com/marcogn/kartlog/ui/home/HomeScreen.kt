package com.marcogn.kartlog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marcogn.kartlog.R

// Totali reali da SPEC §5.1 (127 outfit, 200 medaglioni, 394 pulsanti P): il numeratore è
// fittizio finché lo stato utente non arriva (fase 3+), il denominatore no.
private const val TOTAL_OUTFITS = 127
private const val TOTAL_MEDALLIONS = 200
private const val TOTAL_P_SWITCHES = 394

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
) {
    val tiles = listOf(
        HomeTile(
            icon = Icons.Filled.Checkroom,
            title = stringResource(R.string.home_option_skin_title),
            counter = stringResource(R.string.home_counter_format, 0, TOTAL_OUTFITS),
            color = MaterialTheme.colorScheme.primary,
            onClick = onSkinClick,
        ),
        HomeTile(
            icon = Icons.Filled.MonetizationOn,
            title = stringResource(R.string.home_option_medallions_title),
            counter = stringResource(R.string.home_counter_format, 0, TOTAL_MEDALLIONS),
            color = MaterialTheme.colorScheme.secondary,
            onClick = onMedallionsClick,
        ),
        HomeTile(
            icon = Icons.Filled.TouchApp,
            title = stringResource(R.string.home_option_pswitches_title),
            counter = stringResource(R.string.home_counter_format, 0, TOTAL_P_SWITCHES),
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
