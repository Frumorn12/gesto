package it.frumorn.gesto.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiTouchGestureTest {
    private fun gesture() = MultiTouchGesture(decisionSlopPx = 24f, tapSlopPx = 8f, startMillis = 0L)

    private fun two(ax: Float, ay: Float, bx: Float, by: Float) =
        listOf(RemotePointer(1, ax, ay), RemotePointer(2, bx, by))

    @Test
    fun fingersSpreadingApartZoomAroundTheirCentre() {
        val g = gesture()
        g.update(two(100f, 100f, 200f, 100f), 10L)

        val actions = g.update(two(80f, 100f, 220f, 100f), 40L)

        assertEquals(listOf(MultiTouchAction.Zoom(1.4f, 150f, 100f, 0f, 0f)), actions)
        assertEquals(listOf(MultiTouchAction.Zoom(160f / 140f, 150f, 100f, 0f, 0f)), g.update(two(70f, 100f, 230f, 100f), 60L))
    }

    @Test
    fun fingersMovingTogetherScroll() {
        val g = gesture()
        g.update(two(100f, 100f, 200f, 100f), 10L)

        // The movement held back while undecided is not lost.
        assertEquals(listOf(MultiTouchAction.Scroll(0f, 30f)), g.update(two(100f, 130f, 200f, 130f), 40L))
        assertEquals(listOf(MultiTouchAction.Scroll(0f, 10f)), g.update(two(100f, 140f, 200f, 140f), 60L))
        assertEquals(listOf(MultiTouchAction.FinishScroll), g.end(80L))
    }

    @Test
    fun smallMovementsAreHeldBackUntilTheGestureIsClear() {
        val g = gesture()
        g.update(two(100f, 100f, 200f, 100f), 10L)

        assertTrue(g.update(two(100f, 110f, 200f, 110f), 30L).isEmpty())
        assertTrue(g.update(two(95f, 110f, 205f, 110f), 50L).isEmpty())
    }

    @Test
    fun theSpreadChangingFirstMeansZoomEvenIfTheCentreMovesALittle() {
        val g = gesture()
        g.update(two(100f, 100f, 200f, 100f), 10L)

        assertEquals(
            listOf(MultiTouchAction.Zoom(1.4f, 170f, 100f, 20f, 0f)),
            g.update(two(100f, 100f, 240f, 100f), 40L),
        )
    }

    @Test
    fun liftingAFingerFinishesTheScrollOnce() {
        val g = gesture()
        g.update(two(100f, 100f, 200f, 100f), 10L)
        g.update(two(100f, 140f, 200f, 140f), 40L)

        assertEquals(listOf(MultiTouchAction.FinishScroll), g.update(listOf(RemotePointer(1, 100f, 150f)), 60L))
        assertTrue(g.update(listOf(RemotePointer(1, 100f, 190f)), 80L).isEmpty())
        assertTrue(g.end(100L).isEmpty())
    }

    @Test
    fun aQuickTwoFingerTapIsARightClick() {
        val g = gesture()
        g.update(two(100f, 100f, 200f, 100f), 10L)
        g.update(two(102f, 101f, 201f, 100f), 60L)

        assertEquals(listOf(MultiTouchAction.RightClick), g.end(150L))
    }

    @Test
    fun aQuickThreeFingerTapTogglesTheKeyboard() {
        val g = gesture()
        g.update(two(100f, 100f, 200f, 100f) + RemotePointer(3, 150f, 160f), 10L)

        assertEquals(listOf(MultiTouchAction.ToggleKeyboard), g.end(150L))
    }

    @Test
    fun aSlowTwoFingerTouchIsNoClick() {
        val g = gesture()
        g.update(two(100f, 100f, 200f, 100f), 10L)

        assertTrue(g.end(600L).isEmpty())
    }

    @Test
    fun aQuickPinchIsNoRightClick() {
        val g = gesture()
        g.update(two(100f, 100f, 200f, 100f), 10L)
        g.update(two(60f, 100f, 240f, 100f), 60L)

        assertTrue(g.end(150L).none { it is MultiTouchAction.RightClick })
    }

    @Test
    fun aThirdFingerLandingDoesNotLookLikeAScroll() {
        val g = gesture()
        g.update(two(100f, 100f, 200f, 100f), 10L)

        assertTrue(g.update(two(100f, 100f, 200f, 100f) + RemotePointer(3, 150f, 300f), 30L).isEmpty())
    }
}
