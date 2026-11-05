package it.frumorn.gesto.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shared scaffold for `@Preview` functions.
 *
 * The mode is forced so previews are deterministic.
 */
@Composable
fun GestoPreviewSurface(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    GestoTheme(mode = if (darkTheme) ThemeMode.Dark else ThemeMode.Light) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(GestoTheme.spacing.gutter),
            ) {
                content()
            }
        }
    }
}
