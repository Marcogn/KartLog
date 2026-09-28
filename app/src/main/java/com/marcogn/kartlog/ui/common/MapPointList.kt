package com.marcogn.kartlog.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.map.KartMapButton
import com.marcogn.kartlog.ui.theme.isKartDarkTheme

/** Una riga delle liste di punti della mappa (Monete Peach, pannelli "?"). */
data class MapPointListItem(val id: String, val title: String, val hint: String?, val done: Boolean)

/**
 * Lista dei punti di un tipo della mappa (dati di mkworld-checklist), uno per uno: contatore
 * globale, checkbox, istruzioni (in inglese) e "Mostra sulla mappa"; il pulsantone in basso apre la
 * mappa con quel solo tipo. Condivisa da Monete Peach e pannelli "?".
 */
@Composable
fun MapPointListScreen(
    title: String,
    items: List<MapPointListItem>,
    onMenuClick: () -> Unit,
    onToggle: (id: String, done: Boolean) -> Unit,
    onOpenMap: (focusId: String?) -> Unit,
) {
    Scaffold(
        modifier = Modifier.kartSky(isKartDarkTheme()),
        containerColor = Color.Transparent,
        topBar = {
            KartTopBar(
                title = { KartTitle(title) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                // Spazio in fondo per il pulsantone della mappa.
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.home_counter_format, items.count { it.done }, items.size),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
                items(items, key = { it.id }) { item ->
                    MapPointCard(
                        item = item,
                        onToggle = { onToggle(item.id, it) },
                        onShowOnMap = { onOpenMap(item.id) },
                    )
                }
            }
            KartMapButton(onClick = { onOpenMap(null) }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp))
        }
    }
}

@Composable
private fun MapPointCard(item: MapPointListItem, onToggle: (Boolean) -> Unit, onShowOnMap: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(checked = item.done, onCheckedChange = onToggle)
            Column(Modifier.weight(1f).padding(top = 12.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                item.hint?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onShowOnMap) {
                Icon(Icons.Filled.Place, contentDescription = stringResource(R.string.map_show_on_map))
            }
        }
    }
}
