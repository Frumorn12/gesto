package it.frumorn.gesto.core.video

import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomTransformTest {
    @Test
    fun zoomKeepsThePointUnderTheFingers() {
        val zoom = ZoomTransform().zoomBy(2f, 50f, 50f, 100f, 100f)

        assertEquals(2f, zoom.scale, 0.0001f)
        assertEquals(50f to 50f, zoom.toContent(50f, 50f))
    }

    @Test
    fun zoomOffCentreKeepsThatPointToo() {
        val zoom = ZoomTransform().zoomBy(2f, 20f, 70f, 100f, 100f)

        val (x, y) = zoom.toContent(20f, 70f)
        assertEquals(20f, x, 0.0001f)
        assertEquals(70f, y, 0.0001f)
    }

    @Test
    fun zoomStopsAtThreeTimes() {
        assertEquals(3f, ZoomTransform().zoomBy(10f, 50f, 50f, 100f, 100f).scale, 0.0001f)
    }

    @Test
    fun zoomingOutStopsAtTheWholeScreenWithNoOffset() {
        val zoom = ZoomTransform().zoomBy(2f, 80f, 80f, 100f, 100f).zoomBy(0.1f, 10f, 10f, 100f, 100f)

        assertEquals(ZoomTransform(), zoom)
    }

    @Test
    fun panPastTheEdgeIsHeldAtTheEdge() {
        val zoom = ZoomTransform().zoomBy(2f, 50f, 50f, 100f, 100f)

        val right = zoom.panBy(500f, 500f, 100f, 100f)
        assertEquals(0f, right.offsetX, 0.0001f)
        assertEquals(0f, right.offsetY, 0.0001f)
        assertEquals(0f to 0f, right.toContent(0f, 0f))

        val left = zoom.panBy(-500f, -500f, 100f, 100f)
        assertEquals(-100f, left.offsetX, 0.0001f)
        assertEquals(-100f, left.offsetY, 0.0001f)
        assertEquals(100f to 100f, left.toContent(100f, 100f))
    }

    @Test
    fun panAtTheWholeScreenChangesNothing() {
        assertEquals(ZoomTransform(), ZoomTransform().panBy(30f, -20f, 100f, 100f))
    }

    @Test
    fun toContentWithoutZoomIsTheSamePoint() {
        assertEquals(12f to 34f, ZoomTransform().toContent(12f, 34f))
    }
}
