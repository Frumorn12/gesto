package it.frumorn.gesto.ui.state

import androidx.compose.runtime.Immutable
import it.frumorn.gesto.SessionMode
import it.frumorn.gesto.core.input.RemoteGestureMode
import it.frumorn.gesto.core.model.ButtonState
import it.frumorn.gesto.core.model.DiscoveredHost
import it.frumorn.gesto.core.model.PointerButton
import it.frumorn.gesto.core.model.StreamProfile
import it.frumorn.gesto.core.model.TrustedHost

/**
 * Callback bundles handed down to the screens.
 *
 * Every member defaults to a no-op so a `@Preview` can build a screen with a single constructor
 * call. The real implementations are wired once, in
 * [it.frumorn.gesto.ui.rememberGestoActions].
 */

@Immutable
data class SessionActions(
    /** Back arrow: the PCs list, staying connected. */
    val onBackToPcs: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onSelectMode: (SessionMode) -> Unit = {},
    val onSetKeyboardOpen: (Boolean) -> Unit = {},
    val onSetQuickMenuOpen: (Boolean) -> Unit = {},
    val onDisconnect: () -> Unit = {},
)

@Immutable
data class LaunchActions(
    val onChooseAnotherPc: () -> Unit = {},
)

@Immutable
data class OnboardingActions(
    val onDiscoverHosts: () -> Unit = {},
    val onOpenTrustedHosts: () -> Unit = {},
)

@Immutable
data class DiscoveryActions(
    val onScanLan: () -> Unit = {},
    val onSelectHost: (DiscoveredHost) -> Unit = {},
    val onApplyInvite: (String) -> Unit = {},
    val onManualAddressChange: (String) -> Unit = {},
    val onManualPortChange: (String) -> Unit = {},
    val onUseManualHost: () -> Unit = {},
)

@Immutable
data class PairingActions(
    val onPairingCodeChange: (String) -> Unit = {},
    val onPair: () -> Unit = {},
)

@Immutable
data class RemotePadActions(
    val onPrepareInput: () -> Unit = {},
    val onPointerMove: (dx: Float, dy: Float) -> Unit = { _, _ -> },
    val onScroll: (dx: Float, dy: Float, finish: Boolean) -> Unit = { _, _, _ -> },
    val onPointerButton: (PointerButton, ButtonState) -> Unit = { _, _ -> },
    val onReleasePointerButtons: () -> Unit = {},
    val onBeginInteraction: () -> Long = { 0L },
    val onEndInteraction: (Long) -> Unit = {},
    val onGestureStateChanged: (RemoteGestureMode, Int) -> Unit = { _, _ -> },
    val onPointerCancelled: (String) -> Unit = {},
    val onPointerFailed: (Throwable) -> Unit = {},
)

@Immutable
data class RemoteDisplayActions(
    val onSelectSource: (String) -> Unit = {},
    val onStartStream: () -> Unit = {},
    val onStopStream: () -> Unit = {},
    val onRefreshSources: () -> Unit = {},
    val onSetFullscreen: (Boolean) -> Unit = {},
    val onSetGameMode: (Boolean) -> Unit = {},
    val onSetControlsVisible: (visible: Boolean, reason: String) -> Unit = { _, _ -> },
    val onRevealControls: (reason: String) -> Unit = {},
    val onKeyboardEdit: (previous: String, next: String) -> Unit = { _, _ -> },
    val onPointerButton: (PointerButton, ButtonState) -> Unit = { _, _ -> },
    val onDesktopPointerMove: (x: Float, y: Float) -> Unit = { _, _ -> },
    val onDesktopClick: (x: Float, y: Float) -> Unit = { _, _ -> },
    val onScroll: (dx: Float, dy: Float, finish: Boolean) -> Unit = { _, _, _ -> },
    val onToggleAudioMute: () -> Unit = {},
)

@Immutable
data class KeyboardActions(
    val onKeyboardEdit: (previous: String, next: String) -> Unit = { _, _ -> },
    val onShortcut: (List<String>) -> Unit = {},
)

@Immutable
data class ControlsActions(
    val onMedia: (String) -> Unit = {},
    val onVolume: (String) -> Unit = {},
    val onBrightness: (String) -> Unit = {},
    val onSystem: (String) -> Unit = {},
)

@Immutable
data class SettingsActions(
    val onSelectProfile: (StreamProfile) -> Unit = {},
    val onSetMaxFps: (Int) -> Unit = {},
    val onSetMaxDimension: (Int) -> Unit = {},
    val onSetJpegQuality: (Int) -> Unit = {},
    val onToggleStats: () -> Unit = {},
    val onToggleHaptics: () -> Unit = {},
    val onSetGameMode: (Boolean) -> Unit = {},
    val onOpenTrustedHosts: () -> Unit = {},
    val onOpenDiagnostics: () -> Unit = {},
    val onDisconnect: () -> Unit = {},
)

@Immutable
data class TrustedHostsActions(
    val onDiscoverNewHost: () -> Unit = {},
    val onConnect: (TrustedHost) -> Unit = {},
    val onRemove: (id: String) -> Unit = {},
)

@Immutable
data class DiagnosticsActions(
    val onRefreshCapabilities: () -> Unit = {},
)
