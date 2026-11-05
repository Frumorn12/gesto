package it.frumorn.gesto.ui.state

import androidx.compose.runtime.Immutable
import it.frumorn.gesto.core.externalinput.ExternalInputDeviceSummary
import it.frumorn.gesto.core.input.RemoteGestureMode
import it.frumorn.gesto.core.model.CapabilitySummary
import it.frumorn.gesto.core.model.DiscoveredHost
import it.frumorn.gesto.core.model.ScreenSource
import it.frumorn.gesto.core.model.ScreenStreamStats
import it.frumorn.gesto.core.model.StreamSettings
import it.frumorn.gesto.core.model.TrustedHost

/**
 * Immutable per-screen view state.
 *
 * These types intentionally know nothing about `GestoViewModel` or `GestoUiState`: the root
 * [it.frumorn.gesto.ui.GestoApp] composable is the only place that maps one onto the other. That
 * is what makes every screen previewable and unit-testable in isolation.
 */

@Immutable
data class ShellUiState(
    val hostName: String? = null,
    val status: String = "Ready",
    val error: String? = null,
)

@Immutable
data class DiscoveryUiState(
    val discoveredHosts: List<DiscoveredHost> = emptyList(),
    val manualAddress: String = "",
    val manualPort: String = "47771",
)

@Immutable
data class PairingUiState(
    val hostName: String? = null,
    val fingerprint: String? = null,
    val pairingCode: String = "",
)

@Immutable
data class RemotePadUiState(
    val inputSupported: Boolean = false,
    val inputBackend: String = "unknown",
    val haptics: Boolean = true,
    val sessionActive: Boolean = false,
    val gestureMode: RemoteGestureMode = RemoteGestureMode.Idle,
    val pointerCount: Int = 0,
    val inputBacklog: Int = 0,
    val connectionLabel: String = "Disconnected",
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

@Immutable
data class TrustedHostsUiState(
    val trustedHosts: List<TrustedHost> = emptyList(),
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
