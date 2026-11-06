package it.frumorn.gesto.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import it.frumorn.gesto.BuildConfig
import it.frumorn.gesto.R
import it.frumorn.gesto.core.input.PointerAcceleration
import it.frumorn.gesto.core.model.QualityCycle
import it.frumorn.gesto.core.model.QualityPreset
import it.frumorn.gesto.core.model.TouchMode
import it.frumorn.gesto.core.model.settings
import it.frumorn.gesto.core.model.videoLines
import it.frumorn.gesto.ui.components.GestoDialog
import it.frumorn.gesto.ui.components.GestoSegmented
import it.frumorn.gesto.ui.components.GestoSlider
import it.frumorn.gesto.ui.components.Segment
import it.frumorn.gesto.ui.components.SettingsBlock
import it.frumorn.gesto.ui.components.SettingsLinkRow
import it.frumorn.gesto.ui.components.SettingsSectionLabel
import it.frumorn.gesto.ui.components.SettingsSwitchRow
import it.frumorn.gesto.ui.session.label
import it.frumorn.gesto.ui.session.qualityLabel
import it.frumorn.gesto.ui.state.SettingsActions
import it.frumorn.gesto.ui.state.SettingsUiState
import it.frumorn.gesto.ui.theme.GestoPreviewSurface
import it.frumorn.gesto.ui.theme.GestoTheme
import it.frumorn.gesto.ui.theme.ThemeMode
import kotlin.math.roundToInt

/** Pointer speed steps of 0.1 between 0.5 and 3.0. */
private val PointerSpeedSteps =
    ((PointerAcceleration.MAX_SPEED - PointerAcceleration.MIN_SPEED) * 10).roundToInt() - 1

/**
 * Settings in plain words: the remote screen, the touchpad, the look of the app, and the way to
 * the PCs, Diagnostics, Advanced and the licences. Technical values live in Advanced.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    var showAbout by rememberSaveable { mutableStateOf(false) }
    if (showAbout) AboutDialog(onClose = { showAbout = false })
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = GestoTheme.spacing.gutter),
    ) {
        SettingsSectionLabel(stringResource(R.string.settings_section_screen))
        SettingsBlock(title = stringResource(R.string.settings_quality), description = qualityExplanation(state)) {
            GestoSegmented(
                segments = QualityCycle.map { Segment(it, stringResource(it.qualityLabel)) },
                // Custom has no segment: it comes from Advanced, and none is filled then.
                selected = state.quality,
                onSelect = actions.onSelectQuality,
            )
        }
        SettingsBlock(title = stringResource(R.string.settings_touch)) {
            GestoSegmented(
                segments = TouchMode.entries.map { Segment(it, stringResource(it.label)) },
                selected = state.touchMode,
                onSelect = actions.onSetTouchMode,
            )
        }
        SettingsSwitchRow(
            title = stringResource(R.string.settings_stats),
            description = stringResource(R.string.settings_stats_text),
            checked = state.showStats,
            onCheckedChange = { actions.onToggleStats() },
        )

        SettingsSectionLabel(stringResource(R.string.settings_section_touchpad))
        PointerSpeed(state.pointerSpeed, actions.onSetPointerSpeed)
        SettingsSwitchRow(
            title = stringResource(R.string.settings_acceleration),
            description = stringResource(R.string.settings_acceleration_text),
            checked = state.pointerAcceleration,
            onCheckedChange = actions.onSetPointerAcceleration,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_two_finger),
            checked = state.twoFingerRightClick,
            onCheckedChange = actions.onSetTwoFingerRightClick,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_haptics),
            description = stringResource(R.string.settings_haptics_text),
            checked = state.haptics,
            onCheckedChange = { actions.onToggleHaptics() },
        )

        SettingsSectionLabel(stringResource(R.string.settings_section_look))
        SettingsBlock(title = stringResource(R.string.settings_theme)) {
            GestoSegmented(
                segments = listOf(
                    Segment(ThemeMode.System, stringResource(R.string.theme_system)),
                    Segment(ThemeMode.Light, stringResource(R.string.theme_light)),
                    Segment(ThemeMode.Dark, stringResource(R.string.theme_dark)),
                ),
                selected = state.themeMode,
                onSelect = actions.onSetThemeMode,
            )
        }
        SettingsSwitchRow(
            title = stringResource(R.string.settings_rounded),
            description = stringResource(R.string.settings_rounded_text),
            checked = state.roundedCorners,
            onCheckedChange = actions.onSetRoundedCorners,
        )

        SettingsSectionLabel(stringResource(R.string.settings_section_more))
        SettingsLinkRow(stringResource(R.string.nav_your_pcs), onClick = actions.onOpenPcs)
        SettingsLinkRow(stringResource(R.string.settings_diagnostics), onClick = actions.onOpenDiagnostics)
        SettingsLinkRow(stringResource(R.string.settings_advanced), onClick = actions.onOpenAdvanced)
        SettingsLinkRow(stringResource(R.string.settings_about), onClick = { showAbout = true })
    }
}

/** The line under the quality: what it is for, and for Automatic and Custom what streams now. */
@Composable
private fun qualityExplanation(state: SettingsUiState): String {
    val lines = state.streaming.videoLines
    val fps = state.streaming.maxFps
    return when (state.quality) {
        QualityPreset.Auto -> stringResource(R.string.settings_quality_auto, lines, fps)
        QualityPreset.Smooth -> stringResource(R.string.settings_quality_smooth)
        QualityPreset.Sharp -> stringResource(R.string.settings_quality_sharp)
        QualityPreset.Saver -> stringResource(R.string.settings_quality_saver)
        QualityPreset.Custom -> stringResource(R.string.settings_quality_custom, lines, fps)
    }
}

/** The pointer speed slider; the speed is kept when the finger leaves it. */
@Composable
private fun PointerSpeed(speed: Float, onSet: (Float) -> Unit) {
    var shown by remember(speed) { mutableFloatStateOf(speed) }
    val title = stringResource(R.string.settings_pointer_speed)
    SettingsBlock(title = title) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GestoTheme.spacing.xl),
        ) {
            GestoSlider(
                value = shown,
                onValueChange = { shown = it },
                onValueChangeFinished = { onSet(shown) },
                valueRange = PointerAcceleration.MIN_SPEED..PointerAcceleration.MAX_SPEED,
                steps = PointerSpeedSteps,
                description = title,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.settings_pointer_speed_value, shown),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Version and licences: Gesto's (MIT) and Manrope's (SIL OFL 1.1). */
@Composable
private fun AboutDialog(onClose: () -> Unit) {
    GestoDialog(
        title = stringResource(R.string.about_title, BuildConfig.VERSION_NAME),
        confirmText = stringResource(R.string.about_close),
        onConfirm = onClose,
        dismissText = null,
        onDismiss = onClose,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(GestoTheme.spacing.md)) {
            Text(stringResource(R.string.about_gesto), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.about_manrope), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(name = "Settings - dark", heightDp = 1300)
@Composable
private fun SettingsScreenPreviewDark() = GestoPreviewSurface(darkTheme = true) {
    SettingsScreen(SettingsUiState(), SettingsActions())
}

@Preview(name = "Settings - light, custom", heightDp = 1300)
@Composable
private fun SettingsScreenPreviewLight() = GestoPreviewSurface(darkTheme = false) {
    SettingsScreen(
        SettingsUiState(
            quality = QualityPreset.Custom,
            streaming = QualityPreset.Custom.settings(showStats = true),
            touchMode = TouchMode.Direct,
            themeMode = ThemeMode.Light,
            pointerSpeed = 1.5f,
        ),
        SettingsActions(),
    )
}
