package com.marcogn.kartlog.ui.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.data.local.dao.CharacterProgress
import com.marcogn.kartlog.domain.model.localizedName
import com.marcogn.kartlog.ui.common.CharacterPortrait
import com.marcogn.kartlog.ui.common.KartChoiceButton
import com.marcogn.kartlog.ui.common.KartDropdown
import com.marcogn.kartlog.ui.common.KartInk
import com.marcogn.kartlog.ui.common.KartTiles
import com.marcogn.kartlog.ui.common.KartTitle
import com.marcogn.kartlog.ui.common.KartTopBar
import com.marcogn.kartlog.ui.common.OutlinedTitle
import com.marcogn.kartlog.ui.common.rememberImageAccentColor
import com.marcogn.kartlog.ui.common.kartSky
import com.marcogn.kartlog.ui.theme.isKartDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinScreen(
    onMenuClick: () -> Unit,
    onCharacterClick: (String) -> Unit,
    viewModel: SkinListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.kartSky(isKartDarkTheme()),
        containerColor = Color.Transparent,
        topBar = {
            KartTopBar(
                title = { KartTitle(stringResource(R.string.skin_title)) },
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
                KartChoiceButton(
                    text = stringResource(R.string.skin_filter_all),
                    selected = state.filterMode == SkinFilterMode.ALL,
                    onClick = { viewModel.onFilterModeSelected(SkinFilterMode.ALL) },
                    modifier = Modifier.weight(0.8f),
                )
                KartChoiceButton(
                    text = stringResource(R.string.skin_filter_incomplete),
                    selected = state.filterMode == SkinFilterMode.INCOMPLETE,
                    onClick = { viewModel.onFilterModeSelected(SkinFilterMode.INCOMPLETE) },
                    modifier = Modifier.weight(1.1f),
                )
                KartDropdown(
                    options = SkinSortMode.entries,
                    selected = state.sortMode,
                    label = { sortModeLabel(it) },
                    onSelected = viewModel::onSortModeSelected,
                    modifier = Modifier.weight(1.4f),
                )
            }

            LazyVerticalGrid(
                // Immagini verticali (proporzioni della schermata di selezione del gioco): tre per
                // riga su un telefono, di più su schermi larghi.
                columns = GridCells.Adaptive(minSize = 104.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.characters, key = { it.id }) { character ->
                    CharacterCard(
                        character,
                        onClick = {
                            // Solo chi ha outfit alternativi ha una schermata di dettaglio.
                            if (character.hasOutfits) {
                                onCharacterClick(character.id)
                            } else {
                                viewModel.onUnlockToggled(character.id, !character.unlocked)
                            }
                        },
                    )
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

/**
 * Card "polaroid" (richiesta dell'autore): cornice con bordo nero, l'immagine del wiki e sotto un
 * cartellino del colore del personaggio (preso dall'immagine stessa) con nome e contatore nel font
 * dei titoli. Chi ha tutti gli outfit resta attenuato, come prima.
 */
@Composable
private fun CharacterCard(character: CharacterProgress, onClick: () -> Unit) {
    val displayName = localizedName(character.name, character.nameIt)
    val frame = polaroidFrameColor()
    val accent = rememberImageAccentColor(character.imageUrl) ?: KartTiles.Gray.base
    Polaroid(
        frame = frame,
        onClick = onClick,
        image = {
            CharacterPortrait(
                name = displayName,
                imageUrl = character.imageUrl,
                dimmed = character.isComplete,
                modifier = Modifier.fillMaxWidth().border(1.5.dp, KartInk, RoundedCornerShape(12.dp)),
            )
            if (!character.unlocked) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = stringResource(R.string.skin_locked),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
                        .padding(4.dp)
                        .size(16.dp),
                )
            }
        },
    ) {
        PolaroidLabel(if (character.isComplete) accent.copy(alpha = 0.45f).compositeOver(frame) else accent) {
            // Una riga sola, rimpicciolendo i nomi lunghi: le card della stessa riga restano alte uguali.
            OutlinedTitle(displayName, fontSize = 16.sp, minFontSize = 11.sp)
            OutlinedTitle(
                text = when {
                    character.hasOutfits ->
                        stringResource(R.string.home_counter_format, character.ownedOutfits, character.totalOutfits)
                    character.unlocked -> stringResource(R.string.skin_unlocked)
                    else -> stringResource(R.string.skin_locked)
                },
                fontSize = 14.sp,
                minFontSize = 10.sp,
            )
        }
    }
}
