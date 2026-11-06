package it.frumorn.gesto.ui.state

import androidx.compose.runtime.Immutable
import it.frumorn.gesto.SessionMode
import it.frumorn.gesto.core.input.PointerDelta
import it.frumorn.gesto.core.model.ButtonState
import it.frumorn.gesto.core.model.PointerButton
import it.frumorn.gesto.core.model.RemoteApp
import it.frumorn.gesto.core.model.QualityPreset
import it.frumorn.gesto.core.model.TouchMode
import it.frumorn.gesto.ui.navigation.QuickMenuEntry
import it.frumorn.gesto.ui.theme.ThemeMode

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
data class AddPcActions(
    /** A found PC, by [FoundPcState.key]. */
    val onSelectPc: (String) -> Unit = {},
    val onOpenQr: () -> Unit = {},
    val onOpenAddress: () -> Unit = {},
    /** Cancel on the PIN step: back to the search. */
    val onCancelApproval: () -> Unit = {},
    val onCodeChange: (String) -> Unit = {},
    val onPairWithCode: () -> Unit = {},
    val onAddressChange: (String) -> Unit = {},
    val onPortChange: (String) -> Unit = {},
    val onUseAddress: () -> Unit = {},
    /** The camera read an invite. */
    val onInviteScanned: (String) -> Unit = {},
    /** The camera's close button: back to the search. */
    val onCloseQr: () -> Unit = {},
)

@Immutable
data class TouchpadActions(
    /** Asks the PC to show its approval request. */
    val onPrepareInput: () -> Unit = {},
    /** Asks the PC again what it can do (after it refused input). */
    val onCheckInput: () -> Unit = {},
    /** A finger move; returns the move sent to the PC (after speed and acceleration), or null. */
    val onPointerMove: (dx: Float, dy: Float) -> PointerDelta? = { _, _ -> null },
    val onScroll: (dx: Float, dy: Float, finish: Boolean) -> Unit = { _, _, _ -> },
    /** A button change; false when it was dropped. */
    val onPointerButton: (PointerButton, ButtonState) -> Boolean = { _, _ -> false },
    val onReleasePointerButtons: () -> Unit = {},
    val onBeginInteraction: () -> Long = { 0L },
    val onEndInteraction: (Long) -> Unit = {},
    /** Three-finger tap: opens or closes the keyboard sheet. */
    val onToggleKeyboard: () -> Unit = {},
    val onPointerCancelled: (String) -> Unit = {},
    val onPointerFailed: (Throwable) -> Unit = {},
)

@Immutable
data class RemoteDisplayActions(
    /** Starts the screen again after it stopped. */
    val onStartStream: () -> Unit = {},
    /** A finger move in the Touchpad touch mode; returns the move sent to the PC, or null. */
    val onPointerMove: (dx: Float, dy: Float) -> PointerDelta? = { _, _ -> null },
    val onPointerButton: (PointerButton, ButtonState) -> Unit = { _, _ -> },
    val onReleasePointerButtons: () -> Unit = {},
    /** The pointer to this point of the PC's screen (Direct touch mode). */
    val onDesktopPointerMove: (x: Float, y: Float) -> Unit = { _, _ -> },
    /** A left click at this point of the PC's screen (Direct touch mode). */
    val onDesktopClick: (x: Float, y: Float) -> Unit = { _, _ -> },
    val onScroll: (dx: Float, dy: Float, finish: Boolean) -> Unit = { _, _, _ -> },
    val onToggleAudioMute: () -> Unit = {},
    /** The Keyboard tile and the three-finger tap: opens or closes the keyboard sheet. */
    val onToggleKeyboard: () -> Unit = {},
)

/** The quick menu and its purple button. */
@Immutable
data class QuickMenuActions(
    val onSetOpen: (Boolean) -> Unit = {},
    val onEntry: (QuickMenuEntry) -> Unit = {},
    /** The button was dragged here (fractions of the room it can move in). */
    val onMoveButton: (x: Float, y: Float) -> Unit = { _, _ -> },
)

@Immutable
data class KeyboardActions(
    val onKeyboardEdit: (previous: String, next: String) -> Unit = { _, _ -> },
    /** One key with its modifiers, by daemon key names (`["ctrl", "c"]`, `["left"]`). */
    val onKey: (List<String>) -> Unit = {},
)

@Immutable
data class ControlsActions(
    val onMedia: (String) -> Unit = {},
    val onVolume: (String) -> Unit = {},
    val onBrightness: (String) -> Unit = {},
    val onSystem: (String) -> Unit = {},
    /** One shortcut by daemon key names (`["ctrl", "c"]`). */
    val onShortcut: (keys: List<String>, notice: Int) -> Unit = { _, _ -> },
    val onLaunchApp: (RemoteApp) -> Unit = {},
)

@Immutable
data class SettingsActions(
    val onSelectQuality: (QualityPreset) -> Unit = {},
    val onSetTouchMode: (TouchMode) -> Unit = {},
    val onToggleStats: () -> Unit = {},
    /** Called once the finger leaves the slider. */
    val onSetPointerSpeed: (Float) -> Unit = {},
    val onSetPointerAcceleration: (Boolean) -> Unit = {},
    val onSetTwoFingerRightClick: (Boolean) -> Unit = {},
    val onToggleHaptics: () -> Unit = {},
    val onSetThemeMode: (ThemeMode) -> Unit = {},
    val onSetRoundedCorners: (Boolean) -> Unit = {},
    val onOpenPcs: () -> Unit = {},
    val onOpenDiagnostics: () -> Unit = {},
    val onOpenAdvanced: () -> Unit = {},
)

/** Advanced: each value set here makes the quality Custom. */
@Immutable
data class AdvancedActions(
    val onSetFps: (Int) -> Unit = {},
    val onSetMaxDimension: (Int) -> Unit = {},
    /** Called once the finger leaves the slider. */
    val onSetQuality: (Int) -> Unit = {},
    /** A screen of the PC by id; null leaves the choice to the PC. */
    val onSelectSource: (String?) -> Unit = {},
    val onReloadSources: () -> Unit = {},
)

/** The PCs list. Every PC is addressed by its id. */
@Immutable
data class HomeActions(
    val onConnect: (String) -> Unit = {},
    /** "Back to the PC": the session that is open. */
    val onResume: () -> Unit = {},
    val onDisconnect: () -> Unit = {},
    val onWake: (String) -> Unit = {},
    /** "Add again" on a PC that no longer knows this phone. */
    val onRepair: (String) -> Unit = {},
    val onRename: (id: String, name: String) -> Unit = { _, _ -> },
    val onForget: (String) -> Unit = {},
    val onAddPc: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
)

@Immutable
data class DiagnosticsActions(
    val onRefreshCapabilities: () -> Unit = {},
)
