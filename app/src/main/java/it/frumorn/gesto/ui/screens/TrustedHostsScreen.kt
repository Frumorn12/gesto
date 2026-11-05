package it.frumorn.gesto.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.core.model.TrustedHost
import it.frumorn.gesto.ui.components.SectionHeader
import it.frumorn.gesto.ui.components.TrustedHostCard
import it.frumorn.gesto.ui.state.TrustedHostsActions
import it.frumorn.gesto.ui.state.TrustedHostsUiState
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

/** Pinned host identities kept in the Keystore-protected store. */
@Composable
fun TrustedHostsScreen(
    state: TrustedHostsUiState,
    actions: TrustedHostsActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GestoTheme.spacing.xl),
    ) {
        item {
            SectionHeader(
                "Trusted Hosts",
                "Pinned host identities stored with Android Keystore protected encryption.",
            )
            Button(onClick = actions.onDiscoverNewHost, modifier = Modifier.fillMaxWidth()) {
                Text("Discover new host")
            }
        }
        items(state.trustedHosts) { host ->
            TrustedHostCard(
                host = host,
                onConnect = { actions.onConnect(host) },
                onRemove = { actions.onRemove(host.id) },
            )
        }
    }
}

@Preview(name = "Trusted hosts - dark", heightDp = 700)
@Composable
private fun TrustedHostsScreenPreviewDark() = GestoPreviewSurface(darkTheme = true) {
    TrustedHostsScreen(previewTrustedHostsState, TrustedHostsActions())
}

@Preview(name = "Trusted hosts - light", heightDp = 700)
@Composable
private fun TrustedHostsScreenPreviewLight() = GestoPreviewSurface(darkTheme = false) {
    TrustedHostsScreen(previewTrustedHostsState, TrustedHostsActions())
}

private val previewTrustedHostsState = TrustedHostsUiState(
    trustedHosts = listOf(
        TrustedHost(
            id = "host-1",
            hostName = "frumorn-desktop",
            address = "192.168.1.42",
            port = 47771,
            fingerprint = "SHA256:7f1a3c9d20e4b8a6f0c1d2e3f4a5b6c7d8e9f0a1",
            deviceId = "device",
            sessionToken = "token",
            lastConnectedAt = 0L,
        ),
    ),
)
