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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.text.rememberTextMeasurer
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

/** Nero dei contorni del mockup. */
val KartInk = Color(0xFF1B1B1F)

// "Cornice" grafica dell'app dal mockup dell'autore (26/09/2026): banner con cielo e pista in alto
// su ogni schermata, tessere colorate a scacchi e footer con la pista in Home. Le immagini sono
// in res/drawable-nodpi (banner, footer, logo e icone della Home, fornite dall'autore).

/**
 * Testo bianco con contorno scuro, come i titoli del mockup (font Lilita One). Con [minFontSize]
 * più piccolo di [fontSize] il testo si rimpicciolisce fino a stare su una riga, prima di arrivare
 * ai puntini.
 */
@Composable
fun OutlinedTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    fill: Color = Color.White,
    outline: Color = KartInk,
    textAlign: TextAlign = TextAlign.Center,
    maxLines: Int = 1,
    minFontSize: TextUnit = fontSize,
) {
    if (minFontSize == fontSize) {
        OutlinedTitleText(text, modifier, fontSize, fill, outline, textAlign, maxLines)
    } else {
        ShrinkingOutlinedText(text, modifier, fontSize, minFontSize, fill, outline)
    }
}

/**
 * Titolo su una riga che si rimpicciolisce fino a [minFontSize] per starci. Misura e disegna il
 * testo a mano in un `Layout`, non con `BoxWithConstraints`: quello è una SubcomposeLayout e fa
 * crashare l'app dentro i genitori che chiedono le misure intrinseche (le voci di `DropdownMenu`,
 * le righe con `IntrinsicSize`).
 */
@Composable
private fun ShrinkingOutlinedText(
    text: String,
    modifier: Modifier,
    fontSize: TextUnit,
    minFontSize: TextUnit,
    fill: Color,
    outline: Color,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Scritto in misura e letto nel disegno dello stesso frame: non serve che sia uno State.
    val holder = remember { arrayOfNulls<TextLayoutResult>(1) }
    Layout(
        content = {},
        modifier = modifier
            .semantics { this.text = AnnotatedString(text) }
            .drawBehind {
                val layout = holder[0] ?: return@drawBehind
                drawOutlinedText(layout, fill, outline)
            },
    ) { _, constraints ->
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        fun measureAt(size: Float) = measurer.measure(
            text = text,
            style = TextStyle(fontFamily = KartFont, fontSize = size.sp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            constraints = Constraints(maxWidth = maxWidth),
            density = density,
        )
        var size = fontSize.value
        var result = measureAt(size)
        while (size > minFontSize.value && result.hasVisualOverflow) {
            size -= 0.5f
            result = measureAt(size)
        }
        holder[0] = result
        layout(
            constraints.constrainWidth(result.size.width),
            constraints.constrainHeight(result.size.height),
        ) {}
    }
}

/** Contorno e poi riempimento dello stesso testo già misurato. */
internal fun DrawScope.drawOutlinedText(layout: TextLayoutResult, fill: Color, outline: Color) {
    val strokePx = layout.layoutInput.style.fontSize.toPx() * 0.16f
    drawText(layout, color = outline, drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round))
    // Fill esplicito: senza, il paragrafo tiene lo Stroke del disegno precedente e anche il
    // riempimento esce come contorno (lettere "vuote", bug visto dall'autore).
    drawText(layout, color = fill, drawStyle = Fill)
}

@Composable
private fun OutlinedTitleText(
    text: String,
    modifier: Modifier,
    fontSize: TextUnit,
    fill: Color,
    outline: Color,
    textAlign: TextAlign,
    maxLines: Int,
) {
    // Interlinea stretta: sui titoli a due righe ("Peach Medallions") lascia spazio al contatore.
    val base = TextStyle(fontFamily = KartFont, fontSize = fontSize, lineHeight = fontSize * 1.05f, textAlign = textAlign)
    // Contorno proporzionale al testo (circa un sesto dell'altezza), come nel mockup.
    val strokePx = with(LocalDensity.current) { fontSize.toPx() } * 0.16f
    Box(modifier) {
        // Il contorno è solo disegno: fuori dalla semantica, altrimenti TalkBack legge il testo due volte.
        Text(
            text,
            modifier = Modifier.clearAndSetSemantics {},
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
                    color = if ((col + row) % 2 == 0) KartInk else Color.White,
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

/** Pista in fondo alla Home, che sfuma verso l'alto nel cielo dello sfondo. */
@Composable
fun KartFooter(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.kart_footer),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .matchParentSize()
                // Maschera alfa (DstIn) invece di un gradiente colorato sopra: la parte alta della
                // pista diventa trasparente e lascia vedere il cielo, qualunque colore abbia lì.
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0f to Color.Transparent, 0.5f to Color.Black),
                        blendMode = BlendMode.DstIn,
                    )
                },
        )
        // 96dp di pista più lo spazio della barra di navigazione, così la pista arriva fino in fondo.
        Spacer(Modifier.height(96.dp).navigationBarsPadding())
    }
}

/**
 * Sfondo di tutte le schermate: cielo sfumato con nuvole, come ai lati del mockup. Disegnato, non
 * un'immagine, così si adatta a ogni altezza; di notte nel tema scuro. [faded] lo attenua. Va con
 * `containerColor = Color.Transparent` sullo `Scaffold`.
 */
fun Modifier.kartSky(dark: Boolean, faded: Boolean = false): Modifier = drawBehind {
    val colors = if (dark) {
        listOf(Color(0xFF0B1631), Color(0xFF15264A), Color(0xFF1E3158))
    } else {
        listOf(Color(0xFF8ED3FF), Color(0xFFC4E9FF), Color(0xFFEAF7FF))
    }
    drawRect(Brush.verticalGradient(colors))
    // Tutte le nuvole in un unico livello opaco, reso trasparente solo alla fine: se ogni forma
    // fosse semitrasparente, dove si sovrappongono si vedrebbero i "palloni" (tema scuro).
    val clouds = listOf(
        Triple(0.08f, 0.18f, 0.34f), Triple(0.88f, 0.30f, 0.30f), Triple(0.18f, 0.52f, 0.28f),
        Triple(0.84f, 0.68f, 0.36f), Triple(0.30f, 0.86f, 0.30f),
    ) // (x, y, larghezza) in frazioni dello schermo: poche nuvole ai lati, lontane dal centro.
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { alpha = if (dark) 0.06f else 0.75f })
        clouds.forEach { (x, y, w) -> drawCloud(Offset(size.width * x, size.height * y), size.width * w, Color.White) }
        canvas.restore()
    }
    // Velo sopra cielo e nuvole, per le liste fitte di testo (Risultati): il cielo resta, attenuato.
    if (faded) drawRect(if (dark) Color(0x990B1631) else Color(0xA6FFFFFF))
}

/** Nuvola da cartone: base arrotondata con tre gobbe, centrata su [center]. */
private fun DrawScope.drawCloud(center: Offset, width: Float, color: Color) {
    val h = width * 0.28f
    drawRoundRect(color, Offset(center.x - width / 2, center.y - h / 2), Size(width, h), CornerRadius(h / 2))
    drawCircle(color, h * 0.75f, Offset(center.x - width * 0.18f, center.y - h * 0.35f))
    drawCircle(color, h * 0.95f, Offset(center.x + width * 0.05f, center.y - h * 0.55f))
    drawCircle(color, h * 0.6f, Offset(center.x + width * 0.26f, center.y - h * 0.25f))
}

/** Colori di una tessera: base, più chiaro (luce in alto) e più scuro (bordo e pillola). */
data class KartColors(val base: Color, val light: Color, val dark: Color)

/** Colori delle tessere, presi dal mockup dell'autore (26/09/2026), condivisi da Home e drawer. */
object KartTiles {
    val Orange = KartColors(Color(0xFFF0561D), Color(0xFFFF7B3A), Color(0xFF9E2A0A))
    val Blue = KartColors(Color(0xFF1A8CE8), Color(0xFF3FA9FF), Color(0xFF0B4F97))
    val Pink = KartColors(Color(0xFFE84BA0), Color(0xFFFF72BE), Color(0xFF8E1A5B))
    val Yellow = KartColors(Color(0xFFFFB81C), Color(0xFFFFD34D), Color(0xFFA86A00))
    val Green = KartColors(Color(0xFF2EA83A), Color(0xFF4CCB55), Color(0xFF14621B))
    /** Rosso del drawer, un po' più scuro dell'arancio di Personaggi (richiesta dell'autore). */
    val Red = KartColors(Color(0xFFC9361A), Color(0xFFE0512B), Color(0xFF7E1C08))
    val Gray = KartColors(Color(0xFF7D8590), Color(0xFFB4BCC6), Color(0xFF4A5059))
    /** Disco scuro delle icone del mockup, per quelle disegnate. */
    val Ink = KartColors(Color(0xFF1B1B1F), Color(0xFF3A3D45), Color(0xFF16171B))
}

/**
 * Scacchi tenui a triangolo che partono da un angolo ([dirX]/[dirY] = ±1 dicono verso dove), come
 * negli angoli delle tessere del mockup.
 */
fun DrawScope.drawCornerCheckers(origin: Offset, dirX: Float, dirY: Float, square: Float, tint: Color) {
    for (row in 0 until 4) {
        for (col in 0 until 4) {
            if ((row + col) % 2 != 0 || row + col > 4) continue
            val x = if (dirX > 0) origin.x + col * square else origin.x - (col + 1) * square
            val y = if (dirY > 0) origin.y + row * square else origin.y - (row + 1) * square
            drawRect(tint, Offset(x, y), Size(square, square))
        }
    }
}

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
            val tint = Color.White.copy(alpha = 0.13f)
            val inset = 10.dp.toPx()
            drawCornerCheckers(Offset(inset, inset), 1f, 1f, 12.dp.toPx(), tint)
            drawCornerCheckers(Offset(size.width - inset, inset), -1f, 1f, 12.dp.toPx(), tint)
        }
        content()
    }
}

/** Anello del colore della tessera attorno a un disco: la cornice delle icone tonde della Home. */
@Composable
private fun KartBadgeRing(colors: KartColors, modifier: Modifier, size: Dp, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .background(Brush.verticalGradient(listOf(colors.light, colors.dark)), CircleShape)
            .border(2.dp, KartInk, CircleShape)
            .padding(size * 0.1f),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Icona tonda con anello del colore della tessera (disco e oggetto ritagliati dal mockup). */
@Composable
fun KartBadge(painter: Painter, colors: KartColors, modifier: Modifier = Modifier, size: Dp = 76.dp) {
    KartBadgeRing(colors, modifier, size) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(2.dp, KartInk, CircleShape),
        )
    }
}

/**
 * Stessa icona tonda, disegnata invece che ritagliata dal mockup: un simbolo Material bianco su
 * un disco sfumato (Home e Impostazioni nel drawer, che nel mockup non hanno un'icona).
 */
@Composable
fun KartBadge(icon: ImageVector, colors: KartColors, disc: KartColors, modifier: Modifier = Modifier, size: Dp = 76.dp) {
    KartBadgeRing(colors, modifier, size) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(disc.light, disc.dark)), CircleShape)
                .border(2.dp, KartInk, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.fillMaxSize(0.62f))
        }
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

/** Titolo grande delle schermate nel banner: bianco con contorno nero, rimpicciolito se è lungo. */
@Composable
fun KartTitle(text: String, modifier: Modifier = Modifier) {
    OutlinedTitle(text, modifier, fontSize = 30.sp, textAlign = TextAlign.Start, minFontSize = 20.sp)
}

/**
 * Pulsantone rosso delle scelte (filtri, cilindrate), al posto dei chip Material: rosso pieno con
 * bordo nero e testo bianco contornato come nel drawer. Quello scelto è più chiaro, con un anello
 * bianco interno; gli altri restano rossi, più scuri.
 */
@Composable
fun KartChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val red = KartTiles.Red
    val shape = RoundedCornerShape(14.dp)
    val inner = RoundedCornerShape(11.dp)
    Box(
        modifier = modifier
            .shadow(if (selected) 4.dp else 1.dp, shape)
            .clip(shape)
            .background(
                if (selected) Brush.verticalGradient(listOf(red.light, red.base))
                else Brush.verticalGradient(listOf(red.base, red.dark)),
            )
            .border(2.5.dp, KartInk, shape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(2.5.dp)
            .then(if (selected) Modifier.border(2.dp, Color.White, inner) else Modifier)
            .heightIn(min = 40.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            OutlinedTitle(text, Modifier.weight(1f, fill = false), fontSize = 17.sp, minFontSize = 11.sp)
            trailing?.invoke()
        }
    }
}

/**
 * Menu a tendina con lo stesso pulsantone: mostra la scelta corrente e apre le voci, anch'esse
 * pulsantoni rossi.
 */
@Composable
fun <T> KartDropdown(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        KartChoiceButton(
            text = label(selected),
            selected = true,
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            trailing = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White) },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color.Transparent,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                options.forEach { option ->
                    KartChoiceButton(
                        text = label(option),
                        selected = option == selected,
                        onClick = { onSelected(option); expanded = false },
                        modifier = Modifier.width(220.dp),
                    )
                }
            }
        }
    }
}

/**
 * Schede "a cartella" (Risultati, una per cilindrata): quella scelta è rossa chiara, più alta e
 * attaccata al riquadro del contenuto sotto, bordato dello stesso rosso; le altre più basse e
 * scure. Il contenuto va in [content], dentro il riquadro.
 */
@Composable
fun <T> KartTabs(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    tabTrailing: @Composable (T) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val red = KartTiles.Red
    val panelShape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
    val panelColor = if (isKartDarkTheme()) Color(0xCC15264A) else Color(0xB3FFFFFF)
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                val tabShape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isSelected) 50.dp else 42.dp)
                        .clip(tabShape)
                        .background(
                            if (isSelected) Brush.verticalGradient(listOf(red.light, red.base))
                            else Brush.verticalGradient(listOf(red.base, red.dark)),
                        )
                        .selectable(selected = isSelected, onClick = { onSelected(option) }, role = Role.Tab)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        OutlinedTitle(
                            label(option),
                            Modifier.weight(1f, fill = false),
                            fontSize = if (isSelected) 19.sp else 16.sp,
                            minFontSize = 11.sp,
                        )
                        tabTrailing(option)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(panelShape)
                .background(panelColor)
                .border(3.dp, red.base, panelShape),
            content = content,
        )
    }
}

/**
 * Popup del gioco (richiesta dell'autore, 27/09/2026): rosso pieno con gli scacchi più chiari su
 * tutto lo sfondo, bordo nero, titolo e testi bianchi contornati. La X in alto a destra chiude
 * senza fare nulla, come il tap fuori o il tasto Indietro.
 */
@Composable
fun KartPopup(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val red = KartTiles.Red
    val shape = RoundedCornerShape(22.dp)
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, shape)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(red.light, red.base)))
                .drawBehind {
                    // Scacchiera tenue su tutto il fondo.
                    val s = 18.dp.toPx()
                    val tint = Color.White.copy(alpha = 0.10f)
                    var row = 0
                    var y = 0f
                    while (y < size.height) {
                        var col = 0
                        var x = 0f
                        while (x < size.width) {
                            if ((row + col) % 2 == 0) drawRect(tint, Offset(x, y), Size(s, s))
                            x += s
                            col++
                        }
                        y += s
                        row++
                    }
                }
                .border(3.dp, KartInk, shape),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Spazio a destra per la X, così un titolo lungo non ci finisce sotto.
                OutlinedTitle(title, Modifier.padding(horizontal = 36.dp), fontSize = 26.sp, maxLines = 2)
                content()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KartInk)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close), tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** Testo bianco contornato su più righe, per il corpo dei popup. */
@Composable
fun KartPopupText(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Center) {
    OutlinedTitle(text, modifier, fontSize = 19.sp, textAlign = textAlign, maxLines = 8)
}

/** Piccola "i" tonda che apre un popup informativo. */
@Composable
fun KartInfoButton(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(2.dp, KartInk, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text("i", style = TextStyle(fontFamily = KartFont, fontSize = 14.sp, color = KartInk))
    }
}

/** Etichetta bianca contornata e interruttore, per i popup: verde acceso, rosso scuro spento. */
@Composable
fun KartSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OutlinedTitle(label, fontSize = 22.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = KartTiles.Green.base,
                checkedBorderColor = KartInk,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = KartTiles.Red.dark,
                uncheckedBorderColor = KartInk,
            ),
        )
    }
}
