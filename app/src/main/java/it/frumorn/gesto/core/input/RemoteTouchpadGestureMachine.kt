package it.frumorn.gesto.core.input

import kotlin.math.abs
import kotlin.math.hypot

data class RemotePointer(
    val id: Long,
    val x: Float,
    val y: Float,
) {
    val position: PointerPosition
        get() = PointerPosition(x, y)
}

data class PointerPosition(
    val x: Float,
    val y: Float,
) {
    operator fun minus(other: PointerPosition): PointerDelta = PointerDelta(x - other.x, y - other.y)

    fun distanceTo(other: PointerPosition): Float = hypot((x - other.x).toDouble(), (y - other.y).toDouble()).toFloat()
}

data class PointerDelta(
    val dx: Float,
    val dy: Float,
) {
    fun isSignificant(threshold: Float): Boolean = abs(dx) + abs(dy) > threshold
}

enum class RemoteGestureMode(val label: String) {
    Idle("Idle"),
    SinglePointerDown("SinglePointerDown"),
    MovingPointer("MovingPointer"),
    TwoFingerScroll("TwoFingerScroll"),
    Dragging("Dragging"),
    GestureCancelled("GestureCancelled"),
    Recovering("Recovering"),
}

sealed interface RemoteGestureAction {
    data class Move(val dx: Float, val dy: Float) : RemoteGestureAction
    data class Scroll(val dx: Float, val dy: Float) : RemoteGestureAction
    data object FinishScroll : RemoteGestureAction
    data object Click : RemoteGestureAction
    data object RightClick : RemoteGestureAction
    data object ToggleKeyboard : RemoteGestureAction
    data object DragStart : RemoteGestureAction
    data object DragEnd : RemoteGestureAction
    data class ModeChanged(
        val from: RemoteGestureMode,
        val to: RemoteGestureMode,
        val reason: String,
    ) : RemoteGestureAction
}

/**
 * Pure state machine for the remote touchpad. Compose supplies pointer snapshots; this class owns
 * the gesture transitions so multi-touch changes cannot implicitly reset cursor/scroll state.
 *
 * - One finger moves the pointer. Lifted within [tapMaxMillis] and inside [tapSlopPx]: [RemoteGestureAction.Click].
 * - Held still for [longPressTimeoutMillis], then moved: a drag ([RemoteGestureAction.DragStart]).
 * - Two or more fingers scroll, once their centre has moved past the slop (a tap stays a tap).
 * - Two fingers down and up within [multiTapMaxMillis], centre within the slop and no scroll:
 *   [RemoteGestureAction.RightClick] (if [twoFingerTapEnabled]). Three fingers the same way:
 *   [RemoteGestureAction.ToggleKeyboard].
 */
class RemoteTouchpadGestureMachine(
    private val tapSlopPx: Float,
    private val moveThresholdPx: Float = 0.05f,
    private val longPressTimeoutMillis: Long = 500L,
    private val longPressDragEnabled: Boolean = true,
    private val tapMaxMillis: Long = 250L,
    private val multiTapMaxMillis: Long = 300L,
    private val twoFingerTapEnabled: Boolean = true,
) {
    private var state: GestureState = GestureState.Idle
    private var maxPointerCount = 0
    private var movedBeyondTap = false
    private var sawScroll = false
    private var dragActive = false
    private var downTimeMillis = 0L
    private var downPosition = PointerPosition(0f, 0f)
    /** Where each finger of the gesture first touched. */
    private val origins = mutableMapOf<Long, PointerPosition>()
    /** The fingers' centre has stayed within the slop and nothing dragged: still a tap with several fingers. */
    private var multiTapCandidate = true
    /** Scroll deltas are being sent: the fingers left the slop, or held on too long to be a tap. */
    private var scrolling = false

    val mode: RemoteGestureMode
        get() = state.mode

    fun begin(pointer: RemotePointer, timeMillis: Long): List<RemoteGestureAction> {
        val actions = mutableListOf<RemoteGestureAction>()
        if (state !is GestureState.Idle) {
            actions += cancel("new_pointer_down_before_idle")
        }
        maxPointerCount = 1
        movedBeyondTap = false
        sawScroll = false
        dragActive = false
        downTimeMillis = timeMillis
        downPosition = pointer.position
        origins.clear()
        origins[pointer.id] = pointer.position
        multiTapCandidate = true
        scrolling = false
        actions += transitionTo(GestureState.SinglePointerDown(pointer.id, pointer.position), "pointer_down")
        return actions
    }

    fun update(activePointers: List<RemotePointer>, timeMillis: Long): List<RemoteGestureAction> {
        val pointers = activePointers.filter { it.x.isFinite() && it.y.isFinite() }
        maxPointerCount = maxOf(maxPointerCount, pointers.size)
        if (pointers.isEmpty()) return end(timeMillis, "all_pointers_up")

        // Harmless when Idle: begin() below starts the bookkeeping over.
        pointers.forEach { origins.getOrPut(it.id) { it.position } }
        if (multiTapCandidate && centreTravel(pointers) > tapSlopPx) multiTapCandidate = false

        return when (val current = state) {
            GestureState.Idle -> begin(pointers.first(), timeMillis)
            is GestureState.SinglePointerDown -> updateSinglePointer(current.pointerId, current.lastPosition, pointers, timeMillis)
            is GestureState.MovingPointer -> updateSinglePointer(current.pointerId, current.lastPosition, pointers, timeMillis)
            is GestureState.Dragging -> updateDragging(current.pointerId, current.lastPosition, pointers)
            is GestureState.TwoFingerScroll -> updateTwoFingerScroll(current, pointers, timeMillis)
            is GestureState.Recovering -> updateRecovering(pointers)
            GestureState.Cancelled -> cancel("cancelled_state_update")
        }
    }

    /** Every finger is up at [timeMillis]: finishes what was running and reports a tap, if it was one. */
    fun end(timeMillis: Long, reason: String = "gesture_end"): List<RemoteGestureAction> {
        if (state is GestureState.Idle) return emptyList()
        val actions = mutableListOf<RemoteGestureAction>()
        if (state is GestureState.TwoFingerScroll && sawScroll) {
            actions += RemoteGestureAction.FinishScroll
        }
        if (dragActive) {
            actions += RemoteGestureAction.DragEnd
        }
        tapAction(timeMillis - downTimeMillis)?.let { actions += it }
        actions += transitionTo(GestureState.Idle, reason)
        resetGestureBookkeeping()
        return actions
    }

    fun cancel(reason: String = "gesture_cancelled"): List<RemoteGestureAction> {
        if (state is GestureState.Idle) return emptyList()
        val actions = mutableListOf<RemoteGestureAction>()
        if (state is GestureState.TwoFingerScroll && sawScroll) {
            actions += RemoteGestureAction.FinishScroll
        }
        if (dragActive) {
            actions += RemoteGestureAction.DragEnd
        }
        actions += transitionTo(GestureState.Cancelled, reason)
        actions += transitionTo(GestureState.Idle, "cancel_reset")
        resetGestureBookkeeping()
        return actions
    }

    /** The tap a gesture lasting [durationMillis] was, if any. */
    private fun tapAction(durationMillis: Long): RemoteGestureAction? {
        val single = state is GestureState.SinglePointerDown || state is GestureState.MovingPointer
        val multiTap = multiTapCandidate && !scrolling && durationMillis <= multiTapMaxMillis
        return when {
            maxPointerCount == 1 -> RemoteGestureAction.Click.takeIf { single && !movedBeyondTap && durationMillis <= tapMaxMillis }
            maxPointerCount == 2 -> RemoteGestureAction.RightClick.takeIf { multiTap && twoFingerTapEnabled }
            maxPointerCount == 3 -> RemoteGestureAction.ToggleKeyboard.takeIf { multiTap }
            else -> null
        }
    }

    private fun updateSinglePointer(
        pointerId: Long,
        lastPosition: PointerPosition,
        pointers: List<RemotePointer>,
        timeMillis: Long,
    ): List<RemoteGestureAction> {
        if (pointers.size >= 2) return enterScroll(pointers, "second_pointer_down")
        val pointer = pointers.firstOrNull { it.id == pointerId } ?: return recover(pointers, "primary_pointer_missing")
        val delta = pointer.position - lastPosition
        val canStartDrag = shouldStartDrag(pointer.position, delta, timeMillis)
        if (pointer.position.distanceTo(downPosition) > tapSlopPx) {
            movedBeyondTap = true
        }

        val actions = mutableListOf<RemoteGestureAction>()
        when {
            canStartDrag -> {
                movedBeyondTap = true
                multiTapCandidate = false
                dragActive = true
                actions += transitionTo(GestureState.Dragging(pointer.id, pointer.position), "long_press_drag")
                actions += RemoteGestureAction.DragStart
                if (delta.isSignificant(moveThresholdPx)) actions += RemoteGestureAction.Move(delta.dx, delta.dy)
            }
            delta.isSignificant(moveThresholdPx) -> {
                val next = if (movedBeyondTap) {
                    GestureState.MovingPointer(pointer.id, pointer.position)
                } else {
                    GestureState.SinglePointerDown(pointer.id, pointer.position)
                }
                actions += transitionTo(next, if (movedBeyondTap) "pointer_move" else "pointer_jitter")
                actions += RemoteGestureAction.Move(delta.dx, delta.dy)
            }
            else -> {
                state = if (movedBeyondTap) {
                    GestureState.MovingPointer(pointer.id, pointer.position)
                } else {
                    GestureState.SinglePointerDown(pointer.id, pointer.position)
                }
            }
        }
        return actions
    }

    private fun updateDragging(
        pointerId: Long,
        lastPosition: PointerPosition,
        pointers: List<RemotePointer>,
    ): List<RemoteGestureAction> {
        if (pointers.size >= 2) {
            dragActive = false
            return listOf(RemoteGestureAction.DragEnd) + enterScroll(pointers, "second_pointer_down_while_dragging")
        }
        val pointer = pointers.firstOrNull { it.id == pointerId }
            ?: return listOf(RemoteGestureAction.DragEnd).also { dragActive = false } + recover(pointers, "drag_pointer_missing")
        val delta = pointer.position - lastPosition
        state = GestureState.Dragging(pointer.id, pointer.position)
        return if (delta.isSignificant(moveThresholdPx)) listOf(RemoteGestureAction.Move(delta.dx, delta.dy)) else emptyList()
    }

    private fun updateTwoFingerScroll(
        current: GestureState.TwoFingerScroll,
        pointers: List<RemotePointer>,
        timeMillis: Long,
    ): List<RemoteGestureAction> {
        if (pointers.size < 2) {
            val actions = mutableListOf<RemoteGestureAction>()
            if (sawScroll) actions += RemoteGestureAction.FinishScroll
            actions += transitionTo(recoveringState(pointers), "scroll_pointer_lifted")
            return actions
        }
        val centroid = centroidOf(pointers)
        val ids = pointers.idSet()
        // A finger landing or lifting moves the centre without anyone scrolling: start again from here.
        if (ids != current.pointerIds) {
            state = GestureState.TwoFingerScroll(centroid, ids)
            return emptyList()
        }
        if (!scrolling) {
            // Held back while the fingers may still be tapping, so a tap never scrolls the page.
            scrolling = !multiTapCandidate || timeMillis - downTimeMillis > multiTapMaxMillis
            if (!scrolling) return emptyList()
        }
        val delta = centroid - current.lastCentroid
        state = GestureState.TwoFingerScroll(centroid, ids)
        if (!delta.isSignificant(moveThresholdPx)) return emptyList()
        // Only a scroll that really moved needs finishing; a two-finger tap is just a right click.
        sawScroll = true
        return listOf(RemoteGestureAction.Scroll(delta.dx, delta.dy))
    }

    private fun updateRecovering(pointers: List<RemotePointer>): List<RemoteGestureAction> {
        if (pointers.size >= 2) return enterScroll(pointers, "second_pointer_returned")
        val pointer = pointers.first()
        val current = state as? GestureState.Recovering
        val lastPosition = current?.lastPosition
        if (current?.pointerId == pointer.id && lastPosition != null) {
            val delta = pointer.position - lastPosition
            val actions = mutableListOf<RemoteGestureAction>()
            actions += transitionTo(GestureState.MovingPointer(pointer.id, pointer.position), "recovered_single_pointer")
            if (delta.isSignificant(moveThresholdPx)) {
                actions += RemoteGestureAction.Move(delta.dx, delta.dy)
            }
            return actions
        }
        state = GestureState.Recovering(pointer.id, pointer.position)
        return emptyList()
    }

    private fun enterScroll(pointers: List<RemotePointer>, reason: String): List<RemoteGestureAction> {
        movedBeyondTap = true
        return listOf(transitionTo(GestureState.TwoFingerScroll(centroidOf(pointers), pointers.idSet()), reason))
    }

    private fun recover(pointers: List<RemotePointer>, reason: String): List<RemoteGestureAction> {
        val actions = mutableListOf<RemoteGestureAction>()
        if (dragActive) {
            dragActive = false
            actions += RemoteGestureAction.DragEnd
        }
        actions += transitionTo(recoveringState(pointers), reason)
        return actions
    }

    private fun recoveringState(pointers: List<RemotePointer>): GestureState {
        val pointer = pointers.firstOrNull()
        return GestureState.Recovering(pointer?.id, pointer?.position)
    }

    private fun shouldStartDrag(position: PointerPosition, delta: PointerDelta, timeMillis: Long): Boolean {
        return longPressDragEnabled &&
            !dragActive &&
            maxPointerCount == 1 &&
            !movedBeyondTap &&
            timeMillis - downTimeMillis >= longPressTimeoutMillis &&
            position.distanceTo(downPosition) <= tapSlopPx * 1.5f &&
            delta.isSignificant(moveThresholdPx)
    }

    private fun transitionTo(next: GestureState, reason: String): RemoteGestureAction.ModeChanged {
        val previousMode = state.mode
        state = next
        return RemoteGestureAction.ModeChanged(previousMode, next.mode, reason)
    }

    private fun resetGestureBookkeeping() {
        maxPointerCount = 0
        movedBeyondTap = false
        sawScroll = false
        dragActive = false
        downTimeMillis = 0L
        downPosition = PointerPosition(0f, 0f)
        origins.clear()
        multiTapCandidate = true
        scrolling = false
    }

    /**
     * How far the centre of [pointers] is from the centre of the same fingers where they first
     * touched; fingers landing or lifting do not count as movement.
     */
    private fun centreTravel(pointers: List<RemotePointer>): Float {
        val start = centroidOfPositions(pointers.map { origins.getValue(it.id) })
        return centroidOf(pointers).distanceTo(start)
    }

    private fun List<RemotePointer>.idSet(): Set<Long> = mapTo(HashSet()) { it.id }

    private fun centroidOfPositions(positions: List<PointerPosition>): PointerPosition {
        val x = positions.sumOf { it.x.toDouble() }.toFloat() / positions.size
        val y = positions.sumOf { it.y.toDouble() }.toFloat() / positions.size
        return PointerPosition(x, y)
    }

    private fun centroidOf(pointers: List<RemotePointer>): PointerPosition {
        val x = pointers.sumOf { it.x.toDouble() }.toFloat() / pointers.size
        val y = pointers.sumOf { it.y.toDouble() }.toFloat() / pointers.size
        return PointerPosition(x, y)
    }

    private sealed interface GestureState {
        val mode: RemoteGestureMode

        data object Idle : GestureState {
            override val mode = RemoteGestureMode.Idle
        }

        data class SinglePointerDown(
            val pointerId: Long,
            val lastPosition: PointerPosition,
        ) : GestureState {
            override val mode = RemoteGestureMode.SinglePointerDown
        }

        data class MovingPointer(
            val pointerId: Long,
            val lastPosition: PointerPosition,
        ) : GestureState {
            override val mode = RemoteGestureMode.MovingPointer
        }

        data class TwoFingerScroll(
            val lastCentroid: PointerPosition,
            /** The fingers [lastCentroid] was measured on. */
            val pointerIds: Set<Long>,
        ) : GestureState {
            override val mode = RemoteGestureMode.TwoFingerScroll
        }

        data class Dragging(
            val pointerId: Long,
            val lastPosition: PointerPosition,
        ) : GestureState {
            override val mode = RemoteGestureMode.Dragging
        }

        data class Recovering(
            val pointerId: Long?,
            val lastPosition: PointerPosition?,
        ) : GestureState {
            override val mode = RemoteGestureMode.Recovering
        }

        data object Cancelled : GestureState {
            override val mode = RemoteGestureMode.GestureCancelled
        }
    }
}
