package it.frumorn.gesto.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectTouchMachineTest {
    private fun machine() = DirectTouchMachine(slopPx = 8f, holdToDragMillis = 400, doubleTapMillis = 250, doubleTapSlopPx = 60f)

    @Test
    fun touchingDownDoesNothing() {
        assertTrue(machine().down(100f, 100f, 0L).isEmpty())
    }

    @Test
    fun stillTapClicksWhereItTouched() {
        val touch = machine()
        touch.down(100f, 200f, 0L)
        touch.move(102f, 201f, 40L)

        assertEquals(listOf(DirectTouchAction.Click(100f, 200f)), touch.up(102f, 201f, 80L))
    }

    @Test
    fun fastMoveHoversAndNeverClicks() {
        val touch = machine()
        touch.down(100f, 100f, 0L)
        val first = touch.move(130f, 100f, 30L)
        val second = touch.move(160f, 110f, 60L)
        val lifted = touch.up(160f, 110f, 90L)

        assertEquals(listOf(DirectTouchAction.Hover(130f, 100f)), first)
        assertEquals(listOf(DirectTouchAction.Hover(160f, 110f)), second)
        assertTrue(lifted.none { it is DirectTouchAction.Click })
        assertTrue(lifted.none { it is DirectTouchAction.DragEnd })
    }

    @Test
    fun holdingStillThenMovingDrags() {
        val touch = machine()
        touch.down(100f, 100f, 0L)
        touch.move(101f, 100f, 200L)
        val started = touch.move(140f, 100f, 420L)
        val moved = touch.move(180f, 120f, 450L)
        val lifted = touch.up(180f, 120f, 480L)

        assertEquals(
            listOf(DirectTouchAction.DragStart(100f, 100f), DirectTouchAction.DragMove(140f, 100f)),
            started,
        )
        assertEquals(listOf(DirectTouchAction.DragMove(180f, 120f)), moved)
        assertEquals(listOf(DirectTouchAction.DragEnd), lifted)
    }

    @Test
    fun movingBeforeTheHoldIsAHoverEvenIfItLastsLonger() {
        val touch = machine()
        touch.down(100f, 100f, 0L)
        touch.move(140f, 100f, 100L)
        val later = touch.move(150f, 100f, 600L)

        assertEquals(listOf(DirectTouchAction.Hover(150f, 100f)), later)
        assertTrue(touch.up(150f, 100f, 650L).isEmpty())
    }

    @Test
    fun doubleTapNearbyClicksTwiceWhereTheFirstTapWas() {
        val touch = machine()
        touch.down(100f, 100f, 0L)
        val first = touch.up(100f, 100f, 50L)
        touch.down(130f, 100f, 200L)
        val second = touch.up(130f, 100f, 240L)

        assertEquals(listOf(DirectTouchAction.Click(100f, 100f)), first)
        assertEquals(listOf(DirectTouchAction.Click(100f, 100f)), second)
    }

    @Test
    fun secondTapFarAwayClicksWhereItTouched() {
        val touch = machine()
        touch.down(100f, 100f, 0L)
        touch.up(100f, 100f, 50L)
        touch.down(200f, 100f, 200L)

        assertEquals(listOf(DirectTouchAction.Click(200f, 100f)), touch.up(200f, 100f, 240L))
    }

    @Test
    fun secondTapTooLateClicksWhereItTouched() {
        val touch = machine()
        touch.down(100f, 100f, 0L)
        touch.up(100f, 100f, 50L)
        touch.down(120f, 100f, 400L)

        assertEquals(listOf(DirectTouchAction.Click(120f, 100f)), touch.up(120f, 100f, 440L))
    }

    @Test
    fun cancelDuringADragReleasesTheButton() {
        val touch = machine()
        touch.down(100f, 100f, 0L)
        touch.move(140f, 100f, 500L)

        assertEquals(listOf(DirectTouchAction.DragEnd), touch.cancel())
        assertTrue(touch.up(140f, 100f, 520L).isEmpty())
    }

    @Test
    fun cancelledTapDoesNotClick() {
        val touch = machine()
        touch.down(100f, 100f, 0L)

        assertTrue(touch.cancel().isEmpty())
        assertTrue(touch.up(100f, 100f, 40L).isEmpty())
    }
}
