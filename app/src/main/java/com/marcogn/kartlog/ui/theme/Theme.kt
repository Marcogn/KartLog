package com.marcogn.kartlog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColorScheme = lightColorScheme(
    primary = KartOrange,
    onPrimary = KartOnColorDark,
    secondary = KartTeal,
    onSecondary = KartOnColorDark,
    tertiary = KartMagenta,
    onTertiary = KartOnColorDark,
    background = KartBackgroundLight,
    onBackground = KartOnColorLight,
    surface = KartSurfaceLight,
    onSurface = KartOnColorLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = KartOrangeLight,
    onPrimary = KartOnColorLight,
    secondary = KartTealLight,
    onSecondary = KartOnColorLight,
    tertiary = KartMagentaLight,
    onTertiary = KartOnColorLight,
    background = KartBackgroundDark,
    onBackground = KartOnColorDark,
    surface = KartSurfaceDark,
    onSurface = KartOnColorDark,
)

/**
 * Palette custom vivace, non il colore dinamico di sistema: SPEC §2.2 chiede un look "da gioco"
 * riconoscibile e coerente, non legato al wallpaper dell'utente.
 */
@Composable
fun KartLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalKartDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = KartShapes,
            content = content,
        )
    }
}

/** Tema scuro effettivo (sistema o scelta in Impostazioni), non solo quello di sistema. */
private val LocalKartDarkTheme = staticCompositionLocalOf { false }

@Composable
fun isKartDarkTheme(): Boolean = LocalKartDarkTheme.current
