package com.marcogn.kartlog.ui.map

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.domain.model.MapPointType
import com.marcogn.kartlog.ui.common.KartChoiceButton
import com.marcogn.kartlog.ui.common.KartInk
import com.marcogn.kartlog.ui.common.KartPopup
import com.marcogn.kartlog.ui.common.KartSwitchRow
import com.marcogn.kartlog.ui.common.KartTiles
import com.marcogn.kartlog.ui.common.KartTitle
import com.marcogn.kartlog.ui.common.KartTopBar
import com.marcogn.kartlog.ui.common.OutlinedTitle
import com.marcogn.kartlog.ui.common.kartSky
import com.marcogn.kartlog.ui.theme.isKartDarkTheme

/**
 * Mappa dei collezionabili (dati di mkworld-checklist): una voce del drawer ([onMenuClick]) o il
 * pulsantone di Monete Peach e Pulsanti P ([onBack], che allora sostituisce il menu).
 */
@Composable
fun MapScreen(
    onMenuClick: () -> Unit,
    onBack: (() -> Unit)?,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var imageLoaded by remember { mutableStateOf<Boolean?>(null) }
    val focus = viewModel.focusId?.let { id -> state.all.firstOrNull { it.id == id } }

    Scaffold(
        modifier = Modifier.kartSky(isKartDarkTheme()),
        containerColor = Color.Transparent,
        topBar = {
            KartTopBar(
                title = { KartTitle(stringResource(R.string.map_title)) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    } else {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            CollectibleMapView(
                image = viewModel.image,
                points = state.points,
                focus = focus,
                onPointClick = { selectedId = it.id },
                onImageStateChange = { imageLoaded = it },
                modifier = Modifier.fillMaxSize(),
            )
            MapFilters(
                state = state,
                onTypeToggled = viewModel::onTypeToggled,
                onShowDoneToggled = viewModel::onShowDoneToggled,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            if (imageLoaded == false || viewModel.image == null) {
                OutlinedTitle(
                    stringResource(R.string.map_image_unavailable),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(KartInk.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 16.sp,
                    maxLines = 3,
                )
            }
        }
    }

    selectedId?.let { id -> state.all.firstOrNull { it.id == id } }?.let { point ->
        MapPointPopup(
            point = point,
            onDoneChanged = { viewModel.onDoneChanged(point, it) },
            onDismiss = { selectedId = null },
        )
    }
}

@Composable
internal fun MapFilters(
    state: MapUiState,
    onTypeToggled: (MapPointType) -> Unit,
    onShowDoneToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Griglia 2×2 a larghezza fissa, non una riga scorrevole: con larghezza infinita i testi che si
    // rimpiccioliscono dei pulsantoni finivano a larghezza zero (pulsanti vuoti, visto dall'autore).
    @Composable
    fun TypeButton(type: MapPointType, modifier: Modifier) {
        val counter = state.counters[type]
        KartChoiceButton(
            text = stringResource(type.filterLabel()) + (counter?.let { " ${it.done}/${it.total}" } ?: ""),
            selected = type in state.visibleTypes,
            onClick = { onTypeToggled(type) },
            modifier = modifier,
            trailing = { MarkerIcon(type, Modifier.padding(start = 6.dp)) },
        )
    }
    Column(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Stesso ordine delle voci del drawer: Monete Peach, Pulsanti P, poi i pannelli "?".
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TypeButton(MapPointType.MEDALLION, Modifier.weight(1f))
            TypeButton(MapPointType.P_SWITCH, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TypeButton(MapPointType.QUESTION_PANEL, Modifier.weight(1f))
            KartChoiceButton(
                text = stringResource(R.string.map_filter_show_done),
                selected = state.showDone,
                onClick = onShowDoneToggled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Legenda nei filtri: lo stesso marker disegnato sulla mappa. */
@Composable
private fun MarkerIcon(type: MapPointType, modifier: Modifier = Modifier) {
    val glyphs = rememberMarkerGlyphs()
    Canvas(modifier.size(22.dp)) { drawMarker(type, center, 9.dp.toPx(), glyphs) }
}

private fun MapPointType.filterLabel(): Int = when (this) {
    MapPointType.MEDALLION -> R.string.map_filter_medallions
    MapPointType.P_SWITCH -> R.string.map_filter_pswitches
    MapPointType.QUESTION_PANEL -> R.string.map_filter_panels
}

@Composable
private fun MapPointPopup(point: MapPoint, onDoneChanged: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val title = when (point.type) {
        MapPointType.MEDALLION -> stringResource(R.string.map_point_medallion_format, point.index)
        MapPointType.P_SWITCH -> stringResource(R.string.map_point_pswitch)
        MapPointType.QUESTION_PANEL -> stringResource(R.string.map_point_panel_format, point.index)
    }
    val doneLabel = when (point.type) {
        MapPointType.MEDALLION -> R.string.map_done_medallion
        MapPointType.P_SWITCH -> R.string.map_done_pswitch
        MapPointType.QUESTION_PANEL -> R.string.map_done_panel
    }
    KartPopup(title = title, onDismiss = onDismiss) {
        point.name?.let { OutlinedTitle("“$it”", fontSize = 20.sp, maxLines = 4) }
        point.location?.let { OutlinedTitle(it, fontSize = 17.sp, fill = KartTiles.Yellow.light, maxLines = 2) }
        point.hint?.let { hint ->
            // Istruzioni di mkworld-checklist, solo in inglese: possono essere lunghe, si scorrono.
            Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                OutlinedTitle(hint, fontSize = 16.sp, textAlign = TextAlign.Start, maxLines = Int.MAX_VALUE)
            }
        }
        KartSwitchRow(stringResource(doneLabel), point.done, onDoneChanged)
        point.youtubeId?.let { videoId ->
            KartChoiceButton(
                text = stringResource(R.string.map_watch_video),
                selected = false,
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.youtube.com/watch?v=$videoId".toUri()))
                },
            )
        }
    }
}

/**
 * Pulsantone tondo della mappa, in sovrimpressione su Monete Peach e Pulsanti P (richiesta
 * dell'autore): sempre rosso con l'icona bianca, anche nel tema scuro.
 */
@Composable
fun KartMapButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val red = KartTiles.Red
    Box(
        modifier = modifier
            .size(64.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(red.light, red.base)))
            .border(3.dp, KartInk, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Map, contentDescription = stringResource(R.string.map_open), tint = Color.White, modifier = Modifier.size(34.dp))
    }
}
