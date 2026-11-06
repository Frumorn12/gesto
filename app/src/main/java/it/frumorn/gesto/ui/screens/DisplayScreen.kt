package it.frumorn.gesto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import it.frumorn.gesto.R
import it.frumorn.gesto.core.model.ButtonState
import it.frumorn.gesto.core.model.PointerButton
import it.frumorn.gesto.core.model.ScreenStreamStats
import it.frumorn.gesto.core.model.TouchMode
import it.frumorn.gesto.core.video.ZoomTransform
import it.frumorn.gesto.ui.components.GestoButton
import it.frumorn.gesto.ui.components.GestoIcons
import it.frumorn.gesto.ui.components.GestoQuickTile
import it.frumorn.gesto.ui.session.label
import it.frumorn.gesto.ui.state.RemoteDisplayActions
import it.frumorn.gesto.ui.state.RemoteDisplayUiState
import it.frumorn.gesto.ui.theme.GestoTheme
import it.frumorn.gesto.ui.theme.ThemeMode
import kotlin.math.roundToInt

/** The video's proportions until the first frame says otherwise (a 1920 × 1200 desktop). */
private const val DEFAULT_ASPECT_RATIO = 16f / 10f

/** Background of the stats line over the video. */
private const val STATS_BACKGROUND_ALPHA = 0.7f

/**
 * The remote screen.
 *
 * Upright: the PC's screen across the whole width, then three quick actions (Right click,
 * Keyboard, Audio), a hint to turn the phone and a line on how touches work; the session bars stay.
 * On its side, or in Game mode ([RemoteDisplayUiState.fullscreen]): only the video, edge to edge;
 * the shell hides the bars and the purple quick-menu button stays on top.
 *
 * The video output is a slot ([videoSurface]) so the rendering backend can be swapped - see
 * [RemoteDisplayVideoSurface] for the contract it has to honour. The video area is always dark,
 * whatever the theme.
 */
@Composable
fun DisplayScreen(
    state: RemoteDisplayUiState,
    actions: RemoteDisplayActions,
    modifier: Modifier = Modifier,
    // Empty by default so previews render without a decoder; the app shell supplies the real one.
    videoSurface: @Composable () -> Unit = {},
) {
    val currentActions by rememberUpdatedState(actions)
    // A button held by a drag must not stay down on the PC when the app goes away.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { currentActions.onReleasePointerButtons() }
    DisposableEffect(Unit) { onDispose { currentActions.onReleasePointerButtons() } }

    val spacing = GestoTheme.spacing
    val ratio = state.frameWidth?.let { w -> state.frameHeight?.takeIf { it > 0 }?.let { h -> w.toFloat() / h } }
        ?: DEFAULT_ASPECT_RATIO
    Column(modifier.fillMaxSize()) {
        // One call site, always the first child: turning the phone or switching Game mode only
        // changes its size, so the video view (and the decoder's surface) survives. Two call sites
        // would release the view and restart the decoder on every turn, with a black gap.
        RemoteVideo(
            state = state,
            actions = actions,
            videoSurface = videoSurface,
            modifier = if (state.fullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
            },
        )
        if (!state.fullscreen) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.gutter, vertical = spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                QuickTiles(state, actions)
                RotateHint()
                TouchModeLine(state.touchMode)
            }
        }
    }
}

/** The video, its touches and pinch zoom, the empty state and the stats line. */
@Composable
private fun RemoteVideo(
    state: RemoteDisplayUiState,
    actions: RemoteDisplayActions,
    videoSurface: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GestoTheme.spacing
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val currentState by rememberUpdatedState(state)
    val currentActions by rememberUpdatedState(actions)
    val slops = with(density) {
        RemoteDisplaySlops(
            tapPx = spacing.tapSlop.toPx(),
            multiTouchPx = spacing.multiTouchSlop.toPx(),
            doubleTapPx = spacing.doubleTapSlop.toPx(),
        )
    }
    val callbacks = remember {
        RemoteDisplayGestureCallbacks(
            onDesktopPointerMove = { x, y -> currentActions.onDesktopPointerMove(x, y) },
            onDesktopClick = { x, y -> currentActions.onDesktopClick(x, y) },
            onPointerMove = { dx, dy -> currentActions.onPointerMove(dx, dy) },
            onPointerButton = { button, buttonState -> currentActions.onPointerButton(button, buttonState) },
            onScroll = { dx, dy, finish -> currentActions.onScroll(dx, dy, finish) },
            onToggleKeyboard = { currentActions.onToggleKeyboard() },
            onHaptic = { type: HapticFeedbackType -> if (currentState.haptics) haptics.performHapticFeedback(type) },
        )
    }
    // One holder for the whole life of the box: the gesture recogniser keeps reading and writing it.
    val zoom = remember { mutableStateOf(ZoomTransform()) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    // Another source, frame size or box size (the phone turned): the zoom no longer fits, start over.
    LaunchedEffect(state.sourceKey, state.frameWidth, state.frameHeight, viewSize) { zoom.value = ZoomTransform() }
    Box(
        modifier
            .clipToBounds()
            .background(GestoTheme.colors.video)
            .onSizeChanged { viewSize = it },
        contentAlignment = Alignment.Center,
    ) {
        // Touches go to this layer; what is drawn above it (the Try again button) keeps its own.
        Box(
            Modifier
                .fillMaxSize()
                .remoteDisplayGestures(
                    touchMode = state.touchMode,
                    frameWidth = state.frameWidth,
                    frameHeight = state.frameHeight,
                    sourceKey = state.sourceKey,
                    slops = slops,
                    twoFingerRightClick = state.twoFingerRightClick,
                    callbacks = callbacks,
                    zoom = { zoom.value },
                    onZoom = { zoom.value = it },
                ),
        ) {
            // Children without an explicit `align` are centred, so the self-measuring video view
            // letterboxes itself; the zoom scales the whole box around its top-left corner.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val current = zoom.value
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = current.scale
                        scaleY = current.scale
                        translationX = current.offsetX
                        translationY = current.offsetY
                    },
                contentAlignment = Alignment.Center,
            ) { videoSurface() }
        }
        if (state.frameWidth == null) {
            RemoteDisplayPlaceholder(
                captureSupported = state.captureSupported,
                streaming = state.screenStreaming,
                onRetry = actions.onStartStream,
            )
        }
        val height = state.frameHeight
        if (state.showStats && state.screenStreaming && height != null) {
            StatsLine(
                height = height,
                stats = state.stats,
                latencyMs = state.latencyMs,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(spacing.md),
            )
        }
    }
}

/** "1080p · 60 fps · 8 ms": the PC's screen height, the frames that arrive and the delay. */
@Composable
private fun StatsLine(height: Int, stats: ScreenStreamStats, latencyMs: Int?, modifier: Modifier = Modifier) {
    val fps = stats.deliveredFps.takeIf { it.isFinite() }?.roundToInt() ?: 0
    val text = if (latencyMs != null) {
        stringResource(R.string.display_stats, height, fps, latencyMs)
    } else {
        stringResource(R.string.display_stats_no_delay, height, fps)
    }
    Text(
        text,
        style = GestoTheme.accentTypography.telemetry,
        color = GestoTheme.colors.onVideo,
        maxLines = 1,
        modifier = modifier
            .background(GestoTheme.colors.video.copy(alpha = STATS_BACKGROUND_ALPHA))
            .padding(horizontal = GestoTheme.spacing.sm, vertical = GestoTheme.spacing.hairline),
    )
}

/** Right click, Keyboard and Audio, one tap each. */
@Composable
private fun QuickTiles(state: RemoteDisplayUiState, actions: RemoteDisplayActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GestoTheme.spacing.sm)) {
        GestoQuickTile(
            text = stringResource(R.string.quick_right_click),
            icon = GestoIcons.RightClick,
            onClick = {
                actions.onPointerButton(PointerButton.Right, ButtonState.Pressed)
                actions.onPointerButton(PointerButton.Right, ButtonState.Released)
            },
            modifier = Modifier.weight(1f),
        )
        GestoQuickTile(
            text = stringResource(R.string.dock_keyboard),
            icon = GestoIcons.Keyboard,
            onClick = actions.onToggleKeyboard,
            checked = state.keyboardOpen,
            modifier = Modifier.weight(1f),
        )
        GestoQuickTile(
            text = stringResource(if (state.audioMuted) R.string.display_audio_off else R.string.display_audio_on),
            icon = if (state.audioMuted) GestoIcons.Mute else GestoIcons.Volume,
            onClick = actions.onToggleAudioMute,
            checked = state.audioMuted,
            modifier = Modifier.weight(1f),
        )
    }
}

/** A dashed box: turning the phone gives the PC's screen the whole display. */
@Composable
private fun RotateHint() {
    val spacing = GestoTheme.spacing
    val line = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current
    val stroke = with(density) { spacing.borderWidth.toPx() }
    val dash = with(density) { floatArrayOf(spacing.xs.toPx(), spacing.xs.toPx()) }
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = line,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(dash)),
                )
            }
            .padding(horizontal = spacing.xl, vertical = spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Icon(
            GestoIcons.Rotate,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(spacing.rotateIconSize),
        )
        Text(
            stringResource(R.string.display_rotate_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** "Touch: Touchpad. ..." with the mode in colour. */
@Composable
private fun TouchModeLine(mode: TouchMode) {
    val template = stringResource(
        when (mode) {
            TouchMode.Touchpad -> R.string.display_touch_touchpad
            TouchMode.Direct -> R.string.display_touch_direct
        },
    )
    val colors = MaterialTheme.colorScheme
    Text(
        highlightPlaceholder(
            template,
            stringResource(mode.label),
            SpanStyle(color = colors.primary, fontWeight = FontWeight.SemiBold),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant,
    )
}

/** In the middle of the dark video area while no frame has arrived. */
@Composable
private fun RemoteDisplayPlaceholder(captureSupported: Boolean, streaming: Boolean, onRetry: () -> Unit) {
    val spacing = GestoTheme.spacing
    Column(
        Modifier.padding(spacing.hero),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            GestoIcons.Screen,
            contentDescription = null,
            tint = GestoTheme.colors.onVideoMuted,
            modifier = Modifier.size(spacing.emptyStateIconSize),
        )
        Spacer(Modifier.height(spacing.lg))
        Text(
            stringResource(
                when {
                    !captureSupported -> R.string.display_unavailable
                    streaming -> R.string.display_starting
                    else -> R.string.display_stopped
                },
            ),
            style = MaterialTheme.typography.titleSmall,
            color = GestoTheme.colors.onVideo,
        )
        if (captureSupported && !streaming) {
            Spacer(Modifier.height(spacing.lg))
            GestoButton(stringResource(R.string.action_retry), onRetry)
        }
    }
}

private val previewState = RemoteDisplayUiState(
    captureSupported = true,
    screenStreaming = true,
    frameWidth = 1920,
    frameHeight = 1080,
    stats = ScreenStreamStats(deliveredFps = 59.6),
    latencyMs = 8,
)

@Preview(name = "Screen - upright - light", widthDp = 412, heightDp = 700)
@Composable
private fun DisplayScreenPreviewLight() = GestoTheme(mode = ThemeMode.Light) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) {
        DisplayScreen(previewState, RemoteDisplayActions())
    }
}

@Preview(name = "Screen - upright, starting - dark", widthDp = 412, heightDp = 700)
@Composable
private fun DisplayScreenPreviewDark() = GestoTheme(mode = ThemeMode.Dark) {
    Box(Modifier.background(MaterialTheme.colorScheme.background)) {
        DisplayScreen(
            previewState.copy(frameWidth = null, frameHeight = null, touchMode = TouchMode.Direct, audioMuted = true),
            RemoteDisplayActions(),
        )
    }
}

@Preview(name = "Screen - on its side", widthDp = 860, heightDp = 400)
@Composable
private fun DisplayScreenPreviewFullscreen() = GestoTheme(mode = ThemeMode.Light) {
    DisplayScreen(previewState.copy(fullscreen = true, frameWidth = null, screenStreaming = false), RemoteDisplayActions())
}
