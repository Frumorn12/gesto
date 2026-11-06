package it.frumorn.gesto.ui.session

import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import it.frumorn.gesto.R
import it.frumorn.gesto.core.model.QualityPreset
import it.frumorn.gesto.core.model.TouchMode
import it.frumorn.gesto.ui.components.GestoIcons
import it.frumorn.gesto.ui.navigation.QuickMenuEntry
import it.frumorn.gesto.ui.state.QuickMenuActions
import it.frumorn.gesto.ui.state.QuickMenuUiState
import it.frumorn.gesto.ui.theme.GestoButtonShadow
import it.frumorn.gesto.ui.theme.GestoLightColors
import it.frumorn.gesto.ui.theme.GestoMenuShadow
import it.frumorn.gesto.ui.theme.GestoMotion
import it.frumorn.gesto.ui.theme.GestoTheme
import it.frumorn.gesto.ui.theme.ThemeMode
import kotlin.math.roundToInt

/** The menu grows from 85 % of its height, like the prototype. */
private const val MENU_SCALE_FROM = 0.85f

/** The "+" on the button turns into a "×" while the menu is open. */
private const val OPEN_ROTATION = 45f

/** Strength of the pulse ring when it starts; it fades to nothing as it spreads. */
private const val PULSE_ALPHA = 0.65f

/**
 * The quick menu of a session, over the session content: the purple button (on the Screen), which
 * can be dragged anywhere and remembers where, and the menu it opens. A tap outside the open menu
 * closes it without reaching what is underneath.
 *
 * The menu is white with a dark edge in both themes. It opens next to the button, on the side
 * where there is room (see [placeQuickMenu]); without a button, in the bottom-right corner.
 */
@Composable
fun QuickMenuLayer(state: QuickMenuUiState, actions: QuickMenuActions, modifier: Modifier = Modifier) {
    val spacing = GestoTheme.spacing
    val density = LocalDensity.current
    val inset = with(density) { spacing.xxl.roundToPx() }
    val buttonSize = with(density) { spacing.quickButtonSize.roundToPx() }
    val gap = with(density) { spacing.md.roundToPx() }
    // Away from the cutout and from the edges where a swipe means home or the system bars.
    val room = WindowInsets.displayCutout.union(WindowInsets.mandatorySystemGestures)
    BoxWithConstraints(modifier.fillMaxSize().windowInsetsPadding(room)) {
        val areaWidth = constraints.maxWidth
        val areaHeight = constraints.maxHeight
        val roomX = (areaWidth - 2 * inset - buttonSize).coerceAtLeast(0)
        val roomY = (areaHeight - 2 * inset - buttonSize).coerceAtLeast(0)
        // While a finger drags the button, where it is; the saved position otherwise.
        var dragged by remember { mutableStateOf<Offset?>(null) }
        val left = dragged?.x?.roundToInt() ?: (inset + state.buttonX * roomX).roundToInt()
        val top = dragged?.y?.roundToInt() ?: (inset + state.buttonY * roomY).roundToInt()
        val button = if (state.showButton) QuickButtonBounds(left, top, buttonSize) else null

        if (state.open) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = null, indication = null) { actions.onSetOpen(false) },
            )
        }
        QuickMenuPanelHost(state, actions, button, gap, inset)
        if (button != null) {
            QuickButton(
                open = state.open,
                pulse = state.pulse && !state.open,
                position = IntOffset(left, top),
                onToggle = { actions.onSetOpen(!state.open) },
                onDrag = { delta ->
                    val from = dragged ?: Offset(left.toFloat(), top.toFloat())
                    dragged = Offset(
                        (from.x + delta.x).coerceIn(inset.toFloat(), (inset + roomX).toFloat()),
                        (from.y + delta.y).coerceIn(inset.toFloat(), (inset + roomY).toFloat()),
                    )
                },
                onDragEnd = {
                    dragged?.let { at ->
                        actions.onMoveButton(fraction(at.x - inset, roomX), fraction(at.y - inset, roomY))
                    }
                    dragged = null
                },
            )
        }
    }
}

private fun fraction(value: Float, room: Int): Float = if (room <= 0) 1f else (value / room).coerceIn(0f, 1f)

/** The menu, placed once its size is known and animated from the side of the button. */
@Composable
private fun QuickMenuPanelHost(
    state: QuickMenuUiState,
    actions: QuickMenuActions,
    button: QuickButtonBounds?,
    gap: Int,
    inset: Int,
) {
    val progress by animateFloatAsState(
        targetValue = if (state.open) 1f else 0f,
        animationSpec = if (state.open) {
            tween(GestoMotion.MENU_MS, easing = GestoMotion.Enter)
        } else {
            tween(GestoMotion.EXIT_MS, easing = GestoMotion.Exit)
        },
        label = "quick-menu",
    )
    if (!state.open && progress == 0f) return
    val currentButton by rememberUpdatedState(button)
    Box(
        Modifier.layout { measurable, constraints ->
            val menu = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            val placement = placeQuickMenu(
                areaWidth = constraints.maxWidth,
                areaHeight = constraints.maxHeight,
                button = currentButton,
                menuWidth = menu.width,
                menuHeight = menu.height,
                gap = gap,
                inset = inset,
            )
            layout(constraints.maxWidth, constraints.maxHeight) {
                menu.placeWithLayer(placement.x, placement.y) {
                    transformOrigin = TransformOrigin(placement.originX, placement.originY)
                    scaleY = MENU_SCALE_FROM + (1f - MENU_SCALE_FROM) * progress
                    alpha = progress
                }
            }
        },
    ) {
        QuickMenuPanel(state, enabled = state.open, onEntry = actions.onEntry)
    }
}

/** The 46 dp purple button: tap to open or close the menu, drag to move it. */
@Composable
private fun QuickButton(
    open: Boolean,
    pulse: Boolean,
    position: IntOffset,
    onToggle: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val spacing = GestoTheme.spacing
    val shape = MaterialTheme.shapes.extraSmall
    val primary = MaterialTheme.colorScheme.primary
    val rotation by animateFloatAsState(
        targetValue = if (open) OPEN_ROTATION else 0f,
        animationSpec = tween(GestoMotion.MENU_MS, easing = GestoMotion.Enter),
        label = "quick-button-icon",
    )
    val currentDrag by rememberUpdatedState(onDrag)
    val currentDragEnd by rememberUpdatedState(onDragEnd)
    val description = stringResource(R.string.quick_menu)
    Box(
        Modifier
            .offset { position }
            .size(spacing.quickButtonSize)
            .then(if (pulse) Modifier.pulseRing(shape, primary) else Modifier)
            .dropShadow(
                shape,
                Shadow(
                    radius = spacing.quickButtonShadowBlur,
                    color = GestoButtonShadow,
                    offset = DpOffset(spacing.none, spacing.quickButtonShadowY),
                ),
            )
            .background(primary, shape)
            // A drag that starts on the button near a side edge is the button's, not Back.
            .systemGestureExclusion()
            .pointerInputDrag(onDrag = { currentDrag(it) }, onDragEnd = { currentDragEnd() })
            .clickable(role = Role.Button, onClick = onToggle)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            GestoIcons.Plus,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(spacing.barIconSize)
                .graphicsLayer { rotationZ = rotation },
        )
    }
}

/** Moves the button with the finger; a drag that ends or is cancelled saves where it got to. */
private fun Modifier.pointerInputDrag(onDrag: (Offset) -> Unit, onDragEnd: () -> Unit): Modifier =
    pointerInput(Unit) {
        detectDragGestures(onDragEnd = onDragEnd, onDragCancel = onDragEnd) { change, amount ->
            change.consume()
            onDrag(amount)
        }
    }

/** A square ring that grows out of the button and fades, every [GestoMotion.PULSE_MS]. */
@Composable
private fun Modifier.pulseRing(shape: Shape, color: Color): Modifier {
    val spread = with(LocalDensity.current) { GestoTheme.spacing.quickPulseSpread.toPx() }
    val progress by rememberInfiniteTransition(label = "quick-pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(GestoMotion.PULSE_MS, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "quick-pulse-progress",
    )
    return dropShadow(shape) {
        this.spread = spread * progress
        this.color = color
        this.alpha = PULSE_ALPHA * (1f - progress)
    }
}

/** The white panel with one line per entry. */
@Composable
private fun QuickMenuPanel(state: QuickMenuUiState, enabled: Boolean, onEntry: (QuickMenuEntry) -> Unit) {
    val spacing = GestoTheme.spacing
    val menu = GestoLightColors
    val shape = MaterialTheme.shapes.extraSmall
    Column(
        Modifier
            .width(IntrinsicSize.Max)
            .widthIn(min = spacing.quickMenuWidth)
            .dropShadow(
                shape,
                Shadow(
                    radius = spacing.menuShadowBlur,
                    color = GestoMenuShadow,
                    offset = DpOffset(spacing.none, spacing.menuShadowY),
                ),
            )
            .background(menu.background, shape)
            .border(spacing.borderWidth, menu.onBackground, shape)
            // Taller than the room it has (split screen, a short phone on its side): it scrolls.
            .verticalScroll(rememberScrollState()),
    ) {
        state.entries.forEachIndexed { index, entry ->
            if (index > 0) HorizontalDivider(color = menu.outlineVariant, thickness = spacing.borderWidth)
            QuickMenuRow(entry, state, enabled, onEntry)
        }
    }
}

@Composable
private fun QuickMenuRow(
    entry: QuickMenuEntry,
    state: QuickMenuUiState,
    enabled: Boolean,
    onEntry: (QuickMenuEntry) -> Unit,
) {
    val spacing = GestoTheme.spacing
    val menu = GestoLightColors
    val look = entry.look(state)
    val danger = entry == QuickMenuEntry.BackToTouchpad || entry == QuickMenuEntry.BackToPcs
    val accent = if (danger) menu.error else menu.primary
    val switchValue = when (entry) {
        QuickMenuEntry.Audio -> state.audioOn
        QuickMenuEntry.GameMode -> state.gameMode
        else -> null
    }
    val interaction = if (switchValue != null) {
        Modifier.toggleable(value = switchValue, enabled = enabled, role = Role.Switch) { onEntry(entry) }
    } else {
        Modifier.clickable(enabled = enabled, role = Role.Button) { onEntry(entry) }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .then(interaction)
            .padding(horizontal = spacing.xl, vertical = spacing.xxl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(look.icon, contentDescription = null, tint = accent, modifier = Modifier.size(spacing.menuIconSize))
        Spacer(Modifier.width(spacing.lg))
        // The only weighted child, so the panel's intrinsic width stays the sum of its parts.
        Text(
            stringResource(look.label),
            style = MaterialTheme.typography.labelLarge,
            color = if (danger) menu.error else menu.onBackground,
            modifier = Modifier.weight(1f),
        )
        val value = look.value
        if (value != null) {
            Spacer(Modifier.width(spacing.md))
            Text(
                stringResource(value),
                style = MaterialTheme.typography.labelMedium,
                color = menu.primary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .background(menu.primaryContainer)
                    .padding(horizontal = spacing.sm, vertical = spacing.hairline),
            )
        }
    }
}

/** How a line of the menu looks: its icon, its label and the value pill on the right, if any. */
private data class EntryLook(val icon: ImageVector, @param:StringRes val label: Int, @param:StringRes val value: Int? = null)

private fun QuickMenuEntry.look(state: QuickMenuUiState): EntryLook = when (this) {
    QuickMenuEntry.Keyboard -> EntryLook(GestoIcons.Keyboard, R.string.dock_keyboard)
    QuickMenuEntry.RightClick -> EntryLook(GestoIcons.RightClick, R.string.quick_right_click)
    QuickMenuEntry.Audio -> EntryLook(
        if (state.audioOn) GestoIcons.Volume else GestoIcons.Mute,
        R.string.quick_pc_audio,
        if (state.audioOn) R.string.quick_on else R.string.quick_off,
    )
    QuickMenuEntry.TouchMode -> EntryLook(GestoIcons.Touch, R.string.quick_touch, state.touchMode.label)
    QuickMenuEntry.Quality -> EntryLook(GestoIcons.Quality, R.string.quick_quality, state.quality.qualityLabel)
    QuickMenuEntry.GameMode -> EntryLook(GestoIcons.Game, R.string.quick_game_mode, if (state.gameMode) R.string.quick_on else null)
    QuickMenuEntry.BackToTouchpad -> EntryLook(GestoIcons.Exit, R.string.quick_back_to_touchpad)
    QuickMenuEntry.BackToPcs -> EntryLook(GestoIcons.Exit, R.string.quick_back_to_pcs)
}

/** The name of a touch mode, for the menu and the line under the remote screen. */
@get:StringRes
val TouchMode.label: Int
    get() = when (this) {
        TouchMode.Touchpad -> R.string.touch_mode_touchpad
        TouchMode.Direct -> R.string.touch_mode_direct
    }

/** The plain name of a quality preset. */
@get:StringRes
val QualityPreset.qualityLabel: Int
    get() = when (this) {
        QualityPreset.Auto -> R.string.quality_automatic
        QualityPreset.Smooth -> R.string.quality_smooth
        QualityPreset.Sharp -> R.string.quality_sharp
        QualityPreset.Saver -> R.string.quality_saver
        QualityPreset.Custom -> R.string.quality_custom
    }

private val PreviewEntries = QuickMenuEntry.entries.filter { it != QuickMenuEntry.BackToPcs }

@Preview(name = "Quick menu - open - dark", widthDp = 412, heightDp = 700)
@Composable
private fun QuickMenuPreviewOpen() {
    GestoTheme(mode = ThemeMode.Dark) {
        Box(Modifier.fillMaxSize().background(GestoTheme.colors.video)) {
            QuickMenuLayer(
                QuickMenuUiState(open = true, entries = PreviewEntries, showButton = true, gameMode = true),
                QuickMenuActions(),
            )
        }
    }
}

@Preview(name = "Quick menu - closed, pulsing - light", widthDp = 412, heightDp = 300)
@Composable
private fun QuickMenuPreviewClosed() {
    GestoTheme(mode = ThemeMode.Light) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            QuickMenuLayer(QuickMenuUiState(entries = PreviewEntries, showButton = true, pulse = true), QuickMenuActions())
        }
    }
}

@Preview(name = "Quick menu - landscape", widthDp = 860, heightDp = 400)
@Composable
private fun QuickMenuPreviewLandscape() {
    GestoTheme(mode = ThemeMode.Light) {
        Box(Modifier.fillMaxSize().background(GestoTheme.colors.video)) {
            QuickMenuLayer(
                QuickMenuUiState(open = true, entries = PreviewEntries, showButton = true, touchMode = TouchMode.Direct),
                QuickMenuActions(),
            )
        }
    }
}
