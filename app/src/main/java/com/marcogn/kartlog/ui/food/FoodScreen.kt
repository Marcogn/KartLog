package com.marcogn.kartlog.ui.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.common.FoodImage
import com.marcogn.kartlog.ui.common.KartCounterPill
import com.marcogn.kartlog.ui.common.KartPanel
import com.marcogn.kartlog.ui.common.KartTiles
import com.marcogn.kartlog.ui.common.KartTitle
import com.marcogn.kartlog.ui.common.KartTopBar
import com.marcogn.kartlog.ui.common.OutlinedTitle
import com.marcogn.kartlog.ui.common.kartSky
import com.marcogn.kartlog.ui.theme.isKartDarkTheme

/** Griglia dei gruppi di cibo (sottosezione di Personaggi): toccando una tessera si apre il dettaglio. */
@Composable
fun FoodScreen(
    onMenuClick: () -> Unit,
    onFoodClick: (String) -> Unit,
    viewModel: FoodListViewModel = hiltViewModel(),
) {
    val tiles by viewModel.tiles.collectAsState()
    FoodGridContent(tiles, onMenuClick, onFoodClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FoodGridContent(tiles: List<FoodTile>, onMenuClick: () -> Unit, onFoodClick: (String) -> Unit) {
    Scaffold(
        modifier = Modifier.kartSky(isKartDarkTheme()),
        containerColor = Color.Transparent,
        topBar = {
            KartTopBar(
                title = { KartTitle(stringResource(R.string.food_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            // Due tessere per riga su un telefono, di più su schermi larghi.
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(padding),
        ) {
            items(tiles, key = { it.id }) { tile -> FoodTileCard(tile, onClick = { onFoodClick(tile.id) }) }
        }
    }
}

@Composable
private fun FoodTileCard(tile: FoodTile, onClick: () -> Unit) {
    val colors = KartTiles.Orange
    KartPanel(colors = colors, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FoodImage(tile.name, tile.imageUrl, size = 84.dp)
            // Altezza fissa per due righe: le tessere della stessa riga restano alte uguali.
            Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                OutlinedTitle(tile.name, fontSize = 18.sp, maxLines = 2)
            }
            if (tile.totalOutfits > 0) {
                KartCounterPill(pluralStringResource(R.plurals.food_missing, tile.missingOutfits, tile.missingOutfits), colors)
            } else {
                KartCounterPill(stringResource(R.string.food_no_outfits), colors)
            }
        }
    }
}
