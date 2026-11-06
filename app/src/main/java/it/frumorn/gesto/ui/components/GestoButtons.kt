package it.frumorn.gesto.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextOverflow
import it.frumorn.gesto.ui.theme.GestoTheme

// Material 3 buttons default to pills and ignore MaterialTheme.shapes; these wrappers apply the
// Gesto shape (extraSmall) and the labelLarge text style everywhere.

private val ButtonHeight = 48.dp
private val IconGap = 8.dp
private val IconTarget = 40.dp
internal const val DisabledAlpha = 0.38f

private fun Modifier.described(description: String?): Modifier =
    if (description == null) this else semantics { contentDescription = description }

@Composable
private fun ButtonLabel(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        if (text.isNotEmpty()) Spacer(Modifier.width(IconGap))
    }
    if (text.isNotEmpty()) Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Filled primary button. With an empty [text] it shows only [icon]; give it a [description] then. */
@Composable
fun GestoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    description: String? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.described(description).defaultMinSize(minHeight = ButtonHeight).height(ButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraSmall,
    ) { ButtonLabel(text, icon) }
}

/** Transparent button with a 1 dp primary border. */
@Composable
fun GestoOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(ButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraSmall,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
        border = BorderStroke(
            GestoTheme.spacing.borderWidth,
            if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        ),
    ) { ButtonLabel(text, icon) }
}

/**
 * Neutral tile, like a key: 1 dp outlineVariant border on the page colour, primaryContainer while
 * pressed. For pads of equal actions (the touchpad's Left and Right).
 */
@Composable
fun GestoTileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    description: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.described(description).height(ButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraSmall,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (pressed) colors.primaryContainer else colors.background,
            contentColor = colors.onSurface,
        ),
        border = BorderStroke(GestoTheme.spacing.borderWidth, colors.outlineVariant),
        interactionSource = interaction,
    ) { ButtonLabel(text, icon) }
}

/**
 * A tile with the icon above a short label, for a row of equal quick actions (Right click,
 * Keyboard, Audio under the remote screen). With [checked] it is a switch, tinted while on.
 */
@Composable
fun GestoQuickTile(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    checked: Boolean? = null,
    enabled: Boolean = true,
    height: Dp = GestoTheme.spacing.quickTileHeight,
) {
    val colors = MaterialTheme.colorScheme
    val on = checked == true
    val shape = MaterialTheme.shapes.extraSmall
    val border = if (on) null else BorderStroke(GestoTheme.spacing.borderWidth, colors.outlineVariant)
    val container = if (on) colors.primaryContainer else colors.background
    val content = if (on) colors.primary else colors.onSurface
    val body: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(GestoTheme.spacing.hairline, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(GestoTheme.spacing.menuIconSize))
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
        }
    }
    val sized = modifier.height(height).alpha(if (enabled) 1f else DisabledAlpha)
    if (checked == null) {
        Surface(onClick = onClick, enabled = enabled, modifier = sized, shape = shape, color = container, contentColor = content, border = border) { body() }
    } else {
        Surface(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { onClick() },
            modifier = sized,
            shape = shape,
            color = container,
            contentColor = content,
            border = border,
        ) { body() }
    }
}

/** Borderless text button; [color] replaces the primary colour (e.g. error for a destructive action). */
@Composable
fun GestoTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = Color.Unspecified,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(ButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraSmall,
        colors = if (color == Color.Unspecified) {
            ButtonDefaults.textButtonColors()
        } else {
            ButtonDefaults.textButtonColors(contentColor = color)
        },
    ) { ButtonLabel(text, icon) }
}

/** Square 40 dp icon button; draws a 1 dp outlineVariant border only when [outlined]. */
@Composable
fun GestoIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.extraSmall
    val border = if (outlined) {
        Modifier.border(GestoTheme.spacing.borderWidth, MaterialTheme.colorScheme.outlineVariant, shape)
    } else {
        Modifier
    }
    IconButton(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize().size(IconTarget).then(border),
        enabled = enabled,
        shape = shape,
        content = content,
    )
}
