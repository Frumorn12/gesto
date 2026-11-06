package it.frumorn.gesto.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.R
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
 * Top bar of the screens outside a session: a back arrow (or, on the PCs list, the Gesto mark and
 * name), optionally a centred [title] and the settings entry, and the error strip of the old
 * screens underneath.
 */
@Composable
fun GestoPageBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    title: String? = null,
    error: String? = null,
) {
    val spacing = GestoTheme.spacing
    Column(modifier.windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.xs, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                GestoIconButton(onClick = onBack) {
                    Icon(
                        GestoIcons.Back,
                        contentDescription = stringResource(R.string.action_back),
                        modifier = Modifier.size(spacing.barIconSize),
                    )
                }
            } else {
                Spacer(Modifier.width(spacing.xl))
                GestoMark(size = spacing.barIconSize)
                Spacer(Modifier.width(spacing.md))
                Text(stringResource(R.string.app_name), style = GestoTheme.accentTypography.brand)
            }
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                // Balances the back arrow so the title sits in the middle.
                if (onOpenSettings == null) Spacer(Modifier.width(spacing.barSideSlot))
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (onOpenSettings != null) {
                GestoIconButton(onClick = onOpenSettings) {
                    Icon(
                        GestoIcons.Settings,
                        contentDescription = stringResource(R.string.action_settings),
                        modifier = Modifier.size(spacing.barIconSize),
                    )
                }
            }
        }
        AnimatedErrorBanner(
            message = error,
            modifier = Modifier.padding(horizontal = spacing.gutter),
        )
    }
}

@Preview(name = "Page bar - home - light")
@Composable
private fun GestoPageBarPreviewHome() = GestoPreviewSurface(darkTheme = false) {
    Column {
        GestoPageBar(onOpenSettings = {})
        SectionHeader("Your PCs", "Pick the PC you want to control.")
    }
}

@Preview(name = "Page bar - back, title - light")
@Composable
private fun GestoPageBarPreviewTitle() = GestoPreviewSurface(darkTheme = false) {
    GestoPageBar(onBack = {}, title = "Add a PC")
}

@Preview(name = "Page bar - back, error - dark")
@Composable
private fun GestoPageBarPreviewBack() = GestoPreviewSurface(darkTheme = true) {
    GestoPageBar(onBack = {}, error = "Enter the 6 digit pairing code shown on the Linux host.")
}
