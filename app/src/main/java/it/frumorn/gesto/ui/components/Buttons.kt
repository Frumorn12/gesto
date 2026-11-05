package it.frumorn.gesto.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

/** Oversized tonal button used for the mouse-button row under the touchpad. */
@Composable
fun ClickButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(GestoTheme.spacing.clickButtonHeight),
        shape = MaterialTheme.shapes.extraSmall,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Equal-width outlined action inside a [ControlGroup] row. */
@Composable
fun RowScope.ActionButton(label: String, onClick: () -> Unit) {
    GestoOutlinedButton(label, onClick, Modifier.weight(1f))
}

/** Titled card grouping related host actions, badged with host support for that group. */
@Composable
fun ControlGroup(
    title: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    GestoCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            CapabilityPill(capabilityLabel(enabled), available = enabled)
        }
        Spacer(Modifier.height(GestoTheme.spacing.xl))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GestoTheme.spacing.md),
        ) {
            content()
        }
    }
}

/** Two-column grid of keyboard shortcut buttons. */
@Composable
fun ShortcutGrid(items: List<Pair<String, () -> Unit>>, modifier: Modifier = Modifier) {
    GestoCard(modifier) {
        items.chunked(SHORTCUT_COLUMNS).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GestoTheme.spacing.md),
            ) {
                row.forEach { (label, action) ->
                    GestoOutlinedButton(label, action, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(GestoTheme.spacing.md))
        }
    }
}

private const val SHORTCUT_COLUMNS = 2

@Preview(name = "Buttons - dark")
@Composable
private fun ButtonsPreviewDark() = GestoPreviewSurface(darkTheme = true) { ButtonsPreviewBody() }

@Preview(name = "Buttons - light")
@Composable
private fun ButtonsPreviewLight() = GestoPreviewSurface(darkTheme = false) { ButtonsPreviewBody() }

@Composable
private fun ButtonsPreviewBody() {
    Column(verticalArrangement = Arrangement.spacedBy(GestoTheme.spacing.xl)) {
        Row(horizontalArrangement = Arrangement.spacedBy(GestoTheme.spacing.lg)) {
            ClickButton("Left", Modifier.weight(1f)) {}
            ClickButton("Right", Modifier.weight(1f)) {}
            ClickButton("Middle", Modifier.weight(1f)) {}
        }
        ControlGroup("Media", enabled = true) {
            ActionButton("Play/Pause") {}
            ActionButton("Previous") {}
            ActionButton("Next") {}
        }
        ShortcutGrid(listOf("Copy" to {}, "Paste" to {}, "Undo" to {}))
    }
}
