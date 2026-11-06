package it.frumorn.gesto.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.R
import it.frumorn.gesto.core.model.CustomDimensionOptions
import it.frumorn.gesto.core.model.CustomFpsOptions
import it.frumorn.gesto.core.model.CustomQualityRange
import it.frumorn.gesto.core.model.QualityPreset
import it.frumorn.gesto.core.model.ScreenSource
import it.frumorn.gesto.core.model.settings
import it.frumorn.gesto.ui.components.GestoIcons
import it.frumorn.gesto.ui.components.GestoSegmented
import it.frumorn.gesto.ui.components.GestoSlider
import it.frumorn.gesto.ui.components.GestoTextButton
import it.frumorn.gesto.ui.components.Segment
import it.frumorn.gesto.ui.components.SettingsBlock
import it.frumorn.gesto.ui.components.SettingsDivider
import it.frumorn.gesto.ui.components.SettingsSectionLabel
import it.frumorn.gesto.ui.session.qualityLabel
import it.frumorn.gesto.ui.state.AdvancedActions
import it.frumorn.gesto.ui.state.AdvancedUiState
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme
import kotlin.math.roundToInt

/**
 * Advanced: the stream's own frame rate, size and encoder quality (any change makes the quality
 * Custom) and the PC's screen to show. Values are in monospace; this is where technical words go.
 */
@Composable
fun AdvancedScreen(
    state: AdvancedUiState,
    actions: AdvancedActions,
    modifier: Modifier = Modifier,
) {
    val streaming = state.streaming
    val readout = GestoTheme.accentTypography.readout
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = GestoTheme.spacing.gutter),
    ) {
        Text(
            stringResource(
                R.string.advanced_now,
                stringResource(state.quality.qualityLabel),
                stringResource(R.string.advanced_values, streaming.maxDimension, streaming.maxFps, streaming.jpegQuality),
            ),
            style = readout,
            modifier = Modifier.padding(top = GestoTheme.spacing.md),
        )
        Text(
            stringResource(R.string.advanced_custom_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = GestoTheme.spacing.xs),
        )

        SettingsSectionLabel(stringResource(R.string.advanced_section_stream))
        SettingsBlock(title = stringResource(R.string.advanced_fps)) {
            GestoSegmented(
                segments = CustomFpsOptions.map { Segment(it, "$it") },
                selected = streaming.maxFps,
                onSelect = actions.onSetFps,
                monospace = true,
            )
        }
        SettingsBlock(title = stringResource(R.string.advanced_dimension)) {
            GestoSegmented(
                segments = CustomDimensionOptions.map { Segment(it, "$it") },
                // Sharp streams at 2400 px, which is not one of the offers: then none is filled.
                selected = streaming.maxDimension,
                onSelect = actions.onSetMaxDimension,
                monospace = true,
            )
        }
        EncoderQuality(streaming.jpegQuality, actions.onSetQuality)

        SettingsSectionLabel(stringResource(R.string.advanced_source))
        Sources(state, actions)
    }
}

/** The encoder quality slider, 35 to 92; the value is kept when the finger leaves it. */
@Composable
private fun EncoderQuality(quality: Int, onSet: (Int) -> Unit) {
    var shown by remember(quality) { mutableFloatStateOf(quality.toFloat()) }
    val title = stringResource(R.string.advanced_encoder_quality)
    SettingsBlock(title = title, description = stringResource(R.string.advanced_encoder_quality_text)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GestoTheme.spacing.xl),
        ) {
            GestoSlider(
                value = shown,
                onValueChange = { shown = it },
                onValueChangeFinished = { onSet(shown.roundToInt()) },
                valueRange = CustomQualityRange.first.toFloat()..CustomQualityRange.last.toFloat(),
                steps = CustomQualityRange.last - CustomQualityRange.first - 1,
                description = title,
                modifier = Modifier.weight(1f),
            )
            Text("q${shown.roundToInt()}", style = GestoTheme.accentTypography.readout)
        }
    }
}

/** The PC's screens, fastest first, and the choice to leave it to the PC. */
@Composable
private fun Sources(state: AdvancedUiState, actions: AdvancedActions) {
    if (!state.connected) {
        Note(stringResource(R.string.advanced_source_offline))
        return
    }
    if (state.sourcesLoading && state.sources.isEmpty()) {
        Note(stringResource(R.string.advanced_source_loading))
        return
    }
    Column(Modifier.selectableGroup()) {
        SourceRow(
            title = stringResource(R.string.advanced_source_remembered),
            detail = stringResource(R.string.advanced_source_remembered_text),
            technical = null,
            selected = state.selectedSourceId == null,
            onSelect = { actions.onSelectSource(null) },
        )
        groupSources(state.sources).forEach { (group, sources) ->
            Column(Modifier.padding(top = GestoTheme.spacing.xl, bottom = GestoTheme.spacing.xs)) {
                Text(stringResource(group.title), style = MaterialTheme.typography.labelMedium)
                Text(
                    stringResource(group.caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SettingsDivider()
            sources.forEach { source ->
                SourceRow(
                    title = source.title,
                    detail = source.quality?.let { stringResource(it) },
                    technical = listOfNotNull(source.backend, source.resolution).joinToString(" · "),
                    selected = state.selectedSourceId == source.id,
                    onSelect = { actions.onSelectSource(source.id) },
                )
            }
        }
    }
    if (state.sources.isEmpty()) Note(stringResource(R.string.advanced_source_none))
    GestoTextButton(
        text = stringResource(R.string.advanced_source_reload),
        onClick = actions.onReloadSources,
        icon = GestoIcons.Refresh,
        enabled = !state.sourcesLoading,
    )
}

/** One screen to choose: tinted with the title in the primary colour while chosen. */
@Composable
private fun SourceRow(
    title: String,
    detail: String?,
    technical: String?,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(if (selected) colors.primaryContainer else Color.Transparent)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = GestoTheme.spacing.md, vertical = GestoTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(GestoTheme.spacing.hairline),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (selected) colors.primary else colors.onSurface)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            if (technical != null) Text(technical, style = GestoTheme.accentTypography.readout, color = colors.onSurfaceVariant)
        }
        SettingsDivider()
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = GestoTheme.spacing.md),
    )
}

private val PreviewSources = listOf(
    ScreenSource("portal:chooser", "Portal picker (PipeWire screencast — 30–60 FPS)", "monitor", "wayland-screencast-portal", 0, 0, 0, 0, 1.0, false),
    ScreenSource("hyprland:monitor:eDP-1", "eDP-1 (BOE 0x0BCA)", "monitor", "hyprland-grim", 1920, 1200, 0, 0, 1.0, true),
)

@Preview(name = "Advanced - dark", heightDp = 1100)
@Composable
private fun AdvancedScreenPreviewDark() = GestoPreviewSurface(darkTheme = true) {
    AdvancedScreen(
        AdvancedUiState(connected = true, sources = PreviewSources, selectedSourceId = "portal:chooser"),
        AdvancedActions(),
    )
}

@Preview(name = "Advanced - light, sharp, offline", heightDp = 900)
@Composable
private fun AdvancedScreenPreviewLight() = GestoPreviewSurface(darkTheme = false) {
    AdvancedScreen(
        AdvancedUiState(quality = QualityPreset.Sharp, streaming = QualityPreset.Sharp.settings(showStats = true)),
        AdvancedActions(),
    )
}
