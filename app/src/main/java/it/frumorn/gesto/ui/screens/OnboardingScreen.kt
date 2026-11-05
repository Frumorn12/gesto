package it.frumorn.gesto.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.ui.state.OnboardingActions
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

/** First-run pitch plus the two ways into the app. */
@Composable
fun OnboardingScreen(
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            "Wayland control,\nwithout X11 shortcuts.",
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(GestoTheme.spacing.gutter))
        Text(
            "Pair your Android phone with a Linux Wayland host. Gesto uses a pinned host identity, " +
                "encrypted command channel, and portal-aware capability checks.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(GestoTheme.spacing.section))
        Button(onClick = actions.onDiscoverHosts, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.width(GestoTheme.spacing.md))
            Text("Discover hosts")
        }
        Spacer(Modifier.height(GestoTheme.spacing.lg))
        OutlinedButton(onClick = actions.onOpenTrustedHosts, modifier = Modifier.fillMaxWidth()) {
            Text("Trusted hosts")
        }
    }
}

@Preview(name = "Onboarding - dark", heightDp = 640)
@Composable
private fun OnboardingScreenPreviewDark() = GestoPreviewSurface(darkTheme = true) {
    OnboardingScreen(OnboardingActions())
}

@Preview(name = "Onboarding - light", heightDp = 640)
@Composable
private fun OnboardingScreenPreviewLight() = GestoPreviewSurface(darkTheme = false) {
    OnboardingScreen(OnboardingActions())
}
