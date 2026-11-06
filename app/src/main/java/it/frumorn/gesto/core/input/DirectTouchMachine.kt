package it.frumorn.gesto.core.input

import kotlin.math.hypot

/** What one finger on the remote screen does in the "Direct" touch mode. */
sealed interface DirectTouchAction {
    /** The pointer follows the finger, no button held. */
    data class Hover(val x: Float, val y: Float) : DirectTouchAction

    /** A left click at this point. */
    data class Click(val x: Float, val y: Float) : DirectTouchAction

    /** The left button goes down at this point (where the finger was held). */
    data class DragStart(val x: Float, val y: Float) : DirectTouchAction

    /** The pointer moves with the left button held. */
    data class DragMove(val x: Float, val y: Float) : DirectTouchAction

    /** The left button goes up. */
    data object DragEnd : DirectTouchAction
}

/**
 * One finger on the remote screen in the "Direct" touch mode, as a pure state machine over the
 * positions the finger reports (any coordinate space, as long as it is the same throughout).
 *
 * - Touching down does nothing yet.
 * - Moving past [slopPx] before [holdToDragMillis]: the pointer follows ([DirectTouchAction.Hover]).
 * - Holding still for [holdToDragMillis], then moving: a drag that starts where the finger was held.
 *   Holding first is what keeps a slow swipe from dragging a window by accident.
 * - Lifting without having moved past the slop: a click where the finger touched. If the previous
 *   click was less than [doubleTapMillis] before this touch and at most [doubleTapSlopPx] away, the
 *   click lands on the previous one instead, so a double click on a file stays on the file.
 */
class DirectTouchMachine(
    private val slopPx: Float,
    private val holdToDragMillis: Long = 400,
    private val doubleTapMillis: Long = 250,
    private val doubleTapSlopPx: Float,
) {
    private enum class Phase { Idle, Pressed, Hovering, Dragging }

    private var phase = Phase.Idle
    private var downX = 0f
    private var downY = 0f
    private var downAt = 0L
    private var lastClickX = 0f
    private var lastClickY = 0f
    /** When the last click was sent; null when there is none to pair a second tap with. */
    private var lastClickAt: Long? = null

    fun down(x: Float, y: Float, t: Long): List<DirectTouchAction> {
        phase = Phase.Pressed
        downX = x
        downY = y
        downAt = t
        return emptyList()
    }

    fun move(x: Float, y: Float, t: Long): List<DirectTouchAction> = when (phase) {
        Phase.Idle -> emptyList()
        Phase.Hovering -> listOf(DirectTouchAction.Hover(x, y))
        Phase.Dragging -> listOf(DirectTouchAction.DragMove(x, y))
        Phase.Pressed -> when {
            hypot(x - downX, y - downY) <= slopPx -> emptyList()
            t - downAt >= holdToDragMillis -> {
                phase = Phase.Dragging
                listOf(DirectTouchAction.DragStart(downX, downY), DirectTouchAction.DragMove(x, y))
            }
            else -> {
                phase = Phase.Hovering
                listOf(DirectTouchAction.Hover(x, y))
            }
        }
    }

    fun up(x: Float, y: Float, t: Long): List<DirectTouchAction> {
        val ended = phase
        phase = Phase.Idle
        return when (ended) {
            Phase.Idle, Phase.Hovering -> emptyList()
            Phase.Dragging -> listOf(DirectTouchAction.DragEnd)
            Phase.Pressed -> listOf(click(t))
        }
    }

    /** The gesture stops being one finger (or is taken away): releases a drag, never clicks. */
    fun cancel(): List<DirectTouchAction> {
        val ended = phase
        phase = Phase.Idle
        return if (ended == Phase.Dragging) listOf(DirectTouchAction.DragEnd) else emptyList()
    }

    private fun click(upAt: Long): DirectTouchAction.Click {
        val previousAt = lastClickAt
        val pairsWithPrevious = previousAt != null &&
            downAt - previousAt <= doubleTapMillis &&
            hypot(downX - lastClickX, downY - lastClickY) <= doubleTapSlopPx
        if (!pairsWithPrevious) {
            lastClickX = downX
            lastClickY = downY
        }
        lastClickAt = upAt
        return DirectTouchAction.Click(lastClickX, lastClickY)
    }
}
