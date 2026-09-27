package com.marcogn.kartlog.ui.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.marcogn.kartlog.ui.common.KartColors
import com.marcogn.kartlog.ui.common.KartInk
import com.marcogn.kartlog.ui.theme.isKartDarkTheme

// Card "polaroid" di Personaggi e del dettaglio outfit (richiesta dell'autore, 26/09/2026).
// Normale: cornice bianca e cartellino del colore del personaggio. Invertita: tutta la cornice del
// colore del personaggio, testo direttamente sopra.

/** Colore della cornice bianca (grigio scuro nel tema scuro). */
@Composable
internal fun polaroidFrameColor(): Color = if (isKartDarkTheme()) Color(0xFF2A2D34) else Color.White

/**
 * Il colore del personaggio scurito quanto basta perché un testo bianco senza contorno si legga
 * (Peach o Bowser, chiari, altrimenti non si leggerebbero).
 */
internal fun Color.readableUnderWhiteText(): Color {
    var color = this
    repeat(8) { if (color.luminance() > 0.28f) color = lerp(color, Color.Black, 0.15f) }
    return color
}

/** Tre toni per pillole e anelli a partire dal colore del personaggio. */
internal fun kartColorsOf(color: Color) =
    KartColors(base = color, light = lerp(color, Color.White, 0.25f), dark = lerp(color, Color.Black, 0.45f))

@Composable
internal fun Polaroid(
    frame: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    image: @Composable BoxScope.() -> Unit,
    caption: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(3.dp, shape)
            .clip(shape)
            .background(frame)
            .border(2.5.dp, KartInk, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(content = image)
        caption()
    }
}

/** Cartellino colorato con bordo nero, sotto l'immagine della polaroid normale. */
@Composable
internal fun PolaroidLabel(color: Color, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color)
            .border(1.5.dp, KartInk, shape)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}
