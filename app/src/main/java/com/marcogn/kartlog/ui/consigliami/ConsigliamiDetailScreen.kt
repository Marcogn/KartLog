package com.marcogn.kartlog.ui.consigliami

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.domain.model.Cc
import com.marcogn.kartlog.ui.common.CharacterAvatar
import com.marcogn.kartlog.ui.common.EventIcon
import com.marcogn.kartlog.ui.common.FoodImage
import com.marcogn.kartlog.ui.common.KartColors
import com.marcogn.kartlog.ui.common.KartCounterPill
import com.marcogn.kartlog.ui.common.KartInk
import com.marcogn.kartlog.ui.common.KartPanel
import com.marcogn.kartlog.ui.common.KartTiles
import com.marcogn.kartlog.ui.common.KartTitle
import com.marcogn.kartlog.ui.common.KartTopBar
import com.marcogn.kartlog.ui.common.OutlinedTitle
import com.marcogn.kartlog.ui.common.ccLabel
import com.marcogn.kartlog.ui.common.kartSky
import com.marcogn.kartlog.ui.common.rankLabel
import com.marcogn.kartlog.ui.common.rememberImageAccentColor
import com.marcogn.kartlog.ui.skin.kartColorsOf
import com.marcogn.kartlog.ui.skin.readableUnderWhiteText
import com.marcogn.kartlog.ui.theme.isKartDarkTheme

@Composable
fun ConsigliamiDetailScreen(
    onBack: () -> Unit,
    onFoodClick: (foodGroupId: String) -> Unit,
    viewModel: ConsigliamiDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    ConsigliamiDetailContent(state, onBack, onFoodClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConsigliamiDetailContent(
    state: ConsigliamiDetailUiState,
    onBack: () -> Unit,
    onFoodClick: (foodGroupId: String) -> Unit,
) {
    Scaffold(
        modifier = Modifier.kartSky(isKartDarkTheme()),
        containerColor = Color.Transparent,
        topBar = {
            KartTopBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EventIcon(name = state.eventName, imageUrl = state.eventImageUrl, size = 40.dp, outlined = true)
                        KartTitle(state.eventName, Modifier.weight(1f, fill = false))
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
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Avviso una volta in cima (fase C1): gli outfit sotto sono possibilità, non certezze.
            item(key = "maybe") {
                KartPanel(colors = KartTiles.Blue, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.consigliami_detail_maybe),
                        style = ConsigliamiBodyStyle,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
            if (state.characters.isEmpty()) {
                item(key = "empty") {
                    OutlinedTitle(
                        stringResource(R.string.consigliami_detail_no_characters),
                        fontSize = 18.sp,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            } else {
                items(state.characters, key = { it.characterId }) { character ->
                    CharacterPanel(character, onFoodClick)
                }
            }
            item(key = "results") { ResultsPanel(state) }
        }
    }
}

/** Una tessera per personaggio, del suo colore come nella sezione Cibi, con una riga per outfit. */
@Composable
private fun CharacterPanel(character: DetailCharacterUi, onFoodClick: (String) -> Unit) {
    val accent = rememberImageAccentColor(character.imageUrl) ?: KartTiles.Gray.base
    val colors = kartColorsOf(accent.readableUnderWhiteText())
    KartPanel(colors = colors, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CharacterAvatar(
                    name = character.name,
                    imageUrl = character.imageUrl,
                    size = 48.dp,
                    modifier = Modifier.border(2.dp, KartInk, CircleShape),
                )
                OutlinedTitle(character.name, Modifier.weight(1f), fontSize = 20.sp, textAlign = TextAlign.Start, maxLines = 2)
                KartCounterPill(stringResource(R.string.consigliami_gain_format, character.gain), colors)
            }
            character.outfits.forEach { OutfitRow(it, colors, onFoodClick) }
        }
    }
}

/** "Vespone – [Barbecue · Spiaggia di Peach]": il cibo è un pulsante che apre la sezione Cibi. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OutfitRow(outfit: DetailOutfitUi, colors: KartColors, onFoodClick: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTitle("${outfit.name} –", fontSize = 18.sp, textAlign = TextAlign.Start, maxLines = 2)
        outfit.foods.forEach { food -> FoodChip(food, colors, onClick = { onFoodClick(food.foodGroupId) }) }
    }
}

@Composable
private fun FoodChip(food: DetailFoodUi, colors: KartColors, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(colors.dark.copy(alpha = 0.85f))
            .border(2.dp, KartInk, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FoodImage(food.name, food.imageUrl, size = 34.dp)
        Column(Modifier.weight(1f, fill = false)) {
            OutlinedTitle(food.name, fontSize = 16.sp, textAlign = TextAlign.Start, maxLines = 2)
            if (food.courseNames.isNotEmpty()) {
                Text(food.courseNames.joinToString(", "), style = ConsigliamiBodyStyle.copy(fontSize = 13.sp, lineHeight = 16.sp))
            }
        }
    }
}

/** Migliori trofei per cilindrata, in sola lettura: si registrano dalla schermata Risultati. */
@Composable
private fun ResultsPanel(state: ConsigliamiDetailUiState) {
    KartPanel(colors = KartTiles.Yellow, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTitle(stringResource(R.string.consigliami_detail_results_title), fontSize = 20.sp, textAlign = TextAlign.Start)
            Cc.entries.forEach { cc ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTitle(ccLabel(cc), Modifier.weight(1f), fontSize = 17.sp, textAlign = TextAlign.Start)
                    OutlinedTitle(
                        state.bestRankByCc[cc]?.let { rankLabel(it) } ?: stringResource(R.string.consigliami_detail_results_none),
                        fontSize = 17.sp,
                        textAlign = TextAlign.End,
                    )
                }
            }
            OutlinedTitle(stringResource(R.string.consigliami_detail_results_hint), fontSize = 14.sp, textAlign = TextAlign.Start, maxLines = 2)
        }
    }
}
