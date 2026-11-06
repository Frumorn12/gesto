package it.frumorn.gesto.ui.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import it.frumorn.gesto.R
import it.frumorn.gesto.SessionMode
import it.frumorn.gesto.ui.components.GestoIconButton
import it.frumorn.gesto.ui.components.GestoIcons
import it.frumorn.gesto.ui.navigation.SessionDockItems
import it.frumorn.gesto.ui.navigation.quickMenuEntries
import it.frumorn.gesto.ui.state.QuickMenuActions
import it.frumorn.gesto.ui.state.QuickMenuUiState
import it.frumorn.gesto.ui.state.SessionActions
import it.frumorn.gesto.ui.state.SessionUiState
import it.frumorn.gesto.ui.theme.GestoMotion
import it.frumorn.gesto.ui.theme.GestoTheme
import it.frumorn.gesto.ui.theme.ThemeMode

private const val INDICATOR_WIDTH = 0.4f
private const val SHEET_SCALE_FROM = 0.7f

/**
 * The connected session: top bar (back to the PCs, PC name, link state, latency, settings), the
 * 4-item bottom bar, the keyboard sheet and, over the content, the quick menu ([quickMenu], see
 * [QuickMenuLayer]). The remote screen in fullscreen hides both bars.
 *
 * [onDockHeightChanged] reports the bottom bar height so the app's snackbar can sit above it.
 */
@Composable
fun SessionScaffold(
    state: SessionUiState,
    actions: SessionActions,
    modifier: Modifier = Modifier,
    onDockHeightChanged: (Dp) -> Unit = {},
    keyboardSheet: @Composable () -> Unit = {},
    quickMenu: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    Box(modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = { if (!state.fullscreen) SessionTopBar(state, actions) },
            bottomBar = {
                if (!state.fullscreen) {
                    SessionDock(
                        state = state,
                        actions = actions,
                        modifier = Modifier.onSizeChanged { onDockHeightChanged(with(density) { it.height.toDp() }) },
                    )
                }
            },
        ) { padding ->
            val inner = if (state.fullscreen) PaddingValues(GestoTheme.spacing.none) else padding
            Box(
                Modifier
                    .padding(inner)
                    .consumeWindowInsets(inner)
                    .fillMaxSize(),
            ) {
                content()
                quickMenu()
            }
        }
        KeyboardSheetHost(
            open = state.keyboardOpen,
            onClose = { actions.onSetKeyboardOpen(false) },
            content = keyboardSheet,
        )
    }
}

/**
 * The app's single snackbar, above the session bottom bar when there is one ([dockHeight]) and
 * above the system keyboard when it is up.
 */
@Composable
fun GestoSnackbarHost(hostState: SnackbarHostState, dockHeight: Dp, modifier: Modifier = Modifier) {
    val ime = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val navigation = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    SnackbarHost(hostState, modifier.padding(bottom = max(dockHeight, max(ime, navigation))))
}

@Composable
private fun SessionTopBar(state: SessionUiState, actions: SessionActions) {
    val spacing = GestoTheme.spacing
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = spacing.xs, end = spacing.xs, top = spacing.xs, bottom = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        GestoIconButton(onClick = actions.onBackToPcs) {
            BarIcon(GestoIcons.Back, stringResource(R.string.nav_your_pcs))
        }
        Column(Modifier.weight(1f)) {
            Text(
                state.hostName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(if (state.reconnecting) R.string.session_reconnecting else R.string.session_connected),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        val latency = state.latencyMs
        if (latency != null && !state.reconnecting) {
            Text(
                stringResource(R.string.session_latency, latency),
                style = GestoTheme.accentTypography.telemetry,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .border(spacing.borderWidth, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = spacing.sm, vertical = spacing.xs),
            )
        }
        GestoIconButton(onClick = actions.onOpenSettings) {
            BarIcon(GestoIcons.Settings, stringResource(R.string.action_settings))
        }
    }
}

@Composable
private fun SessionDock(state: SessionUiState, actions: SessionActions, modifier: Modifier = Modifier) {
    val spacing = GestoTheme.spacing
    val line = MaterialTheme.colorScheme.outlineVariant
    val stroke = with(LocalDensity.current) { spacing.borderWidth.toPx() }
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind { drawLine(line, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke) }
            .navigationBarsPadding()
            .padding(horizontal = spacing.sm),
    ) {
        SessionDockItems.forEach { item ->
            val mode = item.mode
            val pressed = if (mode == null) state.keyboardOpen else mode == state.mode && !state.keyboardOpen
            // The three modes are tabs; Keyboard opens and closes the sheet, so it is a switch.
            val interaction = if (mode == null) {
                Modifier.toggleable(
                    value = state.keyboardOpen,
                    role = Role.Switch,
                    onValueChange = actions.onSetKeyboardOpen,
                )
            } else {
                Modifier.selectable(selected = pressed, role = Role.Tab, onClick = { actions.onSelectMode(mode) })
            }
            DockButton(
                label = stringResource(item.label),
                icon = item.icon,
                pressed = pressed,
                modifier = Modifier.weight(1f).then(interaction),
            )
        }
    }
}

@Composable
private fun DockButton(
    label: String,
    icon: ImageVector,
    pressed: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (pressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        if (pressed) {
            // The active item marks the bar's top edge, over the 1 dp line.
            Box(
                Modifier
                    .fillMaxWidth(INDICATOR_WIDTH)
                    .height(GestoTheme.spacing.hairline)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
        Column(
            Modifier.padding(top = GestoTheme.spacing.xl, bottom = GestoTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GestoTheme.spacing.xs),
        ) {
            BarIcon(icon, contentDescription = null, tint = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun BarIcon(icon: ImageVector, contentDescription: String?, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Icon(icon, contentDescription, Modifier.size(GestoTheme.spacing.barIconSize), tint = tint)
}

/** Grows from [from] to full height around [origin] on enter (320/350 ms), fades out faster. */
@Composable
private fun AnimatedVisibilityScope.scaleYModifier(from: Float, origin: TransformOrigin, enterMs: Int): Modifier {
    val progress by transition.animateFloat(
        transitionSpec = {
            if (targetState == EnterExitState.Visible) {
                tween(enterMs, easing = GestoMotion.Enter)
            } else {
                tween(GestoMotion.EXIT_MS, easing = GestoMotion.Exit)
            }
        },
        label = "scale-y",
    ) { if (it == EnterExitState.Visible) 1f else 0f }
    return Modifier.graphicsLayer {
        transformOrigin = origin
        scaleY = from + (1f - from) * progress
        alpha = progress
    }
}

@Composable
private fun BoxScope.KeyboardSheetHost(open: Boolean, onClose: () -> Unit, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = open,
        enter = EnterTransition.None,
        exit = ExitTransition.None,
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        Surface(
            modifier = scaleYModifier(SHEET_SCALE_FROM, TransformOrigin(0.5f, 1f), GestoMotion.SHEET_MS)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = GestoTheme.spacing.md,
        ) {
            val edge = MaterialTheme.colorScheme.onSurface
            val stroke = with(LocalDensity.current) { GestoTheme.spacing.borderWidth.toPx() }
            Column(
                Modifier
                    .drawBehind { drawLine(edge, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke) }
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(GestoTheme.spacing.xl),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.keyboard_sheet_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    GestoIconButton(onClick = onClose) {
                        BarIcon(GestoIcons.Close, stringResource(R.string.keyboard_close))
                    }
                }
                Box(Modifier.heightIn(max = GestoTheme.spacing.keyboardSheetMaxHeight)) { content() }
            }
        }
    }
}

@Preview(name = "Session - touchpad - light", widthDp = 412, heightDp = 860)
@Composable
private fun SessionScaffoldPreviewLight() {
    GestoTheme(mode = ThemeMode.Light) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            SessionScaffold(
                state = SessionUiState(hostName = "Legion", latencyMs = 4),
                actions = SessionActions(),
            ) { Box(Modifier.fillMaxSize()) }
        }
    }
}

@Preview(name = "Session - screen, quick menu - dark", widthDp = 412, heightDp = 860)
@Composable
private fun SessionScaffoldPreviewDark() {
    GestoTheme(mode = ThemeMode.Dark) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            SessionScaffold(
                state = SessionUiState(hostName = "Legion", mode = SessionMode.Display, reconnecting = true),
                actions = SessionActions(),
                quickMenu = {
                    QuickMenuLayer(
                        QuickMenuUiState(
                            open = true,
                            entries = quickMenuEntries(SessionMode.Display, captureSupported = true),
                            showButton = true,
                        ),
                        QuickMenuActions(),
                    )
                },
            ) { Box(Modifier.fillMaxSize()) }
        }
    }
}

@Preview(name = "Session - keyboard sheet - light", widthDp = 412, heightDp = 860)
@Composable
private fun SessionScaffoldPreviewKeyboard() {
    GestoTheme(mode = ThemeMode.Light) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            SessionScaffold(
                state = SessionUiState(hostName = "Legion", latencyMs = 12, keyboardOpen = true),
                actions = SessionActions(),
                keyboardSheet = { Box(Modifier.fillMaxWidth().height(200.dp)) },
            ) { Box(Modifier.fillMaxSize()) }
        }
    }
}
