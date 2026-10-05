package com.marcogn.kartlog.ui.food

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.data.local.dao.FoodOutfitRow
import com.marcogn.kartlog.domain.model.BoostLevel
import com.marcogn.kartlog.domain.model.localizedName
import com.marcogn.kartlog.ui.common.CharacterAvatar
import com.marcogn.kartlog.ui.common.CharacterPortrait
import com.marcogn.kartlog.ui.common.FoodImage
import com.marcogn.kartlog.ui.common.KartInk
import com.marcogn.kartlog.ui.common.KartPanel
import com.marcogn.kartlog.ui.common.KartTiles
import com.marcogn.kartlog.ui.common.KartTitle
import com.marcogn.kartlog.ui.common.KartTopBar
import com.marcogn.kartlog.ui.common.OutlinedTitle
import com.marcogn.kartlog.ui.common.kartSky
import com.marcogn.kartlog.ui.common.rememberImageAccentColor
import com.marcogn.kartlog.ui.skin.Polaroid
import com.marcogn.kartlog.ui.skin.kartColorsOf
import com.marcogn.kartlog.ui.skin.readableUnderWhiteText
import com.marcogn.kartlog.ui.theme.KartFont
import com.marcogn.kartlog.ui.theme.isKartDarkTheme

private val BodyStyle = TextStyle(fontFamily = KartFont, fontSize = 15.sp, lineHeight = 19.sp)

/** Dettaglio di un gruppo di cibo: varianti, dove trovarlo e gli outfit che sblocca, per personaggio. */
@Composable
fun FoodDetailScreen(onBack: () -> Unit, viewModel: FoodDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    FoodDetailContent(state, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FoodDetailContent(state: FoodDetailUiState, onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.kartSky(isKartDarkTheme()),
        containerColor = Color.Transparent,
        topBar = {
            KartTopBar(
                title = { KartTitle(state.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.variants.forEach { VariantRow(it) }

            SectionTitle(stringResource(R.string.food_where))
            if (state.sections.isEmpty()) {
                BodyText(stringResource(R.string.food_no_stands))
            } else {
                state.sections.forEach { StandSectionPanel(it) }
            }

            SectionTitle(stringResource(R.string.food_outfits))
            if (state.characters.isEmpty()) {
                BodyText(stringResource(R.string.food_no_outfits_given))
            }
            // Un cibo dà al massimo un outfit per personaggio (verificato sul seed): due pannelli per riga.
            state.characters.chunked(OUTFIT_COLUMNS).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { CharacterOutfitsPanel(it, Modifier.weight(1f).fillMaxHeight()) }
                    repeat(OUTFIT_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (state.withoutOutfit.isNotEmpty()) {
                BodyText(stringResource(R.string.food_without_outfit, state.withoutOutfit.joinToString(", ")))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    OutlinedTitle(text, fontSize = 22.sp, textAlign = TextAlign.Start, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}

@Composable
private fun BodyText(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(text, style = BodyStyle, color = color, modifier = modifier.padding(horizontal = 4.dp))
}

@Composable
private fun VariantRow(variant: FoodVariantUi) {
    KartPanel(colors = KartTiles.Orange, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FoodImage(variant.name, variant.imageUrl, size = 56.dp)
            Column(Modifier.weight(1f)) {
                OutlinedTitle(variant.name, fontSize = 18.sp, textAlign = TextAlign.Start, maxLines = 2)
                Text(
                    boostLabel(variant.boost),
                    style = BodyStyle.copy(fontSize = 14.sp),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun boostLabel(boost: List<String>): String {
    val levels = boost.map {
        when (BoostLevel.entries.firstOrNull { level -> level.name == it }) {
            BoostLevel.SMALL -> stringResource(R.string.food_boost_small)
            BoostLevel.MEDIUM -> stringResource(R.string.food_boost_medium)
            BoostLevel.LARGE -> stringResource(R.string.food_boost_large)
            null -> it
        }
    }
    return stringResource(R.string.food_boost, levels.joinToString(" / "))
}

@Composable
private fun StandSectionPanel(section: FoodStandSection) {
    KartPanel(colors = KartTiles.Blue, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            section.variantName?.let { OutlinedTitle(it, fontSize = 20.sp, textAlign = TextAlign.Start, maxLines = 2) }
            if (section.courses.isNotEmpty()) {
                OutlinedTitle(stringResource(R.string.food_on_courses), fontSize = 17.sp, textAlign = TextAlign.Start)
                section.courses.forEach { course ->
                    Column {
                        Text(course.title, style = BodyStyle.copy(fontSize = 16.sp), color = Color.White)
                        course.places.forEach { place -> Text("• $place", style = BodyStyle, color = Color.White) }
                    }
                }
            }
            if (section.roads.isNotEmpty()) {
                OutlinedTitle(stringResource(R.string.food_on_roads), fontSize = 17.sp, textAlign = TextAlign.Start)
                section.roads.forEach { road -> Text("• $road", style = BodyStyle, color = Color.White) }
            }
        }
    }
}

@Composable
private fun CharacterOutfitsPanel(entry: FoodCharacterOutfits, modifier: Modifier) {
    val accent = rememberImageAccentColor(entry.imageUrl) ?: KartTiles.Gray.base
    KartPanel(colors = kartColorsOf(accent.readableUnderWhiteText()), modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CharacterAvatar(entry.name, imageUrl = entry.imageUrl, size = 40.dp)
                OutlinedTitle(entry.name, fontSize = 18.sp, textAlign = TextAlign.Start, maxLines = 2)
            }
            entry.outfits.forEach { OutfitPolaroid(it, accent, Modifier) }
        }
    }
}

private const val OUTFIT_COLUMNS = 2

/** Polaroid piccola di un outfit: grigia se non ce l'hai, del colore del personaggio se ce l'hai. */
@Composable
private fun OutfitPolaroid(outfit: FoodOutfitRow, accent: Color, modifier: Modifier) {
    val name = outfit.name?.let { localizedName(it, outfit.nameIt) } ?: stringResource(R.string.skin_default_outfit_name)
    val frame = if (outfit.owned) accent.readableUnderWhiteText() else KartTiles.Gray.dark
    Polaroid(
        frame = frame,
        onClick = null,
        modifier = modifier,
        image = {
            CharacterPortrait(
                name = name,
                imageUrl = outfit.imageUrl,
                dimmed = !outfit.owned,
                modifier = Modifier.fillMaxWidth().border(1.5.dp, KartInk, RoundedCornerShape(12.dp)),
            )
        },
    ) {
        OutlinedTitle(name, fontSize = 14.sp, maxLines = 3)
    }
}
