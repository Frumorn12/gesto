package it.frumorn.gesto.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.ui.theme.GestoMotion
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

/** Opacity of the small panels laid over the picture (the QR scanner's hint). */
internal const val OVERLAY_ALPHA = 0.86f

/** Inline error strip shown under the top bar and under the remote viewport. */
@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(GestoTheme.spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null)
            Spacer(Modifier.width(GestoTheme.spacing.lg))
            Text(message, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** [ErrorBanner] that fades itself in and out when [message] appears/disappears. */
@Composable
fun AnimatedErrorBanner(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = message != null, modifier = modifier) {
        ErrorBanner(message ?: "")
    }
}

private const val LOADING_MS = 1100
private const val LOADING_BLOCK = 0.4f

/** Thin indeterminate bar: a block that slides across a 3 dp track, for waits with no known end. */
@Composable
fun GestoLoadingBar(modifier: Modifier = Modifier) {
    val progress by rememberInfiniteTransition(label = "loading").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(LOADING_MS, easing = GestoMotion.Enter)),
        label = "loading-block",
    )
    val track = MaterialTheme.colorScheme.outlineVariant
    val block = MaterialTheme.colorScheme.primary
    Canvas(
        modifier
            .width(GestoTheme.spacing.loadingBarWidth)
            .height(GestoTheme.spacing.loadingBarHeight)
            .clipToBounds()
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
    ) {
        drawRect(track)
        val width = size.width * LOADING_BLOCK
        // From just outside the left edge to just past the right one.
        val x = -width + progress * (size.width + width)
        drawRect(block, topLeft = Offset(x, 0f), size = Size(width, size.height))
    }
}

@Preview(name = "Indicators - dark")
@Composable
private fun IndicatorsPreviewDark() = GestoPreviewSurface(darkTheme = true) { IndicatorsPreviewBody() }

@Preview(name = "Indicators - light")
@Composable
private fun IndicatorsPreviewLight() = GestoPreviewSurface(darkTheme = false) { IndicatorsPreviewBody() }

@Composable
private fun IndicatorsPreviewBody() {
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(GestoTheme.spacing.xl)) {
        GestoLoadingBar()
        ErrorBanner("Screen stream dropped; retrying...")
    }
}
