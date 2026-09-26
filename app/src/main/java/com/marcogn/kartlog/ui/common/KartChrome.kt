package com.marcogn.kartlog.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.theme.KartFont
import com.marcogn.kartlog.ui.theme.isKartDarkTheme

// "Cornice" grafica dell'app dal mockup dell'autore (26/09/2026): banner con cielo e pista in alto
// su ogni schermata, tessere colorate a scacchi e footer con la pista in Home. Le immagini sono
// in res/drawable-nodpi (banner, footer, logo e icone della Home, fornite dall'autore).

/** Testo bianco con contorno scuro, come i titoli del mockup (font Lilita One). */
@Composable
fun OutlinedTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    fill: Color = Color.White,
    outline: Color = Color(0xFF1B1B1F),
    textAlign: TextAlign = TextAlign.Center,
    maxLines: Int = 1,
) {
    val base = TextStyle(fontFamily = KartFont, fontSize = fontSize, textAlign = textAlign)
    // Contorno proporzionale al testo (circa un sesto dell'altezza), come nel mockup.
    val strokePx = with(LocalDensity.current) { fontSize.toPx() } * 0.16f
    Box(modifier) {
        Text(
            text,
            style = base.copy(color = outline, drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round)),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text, style = base.copy(color = fill), maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

/** Fila di scacchi bianchi e neri, il bordo del banner nel mockup. */
@Composable
private fun CheckerStrip(modifier: Modifier = Modifier, square: Dp = 6.dp) {
    Canvas(modifier.fillMaxWidth().height(square * 2)) {
        val s = square.toPx()
        var col = 0
        var x = 0f
        while (x < size.width) {
            for (row in 0..1) {
                drawRect(
                    color = if ((col + row) % 2 == 0) Color(0xFF1B1B1F) else Color.White,
                    topLeft = Offset(x, row * s),
                    size = Size(s, s),
                )
            }
            x += s
            col++
        }
    }
}

/**
 * Barra in alto di tutte le schermate: il banner (cielo e pista) che arriva fin sotto la status
 * bar, il logo in Home o il titolo della schermata altrove. Stessi slot di `TopAppBar`, così le
 * schermate cambiano solo il nome del componente.
 */
@Composable
fun KartTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    tall: Boolean = false,
) {
    Box(modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.kart_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier.matchParentSize(),
        )
        Column {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .height(if (tall) 96.dp else 72.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC1E2530)),
                        contentAlignment = Alignment.Center,
                    ) { navigationIcon() }
                    Box(Modifier.weight(1f)) {
                        CompositionLocalProvider(
                            LocalTextStyle provides TextStyle(
                                fontFamily = KartFont,
                                fontSize = 24.sp,
                                color = Color.White,
                                shadow = Shadow(Color(0xCC000000), offset = Offset(0f, 4f), blurRadius = 6f),
                            ),
                        ) { title() }
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x991E2530)),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }
            }
            CheckerStrip()
        }
    }
}

/** Logo "KartLog" del mockup, per la barra in alto della Home. */
@Composable
fun KartLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.kart_logo),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        alignment = Alignment.CenterStart,
        modifier = modifier.height(56.dp),
    )
}

/** Pista in fondo alla Home, sfumata verso l'alto nello sfondo della schermata. */
@Composable
fun KartFooter(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.kart_footer),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(0f to background, 0.45f to background.copy(alpha = 0f))),
        )
        // 96dp di pista più lo spazio della barra di navigazione, così la pista arriva fino in fondo.
        Spacer(Modifier.height(96.dp).navigationBarsPadding())
    }
}

/** Colori di una tessera: base, più chiaro (luce in alto) e più scuro (bordo e pillola). */
data class KartColors(val base: Color, val light: Color, val dark: Color)

/** Sfondo delle tessere del mockup: gradiente, bordo scuro, scacchi tenui negli angoli alti. */
@Composable
fun KartPanel(
    colors: KartColors,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    // Nel tema scuro le tessere "brillano" del loro colore, come nella seconda immagine del mockup.
    val glow = if (isKartDarkTheme()) {
        Modifier.shadow(18.dp, shape, ambientColor = colors.base, spotColor = colors.base)
    } else {
        Modifier.shadow(6.dp, shape)
    }
    Box(
        modifier = modifier
            .then(glow)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(Brush.verticalGradient(listOf(colors.light, colors.base)))
            .border(3.dp, colors.dark, shape),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val s = 12.dp.toPx()
            val tint = Color.White.copy(alpha = 0.13f)
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    if ((row + col) % 2 != 0 || row + col > 4) continue
                    drawRect(tint, Offset(10.dp.toPx() + col * s, 10.dp.toPx() + row * s), Size(s, s))
                    drawRect(tint, Offset(size.width - 10.dp.toPx() - (col + 1) * s, 10.dp.toPx() + row * s), Size(s, s))
                }
            }
        }
        content()
    }
}

/** Icona tonda con anello del colore della tessera (disco e oggetto ritagliati dal mockup). */
@Composable
fun KartBadge(painter: Painter, colors: KartColors, modifier: Modifier = Modifier, size: Dp = 76.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .background(Brush.verticalGradient(listOf(colors.light, colors.dark)), CircleShape)
            .border(2.dp, Color(0xFF1B1B1F), CircleShape)
            .padding(size * 0.1f),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(2.dp, Color(0xFF1B1B1F), CircleShape),
        )
    }
}

/** Pillola scura con il contatore, sotto il titolo delle tessere. */
@Composable
fun KartCounterPill(text: String, colors: KartColors, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.dark.copy(alpha = 0.85f))
            .padding(horizontal = 18.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TextStyle(fontFamily = KartFont, fontSize = 18.sp, color = Color.White))
    }
}
