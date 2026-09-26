package com.marcogn.kartlog.ui.common

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * Disegna davvero il testo contornato su una bitmap e conta i pixel: un riempimento bianco deve
 * esserci. Col bug (Stroke "rimasto" sul secondo drawText) le lettere erano vuote, e il bianco
 * stava solo in un sottile anello sopra il contorno nero.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OutlinedTextDrawTest {

    @Test
    fun letterInsidesAreFilledNotHollow() {
        val density = Density(1f)
        val measurer = TextMeasurer(
            createFontFamilyResolver(ApplicationProvider.getApplicationContext()),
            density,
            LayoutDirection.Ltr,
        )
        val layout = measurer.measure("IIII", TextStyle(fontSize = 80.sp))
        val bitmap = ImageBitmap(layout.size.width + 20, layout.size.height + 20)
        CanvasDrawScope().draw(
            density, LayoutDirection.Ltr, Canvas(bitmap),
            Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
        ) {
            drawOutlinedText(layout, fill = Color.White, outline = Color.Black)
        }
        val pixels = bitmap.toPixelMap()
        var white = 0
        var black = 0
        for (x in 0 until pixels.width) for (y in 0 until pixels.height) {
            val c = pixels[x, y]
            if (c.alpha > 0.9f && c.red > 0.9f && c.green > 0.9f) white++
            if (c.alpha > 0.9f && c.red < 0.1f && c.green < 0.1f) black++
        }
        // Col contorno a 1/6 dell'altezza, in "IIII" a 80sp il riempimento pieno supera di gran lunga il contorno sottile.
        assertTrue("bianco=$white nero=$black", white > 0 && black > 0 && white > black / 3)
    }
}
