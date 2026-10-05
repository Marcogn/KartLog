package com.marcogn.kartlog.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.min
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Immagini dal CDN di Super Mario Wiki (URL nel seed, scaricate a runtime: mai nell'APK). Finché
// non sono caricate, o se mancano rete e cache, al loro posto c'è il segnaposto con le iniziali
// (SPEC §0.2).
private val AvatarPalette = listOf(
    Color(0xFFFF6D00), Color(0xFF00BFA5), Color(0xFFE91E8C),
    Color(0xFF3F51B5), Color(0xFFFFC107), Color(0xFF43A047),
)

/** Proporzioni delle immagini di personaggi e outfit sul wiki (circa 203×262, schermata di selezione). */
const val CHARACTER_IMAGE_ASPECT = 203f / 262f

private fun colorFor(seed: String): Color = AvatarPalette[seed.hashCode().absoluteValue % AvatarPalette.size]

private fun initialsFor(name: String): String =
    name.trim().split(Regex("\\s+")).mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")

@Composable
private fun rememberGrayscale(enabled: Boolean): ColorFilter? = remember(enabled) {
    if (enabled) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
}

@Composable
private fun Initials(name: String, dimmed: Boolean, style: TextStyle, modifier: Modifier) {
    val color = colorFor(name).let { if (dimmed) it.copy(alpha = 0.35f) else it }
    Box(modifier = modifier.background(color), contentAlignment = Alignment.Center) {
        Text(text = initialsFor(name), style = style, color = Color.White)
    }
}

/**
 * Immagine del wiki sopra il segnaposto con le iniziali. Il segnaposto si disegna solo finché
 * l'immagine non è caricata (o se non si carica, es. offline senza cache): altrimenti, quando la
 * card è attenuata e l'immagine semitrasparente, le iniziali si vedrebbero attraverso.
 */
@Composable
private fun ImageOverInitials(
    name: String,
    imageUrl: String?,
    dimmed: Boolean,
    initialsStyle: TextStyle,
    alignment: Alignment,
    contentScale: ContentScale = ContentScale.Crop,
) {
    var loaded by remember(imageUrl) { mutableStateOf(false) }
    if (!loaded) {
        Initials(name, dimmed, initialsStyle, Modifier.fillMaxSize())
    }
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = name,
            contentScale = contentScale,
            alignment = alignment,
            colorFilter = rememberGrayscale(dimmed),
            onSuccess = { loaded = true },
            modifier = Modifier.fillMaxSize().alpha(if (dimmed) 0.6f else 1f),
        )
    }
}

/** Avatar tondo: il volto del personaggio (parte alta dell'immagine), o le iniziali. */
@Composable
fun CharacterAvatar(
    name: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    size: Dp = 56.dp,
    dimmed: Boolean = false,
) {
    Box(modifier = modifier.size(size).clip(CircleShape)) {
        ImageOverInitials(name, imageUrl, dimmed, MaterialTheme.typography.titleMedium, Alignment.TopCenter)
    }
}

/**
 * Immagine di un cibo (intera, senza ritagli) con le iniziali finché non è caricata. Le immagini
 * del wiki non sono omogenee (alcune hanno una cornice scura incorporata, altre sono sagome su
 * sfondo trasparente): un riquadro uguale per tutte le rende uniformi.
 */
@Composable
fun FoodImage(name: String, imageUrl: String?, modifier: Modifier = Modifier, size: Dp = 72.dp) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.3f))
            .border(2.dp, Color(0xFF1B1B1F), shape)
            .padding(3.dp),
    ) {
        ImageOverInitials(name, imageUrl, false, MaterialTheme.typography.titleMedium, Alignment.Center, ContentScale.Fit)
    }
}

/** Immagine intera di personaggio o outfit, nelle proporzioni originali (rettangolare, verticale). */
@Composable
fun CharacterPortrait(
    name: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    Box(modifier = modifier.aspectRatio(CHARACTER_IMAGE_ASPECT).clip(RoundedCornerShape(12.dp))) {
        ImageOverInitials(name, imageUrl, dimmed, MaterialTheme.typography.headlineSmall, Alignment.Center)
    }
}

// Colore "del personaggio" per ogni URL, calcolato una volta per processo: la griglia scorre avanti
// e indietro e ricalcolarlo a ogni ricomposizione non serve.
private val accentCache = ConcurrentHashMap<String, Color>()

/**
 * Colore vivace dominante dell'immagine (rosso per Mario, verde per Yoshi…), estratto con Palette
 * dalla stessa immagine del wiki: nessun colore scritto a mano per personaggio. `null` finché non
 * è pronto o se l'immagine non si carica (offline senza cache).
 */
@Composable
fun rememberImageAccentColor(imageUrl: String?): Color? {
    val context = LocalContext.current
    var color by remember(imageUrl) { mutableStateOf(imageUrl?.let { accentCache[it] }) }
    LaunchedEffect(imageUrl) {
        if (imageUrl == null || color != null) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(96) // a Palette basta un'immagine piccola
            .allowHardware(false) // Palette legge i pixel: niente bitmap hardware
            .build()
        val bitmap = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
            ?: return@LaunchedEffect
        val rgb = withContext(Dispatchers.Default) {
            val palette = Palette.from(bitmap).generate()
            (palette.vibrantSwatch ?: palette.darkVibrantSwatch ?: palette.dominantSwatch)?.rgb
        } ?: return@LaunchedEffect
        color = Color(rgb).also { accentCache[imageUrl] = it }
    }
    return color
}

/**
 * Icona di una cup o di un rally. Senza immagine non mostra nulla (il nome è sempre accanto).
 * Con [outlined] la sagoma ha un contorno scuro come i titoli: l'immagine si ridisegna in nero
 * spostata nelle otto direzioni e poi a colori sopra (gli stemmi delle cup sono ritagliati, i
 * rally quadrati: il contorno segue la forma).
 */
@Composable
fun EventIcon(name: String, imageUrl: String?, modifier: Modifier = Modifier, size: Dp = 40.dp, outlined: Boolean = false) {
    if (imageUrl == null) return
    if (!outlined) {
        AsyncImage(
            model = imageUrl,
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size),
        )
        return
    }
    val painter = rememberAsyncImagePainter(imageUrl)
    Canvas(modifier.size(size).semantics { contentDescription = name }) {
        val stroke = OUTLINE_WIDTH.toPx()
        val box = Size(this.size.width - 2 * stroke, this.size.height - 2 * stroke)
        val intrinsic = painter.intrinsicSize
        val drawSize = if (intrinsic.isSpecified && intrinsic.width > 0f && intrinsic.height > 0f) {
            val scale = min(box.width / intrinsic.width, box.height / intrinsic.height)
            Size(intrinsic.width * scale, intrinsic.height * scale)
        } else {
            box
        }
        val left = (this.size.width - drawSize.width) / 2
        val top = (this.size.height - drawSize.height) / 2
        val ink = ColorFilter.tint(OutlineInk)
        for (dx in -1..1) for (dy in -1..1) {
            if (dx == 0 && dy == 0) continue
            translate(left + dx * stroke, top + dy * stroke) { with(painter) { draw(drawSize, colorFilter = ink) } }
        }
        translate(left, top) { with(painter) { draw(drawSize) } }
    }
}

private val OUTLINE_WIDTH = 2.dp
private val OutlineInk = Color(0xFF1B1B1F)
