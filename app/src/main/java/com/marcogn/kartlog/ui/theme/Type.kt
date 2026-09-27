package com.marcogn.kartlog.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.marcogn.kartlog.R

/**
 * Lilita One (Juan Montoreano, SIL Open Font License 1.1: testo in assets/licenses): il font "da
 * gioco" dei titoli del mockup dell'autore. Un solo peso, quindi mai FontWeight.Bold sopra (sarebbe
 * un grassetto sintetico).
 */
val KartFont = FontFamily(Font(R.font.lilita_one))

// Tipografia bold per il look "da gioco" richiesto in SPEC §2.2: Lilita One per titoli e intestazioni.
val Typography = Typography(
    headlineSmall = TextStyle(
        fontFamily = KartFont,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = KartFont,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = KartFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
)
