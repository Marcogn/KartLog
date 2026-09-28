package com.marcogn.kartlog.ui.map

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.marcogn.kartlog.data.seed.MapImageDto
import com.marcogn.kartlog.domain.model.MapPointType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Il tocco sulla mappa arriva al punto giusto, anche senza immagine scaricata (offline). */
@RunWith(AndroidJUnit4::class)
class CollectibleMapViewTest {

    @get:Rule
    val compose = createComposeRule()

    // Mappa quadrata in un riquadro quadrato: le frazioni del punto sono le frazioni del riquadro.
    private val image = MapImageDto("world", "https://example.invalid/map.webp", 1000, 1000)
    private val point = MapPoint(MapPointType.MEDALLION, "medallion_0001", 1, 0.25f, 0.75f, null, null, null, null, false)

    @Test
    fun tappingAMarkerSelectsIt() {
        var clicked: MapPoint? = null
        compose.setContent {
            CollectibleMapView(image, listOf(point), focus = null, onPointClick = { clicked = it },
                onImageStateChange = {}, modifier = Modifier.size(300.dp).testTag("map"))
        }
        compose.onNodeWithTag("map").performTouchInput { click(Offset(width * 0.9f, height * 0.1f)) }
        compose.mainClock.advanceTimeBy(1_000)
        assertNull("lontano dal marker non succede nulla", clicked)

        compose.onNodeWithTag("map").performTouchInput { click(Offset(width * 0.25f, height * 0.75f)) }
        // Il tap singolo arriva solo dopo l'attesa del doppio tap.
        compose.mainClock.advanceTimeBy(1_000)
        assertEquals(point, clicked)
    }

    @Test
    fun doubleTapZoomsAroundTheFingerAndTheMarkerMovesAccordingly() {
        var clicked: MapPoint? = null
        compose.setContent {
            CollectibleMapView(image, listOf(point), focus = null, onPointClick = { clicked = it },
                onImageStateChange = {}, modifier = Modifier.size(300.dp).testTag("map"))
        }
        // Zoom ×2 attorno al centro: il marker a (0.25, 0.75) finisce a (0, 1), fuori dal riquadro
        // di mezzo marker; toccando dove stava prima non lo si prende più.
        compose.onNodeWithTag("map").performTouchInput { doubleClick(center) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithTag("map").performTouchInput { click(Offset(width * 0.25f, height * 0.75f)) }
        compose.mainClock.advanceTimeBy(1_000)
        assertNull(clicked)
        // Il marker ora è nell'angolo in basso a sinistra.
        compose.onNodeWithTag("map").performTouchInput { click(Offset(2f, height - 2f)) }
        compose.mainClock.advanceTimeBy(1_000)
        assertEquals(point, clicked)
    }
}
