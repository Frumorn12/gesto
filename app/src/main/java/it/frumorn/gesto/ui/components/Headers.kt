@file:OptIn(ExperimentalMaterial3Api::class)

package it.frumorn.gesto.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.ui.state.ShellUiState
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

/** Screen title + one-line explanation, repeated at the top of every content screen. */
@Composable
fun SectionHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = GestoTheme.spacing.md)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(GestoTheme.spacing.xs))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * App bar showing the brand, the connected host (or the current status line) and the settings entry
 * point, with the global error strip docked underneath.
 *
 * The Material 3 Expressive `TopAppBar` overload with a dedicated `subtitle` slot exists in
 * material3 1.4.0 but is `internal`, so the second line is rendered inside the `title` slot.
 */
@Composable
fun GestoTopBar(
    state: ShellUiState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        "Gesto",
                        style = GestoTheme.accentTypography.brand,
                    )
                    Text(
                        state.hostName ?: state.status,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon = {
                GestoMark(
                    Modifier.padding(start = GestoTheme.spacing.xl),
                    size = GestoTheme.spacing.brandMarkSize,
                )
            },
            actions = {
                GestoIconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
        AnimatedErrorBanner(
            message = state.error,
            modifier = Modifier.padding(horizontal = GestoTheme.spacing.gutter),
        )
    }
}

@Preview(name = "Top bar - dark")
@Composable
private fun GestoTopBarPreviewDark() = GestoPreviewSurface(darkTheme = true) {
    Column {
        GestoTopBar(ShellUiState(hostName = "frumorn-desktop", status = "Connected"), onOpenSettings = {})
        SectionHeader("Host Discovery", "UDP LAN discovery with manual IP and QR invite fallback.")
    }
}

@Preview(name = "Top bar - error - light")
@Composable
private fun GestoTopBarPreviewLight() = GestoPreviewSurface(darkTheme = false) {
    GestoTopBar(
        state = ShellUiState(status = "Pairing failed", error = "Connection refused"),
        onOpenSettings = {},
    )
}
