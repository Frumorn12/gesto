package it.frumorn.gesto.ui.state

import androidx.compose.runtime.Immutable
import it.frumorn.gesto.AddPcStep
import it.frumorn.gesto.SessionMode
import it.frumorn.gesto.core.externalinput.ExternalInputDeviceSummary
import it.frumorn.gesto.core.model.CapabilitySummary
import it.frumorn.gesto.core.model.RemoteApp
import it.frumorn.gesto.core.model.ScreenStreamStats
import it.frumorn.gesto.core.model.QualityPreset
import it.frumorn.gesto.core.model.ScreenSource
import it.frumorn.gesto.core.model.StreamSettings
import it.frumorn.gesto.core.model.TouchMode
import it.frumorn.gesto.core.session.Presence
import it.frumorn.gesto.ui.navigation.QuickMenuEntry
import it.frumorn.gesto.ui.text.UiText
import it.frumorn.gesto.ui.theme.ThemeMode

/**
 * Immutable per-screen view state.
 *
 * These types intentionally know nothing about `GestoViewModel` or `GestoUiState`: the root
 * [it.frumorn.gesto.ui.GestoApp] composable is the only place that maps one onto the other. That
 * is what makes every screen previewable and unit-testable in isolation.
 */

/** Top and bottom bars of a connected session. */
@Immutable
data class SessionUiState(
    val hostName: String = "",
    val reconnecting: Boolean = false,
    /** Round trip of the last health ping; hidden while unknown. */
    val latencyMs: Int? = null,
    val mode: SessionMode = SessionMode.Touchpad,
    val keyboardOpen: Boolean = false,
    /** The remote screen fills the display: no bars at all. */
    val fullscreen: Boolean = false,
)

/**
 * The quick menu, the same one in every session mode, and the purple button that opens it on the
 * Screen (elsewhere it opens with the back key).
 */
@Immutable
data class QuickMenuUiState(
    val open: Boolean = false,
    val entries: List<QuickMenuEntry> = emptyList(),
    val audioOn: Boolean = true,
    val touchMode: TouchMode = TouchMode.Touchpad,
    /** The quality chosen; the menu line flips through Auto, Smooth, Sharp and Saver. */
    val quality: QualityPreset = QualityPreset.Auto,
    val gameMode: Boolean = false,
    val showButton: Boolean = false,
    /** Where the button sits, as fractions (0..1) of the room it can move in. */
    val buttonX: Float = 1f,
    val buttonY: Float = 1f,
    /** The button pulses to be noticed, in the first sessions until it is opened. */
    val pulse: Boolean = false,
)

@Immutable
data class LaunchUiState(
    val hostName: String = "",
    val mode: SessionMode = SessionMode.Touchpad,
    /** The PC's address is on the home network. */
    val sameNetwork: Boolean = false,
)

/** A PC the search found. */
@Immutable
data class FoundPcState(
    /** Identifies the PC in [AddPcActions.onSelectPc]. */
    val key: String,
    /** The name the user gave it if it is already paired, else the PC's own. */
    val name: String,
    /** This phone is already paired with it (picking it pairs again). */
    val alreadyAdded: Boolean,
)

/** "Add a PC": which step is shown and what each step needs. */
@Immutable
data class AddPcUiState(
    val step: AddPcStep = AddPcStep.Search,
    val found: List<FoundPcState> = emptyList(),
    /** The four digits the PC shows in its notification. */
    val pin: String = "",
    val code: String = "",
    /** The PC's answer is awaited (PIN or code). */
    val busy: Boolean = false,
    val address: String = "",
    val port: String = "47771",
)

/** Whether the PC takes the touchpad's input, as far as the app knows. */
enum class TouchpadInput {
    Ready,
    /** The PC asks once, on its screen, before the phone may control it. */
    NeedsApproval,
    /** The PC said it can't take input from the phone. */
    Blocked,
}

/**
 * Whether the touchpad's input reaches the PC, from what the PC said ([capabilities], known once
 * [connected]) and whether it has told this app that its input is approved ([inputApproved]: the
 * answer to "Ask the PC", which the daemon keeps for as long as it runs). The input backend's name
 * plays no part: a portal PC keeps calling itself `wayland-portal` after the approval.
 */
fun touchpadInput(connected: Boolean, capabilities: CapabilitySummary, inputApproved: Boolean): TouchpadInput = when {
    // Until the PC has answered, what it can do is not known yet.
    connected && !capabilities.inputSupported -> TouchpadInput.Blocked
    capabilities.inputRequiresApproval && !inputApproved -> TouchpadInput.NeedsApproval
    else -> TouchpadInput.Ready
}

/**
 * Whether a click is sent to the PC. Only a PC that refuses input gets none: one still waiting
 * for approval may have been approved already (by this phone before a reconnect, or by another
 * one), and if not the daemon says so.
 */
val TouchpadInput.sendsClicks: Boolean
    get() = this != TouchpadInput.Blocked

@Immutable
data class TouchpadUiState(
    val input: TouchpadInput = TouchpadInput.Ready,
    /** A light vibration on every click. */
    val haptics: Boolean = true,
    /** A two-finger tap is a right click. */
    val twoFingerRightClick: Boolean = true,
    /** The gesture hints at the bottom of the pad (the first sessions only). */
    val hints: Boolean = false,
    val controllerConnected: Boolean = false,
)

/** The remote screen: the video, how touches drive the PC, and what is shown around it. */
@Immutable
data class RemoteDisplayUiState(
    /** Source resolution of the stream; `null` until the decoder reports a format. */
    val frameWidth: Int? = null,
    val frameHeight: Int? = null,
    /** The capture source the frames come from; touches start over when it changes. */
    val sourceKey: String? = null,
    val captureSupported: Boolean = false,
    val screenStreaming: Boolean = false,
    val stats: ScreenStreamStats = ScreenStreamStats(),
    /** The small line with resolution, fps and delay over the video. */
    val showStats: Boolean = true,
    val latencyMs: Int? = null,
    /** Only the video and the purple button: the phone is on its side, or Game mode is on. */
    val fullscreen: Boolean = false,
    val touchMode: TouchMode = TouchMode.Touchpad,
    val haptics: Boolean = true,
    val audioMuted: Boolean = false,
    val keyboardOpen: Boolean = false,
    val twoFingerRightClick: Boolean = true,
)

@Immutable
data class ControlsUiState(
    val mediaSupported: Boolean = false,
    val volumeSupported: Boolean = false,
    val brightnessSupported: Boolean = false,
    /** Shortcuts go through the PC's keyboard, so they need input. */
    val shortcutsSupported: Boolean = false,
    /** The PC's name, for the "Suspend <PC>?" question. */
    val hostName: String = "",
    val lockSupported: Boolean = false,
    val suspendSupported: Boolean = false,
    /** Apps the PC offers; the section is hidden when empty. */
    val apps: List<RemoteApp> = emptyList(),
)

/** Settings, in plain words: the quality, touch, the touchpad's feel and the app's look. */
@Immutable
data class SettingsUiState(
    val quality: QualityPreset = QualityPreset.Auto,
    /** What streams now (Auto's current rung included), for the line under the quality. */
    val streaming: StreamSettings = StreamSettings(),
    val touchMode: TouchMode = TouchMode.Touchpad,
    val showStats: Boolean = true,
    /** 0.5 to 3.0. */
    val pointerSpeed: Float = 1f,
    val pointerAcceleration: Boolean = true,
    val twoFingerRightClick: Boolean = true,
    val haptics: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    val roundedCorners: Boolean = false,
)

/** Advanced: the stream's own values and the PC's screen to show. */
@Immutable
data class AdvancedUiState(
    val quality: QualityPreset = QualityPreset.Auto,
    /** What streams now (Auto's current rung included); the controls show these values. */
    val streaming: StreamSettings = StreamSettings(),
    val connected: Boolean = false,
    val sources: List<ScreenSource> = emptyList(),
    val sourcesLoading: Boolean = false,
    /** The screen chosen for the next stream; null leaves it to the PC (the one it remembers). */
    val selectedSourceId: String? = null,
)

/** One paired PC on the PCs list. */
@Immutable
data class PcCardState(
    val id: String,
    /** The name the user gave the PC, or the PC's own. */
    val name: String,
    val presence: Presence,
    /** This is the PC of the current session. */
    val connected: Boolean,
    /** Offline, and the PC told the app how to wake it. */
    val canWake: Boolean,
    /** What the PC can do, as short labels. */
    val chips: List<UiText>,
    /** The PC no longer knows this phone: it can only be added again (or forgotten). */
    val needsRepair: Boolean = false,
)

@Immutable
data class HomeUiState(
    val pcs: List<PcCardState> = emptyList(),
)

@Immutable
data class DiagnosticsUiState(
    val connectionLabel: String = "Disconnected",
    val capabilities: CapabilitySummary = CapabilitySummary(),
    val externalInputStatus: String = "No external input devices detected",
    val externalInputDevices: List<ExternalInputDeviceSummary> = emptyList(),
    val screenStreaming: Boolean = false,
    val stats: ScreenStreamStats = ScreenStreamStats(),
    val screenStatus: String = "Screen stream idle",
    val screenError: String? = null,
    val audioMuted: Boolean = false,
    val quality: QualityPreset = QualityPreset.Auto,
    /** The rung Automatic has stepped down to (0 = best). */
    val autoQualityRung: Int = 0,
)
