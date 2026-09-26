package com.marcogn.kartlog.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.common.KartBadge
import com.marcogn.kartlog.ui.common.KartColors
import com.marcogn.kartlog.ui.common.KartLogo
import com.marcogn.kartlog.ui.common.KartTiles
import com.marcogn.kartlog.ui.common.OutlinedTitle
import com.marcogn.kartlog.ui.common.drawCornerCheckers

// Drawer nello stile della Home (richiesta dell'autore, 26/09/2026): rosso pieno con gli scacchi
// tenui delle tessere, logo in alto, voci nel font dei titoli con le stesse icone tonde della Home.
// Home e Impostazioni non hanno un'icona nel mockup: sono disegnate (simbolo Material su disco).

private val BadgeSize = 52.dp

/** Contenuto del drawer: ogni voce chiama [onNavigate] con la sua destinazione. */
@Composable
fun KartDrawerSheet(onNavigate: (Destination) -> Unit) {
    val red = KartTiles.Red
    ModalDrawerSheet(drawerContainerColor = red.base, drawerContentColor = Color.White) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                val tint = Color.White.copy(alpha = 0.10f)
                val square = 18.dp.toPx()
                val inset = 12.dp.toPx()
                drawCornerCheckers(Offset(size.width - inset, inset), -1f, 1f, square, tint)
                drawCornerCheckers(Offset(inset, size.height - inset), 1f, -1f, square, tint)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                KartLogo(Modifier.padding(start = 8.dp, bottom = 12.dp))
                DrawerItem(R.string.drawer_home, { onNavigate(Destination.Home) }) {
                    KartBadge(Icons.Filled.Home, red, KartTiles.Ink, size = BadgeSize)
                }
                ImageItem(R.string.drawer_skin, R.drawable.home_banana, KartTiles.Orange) { onNavigate(Destination.Skin) }
                ImageItem(R.string.drawer_medallions, R.drawable.home_coin, KartTiles.Blue) {
                    onNavigate(Destination.PeachMedallions)
                }
                ImageItem(R.string.drawer_pswitches, R.drawable.home_mushroom, KartTiles.Pink) {
                    onNavigate(Destination.PSwitches)
                }
                ImageItem(R.string.drawer_consigliami, R.drawable.home_star, KartTiles.Green) {
                    onNavigate(Destination.Consigliami)
                }
                ImageItem(R.string.drawer_results, R.drawable.home_trophy, KartTiles.Yellow) {
                    onNavigate(Destination.Results)
                }
                Box(
                    Modifier
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(red.dark.copy(alpha = 0.6f)),
                )
                DrawerItem(R.string.drawer_settings, { onNavigate(Destination.Settings) }) {
                    KartBadge(Icons.Filled.Settings, KartTiles.Gray, KartTiles.Gray, size = BadgeSize)
                }
            }
        }
    }
}

@Composable
private fun ImageItem(@StringRes label: Int, @DrawableRes icon: Int, colors: KartColors, onClick: () -> Unit) {
    DrawerItem(label, onClick) { KartBadge(painterResource(icon), colors, size = BadgeSize) }
}

@Composable
private fun DrawerItem(@StringRes label: Int, onClick: () -> Unit, badge: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        badge()
        OutlinedTitle(stringResource(label), fontSize = 22.sp, textAlign = TextAlign.Start)
    }
}
