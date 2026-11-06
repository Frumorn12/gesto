package it.frumorn.gesto.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import it.frumorn.gesto.R
import it.frumorn.gesto.core.model.RemoteApp
import it.frumorn.gesto.ui.components.GestoButton
import it.frumorn.gesto.ui.components.GestoIcons
import it.frumorn.gesto.ui.components.GestoQuickTile
import it.frumorn.gesto.ui.components.GestoTileButton
import it.frumorn.gesto.ui.components.HoldToConfirmButton
import it.frumorn.gesto.ui.state.ControlsActions
import it.frumorn.gesto.ui.state.ControlsUiState
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

/** One of the eight shortcuts, by daemon key names. */
internal data class CommandShortcut(
    @StringRes val label: Int,
    val icon: ImageVector,
    val keys: List<String>,
)

internal val CommandShortcuts: List<CommandShortcut> by lazy {
    listOf(
        CommandShortcut(R.string.shortcut_copy, GestoIcons.Copy, listOf("ctrl", "c")),
        CommandShortcut(R.string.shortcut_paste, GestoIcons.Paste, listOf("ctrl", "v")),
        CommandShortcut(R.string.shortcut_undo, GestoIcons.Undo, listOf("ctrl", "z")),
        CommandShortcut(R.string.shortcut_save, GestoIcons.Save, listOf("ctrl", "s")),
        CommandShortcut(R.string.shortcut_close, GestoIcons.Close, listOf("super", "q")),
        CommandShortcut(R.string.shortcut_terminal, GestoIcons.Terminal, listOf("super", "enter")),
        CommandShortcut(R.string.shortcut_launcher, GestoIcons.Launcher, listOf("super", "space")),
        CommandShortcut(R.string.shortcut_esc, GestoIcons.Escape, listOf("esc")),
    )
}

/** Media, volume, brightness, shortcuts, the PC's apps and the system actions of the Commands mode. */
@Composable
fun CommandsScreen(
    state: ControlsUiState,
    actions: ControlsActions,
    modifier: Modifier = Modifier,
) {
    val spacing = GestoTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(spacing.xxl),
    ) {
        Section(R.string.commands_media, state.mediaSupported) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                GestoTileButton(
                    text = "",
                    icon = GestoIcons.Previous,
                    description = stringResource(R.string.commands_previous),
                    onClick = { actions.onMedia("previous") },
                    enabled = state.mediaSupported,
                    modifier = Modifier.weight(1f).height58(),
                )
                GestoButton(
                    text = stringResource(R.string.commands_play_pause),
                    icon = GestoIcons.Play,
                    onClick = { actions.onMedia("play_pause") },
                    enabled = state.mediaSupported,
                    modifier = Modifier.weight(2f).height58(),
                )
                GestoTileButton(
                    text = "",
                    icon = GestoIcons.Next,
                    description = stringResource(R.string.commands_next),
                    onClick = { actions.onMedia("next") },
                    enabled = state.mediaSupported,
                    modifier = Modifier.weight(1f).height58(),
                )
            }
        }
        Section(R.string.commands_volume, state.volumeSupported) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                GestoTileButton(
                    stringResource(R.string.commands_quieter), { actions.onVolume("down") },
                    Modifier.weight(1f).height58(), state.volumeSupported, GestoIcons.Minus,
                )
                GestoTileButton(
                    stringResource(R.string.commands_mute), { actions.onVolume("mute_toggle") },
                    Modifier.weight(1f).height58(), state.volumeSupported, GestoIcons.Mute,
                )
                GestoTileButton(
                    stringResource(R.string.commands_louder), { actions.onVolume("up") },
                    Modifier.weight(1f).height58(), state.volumeSupported, GestoIcons.Plus,
                )
            }
        }
        Section(R.string.commands_brightness, state.brightnessSupported) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                GestoTileButton(
                    stringResource(R.string.commands_dimmer), { actions.onBrightness("down") },
                    Modifier.weight(1f).height58(), state.brightnessSupported, GestoIcons.Minus,
                )
                GestoTileButton(
                    stringResource(R.string.commands_brighter), { actions.onBrightness("up") },
                    Modifier.weight(1f).height58(), state.brightnessSupported, GestoIcons.Plus,
                )
            }
        }
        Section(R.string.commands_shortcuts, state.shortcutsSupported) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                CommandShortcuts.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                        row.forEach { shortcut ->
                            GestoQuickTile(
                                text = stringResource(shortcut.label),
                                icon = shortcut.icon,
                                onClick = { actions.onShortcut(shortcut.keys) },
                                modifier = Modifier.weight(1f),
                                enabled = state.shortcutsSupported,
                                height = spacing.commandKeyHeight,
                            )
                        }
                    }
                }
            }
        }
        if (state.apps.isNotEmpty()) {
            Section(R.string.commands_apps, true) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    state.apps.forEach { app ->
                        GestoTileButton(
                            text = app.name,
                            icon = GestoIcons.Apps,
                            onClick = { actions.onLaunchApp(app) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        Section(R.string.commands_system, state.lockSupported || state.suspendSupported) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                GestoTileButton(
                    text = stringResource(R.string.commands_lock),
                    icon = GestoIcons.Lock,
                    onClick = { actions.onSystem("lock") },
                    enabled = state.lockSupported,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!state.lockSupported) Reason(stringResource(R.string.commands_unavailable))
                HoldToConfirmButton(
                    text = stringResource(R.string.commands_hold_suspend),
                    icon = GestoIcons.Moon,
                    onConfirmed = { actions.onSystem("suspend") },
                    enabled = state.suspendSupported,
                )
                if (!state.suspendSupported) Reason(stringResource(R.string.commands_suspend_off))
            }
        }
    }
}

@Composable
private fun Modifier.height58(): Modifier = this.then(Modifier.height(GestoTheme.spacing.clickButtonHeight))

/** A small uppercase label, the content below it, and the reason line when the whole group is off. */
@Composable
private fun Section(@StringRes title: Int, supported: Boolean, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GestoTheme.spacing.md)) {
        Text(
            stringResource(title).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
        if (!supported && title != R.string.commands_system) Reason(stringResource(R.string.commands_unavailable))
    }
}

@Composable
private fun Reason(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        overflow = TextOverflow.Clip,
    )
}

@Preview(name = "Commands - dark", heightDp = 1100)
@Composable
private fun CommandsScreenPreviewDark() = GestoPreviewSurface(darkTheme = true) {
    CommandsScreen(
        state = ControlsUiState(
            mediaSupported = true, volumeSupported = true, brightnessSupported = false,
            shortcutsSupported = true, lockSupported = true, suspendSupported = false,
            apps = listOf(RemoteApp("steam", "Steam Big Picture")),
        ),
        actions = ControlsActions(),
    )
}

@Preview(name = "Commands - light", heightDp = 1100)
@Composable
private fun CommandsScreenPreviewLight() = GestoPreviewSurface(darkTheme = false) {
    CommandsScreen(state = ControlsUiState(), actions = ControlsActions())
}
