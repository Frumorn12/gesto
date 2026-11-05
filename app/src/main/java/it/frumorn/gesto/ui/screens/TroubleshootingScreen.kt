package it.frumorn.gesto.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.core.model.formatFps
import it.frumorn.gesto.ui.components.DiagnosticRow
import it.frumorn.gesto.ui.components.SectionHeader
import it.frumorn.gesto.ui.components.GestoCard
import it.frumorn.gesto.ui.components.capabilityLabel
import it.frumorn.gesto.ui.state.DiagnosticsActions
import it.frumorn.gesto.ui.state.DiagnosticsUiState
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

/** Capability-driven diagnostics for the host, the stream and the local input devices. */
@Composable
fun TroubleshootingScreen(
    state: DiagnosticsUiState,
    actions: DiagnosticsActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GestoTheme.spacing.xl),
    ) {
        item {
            SectionHeader(
                "Diagnostics",
                "Wayland support is capability driven. Unsupported actions fail with a host reason.",
            )
            Button(onClick = actions.onRefreshCapabilities, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(GestoTheme.spacing.md))
                Text("Refresh capabilities")
            }
        }
        item {
            val capabilities = state.capabilities
            GestoCard {
                DiagnosticRow("Connection", state.connectionLabel)
                DiagnosticRow("Input backend", capabilities.inputBackend)
                DiagnosticRow("Input status", capabilities.inputReason)
                DiagnosticRow("External pointer", capabilityLabel(capabilities.externalPointerSupported))
                DiagnosticRow("External keyboard", capabilityLabel(capabilities.externalKeyboardSupported))
                DiagnosticRow("Controller forwarding", capabilityLabel(capabilities.externalControllerSupported))
                DiagnosticRow("External input status", state.externalInputStatus)
                DiagnosticRow("Route backend", capabilities.routeBackend)
                DiagnosticRow("LAN direct", capabilityLabel(capabilities.lanDirectSupported))
                DiagnosticRow("Public direct", capabilityLabel(capabilities.publicDirectSupported))
                DiagnosticRow("Relay", capabilityLabel(capabilities.relaySupported))
                DiagnosticRow("Connectivity", capabilities.connectivityReason)
                DiagnosticRow("Capture backend", capabilities.captureBackend)
                DiagnosticRow("Capture status", capabilities.captureReason)
                DiagnosticRow("Volume", capabilityLabel(capabilities.volume))
                DiagnosticRow("Brightness", capabilityLabel(capabilities.brightness))
                DiagnosticRow("Clipboard", capabilityLabel(capabilities.clipboard))
                DiagnosticRow("Lock", capabilityLabel(capabilities.lock))
            }
        }
        item {
            GestoCard {
                Text("Stream diagnostics", style = MaterialTheme.typography.titleMedium)
                val stats = state.stats
                DiagnosticRow("Streaming", if (state.screenStreaming) "yes" else "no")
                DiagnosticRow("Backend", stats.backend)
                DiagnosticRow("Target FPS", "${stats.targetFps}")
                DiagnosticRow("Actual FPS (host)", "${stats.actualFps}")
                DiagnosticRow("Delivered FPS", stats.deliveredFps.formatFps())
                DiagnosticRow("Avg frame size", "${stats.averageKib} KiB")
                DiagnosticRow("Total frames", "${stats.receivedFrames}")
                DiagnosticRow("Stream status", state.screenStatus)
                DiagnosticRow("Stream error", state.screenError ?: "none")
            }
        }
        item {
            GestoCard {
                Text("Android input devices", style = MaterialTheme.typography.titleMedium)
                if (state.externalInputDevices.isEmpty()) {
                    Text(
                        "No external keyboard, mouse, touchpad, or controller detected by Android.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.externalInputDevices.forEach { device ->
                        DiagnosticRow(device.name, device.displayClasses)
                    }
                }
            }
        }
        item {
            GestoCard {
                Text("Host-side checks", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Run `waypad-daemon doctor` and inspect " +
                        "`journalctl --user -u waypad-daemon -f` on the Linux host.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(name = "Diagnostics - dark", heightDp = 1400)
@Composable
private fun TroubleshootingScreenPreviewDark() = GestoPreviewSurface(darkTheme = true) {
    TroubleshootingScreen(DiagnosticsUiState(connectionLabel = "Connected"), DiagnosticsActions())
}

@Preview(name = "Diagnostics - light", heightDp = 1400)
@Composable
private fun TroubleshootingScreenPreviewLight() = GestoPreviewSurface(darkTheme = false) {
    TroubleshootingScreen(DiagnosticsUiState(), DiagnosticsActions())
}
