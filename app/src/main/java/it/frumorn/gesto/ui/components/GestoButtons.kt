package it.frumorn.gesto.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import it.frumorn.gesto.ui.theme.GestoTheme

// Material 3 buttons default to pills and ignore MaterialTheme.shapes; these wrappers apply the
// Gesto shape (extraSmall) and the labelLarge text style everywhere.

private val ButtonHeight = 48.dp
private val IconGap = 8.dp
private val IconTarget = 40.dp

@Composable
private fun ButtonLabel(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(IconGap))
    }
    Text(text, style = MaterialTheme.typography.labelLarge)
}

/** Filled primary button. */
@Composable
fun GestoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = ButtonHeight).height(ButtonHeight),
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

/** Borderless text button. */
@Composable
fun GestoTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(ButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraSmall,
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
