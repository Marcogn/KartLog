package com.marcogn.kartlog.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size as CoilSize
import com.marcogn.kartlog.data.seed.MapImageDto
import com.marcogn.kartlog.domain.model.MapPointType
import com.marcogn.kartlog.ui.common.KartInk
import com.marcogn.kartlog.ui.common.KartTiles
import com.marcogn.kartlog.ui.theme.KartFont
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private const val MAX_ZOOM = 8f
private const val FOCUS_ZOOM = 4f
private val MarkerRadius = 12.dp
private val MinMarkerRadius = 6.5.dp
private val TouchRadius = 24.dp

/** Mare attorno alla mappa, e sfondo finché l'immagine non è scaricata. */
internal val MapSea = Color(0xFF2E6FB8)

/** Colore del marker di ogni tipo, come l'oggetto nel gioco: pulsante P blu, moneta rosa, pannello giallo. */
internal fun MapPointType.markerColor(): Color = when (this) {
    MapPointType.P_SWITCH -> KartTiles.Blue.base
    MapPointType.MEDALLION -> KartTiles.Pink.base
    MapPointType.QUESTION_PANEL -> KartTiles.Yellow.base
}

/**
 * Posizione e zoom della mappa: [scale] 1 = mappa intera nel riquadro, [offset] = angolo in alto a
 * sinistra della mappa nel riquadro, in pixel. Logica pura (nessuna dipendenza da Compose UI) perché
 * è la parte più facile da sbagliare: testata in `MapViewportTest`.
 */
internal data class MapViewport(val viewport: Size, val image: Size, val scale: Float = 1f, val offset: Offset = Offset.Zero) {
    /** Scala a cui la mappa intera sta nel riquadro. */
    val fit: Float get() = if (image.width <= 0f || image.height <= 0f) 1f else min(viewport.width / image.width, viewport.height / image.height)
    val mapWidth: Float get() = image.width * fit * scale
    val mapHeight: Float get() = image.height * fit * scale

    fun toScreen(x: Float, y: Float): Offset = Offset(offset.x + x * mapWidth, offset.y + y * mapHeight)

    /** Zoom di [factor] tenendo fermo [pivot], poi spostamento di [pan]. */
    fun transformed(pivot: Offset, pan: Offset, factor: Float): MapViewport {
        val newScale = (scale * factor).coerceIn(1f, MAX_ZOOM)
        val k = newScale / scale
        return copy(scale = newScale, offset = pivot - (pivot - offset) * k + pan).clamped()
    }

    /** Centrata sul punto ([x], [y] in frazione della mappa) allo zoom [zoom]. */
    fun centeredOn(x: Float, y: Float, zoom: Float): MapViewport {
        val z = copy(scale = zoom.coerceIn(1f, MAX_ZOOM))
        return z.copy(offset = Offset(viewport.width / 2 - x * z.mapWidth, viewport.height / 2 - y * z.mapHeight)).clamped()
    }

    /** Mai fuori dal riquadro: centrata sull'asse dove è più piccola, bordo contro bordo dove è più grande. */
    fun clamped(): MapViewport {
        fun axis(value: Float, size: Float, room: Float) =
            if (size <= room) (room - size) / 2 else value.coerceIn(room - size, 0f)
        return copy(offset = Offset(axis(offset.x, mapWidth, viewport.width), axis(offset.y, mapHeight, viewport.height)))
    }
}

/** Il punto più vicino a [tap] entro [radius] pixel, tra quelli disegnati. */
internal fun MapViewport.hitTest(points: List<MapPoint>, tap: Offset, radius: Float): MapPoint? =
    points.map { it to toScreen(it.x, it.y) }
        .map { (p, s) -> p to hypot(s.x - tap.x, s.y - tap.y) }
        .filter { it.second <= radius }
        .minByOrNull { it.second }
        ?.first

/**
 * Mappa con zoom (pizzico o doppio tap) e trascinamento. L'immagine si scarica a runtime (URL in
 * `map.json`, mai nell'APK): finché non c'è resta il mare, ma i marker si vedono e si toccano lo stesso.
 * I marker hanno la stessa dimensione a ogni zoom: si disegnano sopra l'immagine, non dentro.
 */
@Composable
internal fun CollectibleMapView(
    image: MapImageDto?,
    points: List<MapPoint>,
    focus: MapPoint?,
    onPointClick: (MapPoint) -> Unit,
    onImageStateChange: (loaded: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val imageSize = Size(image?.width?.toFloat() ?: 1f, image?.height?.toFloat() ?: 1f)
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    // Zoom e posizione sopravvivono alla rotazione: salvati come frazioni, non pixel.
    var scale by rememberSaveable { mutableFloatStateOf(1f) }
    var centerX by rememberSaveable { mutableFloatStateOf(0.5f) }
    var centerY by rememberSaveable { mutableFloatStateOf(0.5f) }
    var focused by rememberSaveable { mutableStateOf(false) }
    var viewport by remember { mutableStateOf(MapViewport(Size.Zero, imageSize)) }

    LaunchedEffect(viewportSize, focus?.id) {
        if (viewportSize == IntSize.Zero) return@LaunchedEffect
        val base = MapViewport(Size(viewportSize.width.toFloat(), viewportSize.height.toFloat()), imageSize)
        viewport = if (!focused && focus != null) {
            focused = true
            base.centeredOn(focus.x, focus.y, FOCUS_ZOOM)
        } else {
            base.centeredOn(centerX, centerY, scale)
        }
    }

    fun update(next: MapViewport) {
        viewport = next
        scale = next.scale
        centerX = ((next.viewport.width / 2 - next.offset.x) / next.mapWidth).coerceIn(0f, 1f)
        centerY = ((next.viewport.height / 2 - next.offset.y) / next.mapHeight).coerceIn(0f, 1f)
    }

    val currentPoints by rememberUpdatedState(points)
    val currentOnClick by rememberUpdatedState(onPointClick)
    val touchRadius = with(density) { TouchRadius.toPx() }

    Box(
        modifier = modifier
            .background(MapSea)
            .onSizeChanged { viewportSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ -> update(viewport.transformed(centroid, pan, zoom)) }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { at ->
                        // Doppio tap: zoom ×2 attorno al dito, oppure di nuovo tutta la mappa se si è al massimo.
                        val factor = if (viewport.scale >= MAX_ZOOM) 1f / viewport.scale else 2f
                        update(viewport.transformed(at, Offset.Zero, factor))
                    },
                    onTap = { at -> viewport.hitTest(currentPoints, at, touchRadius)?.let(currentOnClick) },
                )
            },
    ) {
        if (image != null && viewport.viewport != Size.Zero) {
            val fitWidth = with(density) { (imageSize.width * viewport.fit).toDp() }
            val fitHeight = with(density) { (imageSize.height * viewport.fit).toDp() }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.imageUrl)
                    // Dimensione originale: con lo zoom serve tutto il dettaglio (2580×2322, ~24 MB in memoria).
                    .size(CoilSize.ORIGINAL)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                onSuccess = { onImageStateChange(true) },
                onError = { onImageStateChange(false) },
                modifier = Modifier
                    .size(fitWidth, fitHeight)
                    .graphicsLayer {
                        // Trasformazione al momento del disegno: l'immagine resta nitida anche ingrandita.
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = viewport.scale
                        scaleY = viewport.scale
                        translationX = viewport.offset.x
                        translationY = viewport.offset.y
                    },
            )
        }
        MarkerLayer(viewport, points, focus?.id)
    }
}

@Composable
private fun MarkerLayer(viewport: MapViewport, points: List<MapPoint>, highlightId: String?) {
    val glyphs = rememberMarkerGlyphs()
    Canvas(Modifier.fillMaxSize()) {
        val r = markerRadius(viewport.scale).toPx()
        // I già fatti sotto, così non coprono quelli ancora da fare.
        for (point in points.sortedBy { !it.done }) {
            val at = viewport.toScreen(point.x, point.y)
            if (at.x < -2 * r || at.y < -2 * r || at.x > size.width + 2 * r || at.y > size.height + 2 * r) continue
            drawMarker(point.type, at, r, glyphs, done = point.done, highlighted = point.id == highlightId)
        }
    }
}

/**
 * Raggio dei marker: piccoli con la mappa intera (sono più di 700, altrimenti si coprono tutti),
 * pieni da zoom ×4 in su. Il raggio del tocco resta [TouchRadius].
 */
private fun markerRadius(scale: Float): Dp = lerp(MinMarkerRadius, MarkerRadius, ((scale - 1f) / 3f).coerceIn(0f, 1f))

/** Lettere dei marker, misurate una volta a [MarkerRadius] e scalate al disegno. */
@Composable
internal fun rememberMarkerGlyphs(): Map<String, TextLayoutResult> {
    val measurer = rememberTextMeasurer()
    return remember(measurer) {
        val style = TextStyle(fontFamily = KartFont, fontSize = 14.sp, color = Color.White)
        mapOf(
            "P" to measurer.measure("P", style),
            "?" to measurer.measure("?", style.copy(color = KartInk)),
            "✓" to measurer.measure("✓", style.copy(fontSize = 11.sp)),
        )
    }
}

/**
 * Un marker per tipo, con forma e simbolo diversi oltre al colore: Pulsante P = cerchio blu con la
 * "P", Moneta Peach = moneta rosa con la corona, pannello "?" = quadrato giallo con il "?". I già
 * fatti restano riconoscibili (stessa forma, attenuati) con una spunta verde in alto a destra.
 */
internal fun DrawScope.drawMarker(
    type: MapPointType,
    at: Offset,
    r: Float,
    glyphs: Map<String, TextLayoutResult>,
    done: Boolean = false,
    highlighted: Boolean = false,
) {
    val alpha = if (done) 0.5f else 1f
    val border = max(1.5f, r * 0.18f)
    if (highlighted) {
        drawCircle(Color.White, radius = r * 1.9f, center = at, style = Stroke(r * 0.35f))
        drawCircle(KartInk, radius = r * 2.15f, center = at, style = Stroke(r * 0.15f))
    }
    val color = type.markerColor().copy(alpha = alpha)
    val ink = KartInk.copy(alpha = alpha)
    when (type) {
        MapPointType.QUESTION_PANEL -> {
            val side = r * 1.8f
            val corner = CornerRadius(r * 0.3f)
            val topLeft = Offset(at.x - side / 2, at.y - side / 2)
            drawRoundRect(ink, topLeft - Offset(border, border), Size(side + 2 * border, side + 2 * border), corner)
            drawRoundRect(color, topLeft, Size(side, side), corner)
            drawGlyph(glyphs.getValue("?"), at, r, alpha)
        }
        MapPointType.P_SWITCH -> {
            drawCircle(ink, radius = r + border, center = at)
            drawCircle(color, radius = r, center = at)
            drawGlyph(glyphs.getValue("P"), at, r, alpha)
        }
        MapPointType.MEDALLION -> {
            drawCircle(ink, radius = r + border, center = at)
            drawCircle(color, radius = r, center = at)
            drawCircle(Color.White.copy(alpha = alpha), radius = r * 0.78f, center = at, style = Stroke(max(1f, r * 0.1f)))
            drawPath(crownPath(at, r), Color.White.copy(alpha = alpha))
        }
    }
    if (done) {
        val c = at + Offset(r * 0.8f, -r * 0.8f)
        val badge = r * 0.55f
        drawCircle(KartInk, radius = badge + border * 0.6f, center = c)
        drawCircle(KartTiles.Green.base, radius = badge, center = c)
        drawGlyph(glyphs.getValue("✓"), c, badge * 1.6f, 1f)
    }
}

/** Disegna la lettera misurata a [MarkerRadius] scalata al raggio [r], centrata in [at]. */
private fun DrawScope.drawGlyph(glyph: TextLayoutResult, at: Offset, r: Float, alpha: Float) {
    val k = r / MarkerRadius.toPx()
    scale(k, pivot = at) {
        drawText(glyph, topLeft = Offset(at.x - glyph.size.width / 2f, at.y - glyph.size.height / 2f), alpha = alpha)
    }
}

/** Corona stilizzata (tre punte) come sulle monete di Peach. */
private fun crownPath(at: Offset, r: Float): Path = Path().apply {
    fun p(x: Float, y: Float) = Offset(at.x + x * r, at.y + y * r)
    val points = listOf(p(-0.5f, 0.32f), p(-0.55f, -0.3f), p(-0.25f, 0f), p(0f, -0.42f), p(0.25f, 0f), p(0.55f, -0.3f), p(0.5f, 0.32f))
    moveTo(points[0].x, points[0].y)
    points.drop(1).forEach { lineTo(it.x, it.y) }
    close()
}
