package it.frumorn.gesto.ui.screens

import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import it.frumorn.gesto.core.input.DirectTouchAction
import it.frumorn.gesto.core.input.DirectTouchMachine
import it.frumorn.gesto.core.input.MultiTouchAction
import it.frumorn.gesto.core.input.MultiTouchGesture
import it.frumorn.gesto.core.input.PointerDelta
import it.frumorn.gesto.core.input.RemoteGestureAction
import it.frumorn.gesto.core.input.RemotePointer
import it.frumorn.gesto.core.input.RemoteTouchpadGestureMachine
import it.frumorn.gesto.core.input.fingerScroll
import it.frumorn.gesto.core.model.ButtonState
import it.frumorn.gesto.core.model.PointerButton
import it.frumorn.gesto.core.model.TouchMode
import it.frumorn.gesto.core.screen.DesktopPoint
import it.frumorn.gesto.core.screen.ScreenViewport
import it.frumorn.gesto.core.video.ZoomTransform
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "GestoRemoteScreen"

/** Damping applied to two-finger scroll deltas before they are sent to the host. */
private const val SCROLL_DAMPING = 0.75f

/** What a touch on the remote screen can make the PC do. */
@Immutable
data class RemoteDisplayGestureCallbacks(
    /** Absolute pointer position on the PC's screen (Direct mode). */
    val onDesktopPointerMove: (x: Float, y: Float) -> Unit,
    /** Left click at a point of the PC's screen (Direct mode). */
    val onDesktopClick: (x: Float, y: Float) -> Unit,
    /** Relative pointer move (Touchpad mode); returns what was sent, or null when nothing was. */
    val onPointerMove: (dx: Float, dy: Float) -> PointerDelta?,
    val onPointerButton: (PointerButton, ButtonState) -> Unit,
    val onScroll: (dx: Float, dy: Float, finish: Boolean) -> Unit,
    val onToggleKeyboard: () -> Unit,
    /** A light vibration, when the user wants them. */
    val onHaptic: (HapticFeedbackType) -> Unit,
)

/** How far a finger may wander for each gesture, in pixels. */
@Immutable
data class RemoteDisplaySlops(
    /** Still a tap. */
    val tapPx: Float,
    /** Two fingers: pinch or scroll, see [MultiTouchGesture]. */
    val multiTouchPx: Float,
    /** A second tap this close clicks on the first, in Direct mode. */
    val doubleTapPx: Float,
)

/**
 * Touches on the remote screen.
 *
 * One finger follows the [touchMode]: in [TouchMode.Touchpad] it drives the pointer like the
 * touchpad does ([RemoteTouchpadGestureMachine]: move, tap to click, hold then move to drag); in
 * [TouchMode.Direct] it works where it touches ([DirectTouchMachine]). As soon as a second finger
 * lands the one-finger gesture is dropped and [MultiTouchGesture] decides between pinch (zoom and
 * pan, through [zoom] and [onZoom]) and scroll; a quick two-finger tap is a right click (if
 * [twoFingerRightClick]), a three-finger tap opens the keyboard.
 *
 * Direct mode maps view points onto the PC's screen through the zoom and [ScreenViewport], which
 * assumes the video is letterboxed with fit-centre semantics inside this same box (see
 * [RemoteDisplayVideoSurface]); touches on the letterbox do nothing there.
 *
 * @param frameWidth width in pixels of the stream, `null` while nothing has arrived
 * @param sourceKey id of the capture source; a change restarts the recogniser
 */
fun Modifier.remoteDisplayGestures(
    touchMode: TouchMode,
    frameWidth: Int?,
    frameHeight: Int?,
    sourceKey: String?,
    slops: RemoteDisplaySlops,
    twoFingerRightClick: Boolean,
    callbacks: RemoteDisplayGestureCallbacks,
    zoom: () -> ZoomTransform,
    onZoom: (ZoomTransform) -> Unit,
): Modifier = pointerInput(touchMode, frameWidth, frameHeight, sourceKey, slops, twoFingerRightClick, callbacks) {
    // Built once for the whole block, NOT per gesture: it has to remember the previous click to
    // land a quick second tap on it (the stable double click). The block restarts, and so forgets
    // it, only when the touch mode, the stream or the slops change.
    val direct = if (touchMode == TouchMode.Direct) {
        DirectTouchMachine(slopPx = slops.tapPx, doubleTapSlopPx = slops.doubleTapPx)
    } else {
        null
    }
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        val width = size.width.toFloat()
        val height = size.height.toFloat()
        val viewport = if (frameWidth != null && frameHeight != null && width > 0f && height > 0f) {
            ScreenViewport(width, height, frameWidth.toFloat(), frameHeight.toFloat())
        } else {
            null
        }

        fun desktop(x: Float, y: Float): DesktopPoint? {
            val (contentX, contentY) = zoom().toContent(x, y)
            return viewport?.map(contentX, contentY)
        }

        fun click(button: PointerButton) {
            callbacks.onHaptic(HapticFeedbackType.ContextClick)
            callbacks.onPointerButton(button, ButtonState.Pressed)
            callbacks.onPointerButton(button, ButtonState.Released)
        }

        // Direct mode presses the button only where the PC's screen is: a drag that started on
        // the letterbox has nothing to release.
        var directPressed = false
        fun onDirect(action: DirectTouchAction) {
            when (action) {
                is DirectTouchAction.Hover -> desktop(action.x, action.y)?.let { callbacks.onDesktopPointerMove(it.x, it.y) }
                is DirectTouchAction.Click -> desktop(action.x, action.y)?.let {
                    callbacks.onHaptic(HapticFeedbackType.ContextClick)
                    callbacks.onDesktopClick(it.x, it.y)
                }
                is DirectTouchAction.DragStart -> desktop(action.x, action.y)?.let {
                    callbacks.onHaptic(HapticFeedbackType.LongPress)
                    callbacks.onDesktopPointerMove(it.x, it.y)
                    callbacks.onPointerButton(PointerButton.Left, ButtonState.Pressed)
                    directPressed = true
                }
                is DirectTouchAction.DragMove -> desktop(action.x, action.y)?.let { callbacks.onDesktopPointerMove(it.x, it.y) }
                DirectTouchAction.DragEnd -> if (directPressed) {
                    directPressed = false
                    callbacks.onPointerButton(PointerButton.Left, ButtonState.Released)
                }
            }
        }

        fun onPad(action: RemoteGestureAction) {
            when (action) {
                is RemoteGestureAction.Move -> callbacks.onPointerMove(action.dx, action.dy)
                RemoteGestureAction.Click -> click(PointerButton.Left)
                RemoteGestureAction.DragStart -> {
                    callbacks.onHaptic(HapticFeedbackType.LongPress)
                    callbacks.onPointerButton(PointerButton.Left, ButtonState.Pressed)
                }
                RemoteGestureAction.DragEnd -> callbacks.onPointerButton(PointerButton.Left, ButtonState.Released)
                is RemoteGestureAction.ModeChanged -> if (action.from != action.to) {
                    Log.d(TAG, "pad_transition from=${action.from.label} to=${action.to.label} reason=${action.reason}")
                }
                // Several fingers never reach the touchpad machine here: MultiTouchGesture has them.
                is RemoteGestureAction.Scroll,
                RemoteGestureAction.FinishScroll,
                RemoteGestureAction.RightClick,
                RemoteGestureAction.ToggleKeyboard -> Unit
            }
        }

        fun onMulti(action: MultiTouchAction) {
            when (action) {
                // The page follows the fingers, as everywhere in the app.
                is MultiTouchAction.Scroll -> fingerScroll(action.dx, action.dy, SCROLL_DAMPING)
                    .let { callbacks.onScroll(it.dx, it.dy, false) }
                MultiTouchAction.FinishScroll -> callbacks.onScroll(0f, 0f, true)
                is MultiTouchAction.Zoom -> onZoom(
                    zoom()
                        .zoomBy(action.factor, action.focusX, action.focusY, width, height)
                        .panBy(action.panX, action.panY, width, height),
                )
                MultiTouchAction.RightClick -> if (twoFingerRightClick) click(PointerButton.Right)
                MultiTouchAction.ToggleKeyboard -> callbacks.onToggleKeyboard()
            }
        }

        // Built per gesture, like on the touchpad, so a setting change applies from the next touch.
        val pad = if (touchMode == TouchMode.Touchpad) {
            RemoteTouchpadGestureMachine(tapSlopPx = slops.tapPx, twoFingerTapEnabled = false)
        } else {
            null
        }
        var multi: MultiTouchGesture? = null
        direct?.down(down.position.x, down.position.y, down.uptimeMillis)?.forEach(::onDirect)
        pad?.begin(down.pointer(), down.uptimeMillis)?.forEach(::onPad)
        try {
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                val now = event.changes.maxOfOrNull { it.uptimeMillis } ?: down.uptimeMillis
                if (multi == null && pressed.size >= 2) {
                    // A second finger: whatever one finger was doing is over, without a click.
                    direct?.cancel()?.forEach(::onDirect)
                    pad?.cancel("second_finger")?.forEach(::onPad)
                    multi = MultiTouchGesture(slops.multiTouchPx, slops.tapPx, down.uptimeMillis)
                }
                val fingers = multi
                if (fingers != null) {
                    fingers.update(pressed.map { it.pointer() }, now).forEach(::onMulti)
                } else {
                    pressed.firstOrNull()?.let { finger ->
                        direct?.move(finger.position.x, finger.position.y, now)?.forEach(::onDirect)
                        pad?.update(listOf(finger.pointer()), now)?.forEach(::onPad)
                    }
                }
                event.changes.forEach { change ->
                    if (change.changedToDown() || change.changedToUp() || change.positionChanged()) change.consume()
                }
                if (pressed.isEmpty()) {
                    if (fingers != null) {
                        fingers.end(now).forEach(::onMulti)
                    } else {
                        val lifted = event.changes.firstOrNull { it.changedToUp() } ?: down
                        direct?.up(lifted.position.x, lifted.position.y, now)?.forEach(::onDirect)
                        pad?.end(now)?.forEach(::onPad)
                    }
                    break
                }
            }
        } catch (cancelled: CancellationException) {
            Log.w(TAG, "remote_screen_gesture_cancelled")
            throw cancelled
        } finally {
            // Whatever was left half done (a drag, a scroll) is closed; after a normal end these
            // have nothing left to do.
            direct?.cancel()?.forEach(::onDirect)
            pad?.cancel("gesture_finally")?.forEach(::onPad)
            multi?.update(emptyList(), down.uptimeMillis)?.forEach(::onMulti)
        }
    }
}

private fun PointerInputChange.pointer(): RemotePointer = RemotePointer(id.value, position.x, position.y)
