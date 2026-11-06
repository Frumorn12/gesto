package it.frumorn.gesto.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** How long the finger must stay down before a hold-to-confirm action runs. */
const val HOLD_TO_CONFIRM_MS = 1_200

/**
 * A destructive action that runs only after the finger has stayed on it for [HOLD_TO_CONFIRM_MS]:
 * a red bar fills the button meanwhile, and lifting early empties it and does nothing. A screen
 * reader's activate action (and any tap without a hold) calls [onActivate], which should ask first: it never runs [onConfirmed] by itself.
 */
@Composable
fun HoldToConfirmButton(
    text: String,
    onConfirmed: () -> Unit,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val currentConfirmed by rememberUpdatedState(onConfirmed)
    val currentActivate by rememberUpdatedState(onActivate)
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraSmall
    Surface(
        modifier = modifier
            .fillMaxWidth().height(GestoTheme.spacing.commandKeyHeight)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (enabled) onClick { currentActivate(); true }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        val hold: Job = scope.launch {
                            progress.snapTo(0f)
                            progress.animateTo(1f, tween(HOLD_TO_CONFIRM_MS, easing = LinearEasing))
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentConfirmed()
                            progress.snapTo(0f)
                        }
                        tryAwaitRelease()
                        if (hold.isActive) {
                            hold.cancel()
                            scope.launch { progress.animateTo(0f, tween(150)) }
                        }
                    },
                )
            },
        shape = shape,
        color = colors.background,
        contentColor = colors.error,
        border = BorderStroke(GestoTheme.spacing.borderWidth, colors.error),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(progress.value)
                    .background(colors.errorContainer),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(GestoTheme.spacing.menuIconSize))
                    Spacer(Modifier.width(GestoTheme.spacing.md))
                }
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}


@Preview(name = "HoldToConfirm")
@Composable
private fun HoldToConfirmPreview() = GestoPreviewSurface {
    HoldToConfirmButton("Hold to suspend", onConfirmed = {}, onActivate = {}, icon = GestoIcons.Moon)
}
