package it.frumorn.gesto.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteTouchpadGestureMachineTest {
    @Test
    fun singlePointerMoveEmitsMoveEvent() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f, longPressDragEnabled = false)

        gesture.begin(pointer(1, 10f, 10f), timeMillis = 0L)
        val actions = gesture.update(listOf(pointer(1, 20f, 12f)), timeMillis = 16L)

        val move = actions.filterIsInstance<RemoteGestureAction.Move>().single()
        assertEquals(10f, move.dx)
        assertEquals(2f, move.dy)
        assertEquals(RemoteGestureMode.MovingPointer, gesture.mode)
    }

    @Test
    fun secondPointerDownEntersScrollWithoutSyntheticDelta() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        gesture.begin(pointer(1, 10f, 10f), timeMillis = 0L)
        val enterActions = gesture.update(
            listOf(pointer(1, 10f, 10f), pointer(2, 30f, 10f)),
            timeMillis = 20L,
        )
        val scrollActions = gesture.update(
            listOf(pointer(1, 10f, 22f), pointer(2, 30f, 22f)),
            timeMillis = 36L,
        )

        assertTrue(enterActions.none { it is RemoteGestureAction.Scroll })
        assertTrue(enterActions.any { it is RemoteGestureAction.ModeChanged && it.to == RemoteGestureMode.TwoFingerScroll })
        val scroll = scrollActions.filterIsInstance<RemoteGestureAction.Scroll>().single()
        assertEquals(0f, scroll.dx)
        assertEquals(12f, scroll.dy)
    }

    @Test
    fun liftingOnePointerDuringScrollFinishesAndRecovers() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        gesture.begin(pointer(1, 10f, 10f), timeMillis = 0L)
        gesture.update(listOf(pointer(1, 10f, 10f), pointer(2, 30f, 10f)), timeMillis = 10L)
        gesture.update(listOf(pointer(1, 10f, 25f), pointer(2, 30f, 25f)), timeMillis = 20L)
        val actions = gesture.update(listOf(pointer(1, 10f, 25f)), timeMillis = 30L)

        assertTrue(actions.any { it == RemoteGestureAction.FinishScroll })
        assertTrue(actions.any { it is RemoteGestureAction.ModeChanged && it.to == RemoteGestureMode.Recovering })
        assertEquals(RemoteGestureMode.Recovering, gesture.mode)
    }

    @Test
    fun remainingPointerCanMoveAfterScrollRecoveryWithoutJump() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        gesture.begin(pointer(1, 10f, 10f), timeMillis = 0L)
        gesture.update(listOf(pointer(1, 10f, 10f), pointer(2, 30f, 10f)), timeMillis = 10L)
        gesture.update(listOf(pointer(1, 10f, 25f), pointer(2, 30f, 25f)), timeMillis = 20L)
        gesture.update(listOf(pointer(1, 10f, 25f)), timeMillis = 30L)
        val actions = gesture.update(listOf(pointer(1, 14f, 27f)), timeMillis = 46L)

        val move = actions.filterIsInstance<RemoteGestureAction.Move>().single()
        assertEquals(4f, move.dx)
        assertEquals(2f, move.dy)
        assertEquals(RemoteGestureMode.MovingPointer, gesture.mode)
    }

    @Test
    fun fullGestureEndReturnsToIdleCleanly() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        gesture.begin(pointer(1, 10f, 10f), timeMillis = 0L)
        gesture.update(listOf(pointer(1, 16f, 10f)), timeMillis = 16L)
        val actions = gesture.update(emptyList(), timeMillis = 32L)

        assertTrue(actions.any { it is RemoteGestureAction.ModeChanged && it.to == RemoteGestureMode.Idle })
        assertEquals(RemoteGestureMode.Idle, gesture.mode)
    }

    @Test
    fun canceledScrollFinishesAndReturnsToIdle() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        gesture.begin(pointer(1, 10f, 10f), timeMillis = 0L)
        gesture.update(listOf(pointer(1, 10f, 10f), pointer(2, 30f, 10f)), timeMillis = 10L)
        gesture.update(listOf(pointer(1, 10f, 25f), pointer(2, 30f, 25f)), timeMillis = 20L)
        val actions = gesture.cancel("test_cancel")

        assertTrue(actions.any { it == RemoteGestureAction.FinishScroll })
        assertTrue(actions.any { it is RemoteGestureAction.ModeChanged && it.to == RemoteGestureMode.GestureCancelled })
        assertEquals(RemoteGestureMode.Idle, gesture.mode)
    }

    @Test
    fun twoFingerTapRightClicksWithoutFinishingAScroll() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        gesture.begin(pointer(1, 10f, 10f), timeMillis = 0L)
        gesture.update(listOf(pointer(1, 10f, 10f), pointer(2, 30f, 10f)), timeMillis = 10L)
        val actions = gesture.update(emptyList(), timeMillis = 60L)

        assertEquals(listOf(RemoteGestureAction.RightClick), actions.filter { it !is RemoteGestureAction.ModeChanged })
    }

    @Test
    fun scrollStillFinishesOnceFingersMoved() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        gesture.begin(pointer(1, 10f, 10f), timeMillis = 0L)
        gesture.update(listOf(pointer(1, 10f, 10f), pointer(2, 30f, 10f)), timeMillis = 10L)
        gesture.update(listOf(pointer(1, 10f, 40f), pointer(2, 30f, 40f)), timeMillis = 20L)
        val actions = gesture.update(emptyList(), timeMillis = 30L)

        assertTrue(actions.any { it == RemoteGestureAction.FinishScroll })
        assertTrue(actions.none { it == RemoteGestureAction.RightClick })
    }

    @Test
    fun noDeadStateAfterScrollEnds() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f, longPressDragEnabled = false)

        gesture.begin(pointer(1, 10f, 10f), timeMillis = 0L)
        gesture.update(listOf(pointer(1, 10f, 10f), pointer(2, 30f, 10f)), timeMillis = 10L)
        gesture.update(listOf(pointer(1, 10f, 25f), pointer(2, 30f, 25f)), timeMillis = 20L)
        gesture.update(emptyList(), timeMillis = 30L)

        gesture.begin(pointer(3, 5f, 5f), timeMillis = 60L)
        val actions = gesture.update(listOf(pointer(3, 9f, 6f)), timeMillis = 76L)

        assertTrue(actions.any { it is RemoteGestureAction.Move })
        assertEquals(RemoteGestureMode.SinglePointerDown, gesture.mode)
    }

    @Test
    fun single_tap_within_250ms_is_click() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        val actions = record(gesture) {
            begin(pointer(1, 10f, 10f), timeMillis = 0L)
            update(listOf(pointer(1, 12f, 11f)), timeMillis = 80L)
            update(emptyList(), timeMillis = 250L)
        }

        assertEquals(1, actions.count { it == RemoteGestureAction.Click })
    }

    @Test
    fun slow_single_tap_over_250ms_is_not_click() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        val actions = record(gesture) {
            begin(pointer(1, 10f, 10f), timeMillis = 0L)
            update(listOf(pointer(1, 10f, 10f)), timeMillis = 120L)
            update(emptyList(), timeMillis = 251L)
        }

        assertTrue(actions.none { it == RemoteGestureAction.Click })
        assertEquals(RemoteGestureMode.Idle, gesture.mode)
    }

    @Test
    fun two_finger_tap_emits_right_click() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        val actions = record(gesture) {
            begin(pointer(1, 100f, 100f), timeMillis = 0L)
            update(listOf(pointer(1, 100f, 100f), pointer(2, 160f, 104f)), timeMillis = 30L)
            // Fingers resting on glass are never perfectly still.
            update(listOf(pointer(1, 101.5f, 99f), pointer(2, 161f, 105.5f)), timeMillis = 90L)
            update(listOf(pointer(2, 161f, 105.5f)), timeMillis = 160L)
            update(emptyList(), timeMillis = 190L)
        }

        assertEquals(1, actions.count { it == RemoteGestureAction.RightClick })
        assertTrue(actions.none { it is RemoteGestureAction.Scroll })
        assertTrue(actions.none { it == RemoteGestureAction.Click })
        assertTrue(actions.none { it == RemoteGestureAction.ToggleKeyboard })
    }

    @Test
    fun two_finger_tap_lifting_both_at_once_emits_right_click() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        val actions = record(gesture) {
            begin(pointer(1, 100f, 100f), timeMillis = 0L)
            update(listOf(pointer(1, 100f, 100f), pointer(2, 160f, 104f)), timeMillis = 20L)
            update(emptyList(), timeMillis = 140L)
        }

        assertEquals(1, actions.count { it == RemoteGestureAction.RightClick })
    }

    @Test
    fun two_finger_tap_disabled_emits_nothing() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f, twoFingerTapEnabled = false)

        val actions = record(gesture) {
            begin(pointer(1, 100f, 100f), timeMillis = 0L)
            update(listOf(pointer(1, 100f, 100f), pointer(2, 160f, 104f)), timeMillis = 30L)
            update(listOf(pointer(2, 160f, 104f)), timeMillis = 150L)
            update(emptyList(), timeMillis = 180L)
        }

        assertTrue(actions.none { it == RemoteGestureAction.RightClick })
        assertTrue(actions.none { it == RemoteGestureAction.Click })
        assertTrue(actions.none { it is RemoteGestureAction.Scroll })
    }

    @Test
    fun two_finger_scroll_is_not_right_click() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        val actions = record(gesture) {
            begin(pointer(1, 100f, 100f), timeMillis = 0L)
            update(listOf(pointer(1, 100f, 100f), pointer(2, 160f, 100f)), timeMillis = 20L)
            update(listOf(pointer(1, 100f, 115f), pointer(2, 160f, 115f)), timeMillis = 60L)
            update(listOf(pointer(1, 100f, 130f), pointer(2, 160f, 130f)), timeMillis = 100L)
            update(emptyList(), timeMillis = 140L)
        }

        assertTrue(actions.any { it is RemoteGestureAction.Scroll })
        assertEquals(30f, actions.filterIsInstance<RemoteGestureAction.Scroll>().sumOf { it.dy.toDouble() }.toFloat())
        assertTrue(actions.none { it == RemoteGestureAction.RightClick })
    }

    @Test
    fun two_finger_tap_held_too_long_emits_nothing() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        val actions = record(gesture) {
            begin(pointer(1, 100f, 100f), timeMillis = 0L)
            update(listOf(pointer(1, 100f, 100f), pointer(2, 160f, 104f)), timeMillis = 30L)
            update(emptyList(), timeMillis = 301L)
        }

        assertTrue(actions.none { it == RemoteGestureAction.RightClick })
    }

    @Test
    fun three_finger_tap_toggles_keyboard() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        val actions = record(gesture) {
            begin(pointer(1, 100f, 100f), timeMillis = 0L)
            update(listOf(pointer(1, 100f, 100f), pointer(2, 150f, 96f)), timeMillis = 15L)
            update(listOf(pointer(1, 100f, 100f), pointer(2, 150f, 96f), pointer(3, 200f, 104f)), timeMillis = 35L)
            update(listOf(pointer(1, 101f, 100f), pointer(2, 150f, 97f), pointer(3, 199f, 104f)), timeMillis = 120L)
            update(listOf(pointer(1, 101f, 100f), pointer(3, 199f, 104f)), timeMillis = 200L)
            update(listOf(pointer(1, 101f, 100f)), timeMillis = 215L)
            update(emptyList(), timeMillis = 230L)
        }

        assertEquals(1, actions.count { it == RemoteGestureAction.ToggleKeyboard })
        assertTrue(actions.none { it == RemoteGestureAction.RightClick })
        assertTrue(actions.none { it == RemoteGestureAction.Click })
        assertTrue(actions.none { it is RemoteGestureAction.Scroll })
    }

    @Test
    fun three_fingers_that_move_do_not_toggle_keyboard() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        val actions = record(gesture) {
            begin(pointer(1, 100f, 100f), timeMillis = 0L)
            update(listOf(pointer(1, 100f, 100f), pointer(2, 150f, 100f)), timeMillis = 10L)
            update(listOf(pointer(1, 100f, 100f), pointer(2, 150f, 100f), pointer(3, 200f, 100f)), timeMillis = 20L)
            update(listOf(pointer(1, 100f, 125f), pointer(2, 150f, 125f), pointer(3, 200f, 125f)), timeMillis = 80L)
            update(emptyList(), timeMillis = 120L)
        }

        assertTrue(actions.none { it == RemoteGestureAction.ToggleKeyboard })
    }

    @Test
    fun hold_500ms_then_move_starts_drag() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        val actions = record(gesture) {
            begin(pointer(1, 100f, 100f), timeMillis = 0L)
            update(listOf(pointer(1, 100f, 100f)), timeMillis = 250L)
            update(listOf(pointer(1, 104f, 100f)), timeMillis = 500L)
            update(listOf(pointer(1, 140f, 120f)), timeMillis = 540L)
            update(emptyList(), timeMillis = 600L)
        }

        assertEquals(1, actions.count { it == RemoteGestureAction.DragStart })
        assertEquals(1, actions.count { it == RemoteGestureAction.DragEnd })
        assertTrue(actions.indexOf(RemoteGestureAction.DragStart) < actions.indexOf(RemoteGestureAction.DragEnd))
        assertTrue(actions.none { it == RemoteGestureAction.Click })
    }

    @Test
    fun hold_400ms_then_move_is_plain_move() {
        val gesture = RemoteTouchpadGestureMachine(tapSlopPx = 8f)

        val actions = record(gesture) {
            begin(pointer(1, 100f, 100f), timeMillis = 0L)
            update(listOf(pointer(1, 100f, 100f)), timeMillis = 200L)
            // The finger starts off slowly: still inside the slop when the old 420 ms had passed.
            update(listOf(pointer(1, 103f, 100f)), timeMillis = 400L)
            update(listOf(pointer(1, 106f, 100f)), timeMillis = 416L)
            update(listOf(pointer(1, 110f, 100f)), timeMillis = 432L)
            update(listOf(pointer(1, 130f, 100f)), timeMillis = 450L)
            update(listOf(pointer(1, 150f, 100f)), timeMillis = 600L)
            update(emptyList(), timeMillis = 650L)
        }

        assertTrue(actions.none { it == RemoteGestureAction.DragStart })
        assertEquals(50f, actions.filterIsInstance<RemoteGestureAction.Move>().sumOf { it.dx.toDouble() }.toFloat())
        assertTrue(actions.none { it == RemoteGestureAction.Click })
    }

    /** Runs [steps] on [gesture] and returns every action it emitted, in order. */
    private fun record(
        gesture: RemoteTouchpadGestureMachine,
        steps: Steps.() -> Unit,
    ): List<RemoteGestureAction> = Steps(gesture).apply(steps).actions

    private class Steps(private val gesture: RemoteTouchpadGestureMachine) {
        val actions = mutableListOf<RemoteGestureAction>()

        fun begin(pointer: RemotePointer, timeMillis: Long) {
            actions += gesture.begin(pointer, timeMillis)
        }

        fun update(pointers: List<RemotePointer>, timeMillis: Long) {
            actions += gesture.update(pointers, timeMillis)
        }
    }

    private fun pointer(id: Long, x: Float, y: Float) = RemotePointer(id, x, y)
}
