package it.frumorn.gesto.ui.session

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.R
import it.frumorn.gesto.core.input.ModifierKey
import it.frumorn.gesto.core.input.StickyModifiers
import it.frumorn.gesto.ui.components.GestoButton
import it.frumorn.gesto.ui.components.GestoTileButton
import it.frumorn.gesto.ui.state.KeyboardActions as SheetActions
import it.frumorn.gesto.ui.theme.GestoTheme
import it.frumorn.gesto.ui.theme.ThemeMode

/**
 * The body of the keyboard sheet: the modifier row (Ctrl, Alt, Super, Shift, Esc, Tab), the cursor
 * row, and a text field that opens the system keyboard. A modifier stays on until the next key
 * goes out, so Ctrl then "a" is Ctrl+A on the PC. The frame (title, close, insets) is the host's.
 */
@Composable
fun KeyboardSheet(actions: SheetActions, modifier: Modifier = Modifier) {
    val spacing = GestoTheme.spacing
    val sticky = remember { StickyModifiers() }
    var active by remember { mutableStateOf(sticky.active) }
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    // A modifier left on must not leak into the next time the sheet opens.
    DisposableEffect(Unit) { onDispose { sticky.clear() } }

    fun sendKey(name: String) {
        actions.onKey(sticky.withModifiers(name))
        active = sticky.active
        // Enter moves the PC to a new line; the live text starts again too.
        if (name == "enter") text = ""
    }

    fun typed(next: String) {
        val appended = if (next.length > text.length && next.startsWith(text)) next.removePrefix(text) else ""
        val keys = if (appended.isEmpty()) null else sticky.consume(appended)
        if (keys != null) {
            // With a modifier on, the character is a shortcut; it never reaches the live buffer.
            actions.onKey(keys)
            active = sticky.active
            // A paste or swiped word: the first character was the shortcut, the rest is plain text.
            val rest = appended.substring(Character.charCount(appended.codePointAt(0)))
            if (rest.isNotEmpty()) {
                actions.onKeyboardEdit(text, text + rest)
                text += rest
            }
        } else {
            actions.onKeyboardEdit(text, next)
            text = next
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            ModifierKeys.forEach { (key, label) ->
                val name = stringResource(label)
                KeyCap(
                    label = name,
                    on = key in active,
                    onClick = {
                        sticky.toggle(key)
                        active = sticky.active
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            KeyCap(stringResource(R.string.key_esc), onClick = { sendKey("esc") }, modifier = Modifier.weight(1f))
            KeyCap(stringResource(R.string.key_tab), onClick = { sendKey("tab") }, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            CursorKeys.forEach { (name, glyph, description) ->
                KeyCap(
                    label = glyph,
                    description = stringResource(description),
                    onClick = { sendKey(name) },
                    modifier = Modifier.weight(1f),
                )
            }
            KeyCap(stringResource(R.string.key_enter), onClick = { sendKey("enter") }, modifier = Modifier.weight(2f))
        }
        TypeBox(
            value = text,
            onValueChange = ::typed,
            onEnter = { sendKey("enter") },
            modifier = Modifier.focusRequester(focus),
        )
    }
}

private val ModifierKeys = listOf(
    ModifierKey.Ctrl to R.string.key_ctrl,
    ModifierKey.Alt to R.string.key_alt,
    ModifierKey.Super to R.string.key_super,
    ModifierKey.Shift to R.string.key_shift,
)

private data class CursorKey(val name: String, val glyph: String, val description: Int)

private val CursorKeys = listOf(
    CursorKey("left", "←", R.string.key_left),
    CursorKey("up", "↑", R.string.key_up),
    CursorKey("down", "↓", R.string.key_down),
    CursorKey("right", "→", R.string.key_right),
    CursorKey("backspace", "⌫", R.string.key_backspace),
)

/** A key: a tile, or filled with the primary colour while it is on. */
@Composable
private fun KeyCap(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    on: Boolean = false,
    description: String = label,
) {
    val semantics = Modifier.semantics {
        contentDescription = description
        selected = on
    }
    // Six keys share the row: compact, so every label shows whole down to a 360 dp phone.
    if (on) {
        GestoButton(label, onClick, modifier.then(semantics), compact = true)
    } else {
        GestoTileButton(label, onClick, modifier.then(semantics), compact = true)
    }
}

@Composable
private fun TypeBox(
    value: String,
    onValueChange: (String) -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GestoTheme.spacing
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = spacing.keyHeight)
            .border(spacing.borderWidth, colors.primary, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = spacing.xl, vertical = spacing.lg),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        singleLine = true,
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onEnter() }),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        stringResource(R.string.keyboard_type_here),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
                inner()
            }
        },
    )
}

@Preview(name = "Keyboard sheet - light", widthDp = 412)
@Composable
private fun KeyboardSheetPreviewLight() {
    GestoTheme(mode = ThemeMode.Light) {
        Box(Modifier.padding(GestoTheme.spacing.xl)) { KeyboardSheet(SheetActions()) }
    }
}

@Preview(name = "Keyboard sheet - dark", widthDp = 412)
@Composable
private fun KeyboardSheetPreviewDark() {
    GestoTheme(mode = ThemeMode.Dark) {
        Box(Modifier.padding(GestoTheme.spacing.xl)) { KeyboardSheet(SheetActions()) }
    }
}
