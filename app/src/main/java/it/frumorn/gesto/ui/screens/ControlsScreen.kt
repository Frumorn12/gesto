package it.frumorn.gesto.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.ui.components.ActionButton
import it.frumorn.gesto.ui.components.ControlGroup
import it.frumorn.gesto.ui.components.SectionHeader
import it.frumorn.gesto.ui.state.ControlsActions
import it.frumorn.gesto.ui.state.ControlsUiState
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

/** Media, volume, brightness and the capability-gated system actions. */
@Composable
fun ControlsScreen(
    state: ControlsUiState,
    actions: ControlsActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GestoTheme.spacing.xl),
    ) {
        item {
            SectionHeader("Controls", "Media, volume, brightness, and gated system actions.")
        }
        item {
            ControlGroup("Media", state.mediaSupported) {
                ActionButton("Play/Pause") { actions.onMedia("play_pause") }
                ActionButton("Previous") { actions.onMedia("previous") }
                ActionButton("Next") { actions.onMedia("next") }
            }
        }
        item {
            ControlGroup("Volume", state.volumeSupported) {
                ActionButton("Volume -") { actions.onVolume("down") }
                ActionButton("Mute") { actions.onVolume("mute_toggle") }
                ActionButton("Volume +") { actions.onVolume("up") }
            }
        }
        item {
            ControlGroup("Brightness", state.brightnessSupported) {
                ActionButton("Brightness -") { actions.onBrightness("down") }
                ActionButton("Brightness +") { actions.onBrightness("up") }
            }
        }
        item {
            ControlGroup("System", state.systemSupported) {
                ActionButton("Lock") { actions.onSystem("lock") }
                ActionButton("Suspend") { actions.onSystem("suspend") }
            }
        }
    }
}

@Preview(name = "Controls - dark", heightDp = 900)
@Composable
private fun ControlsScreenPreviewDark() = GestoPreviewSurface(darkTheme = true) {
    ControlsScreen(
        state = ControlsUiState(
            mediaSupported = true,
            volumeSupported = true,
            brightnessSupported = false,
            systemSupported = true,
        ),
        actions = ControlsActions(),
    )
}

@Preview(name = "Controls - light", heightDp = 900)
@Composable
private fun ControlsScreenPreviewLight() = GestoPreviewSurface(darkTheme = false) {
    ControlsScreen(state = ControlsUiState(), actions = ControlsActions())
}
