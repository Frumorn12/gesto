package it.frumorn.gesto.ui.screens

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import it.frumorn.gesto.R
import it.frumorn.gesto.core.input.RemoteGestureAction
import it.frumorn.gesto.core.input.fingerScroll
import it.frumorn.gesto.core.input.RemotePointer
import it.frumorn.gesto.core.input.RemoteTouchpadGestureMachine
import it.frumorn.gesto.core.model.ButtonState
import it.frumorn.gesto.core.model.PointerButton
import it.frumorn.gesto.ui.components.GestoIcons
import it.frumorn.gesto.ui.components.GestoTextButton
import it.frumorn.gesto.ui.components.GestoTileButton
import it.frumorn.gesto.ui.state.TouchpadActions
import it.frumorn.gesto.ui.state.TouchpadInput
import it.frumorn.gesto.ui.state.TouchpadUiState
import it.frumorn.gesto.ui.theme.GestoTheme
import it.frumorn.gesto.ui.theme.ThemeMode
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

private const val TAG = "GestoTouchpad"

/**
 * The PC's screen as the mini screen assumes it, in pixels: the app does not know the real one,
 * and the mini screen only says roughly where the pointer is.
 */
private const val NOMINAL_PC_WIDTH = 1920f
private const val NOMINAL_PC_HEIGHT = 1200f

/** Translucency of the square under a finger. */
private const val FINGER_FILL_ALPHA = 0.18f

/**
 * The touchpad: a dotted pad that moves the PC's pointer, a mini PC screen with roughly where the
 * pointer is, gesture hints for the first sessions, and Left | scroll strip | Right below it.
 *
 * Gestures (see [RemoteTouchpadGestureMachine]): one finger moves, a tap clicks, two fingers scroll,
 * a two-finger tap right-clicks, a three-finger tap opens or closes the keyboard, and holding still
 * before moving drags.
 */
@Composable
fun TouchpadScreen(
    state: TouchpadUiState,
    actions: TouchpadActions,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val currentState by rememberUpdatedState(state)
    val currentActions by rememberUpdatedState(actions)
    // Roughly where the pointer is on the PC (0..1 on each axis), from the moves sent.
    var cursorX by rememberSaveable { mutableFloatStateOf(0.5f) }
    var cursorY by rememberSaveable { mutableFloatStateOf(0.5f) }

    fun buzz(type: HapticFeedbackType) {
        if (currentState.haptics) haptics.performHapticFeedback(type)
    }

    fun click(button: PointerButton) {
        // A click that was dropped (no link, input not approved) must not feel like it worked.
        val sent = currentState.input == TouchpadInput.Ready &&
            currentActions.onPointerButton(button, ButtonState.Pressed)
        if (sent) buzz(HapticFeedbackType.ContextClick)
        currentActions.onPointerButton(button, ButtonState.Released)
    }

    fun handle(action: RemoteGestureAction) {
        when (action) {
            is RemoteGestureAction.Move -> {
                val sent = currentActions.onPointerMove(action.dx, action.dy) ?: return
                cursorX = (cursorX + sent.dx / NOMINAL_PC_WIDTH).coerceIn(0f, 1f)
                cursorY = (cursorY + sent.dy / NOMINAL_PC_HEIGHT).coerceIn(0f, 1f)
            }
            is RemoteGestureAction.Scroll -> fingerScroll(action.dx, action.dy).let { currentActions.onScroll(it.dx, it.dy, false) }
            RemoteGestureAction.FinishScroll -> currentActions.onScroll(0f, 0f, true)
            RemoteGestureAction.Click -> click(PointerButton.Left)
            RemoteGestureAction.RightClick -> click(PointerButton.Right)
            RemoteGestureAction.ToggleKeyboard -> currentActions.onToggleKeyboard()
            RemoteGestureAction.DragStart -> {
                buzz(HapticFeedbackType.LongPress)
                currentActions.onPointerButton(PointerButton.Left, ButtonState.Pressed)
            }
            RemoteGestureAction.DragEnd -> currentActions.onPointerButton(PointerButton.Left, ButtonState.Released)
            is RemoteGestureAction.ModeChanged -> if (action.from != action.to) {
                Log.d(TAG, "gesture_transition from=${action.from.label} to=${action.to.label} reason=${action.reason}")
            }
        }
    }

    // A button held by a drag must not stay down on the PC when the app goes away.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { currentActions.onReleasePointerButtons() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { currentActions.onReleasePointerButtons() }
    DisposableEffect(Unit) { onDispose { currentActions.onReleasePointerButtons() } }

    val spacing = GestoTheme.spacing
    Column(modifier.fillMaxSize()) {
        TouchpadNotices(state, actions)
        Pad(
            hints = state.hints,
            twoFingerRightClick = state.twoFingerRightClick,
            cursor = { Offset(cursorX, cursorY) },
            actions = { currentActions },
            onAction = ::handle,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        Spacer(Modifier.height(spacing.md))
        Row(
            Modifier
                .fillMaxWidth()
                .height(spacing.clickButtonHeight),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            GestoTileButton(
                text = stringResource(R.string.touchpad_left),
                onClick = { click(PointerButton.Left) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            ScrollStrip(
                actions = { currentActions },
                modifier = Modifier
                    .width(spacing.scrollStripWidth)
                    .fillMaxHeight(),
            )
            GestoTileButton(
                text = stringResource(R.string.touchpad_right),
                onClick = { click(PointerButton.Right) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        Spacer(Modifier.height(spacing.md))
    }
}

/** What stands between the phone and the PC's pointer, above the pad: approval, refusal, a controller. */
@Composable
private fun TouchpadNotices(state: TouchpadUiState, actions: TouchpadActions) {
    when (state.input) {
        TouchpadInput.NeedsApproval -> NoticeLine(
            text = stringResource(R.string.touchpad_approve_text),
            action = stringResource(R.string.touchpad_approve_action),
            onAction = actions.onPrepareInput,
        )
        TouchpadInput.Blocked -> NoticeLine(
            text = stringResource(R.string.touchpad_blocked_text),
            action = stringResource(R.string.touchpad_blocked_action),
            onAction = actions.onCheckInput,
        )
        TouchpadInput.Ready -> Unit
    }
    if (state.controllerConnected) NoticeLine(text = stringResource(R.string.touchpad_controller))
}

@Composable
private fun NoticeLine(text: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = GestoTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GestoTheme.spacing.md),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (action != null) GestoTextButton(action, onAction)
    }
}

/** The dotted pad itself, with the finger squares, the mini PC screen and the hints. */
@Composable
private fun Pad(
    hints: Boolean,
    twoFingerRightClick: Boolean,
    cursor: () -> Offset,
    actions: () -> TouchpadActions,
    onAction: (RemoteGestureAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GestoTheme.spacing
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraSmall
    val density = LocalDensity.current
    val tapSlop = with(density) { spacing.tapSlop.toPx() }
    val currentTwoFingerRightClick by rememberUpdatedState(twoFingerRightClick)
    val fingers = remember { mutableStateListOf<Offset>() }
    val description = stringResource(R.string.dock_touchpad)
    Box(
        modifier
            .clip(shape)
            .background(colors.surfaceVariant)
            .dotGrid(colors.outlineVariant, spacing.padDotGap, spacing.padDotRadius)
            .border(spacing.borderWidth, colors.outlineVariant, shape)
            .semantics { contentDescription = description }
            .pointerInput(tapSlop) {
                awaitEachGesture {
                    // A machine per gesture, so a settings change applies from the next touch.
                    val gesture = RemoteTouchpadGestureMachine(
                        tapSlopPx = tapSlop,
                        twoFingerTapEnabled = currentTwoFingerRightClick,
                    )
                    var sessionId = 0L
                    var sessionStarted = false
                    try {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        sessionId = actions().onBeginInteraction()
                        sessionStarted = true
                        fingers.showAt(listOf(down))
                        gesture.begin(RemotePointer(down.id.value, down.position.x, down.position.y), down.uptimeMillis)
                            .forEach(onAction)
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            fingers.showAt(pressed)
                            gesture.update(
                                activePointers = pressed.map { RemotePointer(it.id.value, it.position.x, it.position.y) },
                                timeMillis = event.changes.maxOfOrNull { it.uptimeMillis } ?: down.uptimeMillis,
                            ).forEach(onAction)
                            event.changes.forEach { change ->
                                if (change.changedToDown() || change.changedToUp() || change.positionChanged()) change.consume()
                            }
                            if (pressed.isEmpty()) break
                        }
                    } catch (cancelled: CancellationException) {
                        actions().onPointerCancelled("pointer_input_cancelled")
                        gesture.cancel("pointer_input_cancelled").forEach(onAction)
                        throw cancelled
                    } catch (throwable: Throwable) {
                        actions().onPointerFailed(throwable)
                        gesture.cancel("pointer_input_error").forEach(onAction)
                    } finally {
                        fingers.clear()
                        gesture.cancel("pointer_input_finally").forEach(onAction)
                        if (sessionStarted) actions().onEndInteraction(sessionId)
                        actions().onReleasePointerButtons()
                    }
                }
            },
    ) {
        FingerMarks(fingers, Modifier.matchParentSize())
        MiniScreen(
            cursor = cursor,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(spacing.lg),
        )
        if (hints) {
            GestureHints(
                twoFingerRightClick = twoFingerRightClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(spacing.lg),
            )
        }
    }
}

private fun MutableList<Offset>.showAt(pressed: List<PointerInputChange>) {
    clear()
    pressed.mapTo(this) { it.position }
}

/** A grid of small [color] dots, one every [gap]. */
private fun Modifier.dotGrid(color: Color, gap: Dp, radius: Dp): Modifier =
    drawWithCache {
        val gapPx = gap.toPx()
        val diameter = radius.toPx() * 2
        val area = size
        val dots = buildList {
            var y = gapPx / 2
            while (y < area.height) {
                var x = gapPx / 2
                while (x < area.width) {
                    add(Offset(x, y))
                    x += gapPx
                }
                y += gapPx
            }
        }
        onDrawBehind {
            drawPoints(dots, PointMode.Points, color, strokeWidth = diameter, cap = StrokeCap.Round)
        }
    }

/** A square under each finger touching the pad. */
@Composable
private fun FingerMarks(fingers: List<Offset>, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val spacing = GestoTheme.spacing
    Box(
        modifier.drawBehind {
            val side = spacing.fingerMarkSize.toPx()
            val stroke = spacing.hairline.toPx()
            val markSize = Size(side, side)
            fingers.forEach { finger ->
                val topLeft = Offset(finger.x - side / 2, finger.y - side / 2)
                drawRect(accent.copy(alpha = FINGER_FILL_ALPHA), topLeft, markSize)
                drawRect(accent, topLeft, markSize, style = Stroke(stroke))
            }
        },
    )
}

/** A small PC screen with a square where the pointer roughly is. */
@Composable
private fun MiniScreen(cursor: () -> Offset, modifier: Modifier = Modifier) {
    val spacing = GestoTheme.spacing
    val colors = MaterialTheme.colorScheme
    val description = stringResource(R.string.touchpad_cursor_hint)
    Column(
        modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Box(
            Modifier
                .size(spacing.miniScreenWidth, spacing.miniScreenHeight)
                .background(colors.background)
                .border(spacing.borderWidth, colors.outlineVariant),
        ) {
            Box(
                Modifier
                    .offset {
                        // Read here, at layout: a move repositions the square without recomposing.
                        val at = cursor()
                        val free = (spacing.miniScreenWidth - spacing.miniCursorSize).toPx()
                        val freeY = (spacing.miniScreenHeight - spacing.miniCursorSize).toPx()
                        IntOffset((free * at.x).roundToInt(), (freeY * at.y).roundToInt())
                    }
                    .size(spacing.miniCursorSize)
                    .background(colors.primary),
            )
        }
        Text(
            stringResource(R.string.touchpad_pc_screen),
            style = GestoTheme.accentTypography.telemetry,
            color = colors.onSurfaceVariant,
        )
    }
}

/** The gestures, as small chips along the bottom of the pad. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GestureHints(twoFingerRightClick: Boolean, modifier: Modifier = Modifier) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(GestoTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(GestoTheme.spacing.xs),
    ) {
        HintChip(R.string.touchpad_hint_move, fingers = 1)
        HintChip(R.string.touchpad_hint_tap)
        HintChip(R.string.touchpad_hint_scroll, fingers = 2)
        if (twoFingerRightClick) HintChip(R.string.touchpad_hint_right_click, fingers = 2)
        HintChip(R.string.touchpad_hint_keyboard, fingers = 3)
    }
}

@Composable
private fun HintChip(@StringRes text: Int, fingers: Int? = null) {
    val spacing = GestoTheme.spacing
    val colors = MaterialTheme.colorScheme
    val template = stringResource(text)
    val label = if (fingers == null) {
        AnnotatedString(template)
    } else {
        highlightPlaceholder(template, fingers.toString(), SpanStyle(color = colors.primary, fontWeight = FontWeight.Bold))
    }
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = colors.onSurfaceVariant,
        modifier = Modifier
            .background(colors.background, MaterialTheme.shapes.extraSmall)
            .border(spacing.borderWidth, colors.outlineVariant, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
    )
}

/** [template] with its `%1$s` replaced by [value] in [style], wherever the language puts it. */
internal fun highlightPlaceholder(template: String, value: String, style: SpanStyle): AnnotatedString {
    val placeholder = "%1\$s"
    val at = template.indexOf(placeholder)
    if (at < 0) return AnnotatedString(template)
    return buildAnnotatedString {
        append(template.substring(0, at))
        withStyle(style) { append(value) }
        append(template.substring(at + placeholder.length))
    }
}

/** Between Left and Right: drag up or down on it to scroll; the page follows the finger, see [fingerScroll]. */
@Composable
private fun ScrollStrip(actions: () -> TouchpadActions, modifier: Modifier = Modifier) {
    val spacing = GestoTheme.spacing
    val colors = MaterialTheme.colorScheme
    val line = colors.outlineVariant
    val description = stringResource(R.string.touchpad_scroll_strip)
    Box(
        modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .drawBehind {
                val gap = spacing.scrollStripLineGap.toPx()
                val stroke = spacing.borderWidth.toPx()
                var y = size.height - stroke / 2
                while (y > 0f) {
                    drawLine(line, Offset(0f, y), Offset(size.width, y), stroke)
                    y -= gap
                }
            }
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                var sessionId = 0L
                fun finish() {
                    actions().onScroll(0f, 0f, true)
                    actions().onEndInteraction(sessionId)
                }
                detectVerticalDragGestures(
                    onDragStart = { sessionId = actions().onBeginInteraction() },
                    onDragEnd = ::finish,
                    onDragCancel = ::finish,
                ) { change, dy ->
                    change.consume()
                    fingerScroll(0f, dy).let { actions().onScroll(it.dx, it.dy, false) }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            GestoIcons.ScrollVertical,
            contentDescription = null,
            tint = colors.outline,
            modifier = Modifier.size(spacing.barIconSize),
        )
    }
}

@Preview(name = "Touchpad - hints - light", widthDp = 412, heightDp = 720)
@Composable
private fun TouchpadScreenPreviewLight() = GestoTheme(mode = ThemeMode.Light) {
    Surface(color = MaterialTheme.colorScheme.background) {
        TouchpadScreen(
            state = TouchpadUiState(hints = true),
            actions = TouchpadActions(),
            modifier = Modifier.padding(GestoTheme.spacing.gutter),
        )
    }
}

@Preview(name = "Touchpad - approval, controller - dark", widthDp = 412, heightDp = 720)
@Composable
private fun TouchpadScreenPreviewDark() = GestoTheme(mode = ThemeMode.Dark) {
    Surface(color = MaterialTheme.colorScheme.background) {
        TouchpadScreen(
            state = TouchpadUiState(input = TouchpadInput.NeedsApproval, controllerConnected = true, twoFingerRightClick = false, hints = true),
            actions = TouchpadActions(),
            modifier = Modifier.padding(GestoTheme.spacing.gutter),
        )
    }
}
