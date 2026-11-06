package it.frumorn.gesto.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

/**
 * A small floating panel over the screen: title, [content], then Cancel and the confirm action at
 * the bottom end (only the confirm action when [dismissText] is null; back or a tap outside still
 * call [onDismiss]). Square corners and a 1 dp edge like the rest of the app; the only shadow is
 * the one floating panels get. [destructive] paints the confirm action in the error colour.
 */
@Composable
fun GestoDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String?,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val spacing = GestoTheme.spacing
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            border = BorderStroke(spacing.borderWidth, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = spacing.md,
        ) {
            Column(
                Modifier.padding(start = spacing.hero, end = spacing.hero, top = spacing.hero, bottom = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xl),
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                content()
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.End),
                ) {
                    if (dismissText != null) GestoTextButton(text = dismissText, onClick = onDismiss)
                    GestoTextButton(
                        text = confirmText,
                        onClick = onConfirm,
                        color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified,
                    )
                }
            }
        }
    }
}

@Preview(name = "Dialog - light")
@Composable
private fun GestoDialogPreview() = GestoPreviewSurface(darkTheme = false) {
    GestoDialog(
        title = "Forget Legion?",
        confirmText = "Forget",
        onConfirm = {},
        dismissText = "Cancel",
        onDismiss = {},
        destructive = true,
    ) {
        Text("To use it again you'll have to add it again.", style = MaterialTheme.typography.bodyMedium)
    }
}
