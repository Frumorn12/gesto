package it.frumorn.gesto.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.core.model.DiscoveredHost
import it.frumorn.gesto.ui.GestoQrScannerOverlay
import it.frumorn.gesto.ui.components.DiscoveredHostCard
import it.frumorn.gesto.ui.components.GestoButton
import it.frumorn.gesto.ui.components.GestoOutlinedButton
import it.frumorn.gesto.ui.components.SectionHeader
import it.frumorn.gesto.ui.components.GestoCard
import it.frumorn.gesto.ui.state.DiscoveryActions
import it.frumorn.gesto.ui.state.DiscoveryUiState
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

private val INVITE_PREFIXES = listOf("gesto://invite", "waypad://invite")

/** LAN scan results, QR invite entry and the manual IP fallback. */
@Composable
fun DiscoveryScreen(
    state: DiscoveryUiState,
    actions: DiscoveryActions,
    modifier: Modifier = Modifier,
) {
    var inviteText by remember { mutableStateOf("") }
    var scannerOpen by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize()) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(GestoTheme.spacing.xxl)) {
            item {
                SectionHeader(
                    "Host Discovery",
                    "UDP LAN discovery with manual IP and QR invite fallback.",
                )
                GestoButton(
                    text = "Scan LAN",
                    onClick = actions.onScanLan,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Refresh,
                )
            }
            items(state.discoveredHosts) { host ->
                DiscoveredHostCard(host = host, onClick = { actions.onSelectHost(host) })
            }
            item {
                GestoCard {
                    Text("QR invite", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Scan a terminal QR in-app. Invites can carry LAN and remote-direct endpoints.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(GestoTheme.spacing.lg))
                    GestoOutlinedButton(
                        text = "Scan QR invite",
                        onClick = { scannerOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.QrCodeScanner,
                    )
                    Spacer(Modifier.height(GestoTheme.spacing.lg))
                    OutlinedTextField(
                        value = inviteText,
                        onValueChange = { inviteText = it.trim() },
                        label = { Text("gesto://invite...") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(GestoTheme.spacing.md))
                    GestoOutlinedButton(
                        text = "Use pasted invite",
                        onClick = { actions.onApplyInvite(inviteText) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = INVITE_PREFIXES.any { inviteText.startsWith(it) },
                    )
                }
            }
            item {
                GestoCard {
                    Text("Manual connect", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(GestoTheme.spacing.lg))
                    OutlinedTextField(
                        value = state.manualAddress,
                        onValueChange = actions.onManualAddressChange,
                        label = { Text("Host IP address") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(GestoTheme.spacing.md))
                    OutlinedTextField(
                        value = state.manualPort,
                        onValueChange = actions.onManualPortChange,
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(GestoTheme.spacing.xl))
                    GestoButton("Continue", actions.onUseManualHost, Modifier.fillMaxWidth())
                }
            }
        }
        if (scannerOpen) {
            GestoQrScannerOverlay(
                onInviteScanned = { raw ->
                    scannerOpen = false
                    inviteText = raw
                    actions.onApplyInvite(raw)
                },
                onDismiss = { scannerOpen = false },
            )
        }
    }
}

@Preview(name = "Discovery - dark", heightDp = 900)
@Composable
private fun DiscoveryScreenPreviewDark() = GestoPreviewSurface(darkTheme = true) {
    DiscoveryScreen(previewDiscoveryState, DiscoveryActions())
}

@Preview(name = "Discovery - light", heightDp = 900)
@Composable
private fun DiscoveryScreenPreviewLight() = GestoPreviewSurface(darkTheme = false) {
    DiscoveryScreen(previewDiscoveryState, DiscoveryActions())
}

private val previewDiscoveryState = DiscoveryUiState(
    discoveredHosts = listOf(
        DiscoveredHost(
            hostName = "frumorn-desktop",
            address = "192.168.1.42",
            port = 47771,
            fingerprint = "SHA256:7f1a3c9d20e4b8a6f0c1d2e3f4a5b6c7d8e9f0a1",
            inputSupported = true,
            inputBackend = "wayland-portal",
            captureSupported = true,
            captureBackend = "wayland-screencast-portal",
        ),
    ),
    manualAddress = "192.168.1.50",
    manualPort = "47771",
)
