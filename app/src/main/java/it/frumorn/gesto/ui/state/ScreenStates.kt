package it.frumorn.gesto.ui.state

import androidx.compose.runtime.Immutable
import it.frumorn.gesto.AddPcStep
import it.frumorn.gesto.SessionMode
import it.frumorn.gesto.core.externalinput.ExternalInputDeviceSummary
import it.frumorn.gesto.core.model.CapabilitySummary
import it.frumorn.gesto.core.model.ScreenSource
import it.frumorn.gesto.core.model.ScreenStreamStats
import it.frumorn.gesto.core.model.StreamSettings
import it.frumorn.gesto.core.session.Presence
import it.frumorn.gesto.ui.text.UiText

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
    val quickMenuOpen: Boolean = false,
    /** The remote screen fills the display: no bars at all. */
    val fullscreen: Boolean = false,
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

@Immutable
data class RemoteDisplayUiState(
    /** Source resolution of the stream; `null` until the decoder reports a format. */
    val frameWidth: Int? = null,
    val frameHeight: Int? = null,
    val captureSupported: Boolean = false,
    val inputSupported: Boolean = false,
    val screenSources: List<ScreenSource> = emptyList(),
    val selectedScreenSourceId: String? = null,
    val streamingSource: ScreenSource? = null,
    val screenStreaming: Boolean = false,
    val screenStatus: String = "Screen stream idle",
    val screenError: String? = null,
    val stats: ScreenStreamStats = ScreenStreamStats(),
    val fullscreen: Boolean = false,
    val gameMode: Boolean = false,
    val controlsVisible: Boolean = true,
    val haptics: Boolean = true,
    val audioMuted: Boolean = false,
) {
    /** Source actually feeding the viewport, falling back to the user selection. */
    val activeSource: ScreenSource?
        get() = streamingSource ?: screenSources.firstOrNull { it.id == selectedScreenSourceId }
}

@Immutable
data class ControlsUiState(
    val mediaSupported: Boolean = false,
    val volumeSupported: Boolean = false,
    val brightnessSupported: Boolean = false,
    val systemSupported: Boolean = false,
)

@Immutable
data class SettingsUiState(
    val streamSettings: StreamSettings = StreamSettings(),
    val haptics: Boolean = true,
    val gameMode: Boolean = false,
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
)
