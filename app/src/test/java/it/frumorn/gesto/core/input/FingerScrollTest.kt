package it.frumorn.gesto.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the one scroll convention of the app: the PC's content follows the fingers. On the wire a
 * positive dy scrolls down (the content moves up), as the daemon's portal and Hyprland backends read it.
 */
class FingerScrollTest {
    @Test
    fun fingersMovingUpScrollDownSoTheContentGoesUpWithThem() {
        assertTrue(fingerScroll(0f, -30f).dy > 0f)
    }

    @Test
    fun fingersMovingDownScrollUp() {
        assertTrue(fingerScroll(0f, 30f).dy < 0f)
    }

    @Test
    fun sidewaysFollowsTheFingersToo() {
        assertTrue(fingerScroll(40f, 0f).dx < 0f)
        assertTrue(fingerScroll(-40f, 0f).dx > 0f)
    }

    @Test
    fun dampingScalesTheDistance() {
        assertEquals(PointerDelta(-7.5f, 15f), fingerScroll(10f, -20f, damping = 0.75f))
    }

    @Test
    fun noDampingByDefault() {
        assertEquals(PointerDelta(-10f, 20f), fingerScroll(10f, -20f))
    }
}
