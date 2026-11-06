package it.frumorn.gesto.core.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoQualityControllerTest {
    private val controller = AutoQualityController()
    private var now = 0L

    /**
     * Feeds [windows] windows of 2 s at [fps] (target 60), a sample every 500 ms, and returns what
     * the controller answered. A window is judged when the first sample after it arrives.
     */
    private fun feed(windows: Int, fps: Double, target: Int = 60): List<Int?> {
        val answers = mutableListOf<Int?>()
        repeat(windows * 4) {
            answers += controller.onStats(fps, target, now)
            now += 500
        }
        return answers
    }

    /** One more sample, which closes the window being filled. */
    private fun close(fps: Double = 60.0, target: Int = 60): Int? = controller.onStats(fps, target, now)

    @Test
    fun threeSlowWindowsLowerTheQualityOneStep() {
        assertTrue(feed(3, 40.0).all { it == null })

        assertEquals(1, close(40.0))
        assertEquals(1, controller.rung)
    }

    @Test
    fun aGoodWindowStartsTheCountOver() {
        assertTrue(feed(2, 40.0).all { it == null })
        assertTrue(feed(1, 60.0).all { it == null })
        // Two more slow windows make only two in a row.
        assertTrue(feed(2, 40.0).all { it == null })
        assertNull(close(40.0))
        assertEquals(0, controller.rung)

        // The third in a row does lower it.
        assertTrue(feed(1, 40.0).all { it == null })
        assertEquals(1, close(40.0))
    }

    @Test
    fun aWindowIsBadOnlyBelowThreeQuartersOfTheTarget() {
        // 45 of 60 is exactly 75%: not below it.
        assertTrue(feed(4, 45.0).all { it == null })
        assertNull(close(45.0))
        assertEquals(0, controller.rung)
    }

    @Test
    fun theWindowAverageIsWhatCounts() {
        repeat(9) {
            // 20 then 70: the window averages 45 with every slow sample in it.
            controller.onStats(20.0, 60, now); now += 500
            controller.onStats(70.0, 60, now); now += 500
        }
        assertEquals(0, controller.rung)
    }

    @Test
    fun itStopsAtTheLastRung() {
        repeat(3) {
            feed(3, 10.0)
            close(10.0)
            now += 500
        }
        assertEquals(3, controller.rung)

        assertTrue(feed(6, 10.0).all { it == null })
        assertNull(close(10.0))
        assertEquals(3, controller.rung)
    }

    @Test
    fun resetGoesBackToTheFirstRung() {
        feed(3, 40.0)
        assertEquals(1, close(40.0))

        controller.reset()

        assertEquals(0, controller.rung)
        // Nothing from before the reset counts.
        assertTrue(feed(2, 40.0).all { it == null })
        assertNull(close(40.0))
    }

    @Test
    fun goodWindowsNeverRaiseIt() {
        feed(3, 40.0)
        assertEquals(1, close(40.0))

        assertTrue(feed(20, 60.0).all { it == null })
        assertEquals(1, controller.rung)
    }

    @Test
    fun aNewStreamStartsTheWindowsOverAndKeepsTheRung() {
        feed(3, 40.0)
        close(40.0)
        feed(2, 40.0)

        controller.onStreamRestarted()
        now += 10_000

        assertEquals(1, controller.rung)
        assertTrue(feed(2, 40.0).all { it == null })
        assertNull(close(40.0))
    }

    @Test
    fun anUnknownTargetIsIgnored() {
        assertTrue(feed(6, 0.0, target = 0).all { it == null })
        assertEquals(0, controller.rung)
    }

    @Test
    fun windowsFollowTheirOwnLength() {
        val quick = AutoQualityController(windowMillis = 1_000, badWindows = 2)
        assertNull(quick.onStats(10.0, 60, 0))
        assertNull(quick.onStats(10.0, 60, 1_000))
        assertEquals(1, quick.onStats(10.0, 60, 2_000))
    }

    @Test
    fun pickingAutoAgainStartsFromTheBestRung() {
        feed(3, 40.0)
        assertEquals(1, close(40.0))

        controller.onPresetSwitched(toAuto = true)

        assertEquals(0, controller.rung)
    }

    @Test
    fun anyPresetSwitchForgetsTheBadWindowsCounted() {
        assertTrue(feed(2, 40.0).all { it == null })

        controller.onPresetSwitched(toAuto = false)

        // Without the switch, this third slow window would have lowered the quality.
        assertTrue(feed(1, 40.0).all { it == null })
        assertNull(close(40.0))
        assertEquals(0, controller.rung)
    }

    @Test
    fun switchingToAnotherPresetKeepsTheRungUntilAutoIsPickedAgain() {
        feed(3, 40.0)
        assertEquals(1, close(40.0))

        controller.onPresetSwitched(toAuto = false)

        assertEquals(1, controller.rung)
    }
}

class StreamDelayTest {
    @Test
    fun theFirstFrameIsOnTime() {
        assertFalse(StreamDelay().isLate(800))
    }

    @Test
    fun aFrameIsLateWhenItTakesLongerThanTheQuickestOne() {
        val delay = StreamDelay(lateMillis = 150)
        delay.isLate(40)
        delay.isLate(30)

        assertFalse(delay.isLate(170))
        assertTrue(delay.isLate(180))
    }

    @Test
    fun aClockAheadOrBehindDoesNotMatter() {
        // The phone's clock 2 s ahead of the PC's: every age carries it.
        val delay = StreamDelay(lateMillis = 150)
        delay.isLate(2_030)

        assertFalse(delay.isLate(2_100))
        assertTrue(delay.isLate(2_400))
    }

    @Test
    fun resetForgetsTheQuickestFrame() {
        val delay = StreamDelay(lateMillis = 150)
        delay.isLate(10)
        delay.reset()

        assertFalse(delay.isLate(500))
    }
}
