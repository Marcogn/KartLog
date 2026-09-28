package com.marcogn.kartlog.ui.map

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.marcogn.kartlog.domain.model.MapPointType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Zoom, trascinamento e tocco sulla mappa: logica pura di [MapViewport]. */
class MapViewportTest {

    // Mappa 2000×1000 in un riquadro 400×800: sta intera in larghezza (fit 0.2), alta 200.
    private val base = MapViewport(viewport = Size(400f, 800f), image = Size(2000f, 1000f)).clamped()

    @Test
    fun `a zoom 1 la mappa intera sta nel riquadro, centrata in verticale`() {
        assertEquals(0.2f, base.fit, 1e-6f)
        assertEquals(Offset(0f, 300f), base.offset)
        assertEquals(Offset(200f, 400f), base.toScreen(0.5f, 0.5f))
    }

    @Test
    fun `lo zoom tiene fermo il punto sotto le dita`() {
        val pivot = Offset(100f, 400f)
        val before = (pivot.x - base.offset.x) / base.mapWidth
        val zoomed = base.transformed(pivot, Offset.Zero, 2f)
        assertEquals(2f, zoomed.scale)
        assertEquals(before, (pivot.x - zoomed.offset.x) / zoomed.mapWidth, 1e-5f)
    }

    @Test
    fun `lo zoom resta tra 1 e il massimo e non si esce dalla mappa trascinando`() {
        assertEquals(1f, base.transformed(Offset.Zero, Offset.Zero, 0.1f).scale)
        assertEquals(8f, base.transformed(Offset.Zero, Offset.Zero, 100f).scale)
        val dragged = base.transformed(Offset.Zero, Offset(10_000f, 0f), 3f)
        assertEquals(0f, dragged.offset.x)  // bordo sinistro della mappa contro il bordo del riquadro
    }

    @Test
    fun `centrare su un punto lo porta in mezzo allo schermo quando c'e spazio`() {
        val centered = base.centeredOn(0.5f, 0.5f, 4f)
        assertEquals(Offset(200f, 400f), centered.toScreen(0.5f, 0.5f))
    }

    @Test
    fun `il tocco sceglie il punto piu vicino entro il raggio`() {
        val near = point("a", 0.5f, 0.5f)
        val far = point("b", 0.9f, 0.9f)
        assertEquals(near, base.hitTest(listOf(far, near), Offset(205f, 402f), radius = 24f))
        assertNull(base.hitTest(listOf(far, near), Offset(10f, 10f), radius = 24f))
    }

    @Test
    fun `i filtri nascondono i tipi spenti e, se richiesto, i punti gia fatti`() {
        val coin = point("coin", 0f, 0f)
        val done = point("done", 0f, 0f, type = MapPointType.P_SWITCH, done = true)
        val panel = point("panel", 0f, 0f, type = MapPointType.QUESTION_PANEL)
        val all = listOf(coin, done, panel)
        assertEquals(listOf(coin, done), filterPoints(all, setOf(MapPointType.MEDALLION, MapPointType.P_SWITCH), showDone = true))
        assertEquals(listOf(coin, panel), filterPoints(all, MapPointType.entries.toSet(), showDone = false))
    }

    private fun point(id: String, x: Float, y: Float, type: MapPointType = MapPointType.MEDALLION, done: Boolean = false) =
        MapPoint(type, id, 1, x, y, null, null, null, null, done)
}
