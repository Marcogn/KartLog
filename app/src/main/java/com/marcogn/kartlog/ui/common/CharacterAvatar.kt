package com.marcogn.kartlog.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

// Nessuna immagine ufficiale (SPEC §0.2/§2.3): iniziali su sfondo colorato finché non si trova
// un'alternativa originale (nessun asset Nintendo, vedi CLAUDE.md).
private val AvatarPalette = listOf(
    Color(0xFFFF6D00), Color(0xFF00BFA5), Color(0xFFE91E8C),
    Color(0xFF3F51B5), Color(0xFFFFC107), Color(0xFF43A047),
)

private fun colorFor(seed: String): Color = AvatarPalette[seed.hashCode().absoluteValue % AvatarPalette.size]

private fun initialsFor(name: String): String =
    name.trim().split(Regex("\\s+")).mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")

@Composable
fun CharacterAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 56.dp, dimmed: Boolean = false) {
    val color = colorFor(name).let { if (dimmed) it.copy(alpha = 0.35f) else it }
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsFor(name),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}
