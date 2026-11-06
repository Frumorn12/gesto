package it.frumorn.gesto.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.ui.theme.GestoMotion
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme

// The rows of the Settings, Advanced and Diagnostics pages, drawn like the prototype: rows split
// by 1 dp lines, small uppercase labels over each group, square switches and segmented controls.

/** The small uppercase label over a group of rows. */
@Composable
fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = GestoTheme.spacing.gutter, bottom = GestoTheme.spacing.md),
    )
}

/** The 1 dp line under every row. */
@Composable
fun SettingsDivider() {
    HorizontalDivider(thickness = GestoTheme.spacing.borderWidth, color = MaterialTheme.colorScheme.outlineVariant)
}

/** A setting with its [title], a control under it and an optional line explaining it below. */
@Composable
fun SettingsBlock(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    control: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(vertical = GestoTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(GestoTheme.spacing.md),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            control()
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SettingsDivider()
    }
}

/** One choice of a [GestoSegmented]. */
@Immutable
data class Segment<T>(val value: T, val label: String)

/**
 * Equal segments in one box; the chosen one is filled with the primary colour. With [selected]
 * matching none of them, none is filled. [monospace] for numbers (Advanced).
 */
@Composable
fun <T> GestoSegmented(
    segments: List<Segment<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = GestoTheme.spacing
    val shape = MaterialTheme.shapes.extraSmall
    val style = MaterialTheme.typography.labelMedium.let { if (monospace) it.copy(fontFamily = FontFamily.Monospace) else it }
    Row(
        modifier
            .fillMaxWidth()
            .height(spacing.segmentHeight)
            .clip(shape)
            .border(spacing.borderWidth, colors.outlineVariant, shape)
            .selectableGroup(),
    ) {
        segments.forEachIndexed { index, segment ->
            if (index > 0) {
                Box(
                    Modifier
                        .width(spacing.borderWidth)
                        .fillMaxHeight()
                        .background(colors.outlineVariant),
                )
            }
            val on = segment.value == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (on) colors.primary else colors.background)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(segment.value) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    segment.label,
                    style = style,
                    color = if (on) colors.onPrimary else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = spacing.xs),
                )
            }
        }
    }
}

/** The square switch: a knob that slides right and turns the box primary when [checked]. Drawing only. */
@Composable
fun GestoSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val spacing = GestoTheme.spacing
    val shape = MaterialTheme.shapes.extraSmall
    val travel = spacing.switchWidth - spacing.borderWidth * 2 - spacing.switchInset * 2 - spacing.switchKnob
    val offset by animateDpAsState(
        targetValue = if (checked) travel else spacing.none,
        animationSpec = tween(GestoMotion.SWITCH_MS, easing = GestoMotion.Enter),
        label = "switch-knob",
    )
    val box by animateColorAsState(if (checked) colors.primary else colors.surfaceVariant, tween(GestoMotion.SWITCH_MS), label = "switch-box")
    val knob by animateColorAsState(if (checked) colors.onPrimary else colors.outline, tween(GestoMotion.SWITCH_MS), label = "switch-knob-colour")
    Box(
        modifier
            .size(spacing.switchWidth, spacing.switchHeight)
            .clip(shape)
            .background(box)
            .border(spacing.borderWidth, if (checked) colors.primary else colors.outlineVariant, shape)
            .padding(spacing.borderWidth + spacing.switchInset),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(spacing.switchKnob)
                .clip(shape)
                .background(knob),
        )
    }
}

/** A whole row that switches a setting on and off: [title], an optional [description], the switch. */
@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(vertical = GestoTheme.spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GestoTheme.spacing.xl),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                if (description != null) {
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            GestoSwitch(checked)
        }
        SettingsDivider()
    }
}

/** A row that opens another page, with a chevron at its end. */
@Composable
fun SettingsLinkRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = GestoTheme.spacing.xxl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GestoTheme.spacing.xl),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(
                GestoIcons.Chevron,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(GestoTheme.spacing.chevronSize),
            )
        }
        SettingsDivider()
    }
}

/**
 * A slider with a 4 dp track and a square thumb ringed in the page colour. [onValueChange] follows
 * the finger; [onValueChangeFinished] is where the value is kept.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    description: String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val spacing = GestoTheme.spacing
    val shape = MaterialTheme.shapes.extraSmall
    val span = valueRange.endInclusive - valueRange.start
    val fraction = if (span > 0f) ((value - valueRange.start) / span).coerceIn(0f, 1f) else 0f
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        thumb = {
            Box(
                Modifier
                    .size(spacing.sliderThumb)
                    .clip(shape)
                    .background(colors.primary)
                    .border(spacing.sliderThumbRing, colors.background, shape),
            )
        },
        track = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(spacing.sliderTrack),
            ) {
                if (fraction > 0f) Box(Modifier.weight(fraction).fillMaxHeight().background(colors.primary))
                if (fraction < 1f) Box(Modifier.weight(1f - fraction).fillMaxHeight().background(colors.outlineVariant))
            }
        },
    )
}

@Preview(name = "Settings controls - light")
@Composable
private fun SettingsControlsPreviewLight() = GestoPreviewSurface(darkTheme = false) { SettingsControlsPreviewBody() }

@Preview(name = "Settings controls - dark")
@Composable
private fun SettingsControlsPreviewDark() = GestoPreviewSurface(darkTheme = true) { SettingsControlsPreviewBody() }

@Composable
private fun SettingsControlsPreviewBody() {
    Column {
        SettingsSectionLabel("Remote screen")
        SettingsBlock("Quality", description = "Picks for your network. Now: 1080p, 60 fps.") {
            GestoSegmented(
                segments = listOf(Segment(0, "Automatic"), Segment(1, "Smooth"), Segment(2, "Sharp"), Segment(3, "Saver")),
                selected = 0,
                onSelect = {},
            )
        }
        SettingsSwitchRow("Show fps and delay", checked = true, onCheckedChange = {}, description = "A small line over the screen.")
        SettingsSwitchRow("Vibration", checked = false, onCheckedChange = {})
        SettingsBlock("Pointer speed") {
            GestoSlider(value = 1.5f, onValueChange = {}, valueRange = 0.5f..3f, description = "Pointer speed")
        }
        Spacer(Modifier.height(GestoTheme.spacing.md))
        SettingsLinkRow("Diagnostics", onClick = {})
    }
}
