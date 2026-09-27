package com.marcogn.kartlog.ui.skin

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.data.local.dao.OutfitProgress
import com.marcogn.kartlog.domain.model.localizedName
import com.marcogn.kartlog.ui.common.CharacterPortrait
import com.marcogn.kartlog.ui.common.KartCounterPill
import com.marcogn.kartlog.ui.common.KartInk
import com.marcogn.kartlog.ui.common.KartTiles
import com.marcogn.kartlog.ui.common.KartTitle
import com.marcogn.kartlog.ui.common.KartTopBar
import com.marcogn.kartlog.ui.common.OutlinedTitle
import com.marcogn.kartlog.ui.common.kartSky
import com.marcogn.kartlog.ui.common.rememberImageAccentColor
import com.marcogn.kartlog.ui.theme.KartFont
import com.marcogn.kartlog.ui.theme.isKartDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinDetailScreen(
    onBack: () -> Unit,
    viewModel: SkinDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.kartSky(isKartDarkTheme()),
        containerColor = Color.Transparent,
        topBar = {
            KartTopBar(
                title = { KartTitle(state.characterName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        val accent = rememberImageAccentColor(state.characterImageUrl) ?: KartTiles.Gray.base
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Il nome del personaggio è già il titolo grande del banner: qui contatore e sblocco.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KartCounterPill(
                    stringResource(R.string.home_counter_format, state.ownedCount, state.totalCount),
                    kartColorsOf(accent.readableUnderWhiteText()),
                )
                Spacer(Modifier.weight(1f))
                // Solo i piloti da sbloccare: quelli di base sono sempre disponibili.
                if (state.canToggleUnlock) {
                    Text(
                        stringResource(R.string.skin_unlock_switch),
                        style = TextStyle(fontFamily = KartFont, fontSize = 16.sp),
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Switch(checked = state.unlocked, onCheckedChange = viewModel::onUnlockToggled)
                }
            }
            state.unlockCriteria?.let { criterion ->
                Text(
                    stringResource(R.string.skin_unlock_how) + ": " + criterion,
                    style = TextStyle(fontFamily = KartFont, fontSize = 15.sp),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            // Righe normali invece di una griglia lazy (al massimo una decina di outfit): con
            // IntrinsicSize.Min le polaroid della stessa riga si allungano insieme, così nomi e
            // cibi si leggono per intero senza tagli.
            BoxWithConstraints {
                // Due per riga su un telefono: gli outfit spesso cambiano solo nei dettagli.
                val columns = (maxWidth / 170.dp).toInt().coerceAtLeast(2)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.outfits.chunked(columns).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            row.forEach { outfit ->
                                OutfitCard(
                                    outfit,
                                    accent = accent,
                                    onToggle = { owned -> viewModel.onOutfitToggled(outfit.outfitId, owned) },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Outfit a polaroid. Quello di default è come la card della pagina Personaggi (cornice bianca,
 * cartellino colorato); gli altri sono "al contrario": cornice del colore del personaggio, nome
 * bianco contornato e cibi in testo semplice.
 */
@Composable
private fun OutfitCard(outfit: OutfitProgress, accent: Color, onToggle: (Boolean) -> Unit, modifier: Modifier) {
    val name = outfit.outfitName?.let { localizedName(it, outfit.outfitNameIt) }
        ?: stringResource(R.string.skin_default_outfit_name)
    val foodLabel = outfit.localizedFoodGroups ?: stringResource(R.string.skin_unknown_food)
    // Grigio = non ancora ottenuto, colorato = ottenuto (regola dell'autore, 27/09/2026): anche la
    // cornice passa al grigio, così la differenza si vede senza guardare la spunta.
    val owned = outfit.owned || outfit.isDefault
    val frame = when {
        outfit.isDefault -> polaroidFrameColor()
        owned -> accent.readableUnderWhiteText()
        else -> KartTiles.Gray.dark
    }
    val foodStyle = TextStyle(fontFamily = KartFont, fontSize = 13.sp, lineHeight = 16.sp, textAlign = TextAlign.Center)
    Polaroid(
        frame = frame,
        // L'outfit di default è sempre posseduto e non spuntabile (SPEC §2.3): la card non reagisce al tap.
        onClick = if (outfit.isDefault) null else ({ onToggle(!outfit.owned) }),
        modifier = modifier,
        image = {
            CharacterPortrait(
                name = name,
                imageUrl = outfit.imageUrl,
                dimmed = !owned,
                modifier = Modifier.fillMaxWidth().border(1.5.dp, KartInk, RoundedCornerShape(12.dp)),
            )
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
        },
    ) {
        if (outfit.isDefault) {
            PolaroidLabel(accent) { OutlinedTitle(name, fontSize = 17.sp, maxLines = 3) }
            Text(foodLabel, style = foodStyle, color = MaterialTheme.colorScheme.onSurface)
        } else {
            OutlinedTitle(name, fontSize = 17.sp, maxLines = 3)
            Text(foodLabel, style = foodStyle, color = Color.White)
        }
    }
}
