package com.marcogn.kartlog.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.common.KartBadge
import com.marcogn.kartlog.ui.common.KartColors
import com.marcogn.kartlog.ui.common.KartCounterPill
import com.marcogn.kartlog.ui.common.KartFooter
import com.marcogn.kartlog.ui.common.KartLogo
import com.marcogn.kartlog.ui.common.KartPanel
import com.marcogn.kartlog.ui.common.KartTiles
import com.marcogn.kartlog.ui.common.KartTopBar
import com.marcogn.kartlog.ui.common.kartSky
import com.marcogn.kartlog.ui.theme.isKartDarkTheme
import com.marcogn.kartlog.ui.common.OutlinedTitle

private data class HomeTile(
    @DrawableRes val icon: Int,
    val title: String,
    val counter: String,
    val colors: KartColors,
    val onClick: () -> Unit,
)

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
    HomeContent(state, onMenuClick, onSkinClick, onMedallionsClick, onPSwitchesClick, onConsigliamiClick, onResultsClick)
}

/** Home senza ViewModel: separata per poterla disegnare nei test con uno stato fisso. */
@Composable
internal fun HomeContent(
    state: HomeUiState,
    onMenuClick: () -> Unit,
    onSkinClick: () -> Unit,
    onMedallionsClick: () -> Unit,
    onPSwitchesClick: () -> Unit,
    onConsigliamiClick: () -> Unit,
    onResultsClick: () -> Unit,
) {
    val notAvailable = stringResource(R.string.home_counter_not_available)

    // "n/d" se il totale è 0 (SPEC §8, fase 3): i pulsanti P possono mancare finché la fase 2
    // di seedgen non gira, un vero "0 / 0" non avrebbe senso da mostrare.
    @Composable
    fun counter(owned: Int, total: Int): String =
        if (total == 0) notAvailable else stringResource(R.string.home_counter_format, owned, total)

    // Icone dal mockup dell'autore, una per funzione: il trofeo resta a Risultati, la stella a
    // Consigliami anche dopo lo scambio di posizione tra le due.
    val tiles = listOf(
        HomeTile(R.drawable.home_banana, stringResource(R.string.home_option_skin_title),
            counter(state.ownedOutfits, state.totalOutfits), KartTiles.Orange, onSkinClick),
        HomeTile(R.drawable.home_coin, stringResource(R.string.home_option_medallions_title),
            counter(state.collectedMedallions, state.totalMedallions), KartTiles.Blue, onMedallionsClick),
        HomeTile(R.drawable.home_mushroom, stringResource(R.string.home_option_pswitches_title),
            counter(state.completedPSwitches, state.totalPSwitches), KartTiles.Pink, onPSwitchesClick),
        HomeTile(R.drawable.home_trophy, stringResource(R.string.home_option_results_title),
            counter(state.eventsWithResult, state.totalEvents), KartTiles.Yellow, onResultsClick),
    )

    Scaffold(
        modifier = Modifier.kartSky(isKartDarkTheme()),
        containerColor = Color.Transparent,
        topBar = {
            KartTopBar(
                title = { KartLogo() },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_menu))
                    }
                },
                tall = true,
            )
        },
    ) { padding ->
        // Solo il margine in alto: il footer scende fin sotto la barra di navigazione.
        Column(modifier = Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize()) {
            // Righe normali invece di una griglia lazy (sono solo quattro tessere): con
            // IntrinsicSize.Min le due tessere di una riga prendono l'altezza della più alta, così
            // un titolo su due righe allunga entrambe invece di tagliare il contatore.
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                tiles.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        row.forEach { tile -> HomeTileCard(tile, Modifier.weight(1f).fillMaxHeight()) }
                    }
                }
                // Card larga a tutta riga (SPEC §2.1): Consigliami non ha un contatore, ha bisogno
                // di una riga di spiegazione per far capire cosa fa.
                ConsigliamiCard(onClick = onConsigliamiClick)
            }
            KartFooter()
        }
    }
}

@Composable
private fun HomeTileCard(tile: HomeTile, modifier: Modifier) {
    KartPanel(
        colors = tile.colors,
        // Altezza minima come la vecchia tessera quasi quadrata; cresce se il titolo va a capo.
        modifier = modifier.heightIn(min = 180.dp),
        onClick = tile.onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KartBadge(painterResource(tile.icon), tile.colors)
            // Due righe se serve ("Peach Medallions" in inglese non ci sta in una), mai i puntini.
            OutlinedTitle(tile.title, fontSize = 22.sp, maxLines = 2)
            KartCounterPill(tile.counter, tile.colors)
        }
    }
}

@Composable
private fun ConsigliamiCard(onClick: () -> Unit) {
    KartPanel(colors = KartTiles.Green, modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KartBadge(painterResource(R.drawable.home_star), KartTiles.Green, size = 84.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTitle(
                    stringResource(R.string.home_option_consigliami_title),
                    fontSize = 26.sp,
                    textAlign = TextAlign.Start,
                )
                Text(
                    stringResource(R.string.home_option_consigliami_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        shadow = Shadow(Color(0x99000000), offset = Offset(0f, 2f), blurRadius = 3f),
                    ),
                )
            }
        }
    }
}
