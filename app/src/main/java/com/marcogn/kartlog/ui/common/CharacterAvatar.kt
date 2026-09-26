package com.marcogn.kartlog.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import kotlin.math.absoluteValue

// Immagini dal CDN di Super Mario Wiki (URL nel seed, scaricate a runtime: mai nell'APK). Finché
// non sono in cache, o se mancano rete e cache, resta visibile il segnaposto con le iniziali che
// sta sotto l'immagine (SPEC §0.2).
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
        Initials(name, dimmed, MaterialTheme.typography.titleMedium, Modifier.fillMaxSize())
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                colorFilter = rememberGrayscale(dimmed),
                modifier = Modifier.fillMaxSize().alpha(if (dimmed) 0.6f else 1f),
            )
        }
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
    val shape = RoundedCornerShape(12.dp)
    Box(modifier = modifier.aspectRatio(CHARACTER_IMAGE_ASPECT).clip(shape)) {
        Initials(name, dimmed, MaterialTheme.typography.headlineSmall, Modifier.fillMaxSize())
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                colorFilter = rememberGrayscale(dimmed),
                modifier = Modifier.fillMaxSize().alpha(if (dimmed) 0.6f else 1f),
            )
        }
    }
}

/** Icona di una cup o di un rally. Senza immagine non mostra nulla (il nome è sempre accanto). */
@Composable
fun EventIcon(name: String, imageUrl: String?, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    if (imageUrl == null) return
    AsyncImage(
        model = imageUrl,
        contentDescription = name,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}
