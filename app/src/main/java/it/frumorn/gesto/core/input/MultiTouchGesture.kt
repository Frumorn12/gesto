package it.frumorn.gesto.core.input

import kotlin.math.abs
import kotlin.math.hypot

/** What two or more fingers on the remote screen do. */
sealed interface MultiTouchAction {
    /** The fingers' centre moved by ([dx], [dy]) view pixels since the last update. */
    data class Scroll(val dx: Float, val dy: Float) : MultiTouchAction

    data object FinishScroll : MultiTouchAction

    /**
     * Zoom by [factor] around ([focusX], [focusY]), then move the zoomed view by ([panX], [panY]):
     * the fingers' spread and centre since the last update.
     */
    data class Zoom(
        val factor: Float,
        val focusX: Float,
        val focusY: Float,
        val panX: Float,
        val panY: Float,
    ) : MultiTouchAction

    data object RightClick : MultiTouchAction
    data object ToggleKeyboard : MultiTouchAction
}

/**
 * Two or more fingers on the remote screen, from the moment the second one lands until all are up.
 *
 * Nothing is sent until the gesture is clear: if the distance between the fingers changes by more
 * than [decisionSlopPx] before their centre moves that far, it is a pinch (zoom, and pan with the
 * centre); if the centre moves first, it is a scroll. A quick touch that is neither (within
 * [tapSlopPx], at most [tapMaxMillis] after [startMillis]) is a tap: a right click with two
 * fingers, the keyboard with three.
 */
class MultiTouchGesture(
    private val decisionSlopPx: Float,
    private val tapSlopPx: Float,
    private val startMillis: Long,
    private val tapMaxMillis: Long = 300L,
) {
    private enum class Kind { Undecided, Pinch, Scroll }

    private var kind = Kind.Undecided
    private var ids: Set<Long> = emptySet()
    private var maxPointers = 0
    private var originX = 0f
    private var originY = 0f
    private var originSpread = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var lastSpread = 0f
    /** Furthest the centre went from where the fingers landed, for telling a tap. */
    private var maxTravel = 0f
    /** Fewer than two fingers are left: the rest of the gesture does nothing. */
    private var winding = false

    fun update(pointers: List<RemotePointer>, timeMillis: Long): List<MultiTouchAction> {
        val fingers = pointers.filter { it.x.isFinite() && it.y.isFinite() }
        if (winding) return emptyList()
        if (fingers.size < 2) {
            if (ids.isEmpty()) return emptyList()
            winding = true
            return if (kind == Kind.Scroll) listOf(MultiTouchAction.FinishScroll) else emptyList()
        }
        maxPointers = maxOf(maxPointers, fingers.size)
        val centreX = fingers.sumOf { it.x.toDouble() }.toFloat() / fingers.size
        val centreY = fingers.sumOf { it.y.toDouble() }.toFloat() / fingers.size
        val spread = 2f * fingers.sumOf { hypot(it.x - centreX, it.y - centreY).toDouble() }.toFloat() / fingers.size
        val fingerIds = fingers.mapTo(HashSet()) { it.id }
        if (fingerIds != ids) {
            // A finger landing or lifting moves the centre and the spread without anyone moving.
            val first = ids.isEmpty()
            ids = fingerIds
            if (first || kind == Kind.Undecided) {
                originX = centreX
                originY = centreY
                originSpread = spread
            }
            lastX = centreX
            lastY = centreY
            lastSpread = spread
            return emptyList()
        }
        maxTravel = maxOf(maxTravel, hypot(centreX - originX, centreY - originY))
        if (kind == Kind.Undecided) {
            kind = when {
                abs(spread - originSpread) > decisionSlopPx -> Kind.Pinch
                hypot(centreX - originX, centreY - originY) > decisionSlopPx -> Kind.Scroll
                else -> return emptyList()
            }
            // The movement held back while undecided counts from where the fingers landed.
            lastX = originX
            lastY = originY
            lastSpread = originSpread
        }
        val action = when (kind) {
            Kind.Pinch -> MultiTouchAction.Zoom(
                factor = if (lastSpread > 0f) spread / lastSpread else 1f,
                focusX = centreX,
                focusY = centreY,
                panX = centreX - lastX,
                panY = centreY - lastY,
            )
            else -> MultiTouchAction.Scroll(centreX - lastX, centreY - lastY)
        }
        lastX = centreX
        lastY = centreY
        lastSpread = spread
        return listOf(action)
    }

    /** All fingers are up at [timeMillis]. */
    fun end(timeMillis: Long): List<MultiTouchAction> {
        if (ids.isEmpty()) return emptyList()
        val wasWinding = winding
        winding = true
        return when (kind) {
            Kind.Scroll -> if (wasWinding) emptyList() else listOf(MultiTouchAction.FinishScroll)
            Kind.Pinch -> emptyList()
            Kind.Undecided -> {
                val tap = maxTravel <= tapSlopPx && timeMillis - startMillis <= tapMaxMillis
                when {
                    !tap -> emptyList()
                    maxPointers == 2 -> listOf(MultiTouchAction.RightClick)
                    maxPointers == 3 -> listOf(MultiTouchAction.ToggleKeyboard)
                    else -> emptyList()
                }
            }
        }
    }
}
